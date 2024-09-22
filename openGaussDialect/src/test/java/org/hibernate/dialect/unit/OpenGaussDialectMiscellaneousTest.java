package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.sql.ANSICaseFragment;
import org.hibernate.sql.ANSIJoinFragment;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.JoinFragment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenGaussDialectMiscellaneousTest {

    private static OpenGaussDialect dialect;

    @BeforeAll
    public static void init() {
        dialect = new OpenGaussDialect();
    }

    @Test
    public void testCreateOuterJoinFragment() {
        JoinFragment joinFragment = dialect.createOuterJoinFragment();
        Assertions.assertNotNull(joinFragment);
        Assertions.assertInstanceOf(ANSIJoinFragment.class, joinFragment);
    }

    @Test
    public void testCreateCaseFragment() {
        CaseFragment caseFragment = dialect.createCaseFragment();
        Assertions.assertNotNull(caseFragment);
        Assertions.assertInstanceOf(ANSICaseFragment.class, caseFragment);
    }

    @Test
    public void testGetNoColumnsInsertString() {
        String insertString = dialect.getNoColumnsInsertString();
        Assertions.assertEquals("default values", insertString);
    }

    @Test
    public void testSupportsNoColumnsInsert() {
        Assertions.assertTrue(dialect.supportsNoColumnsInsert());
    }

    @Test
    public void testGetLowercaseFunction() {
        String functionName = dialect.getLowercaseFunction();
        Assertions.assertEquals("lower", functionName);
    }

    @Test
    public void testGetCaseInsensitiveLike() {
        String operator = dialect.getCaseInsensitiveLike();
        Assertions.assertEquals("ilike", operator);
    }

    @Test
    public void testSupportsCaseInsensitiveLike() {
        Assertions.assertTrue(dialect.supportsCaseInsensitiveLike());
    }

    @Test
    public void testTransformSelectString() {
        String select = "SELECT * FROM test_table";
        String transformedSelect = dialect.transformSelectString(select);
        Assertions.assertEquals(select, transformedSelect);
    }

    @Test
    public void testGetMaxAliasLength() {
        int maxAliasLength = dialect.getMaxAliasLength();
        Assertions.assertEquals(10, maxAliasLength);
    }

    @Test
    public void testToBooleanValueString() {
        String trueValue = dialect.toBooleanValueString(true);
        String falseValue = dialect.toBooleanValueString(false);
        Assertions.assertEquals("t", trueValue);
        Assertions.assertEquals("f", falseValue);
    }
}
