package org.hibernate.dialect.entity.ddl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "child_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_child_parent"))
    private ParentEntity parent;
}
