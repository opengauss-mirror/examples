package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "binary_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinaryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private byte[] binaryField;
    @Column(name = "varBinaryField", columnDefinition = "bytea")
    private byte[] varBinaryField;
    @Column(name = "longVarBinaryField", columnDefinition = "bytea")
    private byte[] longVarBinaryField;
}
