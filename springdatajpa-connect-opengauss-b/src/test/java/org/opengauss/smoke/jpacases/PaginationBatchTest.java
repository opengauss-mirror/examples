package org.opengauss.smoke.jpacases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.User;
import org.opengauss.smoke.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.smoke.jpacases.JpaDbUtil.log;

/**
 * 分页（LIMIT/OFFSET 生成）与批量保存。
 * 清理用 searchByName（escape '!'），刻意避开转义语义差异（#504）的派生 LIKE。
 */
@SpringBootTest
class PaginationBatchTest {

    @Autowired
    private UserRepository repo;

    @Test
    @DisplayName("P1 分页：多页遍历（Hibernate 生成 LIMIT/OFFSET + count）")
    void pagination() {
        long total = repo.count();
        assertTrue(total >= 3, "user 表至少应有 3 条初始数据");

        int size = 2;
        long pages = (total + size - 1) / size;
        int seen = 0;
        for (int i = 0; i < pages; i++) {
            Page<User> page = repo.findAll(PageRequest.of(i, size));
            seen += page.getContent().size();
        }
        assertEquals(total, seen, "逐页遍历应覆盖全部记录");
        log("P1 分页遍历 OK：共 %d 条 / 页大小 2 / %d 页（LIMIT/OFFSET 与 count 查询均正常）", total, pages);
    }

    @Test
    @DisplayName("P2 saveAll 批量保存 50 条（含 IDENTITY 逐个回填）")
    void saveAll() {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            users.add(new User("t_jpa_batch_" + i, i));
        }
        long before = repo.count();
        repo.saveAll(users);
        long after = repo.count();
        assertEquals(before + 50, after, "saveAll 应新增 50 条");

        // 抽查回填的主键均非空
        for (User u : users) {
            assertTrue(u.getId() != null, "批量保存后主键应回填");
        }
        log("P2 saveAll(50) 成功：count %d -> %d，主键全部回填", before, after);

        // 清理：用显式 escape '!' 的查询（避开转义语义差异）
        List<User> toDelete = repo.searchByName("%t_jpa_batch_%", PageRequest.of(0, 100)).getContent();
        repo.deleteAll(toDelete);
        assertEquals(before, repo.count(), "清理后应恢复原行数");
        log("P2 清理完成，恢复 %d 行", before);
    }
}
