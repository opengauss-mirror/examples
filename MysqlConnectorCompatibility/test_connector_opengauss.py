"""
mysql-connector-python compatibility test suite for openGauss B-compatibility mode (dolphin plugin)

Environment:
  - openGauss 7.0.0-RC3 with dolphin plugin
  - mysql-connector-python 8.0.33
  - Python 3.9
  - MySQL protocol port: 3308

Key connection requirements (compared to standard MySQL):
  - auth_plugin='mysql_native_password'  (caching_sha2_password not supported)
  - use_pure=True                        (C extension fails auth; pure Python required)

Usage:
  python test_connector_opengauss.py
"""

import datetime
import decimal
import mysql.connector
import mysql.connector.errors as ce

# ── Connection parameters ──────────────────────────────────────────────────────
HOST     = "127.0.0.1"
PORT     = 3308
USER     = "pymysql_user"
PASSWORD = "Test@1234"
DATABASE = "pymysql_test"
CHARSET  = "utf8mb4"


def new_conn(**kwargs):
    return mysql.connector.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD,
        database=DATABASE, charset=CHARSET,
        collation="utf8mb4_general_ci",
        auth_plugin="mysql_native_password",
        use_pure=True,
        # Set globally at connection level (not per-test) so every connection
        # is consistent, and mysql-connector's cached connection.sql_mode
        # attribute never diverges from the server-side session sql_mode.
        sql_mode="NO_BACKSLASH_ESCAPES",
        **kwargs,
    )


class XFail(Exception):
    """Raised to signal a known, documented test failure pending a upstream fix."""


def run(name, fn):
    try:
        fn()
        print(f"  PASS  {name}")
        return "pass"
    except XFail as e:
        print(f"  XFAIL {name}: {e}")
        return "xfail"
    except Exception as e:
        print(f"  FAIL  {name}: {e}")
        return "fail"


# ── Test cases ─────────────────────────────────────────────────────────────────

def test_basic_connect():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("SELECT 1")
    assert cur.fetchone() == (1,)
    conn.close()


def test_crud():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_crud")
    cur.execute("CREATE TABLE t_crud (id INT PRIMARY KEY, val VARCHAR(200))")
    cur.execute("INSERT INTO t_crud VALUES (%s, %s)", (1, "hello"))
    conn.commit()
    cur.execute("SELECT val FROM t_crud WHERE id=1")
    assert cur.fetchone()[0] == "hello"
    cur.execute("UPDATE t_crud SET val='world' WHERE id=1")
    conn.commit()
    cur.execute("SELECT val FROM t_crud WHERE id=1")
    assert cur.fetchone()[0] == "world"
    cur.execute("DELETE FROM t_crud WHERE id=1")
    conn.commit()
    cur.execute("SELECT COUNT(*) FROM t_crud")
    assert cur.fetchone()[0] == 0
    # Ordinary CRUD with quote/backslash under the global NO_BACKSLASH_ESCAPES
    # connection setting, to verify normal parameterized writes are safe too.
    tricky = "O'Brien\\Corp"
    cur.execute("INSERT INTO t_crud VALUES (%s, %s)", (2, tricky))
    conn.commit()
    cur.execute("SELECT val FROM t_crud WHERE id=2")
    assert cur.fetchone()[0] == tricky
    cur.execute("DELETE FROM t_crud WHERE id=2")
    conn.commit()
    cur.execute("DROP TABLE t_crud")
    conn.commit()
    conn.close()


def test_parameterized_and_batch():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_batch")
    cur.execute("CREATE TABLE t_batch (id INT PRIMARY KEY, val VARCHAR(100))")
    data = [(i, f"row{i}") for i in range(1, 11)]
    cur.executemany("INSERT INTO t_batch VALUES (%s, %s)", data)
    conn.commit()
    cur.execute("SELECT COUNT(*) FROM t_batch")
    assert cur.fetchone()[0] == 10
    # Verify actual content, not just row count
    # fetchall() returns tuple-of-tuples; convert to list for comparison with data
    cur.execute("SELECT id, val FROM t_batch ORDER BY id")
    assert list(cur.fetchall()) == data, "Batch insert content mismatch"
    cur.execute("DROP TABLE t_batch")
    conn.commit()
    conn.close()


