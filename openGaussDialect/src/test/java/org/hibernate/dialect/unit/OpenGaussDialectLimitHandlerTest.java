package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.engine.spi.RowSelection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OpenGaussDialectLimitHandlerTest {
    private OpenGaussDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new OpenGaussDialect();
    }

    @Test
    public void testProcessSqlWithOffset() {
        LimitHandler limitHandler = dialect.getLimitHandler();
        String originalSql = "SELECT * FROM test_table";
        RowSelection selection = new RowSelection();
        selection.setFirstRow(10); // offset
        selection.setMaxRows(20);  // limit
        String processedSql = limitHandler.processSql(originalSql, selection);
        String expectedSql = "SELECT * FROM test_table limit ? offset ?";
        Assertions.assertEquals(expectedSql, processedSql);
    }

    @Test
    public void testProcessSqlWithoutOffset() {
        LimitHandler limitHandler = dialect.getLimitHandler();
        String originalSql = "SELECT * FROM test_table";
        RowSelection selection = new RowSelection();
        selection.setMaxRows(20);  // limit
        String processedSql = limitHandler.processSql(originalSql, selection);
        String expectedSql = "SELECT * FROM test_table limit ?";
        Assertions.assertEquals(expectedSql, processedSql);
    }

    @Test
    public void testSupportsLimit() {
        LimitHandler limitHandler = dialect.getLimitHandler();
        Assertions.assertTrue(limitHandler.supportsLimit());
    }
}
