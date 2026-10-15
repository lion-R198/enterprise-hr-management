# enterprise_hr 数据库字段参考（自动生成，供后端实体对齐使用）

数据库：`enterprise_hr`　字符集：utf8mb4　表数量：49

> 说明：`PK`=主键，`UK`=唯一键，`FK`=外键。建表 DDL 详见 `enterprise_hr_schema.sql`。

## 表清单

| 表名 | 表注释 | 字段数 |
|---|---|---|
| `approval_record` | 通用审批记录表 | 12 |
| `attendance_month` | 考勤月报表 | 21 |
| `attendance_record` | 考勤打卡记录表 | 17 |
| `business_trip` | 出差申请表 | 16 |
| `candidate` | 候选人表 | 24 |
| `course` | 课程表 | 16 |
| `dept` | 部门表 | 16 |
| `employee` | 员工表 | 42 |
| `employee_attachment` | 员工附件表 | 11 |
| `employee_change` | 员工异动记录表 | 19 |
| `employee_contract` | 劳动合同表 | 19 |
| `employee_salary` | 员工薪资标准表 | 19 |
| `employee_schedule` | 员工排班表 | 8 |
| `insurance_config` | 社保公积金配置表 | 14 |
| `insurance_record` | 社保缴纳记录表 | 24 |
| `interview_record` | 面试记录表 | 14 |
| `job_posting` | 职位发布表 | 13 |
| `leave_balance` | 假期余额表 | 12 |
| `leave_request` | 请假申请表 | 15 |
| `leave_type` | 假别表 | 16 |
| `notice` | 通知公告表 | 15 |
| `notice_read` | 公告已读记录表 | 4 |
| `offer_record` | 录用通知表 | 20 |
| `overtime_request` | 加班申请表 | 16 |
| `performance_indicator` | 考核指标表 | 18 |
| `performance_interview` | 绩效面谈记录表 | 13 |
| `performance_plan` | 考核方案表 | 23 |
| `performance_record` | 绩效考核记录表 | 21 |
| `position` | 岗位表 | 15 |
| `recruit_request` | 招聘需求表 | 24 |
| `salary_item` | 薪资明细项表 | 10 |
| `salary_record` | 月度薪资记录表 | 30 |
| `salary_standard` | 薪资标准模板表 | 15 |
| `shift` | 班次定义表 | 16 |
| `sys_config` | 系统参数表 | 11 |
| `sys_dict_item` | 字典项表 | 12 |
| `sys_dict_type` | 字典类型表 | 8 |
| `sys_login_log` | 登录日志表 | 10 |
| `sys_menu` | 菜单权限表 | 14 |
| `sys_oper_log` | 操作日志表 | 15 |
| `sys_role` | 角色表 | 13 |
| `sys_role_menu` | 角色菜单关联表 | 4 |
| `sys_user` | 系统用户表 | 18 |
| `sys_user_role` | 用户角色关联表 | 4 |
| `todo_task` | 待办任务表 | 15 |
| `trainer` | 讲师表 | 18 |
| `training_enroll` | 培训报名表 | 14 |
| `training_plan` | 培训计划表 | 18 |
| `training_record` | 培训档案表 | 15 |

---

## `approval_record`

通用审批记录表

外键：approver_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `biz_type` | tinyint | NO |  | NULL | 业务类型:1请假 2加班 3出差 4员工异动 5离职 6招聘需求 7录用 8调薪 |
| `biz_id` | bigint | NO |  | NULL | 业务单据ID(多态指向不同业务表,故不设外键) |
| `biz_no` | varchar(64) | YES |  | NULL | 业务单号(冗余,便于检索) |
| `approval_level` | int | NO |  | 1 | 审批层级,从1开始 |
| `approver_id` | bigint | YES |  | NULL | 审批人员工ID |
| `approver_name` | varchar(32) | YES |  | NULL | 审批人姓名(快照) |
| `approval_status` | tinyint | NO |  | 1 | 审批结果:1待审批 2已同意 3已驳回 4已转交 |
| `approval_opinion` | varchar(500) | YES |  | NULL | 审批意见 |
| `approval_time` | datetime | YES |  | NULL | 审批时间 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |

## `attendance_month`

考勤月报表

外键：dept_id → dept；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID(冗余,便于部门维度统计) |
| `record_year` | smallint | NO |  | NULL | 统计年份 |
| `record_month` | tinyint | NO |  | NULL | 统计月份:1-12 |
| `should_days` | decimal(6,1) | NO |  | 0.0 | 应出勤天数 |
| `actual_days` | decimal(6,1) | NO |  | 0.0 | 实际出勤天数 |
| `late_count` | int | NO |  | 0 | 迟到次数 |
| `early_count` | int | NO |  | 0 | 早退次数 |
| `absent_count` | int | NO |  | 0 | 旷工次数 |
| `missing_count` | int | NO |  | 0 | 缺卡次数 |
| `leave_days` | decimal(6,1) | NO |  | 0.0 | 请假天数合计 |
| `overtime_hours` | decimal(6,1) | NO |  | 0.0 | 加班时长合计(小时) |
| `trip_days` | decimal(6,1) | NO |  | 0.0 | 出差天数合计 |
| `lock_status` | tinyint | NO |  | 0 | 封账状态:0未封账 1已封账(禁改) |
| `lock_time` | datetime | YES |  | NULL | 封账时间 |
| `lock_by` | bigint | YES |  | NULL | 封账操作人用户ID |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `attendance_record`

考勤打卡记录表

外键：employee_id → employee；shift_id → shift

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `attendance_date` | date | NO |  | NULL | 考勤日期 |
| `shift_id` | bigint | YES |  | NULL | 排班班次ID(NULL=使用默认班次) |
| `first_check_in` | datetime | YES |  | NULL | 首次上班打卡时间 |
| `last_check_out` | datetime | YES |  | NULL | 末次下班打卡时间 |
| `work_hours` | decimal(5,1) | NO |  | 0.0 | 实际出勤工时(小时) |
| `late_minutes` | int | NO |  | 0 | 迟到分钟数 |
| `early_minutes` | int | NO |  | 0 | 早退分钟数 |
| `check_in_status` | tinyint | NO |  | 1 | 上班打卡状态:1正常 2迟到 3严重迟到 |
| `check_out_status` | tinyint | NO |  | 1 | 下班打卡状态:1正常 2早退 3缺卡 |
| `attendance_status` | tinyint | NO |  | 1 | 考勤结果:1正常 2迟到 3早退 4缺卡 5旷工 6休假 7出差 8调休 |
| `source` | tinyint | NO |  | 1 | 数据来源:1考勤设备 2移动端打卡 3补卡审批 |
| `remark` | varchar(255) | YES |  | NULL | 备注(补卡原因/异常说明) |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `business_trip`

出差申请表

