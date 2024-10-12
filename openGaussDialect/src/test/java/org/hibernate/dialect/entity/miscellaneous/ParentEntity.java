package org.hibernate.dialect.entity.miscellaneous;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parent_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    private Set<ChildEntity> children = new HashSet<>();
}

