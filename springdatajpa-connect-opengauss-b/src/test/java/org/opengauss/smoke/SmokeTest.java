package org.opengauss.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** JPA 冒烟用例：需真实 openGauss B 库环境，与 driver profile 配合做版本扫测。 */
@SpringBootTest
class SmokeTest {

    @Autowired
    private UserRepository repo;

    @Autowired
    private TxService txService;

    @Test
    void saveAndFind() {
        User saved = repo.save(new User("t_jpa", 1));
        // IDENTITY 主键回填
        assertNotNull(saved.getId(), "save 后应回填自增主键");

        User found = repo.findById(saved.getId()).orElseThrow();
        assertEquals("t_jpa", found.getName());
        repo.delete(saved);
    }

    @Test
    void pagination() {
        Page<User> page = repo.findAll(PageRequest.of(0, 2));
        assertEquals(2, page.getContent().size(), "每页应返回 2 条（LIMIT 2）");
        repo.searchByName("%t_%", PageRequest.of(0, 5));
    }

    @Test
    void transactionRollback() {
        try {
            txService.saveThenThrow("t_tx", 1);
        } catch (RuntimeException expected) {
            // 预期异常：触发回滚
        }
        long count = repo.count();
        repo.findAll().forEach(u -> {
            if ("t_tx".equals(u.getName())) {
                throw new AssertionError("事务应已回滚，不应存在 t_tx 记录");
            }
        });
    }
}
