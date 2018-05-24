package main

import (
	"flag"
	"fmt"
	"io/ioutil"
	"os"
	"os/signal"
	"syscall"

	// "github.com/labstack/echo"
	"github.com/qiniu/log"
	yaml "gopkg.in/yaml.v2"
)

// Config configuration `yaml:"loglevel"`
type Config struct {
	Webpath  string
	Hosts    []string `yaml:",flow"`
	Loglevel int
}

var (
	confName = flag.String("f", "conf.yml", "configuration file to load")
)
var conf Config

func waitForInterrupt(interrupt func()) {

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

func main() {
	flag.Usage = func() {
		fmt.Println(`
			Usage:
			
			  mointorKafka [flags]
			
			The flags are:
			  -h                 print usage info to stdout.
			  -f <file>          configuration file to load
			
			Examples:
			
			  # start
			  monitorKafka -f conf.yml
			`)
		os.Exit(0)
	}
	flag.Parse()
	data, err := ioutil.ReadFile(*confName)
	if err != nil {
		log.Fatal("config file read failed:", err)
	}
	if err := yaml.Unmarshal(data, &conf); err != nil {
		log.Fatal("yaml unmarshal failed:", err)
	}
	log.SetOutputLevel(conf.Loglevel)
	log.Infof("hosts %v", len(conf.Hosts))
	log.Info(conf.Hosts[0])
	log.Info(conf.Loglevel)
	log.Infof("conf %v", conf.Webpath)
	// e := echo.New()
	// e.Static("/", conf.Webpath)
	
}
