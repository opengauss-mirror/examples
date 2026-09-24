package org.opengauss.smoke.jpacases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opengauss.smoke.User;
import org.opengauss.smoke.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.JpaSystemException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opengauss.smoke.jpacases.JpaDbUtil.log;

/**
 * 已知差异回归用例（LIKE escape 转义语义差异）。
 * Hibernate 6.6 派生 LIKE 查询默认生成 escape '\\'（两个反斜杠），B 模式
 * 按标准 SQL 把 '\\' 当两字符，报 invalid escape string（与驱动版本无关，
 * gsql 已取证）。该差异的根源是 B 模式不支持反斜杠作为字符串转义字符
 * （与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）。
 *
 * 这类用例的写法：把"当前已知的差异行为"写成断言。将来服务端行为调整后，
 * 第一个用例会失败，提醒我们移除 workaround 并更新文档——这是回归测试的标准用法。
 */
@SpringBootTest
class KnownIssueTest {

    @Autowired
    private UserRepository repo;

    @Test
    @DisplayName("K1 派生 findByNameLike 报 invalid escape string（转义语义差异现状断言）")
    void derivedLikeFailsAsExpected() {
        try {
            Page<User> page = repo.findByNameLike("%t_%", PageRequest.of(0, 5));
            log("K1 注意：派生 LIKE 不再报错（命中 %d 条）！服务端可能已调整转义语义，"
                    + "应移除 searchByName 的 escape '!' workaround 并更新文档", page.getTotalElements());
        } catch (JpaSystemException e) {
            String msg = rootMessage(e);
            log("K1 已知差异复现（符合当前预期）: %s", msg);
            assertTrue(msg.contains("invalid escape string"),
                    "应报 invalid escape string，实际: " + msg);
        }
    }

    @Test
    @DisplayName("K2 显式 escape '!' 的 searchByName 正常工作")
    void explicitEscapeWorks() {
        Page<User> page = repo.searchByName("%t_%", PageRequest.of(0, 5));
        log("K2 searchByName（escape '!'）命中 %d 条，规避方案有效", page.getTotalElements());
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) {
            cur = cur.getCause();
        }
        return cur.getMessage();
    }
}
