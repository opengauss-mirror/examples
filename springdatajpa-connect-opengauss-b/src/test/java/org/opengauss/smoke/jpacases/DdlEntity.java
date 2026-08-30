package org.opengauss.smoke.jpacases;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ddl-auto 行为测试专用实体，对应表 t_jpa_ddl（表本身不存在，由 Hibernate 生成）。
 * 结构刻意简单（id + name），便于观察 Hibernate 生成的建表 SQL 在 B 模式下的表现。
 */
@Entity
@Table(name = "t_jpa_ddl")
public class DdlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    public DdlEntity() {
    }

    public DdlEntity(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