def test_autocommit():
    # mysql-connector uses property instead of method
    conn = new_conn()

    conn.autocommit = False
    cur = conn.cursor()
    cur.execute("SELECT @@autocommit")
    assert cur.fetchone()[0] == 0

    conn.autocommit = True
    cur.execute("SELECT @@autocommit")
    assert cur.fetchone()[0] == 1
    conn.close()


def test_commit_rollback():
    conn = new_conn()
    conn.autocommit = False
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_tx")
    cur.execute("CREATE TABLE t_tx (id INT PRIMARY KEY, val INT)")
    cur.execute("INSERT INTO t_tx VALUES (1, 100)")
    conn.commit()

    cur.execute("UPDATE t_tx SET val=200 WHERE id=1")
    conn.rollback()
    cur.execute("SELECT val FROM t_tx WHERE id=1")
    assert cur.fetchone()[0] == 100

    cur.execute("UPDATE t_tx SET val=300 WHERE id=1")
    conn.commit()
    cur.execute("SELECT val FROM t_tx WHERE id=1")
    assert cur.fetchone()[0] == 300

    cur.execute("DROP TABLE t_tx")
    conn.commit()
    conn.close()


def test_dict_cursor():
    # mysql-connector uses dictionary=True instead of DictCursor class
    conn = new_conn()
    cur = conn.cursor(dictionary=True)
    cur.execute("SELECT 1 AS num, 'hello' AS msg")
    row = cur.fetchone()
    assert isinstance(row, dict)
    assert row["num"] == 1
    assert row["msg"] == "hello"
    conn.close()


def test_unbuffered_cursor():
    # mysql-connector uses buffered=False instead of SSCursor
    conn = new_conn()
    cur = conn.cursor(buffered=False)
    cur.execute("SELECT generate_series(1,5)")
    rows = cur.fetchall()
    assert rows == [(1,), (2,), (3,), (4,), (5,)]
    conn.close()


def test_fetchmany():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_fetch")
    cur.execute("CREATE TABLE t_fetch (id INT PRIMARY KEY)")
    for i in range(1, 11):
        cur.execute("INSERT INTO t_fetch VALUES (%s)", (i,))
    conn.commit()
    cur.execute("SELECT id FROM t_fetch ORDER BY id")
    batch = cur.fetchmany(3)
    assert len(batch) == 3
    assert batch[0][0] == 1
    cur.fetchall()  # consume remaining rows before next statement
    cur.execute("DROP TABLE t_fetch")
    conn.commit()
    conn.close()


def test_chinese_unicode():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_unicode")
    cur.execute("CREATE TABLE t_unicode (id INT PRIMARY KEY, val VARCHAR(500))")
    texts = ["中文测试", "日本語テスト", "한국어테스트", "Ünïcödé", "emoji: 😀🎉"]
    for i, t in enumerate(texts):
        cur.execute("INSERT INTO t_unicode VALUES (%s, %s)", (i, t))
    conn.commit()
    cur.execute("SELECT val FROM t_unicode ORDER BY id")
    results = [r[0] for r in cur.fetchall()]
    assert results == texts
    cur.execute("DROP TABLE t_unicode")
    conn.commit()
    conn.close()


def test_special_characters():
    # NO_BACKSLASH_ESCAPES is now set at the connection level (see new_conn),
    # so mysql-connector uses '' doubling instead of \' escaping consistently,
    # matching openGauss/Dolphin's parsing behavior.
    # Known limitations:
    #   "\x00" (null byte)  - triggers bit string literal syntax error
    #   "a\\b" (backslash)  - triggers bit string literal syntax error
    conn = new_conn()
    cur = conn.cursor()
    specials = ["a'b", 'a"b', "a%b", "a_b"]
    for s in specials:
        cur.execute("SELECT %s", (s,))
        assert cur.fetchone()[0] == s
    conn.close()


