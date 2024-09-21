package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.entity.pagination.TestEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OpenGaussDialectPaginationIntegrationTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createQuery("delete from TestEntity").executeUpdate();
        for (int i = 1; i <= 100; i++) {
            TestEntity entity = new TestEntity();
            entity.setName("Name " + i);
            session.save(entity);
        }

        session.getTransaction().commit();
        session.close();
    }

    @AfterAll
    public static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testPaginationWithOffset() {
        Session session = sessionFactory.openSession();
        int pageSize = 10;
        int pageNumber = 3;
        String hql = "from TestEntity order by id";
        Query<TestEntity> query = session.createQuery(hql, TestEntity.class);
        query.setFirstResult(pageNumber * pageSize); // 设置 offset
        query.setMaxResults(pageSize); // 设置 limit
        List<TestEntity> results = query.list();
        Assertions.assertEquals(pageSize, results.size());
        Assertions.assertEquals(31L, results.get(0).getId());
        Assertions.assertEquals(40L, results.get(results.size() - 1).getId());
        session.close();
    }

    @Test
    public void testPaginationWithoutOffset() {
        Session session = sessionFactory.openSession();
        int pageSize = 10;
        String hql = "from TestEntity order by id";
        Query<TestEntity> query = session.createQuery(hql, TestEntity.class);
        query.setMaxResults(pageSize);
        List<TestEntity> results = query.list();
        Assertions.assertEquals(pageSize, results.size());
        Assertions.assertEquals(1L, results.get(0).getId());
        Assertions.assertEquals(10L, results.get(results.size() - 1).getId());
        session.close();
    }
}
