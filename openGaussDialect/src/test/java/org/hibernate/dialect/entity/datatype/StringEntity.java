package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;

import javax.persistence.*;

@Entity
@Table(name = "string_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StringEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String varCharField;
    @Column(name = "longVarCharField", columnDefinition = "text")
    private String longVarCharField;
    @Nationalized
    private Character nCharField;
    @Nationalized
    private String nVarCharField;
}
