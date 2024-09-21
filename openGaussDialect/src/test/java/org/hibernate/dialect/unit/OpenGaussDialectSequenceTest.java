package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

public class OpenGaussDialectSequenceTest {
    private OpenGaussDialect dialect;

    @BeforeEach
    public void setUp() {
        dialect = new OpenGaussDialect();
    }
    
    @Test
    public void testSupportsSequences() {
        Assertions.assertTrue(dialect.supportsSequences());
    }

    @Test
    public void testSupportsPooledSequences() {
        Assertions.assertTrue(dialect.supportsPooledSequences());
    }

    @Test
    public void testGetSequenceNextValString() {
        String sequenceName = "my_sequence";
        String expected = "select nextval('my_sequence')";
        String actual = dialect.getSequenceNextValString(sequenceName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetSelectSequenceNextValString() {
        String sequenceName = "my_sequence";
        String expected = "nextval('my_sequence')";
        String actual = dialect.getSelectSequenceNextValString(sequenceName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetCreateSequenceString() {
        String sequenceName = "my_sequence";
        String expected = "create sequence my_sequence";
        String actual = dialect.getCreateSequenceString(sequenceName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetCreateSequenceStringWithParameters() throws Exception{
        Method method = OpenGaussDialect.class.getDeclaredMethod("getCreateSequenceString", String.class, int.class, int.class);
        method.setAccessible(true);
        String sequenceName = "my_sequence";

        // Test with positive initialValue and incrementSize
        String expected = "create sequence my_sequence start 1 increment 1";
        String actual = (String) method.invoke(dialect, sequenceName, 1, 1);
        Assertions.assertEquals(expected, actual);

        // Test with negative initialValue and positive incrementSize
        expected = "create sequence my_sequence minvalue -5 start -5 increment 2";
        actual = (String) method.invoke(dialect, sequenceName, -5, 2);
        Assertions.assertEquals(expected, actual);

        // Test with positive initialValue and negative incrementSize
        expected = "create sequence my_sequence maxvalue 10 start 10 increment -1";
        actual = (String) method.invoke(dialect, sequenceName, 10, -1);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetDropSequenceString() {
        String sequenceName = "my_sequence";
        String expected = "drop sequence if exists my_sequence";
        String actual = dialect.getDropSequenceString(sequenceName);
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetQuerySequencesString() {
        String expected = "select * from information_schema.sequences";
        String actual = dialect.getQuerySequencesString();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetNativeIdentifierGeneratorStrategy() {
        String expected = "sequence";
        String actual = dialect.getNativeIdentifierGeneratorStrategy();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testGetIdentityColumnSupport() {
        Assertions.assertNotNull(dialect.getIdentityColumnSupport());
    }
}
