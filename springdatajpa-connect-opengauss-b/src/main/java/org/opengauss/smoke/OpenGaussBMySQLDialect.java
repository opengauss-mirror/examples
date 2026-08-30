package org.opengauss.smoke;

import org.hibernate.dialect.MySQLDialect;

/**
 * openGauss B 兼容库方言（应用经 MySQL JDBC 驱动 + dolphin 协议连接）。
 *
 * openGauss B 模式解析器按标准 SQL 解释字符串字面量（无反斜杠转义，
 * '\\' 是两个字符），与 MySQL 不同，故在此声明"无反斜杠转义"语义。
 *
 * 已知兼容性问题：Hibernate 6.6 默认给 LIKE 谓词追加 escape '\\'
 * （两个反斜杠），B 模式解析器因 '\\' 是两字符而报 "invalid escape string"
 * （与驱动版本无关，8.0.28 与 9.7.0 表现一致）。该差异的根源是 B 模式不
 * 支持反斜杠作为字符串转义字符（与社区已记录的已知差异同源，参见 Plugin
 * 仓 issue #504）。实测 6.6.53 的 LIKE 渲染不读取本 isNoBackslashEscapesEnabled
 * 开关，规避方式是在 JPQL 中显式指定单字符转义符：like ?1 escape '!'
 * （见 UserRepository.searchByName）。
 */
public class OpenGaussBMySQLDialect extends MySQLDialect {

    @Override
    public boolean isNoBackslashEscapesEnabled() {
        return true;
    }
}
