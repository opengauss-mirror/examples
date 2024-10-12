package org.hibernate.dialect.entity.miscellaneous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "test_no_columns_insert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestNoColumnsInsert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