外键：current_approver_id → employee；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `request_no` | varchar(64) | NO | UK | NULL | 申请单号(唯一),如 BT20260901001 |
| `employee_id` | bigint | NO |  | NULL | 申请人员工ID |
| `destination` | varchar(128) | NO |  | NULL | 出差目的地 |
| `purpose` | varchar(500) | YES |  | NULL | 出差事由 |
| `start_time` | datetime | NO |  | NULL | 出差开始时间 |
| `end_time` | datetime | NO |  | NULL | 出差结束时间 |
| `days` | decimal(6,1) | NO |  | NULL | 出差天数 |
| `transport` | tinyint | YES |  | NULL | 交通方式:1飞机 2高铁 3汽车 4自驾 5其他 |
| `budget_amount` | decimal(12,2) | NO |  | 0.00 | 预算金额 |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销 |
| `current_approver_id` | bigint | YES |  | NULL | 当前审批人员工ID |
| `apply_time` | datetime | NO |  | CURRENT_TIMESTAMP | 申请时间 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `candidate`

候选人表

外键：referrer_id → employee；recruit_request_id → recruit_request

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `recruit_request_id` | bigint | NO |  | NULL | 关联招聘需求ID |
| `name` | varchar(32) | NO |  | NULL | 候选人姓名 |
| `gender` | varchar(8) | YES |  | NULL | 性别:M男 F女 U未知 |
| `birth_date` | date | YES |  | NULL | 出生日期 |
| `phone` | varchar(20) | YES |  | NULL | 联系电话 |
| `email` | varchar(64) | YES |  | NULL | 电子邮箱 |
| `education` | tinyint | YES |  | NULL | 最高学历:1博士 2硕士 3本科 4大专 5中专 6高中 7其他 |
| `graduate_school` | varchar(64) | YES |  | NULL | 毕业院校 |
| `major` | varchar(64) | YES |  | NULL | 所学专业 |
| `work_years` | int | YES |  | NULL | 工作年限 |
| `current_company` | varchar(128) | YES |  | NULL | 当前就职公司 |
| `current_position` | varchar(64) | YES |  | NULL | 当前职位 |
| `expect_salary` | decimal(12,2) | YES |  | NULL | 期望薪资 |
| `resume_url` | varchar(500) | YES |  | NULL | 简历文件地址 |
| `source` | tinyint | NO |  | 2 | 简历来源:1官网 2招聘网站 3内部推荐 4猎头 5校园招聘 6其他 |
| `referrer_id` | bigint | YES |  | NULL | 内推人员工ID |
| `stage` | tinyint | NO |  | 1 | 所处阶段:1待筛选 2待面试 3面试中 4待录用 5已录用 6已淘汰 |
| `is_talent` | tinyint | NO |  | 0 | 是否纳入人才库:1是 0否 |
| `talent_tags` | varchar(255) | YES |  | NULL | 人才库标签,逗号分隔,如 Java,后端,3年经验 |
| `remark` | varchar(500) | YES |  | NULL | 备注(淘汰原因等) |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `course`

课程表

外键：trainer_id → trainer

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `course_code` | varchar(32) | NO | UK | NULL | 课程编码(唯一) |
| `course_name` | varchar(128) | NO |  | NULL | 课程名称 |
| `course_type` | tinyint | NO |  | 2 | 授课形式:1线上 2线下 |
| `category` | varchar(64) | YES |  | NULL | 课程分类,如 技术类/管理类 |
| `hours` | decimal(5,1) | NO |  | 0.0 | 课程学时 |
| `trainer_id` | bigint | YES |  | NULL | 主讲讲师ID |
| `courseware_url` | varchar(500) | YES |  | NULL | 课件下载地址 |
| `video_url` | varchar(500) | YES |  | NULL | 录播视频地址 |
| `outline` | text | YES |  | NULL | 课程大纲 |
| `max_students` | int | NO |  | 0 | 人数上限(0=不限) |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `dept`

部门表

外键：leader_id → employee；parent_id → dept

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `dept_code` | varchar(32) | NO | UK | NULL | 部门编码(全局唯一),如 D1001 |
| `name` | varchar(64) | NO | UK | NULL | 部门名称 |
| `parent_id` | bigint | YES |  | NULL | 上级部门ID(NULL=顶级部门) |
| `leader` | varchar(32) | YES |  | NULL | 部门负责人姓名(快照,便于快速展示) |
| `leader_id` | bigint | YES |  | NULL | 部门负责人(员工ID,外键后置添加) |
| `dept_type` | tinyint | NO |  | 3 | 组织类型:1公司 2中心/事业部 3部门 4班组 |
| `dept_level` | tinyint | NO |  | 1 | 层级深度,顶级=1 |
| `dept_path` | varchar(255) | NO |  |  | 层级路径,如 /1/5/9/,便于递归查询 |
| `headcount_limit` | int | NO |  | 0 | 编制上限(0=不限) |
| `phone` | varchar(20) | YES |  | NULL | 部门联系电话 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `status` | tinyint | NO |  | 1 | 状态:1正常 0停用 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee`

员工表

外键：dept_id → dept；manager_id → employee；position_id → position

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `emp_no` | varchar(32) | NO | UK | NULL | 员工工号(唯一,不可修改),如 E0001 |
| `name` | varchar(32) | NO |  | NULL | 姓名 |
| `gender` | varchar(8) | YES |  | NULL | 性别:M男 F女 U未知 |
| `age` | int | YES |  | NULL | 年龄(冗余快照,可由birth_date推算) |
| `birth_date` | date | YES |  | NULL | 出生日期 |
| `id_card` | varchar(32) | YES | UK | NULL | 身份证号(加密存储) |
| `nation` | varchar(20) | YES |  | NULL | 民族,如 汉族 |
| `native_place` | varchar(64) | YES |  | NULL | 籍贯 |
| `marital_status` | tinyint | YES |  | NULL | 婚姻状况:1未婚 2已婚 3离异 4丧偶 |
| `political_status` | tinyint | YES |  | NULL | 政治面貌:1群众 2共青团员 3中共党员 4民主党派 |
| `phone` | varchar(20) | YES |  | NULL | 手机号 |
| `email` | varchar(64) | YES |  | NULL | 电子邮箱 |
| `emergency_contact` | varchar(32) | YES |  | NULL | 紧急联系人姓名 |
| `emergency_phone` | varchar(20) | YES |  | NULL | 紧急联系人电话 |
| `address` | varchar(255) | YES |  | NULL | 现居住地址 |
| `education` | tinyint | YES |  | NULL | 最高学历:1博士 2硕士 3本科 4大专 5中专 6高中 7其他 |
| `graduate_school` | varchar(64) | YES |  | NULL | 毕业院校 |
| `major` | varchar(64) | YES |  | NULL | 所学专业 |
| `graduate_date` | date | YES |  | NULL | 毕业日期 |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID |
| `position_id` | bigint | YES |  | NULL | 所属岗位ID |
| `position` | varchar(64) | YES |  | NULL | 职位名称(冗余快照,避免列表频繁联表) |
| `job_level` | varchar(16) | YES |  | NULL | 职级,如 P5/M2 |
| `employee_type` | tinyint | NO |  | 1 | 用工性质:1正式 2试用 3实习 4兼职 5劳务 |
| `hire_date` | date | YES |  | NULL | 入职日期 |
| `regular_date` | date | YES |  | NULL | 转正日期 |
| `work_start_date` | date | YES |  | NULL | 参加工作时间 |
| `leave_date` | date | YES |  | NULL | 离职日期 |
| `leave_reason` | varchar(255) | YES |  | NULL | 离职原因 |
| `status` | tinyint | NO |  | 1 | 在职状态:0离职 1在职 2试用 3待入职 |
| `manager_id` | bigint | YES |  | NULL | 直属主管(员工ID) |
| `salary` | decimal(12,2) | YES |  | NULL | 当前基本薪资(冗余快照,明细见employee_salary) |
| `bank_name` | varchar(64) | YES |  | NULL | 开户银行 |
| `bank_account` | varchar(32) | YES |  | NULL | 银行卡号(加密存储) |
| `avatar` | varchar(255) | YES |  | NULL | 头像地址 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `created_by` | bigint | YES |  | NULL | 创建人用户ID |
| `updated_by` | bigint | YES |  | NULL | 最后修改人用户ID |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee_attachment`

