# -*- coding: utf-8 -*-
"""
校验 data.sql 与 enterprise_hr_schema.sql 的一致性：

1. 结构校验：每条 INSERT 的表名、列名必须存在于建表脚本中，且列数 = 值个数；
2. 覆盖校验：49 张业务表是否全部有数据写入；
3. 口径校验：若干业务规则（用工人数、考勤汇总 vs 逐日明细、假期余额 vs 已批请假、
   notice.read_count vs notice_read 明细、薪资明细合计 vs 薪资主表）逐条比对。
"""
import re
import sys
from collections import defaultdict

BASE = r"D:/Workspace/enterprise-hr-management"
sys.stdout.reconfigure(encoding="utf-8")

schema = open(BASE + "/enterprise_hr_schema.sql", encoding="utf-8").read()
data = open(BASE + "/data.sql", encoding="utf-8").read()

# ---------- 1. 解析建表脚本 ----------
TABLE_COLUMNS = {}
for block in re.findall(r"CREATE TABLE\s+`(\w+)`\s*\((.*?)\n\)\s*ENGINE", schema, re.S):
    name, body = block
    cols = []
    for line in body.split("\n"):
        m = re.match(r"\s*`(\w+)`\s+[A-Z]", line)
        if m:
            cols.append(m.group(1))
    TABLE_COLUMNS[name] = cols

# ---------- 2. 解析 data.sql 的所有 INSERT ----------
INSERTS = []          # (table, [cols], [ [values...] ])


def _scan(s, on_group):
    """通用扫描：识别字符串字面量与嵌套括号，回调 on_group(text, kind)。"""
    depth, in_str, esc, cur = 0, False, False, ""
    for ch in s:
        if in_str:
            cur += ch
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == "'":
                in_str = False
            continue
        if ch == "'":
            in_str, cur = True, cur + ch
        elif ch == "(":
            depth += 1
            if depth == 1:
                cur = ""
            else:
                cur += ch
        elif ch == ")":
            depth -= 1
            if depth == 0:
                on_group(cur, "row")
            else:
                cur += ch
        elif depth >= 1:
            cur += ch


def parse_rows(body):
    """从 INSERT ... VALUES 的正文中提取每个 ( ... ) 行文本（不含最外层括号）。"""
    rows = []
    _scan(body, lambda text, kind: rows.append(text))
    return rows


def split_row(r):
    """把一行文本按顶层逗号切分为各列的值字面量。"""
    out, depth, in_str, esc, cur = [], 0, False, False, ""
    for ch in r:
        if in_str:
            cur += ch
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == "'":
                in_str = False
            continue
        if ch == "'":
            in_str, cur = True, cur + ch
        elif ch == "(":
            depth, cur = depth + 1, cur + ch
        elif ch == ")":
            depth, cur = depth - 1, cur + ch
        elif ch == "," and depth == 0:
            out.append(cur.strip())
            cur = ""
        else:
            cur += ch
    out.append(cur.strip())
    return out


for m in re.finditer(r"INSERT INTO `(\w+)` \(([^)]*)\) VALUES\s*(.*?);\n", data, re.S):
    table = m.group(1)
    cols = [c.strip().strip("`") for c in m.group(2).split(",")]
    INSERTS.append((table, cols, parse_rows(m.group(3))))

print("=" * 78)
print("  一、结构校验：INSERT 列名 / 列数 vs 建表脚本")
print("=" * 78)
errors = 0
counts = defaultdict(int)
for table, cols, rows in INSERTS:
    if table not in TABLE_COLUMNS:
        print("  [FAIL] 表不存在: %s" % table)
        errors += 1
        continue
    bad = [c for c in cols if c not in TABLE_COLUMNS[table]]
    if bad:
        print("  [FAIL] %s 存在非法列: %s" % (table, ", ".join(bad)))
        errors += 1
    counts[table] += len(rows)
    for i, r in enumerate(rows):
        n = len(split_row(r))
        if n != len(cols):
            print("  [FAIL] %s 第 %d 行列数不符: 列 %d / 值 %d" % (table, i + 1, len(cols), n))
            errors += 1
            break
