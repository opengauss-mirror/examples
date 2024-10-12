package org.hibernate.dialect.unit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

import javax.persistence.*;
import java.sql.Timestamp;

public class OpenGaussDialectCurrentTimestampTest {
    @Entity
    @Table(name = "test_entity")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class TestEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
        private String name;
        @Version
        private Timestamp version;
    }

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
    }

    @AfterAll
    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testCurrentTimestampSelection() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        OpenGaussDialect dialect = new OpenGaussDialect();
        String sql = dialect.getCurrentTimestampSelectString();
        Timestamp currentTimestamp = (Timestamp) session.createNativeQuery(sql).getSingleResult();
        System.out.println("从数据库获取的当前时间戳: " + currentTimestamp);
        Timestamp systemTimestamp = new Timestamp(System.currentTimeMillis());
        System.out.println("从系统获取的当前时间戳: " + systemTimestamp);
        long difference = Math.abs(systemTimestamp.getTime() - currentTimestamp.getTime());
        Assertions.assertTrue(difference < 10000, "时间戳差异应小于 1 秒");
        session.getTransaction().commit();
    }

    @Test
    public void testVersioning() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        TestEntity entity = new TestEntity();
        entity.setName("测试名称");
        session.save(entity);
        session.getTransaction().commit();
        System.out.println("初始版本（时间戳）: " + entity.getVersion());
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        session.beginTransaction();
        entity.setName("更新后的名称");
        session.update(entity);
        session.getTransaction().commit();
        System.out.println("更新后的版本（时间戳）: " + entity.getVersion());
        Assertions.assertNotNull(entity.getVersion());
    }
}
