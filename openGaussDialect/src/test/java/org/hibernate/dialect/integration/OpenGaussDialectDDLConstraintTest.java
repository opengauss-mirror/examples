package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.entity.ddl.ChildEntity;
import org.hibernate.dialect.entity.ddl.ParentEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class OpenGaussDialectDDLConstraintTest {

    private static SessionFactory sessionFactory;

    @BeforeEach
    public void setUp() {
        sessionFactory = HibernateUtil.getSessionFactory(ParentEntity.class, ChildEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createNativeQuery("DELETE FROM child_table").executeUpdate();
        session.createNativeQuery("DELETE FROM parent_table").executeUpdate();
        session.getTransaction().commit();
        session.close();
    }

    @AfterEach
    public void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testConstraintCreation() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        ParentEntity parent = new ParentEntity();
        session.save(parent);
        ChildEntity child = new ChildEntity();
        child.setParent(parent);
        session.save(child);
        session.getTransaction().commit();
        session.beginTransaction();
        String sql = "SELECT constraint_name FROM information_schema.table_constraints WHERE table_name='child_table' AND constraint_type='FOREIGN KEY'";
        String constraintName = (String) session.createNativeQuery(sql).uniqueResult();
        assertEquals("fk_child_parent", constraintName);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testCascadeDelete() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        ParentEntity parent = new ParentEntity();
        ChildEntity child1 = new ChildEntity();
        ChildEntity child2 = new ChildEntity();
        parent.addChild(child1);
        parent.addChild(child2);
        session.save(parent);
        session.getTransaction().commit();
        session.beginTransaction();
        session.delete(parent);
        session.getTransaction().commit();
        session.beginTransaction();
        Long childCount = (Long) session.createQuery("SELECT COUNT(c) FROM ChildEntity c").uniqueResult();
        assertEquals(Long.valueOf(0), childCount);
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testDropConstraintWithIfExists() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        String addConstraintSQL = "alter table child_table add constraint test_constraint CHECK (id > 0)";
        session.createNativeQuery(addConstraintSQL).executeUpdate();
        session.getTransaction().commit();
        session.beginTransaction();
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) sessionFactory;
        Dialect dialect = sfi.getDialect();
        String dropConstraintSQL;
        if (dialect.supportsIfExistsBeforeConstraintName()) {
            dropConstraintSQL = "alter table child_table drop constraint if exists test_constraint" + dialect.getCascadeConstraintsString();
        } else if (dialect.supportsIfExistsAfterConstraintName()) {
            dropConstraintSQL = "alter table child_table drop constraint test_constraint IF EXISTS" + dialect.getCascadeConstraintsString();
        } else {
            dropConstraintSQL = "alter table child_table drop constraint test_constraint" + dialect.getCascadeConstraintsString();
        }
        session.createNativeQuery(dropConstraintSQL).executeUpdate();
        session.getTransaction().commit();
        session.beginTransaction();
        String checkConstraintSQL = "SELECT constraint_name FROM information_schema.table_constraints WHERE table_name='child_table' AND constraint_name='test_constraint'";
        Object result = session.createNativeQuery(checkConstraintSQL).uniqueResult();
        assertNull(result);
        session.getTransaction().commit();
        session.close();
    }
}
