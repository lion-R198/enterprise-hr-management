# 后端接口清单（自动生成）

> 由 `docs/gen-api-inventory.py` 扫描 Controller 源码生成，并与前端 `src/api/*.js` 的请求地址逐一比对。

- 后端端点总数：**94**（去重后 94）
- 前端契约请求地址：**91**
- 前端契约未覆盖：**0**
- 后端额外提供（兼容/扩展端点）：**3**


## 认证与令牌（`auth`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/auth/login` | AuthController#login | — | — | ✔ 契约内 |
| `POST` | `/auth/logout` | AuthController#logout | — | — | ✔ 契约内 |
| `GET` | `/auth/me` | AuthController#me | — | — | ✔ 契约内 |

## 首页看板（`dashboard`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `GET` | `/dashboard/charts` | DashboardController#charts | — | — | ✔ 契约内 |
| `GET` | `/dashboard/notices` | DashboardController#notices | — | — | ✔ 契约内 |
| `GET` | `/dashboard/summary` | DashboardController#summary | — | — | ✔ 契约内 |
| `GET` | `/dashboard/todos` | DashboardController#todos | — | — | ✔ 契约内 |
| `PUT` | `/dashboard/todos/{id}/handle` | DashboardController#handleTodo | — | 首页看板/UPDATE | ✔ 契约内 |

## 组织与员工（`employee`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `GET` | `/depts` | DeptController#list | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `POST` | `/depts` | DeptController#create | HR | 部门管理/INSERT | ✔ 契约内 |
| `GET` | `/depts/tree` | DeptController#tree | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `DELETE` | `/depts/{id}` | DeptController#delete | HR | 部门管理/DELETE | ✔ 契约内 |
| `PUT` | `/depts/{id}` | DeptController#update | HR | 部门管理/UPDATE | ✔ 契约内 |
| `POST` | `/employee-changes` | EmployeeChangeController#create | HR | 员工管理/UPDATE | ✔ 契约内 |
| `GET` | `/employee-changes/page` | EmployeeChangeController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `POST` | `/employee-contracts` | EmployeeContractController#create | HR | 员工管理/INSERT | ✔ 契约内 |
| `GET` | `/employee-contracts/page` | EmployeeContractController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `GET` | `/employees` | EmployeeController#list | HR、MANAGER、EMPLOYEE | — | 扩展 |
| `POST` | `/employees` | EmployeeController#create | HR | 员工管理/INSERT | ✔ 契约内 |
| `DELETE` | `/employees/batch` | EmployeeController#deleteBatch | HR | 员工管理/DELETE | 扩展 |
| `GET` | `/employees/by-emp-no/{empNo}` | EmployeeController#getByEmpNo | HR、MANAGER、EMPLOYEE | — | 扩展 |
| `GET` | `/employees/page` | EmployeeController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `DELETE` | `/employees/{id}` | EmployeeController#delete | HR | 员工管理/DELETE | ✔ 契约内 |
| `GET` | `/employees/{id}` | EmployeeController#detail | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/employees/{id}` | EmployeeController#update | HR | 员工管理/UPDATE | ✔ 契约内 |
| `GET` | `/employees/{id}/changes` | EmployeeController#changes | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/employees/{id}/status` | EmployeeController#changeStatus | HR | 员工管理/UPDATE | ✔ 契约内 |
| `GET` | `/positions` | PositionController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `POST` | `/positions` | PositionController#create | HR | 岗位管理/INSERT | ✔ 契约内 |
| `PUT` | `/positions/{id}` | PositionController#update | HR | 岗位管理/UPDATE | ✔ 契约内 |

