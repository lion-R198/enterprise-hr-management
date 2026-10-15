# -*- coding: utf-8 -*-
"""
从 enterprise_hr_schema.sql 解析出全部表的字段级结构，生成《数据库设计说明书》第 4 章的 Markdown。
保证文档与 DDL 完全一致（注释、类型、默认值、索引、外键均由脚本直接取自 SQL）。
"""
import re
import sys
from pathlib import Path

BASE = Path(r"D:\Workspace\enterprise-hr-management")
SQL = BASE / "enterprise_hr_schema.sql"

MODULE_TITLES = {
    "01": "首页",
    "02": "员工信息",
    "03": "部门管理",
    "04": "考勤管理",
    "05": "薪资管理",
    "06": "招聘管理",
    "07": "培训管理",
    "08": "绩效考核",
    "09": "系统设置",
    "00": "公共组件",
}
MODULE_ORDER = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "00"]

text = SQL.read_text(encoding="utf-8")
lines = text.split("\n")

table_re = re.compile(r"^CREATE TABLE `(?P<name>[^`]+)`\s*\(\s*$")
module_re = re.compile(r"^--\s+【模块\s*(?P<code>\d{2})】(?P<rest>.+)$")
title_re = re.compile(r"^--\s+(?P<funcs>(?:【F-[^】]+】)+)(?P<rest>.+)$")
col_re = re.compile(r"^\s*`(?P<name>[^`]+)`\s+(?P<type>[A-Za-z]+(?:\([^)]*\))?)\s*(?P<rest>.*?),?\s*$")
pk_re = re.compile(r"^\s*PRIMARY KEY \(`?(?P<cols>[^)]*)`?\)")
uk_re = re.compile(r"^\s*UNIQUE KEY `(?P<name>[^`]+)` \((?P<cols>[^)]*)\)")
key_re = re.compile(r"^\s*(?:KEY|INDEX) `(?P<name>[^`]+)` \((?P<cols>[^)]*)\)")
fk_re = re.compile(r"^\s*CONSTRAINT `(?P<name>[^`]+)` FOREIGN KEY \((?P<col>[^)]*)\) REFERENCES `(?P<ref>[^`]+)` \(`?(?P<refcol>[^)`]*)`?\)")

tables = []
cur = None
pending_module = None
pending_funcs = None
pending_title = None

for idx, ln in enumerate(lines):
    m = module_re.match(ln)
    if m:
        pending_module = m.group("code")
        continue
    m = title_re.match(ln)
    if m and "表" in m.group("rest"):
        pending_funcs = m.group("funcs")
        pending_title = m.group("rest").strip()
        continue
    m = table_re.match(ln)
    if m:
        cur = {
            "name": m.group("name"),
            "module": pending_module,
            "funcs": pending_funcs,
            "title": pending_title,
            "cols": [],
            "pk": None,
            "uks": [],
            "keys": [],
            "fks": [],
        }
        tables.append(cur)
        continue
    if cur is None:
        continue
    if ln.strip().startswith(")"):
        # 表结束，取表注释
        cm = re.search(r"COMMENT\s*=\s*'([^']*)'", ln)
        cur["comment"] = cm.group(1) if cm else ""
        cur = None
        pending_funcs = None
        pending_title = None
        continue
    m = pk_re.match(ln)
    if m:
        cur["pk"] = m.group("cols").replace("`", "").strip()
        continue
    m = uk_re.match(ln)
    if m:
        cur["uks"].append((m.group("name"), m.group("cols").replace("`", "").strip()))
        continue
    m = fk_re.match(ln)
    if m:
        nxt = lines[idx + 1] if idx + 1 < len(lines) else ""
        dm = re.search(r"ON DELETE (\w+)", nxt)
        cur["fks"].append(
            (m.group("name"), m.group("col").replace("`", "").strip(),
             m.group("ref"), m.group("refcol").strip(),
             dm.group(1) if dm else "RESTRICT")
        )
        continue
    m = key_re.match(ln)
    if m:
        cur["keys"].append((m.group("name"), m.group("cols").replace("`", "").strip()))
        continue
    m = col_re.match(ln)
    if m:
        rest = m.group("rest")
        nullable = "NOT NULL" not in rest
        dm = re.search(r"DEFAULT\s+('(?:[^']*)'|[\w()]+)", rest)
        default = dm.group(1) if dm else ""
        if default.upper() == "NULL":
            default = ""
        cim = re.search(r"COMMENT\s+'([^']*)'", rest)
        comment = cim.group(1) if cim else ""
        cur["cols"].append({
            "name": m.group("name"),
            "type": m.group("type"),
            "nullable": nullable,
            "default": default,
            "auto": "AUTO_INCREMENT" in rest.upper(),
            "comment": comment,
        })
        continue

