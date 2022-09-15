//go:build appengine
// +build appengine

package ogdialect

func bytesToString(b []byte) string {
	return string(b)
}

func stringToBytes(s string) []byte {
	return []byte(s)
}
