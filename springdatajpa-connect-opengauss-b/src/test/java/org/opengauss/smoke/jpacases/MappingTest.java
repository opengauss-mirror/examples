package org.opengauss.smoke.jpacases;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opengauss.smoke.jpacases.JpaDbUtil.*;

/**
 * JPA 类型映射：BigDecimal / LocalDate / LocalDateTime / Boolean / String
 * 实体-表往返（save → findById 值一致），IDENTITY 主键回填。
 */
@SpringBootTest
class MappingTest {

    @Autowired
    private JpaMapRepository repo;

    @BeforeAll
    static void createTable() throws SQLException {
        try (Connection c = raw()) {
            exec(c,
                    "DROP TABLE IF EXISTS t_jpa_map",
                    "CREATE TABLE t_jpa_map ("
                            + " id INT AUTO_INCREMENT PRIMARY KEY,"
                            + " n_dec DECIMAL(10,2),"
                            + " n_date DATE,"
                            + " n_datetime DATETIME,"
                            + " n_bool TINYINT(1),"
                            + " n_name VARCHAR(50)"
                            + ")");
        }
        log("M0 建表 t_jpa_map 成功");
    }

    @Test
    @DisplayName("M1 五种类型映射往返 + IDENTITY 回填")
    void roundtrip() {
        BigDecimal dec = new BigDecimal("3.14");
        LocalDate date = LocalDate.of(2026, 8, 28);
        LocalDateTime datetime = LocalDateTime.of(2026, 8, 28, 12, 34, 56);
        JpaMapEntity saved = repo.save(new JpaMapEntity(dec, date, datetime, Boolean.TRUE, "jpa_map"));

        assertNotNull(saved.getId(), "save 后应回填自增主键");
        log("M1 保存成功，IDENTITY 回填 id=%d", saved.getId());

        JpaMapEntity found = repo.findById(saved.getId()).orElseThrow();
        assertEquals(dec, found.getDec(), "DECIMAL 往返");
        assertEquals(date, found.getDate(), "DATE 往返");
        assertEquals(datetime, found.getDatetime(), "DATETIME 往返");
        assertEquals(Boolean.TRUE, found.getBool(), "TINYINT(1) ↔ Boolean 往返");
        assertEquals("jpa_map", found.getName(), "VARCHAR 往返");
        log("M1 五种类型映射往返一致：DECIMAL/DATE/DATETIME/Boolean/String");

        repo.delete(found);
    }
}
