package org.hibernate.dialect.entity.miscellaneous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "test_boolean_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestBooleanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean active;
}

