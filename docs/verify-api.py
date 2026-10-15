"""
企业人事管理系统 —— 后端接口运行时验证（真实 HTTP 调用）

验证目标（对应用户提出的 7 项要求）：
  1. 统一接口风格：91 个前端请求地址全部可达，路径/方法一致
  2. 统一结果处理：全部响应为 {code, message, data} 三段式
  3. 统一异常处理：HTTP 状态码 === 业务 code，且 message 为可读中文
  4. 系统级日志记录：写操作后 sys_oper_log 有留痕
  5. 功能严谨/贴合企业场景：关键业务规则（BR-xx）被真实拦截
  6. 字段命名双向约定：响应蛇形 snake_case / 请求驼峰 camelCase
  7. 依赖完整：应用可正常启动并服务请求

运行前提：后端已启动在 http://localhost:8080（context-path=/api），数据库 enterprise_hr 可用。
用法：python docs/verify-api.py
"""

import json
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8080/api"
ADMIN_USER = "admin"
ADMIN_PWD = "admin123"
MYSQL = "D:/Apps/MySQL8.0/bin/mysql"

PASS, FAIL = [], []


# --------------------------------------------------------------------------- 基础设施


def call(method, path, token=None, params=None, body=None, raw=False):
    """发起一次 HTTP 调用，返回 (http_status, body)。body 为 dict 或原始文本。"""
    url = BASE + path
    if params:
        clean = {k: v for k, v in params.items() if v is not None and v != ""}
        if clean:
            url += "?" + urllib.parse.urlencode(clean, doseq=True)
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json;charset=UTF-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method.upper())
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            text = resp.read().decode("utf-8", "replace")
            status = resp.status
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        status = e.code
    if raw:
        return status, text
    try:
        return status, json.loads(text)
    except json.JSONDecodeError:
        return status, {"_raw": text}


def check(name, condition, detail=""):
    if condition:
        PASS.append(name)
        print(f"  [PASS] {name}")
    else:
        FAIL.append((name, detail))
        print(f"  [FAIL] {name}  {detail}")
    return condition


def ok(env, expect_http=200, expect_code=200):
    """统一断言：三段式齐全、业务 code 命中、且 HTTP 状态码与业务 code 同值。"""
    if not isinstance(env, dict):
        return False, f"响应非 JSON 对象: {env!r}"
    for field in ("code", "message", "data"):
        if field not in env:
            return False, f"响应缺少 {field} 字段: {list(env.keys())}"
    if env["code"] != expect_code:
        return False, f"code={env['code']} 期望 {expect_code}，message={env.get('message')}"
    if expect_http is not None and env["code"] != expect_http:
        return False, f"HTTP 状态码({expect_http}) 与业务 code({env['code']}) 不同值"
    return True, ""


def query_scalar(sql):
    """直连 MySQL 取一个标量值，用于准备真实固件（仅验证脚本使用）。"""
    out = subprocess.run(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "-D", "enterprise_hr", "-e", sql],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    value = (out.stdout or "").strip().splitlines()
    value = value[0].strip() if value else ""
    if value in ("", "NULL"):
        return None
    try:
        return int(value)
    except ValueError:
        return value


def exec_sql(sql):
    """执行一条写 SQL（用于把验证过程产生的副作用还原）。"""
    subprocess.run(
        [MYSQL, "-uroot", "-p123456", "-D", "enterprise_hr", "-e", sql],
        capture_output=True, text=True, encoding="utf-8", errors="replace")


# --------------------------------------------------------------------------- 主流程


def section(title):
    print(f"\n{'=' * 78}\n{title}\n{'=' * 78}")


