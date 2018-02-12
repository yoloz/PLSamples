package collectfile

import (
	"flag"
	"log"
	"time"

	"github.com/magiconair/properties"
)

func conf() {
	// init from a file
	p := properties.MustLoadFile("${HOME}/config.properties", properties.UTF8)

	// or multiple files
	p = properties.MustLoadFiles([]string{
		"${HOME}/config.properties",
		"${HOME}/config-${USER}.properties",
	}, properties.UTF8, true)

	// or from a map
	p = properties.LoadMap(map[string]string{"key": "value", "abc": "def"})

	// or from a string
	p = properties.MustLoadString("key=value\nabc=def")

	// or from a URL
	p = properties.MustLoadURL("http://host/path")

	// or from flags
	p.MustFlag(flag.CommandLine)

	// or through Decode
	type Config struct {
		Host    string        `properties:"host"`
		Port    int           `properties:"port,default=9000"`
		Accept  []string      `properties:"accept,default=image/png;image;gif"`
		Timeout time.Duration `properties:"timeout,default=5s"`
	}
	var cfg Config
	if err := p.Decode(&cfg); err != nil {
		log.Fatal(err)
	}
}
