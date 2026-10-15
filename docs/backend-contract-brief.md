# 后端接口契约与编码规范（模块开发统一遵循）

> 本文件是后端各业务模块开发的**唯一风格基线**。新增接口前先读本文件，不要凭个人习惯另起一套。
> 项目根目录：`D:\Workspace\enterprise-hr-management`　数据库：`enterprise_hr`（MySQL 8.0）

---

## 1. 分层与包结构

```
com.enterprise.hr
├─ common/                      公共层（已就绪，勿改）
│  ├─ R / PageResult / PageQuery / ErrorCode / BizException / GlobalExceptionHandler
│  ├─ constant/  RoleCode · BizConst · DataScopeType · BizType
│  ├─ security/  JwtUtil · LoginUser · UserContext · AuthInterceptor · RequireRoles · DataScopeHelper
│  ├─ log/       OperLog · OperType · OperLogRecorder · OperLogRecord · OperLogAspect
│  └─ util/      MaskUtil · BizNoGenerator · IpUtil
├─ config/                      WebMvcConfig · JacksonConfig · EhrProperties · MybatisPlusConfig · MybatisAutoFillHandler
├─ employee/                    组织与员工：dept · position · employee · employee_change · employee_contract
├─ attendance/                  考勤：attendance_record · leave_* · overtime_request · business_trip · attendance_month · shift
├─ salary/                      薪资：salary_standard · salary_record · salary_item · employee_salary · insurance_*
├─ recruit/                     招聘：recruit_request · candidate · interview_record · offer_record · job_posting
├─ training/                    培训：training_plan · course · training_enroll · training_record · trainer
├─ performance/                 绩效：performance_plan · performance_record · performance_indicator
├─ notice/                      通知公告与待办：notice · notice_read · todo_task · approval_record
├─ system/                      系统设置：sys_user · sys_role · sys_dict_* · sys_config · sys_login_log · sys_oper_log
└─ dashboard/                   首页看板聚合
```

每个模块内部固定四层：`controller` / `service`(+`impl`) / `mapper` / `entity`（+ `dto`）。
**Controller 只做参数绑定、DTO↔Entity↔VO 转换、响应封装；业务规则一律落在 Service。**

---

## 2. 统一响应契约

响应体恒为三段式，HTTP 状态码与业务 `code` 同值：

```json
{ "code": 200, "message": "success", "data": { } }
```

| code | 语义 | 前端行为（`src/utils/request.js`） |
|---|---|---|
| 200 | 成功 | `resolve(res.data)`，业务代码拿到的就是 `data` |
| 400 | 参数/业务校验失败（BR-xx） | 弹出 `message`，reject |
| 401 | 令牌缺失/过期/非法 | 清令牌 + 跳登录页 |
| 403 | 已认证但越权（角色不足 / 行级范围外） | 弹出 `message` |
| 404 | 资源或接口不存在 | 弹出 `message` |
| 409 | 唯一键冲突等状态冲突 | 弹出 `message` |
| 500 | 服务端异常（不外露堆栈） | 弹出 `message` |

**分页响应统一用 `PageResult<T>`**（四字段，勿直接返回 `IPage`）：

```json
{ "records": [ ], "total": 91, "current": 1, "size": 10 }
```

**非分页的列表型接口**（如「我的假期余额」「我的薪资条」）同样用 `PageResult.list(list)`，
前端统一读 `records` / `total`。

---

## 3. 字段命名（关键，最易出错）

| 方向 | 命名风格 | 说明 |
|---|---|---|
| **响应** | **蛇形 `snake_case`** | 与数据库列名一致，前端 `el-table-column prop="emp_no"` 直接绑定 |
| **请求体** | 驼峰 `camelCase` | 前端发送 `{ deptId: 1, positionId: 2 }` |
| **查询参数** | 驼峰 `camelCase` | `?current=1&size=10&deptId=2&keyword=张` |

实现方式：
- **实体类**统一标 `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`（已完成）
- **VO 类**同样必须标该注解
- **Req DTO 类**（`@RequestBody` 绑定）**不要标**该注解
- 少数前端明确消费的**驼峰字段**，用 `@JsonProperty("xxx")` 显式覆盖，例如：

```java
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DeptTreeVO {
    private Long id;
    private String name;
    private Long headcountLimit;      // → headcount_limit

    /** 前端 dept/index.vue 读 row.employeeCount，必须保持驼峰 */
    @JsonProperty("employeeCount")
    private Long employeeCount;
}
```

**已知需要 `@JsonProperty` 保持驼峰的字段**（前端已在用，不可改名）：

| 字段 | 使用位置 |
|---|---|
| `employeeCount` | `dept/index.vue`、`dept/positions.vue`（在岗人数） |
| `enrollCount` | `training/plan.vue`（已报名） |
| `recordCount` | `performance/plan.vue`（考核人数） |
| `totalHours` | `training/my.vue`（累计学时） |
| `enrolls` | `training/my.vue`（我的报名） |
| `requestNo` | 请假/加班/出差/招聘需求 新增后回显单号 |
| `planCode` / `contractNo` / `standardCode` | 培训计划/合同/薪资标准 新增后回显编号 |
| `checkinTime` / `checkoutTime` | `attendance/checkin.vue` 打卡成功提示 |
| `finalScore` / `grade` / `weightNote` | `performance/record.vue` 评分结果 |
| `created` | `salary/record.vue`（核算生成条数） |
| `initPassword` | 重置密码回显 |