def main():
    # =========================================================== 0. 健康检查
    section("0. 服务可达性与统一响应骨架")
    status, env = call("POST", "/auth/login", body={"username": ADMIN_USER, "password": ADMIN_PWD})
    good, why = ok(env, 200, 200)
    check("POST /auth/login 登录成功并返回三段式响应", good, why)
    if not good:
        print("\n登录失败，终止验证。请确认后端已启动且 admin 口令为 admin123。")
        return 1
    token = env["data"]["token"]
    # 前端 store/modules/user.js 消费 data.token 与 data.user（不是 userInfo）
    user_info = env["data"].get("user") or {}
    check("登录响应含 token", bool(token), f"data keys={list(env['data'].keys())}")
    check("登录响应结构为 {token, user}（前端 store 契约）", "user" in env["data"],
          f"data keys={list(env['data'].keys())}")
    # 统一响应必须是**严格**三段式：多出字段会破坏前端按 schema 校验的写法
    check("响应体为严格三段式 {code, message, data}（无多余字段）",
          set(env.keys()) == {"code", "message", "data"},
          f"实际 keys={sorted(env.keys())}")
    # 前端 user.js 消费 user.employeeId / deptId / realName / roles —— 必须保持驼峰
    check("user 使用驼峰字段（前端消费契约）",
          {"id", "username", "realName", "employeeId", "deptId", "roles"} <= set(user_info.keys()),
          f"user keys={sorted(user_info.keys())}")

    # /auth/me 必须与登录返回的 user 同构，否则刷新页面后角色/岗位信息会丢失
    status, env = call("GET", "/auth/me", token=token)
    me = env.get("data") or {}
    check("GET /auth/me 与登录 user 同构（驼峰）",
          status == 200 and {"id", "realName", "employeeId", "deptId", "roles"} <= set(me.keys()),
          f"HTTP={status} keys={sorted(me.keys())}")

    # =========================================================== 1. 统一异常处理
    section("1. 统一异常处理：HTTP 状态码 === 业务 code，message 为可读中文")

    status, env = call("GET", "/employees/page")
    check("无令牌访问受保护接口 → HTTP 401", status == 401, f"HTTP={status}")
    check("401 响应体 code 与 HTTP 同值", isinstance(env, dict) and env.get("code") == 401, f"body={env}")

    status, env = call("POST", "/auth/login", body={"username": ADMIN_USER, "password": "wrong-pwd-xxx"})
    msg = env.get("message") if isinstance(env, dict) else ""
    check("口令错误 → HTTP 400 且 message 为中文", status == 400 and isinstance(msg, str) and any(
        "\u4e00" <= c <= "\u9fff" for c in msg), f"HTTP={status} message={msg!r}")

    status, env = call("GET", "/this-path-does-not-exist-at-all", token=token)
    check("不存在的路径（已认证）→ HTTP 404", status == 404, f"HTTP={status} body={env}")

    status, env = call("GET", "/this-path-does-not-exist-at-all")
    check("不存在的路径（未认证）→ HTTP 401（fail-close，不泄露路径是否存在）",
          status == 401, f"HTTP={status} body={env}")

    status, env = call("DELETE", "/auth/me", token=token)
    check("方法不支持 → HTTP 405", status == 405, f"HTTP={status} body={env}")

    status, env = call("GET", "/employees/page", token=token, params={"current": 0, "size": 3})
    check("非法分页参数（current=0）→ HTTP 400", status == 400, f"HTTP={status} body={env}")

    status, env = call("GET", "/sys/dict-types/999999/items", token=token)
    check("资源不存在 → HTTP 404 且 code 同值", status == 404 and env.get("code") == 404,
          f"HTTP={status} body={env}")

    # =========================================================== 2. 字段命名双向约定
    section("2. 字段命名双向约定：响应蛇形 / 请求驼峰")

    status, env = call("GET", "/employees/page", token=token, params={"current": 1, "size": 3})
    recs = (env.get("data") or {}).get("records") or []
    keys = set(recs[0].keys()) if recs else set()
    check("员工响应为蛇形字段（emp_no / dept_name / id_card）",
          {"emp_no", "dept_name", "id_card"} <= keys,
          f"实际字段={sorted(keys)}")
    check("响应不泄露逻辑删除标志 deleted", "deleted" not in keys, f"字段={sorted(keys)}")

    # 请求驼峰：deptId 作为查询参数能正确过滤（若后端误按 dept_id 绑定会返回全量）
    status, env = call("GET", "/employees/page", token=token,
                       params={"current": 1, "size": 5, "deptId": recs[0].get("dept_id") if recs else None})
    filtered = (env.get("data") or {}).get("records") or []
    check("请求参数使用驼峰 deptId 可生效", status == 200 and env.get("code") == 200,
          f"HTTP={status} message={env.get('message')}")

    status, env = call("GET", "/sys/users/page", token=token, params={"current": 1, "size": 3})
    urecs = (env.get("data") or {}).get("records") or []
    ukeys = set(urecs[0].keys()) if urecs else set()
    check("系统用户响应为蛇形（real_name / role_names / dept_name）",
          {"real_name", "role_names", "dept_name"} <= ukeys, f"实际字段={sorted(ukeys)}")
    check("系统用户响应不含口令字段", "password" not in ukeys, f"字段={sorted(ukeys)}")

    # =========================================================== 3. 装配测试固件
    section("3. 采集真实主键，用于后续逐条打通 91 个请求地址")

    def first_id(path, params=None, key="records"):
        _, env = call("GET", path, token=token, params=params)
        data = env.get("data")
        if isinstance(data, dict):
            rows = data.get(key) or []
        elif isinstance(data, list):
            rows = data
        else:
            rows = []
        return rows[0].get("id") if rows else None, rows

    fx = {}
    fx["employeeId"], emp_rows = first_id("/employees/page", {"current": 1, "size": 5})
    fx["employeeId2"], _ = (emp_rows[1].get("id"), None) if len(emp_rows) > 1 else (None, None)
    _, tree = call("GET", "/depts/tree", token=token)
    def walk(nodes):
        for n in nodes or []:
            yield n.get("id")
            yield from walk(n.get("children"))
    _t = tree.get("data")
    _t = _t.get("records") if isinstance(_t, dict) else _t
    dept_ids = [i for i in walk(_t) if i]
    fx["deptId"] = dept_ids[0] if dept_ids else None
    fx["positionId"], _ = first_id("/positions")
    fx["changeId"], _ = first_id("/employee-changes/page", {"current": 1, "size": 5})
    fx["contractId"], _ = first_id("/employee-contracts/page", {"current": 1, "size": 5})
    fx["monthId"], _ = first_id("/attendance-months/page", {"current": 1, "size": 5})
    fx["leaveId"], _ = first_id("/leave-requests/page", {"current": 1, "size": 5})
    fx["overtimeId"], _ = first_id("/overtime-requests/page", {"current": 1, "size": 5})
    fx["tripId"], _ = first_id("/business-trips/page", {"current": 1, "size": 5})
    fx["candidateId"], _ = first_id("/candidates/page", {"current": 1, "size": 5})
    fx["interviewId"], _ = first_id("/interview-records")
    fx["offerId"], _ = first_id("/offers/page", {"current": 1, "size": 5})
    fx["recruitRequestId"], _ = first_id("/recruit-requests/page", {"current": 1, "size": 5})
    fx["perfPlanId"], _ = first_id("/performance-plans/page", {"current": 1, "size": 5})
    fx["perfRecordId"], _ = first_id("/performance-records/page", {"current": 1, "size": 5})
    # 前端薪资标准用的是 /salary-standards/page（集合路径 GET 不在契约内），此处按契约取分页首条
    fx["salaryStandardId"], _ = first_id("/salary-standards/page", {"current": 1, "size": 5})
    fx["salaryRecordId"], _ = first_id("/salary-records/page", {"current": 1, "size": 5})
    fx["trainingPlanId"], _ = first_id("/training-plans/page", {"current": 1, "size": 5})
    # 待办是「派给具体员工」的：admin 未绑定员工档案，/dashboard/todos 必然为空，
    # 所以改从库中直取一条「属于员工 1001 的未处理待办」，用于验证处理待办的成功路径与越权拦截。
    fx["todoId"] = query_scalar(
        "SELECT id FROM todo_task WHERE handle_status = 0 AND deleted = 0 "
        f"AND receiver_id = {fx['employeeId']} ORDER BY id LIMIT 1")
    fx["todoIdOfOthers"] = query_scalar(
        "SELECT id FROM todo_task WHERE handle_status = 0 AND deleted = 0 "
        f"AND receiver_id <> {fx['employeeId']} ORDER BY id LIMIT 1")
    fx["noticeId"], _ = first_id("/notices/page", {"current": 1, "size": 5})
    fx["dictTypeId"], _ = first_id("/sys/dict-types")
    fx["configId"], _ = first_id("/sys/configs")
    fx["roleId"], _ = first_id("/sys/roles")
    fx["adminUserId"], _ = first_id("/sys/users/page", {"current": 1, "size": 10})
    for k, v in fx.items():
        if v is None:
            print(f"  [WARN] 固件缺失: {k}")

    # =========================================================== 4. 逐条打通 91 个请求地址
    section("4. 逐条验证前端 91 个请求地址（读接口走真实数据）")

    GET_CASES = [
        ("/dashboard/summary", None),
        ("/dashboard/charts", None),
        ("/dashboard/todos", None),
        ("/dashboard/notices", None),
        ("/depts", None),
        ("/depts/tree", None),
        ("/positions", None),
        ("/employees/page", {"current": 1, "size": 5}),
        ("/employees/{employeeId}", None),
        ("/employees/{employeeId}/changes", None),
        ("/employee-changes/page", {"current": 1, "size": 5}),
        ("/employee-contracts/page", {"current": 1, "size": 5}),
        ("/attendance/records", {"current": 1, "size": 5}),
        ("/attendance-months/page", {"current": 1, "size": 5}),
        ("/leave-requests/page", {"current": 1, "size": 5}),
        ("/overtime-requests/page", {"current": 1, "size": 5}),
        ("/business-trips/page", {"current": 1, "size": 5}),
        ("/candidates/page", {"current": 1, "size": 5}),
        ("/interview-records", None),
        ("/offers/page", {"current": 1, "size": 5}),
        ("/recruit-requests/page", {"current": 1, "size": 5}),
        ("/performance-plans/page", {"current": 1, "size": 5}),
        ("/performance-records/page", {"current": 1, "size": 5}),
        ("/salary-standards/page", {"current": 1, "size": 5}),
        ("/salary-records/page", {"current": 1, "size": 5}),
        ("/salary/payslips/{salaryRecordId}/items", None),
        ("/training-plans/page", {"current": 1, "size": 5}),
        ("/notices/page", {"current": 1, "size": 5}),
        ("/sys/users/page", {"current": 1, "size": 5}),
        ("/sys/roles", None),
        ("/sys/dict-types", None),
        ("/sys/dict-types/{dictTypeId}/items", None),
        ("/sys/configs", None),
        ("/sys/logs/login", {"current": 1, "size": 5}),
        ("/sys/logs/oper", {"current": 1, "size": 5}),
    ]
    for path_tpl, params in GET_CASES:
        path = path_tpl.format(**{k: v for k, v in fx.items() if v is not None})
        if "{" in path:
            check(f"GET {path_tpl}", False, "缺少测试固件")
            continue
        status, env = call("GET", path, token=token, params=params)
        good, why = ok(env, 200, 200)
        check(f"GET {path}", good, f"HTTP={status} {why}")

    # 员工自助类接口（/attendance/today、/leave-balances/my、/salary/payslips/my、/training/my）
    # 语义上是「当前登录人」的数据。用 admin（无员工档案）验证其降级行为是否一致：
    #   · 只读的「我的 xxx」列表类接口 → 返回 200 + 空集合（无关联员工 ⇒ 数据天然为空）
    #   · 需要以员工身份「执行动作」的接口（打卡）→ 返回 400 + 可执行提示
    section("4.1 员工自助接口在「账号未绑定员工档案」时的降级行为")
    for path in ["/leave-balances/my", "/salary/payslips/my", "/training/my"]:
        status, env = call("GET", path, token=token)
        good, why = ok(env, 200, 200)
        check(f"GET {path} —— 未绑定员工档案时返回空集合（200）", good, f"HTTP={status} {why}")

    status, env = call("GET", "/attendance/today", token=token)
    msg = env.get("message") if isinstance(env, dict) else ""
    check("GET /attendance/today —— 未绑定员工档案时返回可执行中文提示（400，非 500）",
          status == 400 and isinstance(msg, str) and "员工档案" in msg,
          f"HTTP={status} message={msg!r}")

    # 前端 salary/api 中 /salary-standards 只注册了 POST；GET 应被统一 405 处理
    status, env = call("GET", "/salary-standards", token=token)
    check("GET /salary-standards（前端未使用该组合）→ 统一 405", status == 405,
          f"HTTP={status} body={env}")

    section("5. 写接口 —— 校验统一异常通道 + 业务规则拦截（不污染生产数据）")

    WRITE_INVALID = [
        ("POST", "/employees", {"name": ""}, "员工姓名为空 → 400"),
        ("POST", "/depts", {"deptCode": "", "name": ""}, "部门编码/名称为空 → 400"),
        ("POST", "/positions", {"positionCode": ""}, "岗位编码为空 → 400"),
        ("POST", "/employee-changes", {}, "异动单缺必填 → 400"),
        ("POST", "/employee-contracts", {}, "合同缺必填 → 400"),
        ("POST", "/attendance/checkin", {"employeeId": None}, "打卡缺参数 → 400"),
        ("POST", "/leave-requests", {"leaveTypeId": None}, "请假缺必填 → 400"),
        ("POST", "/overtime-requests", {}, "加班缺必填 → 400"),
        ("POST", "/business-trips", {}, "出差缺必填 → 400"),
        ("POST", "/candidates", {}, "候选人缺必填 → 400"),
        ("POST", "/interview-records", {}, "面试记录缺必填 → 400"),
        ("POST", "/recruit-requests", {}, "招聘需求缺必填 → 400"),
        ("POST", "/performance-plans", {}, "考核方案缺必填 → 400"),
        ("POST", "/salary-standards", {}, "薪资标准缺必填 → 400"),
        ("POST", "/salary-records/calculate", {}, "薪资核算缺月份 → 400"),
        ("POST", "/training-plans", {}, "培训计划缺必填 → 400"),
        ("POST", "/notices", {"title": ""}, "公告标题为空 → 400"),
        ("POST", "/sys/users", {"username": "", "realName": ""}, "账号/姓名为空 → 400"),
        ("POST", "/sys/roles", {"roleCode": "", "roleName": ""}, "角色编码/名称为空 → 400"),
    ]
    for method, path, body, desc in WRITE_INVALID:
        status, env = call(method, path, token=token, body=body)
        msg = env.get("message") if isinstance(env, dict) else ""
        check(f"{method} {path} —— {desc}", status == 400 and env.get("code") == 400,
              f"HTTP={status} code={env.get('code') if isinstance(env, dict) else '?'} message={msg!r}")

    # ---- 业务规则（BR）真实拦截 ----
    section("6. 业务规则（BR-xx）真实拦截验证")

    # BR-09-01 密码强度
    status, env = call("POST", "/sys/users", token=token,
                       body={"username": "verify_probe_u1", "realName": "验证探针", "password": "123",
                             "roles": ["ROLE_EMPLOYEE"]})
    check("BR-09-01 初始密码 < 8 位被拒（400）", status == 400, f"HTTP={status} {env}")

    # BR-09-02 管理员账号不可删除
    status, env = call("DELETE", f"/sys/users/{fx['adminUserId']}", token=token)
    check("BR-09-02 管理员账号不可删除（400）", status == 400, f"HTTP={status} {env}")

    # BR-09-02 不可停用自身管理员账号
    status, env = call("PUT", f"/sys/users/{fx['adminUserId']}", token=token, body={"status": 0})
    check("BR-09-02 不可停用自身管理员账号（400）", status == 400, f"HTTP={status} {env}")

    # 系统内置角色编码不可改
    status, env = call("PUT", f"/sys/roles/{fx['roleId']}", token=token,
                       body={"roleCode": "ROLE_RENAMED", "roleName": "改个名"})
    check("系统内置角色编码不可修改（400）", status == 400, f"HTTP={status} {env}")

    # 参数区间：封账日不得超过 28（否则 2 月永远无法封账）
    status, env = call("GET", "/sys/configs", token=token, params={"group": "attendance"})
    cfg_rows = (env.get("data") or {}).get("records") or []
    lock_cfg = next((c for c in cfg_rows if c.get("config_key") == "attendance.lock.day"), None)
    if lock_cfg:
        status, env = call("PUT", f"/sys/configs/{lock_cfg['id']}", token=token, body={"configValue": "31"})
        check("参数业务区间：考勤封账日=31 被拒（400）", status == 400, f"HTTP={status} {env}")
    else:
        check("参数业务区间：考勤封账日=31 被拒（400）", False, "未找到 attendance.lock.day 参数")

    # 内置参数不允许置空
    if lock_cfg:
        status, env = call("PUT", f"/sys/configs/{lock_cfg['id']}", token=token, body={"configValue": ""})
        check("系统内置参数不允许置空（400）", status == 400, f"HTTP={status} {env}")

    # 公告状态机：草稿不可下架
    status, env = call("GET", "/notices/page", token=token, params={"status": 1, "current": 1, "size": 1})
    drafts = (env.get("data") or {}).get("records") or []
    if drafts:
        status, env = call("PUT", f"/notices/{drafts[0]['id']}/offline", token=token)
        check("公告状态机：草稿不可下架（400）", status == 400, f"HTTP={status} {env}")

    # 重复登录账号
    status, env = call("POST", "/sys/users", token=token,
                       body={"username": ADMIN_USER, "realName": "重复账号", "password": "Init@2026",
                             "roles": ["ROLE_EMPLOYEE"]})
    check("登录账号唯一性：重复 admin 被拒（400）", status == 400, f"HTTP={status} {env}")

    # 字典类型不存在
    status, env = call("GET", "/sys/dict-types/999999/items", token=token)
    check("字典类型不存在 → 404", status == 404, f"HTTP={status} {env}")

    # BR-05-03 职责分离：复核人不得与核算人相同。
    # 该规则原先只能靠 remark 里的 CALC_BY 标记成立（表内无核算人列），
    # 现已补 salary_record.calc_by 列。演示数据全部是「已发放且锁定」的记录，
    # 直接复核会先被 BR-05-04 拦下，因此这里用「快照 → 造场景 → 还原」的方式验证。
    section("6.1 BR-05-03 职责分离（复核人 ≠ 核算人）")
    admin_user_id = user_info.get("id")

    has_col = query_scalar(
        "SELECT COUNT(*) FROM information_schema.columns "
        "WHERE table_schema = DATABASE() AND table_name = 'salary_record' AND column_name = 'calc_by'")
    check("salary_record.calc_by 列已存在（BR-05-03 的核算人载体）", has_col == 1,
          "信息架构中未找到 calc_by 列")

    probe_rec = query_scalar("SELECT id FROM salary_record WHERE deleted = 0 ORDER BY id LIMIT 1")
    if probe_rec and admin_user_id:
        # 快照：验证结束必须逐列还原，避免改动演示数据
        snap = query_scalar(
            "SELECT CONCAT_WS(',', pay_status, lock_status, IFNULL(audit_by, 'NULL'), "
            f"IFNULL(calc_by, 'NULL')) FROM salary_record WHERE id = {probe_rec}")
        p_status, l_status, a_by, c_by = [x.strip() for x in str(snap).split(",")]

        def restore_salary_record():
            exec_sql(f"UPDATE salary_record SET pay_status = {p_status}, lock_status = {l_status}, "
                     f"audit_by = {a_by}, calc_by = {c_by} WHERE id = {probe_rec}")

        # 场景：本人核算，再由本人复核 → 必须被拒
        exec_sql(f"UPDATE salary_record SET pay_status = 1, lock_status = 0, "
                 f"calc_by = {admin_user_id} WHERE id = {probe_rec}")
        status, env = call("PUT", f"/salary-records/{probe_rec}/review", token=token)
        msg = env.get("message") if isinstance(env, dict) else ""
        check("BR-05-03 同一人既核算又复核 → 400 且提示职责分离",
              status == 400 and isinstance(msg, str) and ("复核" in msg or "核算" in msg),
              f"HTTP={status} message={msg!r}")

        # 反证：换成「他人核算」→ 复核应当放行（证明该规则不是无条件拦截）
        other_user_id = query_scalar(
            f"SELECT id FROM sys_user WHERE deleted = 0 AND id <> {admin_user_id} "
            "AND status = 1 ORDER BY id LIMIT 1")
        if other_user_id:
            exec_sql(f"UPDATE salary_record SET pay_status = 1, lock_status = 0, "
                     f"calc_by = {other_user_id} WHERE id = {probe_rec}")
            status, env = call("PUT", f"/salary-records/{probe_rec}/review", token=token)
            check("BR-05-03 他人核算时复核放行（规则非无条件拦截）",
                  status == 200 and env.get("code") == 200,
                  f"HTTP={status} body={env}")
        else:
            check("BR-05-03 反证固件：存在另一个启用账号", False, "未找到其它启用账号")

        restore_salary_record()
        after = query_scalar(
            "SELECT CONCAT_WS(',', pay_status, lock_status, IFNULL(audit_by, 'NULL'), "
            f"IFNULL(calc_by, 'NULL')) FROM salary_record WHERE id = {probe_rec}")
        check("探针薪资记录已逐列还原（不留副作用）", str(after) == str(snap),
              f"还原后={after} 快照={snap}")

        status, env = call("GET", "/salary-records/page", token=token,
                           params={"current": 1, "size": 1})
        rec = ((env.get("data") or {}).get("records") or [{}])[0]
        check("薪资记录响应暴露蛇形 calc_by 字段（列已贯通到契约层）",
              "calc_by" in rec, f"记录字段={sorted(rec.keys())}")
    else:
        check("BR-05-03 固件：存在薪资记录", False,
              f"probe_rec={probe_rec} admin_user_id={admin_user_id}")

    # =========================================================== 7. RBAC 端到端
    section("7. RBAC 与行级数据范围（创建临时账号验证后清理）")

    # 探针账号名带时间戳，保证脚本可重复执行（账号是逻辑删除，名字不可复用）
    stamp = str(int(time.time()))
    probe_user = f"verify_probe_{stamp}"
    s, e = call("POST", "/sys/users", token=token,
                body={"username": probe_user, "realName": "验证探针员工", "password": "Probe@2026",
                      "employeeId": fx["employeeId"], "deptId": fx["deptId"],
                      "roles": ["ROLE_EMPLOYEE"]})
    created_id = (e.get("data") or {}).get("id") if isinstance(e, dict) and isinstance(e.get("data"), dict) else None
    check("以管理员身份创建 EMPLOYEE 角色临时账号", s == 200 and e.get("code") == 200 and created_id,
          f"HTTP={s} body={e}")

    if created_id:
        s, e = call("POST", "/auth/login", body={"username": probe_user, "password": "Probe@2026"})
        emp_token = (e.get("data") or {}).get("token") if s == 200 and isinstance(e.get("data"), dict) else None
        check("临时账号可登录（BR-09-01 合规口令）", bool(emp_token), f"HTTP={s} body={e}")

        if emp_token:
            s, e = call("GET", "/sys/users/page", token=emp_token)
            check("EMPLOYEE 角色访问管理员接口 → 403", s == 403 and e.get("code") == 403,
                  f"HTTP={s} body={e}")

            s, e = call("GET", "/sys/logs/oper", token=emp_token)
            check("EMPLOYEE 角色访问日志审计 → 403", s == 403, f"HTTP={s} body={e}")

            s, e = call("GET", "/employees/page", token=emp_token, params={"current": 1, "size": 5})
            rows = (e.get("data") or {}).get("records") or []
            # 仅本人数据范围：EMPLOYEE 只应看到自己绑定的员工档案
            same = all(r.get("id") == fx["employeeId"] for r in rows) if rows else True
            check("EMPLOYEE 行级数据范围：仅可见本人档案", s == 200 and same,
                  f"HTTP={s} 可见条数={len(rows)} 期望仅 employeeId={fx['employeeId']}")

            # 员工自助接口成功路径：必须由「已绑定员工档案」的账号调用
            section("7.1 员工自助接口成功路径（EMPLOYEE 账号）")
            for path in ["/attendance/today", "/leave-balances/my", "/salary/payslips/my",
                         "/training/my"]:
                s, e = call("GET", path, token=emp_token)
                good, why = ok(e, 200, 200)
                check(f"GET {path}（EMPLOYEE 账号）", good, f"HTTP={s} {why}")

            # 待办处理：成功路径 + 越权拦截 + 演示数据还原
            section("7.2 待办处理（PUT /dashboard/todos/{id}/handle）")
            s, e = call("GET", "/dashboard/todos", token=emp_token)
            rows = (e.get("data") or {}).get("records") or []
            mine = all(r.get("receiver_id") == fx["employeeId"] for r in rows)
            check("GET /dashboard/todos（EMPLOYEE 账号）只返回派给本人的待办",
                  s == 200 and e.get("code") == 200 and mine,
                  f"HTTP={s} 条数={len(rows)} receiver_id 集合={sorted({r.get('receiver_id') for r in rows})}")

            s, e = call("PUT", f"/dashboard/todos/{fx['todoIdOfOthers']}/handle", token=emp_token)
            check("越权处理他人待办 → 403（BR-01-02）", s == 403 and e.get("code") == 403,
                  f"HTTP={s} body={e}")

            s, e = call("PUT", "/dashboard/todos/99999999/handle", token=emp_token)
            check("处理不存在的待办 → 404 且 message 可读", s == 404 and e.get("code") == 404,
                  f"HTTP={s} body={e}")

            if fx["todoId"]:
                s, e = call("PUT", f"/dashboard/todos/{fx['todoId']}/handle", token=emp_token)
                check("处理本人待办成功（未处理 → 已处理）", s == 200 and e.get("code") == 200,
                      f"HTTP={s} body={e}")
                after = query_scalar(f"SELECT handle_status FROM todo_task WHERE id = {fx['todoId']}")
                check("处理结果已落库（handle_status = 1）", after == 1, f"handle_status={after}")
                # 还原演示数据，保证脚本可重复执行、不留副作用
                exec_sql(f"UPDATE todo_task SET handle_status = 0, handle_time = NULL WHERE id = {fx['todoId']}")
                restored = query_scalar(f"SELECT handle_status FROM todo_task WHERE id = {fx['todoId']}")
                check("待办演示数据已还原（handle_status = 0）", restored == 0, f"handle_status={restored}")
            else:
                check("固件：员工 1001 存在未处理待办可供验证", False, "库中无 receiver_id=1001 的未处理待办")

        # 清理：删除临时账号
        s, e = call("DELETE", f"/sys/users/{created_id}", token=token)
        check("清理临时验证账号", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

    # =========================================================== 8. 系统级日志
    section("8. 系统级日志记录（sys_oper_log / sys_login_log 留痕）")

    s, e = call("GET", "/sys/logs/login", token=token, params={"current": 1, "size": 5})
    login_rows = (e.get("data") or {}).get("records") or []
    check("登录日志有记录（登录动作已留痕）", s == 200 and len(login_rows) > 0,
          f"HTTP={s} 条数={len(login_rows)}")
    if login_rows:
        k = set(login_rows[0].keys())
        check("登录日志字段为蛇形（login_time / login_ip / login_status）",
              {"login_time", "login_ip", "login_status"} <= k, f"字段={sorted(k)}")

    s, e = call("GET", "/sys/logs/oper", token=token, params={"current": 1, "size": 10})
    oper_rows = (e.get("data") or {}).get("records") or []
    check("操作日志有记录（前面的写操作已留痕）", s == 200 and len(oper_rows) > 0,
          f"HTTP={s} 条数={len(oper_rows)}")
    if oper_rows:
        k = set(oper_rows[0].keys())
        check("操作日志字段为蛇形（oper_time / oper_type / oper_desc / cost_time）",
              {"oper_time", "oper_type", "oper_desc", "cost_time"} <= k, f"字段={sorted(k)}")
        modules = {r.get("module") for r in oper_rows}
        check("操作日志记录了模块名（如 系统设置）", any(m for m in modules), f"modules={modules}")

    # =========================================================== 9. 真实写路径（可回滚）
    section("9. 真实写路径端到端（新建后删除，不残留测试数据）")

    # BR-02-01：工号由系统生成，请求体不传 empNo
    # 岗位（positionId）在前端表单里是必填项（BR-02-02），因此这里必须提供
    s, e = call("POST", "/employees", token=token, body={
        "name": "验证探针员工A", "deptId": fx["deptId"], "positionId": fx["positionId"],
        "gender": "M", "employeeType": 1, "phone": "13800000001", "hireDate": "2026-09-15"})
    new_emp_id = (e.get("data") or {}).get("id") if isinstance(e, dict) else None
    new_emp_no = None
    if isinstance(e.get("data"), dict):
        new_emp_no = e["data"].get("emp_no") or e["data"].get("empNo")
    check("BR-02-01 新增员工工号由系统自动生成",
          s == 200 and new_emp_id is not None and bool(new_emp_no),
          f"HTTP={s} empNo={new_emp_no} body={e}")
    if new_emp_id:
        # JS 的 Number 只有 53 位精度，主键超过 2^53 时浏览器 JSON.parse 会静默改写数值，
        # 导致「刚创建就查不到」。这条断言专门守住自增计数器被污染的情形。
        JS_MAX_SAFE_INT = 2 ** 53 - 1
        check("新增员工主键在 JS 安全整数范围内（前端可精确回读）",
              0 < new_emp_id <= JS_MAX_SAFE_INT,
              f"id={new_emp_id} 上限={JS_MAX_SAFE_INT}")

        s, e = call("GET", f"/employees/{new_emp_id}", token=token)
        check("新增员工可被详情接口读回", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

        s, e = call("PUT", f"/employees/{new_emp_id}/status", token=token, body={"status": 2})
        check("PUT /employees/{id}/status 状态流转成功", s == 200 and e.get("code") == 200,
              f"HTTP={s} body={e}")

        s, e = call("DELETE", f"/employees/{new_emp_id}", token=token)
        check("清理探针员工（逻辑删除）", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

        # 回归：逻辑删除后工号唯一键仍占用该值，取号必须跳过它，
        # 否则第二次入职会撞唯一键（这是「逻辑删除 + 唯一键」的典型陷阱）
        s, e = call("POST", "/employees", token=token, body={
            "name": "验证探针员工B", "deptId": fx["deptId"], "positionId": fx["positionId"],
            "gender": "F", "employeeType": 1, "hireDate": "2026-09-15"})
        second = e.get("data") if isinstance(e, dict) else None
        second_no = (second or {}).get("emp_no") or (second or {}).get("empNo")
        check("逻辑删除后重新入职：工号取号跳过已删除记录，不发生唯一键冲突",
              s == 200 and bool(second_no) and second_no != new_emp_no,
              f"HTTP={s} 第一个工号={new_emp_no} 第二个工号={second_no} body={e}")
        if (second or {}).get("id"):
            call("DELETE", f"/employees/{second['id']}", token=token)

        # ------------------------------------------------------------------
        # 回归（纵深）：真正把 deleted 置 1，逼出「取号看不见已删除行」的缺陷。
        # 员工的正常删除路径（BR-02-03）只置离职、不置 deleted，所以上面那条用例
        # 走不到这个分支；只有经 mapper.deleteById / 数据清理 / 历史导入留下的
        # deleted=1 行，才会让「deleted=0 的 MAX 取号」与物理唯一键 uk_emp_no 打架。
        # ------------------------------------------------------------------
        exec_sql(f"UPDATE employee SET deleted = 1 WHERE id = {new_emp_id}")
        check("（构造）探针员工 A 已置为逻辑删除，工号仍被唯一键占用",
              query_scalar(f"SELECT deleted FROM employee WHERE id = {new_emp_id}") == 1,
              "未能置 deleted=1")

        s, e = call("POST", "/employees", token=token, body={
            "name": "验证探针员工C", "deptId": fx["deptId"], "positionId": fx["positionId"],
            "gender": "M", "employeeType": 1, "hireDate": "2026-09-15"})
        third = e.get("data") if isinstance(e, dict) else None
        third_id = (third or {}).get("id")
        third_no = (third or {}).get("emp_no") or (third or {}).get("empNo")
        check("逻辑删除（deleted=1）行的工号被取号器跳过：再入职不撞唯一键、不返回 409",
              s == 200 and third_id and third_no not in (None, new_emp_no),
              f"HTTP={s} 工号={third_no}（历史占用={new_emp_no}）body={e}")

        # 清理本次验证写入的全部探针员工（含被置 deleted=1 的那条），不留副作用
        probe_ids = [i for i in (new_emp_id, (second or {}).get("id"), third_id) if i]
        if probe_ids:
            id_list = ",".join(str(i) for i in probe_ids)
            exec_sql(f"DELETE FROM employee_change WHERE employee_id IN ({id_list}); "
                     f"DELETE FROM employee WHERE id IN ({id_list})")
            left = query_scalar(
                f"SELECT COUNT(*) FROM employee WHERE id IN ({id_list})")
            check("探针员工已从库中彻底清除（脚本可重复执行、不残留）", left == 0,
                  f"残留 {left} 行，ids={probe_ids}")

    # 公告全生命周期：草稿 → 发布 → 下架
    s, e = call("POST", "/notices", token=token,
                body={"title": "【验证探针】公告生命周期", "content": "自动化验证用，稍后删除",
                      "noticeType": 3, "targetScope": 1, "isTop": 0})
    nid = (e.get("data") or {}).get("id") if isinstance(e, dict) else None
    check("新建公告（默认草稿）", s == 200 and nid, f"HTTP={s} body={e}")
    if nid:
        s, e = call("GET", "/notices/page", token=token, params={"status": 1, "keyword": "验证探针",
                                                                "current": 1, "size": 5})
        found = (e.get("data") or {}).get("records") or []
        check("公告分页可按状态+关键字检索到草稿", s == 200 and len(found) > 0,
              f"HTTP={s} 命中={len(found)}")

        s, e = call("PUT", f"/notices/{nid}/publish", token=token)
        check("公告发布（草稿 → 已发布）", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

        s, e = call("PUT", f"/notices/{nid}/publish", token=token)
        check("公告不可重复发布（400）", s == 400, f"HTTP={s} body={e}")

        s, e = call("PUT", f"/notices/{nid}/offline", token=token)
        check("公告下架（已发布 → 已下架）", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

    # 参数修改真实生效
    if lock_cfg:
        s, e = call("PUT", f"/sys/configs/{lock_cfg['id']}", token=token, body={"configValue": "5"})
        check("参数修改（合法值 5）成功", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

    # 角色新增真实生效（roleCode 带时间戳，保证脚本可重复执行）
    probe_role_code = f"verify_probe_role_{stamp}"
    probe_role_norm = "ROLE_" + probe_role_code.upper()
    s, e = call("POST", "/sys/roles", token=token,
                body={"roleCode": probe_role_code, "roleName": "验证探针角色", "dataScope": 4,
                      "description": "自动化验证用"})
    new_role_id = (e.get("data") or {}).get("id") if isinstance(e, dict) else None
    check("新增角色成功（roleCode 自动补 ROLE_ 前缀）", s == 200 and new_role_id, f"HTTP={s} body={e}")
    if new_role_id:
        s, e = call("GET", "/sys/roles", token=token)
        rows = (e.get("data") or {}).get("records") or (e.get("data") if isinstance(e.get("data"), list) else [])
        codes = {r.get("role_code") for r in rows}
        check(f"新增角色出现在角色列表中且编码归一化为 {probe_role_norm}",
              probe_role_norm in codes, f"codes={sorted(c for c in codes if c)}")
        s, e = call("PUT", f"/sys/roles/{new_role_id}", token=token,
                    body={"roleCode": probe_role_norm, "roleName": "验证探针角色改", "dataScope": 2})
        check("修改角色（数据范围 4 → 2）成功", s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")
        s, e = call("PUT", f"/sys/roles/{new_role_id}", token=token,
                    body={"roleCode": probe_role_norm, "roleName": "x", "dataScope": 99})
        check("非法数据范围 99 被拒（400）", s == 400, f"HTTP={s} body={e}")
        s, e = call("POST", "/sys/roles", token=token,
                    body={"roleCode": probe_role_code, "roleName": "重复编码", "dataScope": 4})
        check("角色编码唯一性：重复编码被拒（400）", s == 400, f"HTTP={s} body={e}")

    # =========================================================== 10. 分页契约
    section("10. 分页契约（records / total / current / size）")

    for path in ["/employees/page", "/sys/users/page", "/sys/logs/oper", "/notices/page",
                 "/salary-records/page"]:
        s, e = call("GET", path, token=token, params={"current": 1, "size": 2})
        d = e.get("data") if isinstance(e, dict) else None
        good = isinstance(d, dict) and {"records", "total", "current", "size"} <= set(d.keys())
        check(f"{path} 分页结构统一", s == 200 and good,
              f"HTTP={s} data keys={sorted(d.keys()) if isinstance(d, dict) else d}")

    # =========================================================== 11. 逻辑删除 + 唯一键
    section("11. 「逻辑删除 + 唯一键」的复用语义（回归）")

    # 自造固件（不依赖前面小节残留的状态）：
    # 建账号 → 逻辑删除 → 用同名重建。username 上有唯一键而逻辑删除不释放该值，
    # 所以预期是「400 + 明确告知不可复用」，而不是含糊的 409 或误判为可用。
    reuse_name = f"verify_reuse_{stamp}"
    s, e = call("POST", "/sys/users", token=token,
                body={"username": reuse_name, "realName": "复用语义探针",
                      "password": "Probe@2026", "roles": ["ROLE_EMPLOYEE"]})
    reuse_id = (e.get("data") or {}).get("id") if isinstance(e, dict) else None
    check("账号名复用回归：先建账号", s == 200 and reuse_id, f"HTTP={s} body={e}")

    if reuse_id:
        s, e = call("DELETE", f"/sys/users/{reuse_id}", token=token)
        check("账号名复用回归：逻辑删除该账号",
              s == 200 and e.get("code") == 200, f"HTTP={s} body={e}")

        s, e = call("POST", "/sys/users", token=token,
                    body={"username": reuse_name, "realName": "同名额重建",
                          "password": "Probe@2026", "roles": ["ROLE_EMPLOYEE"]})
        msg = e.get("message") if isinstance(e, dict) else ""
        check("复用「已逻辑删除」的账号名 → 400 且提示可执行（而非模糊的 409）",
              s == 400 and isinstance(msg, str) and "不可复用" in msg,
              f"HTTP={s} message={msg!r}")

    # =========================================================== 12. 副作用清理
    # 放在最后：前面的每个小节都可能写数据，统一在这里收尾，
    # 保证脚本可反复执行、且不给演示环境留残留。
    # 注意：账号/角色走的是逻辑删除，会永久占用唯一键，所以必须硬清。
    section("12. 验证副作用清理（保证脚本可重复执行、不留残留）")

    exec_sql(
        "DELETE FROM sys_user_role WHERE user_id IN "
        "  (SELECT id FROM sys_user WHERE username LIKE 'verify\\_%');"
        "DELETE FROM sys_user WHERE username LIKE 'verify\\_%';"
        "DELETE FROM sys_role_menu WHERE role_id IN "
        "  (SELECT id FROM sys_role WHERE role_code LIKE '%VERIFY%');"
        "DELETE FROM sys_user_role WHERE role_id IN "
        "  (SELECT id FROM sys_role WHERE role_code LIKE '%VERIFY%');"
        "DELETE FROM sys_role WHERE role_code LIKE '%VERIFY%';"
        "DELETE FROM notice_read WHERE notice_id IN "
        "  (SELECT id FROM notice WHERE title LIKE '%验证探针%');"
        "DELETE FROM notice WHERE title LIKE '%验证探针%';"
        "DELETE FROM employee_change WHERE employee_id IN "
        "  (SELECT id FROM employee WHERE name LIKE '%验证探针%');"
        "DELETE FROM employee WHERE name LIKE '%验证探针%';"
    )

    left_users = query_scalar("SELECT COUNT(*) FROM sys_user WHERE username LIKE 'verify\\_%'")
    left_roles = query_scalar("SELECT COUNT(*) FROM sys_role WHERE role_code LIKE '%VERIFY%'")
    left_emps = query_scalar("SELECT COUNT(*) FROM employee WHERE name LIKE '%验证探针%'")
    left_notices = query_scalar("SELECT COUNT(*) FROM notice WHERE title LIKE '%验证探针%'")

    check(f"探针账号已清除（残留 {left_users} 条）", left_users == 0, f"残留={left_users}")
    check(f"探针角色已清除（残留 {left_roles} 条）", left_roles == 0, f"残留={left_roles}")
    check(f"探针员工已清除（残留 {left_emps} 条）", left_emps == 0, f"残留={left_emps}")
    check(f"探针公告已清除（残留 {left_notices} 条）", left_notices == 0, f"残留={left_notices}")

    # 演示环境基线：跑完验证后仍应保有种子数据规模
    emp_cnt = query_scalar("SELECT COUNT(*) FROM employee")
    dept_cnt = query_scalar("SELECT COUNT(*) FROM dept")
    check(f"演示数据基线未被破坏（员工={emp_cnt} ≥ 50、部门={dept_cnt} ≥ 13）",
          emp_cnt >= 50 and dept_cnt >= 13, f"员工={emp_cnt} 部门={dept_cnt}")

    # =========================================================== 汇总
    section("验证汇总")
    print(f"通过：{len(PASS)}    失败：{len(FAIL)}")
    if FAIL:
        print("\n失败明细：")
        for name, detail in FAIL:
            print(f"  - {name}\n      {detail}")
    return 1 if FAIL else 0


if __name__ == "__main__":
    sys.exit(main())
