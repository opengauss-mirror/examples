package org.hibernate.dialect.unit;

import org.hibernate.dialect.OpenGaussDialect;
import org.junit.jupiter.api.*;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OpenGaussDialectCallableStatementTest {

    private static OpenGaussDialect dialect;

    private CallableStatement callableStatement;

    @BeforeAll
    public static void init() {
        dialect = new OpenGaussDialect();
    }

    @BeforeEach
    public void setup() {
        callableStatement = mock(CallableStatement.class);
    }

    @AfterEach
    public void teardown() {
        callableStatement = null;
    }

    @Test
    public void testRegisterResultSetOutParameter() throws SQLException {
        int position = 1;
        int returnedPosition = dialect.registerResultSetOutParameter(callableStatement, position);
        verify(callableStatement).registerOutParameter(position, Types.REF_CURSOR);
        Assertions.assertEquals(position + 1, returnedPosition, "Position should be incremented by 1");
    }

    @Test
    public void testGetResultSet() throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        when(callableStatement.getObject(1)).thenReturn(mockResultSet);
        ResultSet resultSet = dialect.getResultSet(callableStatement);
        verify(callableStatement).execute();
        verify(callableStatement).getObject(1);
        Assertions.assertEquals(mockResultSet, resultSet, "ResultSet should match the one returned by getObject(1)");
    }

    @Test
    public void testGetResultSetWithPosition() throws SQLException {
        ResultSet mockResultSet = mock(ResultSet.class);
        when(callableStatement.getObject(1)).thenReturn(mockResultSet);
        ResultSet resultSet = dialect.getResultSet(callableStatement, 1);
        verify(callableStatement).getObject(1);
        Assertions.assertEquals(mockResultSet, resultSet, "ResultSet should match the one returned by getObject(1)");
        RuntimeException runtimeException = assertThrows(UnsupportedOperationException.class, () -> {
            dialect.getResultSet(callableStatement, 2);
        });
        Assertions.assertTrue(runtimeException.getMessage().contains("OpenGauss only supports REF_CURSOR parameters as the first parameter"));
    }
}
