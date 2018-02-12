package output

import (
	"log"
)

// null-nothing default-console
var outputs = make(map[string]output)

//Output output file data
type output interface {
	Output(key string, value []byte) error
	Close() error
}

// Register is called to register a output for use by the program.
func Register(outType string, output output) {
	if _, exists := outputs[outType]; exists {
		log.Println(outType, " already registered")
	}
	log.Println("Register ", outType)
	outputs[outType] = output
}
