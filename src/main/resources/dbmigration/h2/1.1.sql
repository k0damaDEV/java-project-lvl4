-- apply changes
alter table url_check alter column title varchar(255);
alter table url_check alter column h1 varchar(255);
alter table url_check alter column description clob;