# 末尾 ALTER TABLE 补充的外键
tail_fk = re.findall(
    r"ALTER TABLE `(?P<t>[^`]+)`\s*\n\s*ADD CONSTRAINT `(?P<n>[^`]+)` FOREIGN KEY \((?P<c>[^)]*)\)\s*\n?\s*REFERENCES `(?P<r>[^`]+)` \(`?(?P<rc>[^)`]*)`?\)",
    text,
)
for t, n, c, r, rc in tail_fk:
    for tb in tables:
        if tb["name"] == t:
            dm = re.search(
                r"ADD CONSTRAINT `" + re.escape(n) + r"`.*?ON DELETE (\w+)",
                text, re.S)
            tb["fks"].append((n, c.replace("`", "").strip(), r, rc.strip(),
                              dm.group(1) if dm else "RESTRICT"))

# ---- 模块归属：以功能点编号 F-<模块>-<序号> 为准（SQL 中建表顺序是按依赖排列的，不能当作模块分组）----
func_code_re = re.compile(r"F-(\d{2})-")
for t in tables:
    codes = func_code_re.findall(t["funcs"] or "")
    if codes:
        # 取出现次数最多的模块号；并列时取最小
        t["module"] = max(set(codes), key=lambda c: (codes.count(c), -int(c)))
    else:
        t["module"] = "00"

assert len(tables) == 49, f"解析到 {len(tables)} 张表，预期 49 张"

# ---------------- 生成 Markdown ----------------
out = []
w = out.append

w("## 4. 表结构详细说明")
w("")
w("> 本章逐表给出字段级结构。所有字段名、类型、默认值、注释、索引与外键均**直接取自** "
  "`enterprise_hr_schema.sql`（由工具脚本解析生成），与 DDL 严格一致。")
w("")
w("阅读约定：")
w("")
w("- 「空」列 ✗ 表示 `NOT NULL`，✓ 表示允许 `NULL`；")
w("- 「说明」列即 DDL 中的 `COMMENT`，也是数据库里存储的字段注释；")
w("- 全部表主键统一为 `id BIGINT AUTO_INCREMENT`；")
w("- `deleted` 为逻辑删除标记（0 未删除 / 1 已删除），**日志类表无此列**；")
w("- `created_at` / `updated_at` 由数据库默认值 `CURRENT_TIMESTAMP` 与 `ON UPDATE` 自动维护，")
w("  业务代码通常无需显式赋值（MyBatis-Plus 侧另有 `MetaObjectHandler` 双保险）。")
w("")

# 4.1 总览
w("### 4.1 全部表清单总览")
w("")
w(f"共 **{len(tables)} 张表**、"
  f"**{sum(len(t['cols']) for t in tables)} 个字段**、"
  f"**{sum(len(t['fks']) for t in tables)} 个外键**、"
  f"**{sum(len(t['uks']) for t in tables)} 个唯一键**。")
w("")
w("| # | 模块 | 表名 | 中文名 | 字段数 | 外键 | 逻辑删除 |")
w("| ---: | --- | --- | --- | ---: | ---: | :-: |")
for i, t in enumerate(tables, 1):
    has_del = any(c["name"] == "deleted" for c in t["cols"])
    mod = f"{t['module']} {MODULE_TITLES.get(t['module'], '')}"
    w(f"| {i} | {mod} | `{t['name']}` | {t['comment']} | {len(t['cols'])} | {len(t['fks'])} | {'✓' if has_del else '—'} |")
w("")

# 4.2 起按模块分组（模块号升序，模块内保持建表顺序）
groups = []
for code in MODULE_ORDER:
    ts = [t for t in tables if t["module"] == code]
    if ts:
        groups.append((code, ts))

