package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.OpenGaussIdentityColumnSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Types;

public class OpenGaussIdentityColumnSupportTest {
    private static OpenGaussDialect dialect;

    @BeforeAll
    public static void setUp() {
        dialect = new OpenGaussDialect();
    }

    @Test
    public void testGetNativeIdentifierGeneratorStrategy() {
        String generatorStrategy = dialect.getNativeIdentifierGeneratorStrategy();
        Assertions.assertEquals("sequence", generatorStrategy, "Native identifier generator strategy should be 'sequence'");
    }

    @Test
    public void testSupportsIdentityColumns() {
        IdentityColumnSupport identitySupport = new OpenGaussIdentityColumnSupport();
        Assertions.assertFalse(identitySupport.supportsIdentityColumns(), "Identity columns should not be supported");
    }

    @Test
    public void testGetIdentitySelectString() {
        IdentityColumnSupport identitySupport = new OpenGaussIdentityColumnSupport();
        String selectString = identitySupport.getIdentitySelectString("test_table", "id", Types.BIGINT);
        Assertions.assertEquals("select currval('test_table_id_seq')", selectString, "Identity select string is incorrect");
    }

    @Test
    public void testGetIdentityColumnString() {
        IdentityColumnSupport identitySupport = new OpenGaussIdentityColumnSupport();
        String bigIntIdentityColumnString = identitySupport.getIdentityColumnString(Types.BIGINT);
        Assertions.assertEquals("bigserial not null", bigIntIdentityColumnString, "BIGINT identity column string is incorrect");
        String defaultIdentityColumnString = identitySupport.getIdentityColumnString(Types.INTEGER);
        Assertions.assertEquals("serial not null", defaultIdentityColumnString, "Default identity column string is incorrect");
    }

    @Test
    public void testHasDataTypeInIdentityColumn() {
        IdentityColumnSupport identitySupport = new OpenGaussIdentityColumnSupport();
        Assertions.assertFalse(identitySupport.hasDataTypeInIdentityColumn(), "Identity column should not have data type included");
    }
}
