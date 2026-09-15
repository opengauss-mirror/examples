"""
PyMySQL compatibility test suite for openGauss B-compatibility mode (dolphin plugin)

Environment:
  - openGauss 7.0.0-RC3 with dolphin plugin
  - PyMySQL 1.2.0
  - Python 3.9
  - MySQL protocol port: 3308

Usage:
  python test_pymysql_opengauss.py
"""

import datetime
import pymysql
import pymysql.cursors
from pymysql.constants import SERVER_STATUS


class XFail(Exception):
    """Expected failure: openGauss behaviour differs from MySQL standard."""


# ── Connection parameters ──────────────────────────────────────────────────────
HOST = "127.0.0.1"
PORT = 3308
USER = "pymysql_user"
PASSWORD = "Test@1234"   # replace before running
DATABASE = "pymysql_test"
CHARSET = "utf8mb4"


def new_conn(**kwargs):
    return pymysql.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD,
        database=DATABASE, charset=CHARSET,
        # Set globally at connection level (not per-test) via init_command, so
        # every connection is consistent and bytes/str parameters containing
        # quotes or backslashes are escaped in a way openGauss actually
        # understands (openGauss follows standard_conforming_strings semantics,
        # where backslash is never a literal-string escape character).
        init_command="SET sql_mode='NO_BACKSLASH_ESCAPES'",
        **kwargs
    )


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
    conn = new_conn()
    # PyMySQL default autocommit is False; assert initial state is False
    assert conn.get_autocommit() is False

    conn.autocommit(False)
    assert conn.get_autocommit() is False

    cur = conn.cursor()
    cur.execute("SELECT @@autocommit")
    assert cur.fetchone()[0] == 0

    conn.autocommit(True)
    assert conn.get_autocommit() is True

    cur.execute("SELECT @@autocommit")
    assert cur.fetchone()[0] == 1
    conn.close()


def test_commit_rollback():
    conn = new_conn()
    conn.autocommit(False)
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
    conn = new_conn()
    cur = conn.cursor(pymysql.cursors.DictCursor)
    cur.execute("SELECT 1 AS num, 'hello' AS msg")
    row = cur.fetchone()
    assert isinstance(row, dict)
    assert row["num"] == 1
    assert row["msg"] == "hello"
    conn.close()


def test_ss_cursor():
    conn = new_conn()
    cur = conn.cursor(pymysql.cursors.SSCursor)
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
    # sql_mode="NO_BACKSLASH_ESCAPES" is now set globally via new_conn()'s
    # init_command, so no per-test SET is needed here.
    conn = new_conn()
    cur = conn.cursor()
    specials = ["a'b", 'a"b', "a\\b", "a%b", "a_b"]
    for s in specials:
        cur.execute("SELECT %s", (s,))
        assert cur.fetchone()[0] == s
    # Known limitation: NUL byte (\x00) is formatted as a bit-string literal by PyMySQL,
    # causing a syntax error in Dolphin. Documented as unsupported.
    conn.close()


def test_no_backslash_escapes():
    val = "a'b\\c%d"

    # Part 1: connection with explicit init_command sql_mode — verify sql_mode
    # string, actual GUC standard_conforming_strings, and protocol status bit.
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("SELECT @@sql_mode")
    assert "NO_BACKSLASH_ESCAPES" in cur.fetchone()[0]
    # standard_conforming_strings must be 'on' so the server lexer treats
    # backslash as a literal character — this is the real guard against
    # backslash-escape misinterpretation, independent of sql_mode.
    cur.execute("SHOW standard_conforming_strings")
    scs = cur.fetchone()[0]
    assert scs == "on", \
        f"standard_conforming_strings must be 'on' for NO_BACKSLASH_ESCAPES semantics, got {scs!r}"
    assert conn.server_status & SERVER_STATUS.SERVER_STATUS_NO_BACKSLASH_ESCAPES, \
        "Protocol NO_BACKSLASH_ESCAPES bit should be ON after init_command"
    cur.execute("SELECT %s", (val,))
    assert cur.fetchone()[0] == val
    conn.close()

    # Part 2: bare connection (no init_command / no SET) — after the HandshakeV10
    # fix, the initial status_flags must already reflect openGauss's
    # standard_conforming_strings=on, so the first query works without any SET.
    conn2 = pymysql.connect(
        host=HOST, port=PORT, user=USER, password=PASSWORD,
        database=DATABASE, charset=CHARSET,
    )
    cur2 = conn2.cursor()
    cur2.execute("SHOW standard_conforming_strings")
    scs2 = cur2.fetchone()[0]
    assert scs2 == "on", \
        f"standard_conforming_strings must be 'on' by default in openGauss, got {scs2!r}"
    assert conn2.server_status & SERVER_STATUS.SERVER_STATUS_NO_BACKSLASH_ESCAPES, \
        "HandshakeV10 must report NO_BACKSLASH_ESCAPES ON for openGauss default config"
    cur2.execute("SELECT %s", (val,))
    assert cur2.fetchone()[0] == val, "Bare connection round-trip must succeed"
    conn2.close()


