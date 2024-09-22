package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OpenGaussDialectDDLTest {
    private static OpenGaussDialect dialect;

    @BeforeAll
    public static void init() {
        dialect = new OpenGaussDialect();
    }

    // Catalog
    @Test
    public void testCanCreateCatalog() {
        assertFalse(dialect.canCreateCatalog());
    }

    @Test
    public void testGetCreateCatalogCommand() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            dialect.getCreateCatalogCommand("test_catalog");
        });
    }

    @Test
    public void testGetDropCatalogCommand() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            dialect.getDropCatalogCommand("test_catalog");
        });
    }

    // Schema
    @Test
    public void testCanCreateSchema() {
        Assertions.assertTrue(dialect.canCreateSchema());
    }

    @Test
    public void testGetCreateSchemaCommand() {
        String[] commands = dialect.getCreateSchemaCommand("test_schema");
        Assertions.assertEquals(1, commands.length);
        Assertions.assertEquals("create schema if not exists test_schema", commands[0]);
    }

    @Test
    public void testGetDropSchemaCommand() {
        String[] commands = dialect.getDropSchemaCommand("test_schema");
        Assertions.assertEquals(1, commands.length);
        Assertions.assertEquals("drop schema if exists test_schema", commands[0]);
    }

    @Test
    public void testGetCurrentSchemaCommand() {
        String command = dialect.getCurrentSchemaCommand();
        Assertions.assertEquals("select current_schema()", command);
    }

    // Table/Column
    @Test
    public void testGetCreateTableString() {
        String expected = "create table";
        Assertions.assertEquals(expected, dialect.getCreateTableString());
    }

    @Test
    public void testGetCreateMultisetTableString() {
        String expected = "create table";
        Assertions.assertEquals(expected, dialect.getCreateMultisetTableString());
    }

    @Test
    public void testGetTableTypeString() {
        String expected = "";
        Assertions.assertEquals(expected, dialect.getTableTypeString());
    }

    @Test
    public void testGetDropTableString() {
        String expected = "drop table if exists test_table cascade";
        Assertions.assertEquals(expected, dialect.getDropTableString("test_table"));
    }

    @Test
    public void testGetCascadeConstraintsString() {
        String expected = " cascade";
        Assertions.assertEquals(expected, dialect.getCascadeConstraintsString());
    }

    @Test
    public void testHasAlterTable() {
        Assertions.assertTrue(dialect.hasAlterTable());
    }

    @Test
    public void testSupportsIfExistsAfterAlterTable() {
        Assertions.assertTrue(dialect.supportsIfExistsAfterAlterTable());
    }

    @Test
    public void testGetAlterTableString() {
        String expected = "alter table if exists test_table";
        Assertions.assertEquals(expected, dialect.getAlterTableString("test_table"));
    }

    @Test
    public void testGetAddColumnString() {
        String expected = "add column";
        Assertions.assertEquals(expected, dialect.getAddColumnString());
    }

    @Test
    public void getAddColumnSuffixString() {
        String expected = "";
        Assertions.assertEquals(expected, dialect.getAddColumnSuffixString());
    }

    @Test
    public void testGetDefaultMultiTableBulkIdStrategy() {
        MultiTableBulkIdStrategy strategy = dialect.getDefaultMultiTableBulkIdStrategy();
        Assertions.assertNotNull(strategy);
        Assertions.assertInstanceOf(LocalTemporaryTableBulkIdStrategy.class, strategy);
    }

    // Constraint
    @Test
    public void testGetAddForeignKeyConstraintString1() {
        String constraintName = "fk_child_parent";
        String[] foreignKey = {"parent_id"};
        String referencedTable = "parent_table";
        String[] primaryKey = {"id"};
        boolean referencesPrimaryKey = true;
        String result = dialect.getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey, referencesPrimaryKey);
        String expected = " add constraint fk_child_parent foreign key (parent_id) references parent_table";
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testGetAddForeignKeyConstraintString2() {
        String constraintName = "fk_child_parent";
        String[] foreignKey = {"parent_id"};
        String referencedTable = "parent_table";
        String[] primaryKey = {"other_column"};
        boolean referencesPrimaryKey = false;
        String result = dialect.getAddForeignKeyConstraintString(constraintName, foreignKey, referencedTable, primaryKey, referencesPrimaryKey);
        String expected = " add constraint fk_child_parent foreign key (parent_id) references parent_table (other_column)";
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testSupportsCascadeDelete() {
        Assertions.assertTrue(dialect.supportsCascadeDelete());
    }

    @Test
    public void testDropConstraints() {
        Assertions.assertTrue(dialect.dropConstraints());
    }

    @Test
    public void testGetDropForeignKeyString() {
        String expected = " drop constraint ";
        Assertions.assertEquals(expected, dialect.getDropForeignKeyString());
    }

    @Test
    public void testHasSelfReferentialForeignKeyBug() {
        Assertions.assertFalse(dialect.hasSelfReferentialForeignKeyBug());
    }

    @Test
    public void testGetAddPrimaryKeyConstraintString() {
        String expected = " add constraint pk_table primary key ";
        Assertions.assertEquals(expected, dialect.getAddPrimaryKeyConstraintString("pk_table"));
    }

    @Test
    public void testSupportsIfExistsBeforeConstraintName() {
        Assertions.assertTrue(dialect.supportsIfExistsBeforeConstraintName());
    }

    @Test
    public void testSupportsIfExistsAfterConstraintName() {
        Assertions.assertFalse(dialect.supportsIfExistsAfterConstraintName());
    }

    @Test
    public void testSupportsColumnCheck() {
        Assertions.assertTrue(dialect.supportsColumnCheck());
    }

    @Test
    public void testSupportsTableCheck() {
        Assertions.assertTrue(dialect.supportsTableCheck());
    }

    // Comments
    @Test
    public void testSupportsCommentOn() {
        Assertions.assertTrue(dialect.supportsCommentOn());
    }

    @Test
    public void testGetTableComment() {
        Assertions.assertEquals("", dialect.getTableComment("This is a table comment"));
    }

    @Test
    public void testGetColumnComment() {
        Assertions.assertEquals("", dialect.getColumnComment("This is a column comment"));
    }
}