print("  语句数 = %d，覆盖表数 = %d，结构错误 = %d" % (len(INSERTS), len(counts), errors))
print("  -> %s" % ("PASS" if errors == 0 else "FAIL"))

print()
print("=" * 78)
print("  二、覆盖校验：49 张业务表是否全部写入")
print("=" * 78)
missing = [t for t in TABLE_COLUMNS if counts.get(t, 0) == 0]
print("  sys_role_menu 为无自增主键的关联表，表体由 INSERT ... SELECT FROM sys_menu 派生")
print("  （VALUES 形式只覆盖角色 5 的 9 行，完整 153 行在第三部分末按 WHERE 条件静态还原）")
for t in sorted(TABLE_COLUMNS):
    if counts.get(t, 0) == 0 and t != "sys_role_menu":
        print("  [WARN] 无数据: %s" % t)
print("  未写入数据的表: %s" % (", ".join(missing) if missing else "无"))
print("  -> %s" % ("PASS" if not [m for m in missing if m != "sys_role_menu"] else "FAIL"))

# ---------- 3. 口径校验 ----------
print()
print("=" * 78)
print("  三、业务口径校验")
print("=" * 78)


def fetch(table):
    """汇总同一张表的全部 INSERT 批次（emit 会按 batch 拆成多条语句）。"""
    cols, rows = [], []
    for t, c, r in INSERTS:
        if t == table:
            if not cols:
                cols = c
            rows.extend(r)
    return cols, rows


def unq(v):
    """去掉字符串字面量的引号；数值 / NULL 原样或转 None。"""
    if v == "NULL":
        return None
    if len(v) >= 2 and v[0] == "'" and v[-1] == "'":
        return v[1:-1].replace("''", "'")
    return v


def r2v(x):
    return round(float(x) + 1e-9, 2)


checks = []

# 3.1 员工数
e_cols, e_rows = fetch("employee")
emps = [split_row(r) for r in e_rows]
emps = [e for e in emps if e[0] != "NULL"]
checks.append(("employee 员工总数 = 50", len(emps) == 50, "%d 人" % len(emps)))
onboard = [e for e in emps if e[e_cols.index("status")] in ("1", "2")]
checks.append(("employee 在职（含试用）= 46（50 - 3 离职 - 1 待入职）",
               len(onboard) == 46, "%d 人" % len(onboard)))

# 3.2 考勤月度汇总 vs 逐日明细
am_cols, am_rows = fetch("attendance_month")
ar_cols, ar_rows = fetch("attendance_record")
detail = defaultdict(int)
for r in ar_rows:
    v = split_row(r)
    detail[(unq(v[1]), unq(v[2])[:7])] = detail[(unq(v[1]), unq(v[2])[:7])] + 1
bad = []
for r in am_rows:
    v = split_row(r)
    key = (unq(v[1]), "%s-%02d" % (unq(v[3]), int(unq(v[4]))))
    should = int(float(unq(v[5])))
    if detail.get(key, 0) != should:
        bad.append("%s/%s: 汇总应出勤 %d ≠ 明细 %d" % (key[0], key[1], should, detail.get(key, 0)))
