/*
 Navicat Premium Data Transfer

 Source Server         : onetrack
 Source Server Type    : MySQL
 Source Server Version : 50531
 Source Host           : 10.38.154.11:3306
 Source Schema         : odc_console

 Target Server Type    : MySQL
 Target Server Version : 50531
 File Encoding         : 65001

 Date: 12/05/2020 20:30:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cluster
-- ----------------------------
DROP TABLE IF EXISTS ` cluster `;
CREATE TABLE ` cluster `
(
    `id` int
(
    11
) NOT NULL AUTO_INCREMENT COMMENT '主键',
    ` type ` varchar
(
    20
) NOT NULL COMMENT '集群类型(HIVE,DORIS,TALOS)',
    ` name ` varchar
(
    200
) NOT NULL COMMENT '集群名字',
    ` cn_name ` varchar
(
    20
) DEFAULT NULL COMMENT '集群中文名',
    ` server_uri ` varchar
(
    200
) NOT NULL COMMENT '集群服务地址',
    ` is_oversea ` varchar
(
    20
) DEFAULT NULL COMMENT '是否海外集群',
    ` doris_dbid ` int
(
    10
) DEFAULT NULL COMMENT 'doris库id',
    ` inuse ` int
(
    1
) NOT NULL COMMENT '是否正在使用',
    ` create_time ` datetime DEFAULT NULL COMMENT '创建时间',
    ` update_time ` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for doris_config
-- ----------------------------
DROP TABLE IF EXISTS ` doris_config `;
CREATE TABLE ` doris_config `
(
    `sql` varchar
(
    400
) DEFAULT NULL COMMENT '建表语句模版',
    ` threshold ` int
(
    200
) DEFAULT NULL COMMENT '增大桶数的阈值',
    ` dbid ` int
(
    100
) DEFAULT NULL COMMENT 'doris库id',
    ` cluster_id ` int
(
    100
) DEFAULT NULL COMMENT '所属集群',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for hive_table_config
-- ----------------------------
DROP TABLE IF EXISTS ` hive_table_config `;
CREATE TABLE ` hive_table_config `
(
    `keytab` varchar
(
    100
) DEFAULT NULL COMMENT '数仓kerberos账号',
    ` privacy_level ` varchar
(
    20
) DEFAULT 'B' COMMENT '隐私级别',
    ` partition_type ` varchar
(
    200
) DEFAULT NULL COMMENT '分区类型',
    ` permission ` varchar
(
    20
) DEFAULT NULL COMMENT '授权权限',
    ` kerberos ` varchar
(
    200
) DEFAULT NULL COMMENT '被授权账号',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_doris_table
-- ----------------------------
DROP TABLE IF EXISTS ` odc_doris_table `;
CREATE TABLE ` odc_doris_table `
(
    `id` int
(
    20
) NOT NULL AUTO_INCREMENT,
    ` app_id ` int
(
    20
) DEFAULT NULL,
    ` name ` varchar
(
    20
) DEFAULT NULL,
    ` status ` int
(
    5
) DEFAULT NULL,
    ` cluster_id ` int
(
    20
) DEFAULT NULL COMMENT '所属集群id',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_doris_update
-- ----------------------------
DROP TABLE IF EXISTS ` odc_doris_update `;
CREATE TABLE ` odc_doris_update `
(
    `id` int
(
    20
) NOT NULL AUTO_INCREMENT,
    ` app_id ` int
(
    20
) DEFAULT NULL,
    ` fields ` varchar
(
    200
) DEFAULT NULL COMMENT '字段变更json',
    ` table_name ` varchar
(
    100
) DEFAULT NULL COMMENT '表名',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_hive_partition
-- ----------------------------
DROP TABLE IF EXISTS ` odc_hive_partition `;
CREATE TABLE ` odc_hive_partition `
(
    `partition_type` varchar
(
    200
) DEFAULT NULL COMMENT '分区类型',
    ` name ` varchar
(
    200
) DEFAULT NULL COMMENT '分区字段名',
    ` type ` varchar
(
    100
) DEFAULT NULL COMMENT '分区字段类型',
    ` comment ` varchar
(
    200
) DEFAULT NULL COMMENT '注释',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_hive_partitiona
-- ----------------------------
DROP TABLE IF EXISTS ` odc_hive_partitiona `;
CREATE TABLE ` odc_hive_partitiona `
(
    `part_id` int
(
    11
) NOT NULL COMMENT '分区id',
    ` table_id ` int
(
    11
) DEFAULT NULL COMMENT '表id',
    ` id ` int
(
    11
) DEFAULT NULL COMMENT '表分区id',
    ` name ` varchar
(
    45
) DEFAULT NULL,
    ` type ` varchar
(
    45
) DEFAULT NULL,
    ` comment ` varchar
(
    45
) DEFAULT NULL,
    PRIMARY KEY
(
    `part_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_hive_table
-- ----------------------------
DROP TABLE IF EXISTS ` odc_hive_table `;
CREATE TABLE ` odc_hive_table `
(
    `id` int
(
    200
) NOT NULL AUTO_INCREMENT,
    ` name ` varchar
(
    200
) DEFAULT NULL COMMENT 'hive表名',
    ` cn_name ` varchar
(
    200
) DEFAULT NULL COMMENT 'hive表中文名',
    ` app_id ` varchar
(
    200
) DEFAULT NULL,
    ` datatype ` varchar
(
    100
) DEFAULT NULL COMMENT '数据类型(log-data/)',
    ` hdfs_file_struct_id ` int
(
    100
) DEFAULT NULL,
    ` keytab ` varchar
(
    200
) DEFAULT NULL COMMENT '数仓kerberos账号',
    ` state ` int
(
    10
) DEFAULT NULL COMMENT '创建状态',
    ` table_id ` int
(
    100
) DEFAULT NULL COMMENT '数据工场资源id',
    ` perm_state ` int
(
    10
) DEFAULT NULL COMMENT '数鲸授权结果',
    ` preserve_time ` int
(
    100
) DEFAULT NULL COMMENT '保存天数',
    ` partition_type ` varchar
(
    200
) DEFAULT NULL COMMENT '分区类型',
    ` cluster_id ` int
(
    100
) DEFAULT NULL COMMENT '所属集群',
    ` create_time ` datetime DEFAULT NULL,
    ` update_time ` datetime DEFAULT NULL,
    PRIMARY KEY
(
    `id`
),
    KEY ` fk_cluster `
(
    `cluster_id`
),
    CONSTRAINT ` fk_cluster ` FOREIGN KEY
(
    `cluster_id`
) REFERENCES ` cluster `
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_hive_table1
-- ----------------------------
DROP TABLE IF EXISTS ` odc_hive_table1 `;
CREATE TABLE ` odc_hive_table1 `
(
    `id` int
(
    11
) NOT NULL,
    ` service ` varchar
(
    45
) CHARACTER SET utf8 DEFAULT NULL COMMENT '业务线',
    ` privacylevel ` varchar
(
    45
) CHARACTER SET utf8 DEFAULT NULL COMMENT '隐私级别',
    ` keytab ` varchar
(
    45
) DEFAULT NULL,
    ` datatype ` varchar
(
    45
) CHARACTER SET utf8 DEFAULT NULL COMMENT 'log-data原始日志\nTemp-data中间日志',
    ` partitiontype ` varchar
(
    45
) CHARACTER SET utf8 DEFAULT NULL COMMENT '分区类型',
    ` preservetime ` varchar
(
    45
) CHARACTER SET utf8 DEFAULT NULL COMMENT '保留时长',
    ` hdfsfilestructureid ` varchar
(
    45
) DEFAULT NULL,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for odc_project
-- ----------------------------
DROP TABLE IF EXISTS ` odc_project `;
CREATE TABLE ` odc_project `
(
    `id` int
(
    11
) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    ` odc_project_name ` varchar
(
    1024
) DEFAULT NULL COMMENT '数据项目名称',
    ` odc_project_appid ` varchar
(
    128
) DEFAULT NULL COMMENT '数据来源app_id',
    ` odc_project_package ` varchar
(
    128
) DEFAULT NULL COMMENT '数据来源app包名',
    ` odc_project_global ` int
(
    11
) DEFAULT NULL COMMENT '是否国际app',
    ` odc_project_desc ` varchar
(
    3072
) DEFAULT NULL COMMENT '项目描述',
    ` odc_project_owner ` varchar
(
    128
) DEFAULT NULL COMMENT '创建项目的owner',
    ` odc_project_department1 ` varchar
(
    1024
) DEFAULT NULL COMMENT '所属一级部门',
    ` odc_project_department2 ` varchar
(
    1024
) DEFAULT NULL COMMENT '所属二级部门',
    ` odc_project_department3 ` varchar
(
    1024
) DEFAULT NULL COMMENT '所属三级部门',
    ` odc_project_users ` varchar
(
    3072
) DEFAULT NULL COMMENT '需要开通权限的邮箱前缀',
    ` odc_project_approved ` int
(
    11
) DEFAULT NULL COMMENT '申请是否审批通过',
    ` odc_project_table_ready ` int
(
    11
) DEFAULT NULL COMMENT '表是否已经创建好',
    ` odc_project_table_type ` int
(
    11
) DEFAULT NULL COMMENT '创建的表类型',
    ` odc_project_table_cluster ` varchar
(
    1024
) DEFAULT NULL,
    ` odc_project_dorisdb_id ` int
(
    11
) DEFAULT NULL COMMENT 'doris库id',
    ` add_time ` datetime DEFAULT NULL COMMENT '添加时间',
    ` update_time ` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COMMENT ='odc_project ';

-- ----------------------------
-- Table structure for odc_talos_topic
-- ----------------------------
DROP TABLE IF EXISTS ` odc_talos_topic `;
CREATE TABLE ` odc_talos_topic `
(
    `app_id` int
(
    20
) DEFAULT NULL,
    ` name ` varchar
(
    200
) DEFAULT NULL COMMENT 'topic名字',
    ` cluster_id ` int
(
    20
) DEFAULT NULL COMMENT '所属集群id',
    ` status ` int
(
    2
) DEFAULT NULL COMMENT '创建成功or失败',
    ` permit_state ` int
(
    2
) DEFAULT NULL COMMENT '授权数鲸team成功or失败',
    ` team_id ` varchar
(
    100
) DEFAULT NULL COMMENT '数鲸teamid',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for project
-- ----------------------------
DROP TABLE IF EXISTS ` project `;
CREATE TABLE ` project `
(
    `id` int
(
    200
) NOT NULL AUTO_INCREMENT,
    ` app_id ` varchar
(
    200
) DEFAULT NULL,
    ` project_name ` varchar
(
    200
) DEFAULT NULL,
    ` owner ` varchar
(
    200
) DEFAULT NULL,
    ` datasize ` int
(
    200
) DEFAULT NULL COMMENT '数据量预估(用于确定doris分桶数)',
    ` isOversea ` int
(
    10
) DEFAULT NULL COMMENT '是否是海外app',
    ` create_time ` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' COMMENT '创建时间',
    ` update_time ` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for talos_config
-- ----------------------------
DROP TABLE IF EXISTS ` talos_config `;
CREATE TABLE ` talos_config `
(
    `teamid_dw` varchar
(
    100
) DEFAULT NULL COMMENT '数仓teamid',
    ` teamid_datawhale ` varchar
(
    100
) DEFAULT NULL COMMENT '数鲸teamid',
    ` key ` varchar
(
    100
) DEFAULT NULL,
    ` secret ` varchar
(
    100
) DEFAULT NULL,
    ` cluster_id ` int
(
    20
) DEFAULT NULL COMMENT '所属集群id',
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS ` user `;
CREATE TABLE ` user `
(
    `id` int
(
    100
) NOT NULL DEFAULT '0',
    ` name ` varchar
(
    100
) DEFAULT NULL,
    ` age ` int
(
    20
) DEFAULT NULL,
    ` email ` varchar
(
    100
) DEFAULT NULL,
    ` create_time ` timestamp NULL DEFAULT NULL,
    ` update_time ` timestamp NULL DEFAULT NULL,
    PRIMARY KEY
(
    `id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
