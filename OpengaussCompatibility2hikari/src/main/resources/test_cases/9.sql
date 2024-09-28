-- DELETE

-- 创建用户架构
CREATE SCHEMA IF NOT EXISTS user1;。

-- 创建客户地址表
CREATE TABLE IF NOT EXISTS user1.customer_address (
    ca_address_sk INT PRIMARY KEY,                -- 地址的唯一标识符
    ca_address_id VARCHAR(16),                     -- 地址ID
    ca_street_number VARCHAR(10),                  -- 街道号码
    ca_street_name VARCHAR(60),                    -- 街道名称
    ca_street_type VARCHAR(15),                    -- 街道类型（如：St, Ave, Rd等）
    ca_suite_number VARCHAR(10),                   -- 套房号（可选）
    ca_city VARCHAR(60),                            -- 城市名称
    ca_county VARCHAR(30),                          -- 县名称
    ca_state VARCHAR(2),                            -- 州/省缩写
    ca_zip VARCHAR(10),                             -- 邮政编码
    ca_country VARCHAR(20),                         -- 国家名称
    ca_gmt_offset DECIMAL(5, 2),                  -- GMT时区偏移
    ca_location_type VARCHAR(20)                   -- 位置类型（如：Residential, Commercial等）
);。

-- 插入示例数据到客户地址表
INSERT INTO user1.customer_address (ca_address_sk, ca_address_id, ca_street_number, ca_street_name, ca_street_type, ca_suite_number, ca_city, ca_county, ca_state, ca_zip, ca_country, ca_gmt_offset, ca_location_type) VALUES
(1, 'A1', '123', 'Main', 'St', '1A', 'Somecity', 'Somecounty', 'SC', '12345', 'USA', -5.00, 'Residential'),
(2, 'A2', '456', 'Second', 'Ave', '2B', 'Othercity', 'Othercounty', 'OC', '54321', 'USA', -5.00, 'Commercial'),
(14887, 'A3', '789', 'Third', 'Blvd', '3C', 'Thirdcity', 'Thirdcounty', 'TC', '67890', 'USA', -5.00, 'Residential'),
(14888, 'A4', '101', 'Fourth', 'Rd', '4D', 'Fourthcity', 'Fourthcounty', 'FC', '09876', 'USA', -5.00, 'Commercial'),
(14889, 'A5', '102', 'Fifth', 'Ln', '5E', 'Fifthcity', 'Fifthcounty', 'FC', '67890', 'USA', -5.00, 'Residential');。

-- 创建客户地址备份表，复制原始表数据
CREATE TABLE user1.customer_address_bak AS TABLE user1.customer_address;。

-- 删除备份表中地址标识小于14888的记录
DELETE FROM user1.customer_address_bak WHERE ca_address_sk < 14888;。

-- 查询备份表，确保删除后数据正确
SELECT * FROM user1.customer_address_bak;。

-- 清空备份表
DELETE FROM user1.customer_address_bak;。

-- 再次查询备份表，确认已清空
SELECT * FROM user1.customer_address_bak;。

-- 从原始表中删除与备份表匹配的记录，且地址标识小于50
DELETE FROM user1.customer_address
USING user1.customer_address_bak
WHERE user1.customer_address.ca_address_sk = user1.customer_address_bak.ca_address_sk
AND user1.customer_address.ca_address_sk < 50;。

-- 使用别名从原始表中删除与备份表匹配的记录，且地址标识小于50
DELETE FROM user1.customer_address a
USING user1.customer_address_bak b
WHERE a.ca_address_sk = b.ca_address_sk
AND a.ca_address_sk < 50;。

-- 删除备份表
DROP TABLE IF EXISTS user1.customer_address_bak CASCADE;。

-- 删除原始客户地址表
DROP TABLE IF EXISTS user1.customer_address CASCADE;。

-- 删除用户架构及其所有内容
DROP SCHEMA IF EXISTS user1 CASCADE;。
