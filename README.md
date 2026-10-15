# Enterprise HR Management

企业人事管理系统最小可运行示例，基于 **Spring Boot 3.3.x + MyBatis-Plus 3.5.7 + MySQL 8.0**。

> 已演示一条**完整的基础查询流程**：列表查询 / 按ID查询 / 按业务字段（工号）查询 / 分页+关键字模糊查询。

---

## 一、技术栈

| 模块       | 版本             | 说明                              |
| ---------- | ---------------- | --------------------------------- |
| Spring Boot | 3.3.4           | Web、Validation                   |
| Java       | 17               | -                                 |
| MyBatis-Plus | 3.5.7 (spring-boot3-starter) | ORM + 分页 + 逻辑删除  |
| MySQL      | 8.0.33 (mysql-connector-j) | JDBC 驱动           |
| Lombok     | 1.18.34          | 简化代码                           |
| JUnit 5    | Spring Boot Test | 自带                                |

只引入关键依赖，已过滤掉 Security / Redis / Swagger / Actuator 等非必需项。

---

## 二、目录结构

```
enterprise-hr-management
├─ pom.xml
├─ sql/
│   └─ enterprise_hr.sql                  # ⚠️ 早期示例（仅 2 表），已被根目录脚本取代
├─ enterprise_hr_schema.sql               # ★ 完整建表脚本：49 表 / 80 外键 / 35 唯一键
├─ data.sql                               # ★★ 完整业务数据集：50 员工 + 全部 49 表 6519 行
├─ enterprise_hr_init_data.sql            # 轻量基线数据（8 员工，快速冒烟用）
├─ 企业人事管理系统需求规格说明书.md        # ★ 9 模块 / 52 功能点 / 6 角色 / 优先级
├─ 数据库设计说明书.md                     # ★ E-R 关系 + 逐表字段级结构 + 索引外键汇总
├─ src/main/java/com/enterprise/hr/
│   ├─ EnterpriseHrApplication.java       # 启动类 (@MapperScan)
│   ├─ config/
│   │  ├─ MybatisPlusConfig.java          # 分页插件
│   │  └─ MybatisAutoFillHandler.java     # 自动填充 createTime/updateTime
│   ├─ common/
│   │  ├─ R.java                          # 统一响应封装 {code, message, data}
│   │  └─ GlobalExceptionHandler.java     # 全局异常处理
│   └─ employee/                          # 员工模块 (示例业务)
│       ├─ entity/
│       │  ├─ Employee.java               # ORM 实体 (不带 JSR 校验)
│       │  └─ Dept.java
│       ├─ dto/                           # 入参 / 出参 DTO
│       │  ├─ EmployeeCreateReq.java      # 新增请求 (JSR 校验)
│       │  ├─ EmployeeUpdateReq.java      # 修改请求 (局部更新)
│       │  ├─ EmployeeVO.java             # 展示 VO
│       │  └─ convert/EmployeeConverter.java  # DTO ↔ Entity 互转
│       ├─ mapper/
│       │  ├─ EmployeeMapper.java
│       │  └─ DeptMapper.java
│       ├─ service/
│       │  ├─ EmployeeService.java
│       │  └─ impl/EmployeeServiceImpl.java
│       └─ controller/
│           ├─ EmployeeController.java
│           └─ DeptController.java
├─ src/main/resources/
│   └─ application.yml
├─ src/test/
│   ├─ java/com/enterprise/hr/
│   │  ├─ EnterpriseHrApplicationTests.java
│   │  └─ employee/
│   │     ├─ service/impl/EmployeeServiceImplTest.java   # 18 用例 (Mockito)
│   │     ├─ controller/EmployeeControllerTest.java       # 19 用例 (MockMvc)
│   │     └─ integration/
│   │        ├─ EmployeeIntegrationTest.java              # 9 用例 (真实 MySQL)
│   │        └─ EmployeeHttpIntegrationTest.java          # 7 用例 (TestRestTemplate)
│   ├─ resources/application-test.yml
│   └─ resources/sql/test-cleanup.sql
├─ docs/
│   ├─ API.md
│   ├─ functional-structure.html / .png / .mmd   # 功能结构图（HTML 可直接打开）
│   ├─ er-diagram.html / .png                    # 数据库 E-R 关系图 + 49 表可搜索速查
│   ├─ gen_data_sql.py                           # ★ 由脚本生成根目录 data.sql（可复现）
│   ├─ check_data_sql.py                         # ★ data.sql 结构 / 覆盖 / 业务口径校验
│   ├─ gen_table_doc.py                          # 由 DDL 生成《数据库设计说明书》第 4-7 章
│   ├─ gen_er_html.py                            # 由 DDL 生成 er-diagram.html
│   └─ check_fk_integrity.py                     # 80 个外键孤儿记录校验
└─ README.md
```

### 文档与脚本索引

