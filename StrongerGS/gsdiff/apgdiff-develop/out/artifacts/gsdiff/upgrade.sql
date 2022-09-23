
CREATE SEQUENCE serial1
	START WITH 1
	INCREMENT BY 1
	NO MAXVALUE
	NO MINVALUE
	CACHE 20;

CREATE TABLE test (
	t integer
)
WITH (orientation=row, compression=no);

ALTER TABLE test OWNER TO yy;

CREATE TABLE test_trigger (
	customer_id integer,
	account_id integer,
	first_name character(10),
	last_name character varying(20)
)
WITH (orientation=row, compression=no);

ALTER TABLE test_trigger OWNER TO omm;

ALTER TABLE customer
	ALTER COLUMN customer_id SET DEFAULT nextval('serial1'::regclass),
	ALTER COLUMN last_name TYPE character varying(20) USING last_name::character varying(20) /* TYPE change - table: customer original: character(10) new: character varying(20) */,
	ALTER COLUMN last_name SET DEFAULT ' '::character varying;

ALTER TABLE ONLY customer ALTER COLUMN first_name SET STORAGE MAIN;
ALTER SEQUENCE serial1
	OWNED BY customer.customer_id;

CREATE OR REPLACE FUNCTION tri_insert_func() RETURNS trigger
    LANGUAGE plpgsql NOT SHIPPABLE
 AS $$
           DECLARE
           BEGIN
                   INSERT INTO test_trigger  VALUES(NEW.account_id, NEW.first_name, NEW.last_name);
                   RETURN NEW;
           END
           $$;

CREATE INDEX first_name_index ON customer USING btree (first_name) TABLESPACE pg_default;

ALTER TABLE customer CLUSTER ON first_name_index;

CREATE TRIGGER insert_trigger
	BEFORE INSERT ON customer
	FOR EACH ROW
	EXECUTE PROCEDURE tri_insert_func();

ALTER TABLE customer DISABLE TRIGGER insert_trigger;


CREATE VIEW myview (account_id, first_name) AS
	SELECT customer.account_id, customer.first_name FROM customer;

ALTER VIEW myview OWNER TO omm;

CREATE RULE "_RETURN" AS
 ON SELECT TO customer
 DO INSTEAD SELECT test_trigger.customer_id, test_trigger.account_id, test_trigger.first_name, test_trigger.last_name FROM test_trigger;

CREATE RULE "_RETURN" AS
 ON SELECT TO test_trigger
 DO INSTEAD SELECT customer.customer_id, customer.account_id, customer.first_name, customer.last_name FROM customer;