员工附件表

外键：employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `attach_type` | tinyint | NO |  | 7 | 附件类型:1身份证 2学历证书 3学位证书 4离职证明 5体检报告 6合同扫描件 7其他 |
| `file_name` | varchar(255) | NO |  | NULL | 文件名 |
| `file_url` | varchar(500) | NO |  | NULL | 文件访问地址 |
| `file_ext` | varchar(16) | YES |  | NULL | 扩展名,如 pdf/jpg |
| `file_size` | bigint | YES |  | NULL | 文件大小(字节) |
| `uploader_id` | bigint | YES |  | NULL | 上传人用户ID |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 上传时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee_change`

员工异动记录表

外键：after_dept_id → dept；before_dept_id → dept；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `change_type` | tinyint | NO |  | NULL | 异动类型:1入职 2转正 3调岗 4调部门 5晋升 6降级 7离职 8返聘 |
| `before_dept_id` | bigint | YES |  | NULL | 异动前部门ID |
| `after_dept_id` | bigint | YES |  | NULL | 异动后部门ID |
| `before_position` | varchar(64) | YES |  | NULL | 异动前职位 |
| `after_position` | varchar(64) | YES |  | NULL | 异动后职位 |
| `before_salary` | decimal(12,2) | YES |  | NULL | 异动前薪资 |
| `after_salary` | decimal(12,2) | YES |  | NULL | 异动后薪资 |
| `effect_date` | date | NO |  | NULL | 生效日期 |
| `reason` | varchar(500) | YES |  | NULL | 异动原因 |
| `apply_no` | varchar(64) | YES |  | NULL | 关联申请单号 |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0待提交 1审批中 2已通过 3已驳回 |
| `operator_id` | bigint | YES |  | NULL | 经办人(员工ID) |
| `attachment_url` | varchar(255) | YES |  | NULL | 异动附件(调令等) |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee_contract`

劳动合同表