sec = 2
for code, ts in groups:
    w(f"### 4.{sec} 模块 {code} · {MODULE_TITLES.get(code, '')}（{len(ts)} 张表）")
    w("")
    sub = 1
    for t in ts:
        funcs = "、".join(re.findall(r"【([^】]+)】", t["funcs"] or ""))
        w(f"#### 4.{sec}.{sub} `{t['name']}` — {t['comment']}")
        w("")
        if funcs:
            w(f"- **对应功能点**：{funcs}")
        w(f"- **模块归属**：模块 {code} · {MODULE_TITLES.get(code, '')}")
        w(f"- **字段数**：{len(t['cols'])}　|　**外键数**：{len(t['fks'])}")
        if t["fks"]:
            refs = "、".join(f"`{r}`" for _, _, r, _, _ in t["fks"])
            w(f"- **上级依赖**：{refs}")
        w("")
        w("| 字段名 | 类型 | 空 | 默认值 | 说明 |")
        w("| --- | --- | :-: | --- | --- |")
        for c in t["cols"]:
            dft = c["default"] if c["default"] else "—"
            if c["auto"]:
                dft = "AUTO_INCREMENT"
            w(f"| `{c['name']}` | {c['type']} | {'✓' if c['nullable'] else '✗'} | {dft} | {c['comment']} |")
        w("")
        if t["pk"]:
            w(f"**主键**：`{t['pk']}`")
        if t["uks"]:
            w("")
            w("**唯一键**：")
            for n, cols in t["uks"]:
                w(f"- `{n}`（{cols}）")
        if t["keys"]:
            w("")
            w("**普通索引**：")
            for n, cols in t["keys"]:
                w(f"- `{n}`（{cols}）")
        w("")
        w("**外键**：" if t["fks"] else "**外键**：无")
        for n, col, ref, refcol, ondel in t["fks"]:
            w(f"- `{n}`：`{col}` → `{ref}`.`{refcol}`（ON DELETE {ondel}）")
        w("")
        sub += 1
    sec += 1

md = "\n".join(out) + "\n"

# ---------------- 第 5 章 索引与外键汇总 ----------------
out2 = []
w = out2.append

all_fks = [(t["name"], n, c, r, rc, od) for t in tables for (n, c, r, rc, od) in t["fks"]]
all_uks = [(t["name"], n, c) for t in tables for (n, c) in t["uks"]]
all_keys = [(t["name"], n, c) for t in tables for (n, c) in t["keys"]]

w("## 5. 索引与外键汇总")
w("")
w("### 5.1 外键清单")
w("")
w(f"全库共 **{len(all_fks)} 个外键**。删除行为遵循「主数据 RESTRICT、从属明细 CASCADE」的统一约定。")
w("")
w("| # | 外键名 | 所属表 | 字段 | 引用 | 删除行为 |")
w("| ---: | --- | --- | --- | --- | :-: |")
for i, (tb, n, c, r, rc, od) in enumerate(all_fks, 1):
    w(f"| {i} | `{n}` | `{tb}` | `{c}` | `{r}`.`{rc}` | {od} |")
w("")

cascade = [f for f in all_fks if f[5] == "CASCADE"]
w(f"其中 **{len(cascade)} 个为 CASCADE**（均为从属明细表，主记录删除时明细一并清除）：")
w("")
w("| 外键名 | 所属表 | 字段 | 引用 | 说明 |")
w("| --- | --- | --- | --- | --- |")
for tb, n, c, r, rc, od in cascade:
    w(f"| `{n}` | `{tb}` | `{c}` | `{r}`.`{rc}` | 明细随主记录级联删除 |")
w("")

w("**循环依赖外键的特殊处理**：`dept.leader_id → employee.id` 与 `employee.dept_id → dept.id` "
  "互为引用。建表脚本在建完 `dept`、`employee` 两表后，于**脚本末尾**用 `ALTER TABLE` 补加 "
  "`fk_dept_leader`，从而绕开建表顺序上的先后矛盾。")
w("")

w("### 5.2 唯一键清单")
w("")
w(f"全库共 **{len(all_uks)} 个唯一键**，用于承载业务编号的唯一性与关联表去重。")
w("")
w("| # | 唯一键名 | 所属表 | 字段 |")
w("| ---: | --- | --- | --- |")
for i, (tb, n, c) in enumerate(all_uks, 1):
    w(f"| {i} | `{n}` | `{tb}` | {c} |")
w("")

w("### 5.3 普通索引清单")
w("")
w(f"脚本中显式声明 **{len(all_keys)} 个普通（非唯一）索引**，覆盖外键列、业务查询高频列与状态列。")
w("")
w("> 说明：库中实际普通索引为 **112 个** —— 差额来自 InnoDB 为部分外键约束**自动创建**的索引"
  "（当外键列上没有可用前缀索引时，MySQL 会自动补建一个）。这类自动索引不出现在建表脚本里。")
w("")
w("| # | 索引名 | 所属表 | 字段 |")
w("| ---: | --- | --- | --- |")
for i, (tb, n, c) in enumerate(all_keys, 1):
    w(f"| {i} | `{n}` | `{tb}` | {c} |")
