package main

import (
	"ylzhang/golang/cases/qiniulog"
	"ylzhang/golang/cases/yamltest"
	"ylzhang/golang/cases/zaplog"
)

func main() {
	zaplog.LogTest()
	qiniulog.LogTest()
	yamltest.YamlTest()

}