def test_binary_blob():
    # Known limitation: PyMySQL's %s binding for `bytes` parameters always
    # assumes backslash escaping is in effect (same underlying defect as
    # mysql-connector-python), but openGauss follows PostgreSQL's
    # standard_conforming_strings semantics, where backslash is NEVER a
    # literal-string escape character - regardless of sql_mode. Any byte that
    # would need escaping (0x27 quote, 0x5C backslash, etc.) triggers
    # "unterminated quoted string" once the client-escaped sequence is
    # misread by openGauss. This was verified independent of sql_mode
    # (reproduced identically with global NO_BACKSLASH_ESCAPES set via
    # init_command in new_conn()).
    #
    # Workaround: bind the hex-encoded *string* via %s (str parameters are
    # correctly escaped by PyMySQL - see tests 10/11) and let UNHEX()/HEX()
    # do the binary conversion server-side, avoiding the client-side bytes
    # escaping bug entirely while still exercising real parameter binding.
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_blob")
    cur.execute("CREATE TABLE t_blob (id INT PRIMARY KEY, data BLOB)")

    # Part A: native %s bytes binding — test all four problematic byte values.
    # Compares raw data column byte-by-byte (SELECT data, not HEX(data)).
    # After Plugin HandshakeV10 fix, PyMySQL uses quote-doubling when
    # NO_BACKSLASH_ESCAPES is active in server_status.
    #
    # Known limitations (XFAIL if triggered):
    #   0x5C backslash — _binary-prefix escaping path silently drops it,
    #                     ignoring NO_BACKSLASH_ESCAPES; confirmed above to be
    #                     independent of sql_mode, i.e. a PyMySQL client-side
    #                     defect unrelated to the Plugin's server_status fix.
    #   0x00 null byte — formatted as bit-literal, causes SQL syntax error.
    native_xfail = []
    native_cases = [
        (91, bytes([0x27]),                    "0x27 single-quote"),
        (92, bytes([0x5c]),                    "0x5c backslash"),
        (93, bytes([0x00]),                    "0x00 null byte"),
        (94, bytes([0xff]),                    "0xff high byte"),
        (95, bytes([0x00, 0x27, 0x5c, 0xff]), "0x00+0x27+0x5c+0xff combined"),
    ]
    for row_id, bval, label in native_cases:
        try:
            cur.execute("INSERT INTO t_blob VALUES (%s, %s)", (row_id, bval))
            conn.commit()
            cur.execute("SELECT data FROM t_blob WHERE id=%s", (row_id,))
            got = cur.fetchone()[0]
            if got != bval:
                native_xfail.append(
                    f"{label}: corrupted — expected {bval!r}, got {got!r}"
                )
        except Exception as e:
            native_xfail.append(f"{label}: {type(e).__name__}: {str(e)[:80]}")
        finally:
            conn.rollback()
            try:
                cur.execute("DELETE FROM t_blob WHERE id=%s", (row_id,))
                conn.commit()
            except Exception:
                conn.rollback()

    # Part B: UNHEX(%s)/HEX(data) workaround — reliable for full 0x00-0xFF range.
    # Bind the hex-encoded string via %s; UNHEX()/HEX() handle binary I/O
    # server-side, bypassing the client-side bytes escaping path entirely.
    empty = b""
    binary = bytes([0x00, 0x01, 0x02, 0x7F, 0xFF])
    big = bytes(range(256)) * 4
    cur.execute("INSERT INTO t_blob VALUES (1, UNHEX(%s))", (empty.hex(),))
    cur.execute("INSERT INTO t_blob VALUES (2, UNHEX(%s))", (binary.hex(),))
    cur.execute("INSERT INTO t_blob VALUES (3, UNHEX(%s))", (big.hex(),))
    conn.commit()
    # Verify exact byte content, not just length
    cur.execute("SELECT id, HEX(data) FROM t_blob ORDER BY id")
    rows = cur.fetchall()
    got_empty = bytes.fromhex(rows[0][1]) if rows[0][1] else b""
    got_binary = bytes.fromhex(rows[1][1])
    got_big = bytes.fromhex(rows[2][1])
    assert got_empty == empty, f"got {got_empty!r} expected {empty!r}"
    assert got_binary == binary, f"got {got_binary!r} expected {binary!r}"
    assert got_big == big, f"got len={len(got_big)} expected len={len(big)}"
    cur.execute("DROP TABLE t_blob")
    conn.commit()
    conn.close()

    if native_xfail:
        raise XFail(
            "native bytes %s binding is incomplete on openGauss+dolphin — "
            "use UNHEX(%s)/HEX(data) for arbitrary binary data. Details: ["
            + "; ".join(f[:120] for f in native_xfail)
            + "]"
        )


