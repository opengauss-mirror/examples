package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.entity.sequence.TestEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

public class OpenGaussDialectSequenceIntegrationTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
    }

    @AfterAll
    public static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @BeforeEach
    public void setUp() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createQuery("delete from TestEntity").executeUpdate();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testSequenceIdentifierGeneration() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        TestEntity entity1 = new TestEntity();
        entity1.setName("Entity One");
        session.save(entity1);
        TestEntity entity2 = new TestEntity();
        entity2.setName("Entity Two");
        session.save(entity2);
        session.getTransaction().commit();
        session.close();
        // 验证 ID 是否自动生成，且递增
        Assertions.assertNotNull(entity1.getId());
        Assertions.assertNotNull(entity2.getId());
        Assertions.assertTrue(entity2.getId() > entity1.getId());
        // 查询并验证数据
        session = sessionFactory.openSession();
        TestEntity result1 = session.get(TestEntity.class, entity1.getId());
        TestEntity result2 = session.get(TestEntity.class, entity2.getId());
        Assertions.assertNotNull(result1);
        Assertions.assertEquals("Entity One", result1.getName());
        Assertions.assertNotNull(result2);
        Assertions.assertEquals("Entity Two", result2.getName());
        session.close();
    }
}

