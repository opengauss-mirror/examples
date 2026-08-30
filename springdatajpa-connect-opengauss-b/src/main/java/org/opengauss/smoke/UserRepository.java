package org.opengauss.smoke;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** JpaRepository 自带 CRUD 与 findAll(Pageable)（分页生成 LIMIT/OFFSET）。 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 派生 LIKE 查询：Hibernate 6.6 默认在所有 LIKE 谓词后追加 escape '\\'
     * （两个反斜杠），B 模式按标准 SQL 把 '\\' 视为 2 个字符，报
     * invalid escape string（转义语义差异；根源是 B 模式不支持反斜杠作为字符串
     * 转义字符，与社区已记录的已知差异同源，参见 Plugin 仓 issue #504）。
     * 保留此方法作为"已知差异"回归用例。
     */
    Page<User> findByNameLike(String name, Pageable pageable);

    /**
     * 显式指定非反斜杠转义符（!）的 LIKE 查询，规避上述问题。
     */
    @Query("select u from User u where u.name like ?1 escape '!'")
    Page<User> searchByName(String pattern, Pageable pageable);
}
