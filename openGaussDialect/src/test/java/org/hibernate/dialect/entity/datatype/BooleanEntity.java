package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "bool_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BooleanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean booleanField;

    @Column(columnDefinition = "bit")
    private Boolean bitField;
}
