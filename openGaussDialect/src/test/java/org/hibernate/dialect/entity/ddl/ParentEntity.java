package org.hibernate.dialect.entity.ddl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "parent_table")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildEntity> children = new ArrayList<>();

    public void addChild(ChildEntity child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(ChildEntity child) {
        children.remove(child);
        child.setParent(null);
    }
}
