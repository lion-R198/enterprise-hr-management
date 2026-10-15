-- ====================================================================================
--  企业人事管理系统 (Enterprise HR Management System) — 完整数据库建表脚本
-- ====================================================================================
--  数据库：enterprise_hr    引擎：InnoDB    字符集：utf8mb4 / utf8mb4_0900_ai_ci
--  目标版本：MySQL 8.0+
--  设计依据：《企业人事管理系统需求规格说明书》(9 模块 / 52 功能点)
-- ------------------------------------------------------------------------------------
--  用法：
--      mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_schema.sql
--  或在 MySQL 客户端中：
--      source /path/to/enterprise_hr_schema.sql
--
--  ⚠️ Windows 环境务必加 --default-character-set=utf8mb4
--     否则客户端默认 gbk，UTF-8 中文注释与数据会乱码甚至报 Data too long。
-- ------------------------------------------------------------------------------------
--  统一设计约定：
--   1. 主键        id BIGINT AUTO_INCREMENT，业务编号（工号/单号/编码）另设唯一键
--   2. 逻辑删除    deleted TINYINT（0 未删除 / 1 已删除）；日志类表不做逻辑删除
--   3. 时间字段    created_at / updated_at 由数据库默认值 + ON UPDATE 自动维护
--   4. 外键命名    fk_<表>_<字段>      索引命名 idx_<表>_<字段>    唯一键 uk_<表>_<字段>
--   5. 外键行为    主数据被引用一律 RESTRICT（禁止误删），从属明细表用 CASCADE
--   6. 金额精度    DECIMAL(12,2)，比例 DECIMAL(6,4)，天数 DECIMAL(6,1)
--   7. 字典字段    以 TINYINT 状态码存储，取值含义写在字段注释中
-- ====================================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS `enterprise_hr`;
CREATE DATABASE `enterprise_hr`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE `enterprise_hr`;


-- ####################################################################################
--  【模块 09】系统设置 —— 基础支撑（10 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-09-04 数据字典】sys_dict_type 字典类型表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_type`  VARCHAR(64)  NOT NULL                COMMENT '字典类型编码(唯一),如 education/gender/leave_type',
    `dict_name`  VARCHAR(64)  NOT NULL                COMMENT '字典类型名称,如 学历',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`     VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`),
    KEY `idx_dict_type_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典类型表';


-- ------------------------------------------------------------------------------------
-- 【F-09-04 数据字典】sys_dict_item 字典项表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_type_id` BIGINT       NOT NULL                COMMENT '所属字典类型ID(sys_dict_type.id)',
    `item_value`   VARCHAR(64)  NOT NULL                COMMENT '字典项值(存库值),如 3',
    `item_label`   VARCHAR(64)  NOT NULL                COMMENT '字典项显示文本,如 本科',
    `sort_no`      INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `is_default`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否默认选中:1是 0否',
    `css_class`    VARCHAR(64)           DEFAULT NULL   COMMENT '前端样式类(标签颜色等)',
    `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`       VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_item_type_value` (`dict_type_id`, `item_value`),
    KEY `idx_dict_item_type` (`dict_type_id`),
    CONSTRAINT `fk_dict_item_type` FOREIGN KEY (`dict_type_id`) REFERENCES `sys_dict_type` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '字典项表';


-- ------------------------------------------------------------------------------------
-- 【F-09-03 菜单管理】sys_menu 菜单权限表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`  BIGINT                DEFAULT NULL   COMMENT '父菜单ID(NULL=顶级菜单)',
    `menu_name`  VARCHAR(64)  NOT NULL                COMMENT '菜单名称',
    `menu_type`  TINYINT      NOT NULL DEFAULT 2      COMMENT '菜单类型:1目录 2菜单 3按钮',
    `path`       VARCHAR(200)          DEFAULT NULL   COMMENT '前端路由地址,如 /employee',
    `component`  VARCHAR(200)          DEFAULT NULL   COMMENT '前端组件路径',
    `perms`      VARCHAR(100)          DEFAULT NULL   COMMENT '权限标识,如 employee:list',
    `icon`       VARCHAR(100)          DEFAULT NULL   COMMENT '菜单图标',
    `sort_no`    INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `visible`    TINYINT      NOT NULL DEFAULT 1      COMMENT '是否显示:1显示 0隐藏',
    `status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_menu_perms` (`perms`),
    KEY `idx_menu_parent` (`parent_id`),
    CONSTRAINT `fk_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `sys_menu` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表';


-- ------------------------------------------------------------------------------------
-- 【F-09-02 角色权限】sys_role 角色表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_code`        VARCHAR(64)  NOT NULL                COMMENT '角色编码(唯一),如 ROLE_HR',
    `role_name`        VARCHAR(64)  NOT NULL                COMMENT '角色名称,如 人事专员',
    `description`      VARCHAR(255)          DEFAULT NULL   COMMENT '角色职责描述',
    `data_scope`       TINYINT      NOT NULL DEFAULT 4      COMMENT '数据范围:1全部 2本部门 3本部门及下级 4仅本人 5自定义',
    `data_scope_depts` VARCHAR(500)          DEFAULT NULL   COMMENT '自定义数据范围部门ID列表,逗号分隔',
    `role_type`        TINYINT      NOT NULL DEFAULT 2      COMMENT '角色类型:1系统内置(不可删除) 2自定义',
    `sort_no`          INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `status`           TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`           VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    KEY `idx_role_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表';


-- ------------------------------------------------------------------------------------
-- 【F-09-05 参数配置】sys_config 系统参数表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_key`   VARCHAR(100) NOT NULL                COMMENT '参数键(唯一),如 attendance.lock.day',
    `config_name`  VARCHAR(100) NOT NULL                COMMENT '参数名称,如 考勤封账日',
    `config_value` VARCHAR(500) NOT NULL                COMMENT '参数值',
    `config_group` VARCHAR(50)  NOT NULL DEFAULT 'system' COMMENT '参数分组:attendance考勤/salary薪资/recruit招聘/system系统',
    `value_type`   TINYINT      NOT NULL DEFAULT 1      COMMENT '值类型:1字符串 2数字 3布尔 4JSON',
    `is_system`    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否系统内置:1是(禁止删除) 0否',
    `remark`       VARCHAR(255)          DEFAULT NULL   COMMENT '备注说明',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_config_group` (`config_group`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统参数表';


-- ------------------------------------------------------------------------------------
-- 【F-09-06 日志审计】sys_login_log 登录日志表（纯追加，不做逻辑删除）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT                DEFAULT NULL   COMMENT '用户ID(登录失败时可能为空,故不设外键)',
    `username`       VARCHAR(64)  NOT NULL                COMMENT '登录账号',
    `login_ip`       VARCHAR(64)           DEFAULT NULL   COMMENT '登录IP',
    `login_location` VARCHAR(100)          DEFAULT NULL   COMMENT '登录地点',
    `browser`        VARCHAR(100)          DEFAULT NULL   COMMENT '浏览器',
    `os`             VARCHAR(100)          DEFAULT NULL   COMMENT '操作系统',
    `login_status`   TINYINT      NOT NULL DEFAULT 1      COMMENT '登录状态:1成功 0失败',
    `message`        VARCHAR(255)          DEFAULT NULL   COMMENT '提示信息',
    `login_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (`id`),
    KEY `idx_login_log_user` (`user_id`),
    KEY `idx_login_log_time` (`login_time`),
    KEY `idx_login_log_name` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '登录日志表';


-- ------------------------------------------------------------------------------------
-- 【F-09-06 日志审计】sys_oper_log 操作日志表（纯追加，不做逻辑删除）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT                 DEFAULT NULL   COMMENT '操作人用户ID',
    `username`        VARCHAR(64)            DEFAULT NULL   COMMENT '操作人账号',
    `module`          VARCHAR(64)            DEFAULT NULL   COMMENT '所属模块,如 员工信息',
    `oper_type`       TINYINT       NOT NULL DEFAULT 7      COMMENT '操作类型:1新增 2修改 3删除 4查询 5导出 6导入 7其他',
    `oper_desc`       VARCHAR(255)           DEFAULT NULL   COMMENT '操作描述',
    `request_method`  VARCHAR(10)            DEFAULT NULL   COMMENT 'HTTP方法:GET/POST/PUT/DELETE',
    `request_url`     VARCHAR(255)           DEFAULT NULL   COMMENT '请求地址',
    `request_param`   TEXT                   DEFAULT NULL   COMMENT '请求参数(JSON)',
    `response_result` TEXT                   DEFAULT NULL   COMMENT '响应结果(截断存储)',
    `oper_ip`         VARCHAR(64)            DEFAULT NULL   COMMENT '操作IP',
    `oper_status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '操作状态:1成功 0失败',
    `error_msg`       VARCHAR(1000)          DEFAULT NULL   COMMENT '异常信息',
    `cost_time`       BIGINT                 DEFAULT NULL   COMMENT '耗时(毫秒)',
    `oper_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_oper_log_user` (`user_id`),
    KEY `idx_oper_log_time` (`oper_time`),
    KEY `idx_oper_log_module` (`module`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表';


-- ####################################################################################
--  【模块 04】考勤管理 —— 基础规则表（3 张）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-04-05 排班管理】shift 班次定义表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `shift`;
CREATE TABLE `shift` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `shift_code`      VARCHAR(32)  NOT NULL                COMMENT '班次编码(唯一),如 STD/DAY/NIGHT',
    `shift_name`      VARCHAR(64)  NOT NULL                COMMENT '班次名称,如 标准班',
    `work_start`      TIME         NOT NULL DEFAULT '09:00:00' COMMENT '上班时间',
    `work_end`        TIME         NOT NULL DEFAULT '18:00:00' COMMENT '下班时间',
    `rest_minutes`    INT          NOT NULL DEFAULT 60      COMMENT '休息时长(分钟)',
    `standard_hours`  DECIMAL(4,1) NOT NULL DEFAULT 8.0     COMMENT '标准工时(小时)',
    `late_tolerance`  INT          NOT NULL DEFAULT 5       COMMENT '迟到容忍(分钟),超出计迟到',
    `early_tolerance` INT          NOT NULL DEFAULT 5       COMMENT '早退容忍(分钟)',
    `flex_start`      INT          NOT NULL DEFAULT 0       COMMENT '弹性上班提前分钟数',
    `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态:1启用 0停用',
    `sort_no`         INT          NOT NULL DEFAULT 0       COMMENT '显示排序,升序',
    `remark`          VARCHAR(255)          DEFAULT NULL    COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0       COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_shift_code` (`shift_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '班次定义表';