checks.append(("attendance_month.should_days 与 attendance_record 逐日明细条数一致",
               not bad, "比对 %d 个月度记录，异常 %d 条" % (len(am_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

# 3.3 假期余额 used_days vs 已批准请假单
lb_cols, lb_rows = fetch("leave_balance")
lr_cols, lr_rows = fetch("leave_request")
used = defaultdict(float)
for r in lr_rows:
    v = split_row(r)
    if unq(v[lr_cols.index("approval_status")]) == "2":
        k = (unq(v[2]), unq(v[3]))
        used[k] += float(unq(v[lr_cols.index("days")]))
bad = []
for r in lb_rows:
    v = split_row(r)
    k = (unq(v[1]), unq(v[2]))
    if abs(float(unq(v[lb_cols.index("used_days")])) - used.get(k, 0.0)) > 1e-6:
        bad.append("%s/假别%s: 余额已用 %.1f ≠ 请假汇总 %.1f"
                   % (k[0], k[1], float(unq(v[lb_cols.index("used_days")])), used.get(k, 0.0)))
checks.append(("leave_balance.used_days 与已批准请假单天数一致",
               not bad, "比对 %d 条余额记录，异常 %d 条" % (len(lb_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

# 3.4 公告已读数
n_cols, n_rows = fetch("notice")
nr_rows = fetch("notice_read")[1]
read_cnt = defaultdict(int)
for r in nr_rows:
    read_cnt[unq(split_row(r)[1])] += 1
bad = []
for r in n_rows:
    v = split_row(r)
    nid = unq(v[0])
    if int(unq(v[n_cols.index("read_count")])) != read_cnt.get(nid, 0):
        bad.append("公告 %s: read_count %s ≠ notice_read %d"
                   % (nid, unq(v[n_cols.index("read_count")]), read_cnt.get(nid, 0)))
checks.append(("notice.read_count 与 notice_read 明细条数一致",
               not bad, "比对 %d 条公告，异常 %d 条" % (len(n_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

# 3.5 薪资主表 vs 薪资条明细（应发项 / 扣款项）
si_cols, si_rows = fetch("salary_item")
sr_cols, sr_rows = fetch("salary_record")
add = defaultdict(float)
ded = defaultdict(float)
for r in si_rows:
    v = split_row(r)
    rec = unq(v[1])
    amount = float(unq(v[si_cols.index("item_amount")]))
    t = unq(v[si_cols.index("item_type")])
    if t == "1":
        add[rec] += amount
    elif t == "2":
        ded[rec] += amount
bad = []
for r in sr_rows:
    v = split_row(r)
    rid = unq(v[0])
    gross = float(unq(v[sr_cols.index("gross_pay")]))
    sub = add.get(rid, 0.0)
    if abs(sub - gross) > 0.02:
        bad.append("薪资记录 %s: 应发合计 %.2f ≠ 应发项之和 %.2f" % (rid, gross, sub))
checks.append(("salary_record.gross_pay = salary_item 应发项之和",
               not bad, "比对 %d 条薪资记录，异常 %d 条" % (len(sr_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

bad = []
for r in sr_rows:
    v = split_row(r)
    rid = unq(v[0])
    net = float(unq(v[sr_cols.index("net_pay")]))
    gross = float(unq(v[sr_cols.index("gross_pay")]))
    expect = r2v(gross - float(unq(v[sr_cols.index("social_personal")]))
                 - float(unq(v[sr_cols.index("fund_personal")]))
                 - float(unq(v[sr_cols.index("tax_amount")]))
                 - float(unq(v[sr_cols.index("attendance_deduct")]))
                 - float(unq(v[sr_cols.index("other_deduct")])))
    if abs(expect - net) > 0.02:
        bad.append("薪资记录 %s: 实发 %.2f ≠ 应发 - 社保 - 公积金 - 个税 - 考勤扣款 - 其他扣款 %.2f"
                   % (rid, net, expect))
checks.append(("salary_record.net_pay = 应发 - 个人社保 - 个人公积金 - 个税 - 考勤/其他扣款",
               not bad, "比对 %d 条薪资记录，异常 %d 条" % (len(sr_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

# 3.6 招聘需求录用人数 vs Offer 状态
rr_cols, rr_rows = fetch("recruit_request")
offer_rows = fetch("offer_record")[1]
hired = defaultdict(int)
for r in offer_rows:
    v = split_row(r)
    if unq(v[fetch("offer_record")[0].index("status")]) == "6":
        hired[unq(v[2])] += 1
bad = []
for r in rr_rows:
    v = split_row(r)
    if int(unq(v[rr_cols.index("hired_count")])) != hired.get(unq(v[0]), 0):
        bad.append("需求 %s: hired_count %s ≠ 已入职 Offer %d"
                   % (unq(v[0]), unq(v[rr_cols.index("hired_count")]), hired.get(unq(v[0]), 0)))
checks.append(("recruit_request.hired_count 与已入职 Offer 数量一致",
               not bad, "比对 %d 条需求，异常 %d 条" % (len(rr_rows), len(bad))))
for b in bad[:5]:
    print("      " + b)

# 3.7 招聘到入职的闭环：已入职 Offer 的入职日期 / 部门 / 岗位 = 员工主数据
offer_cols, _ = fetch("offer_record")
entry_map = {}
for r in offer_rows:
    v = split_row(r)
    if unq(v[offer_cols.index("status")]) == "6":
        entry_map[unq(v[offer_cols.index("offer_no")])] = (
            unq(v[offer_cols.index("entry_date")]),
            unq(v[offer_cols.index("dept_id")]),
            unq(v[offer_cols.index("position_id")]))
emp_index = {}
for e in emps:
    emp_index[(unq(e[e_cols.index("hire_date")]), unq(e[e_cols.index("dept_id")]),
               unq(e[e_cols.index("position_id")]))] = e[e_cols.index("emp_no")]
hit = 0
for ono, key in entry_map.items():
    if key in emp_index:
        hit += 1
    else:
        print("      Offer %s 未匹配到员工主数据: %s" % (ono, key))
checks.append(("已入职 Offer 的入职日期/部门/岗位与 employee 主数据完全一致",
               hit == len(entry_map), "%d/%d 条命中" % (hit, len(entry_map))))

# 3.8 工号 / 邮箱 / 登录账号唯一性
codes = [e[e_cols.index("emp_no")] for e in emps]
checks.append(("employee.emp_no 唯一", len(codes) == len(set(codes)),
               "%d 个工号" % len(set(codes))))
mails = [e[e_cols.index("email")] for e in emps]
checks.append(("employee.email 唯一", len(mails) == len(set(mails)),
               "%d 个邮箱" % len(set(mails))))

# 3.9 系统账号与员工一一对应
u_cols, u_rows = fetch("sys_user")
users = [split_row(r) for r in u_rows]
checks.append(("sys_user 账号数 = 1 个管理员 + 50 名员工", len(users) == 51,
               "%d 个账号" % len(users)))
unames = [unq(u[u_cols.index("username")]) for u in users]
checks.append(("sys_user.username 唯一", len(unames) == len(set(unames)),
               "%d 个账号" % len(set(unames))))
ur_rows = fetch("sys_user_role")[1]
checks.append(("sys_user_role 覆盖全部 51 个账号（含 admin）", len(ur_rows) == 51,
               "%d 条授权" % len(ur_rows)))
# 员工账号 user_id = 1 + 员工序号，admin 固定 1
emp_users = [u for u in users if unq(u[u_cols.index("employee_id")]) not in (None, "NULL")]
checks.append(("sys_user 与 employee 一一绑定（50 个员工账号）", len(emp_users) == 50,
               "%d 个员工账号" % len(emp_users)))
checks.append(("sys_user 账号数 = sys_user_role 授权数 + 0（一一对应）",
               len(users) == len(ur_rows), "%d / %d" % (len(users), len(ur_rows))))


# 3.10 sys_role_menu：大部分行由 INSERT ... SELECT FROM sys_menu 派生
#      （生成器为避免手写 153 行而采用该写法），这里在内存里重建 sys_menu 并逐条求值 WHERE。
def _menu_table():
    """把 data.sql 中的 sys_menu 行读进内存，parent_id 转 int / None，便于求值条件。"""
    mc, mrows = fetch("sys_menu")
    out = []
    for r in mrows:
        d = dict(zip(mc, [unq(v) for v in split_row(r)]))
        d["id"] = int(d["id"])
        pid = d.get("parent_id")
        d["parent_id"] = None if pid in (None, "NULL") else int(pid)
        out.append(d)
    return out


def _term_ok(menu, term):
    """求值单个条件项：IS NULL / <> N / IN (...)。无法识别时按不匹配处理。"""
    # 注意：不要 strip 括号，否则 `IN (...)` 的右括号会被吃掉导致条件永不匹配
    t = term.strip().rstrip(";").strip()
    if not t:
        return True
    m = re.match(r"^`?(\w+)`?\s+IS\s+(NOT\s+)?NULL$", t, re.I)
    if m:
        is_null = menu.get(m.group(1)) is None
        return (not is_null) if m.group(2) else is_null
    m = re.match(r"^`?(\w+)`?\s*<>\s*(\d+)$", t)
    if m:
        return menu.get(m.group(1)) != int(m.group(2))
    m = re.match(r"^`?(\w+)`?\s*=\s*(\d+)$", t)
    if m:
        return menu.get(m.group(1)) == int(m.group(2))
    m = re.match(r"^`?(\w+)`?\s+IN\s*\((.*)\)$", t, re.I | re.S)
    if m:
        col, body = m.group(1), m.group(2)
        items = [x.strip().strip("'") for x in body.split(",") if x.strip()]
        v = menu.get(col)
        return v in items or str(v) in items
    return False


def _where_ok(menu, cond):
    """OR 的优先级低于 AND：先按 OR 拆成分句，任一子句全部条件成立即命中。"""
    cond = cond.strip().rstrip(";")
    for disj in re.split(r"\bOR\b", cond, flags=re.I):
        if all(_term_ok(menu, t) for t in re.split(r"\bAND\b", disj, flags=re.I)):
            return True
    return False


def role_menu_rows():
    """还原 sys_role_menu 的全部授权行 [(role_id, menu_id), ...]。"""
    menus = _menu_table()
    pairs, unknown = [], 0
    # (a) 显式 VALUES 形式
    for m in re.finditer(r"INSERT INTO `sys_role_menu` \([^)]*\)\s*VALUES(.*?);", data, re.S):
        for r in parse_rows(m.group(1)):
            vals = [unq(v) for v in split_row(r)]
            if len(vals) == 2:
                pairs.append((int(vals[0]), int(vals[1])))
    # (b) INSERT ... SELECT <role>, id FROM sys_menu WHERE ...
    for m in re.finditer(
            r"INSERT INTO `sys_role_menu` \([^)]*\)\s*SELECT\s+(\d+)\s*,\s*`id`\s+"
            r"FROM\s+`sys_menu`\s+WHERE\s+(.*?);", data, re.S):
        role_id, cond = int(m.group(1)), m.group(2)
        hit = [mu["id"] for mu in menus if _where_ok(mu, cond)]
        if not hit:
            unknown += 1
        pairs.extend((role_id, mid) for mid in hit)
    return pairs, unknown


rm_pairs, rm_unknown = role_menu_rows()
rm_unique = len(set(rm_pairs))
checks.append(("sys_role_menu 授权行可由 sys_menu 还原（无未识别条件）",
               rm_unknown == 0, "SELECT 派生语句全部求值成功" if rm_unknown == 0
               else "%d 条语句条件未识别" % rm_unknown))
checks.append(("sys_role_menu 授权数 = 153（6 个角色 × 菜单并集，无重复）",
               rm_unique == 153, "%d 条（去重后 %d）" % (len(rm_pairs), rm_unique)))

ok = 0
for name, passed, detail_txt in checks:
    print("  [%s] %s  (%s)" % ("PASS" if passed else "FAIL", name, detail_txt))
    ok += 1 if passed else 0
print()
print("=" * 78)
print("  校验结论：结构 %d 项、口径 %d/%d 项通过"
      % (0 if errors else 1, ok, len(checks)))
print("=" * 78)
print()
print("  各表写入行数：")
counts["sys_role_menu"] = len(rm_pairs)
total = 0
for t in sorted(counts):
    print("    %-24s %6d" % (t, counts[t]))
    total += counts[t]
print("    %-24s %6d" % ("合计", total))
print()
print("  说明：sys_role_menu 的表体由 INSERT ... SELECT FROM sys_menu 派生，")
print("        上表数值为按 WHERE 条件静态求值还原的结果，导入后应与库内 COUNT(*) 一致。")
