package main

import (
	"fmt"

	"github.com/Z88897050/emulateSamples/golang/cases/qiniulog"
	"github.com/Z88897050/emulateSamples/golang/cases/yamltest"
	"github.com/Z88897050/emulateSamples/golang/cases/zaplog"
)

func createPanic() {
	panic("create panic")
}
func recoverPanic(f func()) (b bool) {
	defer func() {
		if x := recover(); x != nil {
			fmt.Println(x)
		}
	}()
	f()
	return
}
func main() {
	zaplog.LogTest()
	qiniulog.LogTest()
	yamltest.YamlTest()

	recoverPanic(createPanic)
}
