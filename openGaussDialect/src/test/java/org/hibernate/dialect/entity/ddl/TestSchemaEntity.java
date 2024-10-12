package org.hibernate.dialect.entity.ddl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "test_table", schema = "test_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestSchemaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @org.hibernate.annotations.Index(name = "idx_name")
    private String name;
}