## 考勤与假勤（`attendance`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `GET` | `/attendance-months/page` | AttendanceMonthController#page | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `PUT` | `/attendance-months/{id}/lock` | AttendanceMonthController#lock | HR | 考勤管理/UPDATE | ✔ 契约内 |
| `POST` | `/attendance/checkin` | AttendanceController#checkin | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/INSERT | ✔ 契约内 |
| `POST` | `/attendance/checkout` | AttendanceController#checkout | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/INSERT | ✔ 契约内 |
| `GET` | `/attendance/records` | AttendanceController#records | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `GET` | `/attendance/today` | AttendanceController#today | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `POST` | `/business-trips` | BusinessTripController#create | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/INSERT | ✔ 契约内 |
| `GET` | `/business-trips/page` | BusinessTripController#page | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `PUT` | `/business-trips/{id}/approve` | BusinessTripController#approve | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/APPROVE | ✔ 契约内 |
| `GET` | `/leave-balances/my` | LeaveBalanceController#my | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `POST` | `/leave-requests` | LeaveRequestController#create | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/INSERT | ✔ 契约内 |
| `GET` | `/leave-requests/page` | LeaveRequestController#page | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `PUT` | `/leave-requests/{id}/approve` | LeaveRequestController#approve | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/APPROVE | ✔ 契约内 |
| `POST` | `/overtime-requests` | OvertimeRequestController#create | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/INSERT | ✔ 契约内 |
| `GET` | `/overtime-requests/page` | OvertimeRequestController#page | HR、MANAGER、EMPLOYEE、FINANCE | — | ✔ 契约内 |
| `PUT` | `/overtime-requests/{id}/approve` | OvertimeRequestController#approve | HR、MANAGER、EMPLOYEE、FINANCE | 考勤管理/APPROVE | ✔ 契约内 |

## 薪资管理（`salary`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/salary-records/calculate` | SalaryRecordController#calculate | HR、FINANCE | 薪资管理/INSERT | ✔ 契约内 |
| `GET` | `/salary-records/page` | SalaryRecordController#page | HR、FINANCE、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/salary-records/{id}/pay` | SalaryRecordController#pay | HR、FINANCE | 薪资管理/UPDATE | ✔ 契约内 |
| `PUT` | `/salary-records/{id}/review` | SalaryRecordController#review | HR、FINANCE | 薪资管理/APPROVE | ✔ 契约内 |
| `POST` | `/salary-standards` | SalaryStandardController#create | HR | 薪资管理/INSERT | ✔ 契约内 |
| `GET` | `/salary-standards/page` | SalaryStandardController#page | HR、FINANCE、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `GET` | `/salary/payslips/my` | SalaryPayslipController#my | HR、FINANCE、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `GET` | `/salary/payslips/{recordId}/items` | SalaryPayslipController#items | HR、FINANCE、MANAGER、EMPLOYEE | — | ✔ 契约内 |

## 招聘管理（`recruit`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/candidates` | CandidateController#create | HR、MANAGER、EMPLOYEE | 招聘管理/INSERT | ✔ 契约内 |
| `GET` | `/candidates/page` | CandidateController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/candidates/{id}/stage` | CandidateController#changeStage | HR、MANAGER、EMPLOYEE | 招聘管理/UPDATE | ✔ 契约内 |
| `GET` | `/interview-records` | InterviewRecordController#list | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `POST` | `/interview-records` | InterviewRecordController#create | HR、MANAGER、EMPLOYEE | 招聘管理/INSERT | ✔ 契约内 |
| `GET` | `/offers/page` | OfferController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/offers/{id}/send` | OfferController#send | HR、MANAGER、EMPLOYEE | 招聘管理/UPDATE | ✔ 契约内 |
| `PUT` | `/offers/{id}/status` | OfferController#changeStatus | HR、MANAGER、EMPLOYEE | 招聘管理/UPDATE | ✔ 契约内 |
| `POST` | `/recruit-requests` | RecruitRequestController#create | HR、MANAGER | 招聘管理/INSERT | ✔ 契约内 |
| `GET` | `/recruit-requests/page` | RecruitRequestController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `PUT` | `/recruit-requests/{id}/approve` | RecruitRequestController#approve | HR | 招聘管理/APPROVE | ✔ 契约内 |

