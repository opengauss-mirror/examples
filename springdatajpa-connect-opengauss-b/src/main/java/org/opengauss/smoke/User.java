package org.opengauss.smoke;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 业务表 `user` 的实体类。
 * 注意：Spring Boot 3.x / Hibernate 6 使用 jakarta.* 包（Boot 2.x 是 javax.*）。
 * user 在 MySQL 中是保留字，Hibernate 6 默认自动加反引号（auto_quote_keyword）。
 */
@Entity
@Table(name = "user")
public class User {

    /** IDENTITY 策略对应 MySQL 的 AUTO_INCREMENT，回填依赖 JDBC getGeneratedKeys。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private Integer age;

    public User() {
    }

    public User(String name, Integer age) {
        this.name = name;
        this.age = age;
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', age=" + age + "}";
    }
}