| 文件 | 说明 |
| --- | --- |
| `enterprise_hr_schema.sql` | 建库 + 49 张表 DDL（含字段注释、主键、外键、索引），实测通过 |
| `data.sql` | **完整业务数据集**：50 名员工 + 全 49 表 6519 行，可重复执行（详见第 2.1 节） |
| `enterprise_hr_init_data.sql` | 轻量基线数据（4 部门 / 8 员工），用于快速冒烟验证 |
| `数据库设计说明书.md` | 7 章：概述 / 设计规范 / E-R 关系 / 逐表结构 / 索引外键汇总 / 初始化数据 / 部署验证 |
| `企业人事管理系统需求规格说明书.md` | 需求基线：52 功能点 + MoSCoW 优先级 + 权限矩阵 |
| `docs/er-diagram.html` | 交互式 E-R 图 + 可搜索表结构速查（推荐浏览器打开） |
| `docs/functional-structure.html` | 功能结构图（9 模块 / 52 子功能） |
| `docs/gen_data_sql.py` | **由脚本生成 `data.sql`**（固定随机种子，输出可复现，改参数后重跑即可） |
| `docs/check_data_sql.py` | `data.sql` 一致性校验：结构 + 覆盖 + 18 项业务口径（导入前跑，无需数据库） |
| `docs/gen_table_doc.py` | 重新生成《数据库设计说明书》第 4-7 章（幂等，可反复执行） |
| `docs/gen_er_html.py` | 重新生成 `er-diagram.html` |
| `docs/check_fk_integrity.py` | 校验全部 80 个外键无孤儿记录 |

---

## 三、使用步骤

### 1. 准备 MySQL 8

```bash
# 1) 建库 + 49 张表（含字段注释 / 主键 / 外键 / 索引）
mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_schema.sql

# 2) 导入数据（二选一，均可在建表脚本之后反复执行）
mysql -uroot -p123456 --default-character-set=utf8mb4 < data.sql                     # 推荐：完整业务数据集
mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_init_data.sql  # 可选：轻量基线数据
```

> ⚠️ Windows 下必须带 `--default-character-set=utf8mb4`，否则客户端默认 gbk，中文注释与数据会乱码。

脚本会：
1. 删除并重建 `enterprise_hr` 库（utf8mb4 / utf8mb4_0900_ai_ci）
2. 创建 **49 张表**（9 大功能模块，80 外键 / 35 唯一键），覆盖需求规格说明书全部 52 项功能
3. 导入演示数据

演示账号初始密码统一为 `123456`（BCrypt 存储），生产部署前务必修改。

### 2. data.sql —— 完整业务数据集

`data.sql` 提供**贴近真实运营状态**的全量数据，导入后即可支撑列表、统计与审批流的演示：

| 维度 | 数据规模 |
| --- | --- |
| 组织与员工 | 13 部门（3 层）/ 16 岗位 / **50 名员工**（工号 E0001~E0050）/ 50 份劳动合同 / 86 条异动记录 / 235 个附件 |
| 账号权限 | 51 个系统账号（1 管理员 + 50 员工自助）/ 6 角色 / 61 菜单 / 153 条角色菜单授权 |
| 考勤休假 | 7-8 月**逐日打卡 1954 条** / 90 个月度汇总（7 月已封账）/ 506 条排班 / 141 条假期余额 / 38 请假 + 38 加班 + 15 出差 |
| 薪资社保 | 2 个月薪资记录 90 条 + **薪资条明细 924 条** / 社保公积金缴纳 90 条 |
| 招聘 | 8 条需求 → 15 条职位发布 → 28 名候选人 → 34 条面试 → 5 份 Offer（3 人已入职并回写员工主数据） |
| 培训 | 8 名讲师 / 10 门课程 / 7 个计划 / 66 条报名签到 / 48 条培训档案 |
| 绩效 | 4 个考核方案 / 402 项指标 / 134 条考核记录 / 71 次绩效面谈 |
| 公共与日志 | 12 条公告 + 349 条已读 / 26 条待办 / 119 条审批流水 / 188 条登录日志 / 167 条操作日志 |
| **合计** | **49 张表 / 6519 行**（`docs/check_data_sql.py` 静态还原值，与库内 `COUNT(*)` 一致） |

**数据自洽性**（`docs/check_data_sql.py` 与库内 SQL 双重验证，全部 0 异常）：

- `attendance_month` 的应出勤天数 = `attendance_record` 逐日明细条数
- 已批准的请假 / 出差单**回填**到逐日打卡明细（考勤状态 6 休假 / 7 出差）
- `leave_balance.used_days` = 已批准请假单天数汇总
- `salary_record.gross_pay` = `salary_item` 应发项之和；`net_pay` = 应发 − 个人社保 − 个人公积金 − 个税 − 考勤/其他扣款
- `salary_record` 的社保 / 公积金金额 = `insurance_record` 明细汇总
- `notice.read_count` = `notice_read` 明细条数
- `recruit_request.hired_count` = 已入职 Offer 数；已入职 Offer 的入职日期 / 部门 / 岗位与 `employee` 主数据完全一致
- 绩效等级与 `performance_plan.grade_rule` 的分档区间一致，并与薪资绩效工资系数同源
- `sys_role_menu` 的 153 条授权由 `INSERT ... SELECT FROM sys_menu` 派生，校验脚本会**静态求值 WHERE 条件**还原行数（144 条 SELECT + 9 条 VALUES），与库内一致

