package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "char_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Character characterField;
}
