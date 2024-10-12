package org.hibernate.dialect;

import org.hibernate.*;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.*;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.OpenGaussIdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.*;

public class OpenGaussDialect extends Dialect {
    public OpenGaussDialect() {
        super();
        registerColumnType(Types.BIT, "bit");
        registerColumnType(Types.BOOLEAN, "bool");

        registerColumnType(Types.TINYINT, "int1");
        registerColumnType(Types.SMALLINT, "int2");
        registerColumnType(Types.INTEGER, "int4");
        registerColumnType(Types.BIGINT, "int8");
        registerColumnType(Types.REAL, "float4");
        registerColumnType(Types.FLOAT, "float8");
        registerColumnType(Types.DOUBLE, "float8");
        registerColumnType(Types.DECIMAL, "decimal($p,$s)");
        registerColumnType(Types.NUMERIC, "numeric($p,$s)");

        registerColumnType(Types.CHAR, "char(1)");

        registerColumnType(Types.VARCHAR, "varchar($l)");
        registerColumnType(Types.LONGVARCHAR, "text");
        registerColumnType(Types.NCHAR, "nchar($l)");
        registerColumnType(Types.NVARCHAR, "nvarchar2($l)");

        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.TIME_WITH_TIMEZONE, "timetz");
        registerColumnType(Types.TIMESTAMP_WITH_TIMEZONE, "timestamptz");