def test_no_backslash_escapes():
    val = "a'b\\c%d"

    # Part 1: connection with explicit sql_mode parameter — verify both the session
    # variable string and the actual round-trip behavior are correct.
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("SELECT @@sql_mode")
    assert "NO_BACKSLASH_ESCAPES" in cur.fetchone()[0]
    cur.execute("SELECT %s", (val,))
    assert cur.fetchone()[0] == val
    conn.close()

    # Part 2: bare connection (no sql_mode parameter).
    # mysql-connector-python does NOT inspect HandshakeV10 server_status to adapt
    # its escaping strategy (unlike PyMySQL which detects SERVER_STATUS_NO_BACKSLASH_ESCAPES
    # and switches to quote-doubling). The connector always uses backslash escaping
    # unless sql_mode="NO_BACKSLASH_ESCAPES" is passed explicitly at connect time.
    # On openGauss (standard_conforming_strings=on) backslash is never an escape char,
    # so strings with single-quotes or backslashes cause syntax errors over a bare
    # connection. This is a known connector limitation → XFAIL.
    conn2 = mysql.connector.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD,
        database=DATABASE, charset=CHARSET,
        collation="utf8mb4_general_ci",
        auth_plugin="mysql_native_password",
        use_pure=True,
    )
    cur2 = conn2.cursor()
    # Plain string (no special chars) must always round-trip correctly.
    simple_val = "hello openGauss 123"
    cur2.execute("SELECT %s", (simple_val,))
    assert cur2.fetchone()[0] == simple_val, \
        "Bare connection basic string round-trip failed"

    # Strings with single-quote and backslash: expected to fail because the
    # connector does not adapt its escaping to the server's handshake status.
    bare_xfail = []
    for label, test_val in [("single-quote 0x27", "a'b"), ("backslash 0x5C", "a\\b")]:
        try:
            cur2.execute("SELECT %s", (test_val,))
            got = cur2.fetchone()[0]
            if got != test_val:
                bare_xfail.append(f"{label}: data mismatch got {got!r}")
        except ce.Error as e:
            errno_got = getattr(e, "errno", "?")
            msg = str(e)
            # Known limitation: mysql-connector-python's backslash-based escaping
            # produces syntax errors (errno 24708, message contains the word
            # unterminated) once misread by openGauss, which never treats
            # backslash as an escape character. Anything else (connection drops,
            # auth/permission errors, protocol errors, etc.) must fail the test,
            # not be swallowed into bare_xfail.
            if errno_got != 24708 and "unterminated" not in msg.lower():
                raise
            bare_xfail.append(f"{label}: {type(e).__name__}: {str(e)[:80]}")
            # Reset the aborted transaction so the NEXT sample in this loop
            # is tested from a clean transaction state, not tainted by this
            # failure (openGauss/PostgreSQL aborts the whole transaction on
            # a syntax error until rollback).
            conn2.rollback()

    try:
        conn2.close()
    except Exception:
        pass

    if bare_xfail:
        raise XFail(
            "mysql-connector-python bare connection does not adapt escaping to "
            "HandshakeV10 server_status (unlike PyMySQL); sql_mode='NO_BACKSLASH_ESCAPES' "
            "must be passed at connect time. Details: "
            + "; ".join(bare_xfail)
        )


def test_binary_blob():
    """
    UNHEX(%s) / HEX(data) workaround — the recommended approach for arbitrary binary data.
    Binds the hex-encoded string via %s (safe text binding), converts on the server side
    via UNHEX(), and reads back via HEX(). Covers full 0x00–0xFF byte range reliably.
    Native Python bytes %s binding is tested separately in test_binary_blob_native (test 19).
    """
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_blob")
    cur.execute("CREATE TABLE t_blob (id INT PRIMARY KEY, data BLOB)")

    empty  = b""
    binary = bytes([0x00, 0x01, 0x02, 0x7F, 0xFF])
    big    = bytes(range(256)) * 4

    # %s binds the hex string (str type) — not raw bytes; UNHEX converts on server side.
    cur.execute("INSERT INTO t_blob VALUES (1, UNHEX(%s))", (empty.hex(),))
    cur.execute("INSERT INTO t_blob VALUES (2, UNHEX(%s))", (binary.hex(),))
    cur.execute("INSERT INTO t_blob VALUES (3, UNHEX(%s))", (big.hex(),))
    conn.commit()

    # Read back via HEX(data) then decode in Python — byte-exact comparison.
    cur.execute("SELECT id, HEX(data) FROM t_blob ORDER BY id")
    rows = cur.fetchall()
    got_empty  = bytes.fromhex(rows[0][1]) if rows[0][1] else b""
    got_binary = bytes.fromhex(rows[1][1])
    got_big    = bytes.fromhex(rows[2][1])
    assert got_empty  == empty,  f"empty blob mismatch: {got_empty!r}"
    assert got_binary == binary, f"binary blob mismatch: {got_binary!r}"
    assert got_big    == big,    f"big blob length mismatch: {len(got_big)} vs {len(big)}"

    cur.execute("DROP TABLE t_blob")
    conn.commit()
    conn.close()