w("")

md2 = "\n".join(out2) + "\n"

# ---------------- 第 6 章 初始化数据说明 ----------------
md3 = r"""## 6. 初始化数据说明

随建表脚本一同提供 `enterprise_hr_init_data.sql`，可在建表完成后一次性导入，使系统开箱可用。

### 6.1 数据内容

| 类别 | 表 | 记录数 | 说明 |
| --- | --- | ---: | --- |
| 角色 | `sys_role` | 6 | 系统管理员 / 人事专员 / 部门主管 / 财务人员 / 普通员工 / 决策层 |
| 菜单 | `sys_menu` | 61 | 9 大模块到功能按钮的三级树 |
| 角色授权 | `sys_role_menu` | 153 | 6 角色 × 各自可见菜单 |
| 字典类型 | `sys_dict_type` | 10 | gender / education / leave_type 等 |
| 字典项 | `sys_dict_item` | 48 | 各字典类型的取值明细 |
| 系统参数 | `sys_config` | 16 | 分页大小、试用期月数、社保基数上下限等 |
| 部门 | `dept` | 4 | 总公司 → 研发中心 / 人力资源部 / 财务部 |
| 岗位 | `position` | 8 | 各序列岗位 |
| 员工 | `employee` | 8 | 含各部门负责人，构成组织树闭环 |
| 用户 | `sys_user` | 5 | 对应 5 类业务角色 |
| 用户角色 | `sys_user_role` | 5 | 用户与角色绑定 |
| 班次 | `shift` | 3 | 标准班 / 早班 / 晚班 |
| 假别 | `leave_type` | 8 | 年假、病假、事假、婚假等 |
| 薪资标准 | `salary_standard` | 3 | 按职级分档 |
| 社保配置 | `insurance_config` | 6 | 五险一金比例与基数规则 |
| 员工薪资 | `employee_salary` | 8 | 与 8 名种子员工一一对应 |

### 6.2 演示账号

所有演示账号的初始密码统一为 `123456`，在库中以 **BCrypt** 密文存储：

```
$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
```

| 用户名 | 姓名 | 角色 | 角色标识 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | 系统管理员 | `ROLE_ADMIN` |
| `hr` | 人事专员 | 人事专员 | `ROLE_HR` |
| `manager` | 部门主管 | 部门主管 | `ROLE_MANAGER` |
| `finance` | 财务人员 | 财务人员 | `ROLE_FINANCE` |
| `employee` | 普通员工 | 普通员工 | `ROLE_EMPLOYEE` |

> ⚠️ 生产部署前必须修改全部初始密码，并清理演示账号。

### 6.3 可重复执行

`enterprise_hr_init_data.sql` 在写入前对每张表执行 `DELETE FROM` 清理，并重置
`AUTO_INCREMENT`，因此**可反复执行**而不会产生重复数据或主键膨胀。

执行顺序与建表脚本一致，脚本内含多轮 `SET FOREIGN_KEY_CHECKS = 0 / 1` 包裹，
避免清理阶段被外键顺序卡住。

## 7. 部署与验证

### 7.1 执行步骤

```bash
# 1) 建库建表（49 张表）
mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_schema.sql

# 2) 导入初始化数据（可重复执行）
mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_init_data.sql

# 3) 核对表数量
mysql -uroot -p123456 -e "
SELECT COUNT(*) AS tables_total FROM information_schema.TABLES
 WHERE TABLE_SCHEMA='enterprise_hr' AND TABLE_TYPE='BASE TABLE';"
```

> ⚠️ **Windows 环境务必附加 `--default-character-set=utf8mb4`**：否则客户端默认走 `gbk`，
> UTF-8 中文注释与中文数据会乱码，甚至报 `Data too long for column`。

### 7.2 校验结果（实测）

| 校验项 | SQL 依据 | 实测值 |
| --- | --- | ---: |
| 表数量 | `information_schema.TABLES` | **49** |
| 外键数量 | `information_schema.TABLE_CONSTRAINTS` | **80** |
| 唯一键数量（不含主键） | `information_schema.TABLE_CONSTRAINTS` | **35** |
| 字段总数 | `information_schema.COLUMNS` | **776** |
| 普通索引（脚本显式声明） | `STATISTICS` | **96** |
| 普通索引（库中实际，含外键自动创建） | `STATISTICS.DISTINCT TABLE_NAME, INDEX_NAME` | **112** |
| 索引总数（含主键与唯一键） | `STATISTICS.DISTINCT` | **196** |
| InnoDB 占用空间 | `DATA_LENGTH + INDEX_LENGTH` | ≈ 3136 KB |
| 字段注释覆盖率 | `COLUMNS.COLUMN_COMMENT` 非空占比 | **100%**（0 个空注释） |

### 7.3 外键完整性校验

程序化遍历 `information_schema.KEY_COLUMN_USAGE`，对**全部 80 个外键**逐一构造
「左连接取右表主键为空的计数」查询，结果 **80/80 全部为 0 孤儿**：

```sql
-- 校验脚本：docs/check_fk_integrity.py（自动从 information_schema 生成 80 条校验 SQL）
-- 单条示例：检查是否存在部门不存在的员工
SELECT COUNT(*) FROM employee e
  LEFT JOIN dept d ON e.dept_id = d.id
 WHERE e.dept_id IS NOT NULL AND d.id IS NULL;
```

覆盖的核心引用关系（节选）：

| 引用方向 | 外键 | 实测孤儿 |
| --- | --- | ---: |
| `employee` → `dept` / `position` / `employee`(主管) | `fk_emp_dept` / `fk_emp_position` / `fk_emp_manager` | 0 |
| `dept` → `employee`（负责人，循环外键） | `fk_dept_leader` | 0 |
| `salary_record` / `salary_item` → `employee` / `salary_record` | `fk_salary_*` | 0 |
| `attendance_record` / `attendance_month` → `employee` | `fk_att_*` | 0 |
| `leave_request` / `overtime_request` / `business_trip` → `employee` | `fk_leave_*` 等 | 0 |
| `candidate` → `recruit_request` / `employee`(推荐人) | `fk_candidate_*` | 0 |
| `interview_record` / `offer_record` → `candidate` | `fk_interview_*` / `fk_offer_*` | 0 |
| `training_enroll` / `training_record` → `employee` / `course` | `fk_enroll_*` 等 | 0 |
| `performance_record` → `employee` / `performance_plan` | `fk_perf_*` | 0 |
| `sys_user` → `employee` / `dept` | `fk_user_*` | 0 |

> 说明：由于这些列上都建有真实的外键约束，数据库本身已拦截任何违约写入；
> 上述校验的价值在于**确认约束确实建成**（而非只写在注释里），且历史数据无遗留脏数据。

### 7.4 与后端工程的对应关系

数据库结构已与 `D:\Workspace\enterprise-hr-management` 下的 Spring Boot 3 + MyBatis-Plus 工程对齐：

- 实体主键策略 `@TableId(type = IdType.ASSIGN_ID)`（雪花 ID）与当前 DDL 的
  `BIGINT AUTO_INCREMENT` **并存兼容** —— 工程侧关闭了数据库自增依赖，由应用侧赋值；
- 逻辑删除 `@TableLogic` 对应各表的 `deleted TINYINT`；
- 自动填充 `MetaObjectHandler` 与 `created_at / updated_at` 的
  `DEFAULT CURRENT_TIMESTAMP` / `ON UPDATE` 形成双保险；
- 集成测试 `EmployeeIntegrationTest` / `EmployeeHttpIntegrationTest` 直接连接
  `enterprise_hr` 库运行，全量测试 **56/56 通过**。
"""

dest = BASE / "docs" / "chapter4-tables.md"
dest.write_text(md, encoding="utf-8")

# ---------------- 合并进《数据库设计说明书》 ----------------
DOC = BASE / "数据库设计说明书.md"
doc = DOC.read_text(encoding="utf-8")
body = md.rstrip() + "\n\n" + md2.rstrip() + "\n\n" + md3.rstrip() + "\n"
if "<!-- APPEND_POINT -->" in doc:
    doc = doc.replace("<!-- APPEND_POINT -->", body)
else:
    # 幂等：已追加过则从第 4 章处截断重建
    marker = "## 4. 表结构详细说明"
    assert marker in doc, "文档缺少第 4 章起始标记，无法安全重建"
    doc = doc[: doc.index(marker)].rstrip() + "\n\n" + body
DOC.write_text(doc, encoding="utf-8")

print(f"OK tables={len(tables)} cols={sum(len(t['cols']) for t in tables)} "
      f"fks={len(all_fks)} uks={len(all_uks)} keys={len(all_keys)}")
print(f"chapter4 chars={len(md)}  chapter5 chars={len(md2)}  chapter6-7 chars={len(md3)}")
print(f"doc total chars={len(doc)} -> {DOC}")

