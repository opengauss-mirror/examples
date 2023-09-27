create database gb18030 encoding='gb18030' LC_COLLATE='zh_CN.GB18030' LC_CTYPE ='zh_CN.GB18030' TEMPLATE=template0;
\c gb18030

show server_encoding;
create table tb_test(id int, content text);

insert into tb_test values(1, 'abcdefghigkABCDEFGHIJK');
insert into tb_test values(2, '�1�2');
insert into tb_test values(3, '���ĺ���');
insert into tb_test values(4, '�0�1�9�8�0�8');
insert into tb_test values(5, '�9�4 �9�5 �9�2 �9�4 �9�7 �9�8 �9�9 �9�0');
insert into tb_test values(5, '�2�5');

select * from tb_test order by id;
select convert_to(content, 'utf8') from tb_test order by id;

drop table tb_test;
\c regression
clean connection to all force for database gb18030;
drop database gb18030;
