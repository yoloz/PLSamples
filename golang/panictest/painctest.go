package main

import (
	"fmt"
)

func createPanic() {
	panic("create panic")
}
func recoverPanic(f func()) (b bool) {
	defer func() {
		if x := recover(); x != nil {
			fmt.Println(x)
		}
	}()
	f()
	return
}
func main() {
	recoverPanic(createPanic)
}
