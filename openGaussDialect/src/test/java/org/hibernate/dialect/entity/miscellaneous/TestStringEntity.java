package org.hibernate.dialect.entity.miscellaneous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "test_string_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestStringEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