def test_large_text():
    conn = new_conn()
    cur = conn.cursor()
    cur.execute("DROP TABLE IF EXISTS t_largetext")
    cur.execute("CREATE TABLE t_largetext (id INT PRIMARY KEY, content TEXT)")
    big = "A" * 1024 * 1024   # 1 MB
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
    cur.execute("SET time_zone = '+08:00'")
    cur.execute("DROP TABLE IF EXISTS t_dt")
    cur.execute(
        "CREATE TABLE t_dt "
        "(id INT PRIMARY KEY, d DATE, t TIME, dt DATETIME, ts TIMESTAMP)"
    )
    now = datetime.datetime(2026, 8, 12, 16, 0, 0)
    cur.execute(
        "INSERT INTO t_dt VALUES (%s,%s,%s,%s,%s)",
        (1, now.date(), now.time(), now, now),
    )
    conn.commit()
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
    import decimal
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

    conn1.autocommit(False)
    conn2.autocommit(False)
    cur1.execute("UPDATE t_iso SET val=200 WHERE id=1")
    cur2.execute("SELECT val FROM t_iso WHERE id=1")
    assert cur2.fetchone()[0] == 100   # not yet committed

    conn1.commit()
    cur2.execute("SELECT val FROM t_iso WHERE id=1")
    assert cur2.fetchone()[0] == 200   # now visible

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
    xfail_reasons = []

    # Scenario 1: INSERT into nonexistent table.
    # MySQL standard: pymysql.err.ProgrammingError, errno 1146 (ER_NO_SUCH_TABLE)
    # Each scenario uses its own independent connection/transaction.
    conn = new_conn()
    cur = conn.cursor()
    try:
        cur.execute("INSERT INTO nonexistent_table VALUES (1)")
        conn.commit()
        assert False, \
            "Expected an exception for INSERT into nonexistent table, but none raised"
    except AssertionError:
        raise
    except pymysql.err.ProgrammingError as e:
        errno = e.args[0] if e.args else None
        assert errno == 1146, \
            f"Expected errno 1146 (ER_NO_SUCH_TABLE), got errno={errno}"
        print("      table_not_found: ProgrammingError/1146 (MySQL-standard) ✓")
    except pymysql.err.Error as e:
        # Only treat this as a *known* error-mapping difference if the
        # underlying error still clearly indicates the table genuinely does
        # not exist (e.g. openGauss surfacing a PostgreSQL-style
        # undefined_table instead of translating it to MySQL errno 1146).
        # Connection drops, permission errors, protocol errors, etc. do
        # NOT match this signature and must fail the test, not be
        # swallowed into xfail_reasons.
        errno = e.args[0] if e.args else "?"
        msg = str(e)
        msg_lower = msg.lower()
        is_known_table_not_found_mapping_diff = (
            "does not exist" in msg_lower
            or "doesn't exist" in msg_lower
            or "不存在" in msg
        )
        if is_known_table_not_found_mapping_diff:
            xfail_reasons.append(
                f"table_not_found: expected ProgrammingError/1146, "
                f"got {type(e).__name__}(errno={errno}): {msg}"
            )
        else:
            raise
    finally:
        conn.rollback()
    conn.close()

    # Scenario 2: duplicate primary key INSERT.
    # MySQL standard: pymysql.err.IntegrityError, errno 1062 (ER_DUP_ENTRY)
    conn = new_conn()
    cur = conn.cursor()
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
        assert False, \
            "Expected an exception for duplicate key INSERT, but none raised"
    except AssertionError:
        raise
    except pymysql.err.IntegrityError as e:
        errno = e.args[0] if e.args else None
        assert errno == 1062, \
            f"Expected errno 1062 (ER_DUP_ENTRY), got errno={errno}"
        print("      dup_key: IntegrityError/1062 (MySQL-standard) ✓")
    except pymysql.err.Error as e:
        # Only treat this as a *known* error-mapping difference if the
        # underlying error still clearly signals a duplicate-key / unique
        # constraint violation (e.g. openGauss surfacing a PostgreSQL-style
        # unique_violation instead of translating it to MySQL errno 1062).
        # Connection drops, permission errors, protocol errors, etc. do
        # NOT match this signature and must fail the test, not be
        # swallowed into xfail_reasons.
        #
        # NOTE: this is a best-effort text-based heuristic. pymysql error
        # objects here do not reliably expose a SQLSTATE attribute, and the
        # message wording could differ under a non-English server locale;
        # both English and Chinese keywords are checked as a mitigation,
        # but this remains a known limitation for other locales.
        errno = e.args[0] if e.args else None
        msg = str(e)
        msg_lower = msg.lower()
        is_known_dup_mapping_diff = (
            "23505" in msg
            or "duplicate key value violates unique constraint" in msg_lower
            or "重复键值违反唯一约束" in msg
        )
        if is_known_dup_mapping_diff:
            xfail_reasons.append(
                f"dup_key: expected IntegrityError/1062, "
                f"got {type(e).__name__}(errno={errno}): {msg}"
            )
        else:
            raise
    finally:
        conn.rollback()
        try:
            cur.execute("DROP TABLE IF EXISTS t_dup")
            conn.commit()
        except Exception:
            conn.rollback()
    conn.close()

    if xfail_reasons:
        raise XFail(
            "openGauss does not conform to MySQL error-type/errno/SQLSTATE mapping; "
            "pending Plugin fix. Details: " + "; ".join(xfail_reasons)
        )


