package org.opengauss.smoke.jpacases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA 类型映射测试实体，对应表 t_jpa_map（由 MappingTest 的 @BeforeAll 用 SQL 创建）。
 * 覆盖 JPA/Hibernate 6 常用映射：BigDecimal / LocalDate / LocalDateTime / Boolean / String。
 */
@Entity
@Table(name = "t_jpa_map")
public class JpaMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "n_dec")
    private BigDecimal dec;

    @Column(name = "n_date")
    private LocalDate date;

    @Column(name = "n_datetime")
    private LocalDateTime datetime;

    @Column(name = "n_bool")
    private Boolean bool;

    @Column(name = "n_name")
    private String name;

    public JpaMapEntity() {
    }

    public JpaMapEntity(BigDecimal dec, LocalDate date, LocalDateTime datetime, Boolean bool, String name) {
        this.dec = dec;
        this.date = date;
        this.datetime = datetime;
        this.bool = bool;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getDec() {
        return dec;
    }

    public void setDec(BigDecimal dec) {
        this.dec = dec;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }

    public Boolean getBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