-- ------------------------------------------------------------------------------------
-- 【F-04-02 请假申请】leave_type 假别表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `leave_type`;
CREATE TABLE `leave_type` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type_code`         VARCHAR(32)  NOT NULL                COMMENT '假别编码(唯一),如 ANNUAL/SICK',
    `type_name`         VARCHAR(32)  NOT NULL                COMMENT '假别名称,如 年假',
    `is_paid`           TINYINT      NOT NULL DEFAULT 1      COMMENT '是否带薪:1带薪 0不带薪',
    `unit`              TINYINT      NOT NULL DEFAULT 1      COMMENT '计量单位:1天 2小时',
    `min_unit`          DECIMAL(4,1) NOT NULL DEFAULT 0.5    COMMENT '最小请假单位(天),如 0.5 表示半天',
    `max_days_per_year` DECIMAL(6,1) NOT NULL DEFAULT 0      COMMENT '每年额度上限(0=不限)',
    `valid_months`      INT          NOT NULL DEFAULT 12     COMMENT '额度有效月数',
    `can_carry_over`    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否可结转下年:1是 0否',
    `need_proof`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否需上传证明:1是 0否',
    `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `sort_no`           INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `remark`            VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_leave_type_code` (`type_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '假别表';


-- ------------------------------------------------------------------------------------
-- 【F-05-01 薪资标准】salary_standard 薪资标准模板表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `salary_standard`;
CREATE TABLE `salary_standard` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `standard_code`      VARCHAR(32)   NOT NULL                COMMENT '标准编码(唯一),如 STD-TECH-P5',
    `standard_name`      VARCHAR(64)   NOT NULL                COMMENT '标准名称,如 P5技术岗标准',
    `position_seq`       TINYINT                DEFAULT NULL   COMMENT '适用岗位序列:1管理 2技术 3职能 4操作',
    `job_level`          VARCHAR(16)            DEFAULT NULL   COMMENT '适用职级,如 P5/M2',
    `base_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '基本工资',
    `post_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '岗位工资',
    `performance_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '绩效工资(基数)',
    `allowance`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '固定补贴合计',
    `effective_date`     DATE                   DEFAULT NULL   COMMENT '生效日期',
    `status`             TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`             VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_salary_standard_code` (`standard_code`),
    KEY `idx_salary_standard_level` (`position_seq`, `job_level`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '薪资标准模板表';


-- ####################################################################################
--  【模块 03】部门管理 + 【模块 02】员工信息（6 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-03-01/02 组织架构/部门维护】dept 部门表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `dept`;
CREATE TABLE `dept` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_code`       VARCHAR(32)  NOT NULL                COMMENT '部门编码(全局唯一),如 D1001',
    `name`            VARCHAR(64)  NOT NULL                COMMENT '部门名称',
    `parent_id`       BIGINT                DEFAULT NULL   COMMENT '上级部门ID(NULL=顶级部门)',
    `leader`          VARCHAR(32)           DEFAULT NULL   COMMENT '部门负责人姓名(快照,便于快速展示)',
    `leader_id`       BIGINT                DEFAULT NULL   COMMENT '部门负责人(员工ID,外键后置添加)',
    `dept_type`       TINYINT      NOT NULL DEFAULT 3      COMMENT '组织类型:1公司 2中心/事业部 3部门 4班组',
    `dept_level`      TINYINT      NOT NULL DEFAULT 1      COMMENT '层级深度,顶级=1',
    `dept_path`       VARCHAR(255) NOT NULL DEFAULT ''     COMMENT '层级路径,如 /1/5/9/,便于递归查询',
    `headcount_limit` INT          NOT NULL DEFAULT 0      COMMENT '编制上限(0=不限)',
    `phone`           VARCHAR(20)           DEFAULT NULL   COMMENT '部门联系电话',
    `sort_no`         INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1正常 0停用',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`),
    UNIQUE KEY `uk_dept_name` (`name`),
    KEY `idx_dept_parent` (`parent_id`),
    KEY `idx_dept_leader` (`leader_id`),
    KEY `idx_dept_status` (`status`),
    CONSTRAINT `fk_dept_parent` FOREIGN KEY (`parent_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表';


-- ------------------------------------------------------------------------------------
-- 【F-03-03 岗位管理】position 岗位表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `position`;
CREATE TABLE `position` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `position_code`   VARCHAR(32)  NOT NULL                COMMENT '岗位编码(全局唯一),如 P-TECH-001',
    `position_name`   VARCHAR(64)  NOT NULL                COMMENT '岗位名称,如 Java开发工程师',
    `position_seq`    TINYINT      NOT NULL DEFAULT 2      COMMENT '岗位序列:1管理 2技术 3职能 4操作',
    `job_level`       VARCHAR(16)           DEFAULT NULL   COMMENT '职级范围,如 P4-P7',
    `dept_id`         BIGINT                DEFAULT NULL   COMMENT '所属部门ID(NULL=公司通用岗位)',
    `duty`            VARCHAR(500)          DEFAULT NULL   COMMENT '岗位职责',
    `requirement`     VARCHAR(500)          DEFAULT NULL   COMMENT '任职要求',
    `headcount_limit` INT          NOT NULL DEFAULT 0      COMMENT '岗位编制上限(0=不限)',
    `sort_no`         INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`          VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_code` (`position_code`),
    KEY `idx_position_dept` (`dept_id`),
    KEY `idx_position_seq` (`position_seq`),
    CONSTRAINT `fk_position_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '岗位表';


-- ------------------------------------------------------------------------------------
-- 【F-02-01 员工档案】employee 员工表（系统核心主数据表）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `emp_no`           VARCHAR(32)   NOT NULL                COMMENT '员工工号(唯一,不可修改),如 E0001',
    `name`             VARCHAR(32)   NOT NULL                COMMENT '姓名',
    `gender`           VARCHAR(8)             DEFAULT NULL   COMMENT '性别:M男 F女 U未知',
    `age`              INT                    DEFAULT NULL   COMMENT '年龄(冗余快照,可由birth_date推算)',
    `birth_date`       DATE                   DEFAULT NULL   COMMENT '出生日期',
    `id_card`          VARCHAR(32)            DEFAULT NULL   COMMENT '身份证号(加密存储)',
    `nation`           VARCHAR(20)            DEFAULT NULL   COMMENT '民族,如 汉族',
    `native_place`     VARCHAR(64)            DEFAULT NULL   COMMENT '籍贯',
    `marital_status`   TINYINT                DEFAULT NULL   COMMENT '婚姻状况:1未婚 2已婚 3离异 4丧偶',
    `political_status` TINYINT                DEFAULT NULL   COMMENT '政治面貌:1群众 2共青团员 3中共党员 4民主党派',
    `phone`            VARCHAR(20)            DEFAULT NULL   COMMENT '手机号',
    `email`            VARCHAR(64)            DEFAULT NULL   COMMENT '电子邮箱',
    `emergency_contact` VARCHAR(32)           DEFAULT NULL   COMMENT '紧急联系人姓名',
    `emergency_phone`  VARCHAR(20)            DEFAULT NULL   COMMENT '紧急联系人电话',
    `address`          VARCHAR(255)           DEFAULT NULL   COMMENT '现居住地址',
    `education`        TINYINT                DEFAULT NULL   COMMENT '最高学历:1博士 2硕士 3本科 4大专 5中专 6高中 7其他',
    `graduate_school`  VARCHAR(64)            DEFAULT NULL   COMMENT '毕业院校',
    `major`            VARCHAR(64)            DEFAULT NULL   COMMENT '所学专业',
    `graduate_date`    DATE                   DEFAULT NULL   COMMENT '毕业日期',
    `dept_id`          BIGINT                 DEFAULT NULL   COMMENT '所属部门ID',
    `position_id`      BIGINT                 DEFAULT NULL   COMMENT '所属岗位ID',
    `position`         VARCHAR(64)            DEFAULT NULL   COMMENT '职位名称(冗余快照,避免列表频繁联表)',
    `job_level`        VARCHAR(16)            DEFAULT NULL   COMMENT '职级,如 P5/M2',
    `employee_type`    TINYINT       NOT NULL DEFAULT 1      COMMENT '用工性质:1正式 2试用 3实习 4兼职 5劳务',
    `hire_date`        DATE                   DEFAULT NULL   COMMENT '入职日期',
    `regular_date`     DATE                   DEFAULT NULL   COMMENT '转正日期',
    `work_start_date`  DATE                   DEFAULT NULL   COMMENT '参加工作时间',
    `leave_date`       DATE                   DEFAULT NULL   COMMENT '离职日期',
    `leave_reason`     VARCHAR(255)           DEFAULT NULL   COMMENT '离职原因',
    `status`           TINYINT       NOT NULL DEFAULT 1      COMMENT '在职状态:0离职 1在职 2试用 3待入职',
    `manager_id`       BIGINT                 DEFAULT NULL   COMMENT '直属主管(员工ID)',
    `salary`           DECIMAL(12,2)          DEFAULT NULL   COMMENT '当前基本薪资(冗余快照,明细见employee_salary)',
    `bank_name`        VARCHAR(64)            DEFAULT NULL   COMMENT '开户银行',
    `bank_account`     VARCHAR(32)            DEFAULT NULL   COMMENT '银行卡号(加密存储)',
    `avatar`           VARCHAR(255)           DEFAULT NULL   COMMENT '头像地址',
    `remark`           VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `created_by`       BIGINT                 DEFAULT NULL   COMMENT '创建人用户ID',
    `updated_by`       BIGINT                 DEFAULT NULL   COMMENT '最后修改人用户ID',
    `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_emp_no` (`emp_no`),
    UNIQUE KEY `uk_emp_id_card` (`id_card`),
    KEY `idx_emp_name` (`name`),
    KEY `idx_emp_dept` (`dept_id`),
    KEY `idx_emp_position` (`position_id`),
    KEY `idx_emp_manager` (`manager_id`),
    KEY `idx_emp_status` (`status`),
    KEY `idx_emp_hire_date` (`hire_date`),
    CONSTRAINT `fk_emp_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_emp_position` FOREIGN KEY (`position_id`) REFERENCES `position` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_emp_manager` FOREIGN KEY (`manager_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工表';


-- ------------------------------------------------------------------------------------
-- 【F-02-05 合同管理】employee_contract 劳动合同表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee_contract`;
CREATE TABLE `employee_contract` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `contract_no`    VARCHAR(64)   NOT NULL                COMMENT '合同编号(唯一)',
    `employee_id`    BIGINT        NOT NULL                COMMENT '员工ID',
    `contract_type`  TINYINT       NOT NULL DEFAULT 1      COMMENT '合同类型:1固定期限 2无固定期限 3以完成任务为期限 4实习协议 5劳务协议',
    `first_party`    VARCHAR(128)           DEFAULT NULL   COMMENT '甲方(公司名称)',
    `second_party`   VARCHAR(32)            DEFAULT NULL   COMMENT '乙方(员工姓名)',
    `sign_date`      DATE                   DEFAULT NULL   COMMENT '签订日期',
    `start_date`     DATE          NOT NULL                COMMENT '合同生效日期',
    `end_date`       DATE                   DEFAULT NULL   COMMENT '合同到期日期(NULL=无固定期限)',
    `term_years`     DECIMAL(4,1)           DEFAULT NULL   COMMENT '合同期限(年)',
    `salary_agreed`  DECIMAL(12,2)          DEFAULT NULL   COMMENT '合同约定薪资',
    `probation_months` INT         NOT NULL DEFAULT 0      COMMENT '试用期月数',
    `renew_from_id`  BIGINT                 DEFAULT NULL   COMMENT '续签来源合同ID(上一份合同)',
    `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '合同状态:1生效中 2已到期 3已续签 4已解除 5已终止',
    `attachment_url` VARCHAR(255)           DEFAULT NULL   COMMENT '合同扫描件地址',
    `remark`         VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_contract_no` (`contract_no`),
    KEY `idx_contract_emp` (`employee_id`),
    KEY `idx_contract_end` (`end_date`, `status`),
    CONSTRAINT `fk_contract_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_contract_renew` FOREIGN KEY (`renew_from_id`) REFERENCES `employee_contract` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '劳动合同表';


-- ------------------------------------------------------------------------------------
-- 【F-02-03 调动转岗】employee_change 员工异动记录表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee_change`;
CREATE TABLE `employee_change` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`      BIGINT        NOT NULL                COMMENT '员工ID',
    `change_type`      TINYINT       NOT NULL                COMMENT '异动类型:1入职 2转正 3调岗 4调部门 5晋升 6降级 7离职 8返聘',
    `before_dept_id`   BIGINT                 DEFAULT NULL   COMMENT '异动前部门ID',
    `after_dept_id`    BIGINT                 DEFAULT NULL   COMMENT '异动后部门ID',
    `before_position`  VARCHAR(64)            DEFAULT NULL   COMMENT '异动前职位',
    `after_position`   VARCHAR(64)            DEFAULT NULL   COMMENT '异动后职位',
    `before_salary`    DECIMAL(12,2)          DEFAULT NULL   COMMENT '异动前薪资',
    `after_salary`     DECIMAL(12,2)          DEFAULT NULL   COMMENT '异动后薪资',
    `effect_date`      DATE          NOT NULL                COMMENT '生效日期',
    `reason`           VARCHAR(500)           DEFAULT NULL   COMMENT '异动原因',
    `apply_no`         VARCHAR(64)            DEFAULT NULL   COMMENT '关联申请单号',
    `approval_status`  TINYINT       NOT NULL DEFAULT 1      COMMENT '审批状态:0待提交 1审批中 2已通过 3已驳回',
    `operator_id`      BIGINT                 DEFAULT NULL   COMMENT '经办人(员工ID)',
    `attachment_url`   VARCHAR(255)           DEFAULT NULL   COMMENT '异动附件(调令等)',
    `remark`           VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_change_emp` (`employee_id`, `effect_date`),
    KEY `idx_change_type` (`change_type`),
    CONSTRAINT `fk_change_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_change_before_dept` FOREIGN KEY (`before_dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_change_after_dept` FOREIGN KEY (`after_dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工异动记录表';


-- ------------------------------------------------------------------------------------
-- 【F-02-01 员工档案】employee_attachment 员工附件表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee_attachment`;
CREATE TABLE `employee_attachment` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id` BIGINT       NOT NULL                COMMENT '员工ID',
    `attach_type` TINYINT      NOT NULL DEFAULT 7      COMMENT '附件类型:1身份证 2学历证书 3学位证书 4离职证明 5体检报告 6合同扫描件 7其他',
    `file_name`   VARCHAR(255) NOT NULL                COMMENT '文件名',
    `file_url`    VARCHAR(500) NOT NULL                COMMENT '文件访问地址',
    `file_ext`    VARCHAR(16)           DEFAULT NULL   COMMENT '扩展名,如 pdf/jpg',
    `file_size`   BIGINT                DEFAULT NULL   COMMENT '文件大小(字节)',
    `uploader_id` BIGINT                DEFAULT NULL   COMMENT '上传人用户ID',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attach_emp` (`employee_id`),
    CONSTRAINT `fk_attach_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工附件表';


-- ####################################################################################
--  【模块 09】系统设置 —— 用户与授权（3 张表，依赖 employee/dept）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-09-01 用户管理】sys_user 系统用户表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`        VARCHAR(64)  NOT NULL                COMMENT '登录账号(唯一)',
    `password`        VARCHAR(100) NOT NULL                COMMENT '密码(BCrypt加盐哈希,禁止明文)',
    `real_name`       VARCHAR(32)           DEFAULT NULL   COMMENT '姓名',
    `employee_id`     BIGINT                DEFAULT NULL   COMMENT '关联员工ID(NULL=非员工账号,如外包运维)',
    `dept_id`         BIGINT                DEFAULT NULL   COMMENT '所属部门ID(冗余,便于数据范围过滤)',
    `phone`           VARCHAR(20)           DEFAULT NULL   COMMENT '手机号',
    `email`           VARCHAR(64)           DEFAULT NULL   COMMENT '邮箱',
    `avatar`          VARCHAR(255)          DEFAULT NULL   COMMENT '头像地址',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '账号状态:1正常 0停用 2锁定',
    `login_fail_count` INT         NOT NULL DEFAULT 0      COMMENT '连续登录失败次数',
    `lock_until`      DATETIME              DEFAULT NULL   COMMENT '锁定到期时间',
    `last_login_time` DATETIME              DEFAULT NULL   COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64)           DEFAULT NULL   COMMENT '最后登录IP',
    `remark`          VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_employee` (`employee_id`),
    KEY `idx_user_dept` (`dept_id`),
    KEY `idx_user_status` (`status`),
    CONSTRAINT `fk_user_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_user_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表';


-- ------------------------------------------------------------------------------------
-- 【F-09-02 角色权限】sys_user_role 用户角色关联表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    BIGINT   NOT NULL                COMMENT '用户ID',
    `role_id`    BIGINT   NOT NULL                COMMENT '角色ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表';


-- ------------------------------------------------------------------------------------
-- 【F-09-03 菜单管理】sys_role_menu 角色菜单关联表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`    BIGINT   NOT NULL                COMMENT '角色ID',
    `menu_id`    BIGINT   NOT NULL                COMMENT '菜单ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
    KEY `idx_role_menu_menu` (`menu_id`),
    CONSTRAINT `fk_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表';


-- ####################################################################################
--  【模块 05】薪资管理（5 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-05-04 社保公积金】insurance_config 社保公积金配置表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `insurance_config`;
CREATE TABLE `insurance_config` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_name`    VARCHAR(64)   NOT NULL                COMMENT '配置名称,如 2026年北京社保-养老',
    `city`           VARCHAR(32)   NOT NULL DEFAULT '北京'  COMMENT '参保城市',
    `social_type`    TINYINT       NOT NULL                COMMENT '险种类型:1养老 2医疗 3失业 4工伤 5生育 6住房公积金',
    `company_rate`   DECIMAL(6,4)  NOT NULL DEFAULT 0.0000 COMMENT '企业缴纳比例(如0.1600=16%)',
    `personal_rate`  DECIMAL(6,4)  NOT NULL DEFAULT 0.0000 COMMENT '个人缴纳比例(如0.0800=8%)',
    `base_min`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '缴费基数下限',
    `base_max`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '缴费基数上限',
    `effective_date` DATE                   DEFAULT NULL   COMMENT '生效日期',
    `status`         TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`         VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`        TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_ins_cfg_city_type` (`city`, `social_type`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社保公积金配置表';


-- ------------------------------------------------------------------------------------
-- 【F-05-01 薪资标准】employee_salary 员工薪资标准表（按生效日期保留调薪历史）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee_salary`;
CREATE TABLE `employee_salary` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`        BIGINT        NOT NULL                COMMENT '员工ID',
    `standard_id`        BIGINT                 DEFAULT NULL   COMMENT '套用的薪资标准模板ID',
    `base_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '基本工资',
    `post_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '岗位工资',
    `performance_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '绩效工资基数',
    `allowance`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '固定补贴合计',
    `social_base`        DECIMAL(12,2)          DEFAULT NULL   COMMENT '社保缴费基数',
    `fund_base`          DECIMAL(12,2)          DEFAULT NULL   COMMENT '公积金缴费基数',
    `fund_rate`          DECIMAL(5,2)           DEFAULT NULL   COMMENT '公积金缴存比例(%)',
    `adjust_reason`      VARCHAR(255)           DEFAULT NULL   COMMENT '调薪原因',
    `effective_date`     DATE          NOT NULL                COMMENT '生效日期',
    `is_current`         TINYINT       NOT NULL DEFAULT 1      COMMENT '是否当前生效:1是 0否(历史记录)',
    `approval_status`    TINYINT       NOT NULL DEFAULT 1      COMMENT '审批状态:0待审 1已生效 2已驳回',
    `remark`             VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `created_by`         BIGINT                 DEFAULT NULL   COMMENT '操作人用户ID',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_emp_salary_emp` (`employee_id`, `is_current`),
    KEY `idx_emp_salary_date` (`effective_date`),
    CONSTRAINT `fk_emp_salary_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_emp_salary_standard` FOREIGN KEY (`standard_id`) REFERENCES `salary_standard` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工薪资标准表';


-- ------------------------------------------------------------------------------------
-- 【F-05-02 薪资核算】salary_record 月度薪资记录表（一人一月一条）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `salary_record`;
CREATE TABLE `salary_record` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`        BIGINT        NOT NULL                COMMENT '员工ID',
    `dept_id`            BIGINT                 DEFAULT NULL   COMMENT '所属部门ID(冗余,便于按部门统计人力成本)',
    `salary_year`        SMALLINT      NOT NULL                COMMENT '薪资年份,如 2026',
    `salary_month`       TINYINT       NOT NULL                COMMENT '薪资月份:1-12',
    `base_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '基本工资',
    `post_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '岗位工资',
    `performance_salary` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '绩效工资(按考核结果浮动)',
    `allowance`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '补贴合计',
    `overtime_pay`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '加班费',
    `other_add`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '其他加项(补发等)',
    `attendance_deduct`  DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '考勤扣款(迟到/早退/旷工)',
    `other_deduct`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '其他扣项',
    `gross_pay`          DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '应发合计',
    `social_personal`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '社保个人缴纳',
    `social_company`     DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '社保企业缴纳',
    `fund_personal`      DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '公积金个人缴纳',
    `fund_company`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '公积金企业缴纳',
    `tax_amount`         DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '个人所得税',
    `net_pay`            DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '实发合计',
    `pay_status`         TINYINT       NOT NULL DEFAULT 0      COMMENT '发放状态:0待核算 1已核算 2已复核 3已发放 4发放失败',
    `pay_date`           DATE                   DEFAULT NULL   COMMENT '实际发放日期',
    `pay_batch_no`       VARCHAR(64)            DEFAULT NULL   COMMENT '发放批次号',
    `lock_status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '锁定状态:0未锁定 1已锁定(禁改)',
    `calc_by`            BIGINT                 DEFAULT NULL   COMMENT '核算人用户ID(须与复核人不同,BR-05-03)',
    `audit_by`           BIGINT                 DEFAULT NULL   COMMENT '复核人用户ID(不得与核算人相同)',
    `remark`             VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_salary_emp_ym` (`employee_id`, `salary_year`, `salary_month`),
    KEY `idx_salary_period` (`salary_year`, `salary_month`),
    KEY `idx_salary_dept` (`dept_id`),
    KEY `idx_salary_status` (`pay_status`),
    CONSTRAINT `fk_salary_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_salary_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '月度薪资记录表';


-- ------------------------------------------------------------------------------------
-- 【F-05-06 薪资条查询】salary_item 薪资明细项表（薪资条逐项展示）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `salary_item`;
CREATE TABLE `salary_item` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `salary_record_id` BIGINT        NOT NULL                COMMENT '月度薪资记录ID',
    `item_type`        TINYINT       NOT NULL DEFAULT 1      COMMENT '项目类型:1应发项 2扣款项 3企业承担项',
    `item_code`        VARCHAR(32)   NOT NULL                COMMENT '项目编码,如 BASE/POST/OT/SOCIAL',
    `item_name`        VARCHAR(64)   NOT NULL                COMMENT '项目名称,如 基本工资',
    `item_amount`      DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '项目金额',
    `sort_no`          INT           NOT NULL DEFAULT 0      COMMENT '展示排序,升序',
    `remark`           VARCHAR(255)           DEFAULT NULL   COMMENT '备注(计算口径说明)',
    `deleted`          TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_salary_item_record` (`salary_record_id`, `item_type`),
    CONSTRAINT `fk_salary_item_record` FOREIGN KEY (`salary_record_id`) REFERENCES `salary_record` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '薪资明细项表';


-- ------------------------------------------------------------------------------------
-- 【F-05-04 社保公积金】insurance_record 社保缴纳记录表（一人一月一条）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `insurance_record`;
CREATE TABLE `insurance_record` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`       BIGINT        NOT NULL                COMMENT '员工ID',
    `config_id`         BIGINT                 DEFAULT NULL   COMMENT '关联社保配置ID',
    `record_year`       SMALLINT      NOT NULL                COMMENT '缴纳年份',
    `record_month`      TINYINT       NOT NULL                COMMENT '缴纳月份:1-12',
    `social_base`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '社保缴费基数',
    `fund_base`         DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '公积金缴费基数',
    `pension_personal`  DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '养老保险-个人',
    `pension_company`   DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '养老保险-企业',
    `medical_personal`  DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '医疗保险-个人',
    `medical_company`   DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '医疗保险-企业',
    `unemploy_personal` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '失业保险-个人',
    `unemploy_company`  DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '失业保险-企业',
    `injury_company`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '工伤保险-企业(个人不缴)',
    `maternity_company` DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '生育保险-企业(个人不缴)',
    `fund_personal`     DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '住房公积金-个人',
    `fund_company`      DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '住房公积金-企业',
    `total_personal`    DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '个人缴纳合计(含公积金)',
    `total_company`     DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '企业缴纳合计(含公积金)',
    `pay_status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '缴纳状态:0未缴 1已缴 2已减员',
    `remark`            VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    `deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_insurance_emp_ym` (`employee_id`, `record_year`, `record_month`),
    KEY `idx_insurance_period` (`record_year`, `record_month`),
    CONSTRAINT `fk_insurance_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_insurance_config` FOREIGN KEY (`config_id`) REFERENCES `insurance_config` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社保缴纳记录表';


-- ####################################################################################
--  【模块 04】考勤管理（8 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-04-02 请假申请】leave_balance 假期余额表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `leave_balance`;
CREATE TABLE `leave_balance` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`      BIGINT       NOT NULL                COMMENT '员工ID',
    `leave_type_id`    BIGINT       NOT NULL                COMMENT '假别ID',
    `balance_year`     SMALLINT     NOT NULL                COMMENT '所属年度,如 2026',
    `total_days`       DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '年度总额度(天)',
    `carry_over_days`  DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '上年结转天数',
    `used_days`        DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '已使用天数',
    `remain_days`      DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '剩余可用天数',
    `expire_date`      DATE                  DEFAULT NULL   COMMENT '额度失效日期',
    `deleted`          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_leave_balance_emp_type_year` (`employee_id`, `leave_type_id`, `balance_year`),
    KEY `idx_leave_balance_type` (`leave_type_id`),
    CONSTRAINT `fk_leave_balance_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_leave_balance_type` FOREIGN KEY (`leave_type_id`) REFERENCES `leave_type` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '假期余额表';


-- ------------------------------------------------------------------------------------
-- 【F-04-01 打卡记录】attendance_record 考勤打卡记录表（一人一天一条）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`       BIGINT       NOT NULL                COMMENT '员工ID',
    `attendance_date`   DATE         NOT NULL                COMMENT '考勤日期',
    `shift_id`          BIGINT                DEFAULT NULL   COMMENT '排班班次ID(NULL=使用默认班次)',
    `first_check_in`    DATETIME              DEFAULT NULL   COMMENT '首次上班打卡时间',
    `last_check_out`    DATETIME              DEFAULT NULL   COMMENT '末次下班打卡时间',
    `work_hours`        DECIMAL(5,1) NOT NULL DEFAULT 0.0    COMMENT '实际出勤工时(小时)',
    `late_minutes`      INT          NOT NULL DEFAULT 0      COMMENT '迟到分钟数',
    `early_minutes`     INT          NOT NULL DEFAULT 0      COMMENT '早退分钟数',
    `check_in_status`   TINYINT      NOT NULL DEFAULT 1      COMMENT '上班打卡状态:1正常 2迟到 3严重迟到',
    `check_out_status`  TINYINT      NOT NULL DEFAULT 1      COMMENT '下班打卡状态:1正常 2早退 3缺卡',
    `attendance_status` TINYINT      NOT NULL DEFAULT 1      COMMENT '考勤结果:1正常 2迟到 3早退 4缺卡 5旷工 6休假 7出差 8调休',
    `source`            TINYINT      NOT NULL DEFAULT 1      COMMENT '数据来源:1考勤设备 2移动端打卡 3补卡审批',
    `remark`            VARCHAR(255)          DEFAULT NULL   COMMENT '备注(补卡原因/异常说明)',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attendance_emp_date` (`employee_id`, `attendance_date`),
    KEY `idx_attendance_date` (`attendance_date`),
    KEY `idx_attendance_status` (`attendance_status`),
    KEY `idx_attendance_shift` (`shift_id`),
    CONSTRAINT `fk_attendance_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_attendance_shift` FOREIGN KEY (`shift_id`) REFERENCES `shift` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤打卡记录表';


-- ------------------------------------------------------------------------------------
-- 【F-04-05 排班管理】employee_schedule 员工排班表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `employee_schedule`;
CREATE TABLE `employee_schedule` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`   BIGINT       NOT NULL                COMMENT '员工ID',
    `shift_id`      BIGINT       NOT NULL                COMMENT '班次ID',
    `schedule_date` DATE         NOT NULL                COMMENT '排班日期',
    `remark`        VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_schedule_emp_date` (`employee_id`, `schedule_date`),
    KEY `idx_schedule_shift` (`shift_id`),
    KEY `idx_schedule_date` (`schedule_date`),
    CONSTRAINT `fk_schedule_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_schedule_shift` FOREIGN KEY (`shift_id`) REFERENCES `shift` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '员工排班表';


-- ------------------------------------------------------------------------------------
-- 【F-04-02 请假申请】leave_request 请假申请表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `leave_request`;
CREATE TABLE `leave_request` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_no`          VARCHAR(64)  NOT NULL                COMMENT '申请单号(唯一),如 LV20260901001',
    `employee_id`         BIGINT       NOT NULL                COMMENT '申请人员工ID',
    `leave_type_id`       BIGINT       NOT NULL                COMMENT '假别ID',
    `start_time`          DATETIME     NOT NULL                COMMENT '请假开始时间',
    `end_time`            DATETIME     NOT NULL                COMMENT '请假结束时间',
    `days`                DECIMAL(6,1) NOT NULL                COMMENT '请假天数(支持0.5天)',
    `reason`              VARCHAR(500)          DEFAULT NULL   COMMENT '请假事由',
    `attachment_url`      VARCHAR(500)          DEFAULT NULL   COMMENT '证明材料地址(病假条等)',
    `approval_status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销',
    `current_approver_id` BIGINT                DEFAULT NULL   COMMENT '当前审批人员工ID',
    `apply_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `deleted`             TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_leave_request_no` (`request_no`),
    KEY `idx_leave_req_emp` (`employee_id`, `start_time`),
    KEY `idx_leave_req_type` (`leave_type_id`),
    KEY `idx_leave_req_status` (`approval_status`),
    KEY `idx_leave_req_approver` (`current_approver_id`),
    CONSTRAINT `fk_leave_req_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_leave_req_type` FOREIGN KEY (`leave_type_id`) REFERENCES `leave_type` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_leave_req_approver` FOREIGN KEY (`current_approver_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '请假申请表';


-- ------------------------------------------------------------------------------------
-- 【F-04-03 加班申请】overtime_request 加班申请表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `overtime_request`;
CREATE TABLE `overtime_request` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_no`          VARCHAR(64)  NOT NULL                COMMENT '申请单号(唯一),如 OT20260901001',
    `employee_id`         BIGINT       NOT NULL                COMMENT '申请人员工ID',
    `overtime_date`       DATE         NOT NULL                COMMENT '加班日期',
    `start_time`          TIME         NOT NULL                COMMENT '加班开始时间',
    `end_time`            TIME         NOT NULL                COMMENT '加班结束时间',
    `hours`               DECIMAL(5,1) NOT NULL                COMMENT '加班时长(小时)',
    `overtime_type`       TINYINT      NOT NULL DEFAULT 1      COMMENT '加班类型:1工作日 2休息日 3法定节假日',
    `compensate_type`     TINYINT      NOT NULL DEFAULT 1      COMMENT '补偿方式:1调休 2计薪',
    `reason`              VARCHAR(500)          DEFAULT NULL   COMMENT '加班事由',
    `approval_status`     TINYINT      NOT NULL DEFAULT 1      COMMENT '审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销',
    `current_approver_id` BIGINT                DEFAULT NULL   COMMENT '当前审批人员工ID',
    `apply_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `deleted`             TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_overtime_request_no` (`request_no`),
    KEY `idx_overtime_emp` (`employee_id`, `overtime_date`),
    KEY `idx_overtime_status` (`approval_status`),
    CONSTRAINT `fk_overtime_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_overtime_approver` FOREIGN KEY (`current_approver_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '加班申请表';


-- ------------------------------------------------------------------------------------
-- 【F-04-04 出差管理】business_trip 出差申请表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `business_trip`;
CREATE TABLE `business_trip` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_no`          VARCHAR(64)   NOT NULL                COMMENT '申请单号(唯一),如 BT20260901001',
    `employee_id`         BIGINT        NOT NULL                COMMENT '申请人员工ID',
    `destination`         VARCHAR(128)  NOT NULL                COMMENT '出差目的地',
    `purpose`             VARCHAR(500)           DEFAULT NULL   COMMENT '出差事由',
    `start_time`          DATETIME      NOT NULL                COMMENT '出差开始时间',
    `end_time`            DATETIME      NOT NULL                COMMENT '出差结束时间',
    `days`                DECIMAL(6,1)  NOT NULL                COMMENT '出差天数',
    `transport`           TINYINT                DEFAULT NULL   COMMENT '交通方式:1飞机 2高铁 3汽车 4自驾 5其他',
    `budget_amount`       DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '预算金额',
    `approval_status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销',
    `current_approver_id` BIGINT                 DEFAULT NULL   COMMENT '当前审批人员工ID',
    `apply_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `deleted`             TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_trip_no` (`request_no`),
    KEY `idx_trip_emp` (`employee_id`, `start_time`),
    KEY `idx_trip_status` (`approval_status`),
    CONSTRAINT `fk_trip_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_trip_approver` FOREIGN KEY (`current_approver_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出差申请表';


-- ------------------------------------------------------------------------------------
-- 【F-04-06 考勤统计】attendance_month 考勤月报表（一人一月一条，封账后禁改）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `attendance_month`;
CREATE TABLE `attendance_month` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`    BIGINT       NOT NULL                COMMENT '员工ID',
    `dept_id`        BIGINT                DEFAULT NULL   COMMENT '所属部门ID(冗余,便于部门维度统计)',
    `record_year`    SMALLINT     NOT NULL                COMMENT '统计年份',
    `record_month`   TINYINT      NOT NULL                COMMENT '统计月份:1-12',
    `should_days`    DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '应出勤天数',
    `actual_days`    DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '实际出勤天数',
    `late_count`     INT          NOT NULL DEFAULT 0      COMMENT '迟到次数',
    `early_count`    INT          NOT NULL DEFAULT 0      COMMENT '早退次数',
    `absent_count`   INT          NOT NULL DEFAULT 0      COMMENT '旷工次数',
    `missing_count`  INT          NOT NULL DEFAULT 0      COMMENT '缺卡次数',
    `leave_days`     DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '请假天数合计',
    `overtime_hours` DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '加班时长合计(小时)',
    `trip_days`      DECIMAL(6,1) NOT NULL DEFAULT 0.0    COMMENT '出差天数合计',
    `lock_status`    TINYINT      NOT NULL DEFAULT 0      COMMENT '封账状态:0未封账 1已封账(禁改)',
    `lock_time`      DATETIME              DEFAULT NULL   COMMENT '封账时间',
    `lock_by`        BIGINT                DEFAULT NULL   COMMENT '封账操作人用户ID',
    `remark`         VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attendance_month_emp_ym` (`employee_id`, `record_year`, `record_month`),
    KEY `idx_att_month_period` (`record_year`, `record_month`),
    KEY `idx_att_month_dept` (`dept_id`),
    CONSTRAINT `fk_att_month_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_att_month_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考勤月报表';


-- ####################################################################################
--  【模块 07】培训管理 —— 讲师（1 张，其余在后方）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-07-03 讲师管理】trainer 讲师表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `trainer`;
CREATE TABLE `trainer` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `trainer_code`  VARCHAR(32)   NOT NULL                COMMENT '讲师编号(唯一)',
    `trainer_name`  VARCHAR(32)   NOT NULL                COMMENT '讲师姓名',
    `trainer_type`  TINYINT       NOT NULL DEFAULT 1       COMMENT '讲师类型:1内部讲师 2外部讲师',
    `employee_id`   BIGINT                 DEFAULT NULL   COMMENT '内部讲师关联员工ID(外部讲师为空)',
    `expertise`     VARCHAR(255)           DEFAULT NULL   COMMENT '擅长领域,如 Java架构/项目管理',
    `trainer_level` TINYINT       NOT NULL DEFAULT 1       COMMENT '讲师等级:1初级 2中级 3高级 4资深',
    `phone`         VARCHAR(20)            DEFAULT NULL   COMMENT '联系电话',
    `email`         VARCHAR(64)            DEFAULT NULL   COMMENT '邮箱',
    `org_name`      VARCHAR(128)           DEFAULT NULL   COMMENT '所属机构(外部讲师)',
    `hour_rate`     DECIMAL(10,2) NOT NULL DEFAULT 0.00    COMMENT '课时费(元/小时)',
    `teach_count`   INT           NOT NULL DEFAULT 0       COMMENT '累计授课次数',
    `avg_score`     DECIMAL(4,1)           DEFAULT NULL    COMMENT '平均评分(5分制)',
    `status`        TINYINT       NOT NULL DEFAULT 1       COMMENT '状态:1启用 0停用',
    `remark`        VARCHAR(255)           DEFAULT NULL    COMMENT '备注',
    `deleted`       TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trainer_code` (`trainer_code`),
    KEY `idx_trainer_employee` (`employee_id`),
    KEY `idx_trainer_type` (`trainer_type`),
    CONSTRAINT `fk_trainer_employee` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '讲师表';


-- ####################################################################################
--  【模块 06】招聘管理（5 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-06-01 招聘需求】recruit_request 招聘需求表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `recruit_request`;
CREATE TABLE `recruit_request` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `request_no`          VARCHAR(64)   NOT NULL                COMMENT '需求单号(唯一),如 RC20260901001',
    `request_dept_id`     BIGINT        NOT NULL                COMMENT '需求部门ID',
    `position_id`         BIGINT                 DEFAULT NULL   COMMENT '招聘岗位ID',
    `position_name`       VARCHAR(64)            DEFAULT NULL   COMMENT '岗位名称(快照)',
    `recruit_count`       INT           NOT NULL DEFAULT 1       COMMENT '招聘人数',
    `hired_count`         INT           NOT NULL DEFAULT 0       COMMENT '已录用人数',
    `job_level`           VARCHAR(16)            DEFAULT NULL   COMMENT '职级',
    `salary_min`          DECIMAL(12,2)          DEFAULT NULL   COMMENT '薪资区间下限',
    `salary_max`          DECIMAL(12,2)          DEFAULT NULL   COMMENT '薪资区间上限',
    `education_req`       TINYINT                DEFAULT NULL   COMMENT '学历要求:1博士 2硕士 3本科 4大专 5中专 6高中 7不限',
    `experience_years`    INT                    DEFAULT NULL   COMMENT '经验要求(年)',
    `job_description`     TEXT                   DEFAULT NULL   COMMENT '岗位职责',
    `job_requirement`     TEXT                   DEFAULT NULL   COMMENT '任职要求',
    `hope_arrive_date`    DATE                   DEFAULT NULL   COMMENT '期望到岗日期',
    `urgency`             TINYINT       NOT NULL DEFAULT 1       COMMENT '紧急程度:1普通 2紧急 3非常紧急',
    `applicant_id`        BIGINT                 DEFAULT NULL   COMMENT '提报人员工ID',
    `apply_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提报时间',
    `approval_status`     TINYINT       NOT NULL DEFAULT 1       COMMENT '审批状态:0草稿 1审批中 2已通过 3已驳回',
    `current_approver_id` BIGINT                 DEFAULT NULL   COMMENT '当前审批人员工ID',
    `status`              TINYINT       NOT NULL DEFAULT 1       COMMENT '需求状态:1进行中 2已完成 3已取消',
    `deleted`             TINYINT       NOT NULL DEFAULT 0       COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_recruit_request_no` (`request_no`),
    KEY `idx_recruit_dept` (`request_dept_id`),
    KEY `idx_recruit_position` (`position_id`),
    KEY `idx_recruit_status` (`status`, `approval_status`),
    CONSTRAINT `fk_recruit_dept` FOREIGN KEY (`request_dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_recruit_position` FOREIGN KEY (`position_id`) REFERENCES `position` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_recruit_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_recruit_approver` FOREIGN KEY (`current_approver_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '招聘需求表';


-- ------------------------------------------------------------------------------------
-- 【F-06-02 职位发布】job_posting 职位发布表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `job_posting`;
CREATE TABLE `job_posting` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `recruit_request_id` BIGINT      NOT NULL                COMMENT '关联招聘需求ID',
    `channel`           TINYINT      NOT NULL DEFAULT 1      COMMENT '发布渠道:1官网 2智联招聘 3前程无忧 4BOSS直聘 5猎聘 6内部推荐 7其他',
    `posting_title`     VARCHAR(128) NOT NULL                COMMENT '发布职位名称',
    `posting_url`       VARCHAR(500)          DEFAULT NULL   COMMENT '发布链接',
    `publish_time`      DATETIME              DEFAULT NULL   COMMENT '发布时间',
    `close_time`        DATETIME              DEFAULT NULL   COMMENT '关闭时间',
    `view_count`        INT          NOT NULL DEFAULT 0      COMMENT '浏览量',
    `apply_count`       INT          NOT NULL DEFAULT 0      COMMENT '投递量',
    `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1发布中 2已关闭',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_posting_request` (`recruit_request_id`),
    KEY `idx_posting_channel` (`channel`, `status`),
    CONSTRAINT `fk_posting_request` FOREIGN KEY (`recruit_request_id`) REFERENCES `recruit_request` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '职位发布表';


-- ------------------------------------------------------------------------------------
-- 【F-06-03 简历筛选 / F-06-06 人才库】candidate 候选人表
--   人才库通过 is_talent + talent_tags 标记实现，不单独建表，避免同一人重复录入
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `candidate`;
CREATE TABLE `candidate` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `recruit_request_id` BIGINT        NOT NULL                COMMENT '关联招聘需求ID',
    `name`               VARCHAR(32)   NOT NULL                COMMENT '候选人姓名',
    `gender`             VARCHAR(8)             DEFAULT NULL   COMMENT '性别:M男 F女 U未知',
    `birth_date`         DATE                   DEFAULT NULL   COMMENT '出生日期',
    `phone`              VARCHAR(20)            DEFAULT NULL   COMMENT '联系电话',
    `email`              VARCHAR(64)            DEFAULT NULL   COMMENT '电子邮箱',
    `education`          TINYINT                DEFAULT NULL   COMMENT '最高学历:1博士 2硕士 3本科 4大专 5中专 6高中 7其他',
    `graduate_school`    VARCHAR(64)            DEFAULT NULL   COMMENT '毕业院校',
    `major`              VARCHAR(64)            DEFAULT NULL   COMMENT '所学专业',
    `work_years`         INT                    DEFAULT NULL   COMMENT '工作年限',
    `current_company`    VARCHAR(128)           DEFAULT NULL   COMMENT '当前就职公司',
    `current_position`   VARCHAR(64)            DEFAULT NULL   COMMENT '当前职位',
    `expect_salary`      DECIMAL(12,2)          DEFAULT NULL   COMMENT '期望薪资',
    `resume_url`         VARCHAR(500)           DEFAULT NULL   COMMENT '简历文件地址',
    `source`             TINYINT       NOT NULL DEFAULT 2      COMMENT '简历来源:1官网 2招聘网站 3内部推荐 4猎头 5校园招聘 6其他',
    `referrer_id`        BIGINT                 DEFAULT NULL   COMMENT '内推人员工ID',
    `stage`              TINYINT       NOT NULL DEFAULT 1      COMMENT '所处阶段:1待筛选 2待面试 3面试中 4待录用 5已录用 6已淘汰',
    `is_talent`          TINYINT       NOT NULL DEFAULT 0      COMMENT '是否纳入人才库:1是 0否',
    `talent_tags`        VARCHAR(255)           DEFAULT NULL   COMMENT '人才库标签,逗号分隔,如 Java,后端,3年经验',
    `remark`             VARCHAR(500)           DEFAULT NULL   COMMENT '备注(淘汰原因等)',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_candidate_request` (`recruit_request_id`),
    KEY `idx_candidate_stage` (`stage`),
    KEY `idx_candidate_talent` (`is_talent`),
    KEY `idx_candidate_phone` (`phone`),
    CONSTRAINT `fk_candidate_request` FOREIGN KEY (`recruit_request_id`) REFERENCES `recruit_request` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_candidate_referrer` FOREIGN KEY (`referrer_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '候选人表';


-- ------------------------------------------------------------------------------------
-- 【F-06-04 面试安排】interview_record 面试记录表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `interview_record`;
CREATE TABLE `interview_record` (
    `id`                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `candidate_id`       BIGINT        NOT NULL                COMMENT '候选人ID',
    `recruit_request_id` BIGINT        NOT NULL                COMMENT '关联招聘需求ID',
    `round_no`           INT           NOT NULL DEFAULT 1      COMMENT '面试轮次,从1开始',
    `interview_type`     TINYINT       NOT NULL DEFAULT 1      COMMENT '面试类型:1初试 2复试 3终试 4HR面谈',
    `interview_time`     DATETIME               DEFAULT NULL   COMMENT '面试时间',
    `interview_place`    VARCHAR(128)           DEFAULT NULL   COMMENT '面试地点/会议链接',
    `interviewer_id`     BIGINT                 DEFAULT NULL   COMMENT '面试官员工ID',
    `score`              INT                    DEFAULT NULL   COMMENT '面试评分(0-100)',
    `evaluation`         VARCHAR(1000)          DEFAULT NULL   COMMENT '面试评价',
    `conclusion`         TINYINT                DEFAULT NULL   COMMENT '面试结论:1通过 2待定 3淘汰',
    `deleted`            TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_interview_candidate` (`candidate_id`, `round_no`),
    KEY `idx_interview_request` (`recruit_request_id`),
    KEY `idx_interview_interviewer` (`interviewer_id`),
    CONSTRAINT `fk_interview_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_interview_request` FOREIGN KEY (`recruit_request_id`) REFERENCES `recruit_request` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_interview_interviewer` FOREIGN KEY (`interviewer_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '面试记录表';


-- ------------------------------------------------------------------------------------
-- 【F-06-05 录用审批】offer_record 录用通知表（录用后一键推送至入职办理）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `offer_record`;
CREATE TABLE `offer_record` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `offer_no`            VARCHAR(64)   NOT NULL                COMMENT 'Offer编号(唯一)',
    `candidate_id`        BIGINT        NOT NULL                COMMENT '候选人ID',
    `recruit_request_id`  BIGINT        NOT NULL                COMMENT '关联招聘需求ID',
    `dept_id`             BIGINT        NOT NULL                COMMENT '录用部门ID',
    `position_id`         BIGINT                 DEFAULT NULL   COMMENT '录用岗位ID',
    `job_level`           VARCHAR(16)            DEFAULT NULL   COMMENT '定级',
    `offer_salary`        DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '转正后月薪',
    `probation_months`    INT           NOT NULL DEFAULT 3      COMMENT '试用期月数',
    `probation_salary`    DECIMAL(12,2)          DEFAULT NULL   COMMENT '试用期月薪(通常为转正的80%)',
    `entry_date`          DATE                   DEFAULT NULL   COMMENT '预计入职日期',
    `approval_status`     TINYINT       NOT NULL DEFAULT 1      COMMENT '审批状态:0草稿 1审批中 2已通过 3已驳回',
    `current_approver_id` BIGINT                 DEFAULT NULL   COMMENT '当前审批人员工ID',
    `send_time`           DATETIME               DEFAULT NULL   COMMENT 'Offer发送时间',
    `confirm_time`        DATETIME               DEFAULT NULL   COMMENT '候选人确认时间',
    `offer_url`           VARCHAR(500)           DEFAULT NULL   COMMENT 'Offer文件地址',
    `status`              TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1待发送 2已发送 3已接受 4已拒绝 5已过期 6已入职',
    `deleted`             TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_offer_no` (`offer_no`),
    KEY `idx_offer_candidate` (`candidate_id`),
    KEY `idx_offer_request` (`recruit_request_id`),
    KEY `idx_offer_dept` (`dept_id`),
    CONSTRAINT `fk_offer_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_offer_request` FOREIGN KEY (`recruit_request_id`) REFERENCES `recruit_request` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_offer_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_offer_position` FOREIGN KEY (`position_id`) REFERENCES `position` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '录用通知表';


-- ####################################################################################
--  【模块 07】培训管理（4 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-07-01 培训计划】training_plan 培训计划表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `training_plan`;
CREATE TABLE `training_plan` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_code`       VARCHAR(32)   NOT NULL                COMMENT '计划编号(唯一)',
    `plan_name`       VARCHAR(128)  NOT NULL                COMMENT '计划名称,如 2026年Q1新员工培训',
    `plan_year`       SMALLINT      NOT NULL                COMMENT '所属年度',
    `plan_quarter`    TINYINT       NOT NULL DEFAULT 0      COMMENT '所属季度:0全年 1-4对应Q1-Q4',
    `train_type`      TINYINT       NOT NULL DEFAULT 2      COMMENT '培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训',
    `target_scope`    TINYINT       NOT NULL DEFAULT 1      COMMENT '培训对象范围:1全体 2指定部门 3指定岗位 4指定人员',
    `target_dept_ids` VARCHAR(500)           DEFAULT NULL   COMMENT '指定部门ID列表,逗号分隔',
    `budget_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '预算金额',
    `actual_amount`   DECIMAL(12,2) NOT NULL DEFAULT 0.00   COMMENT '实际支出',
    `start_date`      DATE                   DEFAULT NULL   COMMENT '开始日期',
    `end_date`        DATE                   DEFAULT NULL   COMMENT '结束日期',
    `organizer_id`    BIGINT                 DEFAULT NULL   COMMENT '组织人员工ID',
    `description`     VARCHAR(1000)          DEFAULT NULL   COMMENT '计划说明',
    `status`          TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1草稿 2已发布 3进行中 4已完成 5已取消',
    `deleted`         TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_training_plan_code` (`plan_code`),
    KEY `idx_plan_year_quarter` (`plan_year`, `plan_quarter`),
    KEY `idx_plan_organizer` (`organizer_id`),
    CONSTRAINT `fk_plan_organizer` FOREIGN KEY (`organizer_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '培训计划表';


-- ------------------------------------------------------------------------------------
-- 【F-07-02 课程管理】course 课程表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_code`    VARCHAR(32)  NOT NULL                COMMENT '课程编码(唯一)',
    `course_name`    VARCHAR(128) NOT NULL                COMMENT '课程名称',
    `course_type`    TINYINT      NOT NULL DEFAULT 2      COMMENT '授课形式:1线上 2线下',
    `category`       VARCHAR(64)           DEFAULT NULL   COMMENT '课程分类,如 技术类/管理类',
    `hours`          DECIMAL(5,1) NOT NULL DEFAULT 0.0    COMMENT '课程学时',
    `trainer_id`     BIGINT                DEFAULT NULL   COMMENT '主讲讲师ID',
    `courseware_url` VARCHAR(500)          DEFAULT NULL   COMMENT '课件下载地址',
    `video_url`      VARCHAR(500)          DEFAULT NULL   COMMENT '录播视频地址',
    `outline`        TEXT                  DEFAULT NULL   COMMENT '课程大纲',
    `max_students`   INT          NOT NULL DEFAULT 0      COMMENT '人数上限(0=不限)',
    `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    `remark`         VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`),
    KEY `idx_course_trainer` (`trainer_id`),
    KEY `idx_course_category` (`category`),
    CONSTRAINT `fk_course_trainer` FOREIGN KEY (`trainer_id`) REFERENCES `trainer` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '课程表';


-- ------------------------------------------------------------------------------------
-- 【F-07-04 报名签到】training_enroll 培训报名表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `training_enroll`;
CREATE TABLE `training_enroll` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`            BIGINT       NOT NULL                COMMENT '培训计划ID',
    `course_id`          BIGINT       NOT NULL                COMMENT '课程ID',
    `employee_id`        BIGINT       NOT NULL                COMMENT '报名员工ID',
    `enroll_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `sign_in_status`     TINYINT      NOT NULL DEFAULT 0      COMMENT '签到状态:0未签到 1已签到 2缺席',
    `sign_in_time`       DATETIME              DEFAULT NULL   COMMENT '签到时间',
    `exam_score`         DECIMAL(5,1)          DEFAULT NULL   COMMENT '考核成绩(0-100)',
    `pass_status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '考核结果:0未考核 1合格 2不合格',
    `satisfaction_score` TINYINT               DEFAULT NULL   COMMENT '满意度评分(1-5星)',
    `remark`             VARCHAR(255)          DEFAULT NULL   COMMENT '备注',
    `deleted`            TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_enroll_plan_emp` (`plan_id`, `employee_id`),
    KEY `idx_enroll_course` (`course_id`),
    KEY `idx_enroll_emp` (`employee_id`),
    CONSTRAINT `fk_enroll_plan` FOREIGN KEY (`plan_id`) REFERENCES `training_plan` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_enroll_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_enroll_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '培训报名表';


-- ------------------------------------------------------------------------------------
-- 【F-07-06 培训档案】training_record 培训档案表（员工参训履历）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `training_record`;
CREATE TABLE `training_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `employee_id`    BIGINT       NOT NULL                COMMENT '员工ID',
    `plan_id`        BIGINT                DEFAULT NULL   COMMENT '关联培训计划ID',
    `course_id`      BIGINT                DEFAULT NULL   COMMENT '关联课程ID',
    `course_name`    VARCHAR(128) NOT NULL                COMMENT '课程名称(快照,防课程改名影响历史)',
    `train_type`     TINYINT               DEFAULT NULL   COMMENT '培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训',
    `hours`          DECIMAL(5,1) NOT NULL DEFAULT 0.0    COMMENT '获得学时',
    `train_date`     DATE                  DEFAULT NULL   COMMENT '培训日期',
    `exam_score`     DECIMAL(5,1)          DEFAULT NULL   COMMENT '考核成绩',
    `pass_status`    TINYINT      NOT NULL DEFAULT 0      COMMENT '考核结果:0未考核 1合格 2不合格',
    `certificate_no` VARCHAR(64)           DEFAULT NULL   COMMENT '证书编号',
    `archive_year`   SMALLINT              DEFAULT NULL   COMMENT '归档年度',
    `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_train_rec_emp` (`employee_id`, `train_date`),
    KEY `idx_train_rec_course` (`course_id`),
    CONSTRAINT `fk_train_rec_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_train_rec_plan` FOREIGN KEY (`plan_id`) REFERENCES `training_plan` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_train_rec_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '培训档案表';


-- ####################################################################################
--  【模块 08】绩效考核（4 张表）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-08-01 考核方案】performance_plan 考核方案表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `performance_plan`;
CREATE TABLE `performance_plan` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_code`          VARCHAR(32)  NOT NULL                COMMENT '方案编号(唯一)',
    `plan_name`          VARCHAR(128) NOT NULL                COMMENT '方案名称,如 2026年Q1绩效考核',
    `period_type`        TINYINT      NOT NULL DEFAULT 2      COMMENT '考核周期:1月度 2季度 3半年度 4年度',
    `plan_year`          SMALLINT     NOT NULL                COMMENT '考核年度',
    `plan_quarter`       TINYINT               DEFAULT NULL   COMMENT '考核季度:1-4(周期为季度时有效)',
    `plan_month`         TINYINT               DEFAULT NULL   COMMENT '考核月份:1-12(周期为月度时有效)',
    `start_date`         DATE                  DEFAULT NULL   COMMENT '考核开始日期',
    `end_date`           DATE                  DEFAULT NULL   COMMENT '考核截止日期',
    `scope_type`         TINYINT      NOT NULL DEFAULT 1      COMMENT '考核范围:1全体 2指定部门 3指定岗位 4指定人员',
    `scope_dept_ids`     VARCHAR(500)          DEFAULT NULL   COMMENT '指定部门ID列表,逗号分隔',
    `grade_rule`         VARCHAR(255)          DEFAULT NULL   COMMENT '等级规则,如 S:90+,A:80-89,B:70-79,C:60-69,D:<60',
    `force_distribution` TINYINT      NOT NULL DEFAULT 0      COMMENT '是否强制分布:1是 0否',
    `distribution_rule`  VARCHAR(255)          DEFAULT NULL   COMMENT '等级分布比例,如 S:10,A:20,B:50,C:15,D:5',
    `self_weight`        DECIMAL(5,2) NOT NULL DEFAULT 20.00  COMMENT '员工自评权重(%)',
    `leader_weight`      DECIMAL(5,2) NOT NULL DEFAULT 70.00  COMMENT '主管评分权重(%)',
    `review_weight`      DECIMAL(5,2) NOT NULL DEFAULT 10.00  COMMENT '隔级复核权重(%)',
    `creator_id`         BIGINT                DEFAULT NULL   COMMENT '创建人员工ID',
    `description`        VARCHAR(1000)         DEFAULT NULL   COMMENT '方案说明',
    `status`             TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1草稿 2已发布 3评分中 4已结束',
    `deleted`            TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_performance_plan_code` (`plan_code`),
    KEY `idx_perf_plan_period` (`plan_year`, `period_type`),
    CONSTRAINT `fk_perf_plan_creator` FOREIGN KEY (`creator_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考核方案表';


-- ------------------------------------------------------------------------------------
-- 【F-08-02 指标设置】performance_indicator 考核指标表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `performance_indicator`;
CREATE TABLE `performance_indicator` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`         BIGINT       NOT NULL                COMMENT '考核方案ID',
    `employee_id`     BIGINT       NOT NULL                COMMENT '被考核员工ID',
    `indicator_name`  VARCHAR(128) NOT NULL                COMMENT '指标名称,如 需求交付及时率',
    `indicator_type`  TINYINT      NOT NULL DEFAULT 1      COMMENT '指标类型:1KPI 2OKR 3能力 4态度',
    `weight`          DECIMAL(5,2) NOT NULL DEFAULT 0.00   COMMENT '指标权重(%),同一员工合计应为100',
    `target_value`    VARCHAR(255)          DEFAULT NULL   COMMENT '目标值,如 95%',
    `actual_value`    VARCHAR(255)          DEFAULT NULL   COMMENT '实际完成值',
    `score_standard`  VARCHAR(500)          DEFAULT NULL   COMMENT '评分标准说明',
    `self_score`      DECIMAL(5,2)          DEFAULT NULL   COMMENT '自评得分(0-100)',
    `leader_score`    DECIMAL(5,2)          DEFAULT NULL   COMMENT '主管评分(0-100)',
    `final_score`     DECIMAL(5,2)          DEFAULT NULL   COMMENT '最终得分(按权重汇总)',
    `source`          TINYINT      NOT NULL DEFAULT 1      COMMENT '指标来源:1主管下达 2员工自拟 3系统模板',
    `confirm_status`  TINYINT      NOT NULL DEFAULT 0      COMMENT '员工确认状态:0待确认 1已确认',
    `sort_no`         INT          NOT NULL DEFAULT 0      COMMENT '显示排序,升序',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_indicator_plan_emp` (`plan_id`, `employee_id`),
    CONSTRAINT `fk_indicator_plan` FOREIGN KEY (`plan_id`) REFERENCES `performance_plan` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_indicator_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考核指标表';


-- ------------------------------------------------------------------------------------
-- 【F-08-03/05 绩效评分/结果归档】performance_record 绩效考核记录表（一人一周期一条）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `performance_record`;
CREATE TABLE `performance_record` (
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `plan_id`                 BIGINT       NOT NULL                COMMENT '考核方案ID',
    `employee_id`             BIGINT       NOT NULL                COMMENT '被考核员工ID',
    `dept_id`                 BIGINT                DEFAULT NULL   COMMENT '所属部门ID(冗余,便于部门统计)',
    `leader_id`               BIGINT                DEFAULT NULL   COMMENT '考核主管员工ID',
    `self_score`              DECIMAL(5,2)          DEFAULT NULL   COMMENT '自评加权总分',
    `leader_score`            DECIMAL(5,2)          DEFAULT NULL   COMMENT '主管评分加权总分',
    `review_score`            DECIMAL(5,2)          DEFAULT NULL   COMMENT '隔级复核分',
    `final_score`             DECIMAL(5,2)          DEFAULT NULL   COMMENT '最终得分(按方案权重计算)',
    `grade`                   VARCHAR(8)            DEFAULT NULL   COMMENT '绩效等级:S/A/B/C/D',
    `dept_rank`               INT                   DEFAULT NULL   COMMENT '部门内排名',
    `employee_confirm_status` TINYINT      NOT NULL DEFAULT 0      COMMENT '员工确认状态:0待确认 1已确认 2已申诉',
    `appeal_reason`           VARCHAR(500)          DEFAULT NULL   COMMENT '申诉理由',
    `appeal_result`           VARCHAR(500)          DEFAULT NULL   COMMENT '申诉处理结果',
    `archive_status`          TINYINT      NOT NULL DEFAULT 0      COMMENT '归档状态:0未归档 1已归档(锁定)',
    `archive_time`            DATETIME              DEFAULT NULL   COMMENT '归档时间',
    `status`                  TINYINT      NOT NULL DEFAULT 1      COMMENT '流程状态:1待自评 2待主管评 3待复核 4待员工确认 5已完成',
    `remark`                  VARCHAR(500)          DEFAULT NULL   COMMENT '备注',
    `deleted`                 TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_perf_record_plan_emp` (`plan_id`, `employee_id`),
    KEY `idx_perf_record_emp` (`employee_id`),
    KEY `idx_perf_record_dept` (`dept_id`, `grade`),
    KEY `idx_perf_record_status` (`status`),
    CONSTRAINT `fk_perf_record_plan` FOREIGN KEY (`plan_id`) REFERENCES `performance_plan` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_perf_record_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_perf_record_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_perf_record_leader` FOREIGN KEY (`leader_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绩效考核记录表';


-- ------------------------------------------------------------------------------------
-- 【F-08-04 绩效面谈】performance_interview 绩效面谈记录表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `performance_interview`;
CREATE TABLE `performance_interview` (
    `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_id`         BIGINT        NOT NULL                COMMENT '绩效考核记录ID',
    `employee_id`       BIGINT        NOT NULL                COMMENT '被面谈员工ID',
    `interviewer_id`    BIGINT        NOT NULL                COMMENT '面谈人(主管)员工ID',
    `interview_time`    DATETIME               DEFAULT NULL   COMMENT '面谈时间',
    `interview_place`   VARCHAR(128)           DEFAULT NULL   COMMENT '面谈地点',
    `achievement`       VARCHAR(1000)          DEFAULT NULL   COMMENT '本期成绩与亮点',
    `shortcoming`       VARCHAR(1000)          DEFAULT NULL   COMMENT '不足与改进方向',
    `next_goal`         VARCHAR(1000)          DEFAULT NULL   COMMENT '下期目标',
    `employee_feedback` VARCHAR(1000)          DEFAULT NULL   COMMENT '员工反馈与诉求',
    `deleted`           TINYINT       NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_perf_interview_record` (`record_id`),
    KEY `idx_perf_interview_emp` (`employee_id`),
    CONSTRAINT `fk_perf_interview_record` FOREIGN KEY (`record_id`) REFERENCES `performance_record` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_perf_interview_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_perf_interview_interviewer` FOREIGN KEY (`interviewer_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '绩效面谈记录表';


-- ####################################################################################
--  【模块 01】首页 —— 公告与待办（2 张）+ 通用审批与待办（2 张）
-- ####################################################################################

-- ------------------------------------------------------------------------------------
-- 【F-01-03 通知公告】notice 通知公告表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`           VARCHAR(200) NOT NULL                COMMENT '公告标题',
    `content`         TEXT                  DEFAULT NULL   COMMENT '公告正文',
    `notice_type`     TINYINT      NOT NULL DEFAULT 3      COMMENT '公告类型:1公司制度 2放假通知 3人事公告 4活动通知 5其他',
    `publisher_id`    BIGINT                DEFAULT NULL   COMMENT '发布人员工ID',
    `publish_time`    DATETIME              DEFAULT NULL   COMMENT '发布时间(支持定时发布)',
    `expire_time`     DATETIME              DEFAULT NULL   COMMENT '失效时间(到期自动下架)',
    `target_scope`    TINYINT      NOT NULL DEFAULT 1      COMMENT '发布范围:1全体 2指定部门',
    `target_dept_ids` VARCHAR(500)          DEFAULT NULL   COMMENT '指定部门ID列表,逗号分隔',
    `is_top`          TINYINT      NOT NULL DEFAULT 0      COMMENT '是否置顶:1是 0否',
    `read_count`      INT          NOT NULL DEFAULT 0      COMMENT '已读人数',
    `status`          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1草稿 2已发布 3已下架',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_notice_status` (`status`, `publish_time`),
    KEY `idx_notice_publisher` (`publisher_id`),
    CONSTRAINT `fk_notice_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知公告表';


-- ------------------------------------------------------------------------------------
-- 【F-01-03 通知公告】notice_read 公告已读记录表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `notice_read`;
CREATE TABLE `notice_read` (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `notice_id`   BIGINT   NOT NULL                COMMENT '公告ID',
    `employee_id` BIGINT   NOT NULL                COMMENT '阅读人员工ID',
    `read_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notice_read` (`notice_id`, `employee_id`),
    KEY `idx_notice_read_emp` (`employee_id`),
    CONSTRAINT `fk_notice_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `notice` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_notice_read_emp` FOREIGN KEY (`employee_id`) REFERENCES `employee` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公告已读记录表';


-- ------------------------------------------------------------------------------------
-- 【F-01-02 待办事项】todo_task 待办任务表
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `todo_task`;
CREATE TABLE `todo_task` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `receiver_id`   BIGINT       NOT NULL                COMMENT '待办接收人员工ID',
    `task_type`     TINYINT      NOT NULL DEFAULT 1      COMMENT '待办类型:1待审批 2合同到期 3转正提醒 4绩效评分 5培训报名 6其他',
    `task_title`    VARCHAR(200) NOT NULL                COMMENT '待办标题',
    `task_content`  VARCHAR(500)          DEFAULT NULL   COMMENT '待办详情',
    `biz_type`      TINYINT               DEFAULT NULL   COMMENT '业务类型(同approval_record.biz_type)',
    `biz_id`        BIGINT                DEFAULT NULL   COMMENT '业务单据ID(多态,不设外键)',
    `biz_no`        VARCHAR(64)           DEFAULT NULL   COMMENT '业务单号',
    `priority`      TINYINT      NOT NULL DEFAULT 2      COMMENT '优先级:1低 2中 3高',
    `deadline`      DATETIME              DEFAULT NULL   COMMENT '处理截止时间',
    `handle_status` TINYINT      NOT NULL DEFAULT 0      COMMENT '处理状态:0未处理 1已处理 2已忽略',
    `handle_time`   DATETIME              DEFAULT NULL   COMMENT '处理时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_todo_receiver` (`receiver_id`, `handle_status`),
    KEY `idx_todo_type` (`task_type`),
    CONSTRAINT `fk_todo_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `employee` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '待办任务表';


-- ------------------------------------------------------------------------------------
-- 通用审批记录表（请假/加班/出差/异动/招聘/录用 等共用一张审批流水表）
-- ------------------------------------------------------------------------------------
DROP TABLE IF EXISTS `approval_record`;
CREATE TABLE `approval_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `biz_type`          TINYINT      NOT NULL                COMMENT '业务类型:1请假 2加班 3出差 4员工异动 5离职 6招聘需求 7录用 8调薪',
    `biz_id`            BIGINT       NOT NULL                COMMENT '业务单据ID(多态指向不同业务表,故不设外键)',
    `biz_no`            VARCHAR(64)           DEFAULT NULL   COMMENT '业务单号(冗余,便于检索)',
    `approval_level`    INT          NOT NULL DEFAULT 1      COMMENT '审批层级,从1开始',
    `approver_id`       BIGINT                DEFAULT NULL   COMMENT '审批人员工ID',
    `approver_name`     VARCHAR(32)           DEFAULT NULL   COMMENT '审批人姓名(快照)',
    `approval_status`   TINYINT      NOT NULL DEFAULT 1      COMMENT '审批结果:1待审批 2已同意 3已驳回 4已转交',
    `approval_opinion`  VARCHAR(500)          DEFAULT NULL   COMMENT '审批意见',
    `approval_time`     DATETIME              DEFAULT NULL   COMMENT '审批时间',
    `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0未删除 1已删除',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_approval_biz` (`biz_type`, `biz_id`),
    KEY `idx_approval_approver` (`approver_id`, `approval_status`),
    CONSTRAINT `fk_approval_approver` FOREIGN KEY (`approver_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通用审批记录表';


-- ####################################################################################
--  收尾：补充「循环依赖」外键
--  dept.leader_id → employee.id 与 employee.dept_id → dept.id 互为引用，
--  必须先建完两张表后再补加，故放在脚本末尾统一 ALTER。
-- ####################################################################################
ALTER TABLE `dept`
    ADD CONSTRAINT `fk_dept_leader` FOREIGN KEY (`leader_id`) REFERENCES `employee` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================================
--  建表完成统计（执行后可直接运行以下语句核对）
-- ------------------------------------------------------------------------------------
--  表数量：
--    SELECT COUNT(*) FROM information_schema.TABLES
--     WHERE TABLE_SCHEMA = 'enterprise_hr' AND TABLE_TYPE = 'BASE TABLE';
--    -- 预期 49
--
--  外键数量：
--    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
--     WHERE CONSTRAINT_SCHEMA = 'enterprise_hr' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
--    -- 预期 80
--
--  表清单（含注释）：
--    SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
--     WHERE TABLE_SCHEMA = 'enterprise_hr' AND TABLE_TYPE = 'BASE TABLE'
--     ORDER BY TABLE_NAME;
-- ====================================================================================

