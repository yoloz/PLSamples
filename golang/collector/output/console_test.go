package output

import (
	"testing"
)

func TestOutput(t *testing.T) {
	var output console
	output.Output("console output key", []byte("console output value"))
}