def test_binary_blob_native():
    """
    Direct Python bytes binding via %s — native parameter path (no UNHEX workaround).

    mysql-connector-python encodes bytes as _binary'...' literals with backslash escaping.
    This path is NOT governed by NO_BACKSLASH_ESCAPES: the server still treats 0x5C as
    an escape prefix inside _binary'...', so backslash bytes are silently dropped.
    Null bytes (0x00) and high bytes (0xFF) may also be affected.

    Expected outcome on openGauss 7.0.0 + dolphin: XFAIL for bytes containing 0x5C.
    Pending Plugin fix: openGauss/Plugin#<issue-number>
    """
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_blob_native")
    cur.execute("CREATE TABLE t_blob_native (id INT PRIMARY KEY, data BLOB)")
    conn.commit()

    xfail_reasons = []
    test_cases = [
        (1, bytes([0x41, 0x27, 0x42]),       "A'B  (0x27 single-quote)"),
        (2, bytes([0x41, 0x5C, 0x42]),       "A\\B  (0x5C backslash)"),
        (3, bytes([0x00, 0x01, 0x7F, 0xFF]), "boundary bytes 0x00/0xFF"),
        (4, bytes(range(256)),               "full 0x00–0xFF range"),
    ]

    for row_id, bdata, label in test_cases:
        try:
            cur.execute("INSERT INTO t_blob_native VALUES (%s, %s)", (row_id, bdata))
            conn.commit()
            cur.execute("SELECT data FROM t_blob_native WHERE id=%s", (row_id,))
            row = cur.fetchone()
            if row is None:
                xfail_reasons.append(f"[{label}] row not found after INSERT")
            else:
                got = row[0]
                if got == bdata:
                    print(f"      native bytes [{label}]: round-trip OK ✓")
                else:
                    xfail_reasons.append(
                        f"[{label}] data corrupted: expected {bdata!r}, got {got!r}"
                    )
        except ce.Error as e:
            errno_got = getattr(e, "errno", "?")
            msg = str(e)
            # Known limitation: same underlying defect as the bare-connection
            # escaping case (errno 24708, message contains the word unterminated) —
            # openGauss never treats backslash as an escape character, so the
            # connector's _binary-prefix escaping produces syntax errors for
            # certain byte values. Anything else must fail the test, not be
            # swallowed into xfail_reasons.
            if errno_got != 24708 and "unterminated" not in msg.lower():
                raise
            xfail_reasons.append(f"[{label}] raised {type(e).__name__}: {e}")
        finally:
            # Always rollback first: openGauss (PostgreSQL engine) marks the
            # transaction as aborted on any statement error, so every subsequent
            # statement in the same transaction will fail with "current transaction
            # is aborted" unless we rollback first.
            try:
                conn.rollback()
            except Exception:
                pass
            try:
                cur.execute("DELETE FROM t_blob_native WHERE id=%s", (row_id,))
                conn.commit()
            except Exception:
                try:
                    conn.rollback()
                except Exception:
                    pass

    # Wrap cleanup in try-except: if the connection is still in a bad state,
    # we must not let DROP TABLE bubble up as an unhandled exception (which would
    # make run() report FAIL instead of XFAIL, hiding the real reason).
    try:
        conn.rollback()
        cur.execute("DROP TABLE IF EXISTS t_blob_native")
        conn.commit()
    except Exception:
        try:
            conn.rollback()
        except Exception:
            pass
    # Wrap conn.close(): a broken connection (e.g. caused by bytes(range(256)))
    # can raise here; without protection the exception propagates before we reach
    # "raise XFail(...)", causing run() to report FAIL instead of XFAIL.
    try:
        conn.close()
    except Exception:
        pass

    if xfail_reasons:
        # Truncate each reason to 200 chars to keep terminal output readable;
        # bytes(range(256)) repr alone is ~780 chars.
        truncated = [r[:200] + ("…" if len(r) > 200 else "") for r in xfail_reasons]
        raise XFail(
            "native bytes %s binding is incomplete on openGauss+dolphin: "
            "0x5C (backslash) bytes are silently dropped by the _binary-prefix "
            "escaping path which is not governed by NO_BACKSLASH_ESCAPES. "
            "Use UNHEX(%s)/HEX(data) for reliable binary data transfer. "
            "Details: " + "; ".join(truncated)
        )