外键：employee_id → employee；renew_from_id → employee_contract

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `contract_no` | varchar(64) | NO | UK | NULL | 合同编号(唯一) |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `contract_type` | tinyint | NO |  | 1 | 合同类型:1固定期限 2无固定期限 3以完成任务为期限 4实习协议 5劳务协议 |
| `first_party` | varchar(128) | YES |  | NULL | 甲方(公司名称) |
| `second_party` | varchar(32) | YES |  | NULL | 乙方(员工姓名) |
| `sign_date` | date | YES |  | NULL | 签订日期 |
| `start_date` | date | NO |  | NULL | 合同生效日期 |
| `end_date` | date | YES |  | NULL | 合同到期日期(NULL=无固定期限) |
| `term_years` | decimal(4,1) | YES |  | NULL | 合同期限(年) |
| `salary_agreed` | decimal(12,2) | YES |  | NULL | 合同约定薪资 |
| `probation_months` | int | NO |  | 0 | 试用期月数 |
| `renew_from_id` | bigint | YES |  | NULL | 续签来源合同ID(上一份合同) |
| `status` | tinyint | NO |  | 1 | 合同状态:1生效中 2已到期 3已续签 4已解除 5已终止 |
| `attachment_url` | varchar(255) | YES |  | NULL | 合同扫描件地址 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee_salary`

员工薪资标准表

外键：employee_id → employee；standard_id → salary_standard

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `standard_id` | bigint | YES |  | NULL | 套用的薪资标准模板ID |
| `base_salary` | decimal(12,2) | NO |  | 0.00 | 基本工资 |
| `post_salary` | decimal(12,2) | NO |  | 0.00 | 岗位工资 |
| `performance_salary` | decimal(12,2) | NO |  | 0.00 | 绩效工资基数 |
| `allowance` | decimal(12,2) | NO |  | 0.00 | 固定补贴合计 |
| `social_base` | decimal(12,2) | YES |  | NULL | 社保缴费基数 |
| `fund_base` | decimal(12,2) | YES |  | NULL | 公积金缴费基数 |
| `fund_rate` | decimal(5,2) | YES |  | NULL | 公积金缴存比例(%) |
| `adjust_reason` | varchar(255) | YES |  | NULL | 调薪原因 |
| `effective_date` | date | NO |  | NULL | 生效日期 |
| `is_current` | tinyint | NO |  | 1 | 是否当前生效:1是 0否(历史记录) |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0待审 1已生效 2已驳回 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `created_by` | bigint | YES |  | NULL | 操作人用户ID |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `employee_schedule`

员工排班表

外键：employee_id → employee；shift_id → shift

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `shift_id` | bigint | NO |  | NULL | 班次ID |
| `schedule_date` | date | NO |  | NULL | 排班日期 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `insurance_config`

社保公积金配置表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `config_name` | varchar(64) | NO |  | NULL | 配置名称,如 2026年北京社保-养老 |
| `city` | varchar(32) | NO |  | 北京 | 参保城市 |
| `social_type` | tinyint | NO |  | NULL | 险种类型:1养老 2医疗 3失业 4工伤 5生育 6住房公积金 |
| `company_rate` | decimal(6,4) | NO |  | 0.0000 | 企业缴纳比例(如0.1600=16%) |
| `personal_rate` | decimal(6,4) | NO |  | 0.0000 | 个人缴纳比例(如0.0800=8%) |
| `base_min` | decimal(12,2) | NO |  | 0.00 | 缴费基数下限 |
| `base_max` | decimal(12,2) | NO |  | 0.00 | 缴费基数上限 |
| `effective_date` | date | YES |  | NULL | 生效日期 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `insurance_record`

社保缴纳记录表

外键：config_id → insurance_config；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `config_id` | bigint | YES |  | NULL | 关联社保配置ID |
| `record_year` | smallint | NO |  | NULL | 缴纳年份 |
| `record_month` | tinyint | NO |  | NULL | 缴纳月份:1-12 |
| `social_base` | decimal(12,2) | NO |  | 0.00 | 社保缴费基数 |
| `fund_base` | decimal(12,2) | NO |  | 0.00 | 公积金缴费基数 |
| `pension_personal` | decimal(12,2) | NO |  | 0.00 | 养老保险-个人 |
| `pension_company` | decimal(12,2) | NO |  | 0.00 | 养老保险-企业 |
| `medical_personal` | decimal(12,2) | NO |  | 0.00 | 医疗保险-个人 |
| `medical_company` | decimal(12,2) | NO |  | 0.00 | 医疗保险-企业 |
| `unemploy_personal` | decimal(12,2) | NO |  | 0.00 | 失业保险-个人 |
| `unemploy_company` | decimal(12,2) | NO |  | 0.00 | 失业保险-企业 |
| `injury_company` | decimal(12,2) | NO |  | 0.00 | 工伤保险-企业(个人不缴) |
| `maternity_company` | decimal(12,2) | NO |  | 0.00 | 生育保险-企业(个人不缴) |
| `fund_personal` | decimal(12,2) | NO |  | 0.00 | 住房公积金-个人 |
| `fund_company` | decimal(12,2) | NO |  | 0.00 | 住房公积金-企业 |
| `total_personal` | decimal(12,2) | NO |  | 0.00 | 个人缴纳合计(含公积金) |
| `total_company` | decimal(12,2) | NO |  | 0.00 | 企业缴纳合计(含公积金) |
| `pay_status` | tinyint | NO |  | 0 | 缴纳状态:0未缴 1已缴 2已减员 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `interview_record`

面试记录表

外键：candidate_id → candidate；interviewer_id → employee；recruit_request_id → recruit_request

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `candidate_id` | bigint | NO |  | NULL | 候选人ID |
| `recruit_request_id` | bigint | NO |  | NULL | 关联招聘需求ID |
| `round_no` | int | NO |  | 1 | 面试轮次,从1开始 |
| `interview_type` | tinyint | NO |  | 1 | 面试类型:1初试 2复试 3终试 4HR面谈 |
| `interview_time` | datetime | YES |  | NULL | 面试时间 |
| `interview_place` | varchar(128) | YES |  | NULL | 面试地点/会议链接 |
| `interviewer_id` | bigint | YES |  | NULL | 面试官员工ID |
| `score` | int | YES |  | NULL | 面试评分(0-100) |
| `evaluation` | varchar(1000) | YES |  | NULL | 面试评价 |
| `conclusion` | tinyint | YES |  | NULL | 面试结论:1通过 2待定 3淘汰 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `job_posting`

职位发布表

外键：recruit_request_id → recruit_request

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `recruit_request_id` | bigint | NO |  | NULL | 关联招聘需求ID |
| `channel` | tinyint | NO |  | 1 | 发布渠道:1官网 2智联招聘 3前程无忧 4BOSS直聘 5猎聘 6内部推荐 7其他 |
| `posting_title` | varchar(128) | NO |  | NULL | 发布职位名称 |
| `posting_url` | varchar(500) | YES |  | NULL | 发布链接 |
| `publish_time` | datetime | YES |  | NULL | 发布时间 |
| `close_time` | datetime | YES |  | NULL | 关闭时间 |
| `view_count` | int | NO |  | 0 | 浏览量 |
| `apply_count` | int | NO |  | 0 | 投递量 |
| `status` | tinyint | NO |  | 1 | 状态:1发布中 2已关闭 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `leave_balance`

假期余额表

外键：employee_id → employee；leave_type_id → leave_type

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `leave_type_id` | bigint | NO |  | NULL | 假别ID |
| `balance_year` | smallint | NO |  | NULL | 所属年度,如 2026 |
| `total_days` | decimal(6,1) | NO |  | 0.0 | 年度总额度(天) |
| `carry_over_days` | decimal(6,1) | NO |  | 0.0 | 上年结转天数 |
| `used_days` | decimal(6,1) | NO |  | 0.0 | 已使用天数 |
| `remain_days` | decimal(6,1) | NO |  | 0.0 | 剩余可用天数 |
| `expire_date` | date | YES |  | NULL | 额度失效日期 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `leave_request`

请假申请表

外键：current_approver_id → employee；employee_id → employee；leave_type_id → leave_type

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `request_no` | varchar(64) | NO | UK | NULL | 申请单号(唯一),如 LV20260901001 |
| `employee_id` | bigint | NO |  | NULL | 申请人员工ID |
| `leave_type_id` | bigint | NO |  | NULL | 假别ID |
| `start_time` | datetime | NO |  | NULL | 请假开始时间 |
| `end_time` | datetime | NO |  | NULL | 请假结束时间 |
| `days` | decimal(6,1) | NO |  | NULL | 请假天数(支持0.5天) |
| `reason` | varchar(500) | YES |  | NULL | 请假事由 |
| `attachment_url` | varchar(500) | YES |  | NULL | 证明材料地址(病假条等) |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销 |
| `current_approver_id` | bigint | YES |  | NULL | 当前审批人员工ID |
| `apply_time` | datetime | NO |  | CURRENT_TIMESTAMP | 申请时间 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `leave_type`

假别表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `type_code` | varchar(32) | NO | UK | NULL | 假别编码(唯一),如 ANNUAL/SICK |
| `type_name` | varchar(32) | NO |  | NULL | 假别名称,如 年假 |
| `is_paid` | tinyint | NO |  | 1 | 是否带薪:1带薪 0不带薪 |
| `unit` | tinyint | NO |  | 1 | 计量单位:1天 2小时 |
| `min_unit` | decimal(4,1) | NO |  | 0.5 | 最小请假单位(天),如 0.5 表示半天 |
| `max_days_per_year` | decimal(6,1) | NO |  | 0.0 | 每年额度上限(0=不限) |
| `valid_months` | int | NO |  | 12 | 额度有效月数 |
| `can_carry_over` | tinyint | NO |  | 0 | 是否可结转下年:1是 0否 |
| `need_proof` | tinyint | NO |  | 0 | 是否需上传证明:1是 0否 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `notice`

通知公告表

外键：publisher_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `title` | varchar(200) | NO |  | NULL | 公告标题 |
| `content` | text | YES |  | NULL | 公告正文 |
| `notice_type` | tinyint | NO |  | 3 | 公告类型:1公司制度 2放假通知 3人事公告 4活动通知 5其他 |
| `publisher_id` | bigint | YES |  | NULL | 发布人员工ID |
| `publish_time` | datetime | YES |  | NULL | 发布时间(支持定时发布) |
| `expire_time` | datetime | YES |  | NULL | 失效时间(到期自动下架) |
| `target_scope` | tinyint | NO |  | 1 | 发布范围:1全体 2指定部门 |
| `target_dept_ids` | varchar(500) | YES |  | NULL | 指定部门ID列表,逗号分隔 |
| `is_top` | tinyint | NO |  | 0 | 是否置顶:1是 0否 |
| `read_count` | int | NO |  | 0 | 已读人数 |
| `status` | tinyint | NO |  | 1 | 状态:1草稿 2已发布 3已下架 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `notice_read`

公告已读记录表

外键：employee_id → employee；notice_id → notice

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `notice_id` | bigint | NO |  | NULL | 公告ID |
| `employee_id` | bigint | NO |  | NULL | 阅读人员工ID |
| `read_time` | datetime | NO |  | CURRENT_TIMESTAMP | 阅读时间 |

## `offer_record`

录用通知表

外键：candidate_id → candidate；dept_id → dept；position_id → position；recruit_request_id → recruit_request

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `offer_no` | varchar(64) | NO | UK | NULL | Offer编号(唯一) |
| `candidate_id` | bigint | NO |  | NULL | 候选人ID |
| `recruit_request_id` | bigint | NO |  | NULL | 关联招聘需求ID |
| `dept_id` | bigint | NO |  | NULL | 录用部门ID |
| `position_id` | bigint | YES |  | NULL | 录用岗位ID |
| `job_level` | varchar(16) | YES |  | NULL | 定级 |
| `offer_salary` | decimal(12,2) | NO |  | 0.00 | 转正后月薪 |
| `probation_months` | int | NO |  | 3 | 试用期月数 |
| `probation_salary` | decimal(12,2) | YES |  | NULL | 试用期月薪(通常为转正的80%) |
| `entry_date` | date | YES |  | NULL | 预计入职日期 |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0草稿 1审批中 2已通过 3已驳回 |
| `current_approver_id` | bigint | YES |  | NULL | 当前审批人员工ID |
| `send_time` | datetime | YES |  | NULL | Offer发送时间 |
| `confirm_time` | datetime | YES |  | NULL | 候选人确认时间 |
| `offer_url` | varchar(500) | YES |  | NULL | Offer文件地址 |
| `status` | tinyint | NO |  | 1 | 状态:1待发送 2已发送 3已接受 4已拒绝 5已过期 6已入职 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `overtime_request`

加班申请表

外键：current_approver_id → employee；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `request_no` | varchar(64) | NO | UK | NULL | 申请单号(唯一),如 OT20260901001 |
| `employee_id` | bigint | NO |  | NULL | 申请人员工ID |
| `overtime_date` | date | NO |  | NULL | 加班日期 |
| `start_time` | time | NO |  | NULL | 加班开始时间 |
| `end_time` | time | NO |  | NULL | 加班结束时间 |
| `hours` | decimal(5,1) | NO |  | NULL | 加班时长(小时) |
| `overtime_type` | tinyint | NO |  | 1 | 加班类型:1工作日 2休息日 3法定节假日 |
| `compensate_type` | tinyint | NO |  | 1 | 补偿方式:1调休 2计薪 |
| `reason` | varchar(500) | YES |  | NULL | 加班事由 |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0草稿 1审批中 2已通过 3已驳回 4已撤销 |
| `current_approver_id` | bigint | YES |  | NULL | 当前审批人员工ID |
| `apply_time` | datetime | NO |  | CURRENT_TIMESTAMP | 申请时间 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `performance_indicator`

考核指标表

外键：employee_id → employee；plan_id → performance_plan

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `plan_id` | bigint | NO |  | NULL | 考核方案ID |
| `employee_id` | bigint | NO |  | NULL | 被考核员工ID |
| `indicator_name` | varchar(128) | NO |  | NULL | 指标名称,如 需求交付及时率 |
| `indicator_type` | tinyint | NO |  | 1 | 指标类型:1KPI 2OKR 3能力 4态度 |
| `weight` | decimal(5,2) | NO |  | 0.00 | 指标权重(%),同一员工合计应为100 |
| `target_value` | varchar(255) | YES |  | NULL | 目标值,如 95% |
| `actual_value` | varchar(255) | YES |  | NULL | 实际完成值 |
| `score_standard` | varchar(500) | YES |  | NULL | 评分标准说明 |
| `self_score` | decimal(5,2) | YES |  | NULL | 自评得分(0-100) |
| `leader_score` | decimal(5,2) | YES |  | NULL | 主管评分(0-100) |
| `final_score` | decimal(5,2) | YES |  | NULL | 最终得分(按权重汇总) |
| `source` | tinyint | NO |  | 1 | 指标来源:1主管下达 2员工自拟 3系统模板 |
| `confirm_status` | tinyint | NO |  | 0 | 员工确认状态:0待确认 1已确认 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `performance_interview`

绩效面谈记录表

外键：employee_id → employee；interviewer_id → employee；record_id → performance_record

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `record_id` | bigint | NO |  | NULL | 绩效考核记录ID |
| `employee_id` | bigint | NO |  | NULL | 被面谈员工ID |
| `interviewer_id` | bigint | NO |  | NULL | 面谈人(主管)员工ID |
| `interview_time` | datetime | YES |  | NULL | 面谈时间 |
| `interview_place` | varchar(128) | YES |  | NULL | 面谈地点 |
| `achievement` | varchar(1000) | YES |  | NULL | 本期成绩与亮点 |
| `shortcoming` | varchar(1000) | YES |  | NULL | 不足与改进方向 |
| `next_goal` | varchar(1000) | YES |  | NULL | 下期目标 |
| `employee_feedback` | varchar(1000) | YES |  | NULL | 员工反馈与诉求 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `performance_plan`

考核方案表

外键：creator_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `plan_code` | varchar(32) | NO | UK | NULL | 方案编号(唯一) |
| `plan_name` | varchar(128) | NO |  | NULL | 方案名称,如 2026年Q1绩效考核 |
| `period_type` | tinyint | NO |  | 2 | 考核周期:1月度 2季度 3半年度 4年度 |
| `plan_year` | smallint | NO |  | NULL | 考核年度 |
| `plan_quarter` | tinyint | YES |  | NULL | 考核季度:1-4(周期为季度时有效) |
| `plan_month` | tinyint | YES |  | NULL | 考核月份:1-12(周期为月度时有效) |
| `start_date` | date | YES |  | NULL | 考核开始日期 |
| `end_date` | date | YES |  | NULL | 考核截止日期 |
| `scope_type` | tinyint | NO |  | 1 | 考核范围:1全体 2指定部门 3指定岗位 4指定人员 |
| `scope_dept_ids` | varchar(500) | YES |  | NULL | 指定部门ID列表,逗号分隔 |
| `grade_rule` | varchar(255) | YES |  | NULL | 等级规则,如 S:90+,A:80-89,B:70-79,C:60-69,D:<60 |
| `force_distribution` | tinyint | NO |  | 0 | 是否强制分布:1是 0否 |
| `distribution_rule` | varchar(255) | YES |  | NULL | 等级分布比例,如 S:10,A:20,B:50,C:15,D:5 |
| `self_weight` | decimal(5,2) | NO |  | 20.00 | 员工自评权重(%) |
| `leader_weight` | decimal(5,2) | NO |  | 70.00 | 主管评分权重(%) |
| `review_weight` | decimal(5,2) | NO |  | 10.00 | 隔级复核权重(%) |
| `creator_id` | bigint | YES |  | NULL | 创建人员工ID |
| `description` | varchar(1000) | YES |  | NULL | 方案说明 |
| `status` | tinyint | NO |  | 1 | 状态:1草稿 2已发布 3评分中 4已结束 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `performance_record`

绩效考核记录表

外键：dept_id → dept；employee_id → employee；leader_id → employee；plan_id → performance_plan

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `plan_id` | bigint | NO |  | NULL | 考核方案ID |
| `employee_id` | bigint | NO |  | NULL | 被考核员工ID |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID(冗余,便于部门统计) |
| `leader_id` | bigint | YES |  | NULL | 考核主管员工ID |
| `self_score` | decimal(5,2) | YES |  | NULL | 自评加权总分 |
| `leader_score` | decimal(5,2) | YES |  | NULL | 主管评分加权总分 |
| `review_score` | decimal(5,2) | YES |  | NULL | 隔级复核分 |
| `final_score` | decimal(5,2) | YES |  | NULL | 最终得分(按方案权重计算) |
| `grade` | varchar(8) | YES |  | NULL | 绩效等级:S/A/B/C/D |
| `dept_rank` | int | YES |  | NULL | 部门内排名 |
| `employee_confirm_status` | tinyint | NO |  | 0 | 员工确认状态:0待确认 1已确认 2已申诉 |
| `appeal_reason` | varchar(500) | YES |  | NULL | 申诉理由 |
| `appeal_result` | varchar(500) | YES |  | NULL | 申诉处理结果 |
| `archive_status` | tinyint | NO |  | 0 | 归档状态:0未归档 1已归档(锁定) |
| `archive_time` | datetime | YES |  | NULL | 归档时间 |
| `status` | tinyint | NO |  | 1 | 流程状态:1待自评 2待主管评 3待复核 4待员工确认 5已完成 |
| `remark` | varchar(500) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `position`

岗位表

外键：dept_id → dept

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `position_code` | varchar(32) | NO | UK | NULL | 岗位编码(全局唯一),如 P-TECH-001 |
| `position_name` | varchar(64) | NO |  | NULL | 岗位名称,如 Java开发工程师 |
| `position_seq` | tinyint | NO |  | 2 | 岗位序列:1管理 2技术 3职能 4操作 |
| `job_level` | varchar(16) | YES |  | NULL | 职级范围,如 P4-P7 |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID(NULL=公司通用岗位) |
| `duty` | varchar(500) | YES |  | NULL | 岗位职责 |
| `requirement` | varchar(500) | YES |  | NULL | 任职要求 |
| `headcount_limit` | int | NO |  | 0 | 岗位编制上限(0=不限) |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `recruit_request`

招聘需求表

外键：applicant_id → employee；current_approver_id → employee；request_dept_id → dept；position_id → position

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `request_no` | varchar(64) | NO | UK | NULL | 需求单号(唯一),如 RC20260901001 |
| `request_dept_id` | bigint | NO |  | NULL | 需求部门ID |
| `position_id` | bigint | YES |  | NULL | 招聘岗位ID |
| `position_name` | varchar(64) | YES |  | NULL | 岗位名称(快照) |
| `recruit_count` | int | NO |  | 1 | 招聘人数 |
| `hired_count` | int | NO |  | 0 | 已录用人数 |
| `job_level` | varchar(16) | YES |  | NULL | 职级 |
| `salary_min` | decimal(12,2) | YES |  | NULL | 薪资区间下限 |
| `salary_max` | decimal(12,2) | YES |  | NULL | 薪资区间上限 |
| `education_req` | tinyint | YES |  | NULL | 学历要求:1博士 2硕士 3本科 4大专 5中专 6高中 7不限 |
| `experience_years` | int | YES |  | NULL | 经验要求(年) |
| `job_description` | text | YES |  | NULL | 岗位职责 |
| `job_requirement` | text | YES |  | NULL | 任职要求 |
| `hope_arrive_date` | date | YES |  | NULL | 期望到岗日期 |
| `urgency` | tinyint | NO |  | 1 | 紧急程度:1普通 2紧急 3非常紧急 |
| `applicant_id` | bigint | YES |  | NULL | 提报人员工ID |
| `apply_time` | datetime | NO |  | CURRENT_TIMESTAMP | 提报时间 |
| `approval_status` | tinyint | NO |  | 1 | 审批状态:0草稿 1审批中 2已通过 3已驳回 |
| `current_approver_id` | bigint | YES |  | NULL | 当前审批人员工ID |
| `status` | tinyint | NO |  | 1 | 需求状态:1进行中 2已完成 3已取消 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `salary_item`

薪资明细项表

外键：salary_record_id → salary_record

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `salary_record_id` | bigint | NO |  | NULL | 月度薪资记录ID |
| `item_type` | tinyint | NO |  | 1 | 项目类型:1应发项 2扣款项 3企业承担项 |
| `item_code` | varchar(32) | NO |  | NULL | 项目编码,如 BASE/POST/OT/SOCIAL |
| `item_name` | varchar(64) | NO |  | NULL | 项目名称,如 基本工资 |
| `item_amount` | decimal(12,2) | NO |  | 0.00 | 项目金额 |
| `sort_no` | int | NO |  | 0 | 展示排序,升序 |
| `remark` | varchar(255) | YES |  | NULL | 备注(计算口径说明) |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |

## `salary_record`

月度薪资记录表

外键：dept_id → dept；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID(冗余,便于按部门统计人力成本) |
| `salary_year` | smallint | NO |  | NULL | 薪资年份,如 2026 |
| `salary_month` | tinyint | NO |  | NULL | 薪资月份:1-12 |
| `base_salary` | decimal(12,2) | NO |  | 0.00 | 基本工资 |
| `post_salary` | decimal(12,2) | NO |  | 0.00 | 岗位工资 |
| `performance_salary` | decimal(12,2) | NO |  | 0.00 | 绩效工资(按考核结果浮动) |
| `allowance` | decimal(12,2) | NO |  | 0.00 | 补贴合计 |
| `overtime_pay` | decimal(12,2) | NO |  | 0.00 | 加班费 |
| `other_add` | decimal(12,2) | NO |  | 0.00 | 其他加项(补发等) |
| `attendance_deduct` | decimal(12,2) | NO |  | 0.00 | 考勤扣款(迟到/早退/旷工) |
| `other_deduct` | decimal(12,2) | NO |  | 0.00 | 其他扣项 |
| `gross_pay` | decimal(12,2) | NO |  | 0.00 | 应发合计 |
| `social_personal` | decimal(12,2) | NO |  | 0.00 | 社保个人缴纳 |
| `social_company` | decimal(12,2) | NO |  | 0.00 | 社保企业缴纳 |
| `fund_personal` | decimal(12,2) | NO |  | 0.00 | 公积金个人缴纳 |
| `fund_company` | decimal(12,2) | NO |  | 0.00 | 公积金企业缴纳 |
| `tax_amount` | decimal(12,2) | NO |  | 0.00 | 个人所得税 |
| `net_pay` | decimal(12,2) | NO |  | 0.00 | 实发合计 |
| `pay_status` | tinyint | NO |  | 0 | 发放状态:0待核算 1已核算 2已复核 3已发放 4发放失败 |
| `pay_date` | date | YES |  | NULL | 实际发放日期 |
| `pay_batch_no` | varchar(64) | YES |  | NULL | 发放批次号 |
| `lock_status` | tinyint | NO |  | 0 | 锁定状态:0未锁定 1已锁定(禁改) |
| `calc_by` | bigint | YES |  | NULL | 核算人用户ID(须与复核人不同,BR-05-03) |
| `audit_by` | bigint | YES |  | NULL | 复核人用户ID(不得与核算人相同) |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `salary_standard`

薪资标准模板表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `standard_code` | varchar(32) | NO | UK | NULL | 标准编码(唯一),如 STD-TECH-P5 |
| `standard_name` | varchar(64) | NO |  | NULL | 标准名称,如 P5技术岗标准 |
| `position_seq` | tinyint | YES |  | NULL | 适用岗位序列:1管理 2技术 3职能 4操作 |
| `job_level` | varchar(16) | YES |  | NULL | 适用职级,如 P5/M2 |
| `base_salary` | decimal(12,2) | NO |  | 0.00 | 基本工资 |
| `post_salary` | decimal(12,2) | NO |  | 0.00 | 岗位工资 |
| `performance_salary` | decimal(12,2) | NO |  | 0.00 | 绩效工资(基数) |
| `allowance` | decimal(12,2) | NO |  | 0.00 | 固定补贴合计 |
| `effective_date` | date | YES |  | NULL | 生效日期 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `shift`

班次定义表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `shift_code` | varchar(32) | NO | UK | NULL | 班次编码(唯一),如 STD/DAY/NIGHT |
| `shift_name` | varchar(64) | NO |  | NULL | 班次名称,如 标准班 |
| `work_start` | time | NO |  | 09:00:00 | 上班时间 |
| `work_end` | time | NO |  | 18:00:00 | 下班时间 |
| `rest_minutes` | int | NO |  | 60 | 休息时长(分钟) |
| `standard_hours` | decimal(4,1) | NO |  | 8.0 | 标准工时(小时) |
| `late_tolerance` | int | NO |  | 5 | 迟到容忍(分钟),超出计迟到 |
| `early_tolerance` | int | NO |  | 5 | 早退容忍(分钟) |
| `flex_start` | int | NO |  | 0 | 弹性上班提前分钟数 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_config`