## 培训管理（`training`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/training-plans` | TrainingPlanController#create | HR | 培训管理/INSERT | ✔ 契约内 |
| `GET` | `/training-plans/page` | TrainingPlanController#page | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |
| `POST` | `/training-plans/{id}/enroll` | TrainingPlanController#enroll | HR、MANAGER、EMPLOYEE | 培训管理/INSERT | ✔ 契约内 |
| `GET` | `/training/my` | TrainingRecordController#my | HR、MANAGER、EMPLOYEE | — | ✔ 契约内 |

## 绩效管理（`performance`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/performance-plans` | PerformancePlanController#create | HR | 绩效考核/INSERT | ✔ 契约内 |
| `GET` | `/performance-plans/page` | PerformancePlanController#page | HR、MANAGER、EMPLOYEE、EXECUTIVE | — | ✔ 契约内 |
| `PUT` | `/performance-plans/{id}/publish` | PerformancePlanController#publish | HR | 绩效考核/APPROVE | ✔ 契约内 |
| `GET` | `/performance-records/page` | PerformanceRecordController#page | HR、MANAGER、EMPLOYEE、EXECUTIVE | — | ✔ 契约内 |
| `PUT` | `/performance-records/{id}/confirm` | PerformanceRecordController#confirm | HR、MANAGER、EMPLOYEE、EXECUTIVE | 绩效考核/APPROVE | ✔ 契约内 |
| `PUT` | `/performance-records/{id}/leader-score` | PerformanceRecordController#leaderScore | HR、MANAGER、EMPLOYEE、EXECUTIVE | 绩效考核/UPDATE | ✔ 契约内 |
| `PUT` | `/performance-records/{id}/self-score` | PerformanceRecordController#selfScore | HR、MANAGER、EMPLOYEE、EXECUTIVE | 绩效考核/UPDATE | ✔ 契约内 |

## 通知公告（`notice`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `POST` | `/notices` | NoticeController#create | ADMIN、HR | 通知公告/INSERT | ✔ 契约内 |
| `GET` | `/notices/page` | NoticeController#page | ADMIN、HR | — | ✔ 契约内 |
| `PUT` | `/notices/{id}/offline` | NoticeController#offline | ADMIN、HR | 通知公告/UPDATE | ✔ 契约内 |
| `PUT` | `/notices/{id}/publish` | NoticeController#publish | ADMIN、HR | 通知公告/UPDATE | ✔ 契约内 |

## 系统管理（`system`）

| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |
|---|---|---|---|---|---|
| `GET` | `/sys/configs` | SysConfigController#list | ADMIN、HR | — | ✔ 契约内 |
| `PUT` | `/sys/configs/{id}` | SysConfigController#update | ADMIN | 系统设置/UPDATE | ✔ 契约内 |
| `GET` | `/sys/dict-types` | SysDictController#listTypes | — | — | ✔ 契约内 |
| `GET` | `/sys/dict-types/{typeId}/items` | SysDictController#listItems | — | — | ✔ 契约内 |
| `GET` | `/sys/logs/login` | SysLogController#pageLoginLogs | ADMIN | — | ✔ 契约内 |
| `GET` | `/sys/logs/oper` | SysLogController#pageOperLogs | ADMIN | — | ✔ 契约内 |
| `GET` | `/sys/roles` | SysRoleController#list | ADMIN、HR | — | ✔ 契约内 |
| `POST` | `/sys/roles` | SysRoleController#create | ADMIN | 系统设置/INSERT | ✔ 契约内 |
| `PUT` | `/sys/roles/{id}` | SysRoleController#update | ADMIN | 系统设置/UPDATE | ✔ 契约内 |
| `POST` | `/sys/users` | SysUserController#create | ADMIN | 系统设置/INSERT | ✔ 契约内 |
| `GET` | `/sys/users/page` | SysUserController#page | ADMIN | — | ✔ 契约内 |
| `DELETE` | `/sys/users/{id}` | SysUserController#delete | ADMIN | 系统设置/DELETE | ✔ 契约内 |
| `PUT` | `/sys/users/{id}` | SysUserController#update | ADMIN | 系统设置/UPDATE | ✔ 契约内 |
| `PUT` | `/sys/users/{id}/password/reset` | SysUserController#resetPassword | ADMIN | 系统设置/UPDATE | ✔ 契约内 |
