package org.opengauss.batch;

import java.lang.reflect.Field;

import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * openGauss B 库分页读取器（FINDING B 规避）。
 *
 * <p>Spring Batch 的 {@link JdbcPagingItemReader#afterPropertiesSet()} 会调用
 * {@code jdbcTemplate.setMaxRows(pageSize)}；MySQL Connector/J 用会话变量
 * {@code SET sql_select_limit} 实现 Statement.setMaxRows，而 openGauss B 库
 * 不识别该变量（error 28804: unrecognized configuration parameter
 * "sql_select_limit"），导致分页读取不可用。</p>
 *
 * <p>规避：基类内部 JdbcTemplate 为私有字段，此处初始化完成后经反射把 JDBC 层
 * maxRows 归零——每页行数已由查询 SQL 自身的 {@code LIMIT} 限定，JDBC 层
 * maxRows 只是冗余保险，归零不影响分页语义。</p>
 */
public class OpenGaussPagingItemReader<T> extends JdbcPagingItemReader<T> {

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Field f = JdbcPagingItemReader.class.getDeclaredField("namedParameterJdbcTemplate");
        f.setAccessible(true);
        NamedParameterJdbcTemplate tpl = (NamedParameterJdbcTemplate) f.get(this);
        ((JdbcTemplate) tpl.getJdbcOperations()).setMaxRows(0);
    }
}
