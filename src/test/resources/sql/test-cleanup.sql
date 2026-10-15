-- ====================================================================
-- Test cleanup & seed
-- 每个集成测试类用 @Sql(scripts = "/sql/test-cleanup.sql",
--   executionPhase = BEFORE_TEST_METHOD) 加载
--
-- 说明：
--  1) 建库脚本 enterprise_hr_schema.sql 为业务表补充了大量外键约束，
--     因此这里必须先关闭外键检查，再按逆依赖顺序清空，避免 RESTRICT 报错。
--  2) 这里的 DELETE 是**硬删除**，避免 MyBatis-Plus 逻辑删除影响测试断言。
--  3) 只重置 Employee / Dept 相关的种子数据，其余基础配置（字典、参数等）保持不变。
--  4) 【鉴权契约】新契约下 AuthInterceptor 是 fail-close 的：除 /auth/login 外
--     所有接口都要 Bearer 令牌。本脚本会清空 sys_user，因此必须在此重建
--     管理员账号（admin / admin123）与其 ROLE_ADMIN 绑定，否则 HTTP 集成测试
--     连登录这一步都过不去（全部 401）。
--     口令为 BCrypt 哈希（与运行环境实际口令 admin123 一致），不得改成明文。
-- ====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 清空业务数据（逆依赖顺序，从最末端的明细表开始）
DELETE FROM `sys_role_menu`;
DELETE FROM `sys_user_role`;
DELETE FROM `notice_read`;
DELETE FROM `salary_item`;
DELETE FROM `training_enroll`;
DELETE FROM `employee_attachment`;
DELETE FROM `employee_change`;
DELETE FROM `employee_contract`;
DELETE FROM `employee_schedule`;
DELETE FROM `performance_interview`;
DELETE FROM `performance_indicator`;
DELETE FROM `interview_record`;
DELETE FROM `performance_record`;
DELETE FROM `training_record`;
DELETE FROM `employee_salary`;
DELETE FROM `insurance_record`;
DELETE FROM `attendance_record`;
DELETE FROM `attendance_month`;
DELETE FROM `leave_balance`;
DELETE FROM `leave_request`;
DELETE FROM `overtime_request`;
DELETE FROM `business_trip`;
DELETE FROM `job_posting`;
DELETE FROM `candidate`;
DELETE FROM `offer_record`;
DELETE FROM `recruit_request`;
DELETE FROM `course`;
DELETE FROM `training_plan`;
DELETE FROM `trainer`;
DELETE FROM `salary_record`;
DELETE FROM `sys_user`;
DELETE FROM `notice`;
DELETE FROM `todo_task`;
DELETE FROM `approval_record`;
DELETE FROM `performance_plan`;
DELETE FROM `employee`;
DELETE FROM `position`;
DELETE FROM `dept`;

ALTER TABLE `employee` AUTO_INCREMENT = 1000;
ALTER TABLE `dept`     AUTO_INCREMENT = 1;
ALTER TABLE `position` AUTO_INCREMENT = 1;

-- 重建最小种子数据（与集成测试断言保持一致）
INSERT INTO `dept` (`id`, `dept_code`, `name`, `leader`, `phone`) VALUES
    (1, 'D1001', '研发部',     '张总监', '13800000001'),
    (2, 'D1002', '产品部',     '李总监', '13800000002');

INSERT INTO `position` (`id`, `position_code`, `position_name`, `position_seq`, `dept_id`, `sort_no`) VALUES
    (1, 'P-TECH-001', 'Java开发工程师', 2, 1, 1),
    (2, 'P-TECH-002', '前端开发工程师', 2, 1, 2),
    (3, 'P-PROD-001', '产品经理',       3, 2, 3);

INSERT INTO `employee`
    (`id`, `emp_no`, `name`, `gender`, `age`, `email`, `phone`, `dept_id`, `position_id`, `position`, `hire_date`, `salary`, `status`)
VALUES
    (1001, 'E0001', '张三', 'M', 28, 'zhangsan@enterprise.com',  '13900000001', 1, 1, 'Java开发工程师', '2023-03-15', 15000.00, 1),
    (1002, 'E0002', '李四', 'F', 26, 'lisi@enterprise.com',      '13900000002', 1, 2, '前端开发工程师', '2023-05-20', 14000.00, 1),
    (1003, 'E0003', '王五', 'M', 32, 'wangwu@enterprise.com',     '13900000003', 2, 3, '产品经理',       '2022-08-10', 18000.00, 1);

-- 管理员账号（admin / admin123）：fail-close 鉴权下的唯一登录入口，HTTP 集成测试强依赖。
-- 该用户不绑定员工档案（employee_id / dept_id 为 NULL），
-- 与生产环境一致：管理员拥有全部接口权限（AuthInterceptor 内置放行 ROLE_ADMIN）。
-- 口令哈希与 enterprise_hr_init_data.sql / data.sql 中的 admin 完全一致（BCrypt，明文 admin123）；
-- 注意那两个种子脚本的注释写的是"密码统一 123456"，该注释已过期，实际口令是 admin123。
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `status`, `login_fail_count`)
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
        '系统管理员', 1, 0);

-- 角色 ID 用子查询取，避免依赖 sys_role 的自增主键具体取值
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT 1, `id` FROM `sys_role` WHERE `role_code` = 'ROLE_ADMIN';

SET FOREIGN_KEY_CHECKS = 1;
