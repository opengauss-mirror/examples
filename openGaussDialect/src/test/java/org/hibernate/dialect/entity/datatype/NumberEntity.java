package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "number_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NumberEntity {
    @Id
    private Long id;
    private Byte tinyIntField;
    private Short smallIntField;
    private Integer integerField;
    private Long bigIntField;
    private Float floatField;
    private Double doubleField;
    @Column(columnDefinition = "decimal(19, 2)")
    private BigDecimal decimalField;
    private BigDecimal numericField;
}