-- auto Generated on 2020-05-16
-- DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id INT (11) NOT NULL AUTO_INCREMENT COMMENT 'id', `
    name
    `
    VARCHAR
(
    50
) NOT NULL DEFAULT '' COMMENT 'name',
    age INT (11) NOT NULL DEFAULT -1 COMMENT 'age',
    PRIMARY KEY (id)
    )ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'person';
