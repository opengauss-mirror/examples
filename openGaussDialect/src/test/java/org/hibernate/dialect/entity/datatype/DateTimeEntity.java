package org.hibernate.dialect.entity.datatype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@Entity
@Table(name = "date_time_test_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DateTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Date dateField;
    private Time timeField;
    private Timestamp timestampField;
    @Column(name = "timetzField", columnDefinition = "timetz")
    private Timestamp timetzField;
    @Column(name = "timestamptzField", columnDefinition = "timestamptz")
    private Timestamp timestamptzField;
}
