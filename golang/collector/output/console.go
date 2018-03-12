package output

import "log"

type console struct{}

func init() {
	var output console
	Register("default", output)
}

// Output to do nothing
func (o console) Output(key string, value []byte) error {
	log.Println("key: ", key, " value: ", string(value))
	return nil
}

// Close to do nothing
func (o console) Close() error {
	return nil
}
