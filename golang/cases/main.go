package main

import (
	"github.com/Z88897050/emulateSamples/golang/cases/qiniulog"
	"github.com/Z88897050/emulateSamples/golang/cases/yamltest"
	"github.com/Z88897050/emulateSamples/golang/cases/zaplog"
)

func main() {
	zaplog.LogTest()
	qiniulog.LogTest()
	yamltest.YamlTest()

}
