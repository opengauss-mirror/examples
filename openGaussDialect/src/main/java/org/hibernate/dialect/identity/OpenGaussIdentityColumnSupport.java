package org.hibernate.dialect.identity;

import java.sql.Types;

public class OpenGaussIdentityColumnSupport extends IdentityColumnSupportImpl {

    @Override
    public boolean supportsIdentityColumns() {
        return false;
    }

    @Override
    public String getIdentitySelectString(String table, String column, int type) {
        // {table}_{column}_seq
        return "select currval('" + table + '_' + column + "_seq')";
    }

    @Override
    public String getIdentityColumnString(int type) {
        return type == Types.BIGINT ? "bigserial not null" : "serial not null";
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }
}
