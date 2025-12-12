package Models

type TypeRoom struct {
	TypeRoom    string `gorm:"type:varchar(30);primaryKey" json:"type_room"`
	Description string `gorm:"type:text" json:"description"`
}

func (TypeRoom) TableName() string { return "type_room" }
