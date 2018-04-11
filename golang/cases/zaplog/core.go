package zaplog

import (
	"os"
	"path/filepath"
	"sync/atomic"
	"unsafe"

	"github.com/pkg/errors"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var baseEncodingConfig = zapcore.EncoderConfig{
	TimeKey:        "ts",
	LevelKey:       "level",
	NameKey:        "logger",
	CallerKey:      "caller",
	MessageKey:     "msg",
	StacktraceKey:  "stacktrace",
	LineEnding:     zapcore.DefaultLineEnding,
	EncodeLevel:    zapcore.LowercaseLevelEncoder,
	EncodeTime:     zapcore.ISO8601TimeEncoder,
	EncodeDuration: zapcore.SecondsDurationEncoder,
	EncodeCaller:   zapcore.ShortCallerEncoder,
}

var (
	_log unsafe.Pointer // Pointer to a coreLogger. Access via atomic.LoadPointer.
)

func init() {
	storeLogger(&coreLogger{
		rootLogger:   zap.NewNop(),
		globalLogger: zap.NewNop(),
	})
}

type coreLogger struct {
	rootLogger   *zap.Logger
	globalLogger *zap.Logger
}

// Configure configures the log package.
func Configure(cfg Config) error {
	var cores []zapcore.Core
	if cfg.ToConsole {
		if c, err := makeStdoutOutput(cfg); err == nil {
			cores = append(cores, c)
		} else {
			return errors.Wrap(err, "failed to build log output")
		}
	}
	if cfg.ToFiles {
		if c, err := makeFileOutput(cfg); err == nil {
			cores = append(cores, c)
		} else {
			return errors.Wrap(err, "failed to build log output")
		}
	}
	if cores == nil {
		cores = append(cores, zapcore.NewNopCore())
	}
	rootL := zap.New(zapcore.NewTee(cores...), makeOptions(cfg)...)
	storeLogger(&coreLogger{
		rootLogger:   rootL,
		globalLogger: rootL.WithOptions(zap.AddCallerSkip(1)),
	})
	return nil
}

// add caller and stacktrace
func makeOptions(cfg Config) []zap.Option {
	var options []zap.Option
	options = append(options, zap.AddCaller())
	options = append(options, zap.AddStacktrace(zapcore.ErrorLevel))
	// if cfg.development {
	// 	options = append(options, zap.Development())
	// }
	return options
}

// Sync flushes any buffered log entries. Applications should take care to call Sync before exiting.
func Sync() error {
	return loadLogger().rootLogger.Sync()
}

func makeStdoutOutput(cfg Config) (zapcore.Core, error) {
	stdout := zapcore.Lock(os.Stdout)
	return zapcore.NewCore(zapcore.NewConsoleEncoder(baseEncodingConfig),
		stdout, cfg.Level.zapLevel()), nil
}

func makeFileOutput(cfg Config) (zapcore.Core, error) {
	name := "collector"
	if cfg.Files.Name != "" {
		name = cfg.Files.Name
	}
	filename := filepath.Join(cfg.Files.Path, name)

	rotator, err := NewFileRotator(filename,
		MaxSizeBytes(cfg.Files.MaxSize),
		MaxBackups(cfg.Files.MaxBackups),
		Permissions(os.FileMode(cfg.Files.Permissions)),
	)
	if err != nil {
		return nil, errors.Wrap(err, "failed to create file rotator")
	}

	return zapcore.NewCore(zapcore.NewConsoleEncoder(baseEncodingConfig),
		rotator, cfg.Level.zapLevel()), nil
}

func loadLogger() *coreLogger {
	p := atomic.LoadPointer(&_log)
	return (*coreLogger)(p)
}

func storeLogger(l *coreLogger) {
	if old := loadLogger(); old != nil {
		old.rootLogger.Sync()
	}
	atomic.StorePointer(&_log, unsafe.Pointer(l))
}
