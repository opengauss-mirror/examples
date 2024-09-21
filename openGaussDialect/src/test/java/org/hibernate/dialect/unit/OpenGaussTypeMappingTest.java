package org.hibernate.dialect.unit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.entity.datatype.*;
import org.hibernate.dialect.pojo.Person;
import org.hibernate.dialect.util.HibernateUtil;
import org.hibernate.dialect.util.JDBCUtil;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class OpenGaussTypeMappingTest {
    private static SessionFactory sessionFactory;

    public static void formatColumns(List<String[]> columns) {
        int maxColumnLength = columns.stream().mapToInt(c -> c[0].length()).max().orElse(0);
        int maxTypeLength = columns.stream().mapToInt(c -> c[1].length()).max().orElse(0);
        for (String[] column : columns) {
            System.out.printf("Column %-" + maxColumnLength + "s : %" + maxTypeLength + "s%n", column[0], column[1]);
        }
    }

    public static void ColumnFormatter(String tableName) {
        List<String[]> columns = null;
        try {
            Connection conn = JDBCUtil.getConnection();
            String sql = "SELECT * FROM " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            columns = new ArrayList<>();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String columnName = rsmd.getColumnName(i);
                String columnType = rsmd.getColumnTypeName(i);
                columns.add(new String[]{columnName, columnType});
            }
            JDBCUtil.disconnect(conn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        formatColumns(columns);
    }

    @Test
    public void testBooleanMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(BooleanEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testNumberMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(NumberEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testCharMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(CharEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testStringMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(StringEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testDateTimeMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(DateTimeEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testBinaryMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(BinaryEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testLobMapping() {
        sessionFactory = HibernateUtil.getSessionFactory(LobEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void testInsertAndRetrieveJsonData() {
        sessionFactory = HibernateUtil.getSessionFactory(JsonEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        JsonEntity entity = new JsonEntity();
        entity.setId(1L);
        entity.setPojoJson(new Person(1, "opengauss"));
        entity.setPojoJsonb(new Person(2, "opengauss"));
        session.save(entity);
        session.getTransaction().commit();
        JsonEntity retrievedEntity = session.get(JsonEntity.class, entity.getId());
        System.out.println(retrievedEntity);
        session.close();
    }

    @Test
    public void testSupportsNationalizedTypes() {
        sessionFactory = HibernateUtil.getSessionFactory(NationalizedEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.getTransaction().commit();
        session.close();
    }
}
