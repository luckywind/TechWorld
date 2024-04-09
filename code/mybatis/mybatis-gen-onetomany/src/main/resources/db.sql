/*
 Navicat Premium Data Transfer

 Source Server         : local
 Source Server Type    : MySQL
 Source Server Version : 50727
 Source Host           : localhost:3306
 Source Schema         : mybatis

 Target Server Type    : MySQL
 Target Server Version : 50727
 File Encoding         : 65001

 Date: 11/05/2020 20:13:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS ` department `;
CREATE TABLE ` department `
(
    `id` int
(
    11
) NOT NULL AUTO_INCREMENT,
    ` name ` varchar
(
    255
) DEFAULT NULL,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for employee
-- ----------------------------
DROP TABLE IF EXISTS ` employee `;
CREATE TABLE ` employee `
(
    `id` int
(
    11
) NOT NULL AUTO_INCREMENT,
    ` name ` varchar
(
    255
) DEFAULT NULL,
    ` dpt_id ` int
(
    11
) DEFAULT NULL,
    PRIMARY KEY
(
    `id`
),
    KEY ` fk_employee `
(
    `dpt_id`
),
    CONSTRAINT ` fk_employee ` FOREIGN KEY
(
    `dpt_id`
) REFERENCES ` department `
(
    `id`
)
    ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
