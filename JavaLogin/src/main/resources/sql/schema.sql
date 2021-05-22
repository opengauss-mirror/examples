CREATE TABLE IF NOT EXISTS  public.user_info(
    username varchar(50) NOT NULL UNIQUE,
    password varchar(2048) NOT NULL,
    name varchar(50) NOT NULL,
    enabled boolean NOT NULL,
    create_date date NOT NULL
);