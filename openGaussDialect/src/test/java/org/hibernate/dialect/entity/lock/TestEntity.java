package org.hibernate.dialect.entity.lock;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "test_lock_entity")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestEntity {
    @Id
    private Long id;
    private String name;
    private Double balance;
}