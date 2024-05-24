ALTER TABLE `成绩` DROP FOREIGN KEY `fk_学生`;
ALTER TABLE `成绩` DROP FOREIGN KEY `fk_成绩_课程_1`;

DROP TABLE `学生`;
DROP TABLE `课程`;
DROP TABLE `成绩`;

CREATE TABLE `学生` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`姓名` varchar(255) NULL,
`年龄` int(255) NULL,
PRIMARY KEY (`id`) 
);

CREATE TABLE `课程` (
`id` int(11) NOT NULL AUTO_INCREMENT,
`学科` varchar(255) NULL,
PRIMARY KEY (`id`) 
);

CREATE TABLE `成绩` (
`id` int(11) NOT NULL,
`学生id` int(11) NULL,
`课程id` int(11) NULL,
`分数` int(255) NULL,
PRIMARY KEY (`id`) 
);


ALTER TABLE `成绩` ADD CONSTRAINT `fk_学生` FOREIGN KEY (`学生id`) REFERENCES `学生` (`id`);
ALTER TABLE `成绩` ADD CONSTRAINT `fk_成绩_课程_1` FOREIGN KEY (`课程id`) REFERENCES `课程` (`id`);

