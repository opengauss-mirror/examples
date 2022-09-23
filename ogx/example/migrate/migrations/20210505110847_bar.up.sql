CREATE TABLE test (
  id bigint PRIMARY KEY
);

--ogx:split

ALTER TABLE test ADD COLUMN name varchar(100);
