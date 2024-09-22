package org.hibernate.dialect.integration;

import org.hibernate.*;
import org.hibernate.dialect.entity.lock.TestEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OpenGaussLockIntegrationTest {

    private static SessionFactory sessionFactory;

    @BeforeAll
    public static void init() {
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        session.createNativeQuery("DELETE FROM test_lock_entity").executeUpdate();
        TestEntity user1 = new TestEntity();
        user1.setId(1L);
        user1.setName("Test User 1");
        user1.setBalance(1000.0);
        session.save(user1);
        TestEntity user2 = new TestEntity();
        user2.setId(2L);
        user2.setName("Test User 2");
        user2.setBalance(1000.0);
        session.save(user2);
        session.getTransaction().commit();
        session.close();
    }

    @AfterAll
    public static void close() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }
    // testPessimisticWriteLockNoWait()：测试当记录已被锁定且设置不等待时，事务是否会立即失败，确保数据一致性和锁机制的有效性。
    @Test
    public void testPessimisticWriteLockNoWait() {
        Session session1 = sessionFactory.openSession();
        Transaction tx1 = session1.beginTransaction();
        TestEntity user1 = session1.get(TestEntity.class, 1L, new LockOptions(LockMode.PESSIMISTIC_WRITE));
        user1.setBalance(user1.getBalance() + 100.0);
        Session session2 = sessionFactory.openSession();
        Transaction tx2 = session2.beginTransaction();
        try {
            LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_WRITE);
            lockOptions.setTimeOut(LockOptions.NO_WAIT);
            TestEntity user2 = session2.get(TestEntity.class, 1L, lockOptions);
            user2.setBalance(user2.getBalance() - 50.0);
            tx2.commit();
        } catch (PessimisticLockException e) {
            System.out.println("无法获取锁：" + e.getMessage());
            tx2.rollback();
        } finally {
            session2.close();
        }
        tx1.commit();
        session1.close();
    }

    //testPessimisticReadLockSkipLocked()：测试在查询时跳过已被锁定的记录，验证 SKIP_LOCKED 的功能，确保在高并发环境下的查询性能。
    @Test
    public void testPessimisticReadLockSkipLocked() {
        Session session1 = sessionFactory.openSession();
        Transaction tx1 = session1.beginTransaction();
        TestEntity user1 = session1.get(TestEntity.class, 2L, new LockOptions(LockMode.PESSIMISTIC_WRITE));
        user1.setBalance(user1.getBalance() + 100.0);
        Session session2 = sessionFactory.openSession();
        Transaction tx2 = session2.beginTransaction();
        LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_READ);
        lockOptions.setTimeOut(LockOptions.SKIP_LOCKED);
        List<TestEntity> users = session2.createQuery("FROM TestEntity", TestEntity.class).setLockOptions(lockOptions).getResultList();
        for (TestEntity user : users) {
            System.out.println("读取到用户：" + user.getId());
            Assertions.assertNotEquals(2L, user.getId());
        }
        tx2.commit();
        session2.close();
        tx1.commit();
        session1.close();
    }
}