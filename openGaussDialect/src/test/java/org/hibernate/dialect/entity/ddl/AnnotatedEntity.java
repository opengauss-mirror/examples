package org.hibernate.dialect.entity.ddl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import javax.persistence.*;

@Entity
@Table(name = "annotated_table")
@org.hibernate.annotations.Table(appliesTo = "annotated_table", comment = "This is a table comment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annotated_column")
    @Comment("This is a column comment")
    private String annotatedField;
}
