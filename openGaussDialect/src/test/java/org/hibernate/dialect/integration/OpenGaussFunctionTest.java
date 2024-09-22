package org.hibernate.dialect.integration;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.entity.function.TestEntity;
import org.hibernate.dialect.util.HibernateUtil;
import org.hibernate.query.Query;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OpenGaussFunctionTest {
    private static SessionFactory sessionFactory;

    private Session session;

    @BeforeAll
    public static void setUpAll() {
        sessionFactory = HibernateUtil.getSessionFactory(TestEntity.class);
    }

    @BeforeEach
    public void setUp() {
        session = sessionFactory.openSession();
        session.beginTransaction();
        session.createQuery("DELETE FROM TestEntity").executeUpdate();
        session.getTransaction().commit();
    }

    @AfterEach
    public void tearDown() {
        if (session != null) {
            session.close();
        }
    }

    @AfterAll
    public static void tearDownALL() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    public void testTypeMapping() {
        Transaction transaction = session.beginTransaction();
        transaction.commit();
    }

    @Test
    public void testMathFunctions() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        // Test abs function
        Query<Double> absQuery = session.createQuery("select abs(-17.4) from TestEntity", Double.class);
        Double absResult = absQuery.getSingleResult();
        assertEquals(17.4, absResult, 0.001);

        // Test cbrt function
        Query<Double> cbrtQuery = session.createQuery("select cbrt(27.0) from TestEntity", Double.class);
        Double cbrtResult = cbrtQuery.getSingleResult();
        assertEquals(3.0, cbrtResult, 0.001);

        // Test ceil function
        Query<Double> ceilQuery = session.createQuery("select ceil(-42.8) from TestEntity", Double.class);
        Double ceilResult = ceilQuery.getSingleResult();
        assertEquals(-42L, ceilResult);

        // Test degrees function
        Query<Double> degreesQuery = session.createQuery("select degrees(0.5) from TestEntity", Double.class);
        Double degreesResult = degreesQuery.getSingleResult();
        assertEquals(28.6478897565412, degreesResult, 0.001);

        // Test exp function
        Query<Double> expQuery = session.createQuery("select exp(1.0) from TestEntity", Double.class);
        Double expResult = expQuery.getSingleResult();
        assertEquals(2.718281828459045, expResult, 0.001);

        // Test floor function
        Query<Double> floorQuery = session.createQuery("select floor(-42.8) from TestEntity", Double.class);
        Double floorResult = floorQuery.getSingleResult();
        assertEquals(-43L, floorResult);

        // Test ln function
        Query<Double> lnQuery = session.createQuery("select ln(2.0) from TestEntity", Double.class);
        Double lnResult = lnQuery.getSingleResult();
        assertEquals(0.6931471805599453, lnResult, 0.001);

        // Test log function
        Query<Double> logQuery = session.createQuery("select log(100.0) from TestEntity", Double.class);
        Double logResult = logQuery.getSingleResult();
        assertEquals(2.0, logResult, 0.001);

        // Test mod function
        Query<Integer> modQuery = session.createQuery("select mod(9, 4) from TestEntity", Integer.class);
        Integer modResult = modQuery.getSingleResult();
        assertEquals(1, modResult.intValue());

        // Test pi function
        Query<Double> piQuery = session.createQuery("select pi() from TestEntity", Double.class);
        Double piResult = piQuery.getSingleResult();
        assertEquals(3.14159265358979, piResult, 0.001);

        // Test power function
        Query<Double> powerQuery = session.createQuery("select power(9.0, 3.0) from TestEntity", Double.class);
        Double powerResult = powerQuery.getSingleResult();
        assertEquals(729.0, powerResult, 0.001);

        // Test radians function
        Query<Double> radiansQuery = session.createQuery("select radians(45.0) from TestEntity", Double.class);
        Double radiansResult = radiansQuery.getSingleResult();
        assertEquals(0.7853981633974483, radiansResult, 0.001);

        // Test random function
        Query<Double> randomQuery = session.createQuery("select random() from TestEntity", Double.class);
        Double randomResult = randomQuery.getSingleResult();
        assertTrue(randomResult >= 0.0 && randomResult <= 1.0);

        // Test round function
        Query<Double> roundQuery = session.createQuery("select round(42.6) from TestEntity", Double.class);
        Double roundResult = roundQuery.getSingleResult();
        assertEquals(43L, roundResult);

        // Test sign function
        Query<Integer> signQuery = session.createQuery("select sign(-8.4) from TestEntity", Integer.class);
        Integer signResult = signQuery.getSingleResult();
        assertEquals(-1, signResult.intValue());

        // Test sqrt function
        Query<Double> sqrtQuery = session.createQuery("select sqrt(2.0) from TestEntity", Double.class);
        Double sqrtResult = sqrtQuery.getSingleResult();
        assertEquals(1.4142135623730951, sqrtResult, 0.001);

        // Test trunc function
        Query<Double> truncQuery = session.createQuery("select trunc(42.8) from TestEntity", Double.class);
        Double truncResult = truncQuery.getSingleResult();
        assertEquals(42L, truncResult);
    }

    @Test
    public void testTrigonometryFunctions() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        // Test acos function
        Query<Double> acosQuery = session.createQuery("select acos(-1) from TestEntity", Double.class);
        Double acosResult = acosQuery.getSingleResult();
        assertEquals(Math.PI, acosResult, 0.0001);

        // Test asin function
        Query<Double> asinQuery = session.createQuery("select asin(0.5) from TestEntity", Double.class);
        Double asinResult = asinQuery.getSingleResult();
        assertEquals(Math.asin(0.5), asinResult, 0.0001);

        // Test atan function
        Query<Double> atanQuery = session.createQuery("select atan(1) from TestEntity", Double.class);
        Double atanResult = atanQuery.getSingleResult();
        assertEquals(Math.atan(1), atanResult, 0.0001);

        // Test atan2 function
        Query<Double> atan2Query = session.createQuery("select atan2(2, 1) from TestEntity", Double.class);
        Double atan2Result = atan2Query.getSingleResult();
        assertEquals(Math.atan2(2, 1), atan2Result, 0.0001);

        // Test cos function
        Query<Double> cosQuery = session.createQuery("select cos(-3.1415927) from TestEntity", Double.class);
        Double cosResult = cosQuery.getSingleResult();
        assertEquals(Math.cos(-3.1415927), cosResult, 0.0001);

        // Test cot function
        Query<Double> cotQuery = session.createQuery("select cot(1) from TestEntity", Double.class);
        Double cotResult = cotQuery.getSingleResult();
        assertEquals(1.0 / Math.tan(1), cotResult, 0.0001);

        // Test sin function
        Query<Double> sinQuery = session.createQuery("select sin(1.57079) from TestEntity", Double.class);
        Double sinResult = sinQuery.getSingleResult();
        assertEquals(Math.sin(1.57079), sinResult, 0.0001);

        // Test tan function
        Query<Double> tanQuery = session.createQuery("select tan(20) from TestEntity", Double.class);
        Double tanResult = tanQuery.getSingleResult();
        assertEquals(Math.tan(20), tanResult, 0.0001);
    }

    @Test
    public void testStringFunctions() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        // Test string concatenation
        Query<String> concatQuery = session.createQuery("select 'MPP' || 'DB' as result from TestEntity", String.class);
        String concatResult = concatQuery.getSingleResult();
        assertEquals("MPPDB", concatResult);

        // Test convert function
        Query<byte[]> query = session.createQuery("select convert('text_in_utf8', 'UTF8', 'GBK') from TestEntity", byte[].class);
        byte[] convertResult = query.getSingleResult();
        String hexResult = "\\x" + bytesToHex(convertResult);
        assertEquals("\\x746578745f696e5f75746638", hexResult);

        // Test bit_length function
        Query<Long> bitLengthQuery = session.createQuery("select bit_length('world') from TestEntity", Long.class);
        Long bitLengthResult = bitLengthQuery.getSingleResult();
        assertEquals(40, bitLengthResult.intValue());

        // Test lower function
        Query<String> lowerQuery = session.createQuery("select lower('TOM') from TestEntity", String.class);
        String lowerResult = lowerQuery.getSingleResult();
        assertEquals("tom", lowerResult);

        // Test overlay function
        Query<String> overlayQuery = session.createQuery("select overlay('hello','world',2,3) from TestEntity", String.class);
        String overlayResult = overlayQuery.getSingleResult();
        assertEquals("hworldo", overlayResult);

        // Test position function
        Query<Integer> positionQuery = session.createQuery("select position('ing','string') from TestEntity", Integer.class);
        Integer positionResult = positionQuery.getSingleResult();
        assertEquals(4, positionResult.intValue());

        // Test substring function (with start and length)
        Query<String> substringQuery = session.createQuery("select substring('Thomas',2,3) from TestEntity", String.class);
        String substringResult = substringQuery.getSingleResult();
        assertEquals("hom", substringResult);

        substringQuery = session.createQuery("select substring('Thomas','...$') from TestEntity", String.class);
        substringResult = substringQuery.getSingleResult();
        assertEquals("mas", substringResult);

        substringQuery = session.createQuery("select substring('foobar','o(.)b') from TestEntity", String.class);
        substringResult = substringQuery.getSingleResult();
        assertEquals("o", substringResult);

        substringQuery = session.createQuery("select substring('foobar','(o(.)b)') from TestEntity", String.class);
        substringResult = substringQuery.getSingleResult();
        assertEquals("oob", substringResult);

        // Test trim function
        Query<String> trimQuery = session.createQuery("select trim(both 'x' from 'xTomxx') from TestEntity", String.class);
        String trimResult = trimQuery.getSingleResult();
        assertEquals("Tom", trimResult);

        // Test upper function
        Query<String> upperQuery = session.createQuery("select upper('tom') from TestEntity", String.class);
        String upperResult = upperQuery.getSingleResult();
        assertEquals("TOM", upperResult);

        // Test ascii function
        Query<Integer> asciiQuery = session.createQuery("select ascii('xyz') from TestEntity", Integer.class);
        Integer asciiResult = asciiQuery.getSingleResult();
        assertEquals(120, asciiResult.intValue());

        // Test btrim function
        Query<String> btrimQuery = session.createQuery("select btrim('sring','ing') from TestEntity", String.class);
        String btrimResult = btrimQuery.getSingleResult();
        assertEquals("sr", btrimResult);

        // Test char function
        Query<Character> chrQuery = session.createQuery("select chr(65) from TestEntity", Character.class);
        Character chrResult = chrQuery.getSingleResult();
        assertEquals('A', chrResult);

        // Test initcap function
        Query<String> initcapQuery = session.createQuery("select initcap('hi THOMAS') from TestEntity", String.class);
        String initcapResult = initcapQuery.getSingleResult();
        assertEquals("Hi Thomas", initcapResult);

        // Test length function
        Query<Integer> lengthQuery = session.createQuery("select length('abcd') from TestEntity", Integer.class);
        Integer lengthResult = lengthQuery.getSingleResult();
        assertEquals(4, lengthResult.intValue());

        // Test lpad function
        Query<String> lpadQuery = session.createQuery("select lpad('hi', 5, 'xyza') from TestEntity", String.class);
        String lpadResult = lpadQuery.getSingleResult();
        assertEquals("xyzhi", lpadResult);

        // Test ltrim function
        Query<String> ltrimQuery = session.createQuery("select ltrim('xxxxTRIM','x') from TestEntity", String.class);
        String ltrimResult = ltrimQuery.getSingleResult();
        assertEquals("TRIM", ltrimResult);

        // Test md5 function
        Query<String> md5Query = session.createQuery("select md5('ABC') from TestEntity", String.class);
        String md5Result = md5Query.getSingleResult();
        assertEquals("902fbdd2b1df0c4f70b4a5d23525e932", md5Result);

        // Test repeat function
        Query<String> repeatQuery = session.createQuery("select repeat('Pg', 4) from TestEntity", String.class);
        String repeatResult = repeatQuery.getSingleResult();
        assertEquals("PgPgPgPg", repeatResult);

        // Test replace function
        Query<String> replaceQuery = session.createQuery("select replace('abcdefabcdef', 'cd', 'XXX') from TestEntity", String.class);
        String replaceResult = replaceQuery.getSingleResult();
        assertEquals("abXXXefabXXXef", replaceResult);

        // Test rpad function
        Query<String> rpadQuery = session.createQuery("select rpad('hi', 5, 'xy') from TestEntity", String.class);
        String rpadResult = rpadQuery.getSingleResult();
        assertEquals("hixyx", rpadResult);

        // Test rtrim function
        Query<String> rtrimQuery = session.createQuery("select rtrim('trimxxxx', 'x') from TestEntity", String.class);
        String rtrimResult = rtrimQuery.getSingleResult();
        assertEquals("trim", rtrimResult);

        // Test split_part function
        Query<String> splitPartQuery = session.createQuery("select split_part('abc~@~def~@~ghi', '~@~', 2) from TestEntity", String.class);
        String splitPartResult = splitPartQuery.getSingleResult();
        assertEquals("def", splitPartResult);

        // Test strpos function
        Query<Integer> strposQuery = session.createQuery("select strpos('source', 'rc') from TestEntity", Integer.class);
        Integer strposResult = strposQuery.getSingleResult();
        assertEquals(4, strposResult.intValue());

        // Test to_hex function
        Query<String> toHexQuery = session.createQuery("select to_hex(2147483647) from TestEntity", String.class);
        String toHexResult = toHexQuery.getSingleResult();
        assertEquals("7fffffff", toHexResult);
    }

    @Test
    public void toCharTest() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        Query<String> translateQuery = session.createQuery("select translate('12345', '143', 'ax') from TestEntity", String.class);
        String translateResult = translateQuery.getSingleResult();
        assertEquals("a2x5", translateResult);

        Query<String> toCharQuery = session.createQuery("select to_char(current_timestamp, 'HH12:MI:SS') from TestEntity", String.class);
        String toCharResult = toCharQuery.getSingleResult();
        System.out.println(toCharResult);

        toCharQuery = session.createNativeQuery(
                        "SELECT to_char(interval '15h 2m 12s', 'HH24:MI:SS') AS formatted_value")
                .addScalar("formatted_value", StandardBasicTypes.STRING
                );
        toCharResult = toCharQuery.getSingleResult();
        assertEquals("15:02:12", toCharResult);

        toCharQuery = session.createQuery("select to_char(125, '999') from TestEntity", String.class);
        toCharResult = toCharQuery.getSingleResult();
        assertEquals(" 125", toCharResult);

        toCharQuery = session.createNativeQuery(
                        "SELECT to_char(CAST(125.8 AS REAL), '999D99') AS formatted_value")
                .addScalar("formatted_value", StandardBasicTypes.STRING
                );
        toCharResult = toCharQuery.getSingleResult();
        assertEquals(" 125.80", toCharResult);

        toCharQuery = session.createQuery("select to_char(-125.8, '999D99S') from TestEntity", String.class);
        toCharResult = toCharQuery.getSingleResult();
        assertEquals("125.80-", toCharResult);
    }

    @Test
    public void toDateTest() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        Query<Date> toTimestampQuery = session.createQuery("select to_timestamp('05 Dec 2000', 'DD Mon YYYY') from TestEntity", Date.class);
        Date result = toTimestampQuery.getSingleResult();
        System.out.println(result);
    }

    @Test
    public void toNumberTest() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();

        Query<BigDecimal> toNumberQuery = session.createQuery(
                "select to_number('125.8', '999D99') from TestEntity", BigDecimal.class
        );
        BigDecimal toNumberResult = toNumberQuery.getSingleResult();
        assertEquals(new BigDecimal("125.8"), toNumberResult);
    }

    @Test
    public void toTimeStampTest() {
        Transaction transaction = session.beginTransaction();
        TestEntity testEntity = new TestEntity();
        testEntity.setValue(0.0);
        session.save(testEntity);
        transaction.commit();
        Query<Date> toTimestampQuery = session.createQuery(
                "select to_timestamp('05 Dec 2000', 'DD Mon YYYY') from TestEntity", Date.class
        );
        Date toTimestampResult = toTimestampQuery.getSingleResult();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expectedTimestamp = "2000-12-05 00:00:00";
        assertEquals(expectedTimestamp, sdf.format(toTimestampResult));
    }
}