        registerColumnType(Types.BINARY, "bytea");
        registerColumnType(Types.VARBINARY, "bytea");
        registerColumnType(Types.LONGVARBINARY, "bytea");


        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CLOB, "clob");
        registerColumnType(Types.NCLOB, "text");

        registerColumnType(Types.JAVA_OBJECT, "json");
        registerColumnType(Types.SQLXML, "xml");
        registerColumnType(Types.OTHER, "uuid");

        registerOpenGaussFunctions();

        getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        getDefaultProperties().setProperty(Environment.NON_CONTEXTUAL_LOB_CREATION, "false");
    }

    protected void registerOpenGaussFunctions() {
        // https://docs-opengauss.osinfra.cn/zh/docs/5.0.0-lite/docs/BriefTutorial/%E5%87%BD%E6%95%B0.html
        // mathematical functions
        registerFunction("abs", new StandardSQLFunction("abs", StandardBasicTypes.DOUBLE));
        registerFunction("cbrt", new StandardSQLFunction("cbrt", StandardBasicTypes.DOUBLE));
        registerFunction("ceil", new StandardSQLFunction("ceil", StandardBasicTypes.DOUBLE));
        registerFunction("degrees", new StandardSQLFunction("degrees", StandardBasicTypes.DOUBLE));
        registerFunction("exp", new StandardSQLFunction("exp", StandardBasicTypes.DOUBLE));
        registerFunction("floor", new StandardSQLFunction("floor", StandardBasicTypes.DOUBLE));
        registerFunction("ln", new StandardSQLFunction("ln", StandardBasicTypes.DOUBLE));
        registerFunction("log", new StandardSQLFunction("log", StandardBasicTypes.DOUBLE));
        registerFunction("mod", new StandardSQLFunction("mod", StandardBasicTypes.INTEGER));
        registerFunction("pi", new NoArgSQLFunction("pi", StandardBasicTypes.DOUBLE, true));
        registerFunction("power", new StandardSQLFunction("power", StandardBasicTypes.DOUBLE));
        registerFunction("radians", new StandardSQLFunction("radians", StandardBasicTypes.DOUBLE));
        registerFunction("random", new NoArgSQLFunction("random", StandardBasicTypes.DOUBLE));
        registerFunction("round", new StandardSQLFunction("round", StandardBasicTypes.DOUBLE));
        registerFunction("sign", new StandardSQLFunction("sign", StandardBasicTypes.INTEGER));
        registerFunction("sqrt", new StandardSQLFunction("sqrt", StandardBasicTypes.DOUBLE));
        registerFunction("trunc", new StandardSQLFunction("trunc", StandardBasicTypes.DOUBLE));

        // trigonometric functions
        registerFunction("acos", new StandardSQLFunction("acos", StandardBasicTypes.DOUBLE));
        registerFunction("asin", new StandardSQLFunction("asin", StandardBasicTypes.DOUBLE));
        registerFunction("atan", new StandardSQLFunction("atan", StandardBasicTypes.DOUBLE));
        registerFunction("atan2", new StandardSQLFunction("atan2", StandardBasicTypes.DOUBLE));
        registerFunction("cos", new StandardSQLFunction("cos", StandardBasicTypes.DOUBLE));
        registerFunction("cot", new StandardSQLFunction("cot", StandardBasicTypes.DOUBLE));
        registerFunction("sin", new StandardSQLFunction("sin", StandardBasicTypes.DOUBLE));
        registerFunction("tan", new StandardSQLFunction("tan", StandardBasicTypes.DOUBLE));

        // string functions
        registerFunction("concat", new VarArgsSQLFunction(StandardBasicTypes.STRING, "", "||", ""));
        registerFunction("bit_length", new StandardSQLFunction("bit_length", StandardBasicTypes.LONG));
        registerFunction("convert", new SQLFunctionTemplate(StandardBasicTypes.BINARY, "convert(?1, ?2, ?3)"));
        registerFunction("lower", new StandardSQLFunction("lower", StandardBasicTypes.STRING));
        registerFunction("octet_length", new StandardSQLFunction("octet_length", StandardBasicTypes.LONG));
        registerFunction("overlay", new SQLFunctionTemplate(StandardBasicTypes.STRING, "overlay(?1 placing ?2 from ?3 for ?4)"));
        registerFunction("position", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "position(?1 in ?2)"));
        registerFunction("substring", new SubstringFunction());
        registerFunction("trim", new SQLFunctionTemplate(StandardBasicTypes.STRING, "trim(?1 ?2 ?3 ?4)"));
        registerFunction("upper", new StandardSQLFunction("upper", StandardBasicTypes.STRING));
        registerFunction("ascii", new StandardSQLFunction("ascii", StandardBasicTypes.INTEGER));
        registerFunction("btrim", new StandardSQLFunction("btrim", StandardBasicTypes.STRING));
        registerFunction("chr", new StandardSQLFunction("chr", StandardBasicTypes.CHARACTER));
        registerFunction("initcap", new StandardSQLFunction("initcap"));
        registerFunction("length", new StandardSQLFunction("length", StandardBasicTypes.INTEGER));
        registerFunction("lpad", new StandardSQLFunction("lpad", StandardBasicTypes.STRING));
        registerFunction("ltrim", new StandardSQLFunction("ltrim", StandardBasicTypes.STRING));
        registerFunction("md5", new StandardSQLFunction("md5", StandardBasicTypes.STRING));
        registerFunction("repeat", new StandardSQLFunction("repeat", StandardBasicTypes.STRING));
        registerFunction("replace", new StandardSQLFunction("replace", StandardBasicTypes.STRING));
        registerFunction("rpad", new StandardSQLFunction("rpad", StandardBasicTypes.STRING));
        registerFunction("rtrim", new StandardSQLFunction("rtrim", StandardBasicTypes.STRING));
        registerFunction("split_part", new StandardSQLFunction("split_part", StandardBasicTypes.STRING));
        registerFunction("strpos", new StandardSQLFunction("strpos", StandardBasicTypes.INTEGER));
        registerFunction("to_hex", new StandardSQLFunction("to_hex", StandardBasicTypes.STRING));
        registerFunction("translate", new StandardSQLFunction("translate", StandardBasicTypes.STRING));

        // type conversion related functions
        registerFunction("to_char", new StandardSQLFunction("to_char", StandardBasicTypes.STRING));
        registerFunction("to_date", new StandardSQLFunction("to_date", StandardBasicTypes.DATE));
        registerFunction("to_number", new StandardSQLFunction("to_number", StandardBasicTypes.BIG_DECIMAL));
        registerFunction("to_timestamp", new StandardSQLFunction("to_timestamp", StandardBasicTypes.TIMESTAMP));

        // other function
        registerFunction("stddev", new StandardSQLFunction("stddev", StandardBasicTypes.DOUBLE));
        registerFunction("variance", new StandardSQLFunction("variance", StandardBasicTypes.DOUBLE));
        registerFunction("rand", new NoArgSQLFunction("random", StandardBasicTypes.DOUBLE));
        registerFunction("to_ascii", new StandardSQLFunction("to_ascii"));
        registerFunction("quote_ident", new StandardSQLFunction("quote_ident", StandardBasicTypes.STRING));
        registerFunction("quote_literal", new StandardSQLFunction("quote_literal", StandardBasicTypes.STRING));
        registerFunction("age", new StandardSQLFunction("age"));
        registerFunction("current_date", new NoArgSQLFunction("current_date", StandardBasicTypes.DATE, false));
        registerFunction("current_time", new NoArgSQLFunction("current_time", StandardBasicTypes.TIME, false));
        registerFunction("current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false));
        registerFunction("date_trunc", new StandardSQLFunction("date_trunc", StandardBasicTypes.TIMESTAMP));
        registerFunction("localtime", new NoArgSQLFunction("localtime", StandardBasicTypes.TIME, false));
        registerFunction("localtimestamp", new NoArgSQLFunction("localtimestamp", StandardBasicTypes.TIMESTAMP, false));
        registerFunction("now", new NoArgSQLFunction("now", StandardBasicTypes.TIMESTAMP));
        registerFunction("timeofday", new NoArgSQLFunction("timeofday", StandardBasicTypes.STRING));
        registerFunction("current_user", new NoArgSQLFunction("current_user", StandardBasicTypes.STRING, false));
        registerFunction("session_user", new NoArgSQLFunction("session_user", StandardBasicTypes.STRING, false));
        registerFunction("user", new NoArgSQLFunction("user", StandardBasicTypes.STRING, false));
        registerFunction("current_database", new NoArgSQLFunction("current_database", StandardBasicTypes.STRING, true));
        registerFunction("current_schema", new NoArgSQLFunction("current_schema", StandardBasicTypes.STRING, true));
        registerFunction("locate", new PositionSubstringFunction());
        registerFunction("str", new SQLFunctionTemplate(StandardBasicTypes.STRING, "cast(?1 as varchar)"));
    }

    // database type mapping support
    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        SqlTypeDescriptor descriptor;
        switch (sqlCode) {
            case Types.BLOB: {
                descriptor = BlobTypeDescriptor.BLOB_BINDING;
                break;
            }
            case Types.CLOB: {
                descriptor = ClobTypeDescriptor.CLOB_BINDING;
                break;
            }
            default: {
                descriptor = super.getSqlTypeDescriptorOverride(sqlCode);
                break;
            }
        }
        return descriptor;
    }

    // hibernate type mapping support
    // extend...

    // function support
    // extend...

    // native identifier generation
    @Override
    public String getNativeIdentifierGeneratorStrategy() {
        return "sequence";
    }

    // IDENTITY support
    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new OpenGaussIdentityColumnSupport();
    }

    // SEQUENCE support
    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsPooledSequences() {
        return true;
    }

    @Override
    public String getSequenceNextValString(String sequenceName) {
        return "select " + getSelectSequenceNextValString(sequenceName);
    }

    @Override
    public String getSelectSequenceNextValString(String sequenceName) {
        return "nextval('" + sequenceName + "')";
    }

    @Override
    public String getCreateSequenceString(String sequenceName) {
        return "create sequence " + sequenceName;
    }

    @Override
    protected String getCreateSequenceString(String sequenceName, int initialValue, int incrementSize) {
        if (initialValue < 0 && incrementSize > 0) {
            return String.format("%s minvalue %d start %d increment %d", getCreateSequenceString(sequenceName), initialValue, initialValue, incrementSize);
        } else if (initialValue > 0 && incrementSize < 0) {
            return String.format("%s maxvalue %d start %d increment %d", getCreateSequenceString(sequenceName), initialValue, initialValue, incrementSize);
        } else {
            return String.format("%s start %d increment %d", getCreateSequenceString(sequenceName), initialValue, incrementSize);
        }
    }

    @Override
    public String getDropSequenceString(String sequenceName) {
        return "drop sequence if exists " + sequenceName;
    }

    @Override
    public String getQuerySequencesString() {
        return "select * from information_schema.sequences";
    }

    // GUID support
    // extend...

    // limit/offset support
    private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
        @Override
        public String processSql(String sql, RowSelection selection) {
            final boolean hasOffset = LimitHelper.hasFirstRow(selection);
            return sql + (hasOffset ? " limit ? offset ?" : " limit ?");
        }

        @Override
        public boolean supportsLimit() {
            return true;
        }

        @Override
        public boolean bindLimitParametersInReverseOrder() {
            return true;
        }
    };

    @Override
    public LimitHandler getLimitHandler() {
        return LIMIT_HANDLER;
    }

    // lock acquisition support
    @Override
    public String getForUpdateString(String aliases) {
         return getForUpdateString() + " of " + aliases;
    }

    @Override
    public String getForUpdateString(String aliases, LockOptions lockOptions) {
        LockMode lockMode = lockOptions.getLockMode();
        if (aliases != null && !aliases.isEmpty()) {
            lockMode = lockOptions.getAliasSpecificLockMode(aliases);
            if (lockMode == null) {
                lockMode = lockOptions.getLockMode();
            }
        }
        switch (lockMode) {
            case UPGRADE:
                return getForUpdateString(aliases);
            case PESSIMISTIC_READ:
                return getReadLockString(aliases, lockOptions.getTimeOut());
            case PESSIMISTIC_WRITE:
                return getWriteLockString(aliases, lockOptions.getTimeOut());
            case UPGRADE_NOWAIT:
            case FORCE:
            case PESSIMISTIC_FORCE_INCREMENT:
                return getForUpdateNowaitString(aliases);
            case UPGRADE_SKIPLOCKED:
                return getForUpdateSkipLockedString(aliases);
            default:
                return "";
        }
    }

    @Override
    public String getWriteLockString(int timeout) {
        if (timeout == LockOptions.NO_WAIT) {
            return " for update nowait";
        } else if (timeout == LockOptions.SKIP_LOCKED) {
            return " for update skip locked";
        } else {
            return " for update";
        }
    }

    @Override
    public String getWriteLockString(String aliases, int timeout) {
        if (timeout == LockOptions.NO_WAIT) {
            return String.format(" for update of %s nowait", aliases);
        } else if (timeout == LockOptions.SKIP_LOCKED) {
            return String.format(" for update of %s skip locked", aliases);
        } else {
            return " for update of " + aliases;
        }
    }

    @Override
    public String getReadLockString(int timeout) {
        if (timeout == LockOptions.NO_WAIT) {
            return " for share nowait";
        } else if (timeout == LockOptions.SKIP_LOCKED) {
            return " for share skip locked";
        } else {
            return " for share";
        }
    }

    @Override
    public String getReadLockString(String aliases, int timeout) {
        if (timeout == LockOptions.NO_WAIT) {
            return String.format(" for share of %s nowait", aliases);
        } else if (timeout == LockOptions.SKIP_LOCKED) {
            return String.format(" for share of %s skip locked", aliases);
        } else {
            return " for share of " + aliases;
        }
    }

    @Override
    public String getForUpdateNowaitString() {
        return getForUpdateString() + " nowait";
    }

    @Override
    public String getForUpdateNowaitString(String aliases) {
        return getForUpdateString(aliases) + " nowait";
    }

    @Override
    public String getForUpdateSkipLockedString() {
        return getForUpdateString() + " skip locked";
    }

    @Override
    public String getForUpdateSkipLockedString(String aliases) {
        return getForUpdateString(aliases) + " skip locked";
    }

    // callable statement support
    @Override
    public int registerResultSetOutParameter(CallableStatement statement, int position) throws SQLException {
        statement.registerOutParameter(position, Types.REF_CURSOR);
        position++;
        return position;
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement) throws SQLException {
        statement.execute();
        return (ResultSet) statement.getObject(1);
    }

    @Override
    public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
        if (position != 1) {
            throw new UnsupportedOperationException("OpenGauss only supports REF_CURSOR parameters as the first parameter");
        }
        return (ResultSet) statement.getObject(1);
    }

    // current timestamp support
    @Override
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }

    @Override
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }

    @Override
    public String getCurrentTimestampSelectString() {
        return "select current_timestamp"; // select current_timestamp
    }

    // SQLException support
    @Override
    public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
        return new SQLExceptionConversionDelegate() {
            @Override
            public JDBCException convert(SQLException sqlException, String message, String sql) {
                final String sqlState = JdbcExceptionHelper.extractSqlState(sqlException);

                if (sqlState != null && sqlState.length() >= 2) {
                    String sqlStateClassCode = sqlState.substring(0, 2);
                    switch (sqlStateClassCode) {
                        case "23":
                            switch (sqlState) {
                                case "23505":
                                case "23503":
                                case "23502": {
                                    String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(sqlException);
                                    return new ConstraintViolationException(message, sqlException, sql, constraintName);
                                }
                            }
                            return new ConstraintViolationException(message, sqlException, sql, null);
                        case "22":
                            return new DataException(message, sqlException, sql);
                        case "40":
                            return new LockAcquisitionException(message, sqlException, sql);
                        case "08":
                            return new JDBCConnectionException(message, sqlException, sql);
                        case "55":
                            return new PessimisticLockException(message, sqlException, sql);
                        case "57":
                            return new QueryTimeoutException(message, sqlException, sql);
                        default:
                            return null;
                    }
                }
                return null;
            }
        };
    }

    private static final ViolatedConstraintNameExtracter EXTRACTOR = new TemplatedViolatedConstraintNameExtracter() {
        @Override
        protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
            final String sqlState = JdbcExceptionHelper.extractSqlState(sqle);
            if (sqlState == null) {
                return null;
            }
            switch (sqlState) {
                // CHECK VIOLATION
                case "23514":
                    return extractUsingTemplate("violates check constraint \"", "\"", sqle.getMessage());
                // UNIQUE VIOLATION
                case "23505":
                    return extractUsingTemplate("violates unique constraint \"", "\"", sqle.getMessage());
                // FOREIGN KEY VIOLATION
                case "23503":
                    return extractUsingTemplate("violates foreign key constraint \"", "\"", sqle.getMessage());
                // NOT NULL VIOLATION
                case "23502":
                    return extractUsingTemplate("null value in column \"", "\" violates not-null constraint", sqle.getMessage());
                // 其他约束违规类型
                default:
                    return null;
            }
        }
    };

    @Override
    public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTOR;
    }

    // union subclass support
    @Override
    public boolean supportsUnionAll() {
        return true;
    }

    // miscellaneous support
    @Override
    public String getNoColumnsInsertString() {
        return "default values";
    }

    @Override
    public String getCaseInsensitiveLike() {
        return "ilike";
    }

    @Override
    public boolean supportsCaseInsensitiveLike() {
        return true;
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        return bool ? "t" : "f";
    }

    // keyword support
    @Override
    public NameQualifierSupport getNameQualifierSupport() {
        return NameQualifierSupport.SCHEMA;
    }

    @Override
    public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
        if (dbMetaData == null) {
            builder.setUnquotedCaseStrategy(IdentifierCaseStrategy.LOWER);
            builder.setQuotedCaseStrategy(IdentifierCaseStrategy.MIXED);
        }
        return super.buildIdentifierHelper(builder, dbMetaData);
    }

    // identifier quoting support
    // extend...


    // DDL support
    @Override
    public String[] getCreateSchemaCommand(String schemaName) {
        return new String[]{"create schema if not exists " + schemaName};
    }

    @Override
    public String[] getDropSchemaCommand(String schemaName) {
        return new String[]{"drop schema if exists " + schemaName};
    }

    @Override
    public String getCurrentSchemaCommand() {
        return "select current_schema()";
    }

    @Override
    public String getAddColumnString() {
        return "add column";
    }

    @Override
    public boolean supportsCommentOn() {
        return true;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public boolean supportsIfExistsBeforeConstraintName() {
        return true;
    }

    @Override
    public boolean supportsIfExistsAfterAlterTable() {
        return true;
    }

    @Override
    public String getCascadeConstraintsString() {
        return " cascade";
    }

    public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
        return new LocalTemporaryTableBulkIdStrategy(new IdTableSupportStandardImpl() {
            @Override
            public String getCreateIdTableCommand() {
                return "create temporary table";
            }
        }, AfterUseAction.DROP, null);
    }

    // Informational metadata
    @Override
    public boolean supportsEmptyInList() {
        return false;
    }

    @Override
    public boolean supportsRowValueConstructorSyntax() {
        return true;
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInSet() {
        return true;
    }

    @Override
    public boolean supportsRowValueConstructorSyntaxInInList() {
        return true;
    }

    @Override
    public boolean requiresParensForTupleDistinctCounts() {
        return true;
    }

    @Override
    public boolean supportsSelectAliasInGroupByClause() {
        return true;
    }

    @Override
    public boolean supportsNonQueryWithCTE() {
        return true;
    }

    @Override
    public boolean supportsValuesList() {
        return true;
    }

    @Override
    public boolean supportsLobValueChangePropogation() {
        return false;
    }

    @Override
    public boolean supportsPartitionBy() {
        return true;
    }

    @Override
    public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
        return false;
    }

    @Override
    public boolean supportsSkipLocked() {
        return true;
    }

    @Override
    public boolean supportsNoWait() {
        return true;
    }
}

