package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.dialect.entity.ddl.TestSchemaEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

public class OpenGaussDialectDDLSchemaTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(null);
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        String dropSchemaSQL = "DROP SCHEMA IF EXISTS test_schema CASCADE";
        session.createNativeQuery(dropSchemaSQL).executeUpdate();
        transaction.commit();
        session.close();
    }

    @AfterAll
    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    @Order(1)
    public void testSchemaCreation() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        OpenGaussDialect dialect = new OpenGaussDialect();
        String[] createSchemaSQL = dialect.getCreateSchemaCommand("test_schema");
        for (String sql : createSchemaSQL) {
            session.createNativeQuery(sql).executeUpdate();
        }
        String checkSchemaSQL = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'test_schema'";
        String schemaName = (String) session.createNativeQuery(checkSchemaSQL).uniqueResult();
        Assertions.assertEquals("test_schema", schemaName);
        transaction.commit();
    }

    @Test
    public void testGetCurrentSchema() {
        Session session = sessionFactory.openSession();
        OpenGaussDialect dialect = new OpenGaussDialect();
        String currentSchema = (String) session.createNativeQuery(dialect.getCurrentSchemaCommand()).uniqueResult();
        Assertions.assertEquals("public", currentSchema);
        session.close();
    }

    @Test
    public void testSchemaDeletion() {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        OpenGaussDialect dialect = new OpenGaussDialect();
        String[] dropSchemaSQL = dialect.getDropSchemaCommand("test_schema");
        for (String sql : dropSchemaSQL) {
            session.createNativeQuery(sql).executeUpdate();
        }
        String checkSchemaSQL = "SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'test_schema'";
        Assertions.assertNull(session.createNativeQuery(checkSchemaSQL).uniqueResult());
        transaction.commit();
    }

    @Test
    public void testIndexNameQualification() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        String createSchemaSQL = "create schema if not exists test_schema";
        session.createNativeQuery(createSchemaSQL).executeUpdate();
        session.getTransaction().commit();
        session.close();
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory(TestSchemaEntity.class);
        session = sessionFactory.openSession();
        String sql = "SELECT schemaname, indexname FROM pg_indexes WHERE schemaname = 'test_schema' AND indexname = 'idx_name'";
        Object[] result = (Object[]) session.createNativeQuery(sql).uniqueResult();
        Assertions.assertEquals("test_schema", result[0]);
        Assertions.assertEquals("idx_name", result[1]);
        session.close();
    }
}
