package log

import (
	"testing"
)

func TestLoadLogger(t *testing.T) {
	Configure(defaultConfig)
	if old := loadLogger(); old == nil {
		t.Fail()
	}
	// logger := NewLogger("test")
	// logger.Info("test info")
}
