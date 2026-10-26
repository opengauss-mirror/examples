package org.opengauss.test;

import org.opengauss.dao.UserDao;
import org.opengauss.entity.User;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * CRUD 功能测试：增删改查、自增主键回填、模糊查询、分页、批量
 * 依赖 mysql_test_db 库中的 user 表（见 src/main/resources/init.sql）
 */
public class CrudTest extends BaseDbTest {

    private UserDao userDao;

    @Before
    public void setUp() {
        userDao = new UserDao(dataSource);
    }

    @Test
    public void testInsertBackfillAutoIncrementKey() throws Exception {
        User u = new User("crud_测试_" + System.currentTimeMillis(), 18);
        assertEquals(1, userDao.insert(u));
        assertNotNull("自增主键应回填", u.getId());
        User db = userDao.findById(u.getId());
        assertNotNull(db);
        assertEquals(u.getName(), db.getName());
        assertEquals(u.getAge(), db.getAge());
        userDao.delete(u.getId());
    }

    @Test
    public void testFindAll() throws Exception {
        List<User> all = userDao.findAll();
        assertTrue("初始化数据应至少有 3 行，实际: " + all.size(), all.size() >= 3);
    }

    @Test
    public void testFindByIdNotExistReturnsNull() throws Exception {
        assertNull(userDao.findById(-1));
    }

    @Test
    public void testFindByNameLike() throws Exception {
        List<User> like = userDao.findByNameLike("张");
        assertTrue("应能模糊查到一个以上用户，实际: " + like.size(), like.size() >= 1);
        like.forEach(u -> assertTrue(u.getName().contains("张")));
    }

    @Test
    public void testFindByPage() throws Exception {
        List<User> page = userDao.findByPage(2, 0);
        assertEquals("LIMIT 2 应返回 2 行", 2, page.size());
    }

    @Test
    public void testUpdate() throws Exception {
        User u = new User("crud_update_" + System.currentTimeMillis(), 20);
        userDao.insert(u);
        u.setAge(30);
        assertEquals(1, userDao.update(u));
        User db = userDao.findById(u.getId());
        assertEquals(Integer.valueOf(30), db.getAge());
        userDao.delete(u.getId());
    }

    @Test
    public void testDelete() throws Exception {
        User u = new User("crud_del_" + System.currentTimeMillis(), 20);
        userDao.insert(u);
        assertEquals(1, userDao.delete(u.getId()));
        assertNull(userDao.findById(u.getId()));
    }

    @Test
    public void testBatchInsert() throws Exception {
        List<User> users = Arrays.asList(
                new User("批_一号", 21),
                new User("批_二号", 22),
                new User("批_三号", 23));
        int[] rows = userDao.batchInsert(users);
        assertEquals("批量插入应返回每行影响行数", 3, rows.length);
        users.forEach(u -> assertNotNull("批量插入应回填主键", u.getId()));
        users.forEach(u -> {
            try {
                userDao.delete(u.getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
