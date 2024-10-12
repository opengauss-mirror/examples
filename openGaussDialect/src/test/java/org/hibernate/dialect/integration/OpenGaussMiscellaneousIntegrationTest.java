package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.entity.miscellaneous.*;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

import java.util.List;

public class OpenGaussMiscellaneousIntegrationTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(
                TestNoColumnsInsert.class,
                ParentEntity.class,
                ChildEntity.class,
                TestStringEntity.class,
                TestBooleanEntity.class);
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.createNativeQuery("DELETE FROM test_no_columns_insert").executeUpdate();
        session.createNativeQuery("DELETE FROM child_entity").executeUpdate();
        session.createNativeQuery("DELETE FROM parent_entity").executeUpdate();
        session.createNativeQuery("DELETE FROM test_string_entity").executeUpdate();
        session.createNativeQuery("DELETE FROM test_boolean_entity").executeUpdate();
        transaction.commit();
    }

    @AfterAll
    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testNoColumnsInsert() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        TestNoColumnsInsert entity = new TestNoColumnsInsert();
        session.save(entity);
        transaction.commit();
        session = sessionFactory.openSession();
        TestNoColumnsInsert result = session.get(TestNoColumnsInsert.class, entity.getId());
        Assertions.assertNotNull(result);
    }

    @Test
    public void testCaseExpression() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        String hql = "SELECT CASE WHEN e.id IS NOT NULL THEN 'Exists' ELSE 'Not Exists' END FROM TestNoColumnsInsert e";
        List<String> results = session.createQuery(hql, String.class).list();
        Assertions.assertNotNull(results);
        transaction.commit();
    }

    @Test
    public void testJoinQuery() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        ParentEntity parent = new ParentEntity();
        session.save(parent);
        ChildEntity child = new ChildEntity();
        child.setParent(parent);
        session.save(child);
        transaction.commit();
        transaction = session.beginTransaction();
        String hql = "SELECT c FROM ChildEntity c JOIN c.parent p WHERE p.id = :parentId";
        List<ChildEntity> results = session.createQuery(hql, ChildEntity.class).setParameter("parentId", parent.getId()).list();
        Assertions.assertEquals(1, results.size());
        transaction.commit();
    }

    @Test
    public void testCaseInsensitiveLike() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        TestStringEntity entity = new TestStringEntity();
        entity.setName("TestName");
        session.save(entity);
        transaction.commit();
        transaction = session.beginTransaction();
        List<TestStringEntity> results = session.createCriteria(TestStringEntity.class).add(Restrictions.ilike("name", "test%")).list();
        Assertions.assertEquals(1, results.size());
        transaction.commit();
    }

    @Test
    public void testBooleanValue() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        TestBooleanEntity entity = new TestBooleanEntity();
        entity.setActive(true);
        session.save(entity);
        transaction.commit();
        session = sessionFactory.openSession();
        TestBooleanEntity result = session.get(TestBooleanEntity.class, entity.getId());
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getActive());
    }
}