# ── Runner ─────────────────────────────────────────────────────────────────────

TESTS = [
    ("01 basic connect",           test_basic_connect),
    ("02 CRUD",                    test_crud),
    ("03 parameterized & batch",   test_parameterized_and_batch),
    ("04 autocommit flag",         test_autocommit),
    ("05 commit / rollback",       test_commit_rollback),
    ("06 DictCursor",              test_dict_cursor),
    ("07 SSCursor",                test_ss_cursor),
    ("08 fetchmany",               test_fetchmany),
    ("09 Chinese & Unicode",       test_chinese_unicode),
    ("10 special characters",      test_special_characters),
    ("11 NO_BACKSLASH_ESCAPES",    test_no_backslash_escapes),
    ("12 binary BLOB",             test_binary_blob),
    ("13 large text (1 MB)",       test_large_text),
    ("14 DATE/TIME/DATETIME",      test_datetime_types),
    ("15 DECIMAL",                 test_decimal),
    ("16 transaction isolation",   test_transaction_isolation),
    ("17 reconnect stability x10", test_reconnect_stability),
    ("18 error type mapping",      test_error_mapping),
]

if __name__ == "__main__":
    passed = failed = xfailed = 0
    print(f"\nPyMySQL × openGauss(dolphin) compatibility test\n{'='*52}")
    for name, fn in TESTS:
        result = run(name, fn)
        if result == "pass":
            passed += 1
        elif result == "xfail":
            xfailed += 1
        else:
            failed += 1
    print(f"{'='*52}")
    print(f"Result: {passed} passed, {failed} failed, {xfailed} xfailed (expected) / {len(TESTS)} total\n")
    raise SystemExit(1 if failed else 0)
