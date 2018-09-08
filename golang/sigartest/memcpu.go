package main

import (
	"flag"
	"fmt"
	"log"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"github.com/cloudfoundry/gosigar"
)

var (
	running = true
	oldCPU  sigar.Cpu
	curCPU  sigar.Cpu
)
var process = flag.String("p", "", "get information about the specified process")

//WaitForInterrupt wait for singl to close
func WaitForInterrupt(interrupt func()) {

	// Set up channel on which to send signal notifications.
	// We must use a buffered channel or risk missing the signal
	// if we're not ready to receive when the signal is sent.
	c := make(chan os.Signal, 1)
	signal.Notify(c, syscall.SIGTERM, os.Interrupt, os.Kill)

	// Block until a signal is received.
	s := <-c

	log.Println("Receiving signal:", s)

	interrupt()
}

// copyCPU copy cpu
func copyCPU(target *sigar.Cpu, source *sigar.Cpu) {
	target.User = source.User
	target.Nice = source.Nice
	target.Sys = source.Sys
	target.Idle = source.Idle
	target.Wait = source.Wait
	target.Irq = source.Irq
	target.SoftIrq = source.SoftIrq
	target.Stolen = source.Stolen
}
func main() {
	flag.Usage = func() {
		fmt.Println(`
			Usage:
			process_performace -p 123
			`)
		os.Exit(0)
	}
	flag.Parse()
	if *process == "" {
		log.Fatal("pid is empty")
	} else {
		// cmd := exec.Command("top", "-b", "-n", "1", "-p", *process)
		// stdout, err := cmd.StdoutPipe()
		// if err != nil {
		// 	log.Fatal(err)
		// }
		// defer stdout.Close()
		// if err := cmd.Start(); err != nil {
		// 	log.Fatal(err)
		// }
		// opBytes, err := ioutil.ReadAll(stdout)
		// if err != nil {
		// 	log.Fatal(err)
		// }
		// log.Println(string(opBytes))
		// if err := cmd.Wait(); err != nil {
		// 	log.Printf("Command finished with error: %v", err)
		// }
		mem := sigar.ProcMem{}
		cpu := sigar.Cpu{}
		pid, err := strconv.Atoi(*process)
		if err != nil {
			log.Fatal(err)
		}
		go WaitForInterrupt(func() { running = false })
		fmt.Print("  CPU%  RSS\n")
		for running {
			if oldCPU.Total() == 0 {
				if err := cpu.Get(); err != nil {
					log.Fatal(err)
				}
				copyCPU(&oldCPU, &cpu)
				time.Sleep(time.Duration(500) * time.Millisecond)
			}
			if err := cpu.Get(); err != nil {
				log.Fatal(err)
			}
			copyCPU(&curCPU, &cpu)
			cpuUsage := (1 - float32(curCPU.Idle-oldCPU.Idle)/float32(curCPU.Total()-oldCPU.Total()))
			oldCPU = curCPU
			if err := mem.Get(pid); err != nil {
				log.Fatal(err)
			}
			fmt.Printf("%.2f%% %.2fM\n", cpuUsage*100, float32(mem.Resident/(1024*1024)))

			time.Sleep(time.Duration(2) * time.Second)
		}
	}

}
