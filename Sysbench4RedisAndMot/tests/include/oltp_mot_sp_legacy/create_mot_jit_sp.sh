#!/bin/bash


for i in {1..24}
do
gsql -d testdb -p 5432 << EOF
    
    CREATE OR REPLACE PROCEDURE select_c_from_sbtest$i(IN id_param INT, OUT out1 TEXT) AS
    DECLARE
        pad_val TEXT;
        i INT := id_param;
        i_end INT := i+10;
    BEGIN
        FOR x IN i..i_end LOOP
            SELECT c INTO pad_val FROM sbtest$i WHERE id = x;
        END LOOP;
    END;
    /

    CREATE OR REPLACE PROCEDURE select_c_where_sbtest$i(IN a_param INT, IN b_param INT, OUT out1 TEXT) AS
    BEGIN
        PERFORM c FROM sbtest$i WHERE id BETWEEN a_param AND b_param;
        out1 := '0';
    END;
    /

    CREATE OR REPLACE PROCEDURE select_sum_sbtest$i(IN a_param INT, IN b_param INT, OUT out1 TEXT) AS
    DECLARE
        pad_val INT;
    BEGIN
        SELECT SUM(K) INTO pad_val FROM sbtest$i WHERE id BETWEEN a_param AND b_param;
        out1 := '0';
    END;
    /

    CREATE OR REPLACE PROCEDURE select_c_where_order_sbtest$i(IN a_param INT, IN b_param INT, OUT out1 TEXT) AS
    BEGIN
        PERFORM c FROM sbtest$i WHERE id BETWEEN a_param AND b_param ORDER BY c;
        out1 := '0';
    END;
    /

    CREATE OR REPLACE PROCEDURE insert_into1_sbtest$i(IN k_param INT, IN c_param TEXT, IN pad_param TEXT, OUT out1 TEXT) AS
    BEGIN
        INSERT INTO sbtest$i (k, c, pad) VALUES (k_param, c_param, pad_param);
        out1 := '1';
    END;
    /

    CREATE OR REPLACE PROCEDURE insert_into2_sbtest$i(IN id_param INT, IN k_param INT, IN c_param TEXT, IN pad_param TEXT, OUT out1 TEXT) AS
    BEGIN
        INSERT INTO sbtest$i (id, k, c, pad) VALUES (id_param, k_param, c_param, pad_param);
        out1 := '1';
    END;
    /

    CREATE OR REPLACE PROCEDURE insert_into_sbtest$i(IN id_param INT, IN k_param INT, IN c_param TEXT, IN pad_param TEXT, OUT out1 TEXT) AS
    BEGIN
        INSERT INTO sbtest$i (id, k, c, pad) VALUES (id_param, k_param, c_param, pad_param);
        out1 := '1';
    END;
    /

    CREATE OR REPLACE PROCEDURE update_sbtest$i(IN c_param TEXT, IN id_param INT, OUT out1 TEXT) AS
    BEGIN
        UPDATE sbtest$i SET c = c_param WHERE id = id_param;
        out1 := '1';
    END;
    /

    CREATE OR REPLACE PROCEDURE delete_from_sbtest$i(IN id_param INT, OUT out1 TEXT) AS
    BEGIN
        DELETE FROM sbtest$i WHERE id = id_param;
        out1 := '1';
    END;
    /

    CREATE OR REPLACE PROCEDURE sp_sbtest$i(IN id_param INT, IN k_param INT, IN c_param TEXT, IN pad_param TEXT, OUT out1 TEXT) AS
    DECLARE
        pad_val TEXT;
        i INT := id_param;
        i_end INT := i+10;
    BEGIN
        FOR x IN i..i_end LOOP
            SELECT c INTO pad_val FROM sbtest$i WHERE id = x;
        END LOOP;
        UPDATE sbtest$i SET c = c_param WHERE id = id_param;
        DELETE FROM sbtest$i WHERE id = id_param;
        INSERT INTO sbtest$i (id, k, c, pad) VALUES (id_param, k_param, c_param, pad_param);
        
        out1 := '0';
        EXCEPTION WHEN OTHERS THEN out1 := '0'; END;
    END;
    /
EOF
# echo $i
done
