package zaplog

import (
	"fmt"

	"github.com/pkg/errors"
)

// LogTest 测试
func LogTest() {
	Configure(DefaultConfig())
	logger := NewLogger("test")
	logger.Info("testMsg")
	logger1 := NewLogger("test1")
	logger1.Warn("test1Msg")
	logger.Error(errors.New("error test"))
	logger.Error(errors.New("error test1"))
	fmt.Println(&logger)
	fmt.Println(&logger1)
}