系统参数表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `config_key` | varchar(100) | NO | UK | NULL | 参数键(唯一),如 attendance.lock.day |
| `config_name` | varchar(100) | NO |  | NULL | 参数名称,如 考勤封账日 |
| `config_value` | varchar(500) | NO |  | NULL | 参数值 |
| `config_group` | varchar(50) | NO |  | system | 参数分组:attendance考勤/salary薪资/recruit招聘/system系统 |
| `value_type` | tinyint | NO |  | 1 | 值类型:1字符串 2数字 3布尔 4JSON |
| `is_system` | tinyint | NO |  | 0 | 是否系统内置:1是(禁止删除) 0否 |
| `remark` | varchar(255) | YES |  | NULL | 备注说明 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_dict_item`

字典项表

外键：dict_type_id → sys_dict_type

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `dict_type_id` | bigint | NO |  | NULL | 所属字典类型ID(sys_dict_type.id) |
| `item_value` | varchar(64) | NO |  | NULL | 字典项值(存库值),如 3 |
| `item_label` | varchar(64) | NO |  | NULL | 字典项显示文本,如 本科 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `is_default` | tinyint | NO |  | 0 | 是否默认选中:1是 0否 |
| `css_class` | varchar(64) | YES |  | NULL | 前端样式类(标签颜色等) |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_dict_type`