def test_large_text():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_largetext")
    cur.execute("CREATE TABLE t_largetext (id INT PRIMARY KEY, content TEXT)")
    big = "A" * 1024 * 1024
    cur.execute("INSERT INTO t_largetext VALUES (1, %s)", (big,))
    conn.commit()
    # Verify both length and exact content integrity (not just byte count)
    cur.execute("SELECT content FROM t_largetext WHERE id=1")
    result = cur.fetchone()[0]
    assert result == big, f"Large text content mismatch: returned length={len(result)}"
    cur.execute("DROP TABLE t_largetext")
    conn.commit()
    conn.close()


def test_datetime_types():
    conn = new_conn()
    cur = conn.cursor()
    # Fix session time zone to +08:00 so TIMESTAMP INSERT/SELECT is deterministic
    # regardless of the server's system_time_zone setting.
    cur.execute("SET time_zone = '+08:00'")
    cur.execute("DROP TABLE IF EXISTS t_dt")
    cur.execute(
        "CREATE TABLE t_dt "
        "(id INT PRIMARY KEY, d DATE, t TIME, dt DATETIME, ts TIMESTAMP)"
    )
    now = datetime.datetime(2026, 8, 13, 16, 0, 0)
    cur.execute(
        "INSERT INTO t_dt VALUES (%s,%s,%s,%s,%s)",
        (1, now.date(), now.time(), now, now),
    )
    conn.commit()
    # Read back all four time-related columns including TIMESTAMP.
    cur.execute("SELECT d, t, dt, ts FROM t_dt WHERE id=1")
    row = cur.fetchone()
    # Verify return types
    assert isinstance(row[0], datetime.date)
    assert isinstance(row[1], datetime.timedelta)
    assert isinstance(row[2], datetime.datetime)
    assert isinstance(row[3], datetime.datetime)
    # Verify actual values round-trip correctly
    assert row[0] == now.date(), f"DATE mismatch: {row[0]} != {now.date()}"
    assert row[1] == datetime.timedelta(hours=16), f"TIME mismatch: {row[1]}"
    assert row[2] == now, f"DATETIME mismatch: {row[2]} != {now}"
    assert row[3] == now, f"TIMESTAMP mismatch: {row[3]} != {now}"
    cur.execute("DROP TABLE t_dt")
    conn.commit()
    conn.close()


def test_decimal():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("SELECT CAST('123.456' AS DECIMAL(10,3))")
    val = cur.fetchone()[0]
    assert val == decimal.Decimal("123.456")
    conn.close()


def test_transaction_isolation():
    conn1 = new_conn()
    conn2 = new_conn()
    cur1 = conn1.cursor()
    cur2 = conn2.cursor()
    cur1.execute("DROP TABLE IF EXISTS t_iso")
    cur1.execute("CREATE TABLE t_iso (id INT PRIMARY KEY, val INT)")
    cur1.execute("INSERT INTO t_iso VALUES (1, 100)")
    conn1.commit()

    conn1.autocommit = False
    conn2.autocommit = False
    cur1.execute("UPDATE t_iso SET val=200 WHERE id=1")
    cur2.execute("SELECT val FROM t_iso WHERE id=1")
    assert cur2.fetchone()[0] == 100

    conn1.commit()
    cur2.execute("SELECT val FROM t_iso WHERE id=1")
    assert cur2.fetchone()[0] == 200

    conn2.rollback()
    conn2.close()
    cur1.execute("DROP TABLE t_iso")
    conn1.commit()
    conn1.close()


