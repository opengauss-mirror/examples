package org.opengauss.test;

import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Standard SQL category coverage through DBCP and the Dolphin MySQL protocol.
 *
 * Each test owns a uniquely named table. The DCL test also creates a temporary
 * role, verifies the privilege transition, and removes the role in finally so
 * repeated mvn test runs do not leave database objects behind.
 */
public class SqlCategoryTest extends BaseDbTest {

    /**
     * Executes CREATE, ALTER, TRUNCATE, and DROP through one pooled connection.
     * JDBC update counts and the inserted row prove that each schema transition
     * completed before the next statement; finally provides retry-safe cleanup.
     */
    @Test
    public void testDdlCreateAlterTruncateDrop() throws Exception {
        String table = tableName("ddl");
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            assertEquals(0, st.executeUpdate("CREATE TABLE " + table
                    + " (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL)"));
            assertEquals(0, st.executeUpdate("ALTER TABLE " + table
                    + " ADD COLUMN amount DECIMAL(10,2)"));
            assertEquals(1, st.executeUpdate("INSERT INTO " + table
                    + " (id, name, amount) VALUES (1, 'ddl-row', 10.50)"));
            assertEquals(0, st.executeUpdate("TRUNCATE TABLE " + table));
            assertEquals(0, st.executeUpdate("DROP TABLE " + table));
        } finally {
            dropTableQuietly(table);
        }
    }

    /**
     * Verifies INSERT, UPDATE, and DELETE by checking affected-row counts, then
     * reads the final state through a separate pooled connection so success is
     * not inferred only from the driver's return value.
     */
    @Test
    public void testDmlInsertUpdateDelete() throws Exception {
        String table = tableName("dml");
        try {
            createTable(table);
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                assertEquals(2, st.executeUpdate("INSERT INTO " + table
                        + " (id, name, amount) VALUES (1, 'one', 1.00), (2, 'two', 2.00)"));
                assertEquals(1, st.executeUpdate("UPDATE " + table
                        + " SET amount = 20.00, name = 'two-updated' WHERE id = 2"));
                assertEquals(1, st.executeUpdate("DELETE FROM " + table + " WHERE id = 1"));
            }
            assertEquals(1, countRows(table));
            assertEquals("two-updated", findName(table, 2));
        } finally {
            dropTableQuietly(table);
        }
    }

    /**
     * Covers parameterized filtering, ORDER BY, and COUNT aggregation. The test
     * asserts result order, cardinality, and end-of-result-set behavior to catch
     * both missing and unexpected rows.
     */
    @Test
    public void testDqlSelectFilteringOrderingAndAggregate() throws Exception {
        String table = tableName("dql");
        try {
            createTable(table);
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                st.executeUpdate("INSERT INTO " + table
                        + " (id, name, amount) VALUES (1, 'alpha', 3.00), (2, 'beta', 8.00), (3, 'beta-2', 5.00)");
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT name FROM " + table
                         + " WHERE amount >= ? ORDER BY amount DESC");
                 ResultSet rs = setAndQuery(ps, 5.00)) {
                assertTrue(rs.next());
                assertEquals("beta", rs.getString(1));
                assertTrue(rs.next());
                assertEquals("beta-2", rs.getString(1));
                assertFalse(rs.next());
            }
            assertEquals(3, countRows(table));
        } finally {
            dropTableQuietly(table);
        }
    }

    /**
     * Creates an isolated login role, grants SELECT, checks the effective table
     * privilege, revokes it, and checks again. The generated password and unique
     * role name prevent credentials or shared database state from entering tests.
     */
    @Test
    public void testDclGrantAndRevoke() throws Exception {
        String table = tableName("dcl");
        String role = "dbcp_sql_role_" + Long.toUnsignedString(System.nanoTime(), 36);
        String rolePassword = "Dbcp!Sql9" + Long.toString(System.nanoTime(), 36);
        try {
            createTable(table);
            try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
                // openGauss requires a password when a role is created through Dolphin.
                // Generate a test-only password at runtime; the role is dropped in finally below.
                st.executeUpdate("CREATE ROLE " + role + " LOGIN PASSWORD '" + rolePassword + "'");
                st.executeUpdate("GRANT SELECT ON TABLE " + table + " TO " + role);
                assertTrue(hasTablePrivilege(conn, role, table, "SELECT"));
                st.executeUpdate("REVOKE SELECT ON TABLE " + table + " FROM " + role);
                assertFalse(hasTablePrivilege(conn, role, table, "SELECT"));
            }
        } finally {
            // Drop the owned table first so a failure between GRANT and REVOKE
            // cannot leave dependencies that prevent the temporary role cleanup.
            dropTableQuietly(table);
            dropRoleQuietly(role);
        }
    }

    /**
     * Executes explicit START TRANSACTION/COMMIT and START TRANSACTION/ROLLBACK.
     * Follow-up reads use new pooled connections, proving committed data is
     * visible and rolled-back data is absent outside the original transaction.
     */
    @Test
    public void testTclCommitAndRollback() throws Exception {
        String table = tableName("tcl");
        try {
            createTable(table);
            String committed = "committed";
            try (Connection conn = dataSource.getConnection(); Statement tx = conn.createStatement()) {
                tx.execute("START TRANSACTION");
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table
                        + " (id, name, amount) VALUES (?, ?, ?)")) {
                    ps.setInt(1, 1);
                    ps.setString(2, committed);
                    ps.setBigDecimal(3, new BigDecimal("1.00"));
                    assertEquals(1, ps.executeUpdate());
                }
                tx.execute("COMMIT");
            }
            assertEquals(1, countRows(table));

            try (Connection conn = dataSource.getConnection(); Statement tx = conn.createStatement()) {
                tx.execute("START TRANSACTION");
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO " + table
                        + " (id, name, amount) VALUES (?, ?, ?)")) {
                    ps.setInt(1, 2);
                    ps.setString(2, "rolled-back");
                    ps.setBigDecimal(3, new BigDecimal("2.00"));
                    assertEquals(1, ps.executeUpdate());
                }
                tx.execute("ROLLBACK");
            }
            assertEquals(1, countRows(table));
            assertEquals("committed", findName(table, 1));
            assertNull(findName(table, 2));
        } finally {
            dropTableQuietly(table);
        }
    }

    private String tableName(String category) {
        return "dbcp_sql_" + category + "_" + Long.toUnsignedString(System.nanoTime(), 36);
    }

    private void createTable(String table) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE " + table
                    + " (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL, amount DECIMAL(10,2))");
        }
    }

    private int countRows(String table) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private String findName(String table, int id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT name FROM " + table + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private ResultSet setAndQuery(PreparedStatement ps, double amount) throws SQLException {
        ps.setDouble(1, amount);
        return ps.executeQuery();
    }

    private boolean hasTablePrivilege(Connection conn, String role, String table, String privilege)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT has_table_privilege(?, ?, ?)")) {
            ps.setString(1, role);
            ps.setString(2, table);
            ps.setString(3, privilege);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                return rs.getBoolean(1);
            }
        }
    }

    private void dropRoleQuietly(String role) {
        if (role == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("DROP ROLE " + role);
        } catch (SQLException ignored) {
            // Cleanup is best effort; preserve the original test failure.
        }
    }

    private void dropTableQuietly(String table) {
        if (table == null) {
            return;
        }
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS " + table);
        } catch (SQLException ignored) {
            // Cleanup is best effort; preserve the original test failure.
        }
    }
}