字典类型表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `dict_type` | varchar(64) | NO | UK | NULL | 字典类型编码(唯一),如 education/gender/leave_type |
| `dict_name` | varchar(64) | NO |  | NULL | 字典类型名称,如 学历 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_login_log`

登录日志表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `user_id` | bigint | YES |  | NULL | 用户ID(登录失败时可能为空,故不设外键) |
| `username` | varchar(64) | NO |  | NULL | 登录账号 |
| `login_ip` | varchar(64) | YES |  | NULL | 登录IP |
| `login_location` | varchar(100) | YES |  | NULL | 登录地点 |
| `browser` | varchar(100) | YES |  | NULL | 浏览器 |
| `os` | varchar(100) | YES |  | NULL | 操作系统 |
| `login_status` | tinyint | NO |  | 1 | 登录状态:1成功 0失败 |
| `message` | varchar(255) | YES |  | NULL | 提示信息 |
| `login_time` | datetime | NO |  | CURRENT_TIMESTAMP | 登录时间 |

## `sys_menu`

菜单权限表

外键：parent_id → sys_menu

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `parent_id` | bigint | YES |  | NULL | 父菜单ID(NULL=顶级菜单) |
| `menu_name` | varchar(64) | NO |  | NULL | 菜单名称 |
| `menu_type` | tinyint | NO |  | 2 | 菜单类型:1目录 2菜单 3按钮 |
| `path` | varchar(200) | YES |  | NULL | 前端路由地址,如 /employee |
| `component` | varchar(200) | YES |  | NULL | 前端组件路径 |
| `perms` | varchar(100) | YES | UK | NULL | 权限标识,如 employee:list |
| `icon` | varchar(100) | YES |  | NULL | 菜单图标 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `visible` | tinyint | NO |  | 1 | 是否显示:1显示 0隐藏 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_oper_log`

