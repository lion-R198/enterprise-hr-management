-- ====================================================================================
--  企业人事管理系统 — 初始化基础数据脚本
-- ====================================================================================
--  数据库：enterprise_hr
--  依赖：需先执行 enterprise_hr_schema.sql 完成建库建表
-- ------------------------------------------------------------------------------------
--  用法：
--      mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_init_data.sql
-- ------------------------------------------------------------------------------------
--  内容清单：
--    一、系统角色（6 个，对应需求规格说明书 R1~R6）
--    二、菜单权限（9 个一级模块 + 功能按钮）
--    三、数据字典（性别/学历/员工状态/合同类型/异动类型等）
--    四、系统参数（考勤封账日、合同预警天数、加班折算规则等）
--    五、组织架构与岗位
--    六、员工与系统用户
--    七、考勤基础（班次、假别）
--    八、薪资基础（薪资标准、社保公积金配置）
-- ====================================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE `enterprise_hr`;

-- 清理已有初始化数据（按依赖关系无关顺序清空，便于脚本可重复执行）
DELETE FROM `sys_role_menu`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_user`;
DELETE FROM `sys_menu`;
DELETE FROM `sys_role`;
DELETE FROM `sys_dict_item`;
DELETE FROM `sys_dict_type`;
DELETE FROM `sys_config`;
DELETE FROM `insurance_config`;
DELETE FROM `salary_standard`;
DELETE FROM `leave_type`;
DELETE FROM `shift`;
DELETE FROM `employee_salary`;
DELETE FROM `position`;
DELETE FROM `employee`;
DELETE FROM `dept`;

ALTER TABLE `dept`              AUTO_INCREMENT = 1;
ALTER TABLE `employee`          AUTO_INCREMENT = 1000;
ALTER TABLE `sys_role`          AUTO_INCREMENT = 1;
ALTER TABLE `sys_user`          AUTO_INCREMENT = 1;
ALTER TABLE `sys_menu`          AUTO_INCREMENT = 1;
ALTER TABLE `sys_dict_type`     AUTO_INCREMENT = 1;
ALTER TABLE `sys_dict_item`     AUTO_INCREMENT = 1;
ALTER TABLE `sys_config`        AUTO_INCREMENT = 1;
ALTER TABLE `position`          AUTO_INCREMENT = 1;
ALTER TABLE `shift`             AUTO_INCREMENT = 1;
ALTER TABLE `leave_type`        AUTO_INCREMENT = 1;
ALTER TABLE `salary_standard`   AUTO_INCREMENT = 1;
ALTER TABLE `insurance_config`  AUTO_INCREMENT = 1;


-- ####################################################################################
--  一、系统角色（对应《需求规格说明书》2.4 用户角色定义）
-- ####################################################################################
INSERT INTO `sys_role` (`id`, `role_code`, `role_name`, `description`, `data_scope`, `role_type`, `sort_no`) VALUES
(1, 'ROLE_ADMIN',     '系统管理员', '负责系统基础配置：用户、角色、菜单、字典、参数及日志审计，不参与人事业务审批', 1, 1, 1),
(2, 'ROLE_HR',        '人事专员',   '员工全生命周期管理、部门岗位维护、招聘培训组织、考勤汇总、薪资核算与发放',       1, 1, 2),
(3, 'ROLE_MANAGER',   '部门主管',   '本部门员工查看、考勤与请假审批、绩效指标设定与评分、招聘需求提报',             3, 1, 3),
(4, 'ROLE_FINANCE',   '财务人员',   '薪资标准维护、薪资核算复核与发放、社保公积金与个税申报',                       1, 1, 4),
(5, 'ROLE_EMPLOYEE',  '普通员工',   '查看个人档案与薪资条、考勤打卡、请假加班出差申请、培训报名、查看绩效结果',       4, 1, 5),
(6, 'ROLE_EXECUTIVE', '决策层',     '跨部门数据看板与报表分析，查看人力成本、编制占用、绩效分布等汇总指标',           1, 1, 6);


-- ####################################################################################
--  二、菜单权限（9 个一级模块，对应功能结构图）
-- ####################################################################################
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `perms`, `icon`, `sort_no`) VALUES
-- 一级目录：9 大模块
(1,  NULL, '首页',    1, '/dashboard',   'dashboard:view',    'HomeFilled',   1),
(2,  NULL, '员工信息', 1, '/employee',    'employee:view',     'User',         2),
(3,  NULL, '部门管理', 1, '/dept',        'dept:view',         'OfficeBuilding', 3),
(4,  NULL, '考勤管理', 1, '/attendance',  'attendance:view',   'Clock',        4),
(5,  NULL, '薪资管理', 1, '/salary',      'salary:view',       'Money',        5),
(6,  NULL, '招聘管理', 1, '/recruit',     'recruit:view',      'Search',       6),
(7,  NULL, '培训管理', 1, '/training',    'training:view',     'Reading',      7),
(8,  NULL, '绩效考核', 1, '/performance', 'performance:view',  'TrendCharts',  8),
(9,  NULL, '系统设置', 1, '/system',      'system:view',       'Setting',      9),
-- 二级菜单：功能入口
(11, 1, '数据概览',   2, '/dashboard/index',    'dashboard:index',    NULL, 1),
(12, 1, '待办事项',   2, '/dashboard/todo',     'dashboard:todo',     NULL, 2),
(13, 1, '通知公告',   2, '/dashboard/notice',   'dashboard:notice',   NULL, 3),
(21, 2, '员工档案',   2, '/employee/list',      'employee:list',      NULL, 1),
(22, 2, '入职办理',   2, '/employee/onboard',   'employee:onboard',   NULL, 2),
(23, 2, '调动转岗',   2, '/employee/transfer',  'employee:transfer',  NULL, 3),
(24, 2, '离职办理',   2, '/employee/leave',     'employee:leave',     NULL, 4),
(25, 2, '合同管理',   2, '/employee/contract',  'employee:contract',  NULL, 5),
(31, 3, '组织架构',   2, '/dept/tree',          'dept:tree',          NULL, 1),
(32, 3, '部门维护',   2, '/dept/list',          'dept:list',          NULL, 2),
(33, 3, '岗位管理',   2, '/dept/position',      'dept:position',      NULL, 3),
(34, 3, '人员编制',   2, '/dept/headcount',     'dept:headcount',     NULL, 4),
(41, 4, '打卡记录',   2, '/attendance/record',  'attendance:record',  NULL, 1),
(42, 4, '请假申请',   2, '/attendance/leave',   'attendance:leave',   NULL, 2),
(43, 4, '加班申请',   2, '/attendance/overtime','attendance:overtime',NULL, 3),
(44, 4, '出差管理',   2, '/attendance/trip',    'attendance:trip',    NULL, 4),
(45, 4, '排班管理',   2, '/attendance/shift',   'attendance:shift',   NULL, 5),
(46, 4, '考勤统计',   2, '/attendance/stat',    'attendance:stat',    NULL, 6),
(51, 5, '薪资标准',   2, '/salary/standard',    'salary:standard',    NULL, 1),
(52, 5, '薪资核算',   2, '/salary/calc',        'salary:calc',        NULL, 2),
(53, 5, '薪资发放',   2, '/salary/pay',         'salary:pay',         NULL, 3),
(54, 5, '社保公积金', 2, '/salary/insurance',   'salary:insurance',   NULL, 4),
(55, 5, '个税申报',   2, '/salary/tax',         'salary:tax',         NULL, 5),
(56, 5, '薪资条查询', 2, '/salary/payslip',     'salary:payslip',     NULL, 6),
(61, 6, '招聘需求',   2, '/recruit/request',    'recruit:request',    NULL, 1),
(62, 6, '职位发布',   2, '/recruit/posting',    'recruit:posting',    NULL, 2),
(63, 6, '简历筛选',   2, '/recruit/candidate',  'recruit:candidate',  NULL, 3),
(64, 6, '面试安排',   2, '/recruit/interview',  'recruit:interview',  NULL, 4),
(65, 6, '录用审批',   2, '/recruit/offer',      'recruit:offer',      NULL, 5),
(66, 6, '人才库',     2, '/recruit/talent',     'recruit:talent',     NULL, 6),
(71, 7, '培训计划',   2, '/training/plan',      'training:plan',      NULL, 1),
(72, 7, '课程管理',   2, '/training/course',    'training:course',    NULL, 2),
(73, 7, '讲师管理',   2, '/training/trainer',   'training:trainer',   NULL, 3),
(74, 7, '报名签到',   2, '/training/enroll',    'training:enroll',    NULL, 4),
(75, 7, '培训考核',   2, '/training/exam',      'training:exam',      NULL, 5),
(76, 7, '培训档案',   2, '/training/record',    'training:record',    NULL, 6),
(81, 8, '考核方案',   2, '/performance/plan',   'performance:plan',   NULL, 1),
(82, 8, '指标设置',   2, '/performance/kpi',    'performance:kpi',    NULL, 2),
(83, 8, '绩效评分',   2, '/performance/score',  'performance:score',  NULL, 3),
(84, 8, '绩效面谈',   2, '/performance/interview','performance:interview', NULL, 4),
(85, 8, '结果归档',   2, '/performance/archive','performance:archive',NULL, 5),
(86, 8, '绩效分析',   2, '/performance/analysis','performance:analysis',NULL, 6),
(91, 9, '用户管理',   2, '/system/user',       'system:user',        NULL, 1),
(92, 9, '角色权限',   2, '/system/role',       'system:role',        NULL, 2),
(93, 9, '菜单管理',   2, '/system/menu',       'system:menu',        NULL, 3),
(94, 9, '数据字典',   2, '/system/dict',       'system:dict',        NULL, 4),
(95, 9, '参数配置',   2, '/system/config',     'system:config',      NULL, 5),
(96, 9, '日志审计',   2, '/system/log',        'system:log',         NULL, 6),
-- 三级按钮：以「员工信息」为例
(211, 21, '新增员工', 3, NULL, 'employee:add',    NULL, 1),
(212, 21, '修改员工', 3, NULL, 'employee:edit',   NULL, 2),
(213, 21, '删除员工', 3, NULL, 'employee:remove', NULL, 3),
(214, 21, '导出员工', 3, NULL, 'employee:export', NULL, 4);

-- 角色菜单授权
-- R1 系统管理员：首页 + 系统设置全部 + 其余模块只读
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `id` FROM `sys_menu` WHERE `id` IN (1, 9, 11, 12, 13, 91, 92, 93, 94, 95, 96)
   OR `id` IN (2, 3, 4, 5, 8);
-- R2 人事专员：除系统设置外全部业务模块
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` <> 9;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 3, 4, 5, 6, 7, 8);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` = 21;
-- R3 部门主管：首页 + 员工 + 考勤 + 招聘 + 绩效
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` IN (1, 2, 4, 6, 8, 9);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 3, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 4, 6, 8, 9);
-- R4 财务人员：首页 + 员工(只读) + 薪资
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 4, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` IN (1, 2, 5);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 4, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 5);
-- R5 普通员工：首页 + 个人相关
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 1), (5, 11), (5, 12), (5, 13), (5, 42), (5, 43), (5, 44), (5, 56), (5, 74);
-- R6 决策层：首页 + 各模块查看
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 6, `id` FROM `sys_menu` WHERE `parent_id` IS NULL;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 6, `id` FROM `sys_menu` WHERE `perms` IN
('dashboard:index','dashboard:todo','dashboard:notice','dept:tree','dept:headcount',
 'attendance:stat','salary:calc','recruit:request','performance:analysis','training:record');


-- ####################################################################################
--  三、数据字典
-- ####################################################################################
INSERT INTO `sys_dict_type` (`id`, `dict_type`, `dict_name`, `remark`) VALUES
(1,  'gender',           '性别',     '与 employee.gender 对应，存 M/F/U'),
(2,  'education',        '学历',     '与 employee.education 状态码对应'),
(3,  'marital_status',   '婚姻状况', '与 employee.marital_status 对应'),
(4,  'political_status', '政治面貌', '与 employee.political_status 对应'),
(5,  'employee_status',  '在职状态', '与 employee.status 对应'),
(6,  'employee_type',    '用工性质', '与 employee.employee_type 对应'),
(7,  'contract_type',    '合同类型', '与 employee_contract.contract_type 对应'),
(8,  'change_type',      '异动类型', '与 employee_change.change_type 对应'),
(9,  'overtime_type',    '加班类型', '与 overtime_request.overtime_type 对应'),
(10, 'approval_status',  '审批状态', '各申请单通用审批状态');

INSERT INTO `sys_dict_item` (`dict_type_id`, `item_value`, `item_label`, `sort_no`, `is_default`) VALUES
-- 性别
(1, 'M', '男',   1, 0), (1, 'F', '女', 2, 0), (1, 'U', '未知', 3, 1),
-- 学历
(2, '1', '博士', 1, 0), (2, '2', '硕士', 2, 0), (2, '3', '本科', 3, 1),
(2, '4', '大专', 4, 0), (2, '5', '中专', 5, 0), (2, '6', '高中', 6, 0), (2, '7', '其他', 7, 0),
-- 婚姻状况
(3, '1', '未婚', 1, 1), (3, '2', '已婚', 2, 0), (3, '3', '离异', 3, 0), (3, '4', '丧偶', 4, 0),
-- 政治面貌
(4, '1', '群众', 1, 1), (4, '2', '共青团员', 2, 0), (4, '3', '中共党员', 3, 0), (4, '4', '民主党派', 4, 0),
-- 在职状态
(5, '0', '离职', 1, 0), (5, '1', '在职', 2, 1), (5, '2', '试用', 3, 0), (5, '3', '待入职', 4, 0),
-- 用工性质
(6, '1', '正式', 1, 1), (6, '2', '试用', 2, 0), (6, '3', '实习', 3, 0), (6, '4', '兼职', 4, 0), (6, '5', '劳务', 5, 0),
-- 合同类型
(7, '1', '固定期限', 1, 1), (7, '2', '无固定期限', 2, 0), (7, '3', '以完成任务为期限', 3, 0),
(7, '4', '实习协议', 4, 0), (7, '5', '劳务协议', 5, 0),
-- 异动类型
(8, '1', '入职', 1, 0), (8, '2', '转正', 2, 0), (8, '3', '调岗', 3, 0), (8, '4', '调部门', 4, 0),
(8, '5', '晋升', 5, 0), (8, '6', '降级', 6, 0), (8, '7', '离职', 7, 0), (8, '8', '返聘', 8, 0),
-- 加班类型
(9, '1', '工作日加班', 1, 1), (9, '2', '休息日加班', 2, 0), (9, '3', '法定节假日加班', 3, 0),
-- 审批状态
(10, '0', '草稿', 1, 0), (10, '1', '审批中', 2, 1), (10, '2', '已通过', 3, 0),
(10, '3', '已驳回', 4, 0), (10, '4', '已撤销', 5, 0);


-- ####################################################################################
--  四、系统参数
-- ####################################################################################
INSERT INTO `sys_config` (`config_key`, `config_name`, `config_value`, `config_group`, `value_type`, `is_system`, `remark`) VALUES
('attendance.lock.day',        '考勤封账日',        '5',        'attendance', 2, 1, '每月第几个工作日封账，封账后考勤数据禁改'),
('attendance.late.minutes',    '迟到判定分钟数',    '5',        'attendance', 2, 1, '超过上班时间该分钟数判定为迟到'),
('attendance.absent.minutes',  '旷工判定分钟数',    '120',      'attendance', 2, 1, '迟到超过该分钟数判定为旷工'),
('attendance.workday.hours',   '标准日工时',        '8',        'attendance', 2, 1, '用于折算加班与缺勤扣款'),
('overtime.rate.workday',      '工作日加班倍率',    '1.5',      'attendance', 1, 0, '工作日加班费倍率'),
('overtime.rate.weekend',      '休息日加班倍率',    '2.0',      'attendance', 1, 0, '休息日加班费倍率'),
('overtime.rate.holiday',      '法定节假日加班倍率','3.0',      'attendance', 1, 0, '法定节假日加班费倍率'),
('salary.tax.threshold',       '个税起征点',        '5000',     'salary',     2, 1, '个税累计预扣法起征点(元/月)'),
('salary.pay.day',             '薪资发放日',        '15',       'salary',     2, 0, '每月发薪日'),
('salary.probation.ratio',     '试用期薪资比例',    '0.8',      'salary',     1, 0, '试用期工资占转正工资比例'),
('contract.expire.warn.days',  '合同到期预警天数',  '30',       'recruit',    2, 1, '合同到期前多少天开始预警'),
('probation.warn.days',        '转正提醒天数',      '7',        'recruit',    2, 0, '试用期到期前多少天提醒办理转正'),
('recruit.offer.valid.days',   'Offer有效期天数',   '7',        'recruit',    2, 0, 'Offer发出后多少天内需候选人确认'),
('performance.appeal.days',    '绩效申诉期天数',    '5',        'system',     2, 0, '结果确认后多少天内可申诉'),
('system.login.fail.limit',    '登录失败锁定次数',  '5',        'system',     2, 1, '连续失败多少次锁定账号'),
('system.login.lock.minutes',  '账号锁定时长(分钟)','30',       'system',     2, 1, '账号锁定持续时间');


-- ####################################################################################
--  五、组织架构与岗位
-- ####################################################################################
INSERT INTO `dept` (`id`, `dept_code`, `name`, `parent_id`, `leader`, `dept_type`, `dept_level`, `dept_path`, `headcount_limit`, `phone`, `sort_no`) VALUES
(1, 'D1001', '研发部',     NULL, '张总监', 3, 1, '/1/',   30, '13800000001', 1),
(2, 'D1002', '产品部',     NULL, '李总监', 3, 1, '/2/',   15, '13800000002', 2),
(3, 'D1003', '人力资源部', NULL, '王总监', 3, 1, '/3/',    8, '13800000003', 3),
(4, 'D1004', '财务部',     NULL, '赵总监', 3, 1, '/4/',    6, '13800000004', 4);

INSERT INTO `position` (`id`, `position_code`, `position_name`, `position_seq`, `job_level`, `dept_id`, `duty`, `headcount_limit`, `sort_no`) VALUES
(1, 'P-TECH-001', 'Java开发工程师', 2, 'P4-P7', 1, '负责后端服务的设计、开发与维护', 10, 1),
(2, 'P-TECH-002', '前端开发工程师', 2, 'P4-P7', 1, '负责Web前端页面开发与交互实现',     6, 2),
(3, 'P-TECH-003', '测试工程师',     2, 'P4-P6', 1, '负责功能测试、自动化测试与质量保障', 5, 3),
(4, 'P-PROD-001', '产品经理',       3, 'P5-P8', 2, '负责产品规划、需求分析与版本迭代',   4, 4),
(5, 'P-PROD-002', '产品助理',       3, 'P3-P5', 2, '协助产品经理完成需求整理与文档编写', 3, 5),
(6, 'P-HR-001',   'HR专员',         3, 'P4-P6', 3, '负责招聘、员工关系与考勤管理',       4, 6),
(7, 'P-HR-002',   'HR经理',         1, 'M1-M3', 3, '统筹人力资源各模块工作',             1, 7),
(8, 'P-FIN-001',  '会计',           3, 'P4-P7', 4, '负责账务处理、薪资核算与税务申报',   3, 8);


-- ####################################################################################
--  六、员工与系统用户
-- ####################################################################################
INSERT INTO `employee`
    (`id`, `emp_no`, `name`, `gender`, `age`, `birth_date`, `email`, `phone`,
     `education`, `dept_id`, `position_id`, `position`, `job_level`, `employee_type`,
     `hire_date`, `manager_id`, `salary`, `status`) VALUES
(1001, 'E0001', '张三', 'M', 28, '1998-05-12', 'zhangsan@enterprise.com', '13900000001', 3, 1, 1, 'Java开发工程师', 'P6', 1, '2023-03-15', NULL, 15000.00, 1),
(1002, 'E0002', '李四', 'F', 26, '2000-08-20', 'lisi@enterprise.com',     '13900000002', 3, 1, 2, '前端开发工程师', 'P5', 1, '2023-05-20', 1001, 14000.00, 1),
(1003, 'E0003', '王五', 'M', 32, '1994-02-08', 'wangwu@enterprise.com',   '13900000003', 2, 2, 4, '产品经理',       'P7', 1, '2022-08-10', NULL, 18000.00, 1),
(1004, 'E0004', '赵六', 'F', 29, '1997-11-03', 'zhaoliu@enterprise.com',  '13900000004', 3, 3, 6, 'HR专员',         'P5', 1, '2024-01-05', 1008, 10000.00, 1),
(1005, 'E0005', '钱七', 'M', 35, '1991-07-19', 'qianqi@enterprise.com',   '13900000005', 3, 4, 8, '会计',           'P6', 1, '2021-11-20', NULL, 16000.00, 1),
(1006, 'E0006', '孙八', 'M', 24, '2002-03-25', 'sunba@enterprise.com',    '13900000006', 3, 1, 3, '测试工程师',     'P4', 1, '2024-06-01', 1001, 12000.00, 1),
(1007, 'E0007', '周九', 'F', 27, '1999-09-14', 'zhoujiu@enterprise.com',  '13900000007', 3, 2, 5, '产品助理',       'P4', 1, '2024-03-10', 1003, 11000.00, 1),
(1008, 'E0008', '吴十', 'M', 30, '1996-01-30', 'wushi@enterprise.com',    '13900000008', 3, 3, 7, 'HR经理',         'M2', 1, '2022-04-25', NULL, 20000.00, 0);

-- 补充部门负责人（循环依赖外键，employee 建好后再更新）
UPDATE `dept` SET `leader_id` = 1001 WHERE `id` = 1;
UPDATE `dept` SET `leader_id` = 1003 WHERE `id` = 2;
UPDATE `dept` SET `leader_id` = 1008 WHERE `id` = 3;
UPDATE `dept` SET `leader_id` = 1005 WHERE `id` = 4;

-- 系统用户（演示账号，密码统一为 123456 的 BCrypt 哈希；生产环境请立即修改）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `employee_id`, `dept_id`, `status`) VALUES
(1, 'admin',     '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', NULL,  NULL, 1),
(2, 'hr01',      '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '赵六',       1004, 3,    1),
(3, 'manager01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三',       1001, 1,    1),
(4, 'finance01', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '钱七',       1005, 4,    1),
(5, 'emp01',     '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四',       1002, 1,    1);

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1),  -- admin     → 系统管理员
(2, 2),  -- hr01      → 人事专员
(3, 3),  -- manager01 → 部门主管
(4, 4),  -- finance01 → 财务人员
(5, 5);  -- emp01     → 普通员工


-- ####################################################################################
--  七、考勤基础数据
-- ####################################################################################
INSERT INTO `shift` (`id`, `shift_code`, `shift_name`, `work_start`, `work_end`, `rest_minutes`, `standard_hours`, `late_tolerance`, `early_tolerance`, `sort_no`) VALUES
(1, 'STD',   '标准班(9:00-18:00)', '09:00:00', '18:00:00', 60, 8.0, 5, 5, 1),
(2, 'EARLY', '早班(8:00-17:00)',   '08:00:00', '17:00:00', 60, 8.0, 5, 5, 2),
(3, 'FLEX',  '弹性班(10:00-19:00)', '10:00:00', '19:00:00', 60, 8.0, 10, 10, 3);

INSERT INTO `leave_type` (`id`, `type_code`, `type_name`, `is_paid`, `unit`, `min_unit`, `max_days_per_year`, `valid_months`, `can_carry_over`, `need_proof`, `sort_no`, `remark`) VALUES
(1, 'ANNUAL',    '年假',   1, 1, 0.5, 15.0, 12, 1, 0, 1, '按司龄折算，可结转至次年3月'),
(2, 'SICK',      '病假',   1, 1, 0.5, 30.0, 12, 0, 1, 2, '需提供医院证明'),
(3, 'PERSONAL',  '事假',   0, 1, 0.5,  0.0, 12, 0, 0, 3, '不带薪，按日扣减工资'),
(4, 'MARRIAGE',  '婚假',   1, 1, 1.0, 10.0, 12, 0, 1, 4, '需提供结婚证'),
(5, 'MATERNITY', '产假',   1, 1, 1.0,158.0, 24, 0, 1, 5, '按国家规定执行'),
(6, 'PATERNITY', '陪产假', 1, 1, 0.5, 15.0, 12, 0, 1, 6, '男职工陪产假'),
(7, 'BEREAVE',   '丧假',   1, 1, 1.0,  3.0, 12, 0, 1, 7, '直系亲属去世'),
(8, 'ANNUAL_ADJ','调休',   1, 2, 1.0,  0.0, 12, 0, 0, 8, '加班换取的调休，按小时计算');


-- ####################################################################################
--  八、薪资基础数据
-- ####################################################################################
INSERT INTO `salary_standard` (`id`, `standard_code`, `standard_name`, `position_seq`, `job_level`, `base_salary`, `post_salary`, `performance_salary`, `allowance`, `effective_date`, `remark`) VALUES
(1, 'STD-TECH-P4', 'P4技术岗标准', 2, 'P4',  8000.00, 3000.00, 2000.00, 1000.00, '2026-01-01', '初级技术岗'),
(2, 'STD-TECH-P6', 'P6技术岗标准', 2, 'P6', 10000.00, 4500.00, 3500.00, 1500.00, '2026-01-01', '资深技术岗'),
(3, 'STD-MGMT-M2', 'M2管理岗标准', 1, 'M2', 12000.00, 6000.00, 5000.00, 2000.00, '2026-01-01', '中层管理岗');

INSERT INTO `insurance_config` (`id`, `config_name`, `city`, `social_type`, `company_rate`, `personal_rate`, `base_min`, `base_max`, `effective_date`) VALUES
(1, '2026年北京社保-养老保险',    '北京', 1, 0.1600, 0.0800, 6821.00, 35283.00, '2026-01-01'),
(2, '2026年北京社保-医疗保险',    '北京', 2, 0.0980, 0.0200, 6821.00, 35283.00, '2026-01-01'),
(3, '2026年北京社保-失业保险',    '北京', 3, 0.0050, 0.0050, 6821.00, 35283.00, '2026-01-01'),
(4, '2026年北京社保-工伤保险',    '北京', 4, 0.0020, 0.0000, 6821.00, 35283.00, '2026-01-01'),
(5, '2026年北京社保-生育保险',    '北京', 5, 0.0080, 0.0000, 6821.00, 35283.00, '2026-01-01'),
(6, '2026年北京住房公积金-12%',   '北京', 6, 0.1200, 0.1200, 2420.00, 35283.00, '2026-01-01');

-- 员工薪资标准（当前生效记录）
INSERT INTO `employee_salary` (`employee_id`, `base_salary`, `post_salary`, `performance_salary`, `allowance`,
                               `social_base`, `fund_base`, `fund_rate`, `effective_date`, `is_current`) VALUES
(1001,  9000.00, 4000.00, 2000.00, 0.00, 15000.00, 15000.00, 12.00, '2025-01-01', 1),
(1002,  8000.00, 3000.00, 3000.00, 0.00, 14000.00, 14000.00, 12.00, '2025-01-01', 1),
(1003, 10000.00, 5000.00, 3000.00, 0.00, 18000.00, 18000.00, 12.00, '2025-01-01', 1),
(1004,  6000.00, 2500.00, 1500.00, 0.00, 10000.00, 10000.00, 12.00, '2025-01-01', 1),
(1005,  9000.00, 4500.00, 2500.00, 0.00, 16000.00, 16000.00, 12.00, '2025-01-01', 1),
(1006,  7000.00, 3000.00, 2000.00, 0.00, 12000.00, 12000.00, 12.00, '2025-06-01', 1),
(1007,  6500.00, 2500.00, 2000.00, 0.00, 11000.00, 11000.00, 12.00, '2025-01-01', 1),
(1008, 11000.00, 5500.00, 3500.00, 0.00, 20000.00, 20000.00, 12.00, '2025-01-01', 1);

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================================
--  初始化完成统计
-- ------------------------------------------------------------------------------------
--    SELECT '部门' AS 对象, COUNT(*) AS 数量 FROM dept
--    UNION ALL SELECT '岗位', COUNT(*) FROM position
--    UNION ALL SELECT '员工', COUNT(*) FROM employee
--    UNION ALL SELECT '角色', COUNT(*) FROM sys_role
--    UNION ALL SELECT '用户', COUNT(*) FROM sys_user
--    UNION ALL SELECT '菜单', COUNT(*) FROM sys_menu
--    UNION ALL SELECT '字典类型', COUNT(*) FROM sys_dict_type
--    UNION ALL SELECT '字典项', COUNT(*) FROM sys_dict_item
--    UNION ALL SELECT '系统参数', COUNT(*) FROM sys_config
--    UNION ALL SELECT '班次', COUNT(*) FROM shift
--    UNION ALL SELECT '假别', COUNT(*) FROM leave_type
--    UNION ALL SELECT '薪资标准', COUNT(*) FROM salary_standard
--    UNION ALL SELECT '社保配置', COUNT(*) FROM insurance_config;
-- ====================================================================================
