package dialect

type Name int

func (n Name) String() string {
	return "openGauss"
}

const (
	Invalid Name = iota
	OPENGAUSS
)
