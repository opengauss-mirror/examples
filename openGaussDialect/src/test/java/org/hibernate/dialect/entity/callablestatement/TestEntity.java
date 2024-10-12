package org.hibernate.dialect.entity.callablestatement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "test_callable_statement_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity {
    @Id
    private Long id;
    private String name;
}

