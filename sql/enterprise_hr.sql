-- ====================================================================
-- ⚠️ 已废弃（LEGACY）—— 请勿再执行本脚本
-- ====================================================================
-- 本脚本是项目早期的最小示例（仅 dept / employee 两表、种子数据 8 人），
-- 已被根目录以下两个脚本取代，它们才是当前唯一权威版本：
--   1. enterprise_hr_schema.sql      —— 49 张表 / 80 外键 / 35 唯一键（9 大模块全量）
--   2. enterprise_hr_init_data.sql   —— 初始化数据（可重复执行）
--
-- 正确用法（Windows 务必带 --default-character-set=utf8mb4）：
--   mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_schema.sql
--   mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_init_data.sql
--
-- 设计文档见《数据库设计说明书.md》。
-- 保留本文件仅作历史参考，避免老同事/旧文档引用时一头雾水。
-- ====================================================================

-- ====================================================================
-- Enterprise HR Management - MySQL 8.0 Schema & Seed Data
-- 用法：
--   1. 启动 MySQL 8 服务
--   2. 登录：mysql -u root -p
--   3. 执行：source /path/to/enterprise_hr.sql;
-- ====================================================================

DROP DATABASE IF EXISTS `enterprise_hr`;
CREATE DATABASE `enterprise_hr` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `enterprise_hr`;

-- --------------------------------------------------------------------
-- 1. 部门表
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS `dept`;
CREATE TABLE `dept` (
    `id`         BIGINT       NOT NULL COMMENT '雪花算法主键',
    `name`       VARCHAR(64)  NOT NULL COMMENT '部门名称',
    `leader`     VARCHAR(32)           COMMENT '部门负责人',
    `phone`      VARCHAR(20)           COMMENT '联系电话',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除(0正常 1删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

-- --------------------------------------------------------------------
-- 2. 员工表
-- --------------------------------------------------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id`         BIGINT        NOT NULL COMMENT '雪花算法主键',
    `emp_no`     VARCHAR(32)   NOT NULL COMMENT '员工工号(唯一)',
    `name`       VARCHAR(32)   NOT NULL COMMENT '姓名',
    `gender`     VARCHAR(8)             COMMENT '性别:M/F/U(未知)',
    `age`        INT                   COMMENT '年龄',
    `email`      VARCHAR(64)           COMMENT '邮箱',
    `phone`      VARCHAR(20)           COMMENT '手机号',
    `dept_id`    BIGINT                COMMENT '部门ID',
    `position`   VARCHAR(64)           COMMENT '职位',
    `hire_date`  DATE                  COMMENT '入职日期',
    `salary`     DECIMAL(12,2)         COMMENT '薪资',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1在职 0离职',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_no` (`emp_no`),
    KEY `idx_dept_id` (`dept_id`),
    KEY `idx_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='员工表';

-- --------------------------------------------------------------------
-- 3. 种子数据
-- --------------------------------------------------------------------
INSERT INTO `dept` (`id`, `name`, `leader`, `phone`) VALUES
    (1, '研发部',     '张总监', '13800000001'),
    (2, '产品部',     '李总监', '13800000002'),
    (3, '人力资源部', '王总监', '13800000003'),
    (4, '财务部',     '赵总监', '13800000004');

INSERT INTO `employee`
    (`id`, `emp_no`, `name`, `gender`, `age`, `email`, `phone`, `dept_id`, `position`, `hire_date`, `salary`, `status`)
VALUES
    (1001, 'E0001', '张三', 'M', 28, 'zhangsan@enterprise.com',  '13900000001', 1, 'Java开发工程师', '2023-03-15', 15000.00, 1),
    (1002, 'E0002', '李四', 'F', 26, 'lisi@enterprise.com',      '13900000002', 1, '前端开发工程师', '2023-05-20', 14000.00, 1),
    (1003, 'E0003', '王五', 'M', 32, 'wangwu@enterprise.com',     '13900000003', 2, '产品经理',       '2022-08-10', 18000.00, 1),
    (1004, 'E0004', '赵六', 'F', 29, 'zhaoliu@enterprise.com',    '13900000004', 3, 'HR专员',         '2024-01-05', 10000.00, 1),
    (1005, 'E0005', '钱七', 'M', 35, 'qianqi@enterprise.com',     '13900000005', 4, '会计',           '2021-11-20', 16000.00, 1),
    (1006, 'E0006', '孙八', 'M', 24, 'sunba@enterprise.com',      '13900000006', 1, '测试工程师',     '2024-06-01', 12000.00, 1),
    (1007, 'E0007', '周九', 'F', 27, 'zhoujiu@enterprise.com',    '13900000007', 2, '产品助理',       '2024-03-10', 11000.00, 1),
    (1008, 'E0008', '吴十', 'M', 30, 'wushi@enterprise.com',      '13900000008', 3, 'HR经理',         '2022-04-25', 20000.00, 0);
