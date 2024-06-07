CREATE DATABASE cloud;
USE cloud;
CREATE TABLE instances (id int AUTO_INCREMENT PRIMARY KEY,
                        organization_uuid varchar(255),
                        instance_uuid varchar(255),
                        price_second int,
                        started TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
