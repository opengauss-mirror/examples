package org.hibernate.dialect.entity.miscellaneous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;


@Entity
@Table(name = "child_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChildEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private ParentEntity parent;
}