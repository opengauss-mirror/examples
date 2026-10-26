package org.opengauss.entity;

/**
 * 用户实体类，对应 B 兼容库 mysql_test_db 下的 user 表
 */
public class User {

    /** 自增主键 */
    private Integer id;
    /** 用户名 */
    private String name;
    /** 年龄 */
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