操作日志表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `user_id` | bigint | YES |  | NULL | 操作人用户ID |
| `username` | varchar(64) | YES |  | NULL | 操作人账号 |
| `module` | varchar(64) | YES |  | NULL | 所属模块,如 员工信息 |
| `oper_type` | tinyint | NO |  | 7 | 操作类型:1新增 2修改 3删除 4查询 5导出 6导入 7其他 |
| `oper_desc` | varchar(255) | YES |  | NULL | 操作描述 |
| `request_method` | varchar(10) | YES |  | NULL | HTTP方法:GET/POST/PUT/DELETE |
| `request_url` | varchar(255) | YES |  | NULL | 请求地址 |
| `request_param` | text | YES |  | NULL | 请求参数(JSON) |
| `response_result` | text | YES |  | NULL | 响应结果(截断存储) |
| `oper_ip` | varchar(64) | YES |  | NULL | 操作IP |
| `oper_status` | tinyint | NO |  | 1 | 操作状态:1成功 0失败 |
| `error_msg` | varchar(1000) | YES |  | NULL | 异常信息 |
| `cost_time` | bigint | YES |  | NULL | 耗时(毫秒) |
| `oper_time` | datetime | NO |  | CURRENT_TIMESTAMP | 操作时间 |

## `sys_role`