---

## 4. 常用类速查

### R
```java
R.ok()                      // 200，data=null
R.ok(data)                  // 200，带数据
R.ok("提示", data)
R.fail(ErrorCode.XXX)       // 按错误码
R.fail("自定义提示")          // 500
```

### BizException（业务规则拦截，一律用错误码，不要硬编码字符串）
```java
throw new BizException(ErrorCode.DEPT_HAS_EMPLOYEE);
throw new BizException(ErrorCode.NOT_FOUND, "员工不存在: " + id);
throw BizException.notFound("培训计划不存在");
throw BizException.forbidden("仅直属主管或人事专员可审批");
throw BizException.dataScopeDenied();
throw BizException.badRequest("缺少必填字段：planName");
```
`ErrorCode` 已有 60+ 业务码（含 BR-02~BR-09 全套），**先查再新增**，不要重复定义。

### 当前登录用户
```java
LoginUser user = UserContext.require();      // 未登录抛 401
Long empId   = user.getEmployeeId();         // 关联员工ID
Long deptId  = user.getDeptId();
boolean hr   = user.hasAnyRole(RoleCode.HR, RoleCode.ADMIN);
boolean limited = DataScopeHelper.isSelfOnly(user);
```

### 行级数据范围（NF-10）
```java
if (!DataScopeHelper.isUnlimited(user)) {
    if (DataScopeHelper.isSelfOnly(user)) {
        w.eq(Xxx::getEmployeeId, user.getEmployeeId());
    } else {                                   // 部门主管
        w.eq(Xxx::getDeptId, user.getDeptId());
    }
}
```
按 ID 直查的详情接口必须显式校验：
```java
if (!DataScopeHelper.canAccessEmployee(user, target.getEmployeeId(), target.getDeptId())) {
    throw BizException.dataScopeDenied();
}
```

### 分页
```java
public class EmployeeQuery extends PageQuery {
    private String keyword;
    private Long deptId;
}
// Service
Page<Employee> page = query.toPage();
return PageResult.of(baseMapper.selectPage(page, w), this::toVO);
```

### 操作日志（只标改变状态的接口）
```java
@OperLog(module = "考勤管理", type = OperType.APPROVE, action = "请假审批")
@PutMapping("/{id}/approve")
public R<Void> approve(...) { }
```

### 单号与工号
```java
long seq = baseMapper.selectCount(Wrappers.<LeaveRequest>lambdaQuery()
        .likeRight(LeaveRequest::getRequestNo, BizNoGenerator.yearPrefix("LV"))) + 1;
String no = BizNoGenerator.next("LV", seq);     // → LV202600001
String empNo = BizNoGenerator.empNo(nextSeq);   // → E0051
```

### 脱敏（NF-08）
```java
vo.setIdCard(MaskUtil.maskIdCard(emp.getIdCard()));       // 110101**********23
vo.setBankAccount(MaskUtil.maskBankAccount(...));
```

### 时间
实体用 `LocalDateTime` / `LocalDate`；序列化已全局配置为 `yyyy-MM-dd HH:mm:ss` / `yyyy-MM-dd`。
取"今天"用 `LocalDate.now()`，"当前时间"用 `LocalDateTime.now()`。
**不要用字符串拼时间**，也不要依赖 mock 里的 `dayOffset`（那是前端 mock 专用）。

---

## 5. 权限注解

```java
@RestController
@RequestMapping("/leave-requests")
@RequireRoles({RoleCode.HR, RoleCode.MANAGER, RoleCode.EMPLOYEE})   // 类级：可访问的角色集合
public class LeaveRequestController {

    @RequireRoles(RoleCode.HR)                                      // 方法级优先，收窄权限
    @PutMapping("/{id}/approve")
    public R<Void> approve(...) { }
}
```
- 未标注 = 只需登录
- `ROLE_ADMIN` 默认拥有全部接口权限（拦截器内置）

---

## 6. 数据库约定

- 49 张表已存在，**只读不改**；字段以 `docs/db-columns.md` 为准（自动生成，含列名/类型/注释）
- 主键 `id BIGINT AUTO_INCREMENT`，实体用 `@TableId(value = "id", type = IdType.AUTO)`
- 逻辑删除：表中**有** `deleted` 列才加 `@TableLogic`；日志类表（`sys_login_log`/`sys_oper_log`/`notice_read`）**没有**该列
- `created_at` / `updated_at` 由 `MybatisAutoFillHandler` 自动填充
- 状态/类型字段一律用 `BizConst` 常量，不要裸写数字

---

## 7. 交付要求

1. **不要运行 Maven**（`mvn.cmd`）：多人同时改同一个 `target/` 会互相破坏，统一由主控编译。
2. **不要修改本模块以外的文件**：特别不要动 `common/`、`config/`、`pom.xml`、`application.yml`、其他模块的 `entity`/`mapper`。
3. 实体与 Mapper 已全部存在（49 表 / 49 Mapper），**只允许新增** `controller` / `service` / `dto`（`vo`）类。
4. 所有 Java 文件用 **UTF-8 无 BOM**，注释用简体中文，类上写 JavaDoc。
5. 每个改动状态的接口都要有 `@OperLog`；每处业务规则拦截都要抛 `BizException` 并在注释里标注 `BR-xx`。
