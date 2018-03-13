package log

// Config contains the configuration options for the logger.
type Config struct {
	Level     Level      `config:"level"` // Logging level (error, warning, info, debug).
	ToConsole bool       `config:"to_console"`
	ToFiles   bool       `config:"to_files"`
	Files     FileConfig `config:"files"`
	// addCaller bool       // Adds package and line number info to messages.
}

// FileConfig contains the configuration options for the file output.
type FileConfig struct {
	Path        string `config:"path"`
	Name        string `config:"name"`
	MaxSize     uint   `config:"rotateeverybytes" validate:"min=1"`
	MaxBackups  uint   `config:"keepfiles" validate:"max=1024"`
	Permissions uint32 `config:"permissions"`
}

var defaultConfig = Config{
	Level:     InfoLevel,
	ToConsole: true,
	ToFiles:   false,
	Files: FileConfig{
		MaxSize:     10 * 1024 * 1024,
		Permissions: 0600,
	},
}

// DefaultConfig returns the default config options.
func DefaultConfig() Config {
	return defaultConfig
}