角色表

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `role_code` | varchar(64) | NO | UK | NULL | 角色编码(唯一),如 ROLE_HR |
| `role_name` | varchar(64) | NO |  | NULL | 角色名称,如 人事专员 |
| `description` | varchar(255) | YES |  | NULL | 角色职责描述 |
| `data_scope` | tinyint | NO |  | 4 | 数据范围:1全部 2本部门 3本部门及下级 4仅本人 5自定义 |
| `data_scope_depts` | varchar(500) | YES |  | NULL | 自定义数据范围部门ID列表,逗号分隔 |
| `role_type` | tinyint | NO |  | 2 | 角色类型:1系统内置(不可删除) 2自定义 |
| `sort_no` | int | NO |  | 0 | 显示排序,升序 |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_role_menu`

角色菜单关联表

外键：menu_id → sys_menu；role_id → sys_role

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `role_id` | bigint | NO |  | NULL | 角色ID |
| `menu_id` | bigint | NO |  | NULL | 菜单ID |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 授权时间 |

## `sys_user`

系统用户表

外键：dept_id → dept；employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `username` | varchar(64) | NO | UK | NULL | 登录账号(唯一) |
| `password` | varchar(100) | NO |  | NULL | 密码(BCrypt加盐哈希,禁止明文) |
| `real_name` | varchar(32) | YES |  | NULL | 姓名 |
| `employee_id` | bigint | YES |  | NULL | 关联员工ID(NULL=非员工账号,如外包运维) |
| `dept_id` | bigint | YES |  | NULL | 所属部门ID(冗余,便于数据范围过滤) |
| `phone` | varchar(20) | YES |  | NULL | 手机号 |
| `email` | varchar(64) | YES |  | NULL | 邮箱 |
| `avatar` | varchar(255) | YES |  | NULL | 头像地址 |
| `status` | tinyint | NO |  | 1 | 账号状态:1正常 0停用 2锁定 |
| `login_fail_count` | int | NO |  | 0 | 连续登录失败次数 |
| `lock_until` | datetime | YES |  | NULL | 锁定到期时间 |
| `last_login_time` | datetime | YES |  | NULL | 最后登录时间 |
| `last_login_ip` | varchar(64) | YES |  | NULL | 最后登录IP |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `sys_user_role`

用户角色关联表

外键：role_id → sys_role；user_id → sys_user

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `user_id` | bigint | NO |  | NULL | 用户ID |
| `role_id` | bigint | NO |  | NULL | 角色ID |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 授权时间 |

## `todo_task`

待办任务表

外键：receiver_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `receiver_id` | bigint | NO |  | NULL | 待办接收人员工ID |
| `task_type` | tinyint | NO |  | 1 | 待办类型:1待审批 2合同到期 3转正提醒 4绩效评分 5培训报名 6其他 |
| `task_title` | varchar(200) | NO |  | NULL | 待办标题 |
| `task_content` | varchar(500) | YES |  | NULL | 待办详情 |
| `biz_type` | tinyint | YES |  | NULL | 业务类型(同approval_record.biz_type) |
| `biz_id` | bigint | YES |  | NULL | 业务单据ID(多态,不设外键) |
| `biz_no` | varchar(64) | YES |  | NULL | 业务单号 |
| `priority` | tinyint | NO |  | 2 | 优先级:1低 2中 3高 |
| `deadline` | datetime | YES |  | NULL | 处理截止时间 |
| `handle_status` | tinyint | NO |  | 0 | 处理状态:0未处理 1已处理 2已忽略 |
| `handle_time` | datetime | YES |  | NULL | 处理时间 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `trainer`

讲师表

外键：employee_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `trainer_code` | varchar(32) | NO | UK | NULL | 讲师编号(唯一) |
| `trainer_name` | varchar(32) | NO |  | NULL | 讲师姓名 |
| `trainer_type` | tinyint | NO |  | 1 | 讲师类型:1内部讲师 2外部讲师 |
| `employee_id` | bigint | YES |  | NULL | 内部讲师关联员工ID(外部讲师为空) |
| `expertise` | varchar(255) | YES |  | NULL | 擅长领域,如 Java架构/项目管理 |
| `trainer_level` | tinyint | NO |  | 1 | 讲师等级:1初级 2中级 3高级 4资深 |
| `phone` | varchar(20) | YES |  | NULL | 联系电话 |
| `email` | varchar(64) | YES |  | NULL | 邮箱 |
| `org_name` | varchar(128) | YES |  | NULL | 所属机构(外部讲师) |
| `hour_rate` | decimal(10,2) | NO |  | 0.00 | 课时费(元/小时) |
| `teach_count` | int | NO |  | 0 | 累计授课次数 |
| `avg_score` | decimal(4,1) | YES |  | NULL | 平均评分(5分制) |
| `status` | tinyint | NO |  | 1 | 状态:1启用 0停用 |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `training_enroll`

培训报名表

外键：course_id → course；employee_id → employee；plan_id → training_plan

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `plan_id` | bigint | NO |  | NULL | 培训计划ID |
| `course_id` | bigint | NO |  | NULL | 课程ID |
| `employee_id` | bigint | NO |  | NULL | 报名员工ID |
| `enroll_time` | datetime | NO |  | CURRENT_TIMESTAMP | 报名时间 |
| `sign_in_status` | tinyint | NO |  | 0 | 签到状态:0未签到 1已签到 2缺席 |
| `sign_in_time` | datetime | YES |  | NULL | 签到时间 |
| `exam_score` | decimal(5,1) | YES |  | NULL | 考核成绩(0-100) |
| `pass_status` | tinyint | NO |  | 0 | 考核结果:0未考核 1合格 2不合格 |
| `satisfaction_score` | tinyint | YES |  | NULL | 满意度评分(1-5星) |
| `remark` | varchar(255) | YES |  | NULL | 备注 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `training_plan`

培训计划表

外键：organizer_id → employee

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `plan_code` | varchar(32) | NO | UK | NULL | 计划编号(唯一) |
| `plan_name` | varchar(128) | NO |  | NULL | 计划名称,如 2026年Q1新员工培训 |
| `plan_year` | smallint | NO |  | NULL | 所属年度 |
| `plan_quarter` | tinyint | NO |  | 0 | 所属季度:0全年 1-4对应Q1-Q4 |
| `train_type` | tinyint | NO |  | 2 | 培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训 |
| `target_scope` | tinyint | NO |  | 1 | 培训对象范围:1全体 2指定部门 3指定岗位 4指定人员 |
| `target_dept_ids` | varchar(500) | YES |  | NULL | 指定部门ID列表,逗号分隔 |
| `budget_amount` | decimal(12,2) | NO |  | 0.00 | 预算金额 |
| `actual_amount` | decimal(12,2) | NO |  | 0.00 | 实际支出 |
| `start_date` | date | YES |  | NULL | 开始日期 |
| `end_date` | date | YES |  | NULL | 结束日期 |
| `organizer_id` | bigint | YES |  | NULL | 组织人员工ID |
| `description` | varchar(1000) | YES |  | NULL | 计划说明 |
| `status` | tinyint | NO |  | 1 | 状态:1草稿 2已发布 3进行中 4已完成 5已取消 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

## `training_record`

培训档案表

外键：course_id → course；employee_id → employee；plan_id → training_plan

| 字段 | 类型 | 允许NULL | 键 | 默认值 | 注释 |
|---|---|---|---|---|---|
| `id` | bigint | NO | PK | NULL | 主键ID |
| `employee_id` | bigint | NO |  | NULL | 员工ID |
| `plan_id` | bigint | YES |  | NULL | 关联培训计划ID |
| `course_id` | bigint | YES |  | NULL | 关联课程ID |
| `course_name` | varchar(128) | NO |  | NULL | 课程名称(快照,防课程改名影响历史) |
| `train_type` | tinyint | YES |  | NULL | 培训类型:1新员工 2专业技能 3管理能力 4通用素质 5合规培训 |
| `hours` | decimal(5,1) | NO |  | 0.0 | 获得学时 |
| `train_date` | date | YES |  | NULL | 培训日期 |
| `exam_score` | decimal(5,1) | YES |  | NULL | 考核成绩 |
| `pass_status` | tinyint | NO |  | 0 | 考核结果:0未考核 1合格 2不合格 |
| `certificate_no` | varchar(64) | YES |  | NULL | 证书编号 |
| `archive_year` | smallint | YES |  | NULL | 归档年度 |
| `deleted` | tinyint | NO |  | 0 | 逻辑删除:0未删除 1已删除 |
| `created_at` | datetime | NO |  | CURRENT_TIMESTAMP | 创建时间 |
| `updated_at` | datetime | NO |  | CURRENT_TIMESTAMP | 更新时间 |

