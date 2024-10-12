package org.hibernate.dialect.integration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.EnumSet;

public class OpenGaussDialectDDLTableAndColumTest {

    private static StandardServiceRegistry standardRegistry;

    private static Metadata metadata;

    private static SessionFactory sessionFactory;

    public static void setUp(Class<?>... testClasses) {
        standardRegistry = new StandardServiceRegistryBuilder().configure().build();
        MetadataSources metadataSources = new MetadataSources(standardRegistry);
        for (Class<?> testClass : testClasses) {
            metadataSources.addAnnotatedClass(testClass);
        }
        metadata = metadataSources.buildMetadata();
        sessionFactory = metadata.buildSessionFactory();
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createNativeQuery("DELETE FROM test_table").executeUpdate();
        session.getTransaction().commit();
        session.close();
    }

    @AfterEach
    public void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (standardRegistry != null) {
            StandardServiceRegistryBuilder.destroy(standardRegistry);
        }
    }


    @Entity
    @Table(name = "test_table")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestEntity1 {
        @Id
        private Long id;
        private String name;
    }

    @Test
    public void testCreateTable() {
        setUp(TestEntity1.class);
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.execute(EnumSet.of(TargetType.DATABASE), SchemaExport.Action.CREATE, metadata);
        Session session = sessionFactory.openSession();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_name = 'test_table'";
        String tableName = (String) session.createNativeQuery(sql).uniqueResult();
        Assertions.assertEquals("test_table", tableName);
        session.close();
    }

    @Entity
    @Table(name = "test_table")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestEntity2 {
        @Id
        private Long id;
        private String name;
        private String description;
    }

    @Test
    public void testAddColumn() {
        setUp(TestEntity1.class);
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.execute(EnumSet.of(TargetType.DATABASE), SchemaExport.Action.CREATE, metadata);
        sessionFactory.close();
        setUp(TestEntity2.class);
        SchemaUpdate schemaUpdate = new SchemaUpdate();
        schemaUpdate.execute(EnumSet.of(TargetType.DATABASE), metadata);
        Session session = sessionFactory.openSession();
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = 'test_table' AND column_name = 'description'";
        String columnName = (String) session.createNativeQuery(sql).uniqueResult();
        Assertions.assertEquals("description", columnName);
        session.close();
    }

    @Test
    public void testDropTable() {
        setUp(TestEntity2.class);
        SchemaExport schemaExport = new SchemaExport();
        schemaExport.execute(EnumSet.of(TargetType.DATABASE), SchemaExport.Action.DROP, metadata);
        Session session = sessionFactory.openSession();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_name = 'test_table'";
        String tableName = (String) session.createNativeQuery(sql).uniqueResult();
        Assertions.assertNull(tableName);
        session.close();
    }
}