def test_reconnect_stability():
    for i in range(10):
        conn = new_conn()
        cur = conn.cursor()
        cur.execute("SELECT %s", (i,))
        assert cur.fetchone()[0] == i
        conn.close()


def test_error_mapping():
    """
    Verify that openGauss+dolphin maps MySQL error codes / exception types correctly.

    MySQL standard:
      table-not-found  → ProgrammingError, errno=1146, SQLSTATE='42S02'
      duplicate key    → IntegrityError,   errno=1062, SQLSTATE='23000'

    openGauss 7.0.0 maps both to DatabaseError (non-standard).
    Each mismatch is collected and raised as XFail at the end, so the test is
    clearly marked as an expected failure pending a Plugin fix, rather than silently
    passing with a misleading PASS.
    Pending Plugin fix: openGauss/Plugin#<issue-number>
    """
    conn = new_conn()
    cur = conn.cursor()
    xfail_reasons = []

    # ── Scenario 1: table not found ──────────────────────────────────────────
    # MySQL standard: ProgrammingError, errno=1146, SQLSTATE='42S02'
    try:
        cur.execute("INSERT INTO nonexistent_table VALUES (1)")
        # If we reach here, no exception at all — that is a hard failure.
        assert False, "No exception raised for INSERT into nonexistent_table"
    except AssertionError:
        raise
    except ce.ProgrammingError as e:
        errno_got = getattr(e, "errno", None)
        sqlstate_got = getattr(e, "sqlstate", None)
        if errno_got == 1146 and sqlstate_got == "42S02":
            print(f"      table_not_found: ProgrammingError/1146/42S02 ✓")
        else:
            # Even though the exception type matches ProgrammingError, do not
            # blindly accept an errno/sqlstate mismatch as a known mapping
            # difference: verify the underlying error still clearly indicates
            # the table genuinely does not exist (PostgreSQL-style SQLSTATE
            # 42P01 undefined_table, or an explicit does-not-exist message).
            # Permission errors, syntax errors, etc. that happen to also raise
            # ProgrammingError must fail the test, not be swallowed here.
            msg = str(e)
            msg_lower = msg.lower()
            is_known_table_not_found_mapping_diff = (
                sqlstate_got == "42P01"
                or "does not exist" in msg_lower
                or "doesn't exist" in msg_lower
                or "不存在" in msg
            )
            if not is_known_table_not_found_mapping_diff:
                raise
            xfail_reasons.append(
                f"table_not_found: ProgrammingError but errno={errno_got} "
                f"sqlstate={sqlstate_got} (expected 1146/42S02): {msg}"
            )
    except ce.Error as e:
        # Only treat this as a *known* error-mapping difference if the
        # underlying error still clearly indicates the table genuinely does
        # not exist (e.g. openGauss surfacing a PostgreSQL-style
        # undefined_table instead of translating it to MySQL errno
        # 1146/SQLSTATE 42S02). Connection drops, permission errors,
        # protocol errors, etc. do NOT match this signature and must fail
        # the test, not be swallowed into xfail_reasons.
        errno_got = getattr(e, "errno", "?")
        sqlstate_got = getattr(e, "sqlstate", "?")
        msg = str(e)
        msg_lower = msg.lower()
        is_known_table_not_found_mapping_diff = (
            "does not exist" in msg_lower
            or "doesn't exist" in msg_lower
            or "不存在" in msg
        )
        if is_known_table_not_found_mapping_diff:
            xfail_reasons.append(
                f"table_not_found: expected ProgrammingError/1146/42S02, "
                f"got {type(e).__name__}(errno={errno_got}, sqlstate={sqlstate_got}): {msg}"
            )
        else:
            raise
    finally:
        conn.rollback()

    # ── Scenario 2: duplicate primary key ────────────────────────────────────
    # MySQL standard: IntegrityError, errno=1062, SQLSTATE='23000'
    # Setup is NOT part of the expected-failure path: if DROP/CREATE/first
    # INSERT fails for an unrelated reason, let the exception propagate as a
    # real test failure instead of being swallowed into xfail_reasons below.
    cur.execute("DROP TABLE IF EXISTS t_dup")
    cur.execute("CREATE TABLE t_dup (id INT PRIMARY KEY)")
    cur.execute("INSERT INTO t_dup VALUES (1)")
    conn.commit()
    try:
        cur.execute("INSERT INTO t_dup VALUES (1)")
        conn.commit()
        # If we reach here, no exception at all — hard failure.
        assert False, "No exception raised for duplicate key INSERT"
    except AssertionError:
        raise
    except ce.IntegrityError as e:
        errno_got = getattr(e, "errno", None)
        sqlstate_got = getattr(e, "sqlstate", None)
        if errno_got == 1062 and sqlstate_got == "23000":
            print(f"      dup_key: IntegrityError/1062/23000 ✓")
        else:
            xfail_reasons.append(
                f"dup_key: IntegrityError but errno={errno_got} "
                f"sqlstate={sqlstate_got} (expected 1062/23000)"
            )
    except ce.Error as e:
        # Only treat this as a *known* error-mapping difference if the
        # underlying error still clearly signals a duplicate-key / unique
        # constraint violation (e.g. openGauss surfacing a PostgreSQL-style
        # unique_violation instead of translating it to MySQL errno
        # 1062/SQLSTATE 23000). Connection drops, permission errors,
        # protocol errors, etc. do NOT match this signature and must fail
        # the test, not be swallowed into xfail_reasons.
        errno_got = getattr(e, "errno", "?")
        sqlstate_got = getattr(e, "sqlstate", "?")
        msg = str(e)
        msg_lower = msg.lower()
        is_known_dup_mapping_diff = (
            sqlstate_got == "23505"
            or "duplicate key value violates unique constraint" in msg_lower
            or "重复键值违反唯一约束" in msg
        )
        if is_known_dup_mapping_diff:
            xfail_reasons.append(
                f"dup_key: expected IntegrityError/1062/23000, "
                f"got {type(e).__name__}(errno={errno_got}, sqlstate={sqlstate_got}): {msg}"
            )
        else:
            raise
    finally:
        conn.rollback()
        try:
            cur.execute("DROP TABLE IF EXISTS t_dup")
            conn.commit()
        except Exception:
            pass

    try:
        conn.close()
    except Exception:
        pass

    if xfail_reasons:
        raise XFail(
            "openGauss does not conform to MySQL error-type/errno/SQLSTATE mapping; "
            "pending Plugin fix. "
            "Details: " + "; ".join(xfail_reasons)
        )


