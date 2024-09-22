package org.hibernate.dialect.unit;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.OpenGaussDialect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OpenGaussDialectLockTest {
    private static OpenGaussDialect dialect;

    @BeforeAll
    public static void setUp() {
        dialect = new OpenGaussDialect();
    }

    @Test
    public void testSupportsLockTimeouts() {
        Assertions.assertTrue(dialect.supportsLockTimeouts());
    }

    @Test
    public void testIsLockTimeoutParameterized() {
        Assertions.assertFalse(dialect.isLockTimeoutParameterized());
    }

    @Test
    public void testGetWriteLockString() {
        String sql = dialect.getWriteLockString(LockOptions.WAIT_FOREVER);
        Assertions.assertEquals(" for update", sql);
        sql = dialect.getWriteLockString(LockOptions.NO_WAIT);
        Assertions.assertEquals(" for update nowait", sql);
        sql = dialect.getWriteLockString(LockOptions.SKIP_LOCKED);
        Assertions.assertEquals(" for update skip locked", sql);
    }

    @Test
    public void testGetWriteLockStringWithAliases() {
        String aliases = "t1";
        String sql = dialect.getWriteLockString(aliases, LockOptions.WAIT_FOREVER);
        Assertions.assertEquals(" for update of t1", sql);
        sql = dialect.getWriteLockString(aliases, LockOptions.NO_WAIT);
        Assertions.assertEquals(" for update of t1 nowait", sql);
        sql = dialect.getWriteLockString(aliases, LockOptions.SKIP_LOCKED);
        Assertions.assertEquals(" for update of t1 skip locked", sql);
    }

    @Test
    public void testGetReadLockString() {
        String sql = dialect.getReadLockString(LockOptions.WAIT_FOREVER);
        Assertions.assertEquals(" for share", sql);
        sql = dialect.getReadLockString(LockOptions.NO_WAIT);
        Assertions.assertEquals(" for share nowait", sql);
        sql = dialect.getReadLockString(LockOptions.SKIP_LOCKED);
        Assertions.assertEquals(" for share skip locked", sql);
    }

    @Test
    public void testGetReadLockStringWithAliases() {
        String aliases = "t1";
        String sql = dialect.getReadLockString(aliases, LockOptions.WAIT_FOREVER);
        Assertions.assertEquals(" for share of t1", sql);
        sql = dialect.getReadLockString(aliases, LockOptions.NO_WAIT);
        Assertions.assertEquals(" for share of t1 nowait", sql);
        sql = dialect.getReadLockString(aliases, LockOptions.SKIP_LOCKED);
        Assertions.assertEquals(" for share of t1 skip locked", sql);
    }

    @Test
    public void testGetForUpdateStringWithAliases() {
        String aliases = "t1";
        String sql = dialect.getForUpdateString(aliases);
        Assertions.assertEquals(" for update of t1", sql);
    }

    @Test
    public void testGetForUpdateStringWithAliasesAndLockOptions() {
        String aliases = "t1";
        LockOptions lockOptions = new LockOptions(LockMode.PESSIMISTIC_WRITE);
        lockOptions.setTimeOut(LockOptions.WAIT_FOREVER);
        String sql = dialect.getForUpdateString(aliases, lockOptions);
        Assertions.assertEquals(" for update of t1", sql);
        lockOptions.setTimeOut(LockOptions.NO_WAIT);
        sql = dialect.getForUpdateString(aliases, lockOptions);
        Assertions.assertEquals(" for update of t1 nowait", sql);
        lockOptions.setTimeOut(LockOptions.SKIP_LOCKED);
        sql = dialect.getForUpdateString(aliases, lockOptions);
        Assertions.assertEquals(" for update of t1 skip locked", sql);
    }

    @Test
    public void testGetForUpdateNowaitString() {
        String sql = dialect.getForUpdateNowaitString();
        Assertions.assertEquals(" for update nowait", sql);
    }

    @Test
    public void testGetForUpdateNowaitStringWithAliases() {
        String aliases = "t1";
        String sql = dialect.getForUpdateNowaitString(aliases);
        Assertions.assertEquals(" for update of t1 nowait", sql);
    }

    @Test
    public void testGetForUpdateSkipLockedString() {
        String sql = dialect.getForUpdateSkipLockedString();
        Assertions.assertEquals(" for update skip locked", sql);
    }

    @Test
    public void testGetForUpdateSkipLockedStringWithAliases() {
        String aliases = "t1";
        String sql = dialect.getForUpdateSkipLockedString(aliases);
        Assertions.assertEquals(" for update of t1 skip locked", sql);
    }

    @Test
    public void testSupportsOuterJoinForUpdate() {
        Assertions.assertTrue(dialect.supportsOuterJoinForUpdate());
    }

    @Test
    public void testForUpdateOfColumns() {
        Assertions.assertTrue(dialect.forUpdateOfColumns());
    }
}
