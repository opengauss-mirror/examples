package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.dialect.pojo.Person;
import org.hibernate.dialect.type.JsonType;
import org.hibernate.dialect.type.JsonbType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
        @TypeDef(name = "jsonb", typeClass = JsonbType.class),
        @TypeDef(name = "json", typeClass = JsonType.class),
})
public class JsonEntity {
    @Id
    private Long id;

    @Type(type = "json")
    private Person pojoJson;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Person pojoJsonb;
}
