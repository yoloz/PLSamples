package output

// nothing to do nothing
type nothing struct{}

func init() {
	var output nothing
	Register("null", output)
}

// Output to do nothing
func (o nothing) Output(key string, value []byte) error {
	return nil
}

// Close to do nothing
func (o nothing) Close() error {
	return nil
}