# ── Runner ─────────────────────────────────────────────────────────────────────

TESTS = [
    ("01 basic connect",                    test_basic_connect),
    ("02 CRUD",                             test_crud),
    ("03 parameterized & batch",            test_parameterized_and_batch),
    ("04 autocommit property",              test_autocommit),
    ("05 commit / rollback",               test_commit_rollback),
    ("06 dict cursor",                      test_dict_cursor),
    ("07 unbuffered cursor",               test_unbuffered_cursor),
    ("08 fetchmany",                        test_fetchmany),
    ("09 Chinese & Unicode",               test_chinese_unicode),
    ("10 special characters",              test_special_characters),
    ("11 NO_BACKSLASH_ESCAPES",            test_no_backslash_escapes),
    ("12 binary BLOB (UNHEX workaround)",  test_binary_blob),
    ("13 large text (1 MB)",               test_large_text),
    ("14 DATE/TIME/DATETIME",              test_datetime_types),
    ("15 DECIMAL",                         test_decimal),
    ("16 transaction isolation",           test_transaction_isolation),
    ("17 reconnect stability x10",         test_reconnect_stability),
    ("18 error type mapping",              test_error_mapping),
    ("19 binary BLOB native bytes",        test_binary_blob_native),
]

if __name__ == "__main__":
    passed = failed = xfailed = 0
    print(f"\nmysql-connector × openGauss(dolphin) compatibility test\n{'='*56}")
    for name, fn in TESTS:
        result = run(name, fn)
        if result == "pass":
            passed += 1
        elif result == "xfail":
            xfailed += 1
        else:
            failed += 1
    print(f"{'='*56}")
    xfail_note = f", {xfailed} xfailed (expected)" if xfailed else ""
    print(f"Result: {passed} passed, {failed} failed{xfail_note} / {len(TESTS)} total\n")
    raise SystemExit(1 if failed else 0)