> 数据由 `docs/gen_data_sql.py` 生成（固定随机种子 `20260914`），改完参数重跑即可复现同一份结果：
> ```bash
> python docs/gen_data_sql.py      # 重新生成 data.sql
> python docs/check_data_sql.py    # 校验结构与业务口径（不需要数据库）
> ```

> `sql/enterprise_hr.sql` 是早期示例脚本（仅 `dept` / `employee` 两表），已被根目录脚本取代，仅作历史参考。若只要一个最小的可跑数据集，用 `enterprise_hr_init_data.sql`（8 名员工）；要演示完整业务链路，用 `data.sql`。

> ⚠️ **跑完测试后需重新导入数据**：`mvn test` 中的集成测试会执行
> `src/test/resources/sql/test-cleanup.sql`，**硬删除全部 49 张业务表的数据**，
> 并重建「2 部门 / 3 岗位 / 3 员工」的最小种子数据。因此测试结束后演示数据集会被清空，
> 如需继续演示请重新执行 `mysql -uroot -p123456 --default-character-set=utf8mb4 < data.sql`。

### 3. 修改数据库连接

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: <你的 MySQL 用户>
    password: <你的 MySQL 密码>
```

### 4. 用 IDEA 打开

打开 IDEA → **File → Open** → 选择 `D:\Workspace\enterprise-hr-management\pom.xml` → 一路确认。

等待 Maven 同步完成后：

- 主类：`com.enterprise.hr.EnterpriseHrApplication`
- 点击 ▶️ 启动。

### 5. 验证接口

| 方法 | 路径                                | 说明              |
| ---- | ----------------------------------- | ----------------- |
| GET  | `/api/employees`                    | 全部员工          |
| GET  | `/api/employees/{id}`               | 按ID查询          |
| GET  | `/api/employees/by-emp-no/{empNo}`  | 按工号查询        |
| GET  | `/api/employees/page?current=1&size=10&keyword=张` | 分页+关键字 |
| POST | `/api/employees`                    | 新增员工          |
| PUT  | `/api/employees/{id}`               | 修改员工          |
| DELETE | `/api/employees/{id}`             | 删除员工（逻辑删除）|
| DELETE | `/api/employees/batch`            | 批量删除          |
| GET  | `/api/depts`                        | 全部部门          |

幂等响应示例（`EmployeeVO` 不暴露 `deleted` / `createdAt` 等内部字段，主键由雪花算法生成）：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1943257861294874624,
      "empNo": "E0001",
      "name": "张三",
      "gender": "M",
      "email": "zhangsan@enterprise.com",
      "phone": "13800000001",
      "deptId": 1,
      "position": "Java 开发工程师",
      "hireDate": "2024-03-01",
      "salary": 15000.00,
      "status": 1
    }
  ]
}
```

---

## 四、关键设计点

| 主题       | 落点                                                                                                |
| ---------- | --------------------------------------------------------------------------------------------------- |
| 统一响应   | `R<T>` 三段式 `{code, message, data}`                                                               |
| DTO / VO 分层 | 入参 `EmployeeCreateReq/UpdateReq` 带 JSR 校验；出参 `EmployeeVO` 不暴露 deleted/createdAt；Entity 仅做 ORM 映射 |
| 校验分工   | Controller 入参 `@Valid` 自动 400；业务必填兜底在 Service 层抛 `IllegalArgumentException` 由 GlobalExceptionHandler 收口 |
| 局部更新   | `copyUpdatableFields` 只覆盖入参非空字段，缺失字段不破坏原值 |
| 工号不可变 | 工号作为业务主键，更新时拒绝修改（防破坏唯一键） |
| 逻辑删除   | `deleted` 字段 + `@TableLogic`，全局已配置                                                           |
| 雪花 ID    | `IdType.ASSIGN_ID` 对应 MyBatis-Plus 自带雪花算法                                                    |
| 自动填充   | `MybatisAutoFillHandler` 在 insert/update 时回填 createdAt / updatedAt                              |
| 分页插件   | `PaginationInnerInterceptor(DbType.MYSQL)` 注入到 `MybatisPlusInterceptor`                           |
| 全局异常   | `@RestControllerAdvice` 捕获 `IllegalArgumentException` / `IllegalStateException` / `ConstraintViolationException` / `MethodArgumentNotValidException` 等 |
| Mapper扫描 | `@MapperScan("com.enterprise.hr.**.mapper")` 自动注册全部 Mapper                                     |

---

## 五、扩展建议（按需增加，非必需已隐藏）

- 用户认证 → `spring-boot-starter-security`
- API 文档 → `springdoc-openapi-starter-webmvc-ui`
- 缓存 → `spring-boot-starter-data-redis`
- 监控 → `spring-boot-starter-actuator`
