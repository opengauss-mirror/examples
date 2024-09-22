package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.entity.ddl.AnnotatedEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

public class OpenGaussDialectDDLCommentTest {
    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(AnnotatedEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createNativeQuery("DELETE FROM annotated_table").executeUpdate();
        session.getTransaction().commit();
        session.close();
    }

    @AfterAll
    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testTableAndColumnComments() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        AnnotatedEntity entity = new AnnotatedEntity();
        entity.setAnnotatedField("Test");
        session.save(entity);
        session.getTransaction().commit();
        String tableCommentQuery = "SELECT description " + "FROM pg_description " + "WHERE objoid = (" + "    SELECT oid FROM pg_class WHERE relname = 'annotated_table'" + ") AND objsubid = 0";
        String tableComment = (String) session.createNativeQuery(tableCommentQuery).getSingleResult();
        Assertions.assertEquals("This is a table comment", tableComment);
        String columnCommentQuery = "SELECT description " + "FROM pg_description " + "WHERE objoid = (" + "    SELECT oid FROM pg_class WHERE relname = 'annotated_table'" + ") AND objsubid = (" + "    SELECT attnum FROM pg_attribute " + "    WHERE attrelid = (" + "        SELECT oid FROM pg_class WHERE relname = 'annotated_table'" + "    ) AND attname = 'annotated_column'" + ")";
        String columnComment = (String) session.createNativeQuery(columnCommentQuery).getSingleResult();
        Assertions.assertEquals("This is a column comment", columnComment);
        session.close();
    }
}
