package org.opengauss;

import org.apache.commons.dbcp2.BasicDataSource;
import org.opengauss.dao.UserDao;
import org.opengauss.datasource.BasicDataSourceFactory;
import org.opengauss.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * DBCP 连接池连接 openGauss B 兼容模式数据库 —— 主示例程序。
 *
 * <p>运行前提：
 * <ol>
 *   <li>openGauss 已开启 MySQL 协议兼容（enable_dolphin_proto=on），dolphin 监听端口可用</li>
 *   <li>已创建 B 兼容库、业务用户并完成建表（见 src/main/resources/init.sql）</li>
 *   <li>src/main/resources/dbcp.properties 中连接信息正确</li>
 * </ol>
 *
 * <p>演示流程：建池 → 查询 → 新增（自增主键回填）→ 更新 → 模糊/分页查询 → 批量插入 → 删除 → 关池。
 */
public class DbcpConnectDemo {

    private static final Logger log = LoggerFactory.getLogger(DbcpConnectDemo.class);

    public static void main(String[] args) throws Exception {
        // 1. 创建 DBCP 连接池
        BasicDataSource dataSource = BasicDataSourceFactory.create();
        log.info("连接池创建成功, initialSize={}, maxTotal={}",
                dataSource.getInitialSize(), dataSource.getMaxTotal());

        UserDao userDao = new UserDao(dataSource);

        try {
            // 2. 查询：全表
            log.info("===== 全表查询 =====");
            userDao.findAll().forEach(u -> log.info("{}", u));

            // 3. 新增：自增主键回填
            log.info("===== 新增 =====");
            User newUser = new User("赵六", 21);
            int rows = userDao.insert(newUser);
            log.info("插入 {} 行, 回填主键 id={}", rows, newUser.getId());

            // 4. 按 id 查询
            User found = userDao.findById(newUser.getId());
            log.info("按 id={} 查询结果: {}", newUser.getId(), found);

            // 5. 更新
            log.info("===== 更新 =====");
            newUser.setAge(22);
            log.info("更新 {} 行", userDao.update(newUser));

            // 6. 模糊查询
            log.info("===== 模糊查询 LIKE '%张%' =====");
            userDao.findByNameLike("张").forEach(u -> log.info("{}", u));

            // 7. 分页查询
            log.info("===== 分页查询 LIMIT 2 OFFSET 1 =====");
            userDao.findByPage(2, 1).forEach(u -> log.info("{}", u));

            // 8. 批量插入
            log.info("===== 批量插入 =====");
            List<User> batch = Arrays.asList(new User("钱七", 23), new User("孙八", 24));
            int[] batchRows = userDao.batchInsert(batch);
            log.info("批量插入影响行数: {}", Arrays.toString(batchRows));
            batch.forEach(u -> log.info("回填主键: {}", u));

            // 9. 删除
            log.info("===== 删除 =====");
            log.info("删除 {} 行", userDao.delete(newUser.getId()));

            // 10. 最终数据
            log.info("===== 最终全表数据 =====");
            userDao.findAll().forEach(u -> log.info("{}", u));
        } finally {
            // 11. 关闭连接池（释放全部连接）
            dataSource.close();
            log.info("连接池已关闭");
        }
    }
}
