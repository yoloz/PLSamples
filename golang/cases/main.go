package main

import (
	"errors"
	"fmt"
	log "ylzhang/golang/cases/zaplog"
)

func main() {

	log.Configure(log.DefaultConfig())
	logger := log.NewLogger("test")
	logger.Info("testMsg")
	logger1 := log.NewLogger("test1")
	logger1.Warn("test1Msg")
	logger.Error(errors.New("error test"))
	fmt.Println(&logger)
	fmt.Println(&logger1)

	// qiniulog.SetOutput(os.Stdout)
	// qiniulog.SetOutputLevel(qiniulog.Ldebug)

	// qiniulog.Debugf("Debug: foo\n")
	// qiniulog.Debug("Debug: foo")

	// qiniulog.Infof("Info: foo\n")
	// qiniulog.Info("Info: foo")

	// qiniulog.Warnf("Warn: foo\n")
	// qiniulog.Warn("Warn: foo")

	// qiniulog.Errorf("Error: foo\n")
	// qiniulog.Error("Error: foo")

	// qiniulog.SetOutputLevel(qiniulog.Linfo)

	// qiniulog.Debugf("Debug: foo\n")
	// qiniulog.Debug("Debug: foo")

	// qiniulog.Infof("Info: foo\n")
	// qiniulog.Info("Info: foo")

	// qiniulog.Warnf("Warn: foo\n")
	// qiniulog.Warn("Warn: foo")

	// qiniulog.Errorf("Error: foo\n")
	// qiniulog.Error("Error: foo")
}
