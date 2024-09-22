package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.entity.callablestatement.TestEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.*;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class OpenGaussCallableStatementIntegrationTest {
    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        /**
         * OpenGauss 函数和存储过程示例：
         *
         * 函数：
         *
         * CREATE OR REPLACE FUNCTION get_users()
         * RETURNS refcursor AS
         * $$
         * DECLARE
         *     out_cursor refcursor;
         * BEGIN
         *     OPEN out_cursor FOR SELECT id, name FROM test_table;
         *     RETURN out_cursor;
         * END;
         * $$ LANGUAGE plpgsql;
         *
         * 存储过程：
         *
         * CREATE OR REPLACE PROCEDURE get_users(out_cursor OUT REFCURSOR)
         * AS
         * BEGIN
         *     OPEN out_cursor FOR SELECT id, name FROM users ORDER BY id;
         * END;
         */
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createNativeQuery("DELETE FROM test_table").executeUpdate();
        session.save(new TestEntity(1L, "Alice"));
        session.save(new TestEntity(2L, "Bob"));
        session.save(new TestEntity(3L, "Charlie"));
        session.getTransaction().commit();
        session.close();
    }

    @AfterAll
    public static void tearDownAll() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    @Test
    public void testCallableStatement() throws Exception {
        Session session = sessionFactory.openSession();
        Connection connection = session.doReturningWork(conn -> conn);
        String functionCall = "{? = call get_users()}";
        CallableStatement callableStatement = connection.prepareCall(functionCall);
        callableStatement.registerOutParameter(1, Types.REF_CURSOR);
        callableStatement.execute();
        ResultSet resultSet = (ResultSet) callableStatement.getObject(1);
        List<TestEntity> testEntities = new ArrayList<>();
        while (resultSet.next()) {
            Long id = resultSet.getLong("id");
            String name = resultSet.getString("name");
            testEntities.add(new TestEntity(id, name));
        }
        Assertions.assertEquals(3, testEntities.size(), "应检索到 3 个用户");
        Assertions.assertEquals("Alice", testEntities.get(0).getName());
        Assertions.assertEquals("Bob", testEntities.get(1).getName());
        Assertions.assertEquals("Charlie", testEntities.get(2).getName());
        resultSet.close();
        callableStatement.close();
    }
}
