package org.hibernate.dialect.type;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

public class JsonbType implements UserType {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.OTHER};
    }

    @Override
    public Class<Object> returnedClass() {
        return Object.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        final String cellContent = rs.getString(names[0]);
        if (cellContent == null) {
            return null;
        }
        try {
            return objectMapper.readValue(cellContent.getBytes(StandardCharsets.UTF_8), returnedClass());
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to convert String to Invoice: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        try {
            final StringWriter w = new StringWriter();
            objectMapper.writeValue(w, value);
            w.flush();
            st.setObject(index, w.toString(), Types.OTHER);
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to convert Invoice to String: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            oos.flush();
            oos.close();
            bos.close();
            ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
            Object obj = new ObjectInputStream(bais).readObject();
            bais.close();
            return obj;
        } catch (ClassNotFoundException | IOException ex) {
            throw new HibernateException(ex);
        }
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
