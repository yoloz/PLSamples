package main

import (
	"log"
	"os"
	"os/exec"
	"path/filepath"
	"ylzhang/golang/gatherdata/module/collectfile"
)

func init() {
	log.SetOutput(os.Stdout)
}
func main() {
	execPath, err := exec.LookPath(os.Args[0])
	if err != nil {
		log.Fatal(err)
	}
	//Is Symlink
	fi, err := os.Lstat(execPath)
	if err != nil {
		log.Fatal(err)
	}
	if fi.Mode()&os.ModeSymlink == os.ModeSymlink {
		execPath, err = os.Readlink(execPath)
		if err != nil {
			log.Fatal(err)
		}
	}
	execDir := filepath.Dir(execPath)
	if execDir == "." {
		execDir, err = os.Getwd()
		if err != nil {
			log.Fatal(err)
		}
	}
	collCfg, err := collectfile.CollectCfg(execDir)
	if err != nil {
		log.Fatal(err)
	}
	log.Println(execDir)
	log.Println(collCfg.Mode)
	// log.Println(collCfg.)
	// propPath := flag.String("conf","/home/manu/sample/","DB data path")
}
