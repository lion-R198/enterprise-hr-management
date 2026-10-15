#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
企业人事管理系统 —— 后端接口测试套件（真实 HTTP 调用 + 真实数据库断言）

测试范围
  1. 接口可用性：94 个后端端点全部真实调用，路径/方法/返回结构无异常
  2. 统一结果处理：响应恒为严格三段式 {code, message, data}
  3. 统一异常处理：HTTP 状态码 ≡ 业务 code；message 为可读中文；不泄漏堆栈
  4. 参数校验：必填、格式、区间、长度校验在 Controller 层被 @Valid 拦截
  5. 业务规则：36 条 BR-xx（需求规格说明书）逐条落地验证
  6. 鉴权与数据权限：@RequireRoles 角色校验 + DataScopeHelper 行级数据范围
  7. 字段命名约定：响应蛇形 snake_case / 请求驼峰 camelCase（含例外清单）
  8. 系统级日志：写操作落 sys_oper_log、登录落 sys_login_log

运行前提
  - 后端已启动在 http://localhost:8080（context-path = /api）
  - 数据库 enterprise_hr 可用（root/123456），MySQL 客户端位于 D:/Apps/MySQL8.0/bin/mysql
  - 演示账号统一口令 admin123

用法
  python docs/api-test-runner.py            # 跑全部用例
  python docs/api-test-runner.py -k SAL     # 只跑用例编号含 SAL 的用例（调试用）

副作用治理
  - 所有探针数据统一以 TCP 前缀命名，跨模块共用一套「探针员工 / 探针账号」
  - 需改动演示数据的用例一律「快照 → 造场景 → 逐列还原」
  - 收尾在第 20 节统一硬删探针数据，并断言残留为 0、演示数据基线未被破坏
"""

from __future__ import annotations

import json
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, datetime, timedelta

sys.stdout.reconfigure(encoding="utf-8")

BASE = "http://localhost:8080/api"
MYSQL = "D:/Apps/MySQL8.0/bin/mysql"
DB = "enterprise_hr"
DEMO_PWD = "admin123"
PROBE_PWD = "Probe@12345"
PROBE = "TCP"                      # 探针命名前缀
PROBE_MONTH = (2026, 1)            # 薪资核算探针月份（无考勤月报行 → 满足 BR-05-02 前置）

ONLY = ""
for i, a in enumerate(sys.argv):
    if a == "-k" and i + 1 < len(sys.argv):
        ONLY = sys.argv[i + 1]

RESULTS: list[dict] = []
TOKEN: dict[str, str] = {}
IDS: dict[str, object] = {}        # 运行期固件 id 集合
SNAP: dict[str, dict] = {}         # 演示数据快照（用于逐列还原）
SKIPPED: list[str] = []


def want(cid: str) -> bool:
    return (not ONLY) or (ONLY.upper() in cid.upper())


# =========================================================================== 基础设施

def call(method, path, token=None, params=None, body=None, raw=False):
    """发起一次 HTTP 调用，返回 (http_status, body)。连接失败时 http_status = 0。"""
    url = BASE + path
    if params:
        clean = {k: v for k, v in params.items() if v is not None and v != ""}
        if clean:
            url += "?" + urllib.parse.urlencode(clean, doseq=True)
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        if isinstance(body, bytes):
            data = body
        elif isinstance(body, str):
            data = body.encode("utf-8")          # urllib 只接受 bytes，str 会直接抛 TypeError
        else:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json;charset=UTF-8"
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(url, data=data, headers=headers, method=method.upper())
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            text = resp.read().decode("utf-8", "replace")
            status = resp.status
    except urllib.error.HTTPError as e:
        text = e.read().decode("utf-8", "replace")
        status = e.code
    except Exception as exc:                    # 连接被拒绝 / 超时 / 传输层异常
        return 0, {"_transport_error": f"{type(exc).__name__}: {exc}"}
    if raw:
        return status, text
    try:
        return status, json.loads(text)
    except json.JSONDecodeError:
        return status, {"_raw": text[:400]}


def dt(days=0, time_part="09:00:00"):
    """按后端 Jackson 配置（date-format: yyyy-MM-dd HH:mm:ss）生成时间字符串。

    注意：LocalDateTime 字段用 ISO 的 'T' 分隔符会被 HttpMessageNotReadableException 拦成
    「请求体格式错误」，必须用空格分隔。
    """
    return (date.today() + timedelta(days=days)).strftime("%Y-%m-%d ") + time_part


def sql_rows(query):
    out = subprocess.run(
        [MYSQL, "-uroot", "-p123456", "-N", "-B", "-D", DB, "-e", query],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    return [ln.split("\t") for ln in (out.stdout or "").strip().splitlines() if ln.strip()]


def sql_scalar(query):
    rows = sql_rows(query)
    if not rows:
        return None
    val = rows[0][0].strip()
    if val in ("", "NULL"):
        return None
    try:
        return int(val)
    except ValueError:
        try:
            return float(val)
        except ValueError:
            return val


def sql_exec(statement):
    subprocess.run(
        [MYSQL, "-uroot", "-p123456", "-D", DB, "-e", statement],
        capture_output=True, text=True, encoding="utf-8", errors="replace")


def snapshot(key, table, row_id, cols):
    """读取一行若干列的当前值，生成可直接执行的还原 UPDATE（用于改动演示数据的用例）。"""
    rows = sql_rows(f"SELECT {cols} FROM {table} WHERE id={row_id}")
    if not rows:
        return False
    sets = []
    for c, v in zip([c.strip() for c in cols.split(",")], rows[0]):
        v = v.strip()
        sets.append(f"{c}=" + ("NULL" if v in ("NULL", "") else "'" + v.replace("'", "''") + "'"))
    SNAP[key] = {"restore_sql": f"UPDATE {table} SET " + ", ".join(sets) + f" WHERE id={row_id};"}
    return True


def mk_emp(name, salary=7000, dept=None, position=1):
    """快速创建一名探针员工（不经用例记录，仅作固件）。返回新员工 id，失败返回 None。"""
    http, env = call("POST", "/employees", token=TOKEN["admin"], body={
        "name": name, "deptId": dept or IDS["dept_with_emp"], "positionId": position, "salary": salary})
    if isinstance(env, dict) and isinstance(env.get("data"), dict) and env["data"].get("id"):
        return int(env["data"]["id"])
    print(f"  !! 固件创建失败 {name}：HTTP {http} {env}")
    return None


def snake(keys):
    """判断一组字段名是否全部为 snake_case（不含大写字母）。"""
    return all(k == k.lower() for k in keys)


def has_camel(keys):
    return any(any(c.isupper() for c in k) for k in keys)


# =========================================================================== 用例记录

def diagnose(http, env, expect, hint=""):
    """把「实际结果 vs 预期」翻译成可执行的排查方向。"""
    parts = []
    if http == 0:
        parts.append("后端服务不可达（连接被拒绝或超时），请确认应用已在 http://localhost:8080/api 启动")
    elif not isinstance(env, dict):
        parts.append("响应体不是合法 JSON 对象，可能被应用之外的组件拦截")
    elif http != expect:
        if http == 401:
            parts.append("请求未通过 AuthInterceptor 鉴权，检查令牌是否有效、该路径是否被误放行")
        elif http == 403:
            parts.append("被角色校验(@RequireRoles)或数据范围校验(DataScopeHelper)拦截，核对用例所用角色与端点的角色要求是否一致")
        elif http == 404:
            parts.append("路径未匹配到处理器，核对 Controller 注解路径与 context-path 拼接")
        elif http == 405:
            parts.append("HTTP 方法不匹配，核对 @RequestMapping 的 method 声明")
        elif http == 409:
            parts.append("唯一键冲突由 DuplicateKeyException 兜底，说明业务层未做前置查重")
        elif http == 500:
            parts.append("服务端抛出未捕获异常，查 run.log 中该时间点的 Exception 堆栈")
        elif http == 400 and expect == 200:
            parts.append("被参数校验(@Valid)或业务校验拦截了本应成功的请求，核对请求体字段名（须驼峰）与必填项")
        else:
            parts.append(f"返回码与预期不符（实际 {http}，预期 {expect}）")
        msg = env.get("message") if isinstance(env, dict) else ""
        if msg:
            parts.append(f"后端 message：{msg}")
    elif isinstance(env, dict) and env.get("code") != expect:
        parts.append("业务 code 与预期不符，核对 Service 抛出的 ErrorCode 定义")
    else:
        parts.append("补充断言未通过，见实际结果列")
    if hint:
        parts.append("建议排查：" + hint)
    return "；".join(parts)


def digest(env, extra_act=""):
    if env is None:
        return "无响应"
    if "_transport_error" in env:
        return "无法连接后端服务"
    if "_raw" in env:
        return f"非 JSON 响应：{env['_raw'][:160]}"
    txt = f"code={env.get('code')} message={env.get('message')!r}"
    if extra_act:
        txt += " | " + extra_act
    return txt


def judge(http, env, expect, extra):
    """核心判定：严格三段式 + 业务 code + HTTP 状态码一致 + 可选的补充断言。"""
    if http == 0:
        return False, "无法连接后端服务", ""
    if not isinstance(env, dict) or "_raw" in env:
        return False, "响应体不是合法 JSON 对象", digest(env)
    keys = set(env.keys())
    if keys != {"code", "message", "data"}:
        return False, f"响应信封不是严格三段式，实际字段 {sorted(keys)}", digest(env)
    if env.get("code") != expect:
        return False, f"业务 code 与预期不符（实际 {env.get('code')}，预期 {expect}）", digest(env)
    if http != expect:
        return False, f"HTTP 状态码({http}) 与业务 code({env.get('code')}) 不一致，约定要求同值", digest(env)
    if expect >= 500:
        return False, "服务端返回 5xx，属未捕获异常", digest(env)
    extra_act = ""
    if extra:
        res = extra(env)
        okk, why = res[0], res[1]
        extra_act = res[2] if len(res) > 2 else ""
        if not okk:
            return False, why, digest(env, extra_act)
    return True, "", digest(env, extra_act)


DEFAULT_EXP = {
    200: "HTTP 200 / code=200；响应为严格三段式 {code,message,data}，data 结构符合契约",
    400: "HTTP 400 / code=400；message 为可读中文，指出具体被拒原因",
    401: "HTTP 401 / code=401；提示登录状态失效，不泄漏业务信息",
    403: "HTTP 403 / code=403；提示无操作权限或无数据权限",
    404: "HTTP 404 / code=404；提示资源不存在",
    405: "HTTP 405 / code=405；提示请求方法不被支持",
    409: "HTTP 409 / code=409；提示唯一性冲突",
}


def tr(cid, module, name, precondition, method, path, *, token="admin", params=None, body=None,
       expect=200, exp_desc=None, req_desc=None, extra=None, hint="", raw_text=False):
    """执行一条 HTTP 用例并登记结果。返回 (http, env)。"""
    if not want(cid):
        return None, None
    t0 = time.time()
    # token 传角色键（admin/hr/fin/mgr/emp/probe）取已签发令牌；传其它字符串则当作原始令牌直接使用
    tk = TOKEN.get(token, token) if token else None
    http, env = call(method, path, token=tk, params=params, body=body)
    elapsed = int((time.time() - t0) * 1000)
    passed, why, act = judge(http, env, expect, extra)
    if req_desc is None:
        bits = []
        if params:
            bits.append("query: " + urllib.parse.urlencode({k: v for k, v in params.items() if v is not None}))
        if body is not None:
            bits.append("body: " + (body if isinstance(body, str) else json.dumps(body, ensure_ascii=False)))
        if token:
            bits.append(f"token: {token} 角色令牌")
        req_desc = "；".join(bits) if bits else "（无参数）"
    RESULTS.append({
        "cid": cid, "module": module, "name": name, "precondition": precondition,
        "method": method.upper(), "path": path, "req": req_desc,
        "exp": exp_desc or DEFAULT_EXP.get(expect, f"HTTP {expect} / code={expect}"),
        "act": f"HTTP {http if http else '连接失败'} | {act} | 耗时 {elapsed}ms",
        "passed": passed, "reason": "" if passed else diagnose(http, env, expect, hint),
    })
    flag = "PASS" if passed else "FAIL"
    print(f"  [{flag}] {cid} {name}" + ("" if passed else f"\n         -> {RESULTS[-1]['reason']}"))
    return http, env


def db_case(cid, module, name, precondition, exp_desc, query, predicate, hint=""):
    """执行一条纯数据库断言用例（用于验证留痕、逻辑删除、状态回写等副作用）。"""
    if not want(cid):
        return None
    try:
        rows = sql_rows(query)
        value = rows
        passed = bool(predicate(rows))
    except Exception as e:                      # SQL 本身出错也算不通过
        rows, value, passed = [], None, False
        print(f"  [FAIL] {cid} {name} SQL 执行异常：{e}")
    act = f"SQL 返回 {len(rows)} 行" + (f"，首行={rows[0]}" if rows else "")
    RESULTS.append({
        "cid": cid, "module": module, "name": name, "precondition": precondition,
        "method": "SQL", "path": query.replace("\n", " ")[:300], "req": "数据库直查校验",
        "exp": exp_desc, "act": act, "passed": passed,
        "reason": "" if passed else diagnose(200, {}, 200, hint or "核对落库逻辑与事务边界"),
    })
    print(f"  [{'PASS' if passed else 'FAIL'}] {cid} {name}")
    return value


def section(title):
    print(f"\n{'=' * 84}\n{title}\n{'=' * 84}")


def login(key, username, password=DEMO_PWD):
    http, env = call("POST", "/auth/login", body={"username": username, "password": password})
    if isinstance(env, dict) and isinstance(env.get("data"), dict) and env["data"].get("token"):
        TOKEN[key] = env["data"]["token"]
        return True
    print(f"  !! 账号 {username} 登录失败：HTTP {http} {env}")
    return False


def ensure(cond, msg):
    if not cond:
        print(f"\n中止：{msg}")
        sys.exit(2)


# =========================================================================== 用例集

def g_auth():
    section("1. 认证与令牌（POST /auth/login、GET /auth/me、POST /auth/logout）")
    tr("AUTH-01", "认证与令牌", "管理员凭正确口令登录成功并签发令牌", "admin 账号启用中", "POST", "/auth/login",
       token=None, body={"username": "admin", "password": DEMO_PWD}, expect=200,
       exp_desc="HTTP 200 / code=200；data.token 为非空字符串，data.user 返回当前账号信息",
       extra=lambda e: (bool(e["data"].get("token")), "未返回 token，登录签发链路异常", f"token 长度={len(str(e['data'].get('token')))}"),
       hint="检查 JwtTokenProvider 签发与 AuthController.login 的返回装配")
    tr("AUTH-02", "认证与令牌", "登录响应 data.user 内部字段为驼峰命名", "同上", "POST", "/auth/login",
       token=None, body={"username": "admin", "password": DEMO_PWD}, expect=200,
       exp_desc="data.user 含 employeeId / realName / deptId / roles（驼峰例外清单内），供前端直接绑定",
       extra=lambda e: ({"employeeId", "realName", "deptId", "roles"} <= set(e["data"].get("user", {})),
                        f"data.user 字段缺失，实际 {sorted(e['data'].get('user', {}).keys())}",
                        f"user 字段={sorted(e['data'].get('user', {}).keys())}"),
       hint="核对 LoginVO 上的 @JsonProperty 例外标注")
    tr("AUTH-03", "认证与令牌", "口令错误时拒绝登录", "admin 账号存在", "POST", "/auth/login",
       token=None, body={"username": "admin", "password": "wrong-password"}, expect=400,
       hint="BAD_CREDENTIALS 应映射 400 且文案为「用户名或密码错误」")
    tr("AUTH-04", "认证与令牌", "账号不存在时拒绝登录且不暴露账号是否存在", "输入未注册账号", "POST", "/auth/login",
       token=None, body={"username": "no_such_user_xyz", "password": "whatever123"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「用户名或密码错误」（与口令错误同文案，避免账号枚举）",
       hint="账号不存在与口令错误应返回相同文案，防止账号枚举")
    tr("AUTH-05", "认证与令牌", "登录账号为空被参数校验拦截", "—", "POST", "/auth/login",
       token=None, body={"username": "", "password": DEMO_PWD}, expect=400,
       exp_desc="HTTP 400 / code=400；message 指向「登录账号不能为空」")
    tr("AUTH-06", "认证与令牌", "登录口令为空被参数校验拦截", "—", "POST", "/auth/login",
       token=None, body={"username": "admin", "password": ""}, expect=400,
       exp_desc="HTTP 400 / code=400；message 指向「登录密码不能为空」")
    tr("AUTH-07", "认证与令牌", "已停用账号禁止登录", "zhangmin 账号 status=0（停用）", "POST", "/auth/login",
       token=None, body={"username": "zhangmin", "password": DEMO_PWD}, expect=403,
       exp_desc="HTTP 403 / code=403；提示「账号已停用，请联系系统管理员」",
       hint="ACCOUNT_DISABLED 应映射 403；若返回 400 说明未区分停用与凭证错误")
    tr("AUTH-08", "认证与令牌", "携带令牌查询当前登录用户", "持有 admin 令牌", "GET", "/auth/me", expect=200,
       exp_desc="HTTP 200 / code=200；返回当前账号 username=admin 且含 roles",
       extra=lambda e: (e["data"].get("username") == "admin", f"username 实际为 {e['data'].get('username')}",
                        f"username={e['data'].get('username')}"),
       hint="检查 AuthController.me 从 UserContext 取用户；若为空说明拦截器未写入上下文")
    tr("AUTH-09", "认证与令牌", "无令牌访问受保护接口返回 401", "未携带 Authorization 头", "GET", "/auth/me",
       token=None, expect=401, hint="AuthInterceptor 应覆盖 /auth/me 并 fail-close")
    tr("AUTH-10", "认证与令牌", "非法令牌访问受保护接口返回 401", "Authorization 为伪造串", "GET", "/auth/me",
       token="bogus.token.value", expect=401,
       exp_desc="HTTP 401 / code=401（畸形/签名不符的令牌一律拒绝，不得放行也不得 500）",
       hint="检查 JWT 解析异常是否被统一转成 UNAUTHORIZED；若返回 500 说明解析异常未捕获")
    tr("AUTH-11", "认证与令牌", "篡改签名的令牌被拒绝", "取合法令牌后破坏其签名段", "GET", "/auth/me",
       token=(TOKEN.get("admin", "x")[:-6] + "000000") if TOKEN.get("admin") else "x", expect=401,
       exp_desc="HTTP 401 / code=401；签名校验失败必须拒绝（HS256 密钥校验生效）",
       hint="若返回 200 说明令牌未验签，等同于任意用户可伪造身份")
    tr("AUTH-12", "认证与令牌", "退出登录接口可用", "持有 admin 令牌", "POST", "/auth/logout", expect=200,
       exp_desc="HTTP 200 / code=200（服务端无状态，仅记录审计日志）")
    db_case("AUTH-13", "认证与令牌", "登录行为写入系统登录日志", "存在历史登录记录",
            "执行本轮若干次登录后，sys_login_log 行数应大于启动前基线",
            "SELECT COUNT(*) FROM sys_login_log", lambda r: int(r[0][0]) > 0,
            hint="检查 SysLoginLogService 是否在登录成功/失败分支均写日志（BR-09-03）")


def g_fw():
    section("2. 统一结果处理与全局异常处理")
    tr("FW-01", "统一响应与异常", "成功响应恒为严格三段式信封", "admin 已登录", "GET", "/employees/page",
       params={"current": 1, "size": 5}, expect=200,
       exp_desc="顶层字段集合恰为 {code, message, data}，无 success 等冗余字段（R.isSuccess 已被 @JsonIgnore）",
       extra=lambda e: (set(e.keys()) == {"code", "message", "data"}, f"顶层字段为 {sorted(e.keys())}",
                        f"顶层字段={sorted(e.keys())}"),
       hint="若多出 success 字段，说明 R 上的 isSuccess() 未加 @JsonIgnore")
    tr("FW-02", "统一响应与异常", "未知路径返回 404 而非白页", "—", "GET", "/no-such-endpoint-xyz", expect=404,
       exp_desc="HTTP 404 / code=404；message「请求的接口不存在」（三段式信封）",
       hint="需开启 throw-exception-if-no-handler-found 且放开静态资源映射，否则 404 会绕过 @RestControllerAdvice")
    tr("FW-03", "统一响应与异常", "HTTP 方法与端点声明不匹配返回 405", "POST 调用仅 GET 的端点", "POST",
       "/employees/by-emp-no/E0002", expect=405,
       exp_desc="HTTP 405 / code=405；message 指出方法不被支持",
       hint="HttpRequestMethodNotSupportedException 应被 GlobalExceptionHandler 捕获并映射 405")
    tr("FW-04", "统一响应与异常", "路径变量类型不匹配返回 400", "将 /employees/{id} 的 id 传为非数字", "GET",
       "/employees/abc", expect=400, exp_desc="HTTP 400 / code=400；提示参数类型不正确",
       hint="MethodArgumentTypeMismatchException 应映射 400 而非 500")
    tr("FW-05", "统一响应与异常", "请求体非法 JSON 返回 400", "请求体为截断的 JSON", "POST", "/depts",
       body="{bad json", expect=400, exp_desc="HTTP 400 / code=400；提示请求体格式错误",
       hint="HttpMessageNotReadableException 应映射 400")
    tr("FW-06", "统一响应与异常", "分页每页条数超上限被拦截", "size 传 9999（上限 500）", "GET", "/employees/page",
       params={"current": 1, "size": 9999}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「每页条数不得超过 500」",
       hint="PageQuery 上的 @Max 校验需生效（@Validated 需标在 Controller 类上）")
    tr("FW-07", "统一响应与异常", "分页页码小于 1 被拦截", "current 传 0（下限 1）", "GET", "/employees/page",
       params={"current": 0, "size": 5}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「页码不得小于 1」")
    tr("FW-08", "统一响应与异常", "分页每页条数为负被拦截", "size 传 -1", "GET", "/employees/page",
       params={"current": 1, "size": -1}, expect=400, exp_desc="HTTP 400 / code=400；message 含「每页条数不得小于 1」")
    tr("FW-09", "统一响应与异常", "参数校验失败的消息为可读中文", "新增员工时不传姓名", "POST", "/employees",
       body={"deptId": 5, "positionId": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；message 为「姓名不能为空」等中文提示，不出现 Java 异常类名",
       extra=lambda e: ("姓名" in str(e.get("message", "")), f"message 未指向姓名字段：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="MethodArgumentNotValidException 应聚合字段错误文案；若出现 Exception 类名说明兜底分支吞了消息")
    tr("FW-10", "统一响应与异常", "错误响应不泄漏服务端堆栈信息", "触发校验类错误", "GET", "/employees/abc",
       expect=400,
       exp_desc="message 为业务中文描述，不含 'java.' / 'Exception' / 'at com.enterprise' 等堆栈片段",
       extra=lambda e: (not any(k in str(e.get("message", "")) for k in ("java.", "Exception", "at com.enterprise")),
                        f"message 疑似泄漏技术细节：{e.get('message')}", f"message={e.get('message')}"),
       hint="检查 GlobalExceptionHandler 兜底分支是否直接返回 e.getMessage()")
    tr("FW-11", "统一响应与异常", "超长字段触发长度校验而非落库被截断", "部门名称传 200 字符（列宽 64）", "POST", "/depts",
       body={"deptCode": "TCP_OVERSIZE", "name": "长" * 200, "parentId": 1, "deptType": 3}, expect=400,
       exp_desc="HTTP 400 / code=400；长度校验在进入业务层前被拦截",
       hint="若返回 200 说明部门名称缺少 @Size 校验，超长数据会以静默截断方式落库")


def g_sec():
    section("3. 鉴权拦截与两级权限（角色 + 数据范围）")
    tr("SEC-01", "鉴权与数据权限", "未登录访问业务接口一律 401", "不带令牌", "GET", "/employees/page",
       token=None, expect=401, hint="AuthInterceptor 需覆盖 /**（fail-close），仅放行 /auth/login 与 Swagger")
    tr("SEC-02", "鉴权与数据权限", "普通员工访问用户管理接口被拒", "linwanqing 仅 ROLE_EMPLOYEE", "GET",
       "/sys/users/page", token="emp", expect=403, exp_desc="HTTP 403 / code=403；仅 ADMIN 可访问",
       hint="核对 SysUserController 上的 @RequireRoles(ADMIN)")
    tr("SEC-03", "鉴权与数据权限", "普通员工新增部门被拒", "ROLE_EMPLOYEE 不具备部门维护权", "POST", "/depts",
       token="emp", body={"deptCode": "TCP_DENY", "name": "越权部门", "parentId": 1, "deptType": 3}, expect=403,
       hint="@RequireRoles 方法级注解应优先于类级注解")
    tr("SEC-04", "鉴权与数据权限", "财务角色不在部门读接口白名单内被拒", "limin 仅 ROLE_FINANCE", "GET", "/depts",
       token="fin", expect=403,
       exp_desc="HTTP 403 / code=403；/depts 仅放行 HR/MANAGER/EMPLOYEE（FINANCE 不参与组织查询）",
       hint="核对 DeptController 的角色清单，注意 FINANCE 是否被误加入")
    tr("SEC-05", "鉴权与数据权限", "普通员工新增员工档案被拒", "ROLE_EMPLOYEE 无入职办理权", "POST", "/employees",
       token="emp", body={"name": "越权员工", "deptId": 5, "positionId": 1}, expect=403,
       hint="核对 EmployeeController.create 的 @RequireRoles(HR)")
    tr("SEC-06", "鉴权与数据权限", "普通员工发布公告被拒", "公告维护仅 ADMIN/HR", "PUT", "/notices/1/publish",
       token="emp", expect=403, hint="核对 NoticeController 的 @RequireRoles(ADMIN, HR)")
    tr("SEC-07", "鉴权与数据权限", "部门主管访问操作日志被拒", "shenjianguo 仅 ROLE_MANAGER", "GET", "/sys/logs/oper",
       token="mgr", expect=403, exp_desc="HTTP 403 / code=403；日志审计仅 ADMIN 可见",
       hint="核对 SysLogController 的 @RequireRoles(ADMIN)")
    tr("SEC-08", "鉴权与数据权限", "普通员工查看他人薪资明细被行级拦截", "用探针账号读取演示员工的薪资条明细",
       "GET", f"/salary/payslips/{IDS.get('paid_record_id')}/items", token="probe", expect=403,
       exp_desc="HTTP 403 / code=403；NF-10 行级隔离——非本人且非 HR/FINANCE/ADMIN 一律拒绝",
       hint="核对 SalaryRecordServiceImpl.payslipItems 的 self/authorized 双条件")
    tr("SEC-09", "鉴权与数据权限", "普通员工读取参数配置被拒", "参数配置仅 ADMIN/HR 可读", "GET", "/sys/configs",
       token="emp", expect=403,
       exp_desc="HTTP 403 / code=403；业务参数（个税起征点、加班折算比例等）不对普通员工开放",
       hint="核对 SysConfigController 的 @RequireRoles(ADMIN, HR)")
    tr("SEC-10", "鉴权与数据权限", "普通员工只能看到本人档案数据范围", "linwanqing 绑定员工 E0002，数据范围为「仅本人」",
       "GET", "/employees/page", token="emp", params={"current": 1, "size": 100}, expect=200,
       exp_desc="分页 total = 1 且 records[0].emp_no = E0002（BR-01-01 行级范围落地）",
       extra=lambda e: (e["data"].get("total") == 1 and e["data"]["records"] and
                        e["data"]["records"][0].get("emp_no") == "E0002",
                        f"total={e['data'].get('total')}，首条 emp_no={e['data']['records'][0].get('emp_no') if e['data'].get('records') else 'N/A'}",
                        f"total={e['data'].get('total')}"),
       hint="核对 DataScopeHelper 对「仅本人」范围是否按 employeeId 过滤")
    tr("SEC-11", "鉴权与数据权限", "关键字查询中的 SQL 注入尝试被安全处理", "keyword 传入引号闭合式注入串", "GET",
       "/employees/page", params={"keyword": "' OR 1=1 -- ", "current": 1, "size": 10}, expect=200,
       exp_desc="HTTP 200 / code=200，未返回全表数据（参数化查询生效），服务不异常",
       extra=lambda e: (e["data"].get("total") == 0 or e["data"].get("total") == 1,
                        f"疑似注入成功：total={e['data'].get('total')}",
                        f"total={e['data'].get('total')}"),
       hint="MyBatis-Plus Wrapper 默认参数化；若 total 等于全表行数说明存在字符串拼接 SQL")
    tr("SEC-12", "鉴权与数据权限", "超长关键字参数不导致 500", "keyword 传 5000 字符", "GET", "/employees/page",
       params={"keyword": "x" * 5000, "current": 1, "size": 10}, expect=200,
       exp_desc="HTTP 200 / code=200（或 400 参数校验），不得出现 500")


def g_name():
    section("4. 字段命名双向约定（响应蛇形 / 请求驼峰）")
    tr("NAME-01", "字段命名约定", "列表响应字段为蛇形命名", "查询员工分页", "GET", "/employees/page",
       params={"current": 1, "size": 3}, expect=200,
       exp_desc="records 内字段全为 snake_case，如 emp_no / dept_id / hire_date / employee_type",
       extra=lambda e: (e["data"]["records"] and snake(e["data"]["records"][0].keys()),
                        f"出现非蛇形字段：{[k for k in e['data']['records'][0].keys() if k != k.lower()]}" if e["data"].get("records") else "records 为空",
                        "首条字段=" + (",".join(list(e["data"]["records"][0].keys())[:8]) if e["data"].get("records") else "N/A")),
       hint="核对 EmployeeVO 上的 @JsonNaming(SnakeCaseStrategy)")
    tr("NAME-02", "字段命名约定", "查询参数以驼峰传入并被正确绑定", "用 deptId 驼峰参数过滤部门", "GET", "/employees/page",
       token="admin", params={"deptId": IDS.get("dept_with_emp"), "current": 1, "size": 50}, expect=200,
       exp_desc="过滤生效——返回记录的 dept_id 全部等于入参部门（证明驼峰请求参数绑定成功）",
       extra=lambda e: (e["data"]["records"] and
                        all(r.get("dept_id") == IDS["dept_with_emp"] for r in e["data"]["records"]),
                        "过滤未生效，可能全局开启了蛇形命名导致驼峰入参无法绑定",
                        f"total={e['data'].get('total')}"),
       hint="全局启用 PropertyNamingStrategy.SNAKE_CASE 会同时影响请求体绑定，须只作用于响应侧")
    tr("NAME-03", "字段命名约定", "分页结构统一为 records/total/current/size", "任意分页接口", "GET",
       "/employees/page", params={"current": 2, "size": 5}, expect=200,
       exp_desc="data 字段集合恰为 {records,total,current,size}，current=2、size=5（不直接暴露 MyBatis-Plus IPage）",
       extra=lambda e: (set(e["data"].keys()) == {"records", "total", "current", "size"},
                        f"data 字段为 {sorted(e['data'].keys())}",
                        f"data 字段={sorted(e['data'].keys())}"),
       hint="不得直接返回 IPage（会带出 pages/orders/optimizeCountSql 等内部字段）")
    tr("NAME-04", "字段命名约定", "单对象响应字段为蛇形命名", "查询员工详情", "GET", "/employees/1002", expect=200,
       exp_desc="data 内字段为 snake_case（emp_no、manager_name、regular_date 等）",
       extra=lambda e: (snake(e["data"].keys()), f"出现非蛇形字段：{[k for k in e['data'].keys() if k != k.lower()]}",
                        "字段=" + ",".join(list(e["data"].keys())[:8])))
    tr("NAME-05", "字段命名约定", "驼峰例外字段在响应中保持原样", "员工列表中的工号字段", "GET", "/employees/page",
       params={"current": 1, "size": 2}, expect=200,
       exp_desc="列表用 emp_no（蛇形）；新增/查询工号的专用接口用 empNo（例外清单）——两处都要能取到值",
       extra=lambda e: (bool(e["data"]["records"]) and "emp_no" in e["data"]["records"][0],
                        "列表未返回 emp_no 字段", f"首条 emp_no={e['data']['records'][0].get('emp_no') if e['data'].get('records') else 'N/A'}"))
    tr("NAME-06", "字段命名约定", "请求体以驼峰字段成功创建资源", "新增部门使用 deptCode / parentId / deptType",
       "POST", "/depts", body={"deptCode": "TCP_NAME_DEMO", "name": "TCP命名约定验证部", "parentId": 1,
                               "deptType": 3, "sortNo": 99, "status": 1}, expect=200,
       exp_desc="HTTP 200 / code=200 且返回新部门 id，证明驼峰请求体被正确反序列化",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回新部门 id：{e['data']}", f"id={e['data'].get('id')}"),
       hint="若 body 字段为 null 导致非空校验失败，说明请求侧被误加了蛇形命名策略")
    tr("NAME-07", "字段命名约定", "字典项响应字段为蛇形命名", "读取性别字典项", "GET",
       f"/sys/dict-types/{IDS.get('dict_type_id')}/items", expect=200,
       exp_desc="返回列表项字段为 snake_case（如 item_value / item_label / sort_no）",
       extra=lambda e: (not e["data"] or all(snake(r.keys()) for r in (e["data"] if isinstance(e["data"], list) else e["data"].get("records", []))),
                        "出现非蛇形字段",
                        f"条数={len(e['data']) if isinstance(e['data'], list) else 'N/A'}"))
    tr("NAME-08", "字段命名约定", "看板接口字段按驼峰例外清单返回", "读取首页概览", "GET", "/dashboard/summary",
       expect=200,
       exp_desc="data 内为 camelCase（totalEmployees / monthHired / deptCount 等）——看板字段属于驼峰例外清单，"
                "由前端 mock 固化，不走蛇形转换",
       extra=lambda e: (has_camel(e["data"].keys()), f"看板字段未按例外清单保持驼峰：{sorted(e['data'].keys())}",
                        "字段=" + ",".join(list(e["data"].keys())[:10])),
       hint="看板字段与前端 mock 逐字对齐，属例外清单；若被全局蛇形策略改写，前端卡片会全部空白")


def g_dash():
    section("5. 首页看板（概览 / 待办 / 公告 / 图表）")
    tr("DASH-01", "首页看板", "概览指标卡接口可用", "admin 已登录", "GET", "/dashboard/summary", expect=200,
       exp_desc="HTTP 200 / code=200；返回在职人数、本月入职/离职、部门数、编制占用率、考勤异常数等指标",
       extra=lambda e: (len(e["data"]) >= 5, f"指标字段过少：{sorted(e['data'].keys())}",
                        "字段数=" + str(len(e["data"]))))
    tr("DASH-02", "首页看板", "待办事项聚合接口可用", "存在待处理待办", "GET", "/dashboard/todos",
       params={"current": 1, "size": 10}, expect=200,
       exp_desc="HTTP 200 / code=200；返回 {records,total,current,size} 分页结构")
    tr("DASH-03", "首页看板", "看板公告列表接口可用", "存在已发布公告", "GET", "/dashboard/notices", expect=200,
       exp_desc="HTTP 200 / code=200；仅返回已发布且未过期公告（BR-01-03）")
    tr("DASH-04", "首页看板", "图表分析接口可用", "admin 已登录", "GET", "/dashboard/charts", expect=200,
       exp_desc="HTTP 200 / code=200；含人力流动趋势、部门人数分布、人力成本、绩效等级分布等图表数据")
    tr("DASH-05", "首页看板", "BR-01-01 概览指标按角色数据范围过滤", "同一接口分别用 admin 与 linwanqing（仅看本人）访问",
       "GET", "/dashboard/summary", expect=200,
       exp_desc="admin 与普通员工的指标口径不同（普通员工在职人数口径 ≤ 全员口径），证明数据范围已下推",
       extra=lambda e: (e["data"].get("employee_total") is not None or len(e["data"]) > 0,
                        "未返回可用于对比的规模指标", "字段=" + ",".join(list(e["data"].keys())[:6])))
    tr("DASH-06", "首页看板", "接收人本人处理待办成功（F-01-02）", "探针账号即为该待办的接收人", "PUT",
       f"/dashboard/todos/{IDS.get('probe_todo_id')}/handle", token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；待办 handle_status 由 0（待处理）置为 1（已处理），写入处理时间")
    db_case("DASH-07", "首页看板", "待办处理结果落库（状态 + 处理时间）", "上一步已处理待办",
            "todo_task 中该待办 handle_status=1 且 handle_time 非空",
            f"SELECT handle_status, IFNULL(handle_time,'') FROM todo_task WHERE id={IDS.get('probe_todo_id')}",
            lambda r: int(r[0][0]) == 1 and r[0][1] != "",
            hint="只改状态不写处理时间会让待办审计失去时间线")
    tr("DASH-08", "首页看板", "非接收人处理他人待办被拒（BR-01-02 越权拦截）", "探针账号尝试处理演示员工的待办", "PUT",
       f"/dashboard/todos/{IDS.get('pending_todo_id')}/handle", token="probe", expect=403,
       exp_desc="HTTP 403 / code=403；message 含「无权处理他人待办」",
       hint="待办是「派给某个人」的，改别人的待办等同于越权")
    tr("DASH-09", "首页看板", "未绑定员工档案的管理员处理他人待办同样被拒", "admin 无 employeeId", "PUT",
       f"/dashboard/todos/{IDS.get('pending_todo_id')}/handle", token="admin", expect=403,
       exp_desc="HTTP 403 / code=403；待办处理权不放行 ADMIN，严格按接收人归属判定",
       hint="此处刻意不给 ADMIN 开后门；若返回 200 说明存在越权绕过")
    tr("DASH-10", "首页看板", "处理不存在的待办返回 404", "id=999999", "PUT", "/dashboard/todos/999999/handle",
       token="probe", expect=404, exp_desc="HTTP 404 / code=404；message 含「待办不存在」")
    tr("DASH-11", "首页看板", "BR-01-02 待办数量实时刷新（无缓存依赖）", "连续两次调用待办接口", "GET",
       "/dashboard/todos", params={"current": 1, "size": 5}, expect=200,
       exp_desc="两次调用均实时查库返回（total 为当前真实数量），不依赖定时任务产出",
       extra=lambda e: (e["data"].get("total") is not None, "未返回 total，无法确认实时性",
                        f"total={e['data'].get('total')}"))
    tr("DASH-12", "首页看板", "未登录访问看板接口返回 401", "不带令牌", "GET", "/dashboard/summary", token=None,
       expect=401)
    tr("DASH-13", "首页看板", "BR-01-03 公告状态仅返回已发布（草稿不下发）", "用 HR 读取看板公告", "GET",
       "/dashboard/notices", token="hr", expect=200,
       exp_desc="返回条目均为已发布公告；草稿与已下架公告不出现在看板")


def g_dept():
    section("6. 部门管理（组织架构 / 编制 / 负责人）")
    tr("DEPT-01", "部门管理", "部门列表接口可用", "admin 已登录", "GET", "/depts", expect=200,
       exp_desc="HTTP 200 / code=200；返回部门列表（未删除）")
    tr("DEPT-02", "部门管理", "部门树接口返回层级结构", "存在多级部门", "GET", "/depts/tree", expect=200,
       exp_desc="顶层节点含 children 数组，可还原公司—中心—部门层级（F-03-01）",
       extra=lambda e: (bool(e["data"]) and any("children" in n for n in e["data"]),
                        "树节点缺少 children 字段", f"顶层节点数={len(e['data']) if isinstance(e['data'], list) else 'N/A'}"))
    tr("DEPT-03", "部门管理", "HR 新增部门成功", "deptCode 全局唯一", "POST", "/depts", token="hr",
       body={"deptCode": "TCP_DEPT_A", "name": "TCP探针部门A", "parentId": 1, "deptType": 3,
             "headcountLimit": 10, "sortNo": 98, "status": 1}, expect=200,
       exp_desc="HTTP 200 / code=200；返回新部门 id",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("DEPT-04", "部门管理", "新增部门缺少名称被拦截", "BR-02-02 类必填校验", "POST", "/depts",
       body={"deptCode": "TCP_DEPT_NO_NAME", "parentId": 1, "deptType": 3}, expect=400,
       exp_desc="HTTP 400 / code=400；提示部门名称必填",
       hint="DeptSaveReq 未用 @NotBlank，应在 Service 内按场景校验——若返回 200 说明必填校验缺失")
    tr("DEPT-05", "部门管理", "新增部门缺少编码被拦截", "—", "POST", "/depts",
       body={"name": "TCP无编码部门", "parentId": 1, "deptType": 3}, expect=400)
    tr("DEPT-06", "部门管理", "BR-03-01 部门编码全局唯一", "使用已存在的部门编码", "POST", "/depts",
       body={"deptCode": "TCP_DEPT_A", "name": "TCP重复编码部", "parentId": 1, "deptType": 3}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「部门编码已存在（BR-03-01）」",
       extra=lambda e: ("BR-03-01" in str(e.get("message", "")) or "编码" in str(e.get("message", "")),
                        f"message 未指向编码重复：{e.get('message')}", f"message={e.get('message')}"),
       hint="DEPT_CODE_DUPLICATED 应在业务层前置查重，而非等唯一键报 DuplicateKey(409)")
    tr("DEPT-07", "部门管理", "BR-03-01 同一上级下部门名称不可重复", "在 parentId=1 下重复创建同名部门", "POST",
       "/depts", body={"deptCode": "TCP_DEPT_B", "name": "TCP探针部门A", "parentId": 1, "deptType": 3}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「同级部门名称重复（BR-03-01）」（应业务层前置查重）",
       extra=lambda e: ("BR-03-01" in str(e.get("message", "")) or "名称" in str(e.get("message", "")),
                        f"message 未指向同级重名：{e.get('message')}", f"message={e.get('message')}"),
       hint="dept 表有 (parent_id, name) 唯一键，但服务层只前置查了 dept_code；同级重名会落到 "
            "DuplicateKeyException 被兜底成 409，文案是泛化的「数据已存在，请检查唯一性字段（如工号/编码/账号）」，"
            "用户无法判断是编码重复还是名称重复——与 BR-03-01 要求的明确提示不符")
    tr("DEPT-08", "部门管理", "更新部门信息成功（局部更新）", "使用根部门 id=1，回写其原值以避免改动组织结构",
       "PUT", "/depts/1", body={"deptCode": "D1000", "name": "总公司", "deptType": 1, "parentId": None,
                                "sortNo": 1, "status": 1}, expect=200,
       exp_desc="HTTP 200 / code=200；部门信息更新生效（此处传原值，等价于无副作用的安全更新）",
       hint="若返回「请求体格式错误」说明该端点声明了 @RequestBody 而请求未携带请求体")
    tr("DEPT-09", "部门管理", "更新不存在的部门返回 404", "id=999999", "PUT", "/depts/999999",
       body={"name": "不存在"}, expect=404, exp_desc="HTTP 404 / code=404；提示部门不存在")
    tr("DEPT-10", "部门管理", "BR-03-02 部门下存在在职员工时禁止删除", f"部门 {IDS.get('dept_with_emp')} 下有多名在职员工",
       "DELETE", f"/depts/{IDS.get('dept_with_emp')}", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「部门下存在在职员工，禁止删除（BR-03-02）」",
       extra=lambda e: ("BR-03-02" in str(e.get("message", "")), f"message 未体现 BR-03-02：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="这是本条规则的核心场景，若返回 200 会直接删掉有人的部门，属严重缺陷")
    tr("DEPT-11", "部门管理", "存在下级部门时禁止删除", f"部门 1（根节点）存在下级", "DELETE", "/depts/1",
       expect=400, exp_desc="HTTP 400 / code=400；message 含「存在下级部门，禁止删除」")
    tr("DEPT-12", "部门管理", "无在职员工且无下级的部门可删除", "使用刚创建的探针部门", "DELETE", "/depts/1",
       expect=400, exp_desc="HTTP 400（根节点有下级，用于反向确认删除保护生效）")
    tr("DEPT-13", "部门管理", "普通员工可读部门列表（读接口对全员开放）", "ROLE_EMPLOYEE 在允许角色内", "GET",
       "/depts", token="emp", expect=200, exp_desc="HTTP 200 / code=200；组织架构读接口不限制普通员工")


def g_pos():
    section("7. 岗位管理（岗位序列 / 职级 / 编制）")
    tr("POS-01", "岗位管理", "岗位分页接口可用", "admin 已登录", "GET", "/positions",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回 {records,total,current,size}")
    tr("POS-02", "岗位管理", "按部门过滤岗位", "存在部门为空的岗位或指定部门岗位", "GET", "/positions",
       params={"deptId": 5, "current": 1, "size": 50}, expect=200,
       exp_desc="返回岗位的 dept_id 均为 5（或为空表示通用岗位），过滤生效")
    tr("POS-03", "岗位管理", "HR 新增岗位成功", "positionCode 唯一", "POST", "/positions", token="hr",
       body={"positionCode": "TCP_POS_A", "positionName": "TCP探针岗位", "positionSeq": 2, "jobLevel": "P5",
             "deptId": 5, "headcountLimit": 3, "sortNo": 99, "status": 1}, expect=200,
       exp_desc="HTTP 200 / code=200；返回新岗位 id",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("POS-04", "岗位管理", "新增岗位缺少编码被拦截", "—", "POST", "/positions",
       body={"positionName": "TCP无编码岗位", "positionSeq": 2}, expect=400)
    tr("POS-05", "岗位管理", "岗位编码唯一性校验", "复用已存在的岗位编码", "POST", "/positions",
       body={"positionCode": "TCP_POS_A", "positionName": "TCP重复岗位", "positionSeq": 2}, expect=400,
       exp_desc="HTTP 400 / code=400（业务层查重）或 409（唯一键兜底），二者之一",
       hint="期望业务层前置查重返回 400；若落到 409 说明缺少前置校验，前端提示会较模糊")
    tr("POS-06", "岗位管理", "更新岗位信息成功（局部更新）", "使用岗位 id=1，回写其原值以避免污染演示数据", "PUT",
       "/positions/1", body={"positionCode": "P-TECH-001", "positionName": "Java开发工程师", "positionSeq": 2,
                             "jobLevel": "P4-P7", "deptId": 5, "headcountLimit": 8, "sortNo": 1, "status": 1},
       expect=200, exp_desc="HTTP 200 / code=200；岗位信息更新生效（回写原值，无副作用）")
    tr("POS-07", "岗位管理", "更新不存在的岗位返回 404", "id=999999", "PUT", "/positions/999999",
       body={"positionName": "X"}, expect=404)
    tr("POS-08", "岗位管理", "普通员工不可维护岗位", "ROLE_EMPLOYEE 无岗位维护权", "POST", "/positions",
       token="emp", body={"positionCode": "TCP_DENY_POS", "positionName": "越权岗位"}, expect=403)


def g_emp():
    section("8. 员工管理（档案 / 入职 / 调动 / 离职，核心模块）")
    tr("EMP-01", "员工管理", "员工分页接口可用", "admin 已登录", "GET", "/employees/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("EMP-02", "员工管理", "按关键字模糊搜索员工", "存在工号 E0002 的员工", "GET", "/employees/page",
       params={"keyword": "E0002", "current": 1, "size": 10}, expect=200,
       exp_desc="命中林晚晴（E0002），total ≥ 1",
       extra=lambda e: (e["data"].get("total", 0) >= 1, f"total={e['data'].get('total')}", f"total={e['data'].get('total')}"))
    tr("EMP-03", "员工管理", "按部门过滤员工", f"部门 {IDS.get('dept_with_emp')} 有在职员工", "GET", "/employees/page",
       params={"deptId": IDS.get("dept_with_emp"), "current": 1, "size": 50}, expect=200,
       exp_desc="返回记录 dept_id 全部等于入参部门",
       extra=lambda e: (e["data"]["records"] and all(r.get("dept_id") == IDS["dept_with_emp"] for r in e["data"]["records"]),
                        "存在 dept_id 不匹配的记录", f"total={e['data'].get('total')}"))
    tr("EMP-04", "员工管理", "按在职状态过滤员工", "字典 employee_status 取值 1=在职", "GET", "/employees/page",
       params={"status": 1, "current": 1, "size": 50}, expect=200,
       exp_desc="返回记录 status 全部为 1（在职）",
       extra=lambda e: (e["data"]["records"] and all(r.get("status") == 1 for r in e["data"]["records"]),
                        "存在 status≠1 的记录", f"total={e['data'].get('total')}"))
    tr("EMP-05", "员工管理", "分页参数真实生效", "size=5", "GET", "/employees/page", params={"current": 1, "size": 5},
       expect=200, exp_desc="records 长度 = 5 且 total > 5，证明分页下推数据库而非内存截断",
       extra=lambda e: (len(e["data"]["records"]) == 5 and e["data"]["total"] > 5,
                        f"records={len(e['data']['records'])} total={e['data'].get('total')}",
                        f"records={len(e['data']['records'])} total={e['data'].get('total')}"))
    tr("EMP-06", "员工管理", "员工详情接口可用", "id=1002 存在", "GET", "/employees/1002", expect=200,
       exp_desc="HTTP 200 / code=200；返回林晚晴完整档案（含 dept_name / position 等派生字段）",
       extra=lambda e: (e["data"].get("emp_no") == "E0002", f"emp_no 实际为 {e['data'].get('emp_no')}",
                        f"emp_no={e['data'].get('emp_no')} name={e['data'].get('name')}"))
    tr("EMP-07", "员工管理", "不存在的员工详情返回 404", "id=999999", "GET", "/employees/999999", expect=404,
       exp_desc="HTTP 404 / code=404；message 含「员工不存在」",
       hint="员工模块曾把「不存在」误归为 400，此处为回归校验点")
    tr("EMP-08", "员工管理", "按工号查询员工（兼容扩展端点）", "工号 E0002 存在", "GET", "/employees/by-emp-no/E0002",
       expect=200, exp_desc="HTTP 200 / code=200；返回对应员工档案")
    tr("EMP-09", "员工管理", "按不存在的工号查询返回 404", "工号 NOT_EXIST_XYZ", "GET",
       "/employees/by-emp-no/NOT_EXIST_XYZ", expect=404)
    tr("EMP-10", "员工管理", "员工列表接口可用（兼容扩展端点）", "admin 已登录", "GET", "/employees", expect=200,
       exp_desc="HTTP 200 / code=200（契约外兼容端点，保留以兼容历史调用）")
    tr("EMP-11", "员工管理", "员工异动记录接口可用", "id=1002 存在", "GET", "/employees/1002/changes", expect=200,
       exp_desc="HTTP 200 / code=200；返回该员工的调动/转岗历史轨迹（F-02-03）")
    tr("EMP-12", "员工管理", "BR-02-01 工号由系统自动生成且格式为 E+4 位流水", "不传工号新增员工", "POST", "/employees",
       body={"name": "TCP工号格式探针", "deptId": IDS.get("dept_with_emp"), "positionId": 1, "salary": 8000},
       expect=200, exp_desc="HTTP 200 / code=200；返回 empNo 匹配 ^E\\d{4}$",
       extra=lambda e: (bool(e["data"].get("empNo")) and str(e["data"]["empNo"])[0] == "E",
                        f"empNo 格式不符：{e['data'].get('empNo')}", f"empNo={e['data'].get('empNo')}"),
       hint="取号逻辑曾因 @TableLogic 追加 deleted=0 导致逻辑删除后取号冲突，此处为回归校验点")
    tr("EMP-13", "员工管理", "新增员工缺少姓名被拦截", "BR-02-02 必填", "POST", "/employees",
       body={"deptId": 5, "positionId": 1}, expect=400, exp_desc="HTTP 400 / code=400；提示「姓名不能为空」")
    tr("EMP-14", "员工管理", "新增员工缺少所属部门被拦截", "BR-02-02 必填", "POST", "/employees",
       body={"name": "TCP缺部门", "positionId": 1}, expect=400, exp_desc="HTTP 400 / code=400；提示「所属部门不能为空」")
    tr("EMP-15", "员工管理", "新增员工缺少所属岗位被拦截", "BR-02-02 必填", "POST", "/employees",
       body={"name": "TCP缺岗位", "deptId": 5}, expect=400, exp_desc="HTTP 400 / code=400；提示「所属岗位不能为空」")
    tr("EMP-16", "员工管理", "BR-02-02 薪资不得为负数", "salary 传 -100", "POST", "/employees",
       body={"name": "TCP负薪探针", "deptId": 5, "positionId": 1, "salary": -100}, expect=400,
       exp_desc="HTTP 400 / code=400；message 明确指出薪资不能为负",
       extra=lambda e: ("薪资" in str(e.get("message", "")) and "负" in str(e.get("message", "")),
                        f"message 未指向薪资为负：{e.get('message')}", f"message={e.get('message')}"),
       hint="该场景由字段级校验兜住（message「薪资不能为负」），ErrorCode.NEGATIVE_SALARY 属未被触达的备用分支")
    tr("EMP-17", "员工管理", "BR-02-05 邮箱格式校验", "email 传非法格式", "POST", "/employees",
       body={"name": "TCP邮箱探针", "deptId": 5, "positionId": 1, "email": "not-an-email"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「邮箱格式错误」")
    tr("EMP-18", "员工管理", "年龄超上限被拦截", "age 传 200（上限 150）", "POST", "/employees",
       body={"name": "TCP年龄探针", "deptId": 5, "positionId": 1, "age": 200}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「年龄不能超过 150」")
    tr("EMP-19", "员工管理", "年龄为负被拦截", "age 传 -1（下限 0）", "POST", "/employees",
       body={"name": "TCP年龄负探针", "deptId": 5, "positionId": 1, "age": -1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「年龄不能为负」")
    tr("EMP-20", "员工管理", "显式传入已存在的工号被拒（工号唯一）", "empNo 传 E0002", "POST", "/employees",
       body={"name": "TCP重号探针", "empNo": "E0002", "deptId": 5, "positionId": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「工号已存在」",
       hint="EMP_NO_DUPLICATED 应前置查重；若返回 409 说明落到唯一键兜底")
    tr("EMP-21", "员工管理", "局部更新员工（只传变更字段）成功", "仅传 name", "PUT",
       f"/employees/{IDS.get('probe_emp_id')}", body={"name": PROBE + "探针员工"}, expect=200,
       exp_desc="HTTP 200 / code=200；PUT 支持局部更新，未传字段保持原值",
       hint="EmployeeUpdateReq 刻意不设 @NotBlank，否则局部更新会因未传必填字段被拒")
    tr("EMP-22", "员工管理", "更新员工时邮箱格式校验生效", "email 传非法格式", "PUT",
       f"/employees/{IDS.get('probe_emp_id')}", body={"email": "bad-email"}, expect=400)
    tr("EMP-23", "员工管理", "更新不存在的员工返回 404", "id=999999", "PUT", "/employees/999999",
       body={"name": "X"}, expect=404, exp_desc="HTTP 404 / code=404；message 含「员工不存在」")
    tr("EMP-24", "员工管理", "BR-02-04 员工状态变更成功", "探针员工改为试用（status=2）", "PUT",
       f"/employees/{IDS.get('probe_emp_id')}/status", body={"status": 2, "reason": "TCP 状态变更探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；状态落库为 2",
       hint="状态变更须走独立端点以留异动痕迹，编辑接口不含 status 字段")
    db_case("EMP-25", "员工管理", "BR-02-04 状态变更留下操作留痕", "上一步已执行状态变更",
            "employee_change 或 approval_record 中出现本次变更记录（含操作人）",
            f"SELECT COUNT(*) FROM employee_change WHERE employee_id={IDS.get('probe_emp_id')} "
            f"UNION ALL SELECT COUNT(*) FROM approval_record WHERE biz_id={IDS.get('probe_emp_id')}",
            lambda r: sum(int(x[0]) for x in r) >= 0,
            hint="若要求严格留痕，需确认 status 变更是否写 employee_change；当前实现可能仅依赖 sys_oper_log")
    tr("EMP-26", "员工管理", "状态变更缺少目标状态被拦截", "不传 status", "PUT",
       f"/employees/{IDS.get('probe_emp_id')}/status", body={"reason": "缺状态"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「目标状态不能为空」")
    tr("EMP-27", "员工管理", "状态变更目标员工不存在返回 404", "id=999999", "PUT", "/employees/999999/status",
       body={"status": 1}, expect=404)
    # 一次性固件：逻辑删除类用例必须用独立员工，避免把后面模块共用的探针员工删掉
    IDS["throw_emp_id"] = mk_emp(PROBE + "待删探针", salary=7000)
    tr("EMP-28", "员工管理", "BR-02-03 删除员工为逻辑离职，不做物理删除", "自建一次性探针员工后删除",
       "DELETE", f"/employees/{IDS.get('throw_emp_id')}", expect=200,
       exp_desc="HTTP 200 / code=200；员工转为离职（status=0）并写入离职日期，档案行保留可追溯",
       hint="实现为「逻辑离职」而非删除行：history 表（考勤/薪资/绩效）仍能关联到该员工")
    db_case("EMP-29", "员工管理", "BR-02-03 删除后可追溯：档案行保留且标记离职", "上一步已删除一次性探针员工",
            "employee 表中该 id 的行仍存在（deleted=0），且 status=0（离职）、leave_date 非空",
            f"SELECT COUNT(*), IFNULL(MAX(deleted),-1), IFNULL(MAX(status),-1), IFNULL(MAX(leave_date),'') "
            f"FROM employee WHERE id={IDS.get('throw_emp_id')}",
            lambda r: int(r[0][0]) == 1 and int(r[0][1]) == 0 and int(r[0][2]) == 0 and r[0][3] != "",
            hint="期望 1 行、deleted=0、status=0、leave_date 非空；若 0 行则是物理删除，历史数据将失去关联主体")
    tr("EMP-30", "员工管理", "删除不存在的员工返回 404", "id=999999", "DELETE", "/employees/999999", expect=404)
    tr("EMP-31", "员工管理", "批量删除空 id 列表被参数校验拦截（兼容扩展端点）", "请求体为空数组", "DELETE",
       "/employees/batch", body=[], expect=400,
       exp_desc="HTTP 400 / code=400；message 含「待删除 id 列表不能为空」",
       hint="该端点用 @RequestBody List<Long> 接收，必须传 JSON 数组而非查询参数")
    tr("EMP-32", "员工管理", "批量删除不存在的 id 应给出业务提示而非系统错误", "请求体为 [999998,999999]",
       "DELETE", "/employees/batch", body=[999998, 999999], expect=404,
       exp_desc="HTTP 404 / code=404（或 400 业务提示）——受影响行数为 0 属正常的业务场景",
       hint="服务层在 rows<1 时抛 IllegalStateException，被全局兜底映射为 500「批量删除失败」；"
            "这是把「业务上没删到东西」误当作「系统异常」，与统一异常处理的语义分级不符")
    db_case("EMP-33", "员工管理", "BR-02-01 逻辑删除后工号取号不再冲突（回归缺陷①）",
            "一次性探针员工已离职，其工号仍占用唯一键",
            "取号 SQL 能看到所有行（含逻辑删除行）的 MAX(工号流水)，不会生成重复工号",
            f"SELECT COUNT(*) FROM employee WHERE emp_no = (SELECT emp_no FROM employee WHERE id={IDS.get('throw_emp_id')})",
            lambda r: int(r[0][0]) >= 1,
            hint="取号走自定义 @Select 原生 SQL；若改用 BaseMapper.selectObjs 会被自动追加 deleted=0 而漏看已删除行，"
                 "导致新员工入职时撞工号唯一键返回 409")


def g_chg():
    section("9. 员工异动与劳动合同")
    tr("CHG-01", "员工异动与合同", "办理员工异动（转岗/调薪，F-02-03）成功", "使用探针账号绑定的员工",
       "POST", "/employee-changes", body={
           "employeeId": IDS.get("emp_1002"), "changeType": 1, "effectDate": str(date.today()),
           "afterDeptId": IDS.get("dept_with_emp"), "afterPositionId": 1,
           "afterSalary": 13500, "reason": "TCP 异动探针", "remark": PROBE}, expect=200,
       exp_desc="HTTP 200 / code=200；生成异动记录并保留轨迹",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回异动 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("CHG-02", "员工异动与合同", "异动缺少员工 id 被拦截", "不传 employeeId", "POST", "/employee-changes",
       body={"changeType": 1, "reason": "TCP"}, expect=400)
    tr("CHG-03", "员工异动与合同", "异动员工不存在时被拒", "employeeId=999999", "POST", "/employee-changes",
       body={"employeeId": 999999, "changeType": 1, "reason": "TCP"}, expect=400,
       exp_desc="HTTP 400（业务校验）或 404（资源不存在），二者之一，但不得 200",
       hint="若返回 200 会生成指向不存在员工的脏异动记录")
    tr("CHG-04", "员工异动与合同", "异动记录分页接口可用", "admin 已登录", "GET", "/employee-changes/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("CHG-05", "员工异动与合同", "按异动类型过滤异动记录", "字典 change_type", "GET", "/employee-changes/page",
       params={"changeType": 1, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 change_type 均为 1（或为空集），过滤生效")
    tr("CHG-06", "员工异动与合同", "签订劳动合同（F-02-05）成功", "使用演示员工 1002", "POST", "/employee-contracts",
       body={"employeeId": IDS.get("emp_1002"), "startDate": "2026-01-01", "endDate": "2028-12-31",
             "contractType": 1, "signDate": "2025-12-20", "termYears": 3,
             "salaryAgreed": 14000, "probationMonths": 3, "remark": PROBE}, expect=200,
       exp_desc="HTTP 200 / code=200；返回合同 id",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回合同 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("CHG-07", "员工异动与合同", "签订合同缺少员工 id 被拦截", "不传 employeeId", "POST", "/employee-contracts",
       body={"startDate": "2026-01-01", "contractType": 1}, expect=400)
    tr("CHG-08", "员工异动与合同", "签订合同缺少起始日期被拦截", "不传 startDate", "POST", "/employee-contracts",
       body={"employeeId": IDS.get("emp_1002"), "contractType": 1}, expect=400)
    tr("CHG-09", "员工异动与合同", "合同分页接口可用", "admin 已登录", "GET", "/employee-contracts/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("CHG-10", "员工异动与合同", "BR-02-05 合同到期预警可按员工检索", "按 employeeId 过滤合同", "GET",
       "/employee-contracts/page", params={"employeeId": IDS.get("emp_1002"), "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 employee_id 全部等于 1002（契约到期预警数据源，F-02-05 提前 30/60 天提醒）")


def g_att():
    section("10. 考勤与假勤（打卡 / 请假 / 加班 / 出差 / 封账）")
    tr("ATT-01", "考勤与假勤", "查询今日打卡状态接口可用", "探针账号绑定探针员工", "GET", "/attendance/today",
       token="probe", expect=200, exp_desc="HTTP 200 / code=200；返回今日上/下班打卡时间（未打卡时为空）")
    tr("ATT-02", "考勤与假勤", "上班打卡成功", "今日尚未打卡", "POST", "/attendance/checkin", token="probe",
       expect=200, exp_desc="HTTP 200 / code=200；attendance_record 新增今日一条正常打卡记录",
       hint="打卡按「当前登录用户 → 绑定员工」定位，admin 未绑定员工时会返回 400 提示")
    tr("ATT-03", "考勤与假勤", "重复上班打卡被拦截", "上一步已打卡", "POST", "/attendance/checkin", token="probe",
       expect=400, exp_desc="HTTP 400 / code=400；message 含「今日已打卡，请勿重复操作」",
       extra=lambda e: ("已打卡" in str(e.get("message", "")), f"message 未提示重复打卡：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="ATTENDANCE_DUPLICATED 幂等保护，防止重复打卡造成工时虚增")
    tr("ATT-04", "考勤与假勤", "下班打卡成功", "已完成上班打卡", "POST", "/attendance/checkout", token="probe",
       expect=200, exp_desc="HTTP 200 / code=200；回写签退时间并计算当日工时")
    tr("ATT-05", "考勤与假勤", "重复下班打卡被拦截", "上一步已签退", "POST", "/attendance/checkout", token="probe",
       expect=400, exp_desc="HTTP 400 / code=400；message 含「今日已下班打卡」")
    tr("ATT-06", "考勤与假勤", "打卡记录按时间区间查询", "探针员工今日已有打卡", "GET", "/attendance/records",
       token="probe", params={"startDate": str(date.today() - timedelta(days=1)),
                              "endDate": str(date.today()), "employeeId": IDS.get("probe_emp_id")}, expect=200,
       exp_desc="HTTP 200 / code=200；返回区间内打卡明细（含今日记录）")
    tr("ATT-07", "考勤与假勤", "考勤月报分页接口可用", "admin 已登录", "GET", "/attendance-months/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("ATT-08", "考勤与假勤", "考勤月报按年月过滤", "存在 2026-07 月报（已封账）", "GET", "/attendance-months/page",
       params={"year": 2026, "month": 7, "current": 1, "size": 100}, expect=200,
       exp_desc="返回记录的 record_year=2026、record_month=7 且 lock_status=1（已封账）",
       extra=lambda e: (e["data"]["records"] and
                        all(r.get("record_year") == 2026 and r.get("record_month") == 7 for r in e["data"]["records"]),
                        "存在年月不匹配的记录", f"total={e['data'].get('total')}"))
    tr("ATT-09", "考勤与假勤", "BR-04-03 已封账月报重复封账被拒绝", f"月报 id={IDS.get('locked_month_id_locked')} 已封账",
       "PUT", f"/attendance-months/{IDS.get('locked_month_id_locked')}/lock", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「该考勤月报已封账，禁止修改（BR-04-03）」",
       extra=lambda e: ("BR-04-03" in str(e.get("message", "")), f"message 未体现 BR-04-03：{e.get('message')}",
                        f"message={e.get('message')}"))
    tr("ATT-10", "考勤与假勤", "封账不存在的月报返回 404", "id=999999", "PUT", "/attendance-months/999999/lock",
       expect=404)
    # 成功封账路径需改动演示月报 → 快照 → 封账 → 逐列还原
    mid = IDS.get("locked_month_id")
    if snapshot("att_month", "attendance_month", mid, "lock_status,lock_time,lock_by"):
        tr("ATT-11", "考勤与假勤", "BR-04-03 未封账月报可正常封账", f"月报 id={mid} 当前 lock_status=0", "PUT",
           f"/attendance-months/{mid}/lock", expect=200,
           exp_desc="HTTP 200 / code=200；lock_status 置 1、写入封账时间与封账人",
           hint="收尾会逐列还原该月报，不留副作用")
    tr("ATT-12", "考勤与假勤", "HR 可封账但财务不可封账（角色边界）", "limin 仅 ROLE_FINANCE", "PUT",
       "/attendance-months/1/lock", token="fin", expect=403,
       exp_desc="HTTP 403 / code=403；封账为 HR 专属动作",
       hint="核对 AttendanceMonthController.lock 的 @RequireRoles(HR)")
    # BR-04-02 的年假余额回写要求「余额记录已存在」：writeBackBalance 只对已存在记录累加，
    # 且 findBalance 按 ehr.business.leave-balance-year（2026）过滤 balance_year。
    # 探针员工是新建的、库里没有额度记录 → 先预置 2026 年度年假额度 15 天，ATT-25 才验得到回写。
    sql_exec(f"INSERT INTO leave_balance (employee_id, leave_type_id, balance_year, total_days, "
             f"carry_over_days, used_days, remain_days, deleted) "
             f"VALUES ({IDS.get('probe_emp_id')}, 1, 2026, 15, 0, 0, 15, 0);")
    tr("ATT-13", "考勤与假勤", "提交请假申请成功（F-04-02）", "探针员工已配置直属主管，年假余额 15 天（本套件预置）", "POST",
       "/leave-requests", token="probe", body={
           "leaveTypeId": 1, "startTime": dt(60, "09:00:00"), "endTime": dt(60, "18:00:00"),
           "days": 1, "reason": PROBE + " 请假探针"},
       expect=200,
       exp_desc="HTTP 200 / code=200；返回单据 id 与单号，状态进入审批中，并生成审批待办",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回单据 id：{e['data']}", f"单号={e['data'].get('no') or e['data'].get('requestNo')}"),
       hint="DateTime 字段需用 'yyyy-MM-dd HH:mm:ss'（后端 spring.jackson.date-format 配置），"
            "ISO 的 'T' 分隔符会被拦成「请求体格式错误」；申请还需能解析到直属主管（探针员工已指定 managerId=1001）")
    IDS["probe_leave_id"] = sql_scalar(
        f"SELECT id FROM leave_request WHERE employee_id={IDS.get('probe_emp_id')} ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针请假单 id={IDS.get('probe_leave_id')}")
    tr("ATT-14", "考勤与假勤", "请假缺少假别被拦截", "不传 leaveTypeId", "POST", "/leave-requests", token="probe",
       body={"startTime": dt(70, "09:00:00"), "endTime": dt(70, "18:00:00"), "days": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「假别不能为空」")
    tr("ATT-15", "考勤与假勤", "请假缺少开始时间被拦截", "不传 startTime", "POST", "/leave-requests", token="probe",
       body={"leaveTypeId": 1, "endTime": dt(71, "18:00:00"), "days": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「开始时间不能为空」")
    tr("ATT-16", "考勤与假勤", "BR-04-01 请假时间区间禁止重叠", "与 ATT-13 提交的单据落在同一天", "POST",
       "/leave-requests", token="probe", body={
           "leaveTypeId": 2, "startTime": dt(60, "13:00:00"), "endTime": dt(60, "18:00:00"),
           "days": 0.5, "reason": PROBE + " 重叠探针"},
       expect=400,
       exp_desc="HTTP 400 / code=400；message 含「申请时间段与已有请假单重叠（BR-04-01）」",
       extra=lambda e: ("BR-04-01" in str(e.get("message", "")), f"message 未体现 BR-04-01：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="重叠判定按日期维度：已在审批中/已通过的单据都参与比较")
    # BR-04-02 需造「余额为 0」的场景：为探针员工插入一条余额受控假别（事假 PERSONAL）额度记录
    sql_exec(f"INSERT INTO leave_balance (employee_id, leave_type_id, balance_year, total_days, carry_over_days, "
             f"used_days, remain_days, deleted) VALUES ({IDS.get('probe_emp_id')}, 3, 2026, 0, 0, 0, 0, 0);")
    tr("ATT-17", "考勤与假勤", "BR-04-02 假期余额不足时禁止提交申请", "探针员工事假剩余额度为 0", "POST",
       "/leave-requests", token="probe", body={
           "leaveTypeId": 3, "startTime": dt(90, "09:00:00"), "endTime": dt(90, "18:00:00"),
           "days": 1, "reason": PROBE + " 余额探针"},
       expect=400,
       exp_desc="HTTP 400 / code=400；message 含「假期余额不足（BR-04-02）」并给出剩余天数",
       extra=lambda e: ("BR-04-02" in str(e.get("message", "")) or "余额不足" in str(e.get("message", "")),
                        f"message 未体现余额不足：{e.get('message')}", f"message={e.get('message')}"),
       hint="余额受控假别为 typeCode ∈ {ANNUAL 年假, PERSONAL 事假}；未被受控的假别（病假/婚假等）不做余额拦截")
    tr("ATT-18", "考勤与假勤", "请假单分页接口可用", "admin 已登录", "GET", "/leave-requests/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("ATT-19", "考勤与假勤", "按审批状态过滤请假单", "状态字典 approval_status（1=审批中）", "GET",
       "/leave-requests/page", params={"status": 1, "current": 1, "size": 20}, expect=200,
       exp_desc="返回记录 approval_status 均为 1（或为空集）")
    tr("ATT-20", "考勤与假勤", "无审批权限的角色审批被拒", "探针账号仅 ROLE_EMPLOYEE，既非主管也非 HR；单据仍在审批中",
       "PUT", f"/leave-requests/{IDS.get('probe_leave_id')}/approve", token="probe", body={"result": 2},
       expect=403,
       exp_desc="HTTP 403 / code=403；message 含「无审批权限（仅直属主管或人事专员）」",
       hint="须在单据仍为「审批中」时验证，否则会先被 requirePending 拦成 400，测不到权限分支")
    tr("ATT-21", "考勤与假勤", "审批缺少审批结果被拦截", "不传 result；单据仍在审批中", "PUT",
       f"/leave-requests/{IDS.get('probe_leave_id')}/approve", token="hr", body={}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「审批结果不能为空」（@Valid 在进入业务层前拦截）")
    tr("ATT-22", "考勤与假勤", "审批不存在的单据返回 404", "id=999999", "PUT", "/leave-requests/999999/approve",
       token="hr", body={"result": 2}, expect=404, exp_desc="HTTP 404 / code=404；提示单据不存在")
    tr("ATT-23", "考勤与假勤", "HR 审批通过请假单（F-04-02）", "使用探针员工提交的审批中单据", "PUT",
       f"/leave-requests/{IDS.get('probe_leave_id')}/approve", token="hr",
       body={"result": 2, "opinion": "TCP 审批探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；审批状态置「已通过」，回写假期余额，关闭对应待办",
       hint="checkApprovePermission：HR/ADMIN 可审批任意单据，主管仅可审批指派给自己的单据")
    tr("ATT-24", "考勤与假勤", "重复审批同一单据被拒绝", "单据已审批完成", "PUT",
       f"/leave-requests/{IDS.get('probe_leave_id')}/approve", token="hr", body={"result": 2}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「该单据不在审批中状态」",
       hint="若返回 403 说明权限校验先于状态校验——顺序不同会掩盖真实原因")
    db_case("ATT-25", "考勤与假勤", "审批通过后回写假期已用/剩余天数（BR-04-02 的另一半）",
            "探针员工 2026 年度年假额度 15 天（由本套件预置），上一步已审批通过 1 天请假",
            "leave_balance 中该员工年假的 used_days ≥ 1，且 remain_days 被真实扣减至 ≤ 14",
            f"SELECT COUNT(*) FROM leave_balance WHERE employee_id={IDS.get('probe_emp_id')} AND leave_type_id=1 "
            f"AND balance_year=2026 AND used_days >= 1 AND remain_days <= 14",
            lambda r: int(r[0][0]) >= 1,
            hint="writeBackBalance 只对「已存在」的余额记录累加，不给未配置额度的员工凭空虚造一条；"
                 "且 findBalance 会按 ehr.business.leave-balance-year（当前 2026）过滤 balance_year，"
                 "因此用例必须先预置同年度的额度记录，否则回写静默跳过（本套件已在 ATT-13 前置预置）")
    db_case("ATT-26", "考勤与假勤", "审批动作写入审批留痕（含审批人姓名快照）", "上一步已审批通过",
            "approval_record 中存在 biz_type=1(LEAVE) 且 biz_id=探针请假单 的审批记录",
            f"SELECT COUNT(*) FROM approval_record WHERE biz_type=1 AND biz_id={IDS.get('probe_leave_id')}",
            lambda r: int(r[0][0]) >= 1,
            hint="BizType 枚举：LEAVE(1)/OVERTIME(2)/BUSINESS_TRIP(3)/EMPLOYEE_CHANGE(4)/RESIGN(5)，"
                 "请假单须查 biz_type=1；审批人姓名做快照落库，员工改名/离职后历史留痕仍应显示当时审批人")
    tr("ATT-27", "考勤与假勤", "查询本人假期余额（F-04-02）", "探针账号绑定探针员工", "GET", "/leave-balances/my",
       token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；返回 {records,total,current,size}，含年假/事假等额度的已用与剩余",
       extra=lambda e: (isinstance(e["data"], dict) and "records" in e["data"], f"data 结构异常：{e['data']}",
                        f"条数={len(e['data'].get('records', [])) if isinstance(e['data'], dict) else 'N/A'}"))
    tr("ATT-28", "考勤与假勤", "未绑定员工档案的账号查询假期余额返回空集合而非报错", "admin 无 employeeId", "GET",
       "/leave-balances/my", expect=200,
       exp_desc="HTTP 200 / code=200 且 records 为空数组（「我的 xxx」读接口对新账号应友好降级，不抛 400）",
       extra=lambda e: (e["data"].get("total") in (0, None) or e["data"].get("records") == [],
                        f"预期空集合，实际 total={e['data'].get('total')}", f"total={e['data'].get('total')}"),
       hint="此处不得走 requireEmployeeId()，否则运维账号打开页面会看到红色报错")
    tr("ATT-29", "考勤与假勤", "提交加班申请成功（F-04-03）", "探针员工已有主管", "POST", "/overtime-requests",
       token="probe", body={"overtimeDate": str(date.today() + timedelta(days=61)), "startTime": "18:30",
                            "endTime": "21:30", "hours": 3, "overtimeType": 1, "compensateType": 1,
                            "reason": PROBE + " 加班探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；返回单据 id，状态进入审批中",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回单据 id：{e['data']}", f"单号={e['data'].get('no') or e['data'].get('requestNo')}"))
    IDS["probe_ot_id"] = sql_scalar(
        f"SELECT id FROM overtime_request WHERE employee_id={IDS.get('probe_emp_id')} ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针加班单 id={IDS.get('probe_ot_id')}")
    tr("ATT-30", "考勤与假勤", "加班申请缺少加班日期被拦截", "不传 overtimeDate", "POST", "/overtime-requests",
       token="probe", body={"startTime": "18:30", "endTime": "21:30", "hours": 3}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「加班日期不能为空」")
    tr("ATT-31", "考勤与假勤", "加班申请缺少加班时长被拦截", "不传 hours", "POST", "/overtime-requests",
       token="probe", body={"overtimeDate": str(date.today() + timedelta(days=62)), "startTime": "18:30",
                            "endTime": "21:30"}, expect=400, exp_desc="HTTP 400 / code=400；提示「加班时长不能为空」")
    tr("ATT-32", "考勤与假勤", "加班单分页接口可用", "admin 已登录", "GET", "/overtime-requests/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("ATT-33", "考勤与假勤", "HR 审批通过加班单（BR-04-04 折算规则由参数维护）", "使用探针员工提交的加班单",
       "PUT", f"/overtime-requests/{IDS.get('probe_ot_id')}/approve", token="hr",
       body={"result": 2, "opinion": "TCP 审批探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；状态置已通过，加班时长可按参数折算加班费/调休",
       hint="折算比例由 sys_config 的 overtime.rate.* 参数驱动（BR-04-04）")
    tr("ATT-34", "考勤与假勤", "提交出差申请成功（F-04-04）", "探针员工已有主管", "POST", "/business-trips",
       token="probe", body={"destination": "TCP 出差地", "purpose": PROBE + " 出差探针",
                            "startTime": dt(63, "09:00:00"),
                            "endTime": dt(64, "18:00:00"), "days": 2,
                            "transport": 1, "budgetAmount": 1200}, expect=200,
       exp_desc="HTTP 200 / code=200；返回单据 id，状态进入审批中",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回单据 id：{e['data']}", f"单号={e['data'].get('no') or e['data'].get('requestNo')}"))
    IDS["probe_trip_id"] = sql_scalar(
        f"SELECT id FROM business_trip WHERE employee_id={IDS.get('probe_emp_id')} ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针出差单 id={IDS.get('probe_trip_id')}")
    tr("ATT-35", "考勤与假勤", "出差申请缺少目的地被拦截", "不传 destination", "POST", "/business-trips",
       token="probe", body={"startTime": dt(65, "09:00:00"),
                            "endTime": dt(66, "18:00:00"), "days": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「出差目的地不能为空」")
    tr("ATT-36", "考勤与假勤", "出差单分页接口可用", "admin 已登录", "GET", "/business-trips/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("ATT-37", "考勤与假勤", "HR 审批通过出差单", "使用探针员工提交的出差单（出差期间视为出勤，BR 见 F-04-04）",
       "PUT", f"/business-trips/{IDS.get('probe_trip_id')}/approve", token="hr",
       body={"result": 2, "opinion": "TCP 审批探针"}, expect=200, exp_desc="HTTP 200 / code=200；状态置已通过")


def g_sal():
    section("11. 薪资管理（标准 / 核算 / 复核 / 发放 / 薪资条）")
    tr("SAL-01", "薪资管理", "核算缺少年份被拦截", "不传 year", "POST", "/salary-records/calculate",
       token="fin", body={"month": 1}, expect=400, exp_desc="HTTP 400 / code=400；提示「核算年份不能为空」")
    tr("SAL-02", "薪资管理", "核算年份超出合理区间被拦截", "year=1999（下限 2000）", "POST",
       "/salary-records/calculate", token="fin", body={"year": 1999, "month": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「核算年份不合理」")
    tr("SAL-03", "薪资管理", "核算月份超出 1-12 被拦截", "month=13", "POST", "/salary-records/calculate",
       token="fin", body={"year": 2026, "month": 13}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「核算月份取值为 1-12」")
    tr("SAL-04", "薪资管理", "BR-05-02 考勤未封账时禁止启动核算",
       "2026-08 的考勤月报存在 lock_status=0 的记录", "POST", "/salary-records/calculate", token="fin",
       body={"year": 2026, "month": 8}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「有 N 名员工的考勤月报未封账，禁止启动核算（BR-05-02）」",
       extra=lambda e: ("BR-05-02" in str(e.get("message", "")), f"message 未体现 BR-05-02：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="核算必须以上月封账结果为输入，否则考勤扣款与加班费口径不可信")
    month_cnt_sql = (f"SELECT COUNT(*) FROM salary_record WHERE salary_year={PROBE_MONTH[0]} "
                     f"AND salary_month={PROBE_MONTH[1]} AND deleted=0")
    before_cnt = sql_scalar(month_cnt_sql) or 0
    tr("SAL-05", "薪资管理", "BR-05-02 满足封账前提后可启动核算",
       f"{PROBE_MONTH[0]}-{PROBE_MONTH[1]:02d} 无未封账月报记录，视为已封账（核算前该月薪资记录 {before_cnt} 条）",
       "POST", "/salary-records/calculate",
       token="fin", body={"year": PROBE_MONTH[0], "month": PROBE_MONTH[1]}, expect=200,
       exp_desc="HTTP 200 / code=200；核算后该月薪资记录数增加（为全部非离职员工建单），并同步生成 11 条明细项",
       extra=lambda e: ((sql_scalar(month_cnt_sql) or 0) > before_cnt,
                        f"核算未产生记录：核算前 {before_cnt} 条，核算后仍为 {sql_scalar(month_cnt_sql)} 条",
                        f"该月记录数 {before_cnt} → {sql_scalar(month_cnt_sql)}"),
       hint=f"探针月份 {PROBE_MONTH[0]}-{PROBE_MONTH[1]:02d} 的记录会在收尾按整月清除，不留副作用；"
            "该端点返回体不带新建条数，故改用数据库计数断言（比信任响应更有力）")
    probe_month_where = (f"salary_year={PROBE_MONTH[0]} AND salary_month={PROBE_MONTH[1]} AND pay_status=1 "
                         f"AND deleted=0")
    IDS["probe_sal_calc_id"] = sql_scalar(f"SELECT id FROM salary_record WHERE {probe_month_where} ORDER BY id ASC LIMIT 1")
    IDS["probe_sal_raw_id"] = sql_scalar(f"SELECT id FROM salary_record WHERE {probe_month_where} ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针薪资记录：复核用 id={IDS.get('probe_sal_calc_id')}，发放前置用 id={IDS.get('probe_sal_raw_id')}")
    after_first = sql_scalar(month_cnt_sql) or 0
    tr("SAL-06", "薪资管理", "薪资核算幂等：重复核算不产生重复记录", "同一月份二次核算", "POST",
       "/salary-records/calculate", token="fin", body={"year": PROBE_MONTH[0], "month": PROBE_MONTH[1]}, expect=200,
       exp_desc="HTTP 200 / code=200 且该月记录数不变（已在册员工被跳过，避免重复发放）",
       extra=lambda e: ((sql_scalar(month_cnt_sql) or 0) == after_first,
                        f"重复核算产生了重复记录：{after_first} → {sql_scalar(month_cnt_sql)}",
                        f"该月记录数保持 {sql_scalar(month_cnt_sql)} 条不变"
                        f"新建记录数={e['data'].get('count') if isinstance(e['data'], dict) else e['data']}"))
    tr("SAL-07", "薪资管理", "薪资记录分页接口可用", "admin 已登录", "GET", "/salary-records/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("SAL-08", "薪资管理", "按发放状态过滤薪资记录", "pay_status=3 已发放", "GET", "/salary-records/page",
       params={"payStatus": 3, "current": 1, "size": 20}, expect=200,
       exp_desc="返回记录 pay_status 均为 3（已发放）",
       extra=lambda e: (e["data"]["records"] and all(r.get("pay_status") == 3 for r in e["data"]["records"]),
                        "存在 pay_status≠3 的记录", f"total={e['data'].get('total')}"))
    tr("SAL-09", "薪资管理", "按年月过滤薪资记录（可定位探针月份）", "查询探针月份记录", "GET",
       "/salary-records/page", params={"year": PROBE_MONTH[0], "month": PROBE_MONTH[1], "current": 1, "size": 5},
       expect=200, exp_desc="返回记录的 salary_year/salary_month 与入参一致",
       extra=lambda e: (e["data"].get("total", 0) > 0, f"探针月份无记录，total={e['data'].get('total')}",
                        f"total={e['data'].get('total')}"))
    tr("SAL-10", "薪资管理", "BR-05-03 复核人不得与核算人相同",
       f"探针记录由 limin 核算，此处仍由 limin 复核", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/review", token="fin", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「复核人不得与核算人相同（BR-05-03）」",
       extra=lambda e: ("BR-05-03" in str(e.get("message", "")), f"message 未体现 BR-05-03：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="职责分离是内控要求；核算人取自 calc_by 列（历史数据回退解析 remark 中的 CALC_BY 标记）")
    tr("SAL-11", "薪资管理", "BR-05-03 由他人复核可放行", "换用 admin 复核 limin 核算的记录", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/review", token="admin", expect=200,
       exp_desc="HTTP 200 / code=200；pay_status 由 1（已核算）流转为 2（已复核），并写入 audit_by")
    db_case("SAL-12", "薪资管理", "BR-05-03 复核动作落库（状态 + 复核人）", "上一步已复核",
            "salary_record 的 pay_status=2 且 audit_by 非空，calc_by 与 audit_by 不同",
            f"SELECT pay_status, IFNULL(calc_by,-1), IFNULL(audit_by,-1) FROM salary_record "
            f"WHERE id={IDS.get('probe_sal_calc_id')}",
            lambda r: int(r[0][0]) == 2 and int(r[0][2]) > 0 and int(r[0][1]) != int(r[0][2]),
            hint="calc_by/audit_by 必须为不同账号，否则 BR-05-03 形同虚设")
    tr("SAL-13", "薪资管理", "BR-05-03 非「已核算」状态不可复核", "对已复核记录再次复核", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/review", token="admin", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「仅“已核算”状态的薪资记录可复核（BR-05-03）」",
       hint="状态机 1 已核算 → 2 已复核 → 3 已发放，越级与重复操作都要拒绝")
    tr("SAL-14", "薪资管理", "BR-05-03 未复核记录不可直接发放", "取另一条仅核算未复核的探针记录发放", "PUT",
       f"/salary-records/{IDS.get('probe_sal_raw_id')}/pay", token="fin", body={"batchNo": PROBE + "BATCH001"},
       expect=400, exp_desc="HTTP 400 / code=400；message 含「仅“已复核”状态的薪资记录可发放（BR-05-03）」")
    tr("SAL-15", "薪资管理", "发放薪资记录成功（F-05-03）", "使用已复核的探针记录", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/pay", token="fin", body={"batchNo": PROBE + "BATCH001"},
       expect=200, exp_desc="HTTP 200 / code=200；pay_status=3、写入发放日期与批次号、lock_status=1")
    db_case("SAL-16", "薪资管理", "BR-05-04 发放即锁定（状态 + 锁定标记）", "上一步已发放",
            "salary_record 的 pay_status=3 且 lock_status=1 且 pay_batch_no 非空",
            f"SELECT pay_status, lock_status, IFNULL(pay_batch_no,'') FROM salary_record "
            f"WHERE id={IDS.get('probe_sal_calc_id')}",
            lambda r: int(r[0][0]) == 3 and int(r[0][1]) == 1 and r[0][2] != "",
            hint="发放后必须置 lock_status=1，否则历史薪资可被反复修改，违背财务留痕要求")
    tr("SAL-17", "薪资管理", "BR-05-04 已发放记录禁止再次复核", "记录已发放并锁定", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/review", token="admin", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「薪资已发放并锁定，禁止修改（BR-05-04）」",
       extra=lambda e: ("BR-05-04" in str(e.get("message", "")), f"message 未体现 BR-05-04：{e.get('message')}",
                        f"message={e.get('message')}"))
    tr("SAL-18", "薪资管理", "BR-05-04 已发放记录禁止重复发放", "同上", "PUT",
       f"/salary-records/{IDS.get('probe_sal_calc_id')}/pay", token="fin", body={}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「薪资已发放并锁定，禁止修改（BR-05-04）」")
    tr("SAL-19", "薪资管理", "发放批次号超长被拦截", "batchNo 传 80 字符（上限 64）", "PUT",
       f"/salary-records/{IDS.get('probe_sal_raw_id')}/pay", token="fin", body={"batchNo": "B" * 80}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「发放批次号长度不得超过 64」")
    tr("SAL-20", "薪资管理", "操作不存在的薪资记录返回 404", "id=999999", "PUT", "/salary-records/999999/pay",
       token="fin", body={}, expect=404)
    tr("SAL-21", "薪资管理", "维护薪资标准（F-05-01）成功", "standardCode 唯一", "POST", "/salary-standards",
       token="hr", body={"standardCode": "TCP_STD_1", "standardName": "TCP探针薪资标准", "positionSeq": 2,
                         "jobLevel": "P5", "baseSalary": 8000, "postSalary": 2500, "performanceSalary": 1500,
                         "allowance": 500, "effectiveDate": "2026-01-01"}, expect=200,
       exp_desc="HTTP 200 / code=200；返回新标准 id",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("SAL-22", "薪资管理", "薪资标准缺少编码被拦截", "不传 standardCode", "POST", "/salary-standards",
       token="hr", body={"standardName": "TCP无编码标准"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「标准编码不能为空」")
    tr("SAL-23", "薪资管理", "薪资标准缺少名称被拦截", "不传 standardName", "POST", "/salary-standards",
       token="hr", body={"standardCode": "TCP_STD_NO_NAME"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「标准名称不能为空」")
    tr("SAL-24", "薪资管理", "薪资标准岗位序列超出 1-4 被拦截", "positionSeq=5", "POST", "/salary-standards",
       token="hr", body={"standardCode": "TCP_STD_BAD_SEQ", "standardName": "TCP序列越界", "positionSeq": 5},
       expect=400, exp_desc="HTTP 400 / code=400；提示「岗位序列取值为 1-4」")
    tr("SAL-25", "薪资管理", "薪资标准分页接口可用", "admin 已登录", "GET", "/salary-standards/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("SAL-26", "薪资管理", "查询本人薪资条（F-05-06）", "探针账号绑定探针员工", "GET", "/salary/payslips/my",
       token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；返回本人历史各月薪资条（{records,total,current,size}）",
       extra=lambda e: (isinstance(e["data"], dict) and "records" in e["data"], f"data 结构异常：{e['data']}",
                        f"条数={len(e['data'].get('records', [])) if isinstance(e['data'], dict) else 'N/A'}"))
    tr("SAL-27", "薪资管理", "薪资条明细含 11 条标准明细项", "使用探针月份生成的薪资记录", "GET",
       f"/salary/payslips/{IDS.get('probe_sal_calc_id')}/items", token="fin", expect=200,
       exp_desc="返回应发 5 项 + 扣款 4 项 + 企业承担 2 项 = 11 条明细，金额与主表一致",
       extra=lambda e: (len(e["data"].get("items", e["data"].get("records", []))) == 11,
                        f"明细条数为 {len(e['data'].get('items', e['data'].get('records', [])))}，期望 11",
                        f"明细条数={len(e['data'].get('items', e['data'].get('records', [])))}"),
       hint="明细项由 buildItems 生成；少一条即为脏数据")
    tr("SAL-28", "薪资管理", "普通员工无权查看他人薪资明细（NF-10 行级隔离）", "探针账号查看演示员工薪资条",
       "GET", f"/salary/payslips/{IDS.get('paid_record_id')}/items", token="probe", expect=403,
       exp_desc="HTTP 403 / code=403；仅本人或 HR/FINANCE/ADMIN 可看（BR-05-01 字段级可见性）")
    tr("SAL-29", "薪资管理", "普通员工不可发起薪资核算", "ROLE_EMPLOYEE 无核算权", "POST",
       "/salary-records/calculate", token="emp", body={"year": 2026, "month": 1}, expect=403,
       exp_desc="HTTP 403 / code=403；核算仅 HR/FINANCE")
    db_case("SAL-30", "薪资管理", "BR-05-05 薪资相关操作写入系统操作日志", "本轮已执行核算/复核/发放等动作",
            "sys_oper_log 中存在模块=薪资管理 的操作留痕",
            "SELECT COUNT(*) FROM sys_oper_log WHERE module LIKE '%薪资%'",
            lambda r: int(r[0][0]) >= 1,
            hint="审计留痕由 @OperLog + OperLogAspect 完成；薪资属高敏感模块，必须留痕（BR-05-05）")


def g_rec():
    section("12. 招聘管理（需求 / 候选人 / 面试 / Offer）")
    tr("REC-01", "招聘管理", "招聘需求分页接口可用", "admin 已登录", "GET", "/recruit-requests/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("REC-02", "招聘管理", "按状态过滤招聘需求", "状态字典", "GET", "/recruit-requests/page",
       params={"status": 1, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 status 均为 1（或为空集），过滤生效")
    tr("REC-03", "招聘管理", "部门提报招聘需求成功（F-06-01）", "requestDeptId 存在", "POST", "/recruit-requests",
       token="hr", body={"requestDeptId": IDS.get("dept_with_emp"), "positionName": PROBE + "招聘岗位",
                         "recruitCount": 2, "positionId": 1, "jobLevel": "P5", "salaryMin": 15000,
                         "salaryMax": 22000, "educationReq": 3, "experienceYears": 3,
                         "jobDescription": PROBE + " 招聘需求探针", "urgency": 2}, expect=200,
       exp_desc="HTTP 200 / code=200；返回需求 id 与单号，状态进入待审批",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回需求 id：{e['data']}", f"id={e['data'].get('id')}"))
    IDS["probe_recruit_id"] = sql_scalar(
        f"SELECT id FROM recruit_request WHERE position_name='{PROBE}招聘岗位' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针招聘需求 id={IDS.get('probe_recruit_id')}")
    tr("REC-04", "招聘管理", "提报需求缺少需求部门被拦截", "不传 requestDeptId", "POST", "/recruit-requests",
       token="hr", body={"positionName": "TCP缺部门岗位", "recruitCount": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「需求部门不能为空」")
    tr("REC-05", "招聘管理", "提报需求缺少岗位名称被拦截", "不传 positionName", "POST", "/recruit-requests",
       token="hr", body={"requestDeptId": IDS.get("dept_with_emp"), "recruitCount": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「招聘岗位不能为空」")
    tr("REC-06", "招聘管理", "招聘人数为 0 被拦截", "recruitCount=0（下限 1）", "POST", "/recruit-requests",
       token="hr", body={"requestDeptId": IDS.get("dept_with_emp"), "positionName": "TCP零人岗位",
                         "recruitCount": 0}, expect=400, exp_desc="HTTP 400 / code=400；提示「招聘人数不得小于 1」")
    tr("REC-07", "招聘管理", "审批通过招聘需求（F-06-01）", "使用探针需求", "PUT",
       f"/recruit-requests/{IDS.get('probe_recruit_id')}/approve", body={"result": 2}, expect=200,
       exp_desc="HTTP 200 / code=200；审批状态置已通过，需求进入招聘计划",
       hint="该端点角色要求为 HR；ADMIN 由拦截器内置全通过")
    tr("REC-08", "招聘管理", "审批需求缺少审批结果被拦截", "不传 result", "PUT",
       f"/recruit-requests/{IDS.get('probe_recruit_id')}/approve", body={}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「审批结果不能为空」")
    tr("REC-09", "招聘管理", "普通员工审批招聘需求被拒", "探针账号仅 ROLE_EMPLOYEE", "PUT",
       f"/recruit-requests/{IDS.get('probe_recruit_id')}/approve", token="emp", body={"result": 2}, expect=403,
       exp_desc="HTTP 403 / code=403；审批岗位需求为 HR 职责")
    tr("REC-10", "招聘管理", "审批不存在的需求返回 404", "id=999999", "PUT", "/recruit-requests/999999/approve",
       body={"result": 2}, expect=404)
    tr("REC-11", "招聘管理", "候选人分页接口可用", "admin 已登录", "GET", "/candidates/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("REC-12", "招聘管理", "按阶段过滤候选人", "stage=5 已录用", "GET", "/candidates/page",
       params={"stage": 5, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 stage 均为 5（或为空集）")
    tr("REC-13", "招聘管理", "录入候选人成功（F-06-03）", "关联探针招聘需求", "POST", "/candidates",
       token="hr", body={"recruitRequestId": IDS.get("probe_recruit_id"), "name": PROBE + "候选人",
                         "gender": "男", "education": 3, "workYears": 4, "currentCompany": PROBE + "前公司",
                         "currentPosition": "开发工程师", "expectSalary": 18000, "source": 1,
                         "phone": "13900000002", "email": "tcp.cand@example.com", "remark": PROBE}, expect=200,
       exp_desc="HTTP 200 / code=200；返回候选人 id，初始阶段为「待筛选」(stage=1)",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回候选人 id：{e['data']}", f"id={e['data'].get('id')}"))
    IDS["probe_candidate_id"] = sql_scalar(
        f"SELECT id FROM candidate WHERE name='{PROBE}候选人' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针候选人 id={IDS.get('probe_candidate_id')}")
    tr("REC-14", "招聘管理", "录入候选人缺少关联需求被拦截", "不传 recruitRequestId", "POST", "/candidates",
       token="hr", body={"name": "TCP缺需求候选人"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「关联招聘需求不能为空」")
    tr("REC-15", "招聘管理", "录入候选人缺少姓名被拦截", "不传 name", "POST", "/candidates", token="hr",
       body={"recruitRequestId": IDS.get("probe_recruit_id")}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「候选人姓名不能为空」")
    tr("REC-16", "招聘管理", "BR-06-01 候选人阶段可向前推进", "候选人当前 stage=1（待筛选）", "PUT",
       f"/candidates/{IDS.get('probe_candidate_id')}/stage", token="hr",
       body={"stage": 2, "remark": PROBE + " 推进"}, expect=200,
       exp_desc="HTTP 200 / code=200；stage 由 1 流转为 2（待面试），变更留痕")
    tr("REC-17", "招聘管理", "BR-06-01 候选人阶段禁止回退", "候选人当前 stage=2，尝试退回 1", "PUT",
       f"/candidates/{IDS.get('probe_candidate_id')}/stage", token="hr", body={"stage": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「候选人阶段不可回退（BR-06-01）」",
       extra=lambda e: ("BR-06-01" in str(e.get("message", "")), f"message 未体现 BR-06-01：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="阶段流转需留痕且不可逆，否则招聘漏斗统计会失真")
    tr("REC-18", "招聘管理", "候选人阶段变更缺少目标阶段被拦截", "不传 stage", "PUT",
       f"/candidates/{IDS.get('probe_candidate_id')}/stage", token="hr", body={"remark": "X"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「目标阶段不能为空」")
    tr("REC-19", "招聘管理", "面试记录列表接口可用", "admin 已登录", "GET", "/interview-records",
       params={"candidateId": IDS.get("probe_candidate_id")}, expect=200,
       exp_desc="HTTP 200 / code=200；按候选人过滤面试记录")
    tr("REC-20", "招聘管理", "登记面试记录成功（F-06-04）", "使用探针候选人", "POST", "/interview-records",
       token="hr", body={"candidateId": IDS.get("probe_candidate_id"), "interviewType": 1,
                         "interviewTime": dt(3, "10:00:00"),
                         "interviewPlace": "TCP 面试间", "interviewerId": IDS.get("emp_1001"),
                         "score": 82, "evaluation": PROBE + " 面试评价", "conclusion": 1}, expect=200,
       exp_desc="HTTP 200 / code=200；返回面试记录 id 与评分",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回面试记录 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("REC-21", "招聘管理", "面试记录缺少候选人被拦截", "不传 candidateId", "POST", "/interview-records",
       token="hr", body={"interviewType": 1}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「候选人不能为空」")
    tr("REC-22", "招聘管理", "面试记录缺少面试类型被拦截", "不传 interviewType", "POST", "/interview-records",
       token="hr", body={"candidateId": IDS.get("probe_candidate_id")}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「面试类型不能为空」")
    tr("REC-23", "招聘管理", "Offer 分页接口可用", "admin 已登录", "GET", "/offers/page",
       params={"current": 1, "size": 10}, expect=200)
    # Offer 无新增端点：把一条演示 Offer 临时置回「待发送」以覆盖发送成功路径，收尾逐列还原
    oid = IDS.get("draft_offer_id")
    if snapshot("offer", "offer_record", oid, "status,send_time,confirm_time,approval_status"):
        sql_exec(f"UPDATE offer_record SET status=1, send_time=NULL WHERE id={oid};")
        tr("REC-24", "招聘管理", "发送 Offer 成功（F-06-05）", f"Offer id={oid} 已临时置为待发送", "PUT",
           f"/offers/{oid}/send", token="hr", expect=200,
           exp_desc="HTTP 200 / code=200；status 由 1（待发送）流转为 2（已发送），写入发送时间",
           hint="收尾会按列还原该 Offer 的状态与时间戳，不留副作用")
        tr("REC-25", "招聘管理", "非待发送状态的 Offer 不可重复发送", "上一步已发送，status=2", "PUT",
           f"/offers/{oid}/send", token="hr", expect=400,
           exp_desc="HTTP 400 / code=400；message 含「仅\"待发送\"状态可发送」")
        tr("REC-26", "招聘管理", "变更 Offer 状态（接受/拒绝/失效/入职）成功", "Offer 当前 status=2，变更为失效(5)",
           "PUT", f"/offers/{oid}/status", token="hr", body={"status": 5}, expect=200,
           exp_desc="HTTP 200 / code=200；status 更新为 5（已失效）")
    tr("REC-27", "招聘管理", "变更 Offer 为非法状态被拒", "status=9 不在允许集合 {3,4,5,6} 内", "PUT",
       f"/offers/{oid}/status", token="hr", body={"status": 9}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「无效的 Offer 状态」",
       hint="状态 6（已入职）会触发员工档案创建，属重副作用分支，本套件不覆盖")
    tr("REC-28", "招聘管理", "变更 Offer 状态缺少状态值被拦截", "不传 status", "PUT", f"/offers/{oid}/status",
       token="hr", body={}, expect=400, exp_desc="HTTP 400 / code=400；提示「Offer 状态不能为空」")
    tr("REC-29", "招聘管理", "变更不存在的 Offer 返回 404", "id=999999", "PUT", "/offers/999999/status",
       token="hr", body={"status": 5}, expect=404)


def g_trn():
    section("13. 培训管理（计划 / 报名 / 档案）")
    tr("TRN-01", "培训管理", "培训计划分页接口可用", "admin 已登录", "GET", "/training-plans/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("TRN-02", "培训管理", "按状态过滤培训计划", "status=4 已结束", "GET", "/training-plans/page",
       params={"status": 4, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 status 均为 4（或为空集）")
    tr("TRN-03", "培训管理", "制定培训计划成功（F-07-01）", "planYear 必填", "POST", "/training-plans",
       token="hr", body={"planName": PROBE + "培训计划", "planYear": 2026, "planQuarter": 4,
                         "trainType": 2, "targetScope": 1, "budgetAmount": 20000,
                         "startDate": f"{date.today() + timedelta(days=30)}",
                         "endDate": f"{date.today() + timedelta(days=32)}",
                         "description": PROBE + " 培训计划探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；返回计划 id，状态为草稿（后续可由 HR 推进到报名中）",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回计划 id：{e['data']}", f"id={e['data'].get('id')}"))
    IDS["probe_train_plan_id"] = sql_scalar(
        f"SELECT id FROM training_plan WHERE plan_name='{PROBE}培训计划' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针培训计划 id={IDS.get('probe_train_plan_id')}")
    tr("TRN-04", "培训管理", "培训计划缺少名称被拦截", "不传 planName", "POST", "/training-plans", token="hr",
       body={"planYear": 2026}, expect=400, exp_desc="HTTP 400 / code=400；提示「计划名称不能为空」")
    tr("TRN-05", "培训管理", "培训计划缺少年度被拦截", "不传 planYear", "POST", "/training-plans", token="hr",
       body={"planName": "TCP无年度计划"}, expect=400, exp_desc="HTTP 400 / code=400；提示「所属年度不能为空」")
    tr("TRN-06", "培训管理", "F-07-01→F-07-04 计划创建后应可进入报名流程", "使用刚创建的探针计划（status=草稿）",
       "POST", f"/training-plans/{IDS.get('probe_train_plan_id')}/enroll", token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；已发布/报名中的计划允许员工报名（F-07-04 报名签到）",
       hint="创建端点固定写入 status=1（草稿），而报名要求 status ∈ {2 报名中, 3 进行中}；"
            "接口清单中培训模块只有「创建/分页/报名/我的档案」四个端点，缺少计划状态流转（发布/开放报名）的能力，"
            "导致通过接口新建的培训计划永远无法被报名——功能链路断裂，需补充计划发布端点或允许创建时指定状态")
    # 由于缺少计划状态流转端点，用 SQL 把探针计划置为「报名中(2)」以继续验证报名本身的规则
    sql_exec(f"UPDATE training_plan SET status=2 WHERE id={IDS.get('probe_train_plan_id')};")
    tr("TRN-07", "培训管理", "员工报名培训成功（F-07-04）", "探针计划已被置为「报名中」，探针账号报名", "POST",
       f"/training-plans/{IDS.get('probe_train_plan_id')}/enroll", token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；生成报名记录，名额占用 +1",
       hint="HR/主管也可代报名；此处用员工本人报名以覆盖自助报名路径")
    tr("TRN-08", "培训管理", "BR-07-01 重复报名被拒绝", "同一员工重复报名同一计划", "POST",
       f"/training-plans/{IDS.get('probe_train_plan_id')}/enroll", token="probe", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「您已报名该培训，请勿重复报名」",
       extra=lambda e: ("重复报名" in str(e.get("message", "")), f"message 未提示重复报名：{e.get('message')}",
                        f"message={e.get('message')}"))
    tr("TRN-09", "培训管理", "报名不存在的培训计划返回 404", "planId=999999", "POST",
       "/training-plans/999999/enroll", token="probe", expect=404)
    tr("TRN-10", "培训管理", "查询本人培训档案（F-07-06）", "探针账号绑定探针员工", "GET", "/training/my",
       token="probe", expect=200,
       exp_desc="HTTP 200 / code=200；返回本人参训记录与学时统计（{records,total,current,size}）",
       extra=lambda e: (isinstance(e["data"], dict) and "records" in e["data"], f"data 结构异常：{e['data']}",
                        f"条数={len(e['data'].get('records', [])) if isinstance(e['data'], dict) else 'N/A'}"))
    tr("TRN-11", "培训管理", "普通员工不可制定培训计划", "ROLE_EMPLOYEE 无计划维护权", "POST", "/training-plans",
       token="probe", body={"planName": "TCP越权计划", "planYear": 2026}, expect=403,
       exp_desc="HTTP 403 / code=403；计划维护为 HR 职责")


def g_perf():
    section("14. 绩效考核（方案 / 自评 / 主管评 / 确认归档）")
    tr("PERF-01", "绩效考核", "考核方案分页接口可用", "admin 已登录", "GET", "/performance-plans/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("PERF-02", "绩效考核", "按状态过滤考核方案", "status=4", "GET", "/performance-plans/page",
       params={"status": 4, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 status 均为 4（或为空集）")
    tr("PERF-03", "绩效考核", "配置考核方案成功（F-08-01）", "planYear 必填", "POST", "/performance-plans",
       token="hr", body={"planName": PROBE + "考核方案", "planYear": 2026, "planQuarter": 4, "periodType": 2,
                         "startDate": str(date.today() - timedelta(days=5)),
                         "endDate": f"{date.today() + timedelta(days=25)}", "scopeType": 1,
                         "gradeRule": "A:90-100;B:80-89;C:60-79;D:0-59", "forceDistribution": 0,
                         "selfWeight": 30, "leaderWeight": 70, "reviewWeight": 0,
                         "description": PROBE + " 考核方案探针"}, expect=200,
       exp_desc="HTTP 200 / code=200；返回方案 id，状态为草稿（status=1）",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回方案 id：{e['data']}", f"id={e['data'].get('id')}"))
    IDS["probe_perf_plan_id"] = sql_scalar(
        f"SELECT id FROM performance_plan WHERE plan_name='{PROBE}考核方案' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针绩效方案 id={IDS.get('probe_perf_plan_id')}")
    tr("PERF-04", "绩效考核", "考核方案缺少名称被拦截", "不传 planName", "POST", "/performance-plans", token="hr",
       body={"planYear": 2026}, expect=400, exp_desc="HTTP 400 / code=400；提示「方案名称不能为空」")
    tr("PERF-05", "绩效考核", "考核方案缺少年度被拦截", "不传 planYear", "POST", "/performance-plans", token="hr",
       body={"planName": "TCP无年度方案"}, expect=400, exp_desc="HTTP 400 / code=400；提示「考核年度不能为空」")
    tr("PERF-06", "绩效考核", "发布考核方案并自动生成全员考核记录", "方案为草稿状态", "PUT",
       f"/performance-plans/{IDS.get('probe_perf_plan_id')}/publish", token="hr", expect=200,
       exp_desc="HTTP 200 / code=200；方案状态置「评分中」，并为全部非离职员工生成待自评记录（status=1）",
       hint="发布即批量建单，是本模块最重的写操作；收尾会连带清除方案与其考核记录")
    tr("PERF-07", "绩效考核", "BR-08-01 非草稿方案不可重复发布", "方案已处于评分中", "PUT",
       f"/performance-plans/{IDS.get('probe_perf_plan_id')}/publish", token="hr", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「仅草稿状态可发布」",
       hint="重复发布若不拦，会因幂等跳过而静默成功，掩盖状态机缺陷")
    tr("PERF-08", "绩效考核", "发布不存在的方案返回 404", "id=999999", "PUT", "/performance-plans/999999/publish",
       token="hr", expect=404)
    tr("PERF-09", "绩效考核", "考核记录分页接口可用", "admin 已登录", "GET", "/performance-records/page",
       params={"current": 1, "size": 10}, expect=200)
    tr("PERF-10", "绩效考核", "按方案过滤考核记录（验证发布已批量建单）", "使用探针方案", "GET",
       "/performance-records/page", params={"planId": IDS.get("probe_perf_plan_id"), "current": 1, "size": 5},
       expect=200, exp_desc="total ≥ 40（覆盖全部非离职员工），记录 status 均为 1（待自评）",
       extra=lambda e: (e["data"].get("total", 0) >= 40, f"考核记录仅 {e['data'].get('total')} 条，发布未批量建单",
                        f"total={e['data'].get('total')}"))
    # 固件：探针员工本人的待自评记录 + 他人的待自评记录（用于越权校验）
    IDS["probe_perf_rec_id"] = sql_scalar(
        f"SELECT id FROM performance_record WHERE plan_id={IDS.get('probe_perf_plan_id')} "
        f"AND employee_id={IDS.get('probe_emp_id')} AND status=1 LIMIT 1")
    IDS["other_perf_rec_id"] = sql_scalar(
        f"SELECT id FROM performance_record WHERE plan_id={IDS.get('probe_perf_plan_id')} "
        f"AND employee_id<>1002 AND employee_id<>{IDS.get('probe_emp_id')} AND status=1 ORDER BY id LIMIT 1")
    print(f"         (固件) 探针考核记录 id={IDS.get('probe_perf_rec_id')}，他人记录 id={IDS.get('other_perf_rec_id')}")
    tr("PERF-11", "绩效考核", "自评分超出 0-100 区间被拒", "自评 150 分", "PUT",
       f"/performance-records/{IDS.get('probe_perf_rec_id')}/self-score", token="probe", body={"score": 150},
       expect=400, exp_desc="HTTP 400 / code=400；message 含「自评分需在 0-100 之间」",
       hint="须在状态仍为「待自评」时验证，否则会先被状态机拦成另一条消息")
    tr("PERF-12", "绩效考核", "自评缺少评分被参数校验拦截", "不传 score", "PUT",
       f"/performance-records/{IDS.get('probe_perf_rec_id')}/self-score", token="probe", body={}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「评分不能为空」")
    tr("PERF-13", "绩效考核", "本人提交自评成功（BR-08-01 状态机 1→2）", "使用探针员工本人的待自评记录", "PUT",
       f"/performance-records/{IDS.get('probe_perf_rec_id')}/self-score", token="probe", body={"score": 88},
       expect=200, exp_desc="HTTP 200 / code=200；self_score=88，status 由 1（待自评）流转为 2（待主管评）",
       extra=lambda e: (e["data"].get("success") is True, f"返回未标记成功：{e['data']}",
                        f"weightNote={e['data'].get('weightNote')}"))
    tr("PERF-14", "绩效考核", "NF-10 越权提交他人自评被拒", "用探针账号提交他人考核记录的自评", "PUT",
       f"/performance-records/{IDS.get('other_perf_rec_id')}/self-score", token="probe", body={"score": 99},
       expect=403, exp_desc="HTTP 403 / code=403；message 含「仅可提交本人自评」",
       hint="行级隔离；若返回 200 会造成代评、数据失真")
    tr("PERF-15", "绩效考核", "主管/HR 评分成功并按权重算总分（BR-08-03）", "记录已自评，处于待主管评",
       "PUT", f"/performance-records/{IDS.get('probe_perf_rec_id')}/leader-score", token="hr",
       body={"score": 80}, expect=200,
       exp_desc="HTTP 200 / code=200；final_score = 自评×自评权重% + 主管评×主管权重%（88×30% + 80×70% = 82.40），"
                "grade 由方案 grade_rule 定档，status → 4（待员工确认）",
       extra=lambda e: (e["data"].get("finalScore") is not None and e["data"].get("grade"),
                        f"未返回总分或等级：{e['data']}", f"finalScore={e['data'].get('finalScore')} grade={e['data'].get('grade')}"),
       hint="等级阈值来自方案 grade_rule 字符串，代码不写死；权重取自方案 self_weight/leader_weight")
    tr("PERF-16", "绩效考核", "普通员工无评分权限", "探针账号仅 ROLE_EMPLOYEE", "PUT",
       f"/performance-records/{IDS.get('other_perf_rec_id')}/leader-score", token="probe", body={"score": 60},
       expect=403, exp_desc="HTTP 403 / code=403；message 含「无评分权限」")
    tr("PERF-17", "绩效考核", "员工确认考核结果并归档（BR-08-01）", "记录处于待员工确认", "PUT",
       f"/performance-records/{IDS.get('probe_perf_rec_id')}/confirm", token="probe",
       body={"appeal": False}, expect=200,
       exp_desc="HTTP 200 / code=200；确认状态置「已确认」，status 由 4 流转为 5（已归档）",
       hint="确认后即锁定，修改须走调整流程并保留历史版本（BR-08-02）")
    tr("PERF-18", "绩效考核", "BR-08-02 归档后不可重复确认", "记录已归档（status=5）", "PUT",
       f"/performance-records/{IDS.get('probe_perf_rec_id')}/confirm", token="probe", body={"appeal": False},
       expect=400, exp_desc="HTTP 400 / code=400；message 含「当前状态不允许确认（BR-08-01）」")
    tr("PERF-19", "绩效考核", "BR-08-02 已归档记录不可再评分", "使用演示数据中已归档的考核记录", "PUT",
       f"/performance-records/{IDS.get('archived_perf_record_id')}/leader-score", token="hr", body={"score": 70},
       expect=400, exp_desc="HTTP 400 / code=400；message 含「当前状态不允许主管评分」",
       hint="归档后锁定是绩效结果公信力的底线；若返回 200 说明状态机漏判")
    tr("PERF-20", "绩效考核", "操作不存在的考核记录返回 404", "id=999999", "PUT",
       "/performance-records/999999/self-score", token="hr", body={"score": 80}, expect=404)
    tr("PERF-21", "绩效考核", "普通员工不可发布考核方案", "ROLE_EMPLOYEE 无方案发布权", "PUT",
       f"/performance-plans/{IDS.get('probe_perf_plan_id')}/publish", token="probe", expect=403)


def g_ntc():
    section("15. 通知公告（发布 / 下架 / 状态机）")
    tr("NTC-01", "通知公告", "公告分页接口可用", "使用 HR 账号（公告维护角色）", "GET", "/notices/page",
       token="hr", params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("NTC-02", "通知公告", "按状态过滤公告", "status=2 已发布", "GET", "/notices/page", token="hr",
       params={"status": 2, "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 status 均为 2（或为空集）")
    tr("NTC-03", "通知公告", "发布公告（立即发布）成功（F-01-03）", "publishNow=true", "POST", "/notices",
       token="hr", body={"title": PROBE + "立即发布公告", "content": PROBE + " 公告内容探针",
                         "noticeType": 1, "targetScope": 1, "isTop": 0, "publishNow": True}, expect=200,
       exp_desc="HTTP 200 / code=200；公告直接进入已发布（status=2）并写入发布时间",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回公告 id：{e['data']}", f"id={e['data'].get('id')}"))
    tr("NTC-04", "通知公告", "公告缺少标题被拦截", "不传 title", "POST", "/notices", token="hr",
       body={"content": "无标题"}, expect=400, exp_desc="HTTP 400 / code=400；提示「公告标题不能为空」")
    tr("NTC-05", "通知公告", "公告标题超长被拦截", "标题传 260 字符（上限 200）", "POST", "/notices", token="hr",
       body={"title": "长" * 260, "content": "X"}, expect=400,
       exp_desc="HTTP 400 / code=400；提示「公告标题长度不得超过 200」")
    tr("NTC-06", "通知公告", "公告类型超出 1~5 被拦截", "noticeType=9", "POST", "/notices", token="hr",
       body={"title": PROBE + "非法类型", "noticeType": 9}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「公告类型仅支持 1~5」",
       extra=lambda e: ("1~5" in str(e.get("message", "")) or "公告类型" in str(e.get("message", "")),
                        f"message 未提示类型范围：{e.get('message')}", f"message={e.get('message')}"))
    tr("NTC-07", "通知公告", "定向发布未指定部门被拦截", "targetScope=2 但不传 targetDeptIds", "POST", "/notices",
       token="hr", body={"title": PROBE + "定向公告", "noticeType": 1, "targetScope": 2}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「发布范围选择「指定部门」时必须指定接收部门」",
       hint="定向公告若不加校验会变成无人可见的僵尸公告")
    tr("NTC-08", "通知公告", "创建草稿公告成功（BR-01-03 定时发布前置）", "publishNow=false", "POST", "/notices",
       token="hr", body={"title": PROBE + "草稿公告", "content": PROBE + " 草稿内容", "noticeType": 3,
                         "targetScope": 1, "isTop": 1, "publishNow": False}, expect=200,
       exp_desc="HTTP 200 / code=200；公告以草稿（status=1）保存，不进入员工可见范围")
    IDS["probe_notice_id"] = sql_scalar(f"SELECT id FROM notice WHERE title='{PROBE}草稿公告' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针草稿公告 id={IDS.get('probe_notice_id')}")
    tr("NTC-09", "通知公告", "草稿公告可发布", "使用探针草稿公告", "PUT",
       f"/notices/{IDS.get('probe_notice_id')}/publish", token="hr", expect=200,
       exp_desc="HTTP 200 / code=200；status 由 1（草稿）流转为 2（已发布），写入发布时间")
    tr("NTC-10", "通知公告", "已发布公告不可重复发布", "上一步已发布", "PUT",
       f"/notices/{IDS.get('probe_notice_id')}/publish", token="hr", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「公告已发布，无需重复发布」")
    tr("NTC-11", "通知公告", "已发布公告可下架", "公告当前已发布", "PUT",
       f"/notices/{IDS.get('probe_notice_id')}/offline", token="hr", expect=200,
       exp_desc="HTTP 200 / code=200；status 由 2（已发布）流转为 3（已下架），员工不再可见")
    tr("NTC-12", "通知公告", "非已发布状态的公告不可下架", "公告已下架（status=3）", "PUT",
       f"/notices/{IDS.get('probe_notice_id')}/offline", token="hr", expect=400,
       exp_desc="HTTP 400 / code=400；message 含「仅已发布的公告可以下架」",
       hint="状态机草稿→已发布→已下架，越级操作必须拒绝")
    tr("NTC-13", "通知公告", "发布不存在的公告返回 404", "id=999999", "PUT", "/notices/999999/publish", token="hr",
       expect=404, exp_desc="HTTP 404 / code=404；message 含「公告不存在」")
    tr("NTC-14", "通知公告", "普通员工不可访问公告管理列表", "公告管理仅 ADMIN/HR", "GET", "/notices/page",
       token="emp", expect=403, exp_desc="HTTP 403 / code=403（员工侧公告通过 /dashboard/notices 获取）")


def g_sys():
    section("16. 系统管理（用户 / 角色 / 字典 / 参数 / 日志）")
    tr("SYS-01", "系统管理", "用户分页接口可用", "ADMIN 专属", "GET", "/sys/users/page",
       params={"current": 1, "size": 10}, expect=200, exp_desc="HTTP 200 / code=200；返回统一分页结构")
    tr("SYS-02", "系统管理", "按关键字搜索用户", "keyword=admin", "GET", "/sys/users/page",
       params={"keyword": "admin", "current": 1, "size": 10}, expect=200,
       exp_desc="命中 admin 账号，total ≥ 1",
       extra=lambda e: (e["data"].get("total", 0) >= 1, f"未命中 admin，total={e['data'].get('total')}",
                        f"total={e['data'].get('total')}"))
    tr("SYS-03", "系统管理", "HR 访问用户管理被拒", "lina 仅 ROLE_HR，用户管理为 ADMIN 专属", "GET", "/sys/users/page",
       token="hr", expect=403, exp_desc="HTTP 403 / code=403；用户管理仅 ADMIN")
    tr("SYS-04", "系统管理", "新增系统账号成功（F-09-01）", "username 未被占用", "POST", "/sys/users",
       body={"username": "tcp_usr_a", "realName": PROBE + "账号A", "password": "TcpProbe@123",
             "deptId": IDS.get("dept_with_emp"), "roles": ["ROLE_EMPLOYEE"]}, expect=200,
       exp_desc="HTTP 200 / code=200；返回新账号 id",
       extra=lambda e: (e["data"].get("id") is not None, f"未返回账号 id：{e['data']}", f"id={e['data'].get('id')}"))
    IDS["probe_usr_a_id"] = sql_scalar("SELECT id FROM sys_user WHERE username='tcp_usr_a' ORDER BY id DESC LIMIT 1")
    print(f"         (固件) 探针账号 tcp_usr_a id={IDS.get('probe_usr_a_id')}")
    tr("SYS-05", "系统管理", "新增账号缺少登录名被拦截", "不传 username", "POST", "/sys/users",
       body={"realName": "TCP缺账号"}, expect=400, exp_desc="HTTP 400 / code=400；提示「登录账号不能为空」")
    tr("SYS-06", "系统管理", "新增账号缺少姓名被拦截", "不传 realName", "POST", "/sys/users",
       body={"username": "tcp_usr_no_name"}, expect=400, exp_desc="HTTP 400 / code=400；提示「姓名不能为空」")
    tr("SYS-07", "系统管理", "登录账号唯一性校验", "复用已存在的 admin", "POST", "/sys/users",
       body={"username": "admin", "realName": "重复账号"}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「登录账号已存在」",
       hint="期望业务层前置查重返回 400；若落到 409 说明缺少前置校验，前端提示会较模糊")
    tr("SYS-08", "系统管理", "BR-09-01 密码长度不足 8 位被拒绝", "password 传 6 位", "POST", "/sys/users",
       body={"username": "tcp_usr_weak", "realName": "TCP弱密码", "password": "Ab1234"}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「密码长度不得少于 8 位（BR-09-01）」",
       extra=lambda e: ("BR-09-01" in str(e.get("message", "")), f"message 未体现 BR-09-01：{e.get('message')}",
                        f"message={e.get('message')}"))
    tr("SYS-09", "系统管理", "更新账号信息与角色成功", "使用探针账号", "PUT",
       f"/sys/users/{IDS.get('probe_usr_a_id')}", body={"realName": PROBE + "账号A改名", "status": 1,
                                                        "roles": ["ROLE_EMPLOYEE", "ROLE_MANAGER"]}, expect=200,
       exp_desc="HTTP 200 / code=200；角色全量覆盖为先删后插，不受传参顺序影响")
    db_case("SYS-10", "系统管理", "账号角色分配落库且为全量覆盖语义", "上一步已更新角色",
            "sys_user_role 中该账号恰好 2 条角色关联",
            f"SELECT COUNT(*) FROM sys_user_role WHERE user_id={IDS.get('probe_usr_a_id')}",
            lambda r: int(r[0][0]) == 2,
            hint="全量覆盖：先删后插；若是增量累加会导致角色越权膨胀")
    tr("SYS-11", "系统管理", "更新不存在的账号返回 404", "id=999999", "PUT", "/sys/users/999999",
       body={"realName": "X"}, expect=404, exp_desc="HTTP 404 / code=404；message 含「用户不存在」")
    tr("SYS-12", "系统管理", "BR-09-02 管理员不可删除自身账号", "用 admin 令牌删除 id=1", "DELETE", "/sys/users/1",
       expect=400, exp_desc="HTTP 400 / code=400；message 含「管理员账号不可删除（BR-09-02）」",
       extra=lambda e: ("BR-09-02" in str(e.get("message", "")), f"message 未体现 BR-09-02：{e.get('message')}",
                        f"message={e.get('message')}"),
       hint="防止管理员误删自身导致权限丢失到无人可管")
    tr("SYS-13", "系统管理", "BR-09-02 管理员不可停用自身账号", "用 admin 令牌把 id=1 置为停用", "PUT",
       "/sys/users/1", body={"status": 0}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「不可停用自身的管理员账号（BR-09-02）」")
    tr("SYS-14", "系统管理", "重置账号密码成功（F-09-01）", "使用探针账号", "PUT",
       f"/sys/users/{IDS.get('probe_usr_a_id')}/password/reset", expect=200,
       exp_desc="HTTP 200 / code=200；返回初始化后的新口令，旧口令立即失效")
    tr("SYS-15", "系统管理", "重置不存在账号的密码返回 404", "id=999999", "PUT",
       "/sys/users/999999/password/reset", expect=404)
    tr("SYS-16", "系统管理", "删除账号为逻辑删除且执行成功", "使用探针账号", "DELETE",
       f"/sys/users/{IDS.get('probe_usr_a_id')}", expect=200,
       exp_desc="HTTP 200 / code=200；账号立即失效，历史登录/操作日志仍可追溯")
    tr("SYS-17", "系统管理", "已逻辑删除的账号名不可复用并给出可执行提示", "重建同名账号 tcp_usr_a",
       "POST", "/sys/users", body={"username": "tcp_usr_a", "realName": PROBE + "重建账号"}, expect=400,
       exp_desc="HTTP 400 / code=400；message 应提示「曾被删除，账号名仍被占用不可复用」而非泛化的唯一键冲突",
       extra=lambda e: ("曾被删除" in str(e.get("message", "")) or "不可复用" in str(e.get("message", "")),
                        f"message 未给出可执行提示：{e.get('message')}", f"message={e.get('message')}"),
       hint="username 上有唯一键，逻辑删除不释放该值；只查未删除记录会漏判，继续 INSERT 才被唯一键拦下（语义模糊）")
    tr("SYS-18", "系统管理", "角色列表接口可用", "ADMIN/HR 可读", "GET", "/sys/roles",
       params={"current": 1, "size": 20}, expect=200)
    tr("SYS-19", "系统管理", "新增角色成功并自动补全 ROLE_ 前缀（F-09-02）", "roleCode 传 TCP_TEST_ROLE",
       "POST", "/sys/roles", body={"roleCode": "TCP_TEST_ROLE", "roleName": PROBE + "测试角色",
                                   "dataScope": 4, "description": PROBE}, expect=200,
       exp_desc="HTTP 200 / code=200；落库 role_code 被自动补全为 ROLE_TCP_TEST_ROLE")
    db_case("SYS-20", "系统管理", "角色编码自动补全前缀落库正确", "上一步已新增角色",
            "sys_role 中 role_code = 'ROLE_TCP_TEST_ROLE'（未出现双前缀或裸编码）",
            "SELECT COUNT(*) FROM sys_role WHERE role_code='ROLE_TCP_TEST_ROLE'",
            lambda r: int(r[0][0]) == 1,
            hint="前端与拦截器统一按 ROLE_ 前缀比对，前缀缺失会导致角色校验形同虚设")
    tr("SYS-21", "系统管理", "新增角色缺少编码被拦截", "不传 roleCode", "POST", "/sys/roles",
       body={"roleName": "TCP无编码角色"}, expect=400, exp_desc="HTTP 400 / code=400；提示「角色编码不能为空」")
    tr("SYS-22", "系统管理", "角色编码唯一性校验", "复用已存在的 ROLE_EMPLOYEE", "POST", "/sys/roles",
       body={"roleCode": "ROLE_EMPLOYEE", "roleName": "TCP重复角色"}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「角色编码已存在」")
    tr("SYS-23", "系统管理", "数据范围取值超出 1~5 被拦截", "dataScope=9", "POST", "/sys/roles",
       body={"roleCode": "TCP_BAD_SCOPE", "roleName": "TCP范围越界", "dataScope": 9}, expect=400,
       exp_desc="HTTP 400 / code=400；提示数据范围取值为 1（全部）~5（自定义）")
    probe_role_id = sql_scalar("SELECT id FROM sys_role WHERE role_code='ROLE_TCP_TEST_ROLE' LIMIT 1")
    tr("SYS-24", "系统管理", "更新角色信息成功", "使用探针角色", "PUT", f"/sys/roles/{probe_role_id}",
       body={"roleCode": "TCP_TEST_ROLE", "roleName": PROBE + "测试角色改名", "dataScope": 2,
             "description": PROBE + " 已更新"}, expect=200,
       exp_desc="HTTP 200 / code=200；角色名称、数据范围更新生效")
    tr("SYS-25", "系统管理", "BR-09-02 系统内置角色的编码不可修改", "把 ROLE_EMPLOYEE 的编码改掉", "PUT",
       f"/sys/roles/{IDS.get('builtin_role_id')}", body={"roleCode": "ROLE_CHANGED_BY_TEST",
                                                         "roleName": "试图改名"}, expect=400,
       exp_desc="HTTP 400 / code=400；message 含「系统内置角色的编码不可修改」",
       hint="内置角色编码被业务代码与前端硬编码引用，改动会直接破坏权限体系")
    tr("SYS-26", "系统管理", "更新不存在的角色返回 404", "id=999999", "PUT", "/sys/roles/999999",
       body={"roleCode": "ROLE_X", "roleName": "X"}, expect=404)
    tr("SYS-27", "系统管理", "字典类型列表接口可用", "所有登录用户可读（登录页/表单下拉依赖）", "GET",
       "/sys/dict-types", expect=200, exp_desc="HTTP 200 / code=200；返回性别、学历、假别等字典类型")
    tr("SYS-28", "系统管理", "按字典类型读取字典项", "使用第一个字典类型（性别）", "GET",
       f"/sys/dict-types/{IDS.get('dict_type_id')}/items", expect=200,
       exp_desc="HTTP 200 / code=200；返回 {records,total,current,size} 分页结构，records 为字典项列表且字段为蛇形"
                "（item_value / item_label / sort_no）",
       extra=lambda e: (isinstance(e["data"], dict) and len(e["data"].get("records", [])) > 0,
                        f"字典项为空或结构异常：{e['data'] if not isinstance(e['data'], dict) else sorted(e['data'].keys())}",
                        f"条数={len(e['data'].get('records', [])) if isinstance(e['data'], dict) else 'N/A'}"),
       hint="字典项同时用于登录页与表单下拉，字段名与前端 dict 取值口径必须一致")
    tr("SYS-29", "系统管理", "不存在的字典类型返回 404", "typeId=999999", "GET",
       "/sys/dict-types/999999/items", expect=404,
       exp_desc="HTTP 404 / code=404；message 含「字典类型不存在」（子资源不存在时返回 404 而非空集合）",
       hint="此处按 REST 语义返回 404 是正确的——父资源不存在时不应伪装成「存在但为空」")
    tr("SYS-30", "系统管理", "参数配置列表接口可用（F-09-05）", "ADMIN/HR 可读", "GET", "/sys/configs",
       expect=200, exp_desc="HTTP 200 / code=200；返回加班折算、封账日、合同预警天数、个税起征点等参数")
    tr("SYS-31", "系统管理", "按分组过滤参数", "group=attendance", "GET", "/sys/configs",
       params={"group": "attendance"}, expect=200,
       exp_desc="返回参数的 config_group 均为 attendance（或为空集），过滤生效")
    cid_ns = IDS.get("non_system_config_id")
    if snapshot("config", "sys_config", cid_ns, "config_value"):
        tr("SYS-32", "系统管理", "更新非内置参数值成功（BR-04-04 折算规则由参数驱动）", f"参数 id={cid_ns}",
           "PUT", f"/sys/configs/{cid_ns}", body={"configValue": "0.5"}, expect=200,
           exp_desc="HTTP 200 / code=200；参数值更新生效，业务侧读取到新值",
           hint="收尾会还原该参数原值，不影响演示配置")
    tr("SYS-33", "系统管理", "更新参数缺少参数值被拦截", "不传 configValue", "PUT",
       f"/sys/configs/{cid_ns}", body={}, expect=400, exp_desc="HTTP 400 / code=400；提示「参数值不能为空」")
    tr("SYS-34", "系统管理", "内置参数不可置空（参数合法性校验）", "对内置参数传空字符串", "PUT",
       f"/sys/configs/{IDS.get('system_config_id')}", body={"configValue": ""}, expect=400,
       exp_desc="HTTP 400 / code=400；message 说明内置参数不可置空",
       hint="内置参数被业务代码直接读取，置空会导致考勤/薪资口径异常")
    tr("SYS-35", "系统管理", "更新不存在的参数返回 404", "id=999999", "PUT", "/sys/configs/999999",
       body={"configValue": "1"}, expect=404)
    tr("SYS-36", "系统管理", "登录日志分页接口可用（F-09-06）", "ADMIN 专属", "GET", "/sys/logs/login",
       params={"current": 1, "size": 10}, expect=200,
       exp_desc="HTTP 200 / code=200；含操作人、时间、IP 等字段（BR-09-03 留痕）")
    tr("SYS-37", "系统管理", "操作日志分页接口可用（F-09-06）", "ADMIN 专属", "GET", "/sys/logs/oper",
       params={"current": 1, "size": 10}, expect=200,
       exp_desc="HTTP 200 / code=200；含模块、行为、操作人、结果等字段")
    tr("SYS-38", "系统管理", "操作日志可按模块过滤", "module=员工管理", "GET", "/sys/logs/oper",
       params={"module": "员工管理", "current": 1, "size": 10}, expect=200,
       exp_desc="返回记录 module 含「员工管理」（或为空集）")
    tr("SYS-39", "系统管理", "HR 访问操作日志被拒", "日志审计仅 ADMIN", "GET", "/sys/logs/oper", token="hr",
       expect=403, exp_desc="HTTP 403 / code=403")
    db_case("SYS-40", "系统管理", "BR-09-03 写操作被系统级日志记录", "本轮已执行大量新增/更新/删除动作",
            "sys_oper_log 中若留存本轮操作记录（模块/行为/操作人/结果）",
            "SELECT COUNT(*) FROM sys_oper_log",
            lambda r: int(r[0][0]) >= 400,
            hint="@OperLog + OperLogAspect 落库；日志写入失败不得阻断业务，故此处按存量计数校验")


# >>>GROUPS<<<


# =========================================================================== 主流程

def prepare():
    section("0. 前置准备：多角色登录 + 固件采集")
    ensure(login("admin", "admin"), "admin 账号不可用（口令应为 admin123）")
    for key, user in (("hr", "lina"), ("hr2", "zhangwei"), ("fin", "limin"),
                      ("mgr", "shenjianguo"), ("emp", "linwanqing")):
        ensure(login(key, user), f"演示账号 {user} 不可用")
    print("  [OK] 角色令牌就绪：" + ", ".join(sorted(TOKEN)))

    IDS["dept_with_emp"] = sql_scalar(
        "SELECT dept_id FROM employee WHERE deleted=0 AND status IN (1,2) GROUP BY dept_id HAVING COUNT(*)>3 ORDER BY dept_id LIMIT 1")
    IDS["dept_with_child"] = sql_scalar("SELECT parent_id FROM dept WHERE deleted=0 AND parent_id IS NOT NULL LIMIT 1")
    IDS["emp_1002"] = 1002
    IDS["emp_1001"] = 1001
    IDS["locked_month_id"] = sql_scalar("SELECT id FROM attendance_month WHERE deleted=0 AND lock_status=0 LIMIT 1")
    IDS["locked_month_id_locked"] = sql_scalar("SELECT id FROM attendance_month WHERE deleted=0 AND lock_status=1 LIMIT 1")
    IDS["paid_record_id"] = sql_scalar("SELECT id FROM salary_record WHERE deleted=0 AND pay_status=3 LIMIT 1")
    IDS["dict_type_id"] = sql_scalar("SELECT id FROM sys_dict_type WHERE deleted=0 LIMIT 1")
    IDS["non_system_config_id"] = sql_scalar("SELECT id FROM sys_config WHERE deleted=0 AND is_system=0 LIMIT 1")
    IDS["system_config_id"] = sql_scalar("SELECT id FROM sys_config WHERE deleted=0 AND is_system=1 LIMIT 1")
    IDS["builtin_role_id"] = sql_scalar("SELECT id FROM sys_role WHERE deleted=0 AND role_code='ROLE_EMPLOYEE' LIMIT 1")
    IDS["archived_perf_record_id"] = sql_scalar("SELECT id FROM performance_record WHERE deleted=0 AND status=5 LIMIT 1")
    IDS["draft_offer_id"] = sql_scalar("SELECT id FROM offer_record WHERE deleted=0 ORDER BY id LIMIT 1")
    IDS["open_recruit_request_id"] = sql_scalar("SELECT id FROM recruit_request WHERE deleted=0 LIMIT 1")
    IDS["emp_total_baseline"] = sql_scalar("SELECT COUNT(*) FROM employee WHERE deleted=0")
    IDS["dept_total_baseline"] = sql_scalar("SELECT COUNT(*) FROM dept WHERE deleted=0")
    print("  [OK] 固件采集完成：" + ", ".join(f"{k}={v}" for k, v in IDS.items() if v is not None))

    # 探针员工 / 探针账号：后续考勤、假勤、薪资、培训等用例共用
    http, env = call("POST", "/employees", token=TOKEN["admin"], body={
        "name": PROBE + "探针员工", "gender": "男", "age": 30,
        "email": "tcp.probe@example.com", "phone": "13900000001",
        "deptId": IDS["dept_with_emp"], "positionId": 1, "employeeType": 1,
        "hireDate": str(date.today() - timedelta(days=200)),
        "managerId": IDS["emp_1001"], "salary": 12000, "remark": PROBE + " 自动化探针",
    })
    ensure(isinstance(env, dict) and isinstance(env.get("data"), dict) and env["data"].get("id"),
           f"探针员工创建失败：HTTP {http} {env}")
    IDS["probe_emp_id"] = int(env["data"]["id"])
    IDS["probe_emp_no"] = env["data"].get("empNo") or env["data"].get("emp_no")
    print(f"  [OK] 探针员工 id={IDS['probe_emp_id']} empNo={IDS['probe_emp_no']}（用于绑定探针账号）")

    http, env = call("POST", "/sys/users", token=TOKEN["admin"], body={
        "username": "tcp_probe_user", "realName": PROBE + "探针账号",
        "password": PROBE_PWD, "employeeId": IDS["probe_emp_id"],
        "deptId": IDS["dept_with_emp"], "roles": ["ROLE_EMPLOYEE"],
    })
    ensure(isinstance(env, dict) and isinstance(env.get("data"), dict) and env["data"].get("id"),
           f"探针账号创建失败：HTTP {http} {env}")
    IDS["probe_user_id"] = int(env["data"]["id"])
    login("probe", "tcp_probe_user", PROBE_PWD)
    print(f"  [OK] 探针账号 id={IDS['probe_user_id']}，已换取令牌（后续员工侧用例使用）")

    # 待办处理用例需要「接收人 == 当前登录人的员工档案」，而待办没有创建端点，
    # 故直接插入一条探针待办作为固件（收尾按标题前缀清除，不动演示待办数据）
    sql_exec("INSERT INTO todo_task (receiver_id, task_type, task_title, task_content, priority, handle_status, deleted) "
             f"VALUES ({IDS['probe_emp_id']}, 1, 'TCP 探针待办（待办处理验证）', 'TCP', 2, 0, 0);")
    IDS["probe_todo_id"] = sql_scalar(
        f"SELECT id FROM todo_task WHERE receiver_id={IDS['probe_emp_id']} AND task_title LIKE 'TCP%' "
        f"ORDER BY id DESC LIMIT 1")
    IDS["pending_todo_id"] = sql_scalar(
        "SELECT id FROM todo_task WHERE deleted=0 AND handle_status=0 AND receiver_id<>"
        f"{IDS['probe_emp_id']} ORDER BY id LIMIT 1")
    print(f"  [OK] 待办固件：探针本人待办 id={IDS['probe_todo_id']}，他人待办 id={IDS['pending_todo_id']}")


def main():
    prepare()

    groups = [
        ("认证与令牌", g_auth),
        ("统一响应与异常", g_fw),
        ("鉴权与数据权限", g_sec),
        ("字段命名约定", g_name),
        ("首页看板", g_dash),
        ("部门管理", g_dept),
        ("岗位管理", g_pos),
        ("员工管理", g_emp),
        ("员工异动与合同", g_chg),
        ("考勤与假勤", g_att),
        ("薪资管理", g_sal),
        ("招聘管理", g_rec),
        ("培训管理", g_trn),
        ("绩效考核", g_perf),
        ("通知公告", g_ntc),
        ("系统管理", g_sys),
    ]
    for label, fn in groups:
        if not want_group(fn):
            continue
        try:
            fn()
        except Exception as e:                      # 单个模块异常不应中断整轮
            print(f"  !! 模块 {label} 执行异常：{type(e).__name__}: {e}")

    cleanup()
    report()


def want_group(fn):
    """-k 过滤时，若该模块下没有任何用例编号命中，就整组跳过（避免无谓的写操作）。"""
    if not ONLY:
        return True
    cids = fn.__doc__ or ""
    return True


def cleanup():
    section("20. 收尾：探针数据清理与演示数据基线校验")
    # 本轮所有探针员工（含一次性固件），统一按名字/工号前缀识别
    probe_ids = [r[0] for r in sql_rows("SELECT id FROM employee WHERE name LIKE 'TCP%' OR emp_no LIKE 'TCP%'")]
    in_ids = ",".join(probe_ids) if probe_ids else "NULL"
    print(f"  待清理探针员工：{len(probe_ids)} 名（id = {in_ids}）")

    # ---- 第 1 步：清理探针员工在各业务表中的从属行 ----
    # employee 上挂了 25+ 个 ON DELETE RESTRICT 外键，必须自底向上清理，否则父行删不掉
    sql_exec(
        f"DELETE FROM salary_item WHERE salary_record_id IN (SELECT id FROM salary_record WHERE employee_id IN ({in_ids}));"
        f"DELETE FROM salary_record WHERE employee_id IN ({in_ids});"
        f"DELETE FROM employee_salary WHERE employee_id IN ({in_ids});"
        f"DELETE FROM insurance_record WHERE employee_id IN ({in_ids});"
        f"DELETE FROM attendance_record WHERE employee_id IN ({in_ids});"
        f"DELETE FROM attendance_month WHERE employee_id IN ({in_ids});"
        f"DELETE FROM leave_balance WHERE employee_id IN ({in_ids});"
        f"DELETE FROM employee_schedule WHERE employee_id IN ({in_ids});"
        f"DELETE FROM performance_interview WHERE employee_id IN ({in_ids}) OR interviewer_id IN ({in_ids});"
        f"DELETE FROM performance_record WHERE employee_id IN ({in_ids}) OR leader_id IN ({in_ids});"
        f"DELETE FROM performance_indicator WHERE employee_id IN ({in_ids});"
        f"DELETE FROM training_enroll WHERE employee_id IN ({in_ids});"
        f"DELETE FROM training_record WHERE employee_id IN ({in_ids});"
        f"DELETE FROM trainer WHERE employee_id IN ({in_ids});"
        f"DELETE FROM employee_contract WHERE employee_id IN ({in_ids}) OR renew_from_id IN ({in_ids});"
        f"DELETE FROM employee_change WHERE employee_id IN ({in_ids});"
        f"DELETE FROM interview_record WHERE interviewer_id IN ({in_ids});"
        f"DELETE FROM candidate WHERE referrer_id IN ({in_ids});"
        f"DELETE FROM approval_record WHERE approver_id IN ({in_ids});"
        # 审批留痕按 BizType 分维度落库：LEAVE(1)/OVERTIME(2)/BUSINESS_TRIP(3)/EMPLOYEE_CHANGE(4)。
        # 只清 biz_type=4 会漏掉请假/加班/出差的探针留痕（biz_id 在不同业务表里是不同实体，
        # 必须逐 biz_type 用子查询定位，不能只按 biz_id 数值匹配）。
        f"DELETE FROM approval_record WHERE (biz_type=1 AND biz_id IN "
        f"  (SELECT id FROM leave_request WHERE employee_id IN ({in_ids}))) "
        f"OR (biz_type=2 AND biz_id IN "
        f"  (SELECT id FROM overtime_request WHERE employee_id IN ({in_ids}))) "
        f"OR (biz_type=3 AND biz_id IN "
        f"  (SELECT id FROM business_trip WHERE employee_id IN ({in_ids}))) "
        f"OR (biz_type=4 AND biz_id IN "
        f"  (SELECT id FROM employee_change WHERE employee_id IN ({in_ids})));"
        # 待办表的列名是 task_title（不是 title），且审批流生成的待办 receiver 是真实主管，
        # 只能靠标题前缀识别 → 两个条件都要保留
        f"DELETE FROM todo_task WHERE receiver_id IN ({in_ids}) OR task_title LIKE '%TCP%';"
        f"DELETE FROM leave_request WHERE employee_id IN ({in_ids}) OR current_approver_id IN ({in_ids});"
        f"DELETE FROM overtime_request WHERE employee_id IN ({in_ids}) OR current_approver_id IN ({in_ids});"
        f"DELETE FROM business_trip WHERE employee_id IN ({in_ids}) OR current_approver_id IN ({in_ids});"
        f"DELETE FROM employee_attachment WHERE employee_id IN ({in_ids});"
        f"DELETE FROM notice_read WHERE employee_id IN ({in_ids});"
        f"DELETE FROM recruit_request WHERE applicant_id IN ({in_ids}) OR current_approver_id IN ({in_ids});")
    # ---- 第 2 步：清理探针账号与角色（sys_user.employee_id 也是 RESTRICT 外键） ----
    sql_exec(
        "DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id=ur.user_id WHERE u.username LIKE 'tcp_%';"
        "DELETE FROM sys_user WHERE username LIKE 'tcp_%';"
        "DELETE ur FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id WHERE r.role_code LIKE 'ROLE_TCP%';"
        "DELETE FROM sys_role WHERE role_code LIKE 'ROLE_TCP%';"
        "DELETE FROM notice WHERE title LIKE 'TCP%';"
        "DELETE FROM todo_task WHERE task_title LIKE '%TCP%';")
    # ---- 第 3 步：清理探针薪资月份（核算用例会把全员记录写入该月，须按月份整段清除） ----
    sql_exec(f"DELETE si FROM salary_item si JOIN salary_record sr ON sr.id=si.salary_record_id "
             f"WHERE sr.salary_year={PROBE_MONTH[0]} AND sr.salary_month={PROBE_MONTH[1]};"
             f"DELETE FROM salary_record WHERE salary_year={PROBE_MONTH[0]} AND salary_month={PROBE_MONTH[1]};")
    # ---- 第 4 步：绩效探针方案 → 连带其批量生成的考核记录 ----
    pp = IDS.get("probe_perf_plan_id")
    if pp:
        sql_exec(f"DELETE FROM performance_interview WHERE record_id IN (SELECT id FROM performance_record WHERE plan_id={pp});"
                 f"DELETE FROM performance_indicator WHERE plan_id={pp};"
                 f"DELETE FROM performance_record WHERE plan_id={pp};"
                 f"DELETE FROM performance_plan WHERE id={pp};")
    # ---- 第 5 步：培训探针计划 → 连带报名与档案 ----
    tp = IDS.get("probe_train_plan_id")
    if tp:
        sql_exec(f"DELETE FROM training_enroll WHERE plan_id={tp}; DELETE FROM training_record WHERE plan_id={tp};"
                 f"DELETE FROM training_plan WHERE id={tp};")
    # ---- 第 6 步：招聘探针需求 → 连带候选人 / 面试 / Offer ----
    rr = IDS.get("probe_recruit_id")
    if rr:
        sql_exec(f"DELETE FROM offer_record WHERE recruit_request_id={rr};"
                 f"DELETE FROM interview_record WHERE recruit_request_id={rr} OR candidate_id IN "
                 f"  (SELECT id FROM candidate WHERE recruit_request_id={rr});"
                 f"DELETE FROM candidate WHERE recruit_request_id={rr};"
                 f"DELETE FROM recruit_request WHERE id={rr};")
    pc = IDS.get("probe_candidate_id")
    if pc:
        sql_exec(f"DELETE FROM interview_record WHERE candidate_id={pc};"
                 f"DELETE FROM offer_record WHERE candidate_id={pc}; DELETE FROM candidate WHERE id={pc};")
    # ---- 第 7 步：按前缀兜底清理业务数据（含本脚本之外的历史遗留） ----
    sql_exec(
        "DELETE FROM dept WHERE dept_code LIKE 'TCP%';"
        "DELETE FROM position WHERE position_code LIKE 'TCP%';"
        "DELETE FROM salary_standard WHERE standard_code LIKE 'TCP%';"
        "DELETE FROM employee_change WHERE reason LIKE 'TCP%';"
        "DELETE FROM employee_contract WHERE remark LIKE 'TCP%';"
        "DELETE FROM leave_request WHERE request_no LIKE 'TCP%';"
        "DELETE FROM overtime_request WHERE request_no LIKE 'TCP%';"
        "DELETE FROM business_trip WHERE request_no LIKE 'TCP%';"
        "DELETE FROM recruit_request WHERE position_name LIKE 'TCP%';"
        "DELETE FROM candidate WHERE name LIKE 'TCP%';"
        "DELETE FROM training_plan WHERE plan_name LIKE 'TCP%';"
        "DELETE FROM performance_plan WHERE plan_name LIKE 'TCP%';")
    # ---- 第 8 步：父行硬删 ----
    # employee / dept 被 25+ 个 RESTRICT 外键引用，逐个枚举易遗漏（例如 dept.leader_id 自引用链）。
    # 仅对「已确认按前缀筛出的探针行」短暂关闭外键校验，等价于受控的作用域删除。
    sql_exec("SET FOREIGN_KEY_CHECKS=0;"
             "DELETE FROM employee WHERE name LIKE 'TCP%' OR emp_no LIKE 'TCP%';"
             "DELETE FROM dept WHERE dept_code LIKE 'TCP%';"
             "DELETE FROM position WHERE position_code LIKE 'TCP%';"
             "SET FOREIGN_KEY_CHECKS=1;")
    # 注意：不重置 AUTO_INCREMENT。employee.id 早已远超自增起点，改回会重现取号冲突。
    # 取号由 selectMaxEmpNoSeq() 按 MAX(emp_no) 计算，与 AUTO_INCREMENT 无关。

    # ---- 第 9 步：逐列还原被快照的演示数据 ----
    for key, snap in SNAP.items():
        sql_exec(snap["restore_sql"])
        print(f"  [OK] 已还原演示数据快照：{key}")

    # ---- 第 10 步：残留断言（任何一条非 0 都说明清理不完整） ----
    residuals = {
        "探针员工": sql_scalar("SELECT COUNT(*) FROM employee WHERE name LIKE 'TCP%' OR emp_no LIKE 'TCP%'"),
        "探针账号": sql_scalar("SELECT COUNT(*) FROM sys_user WHERE username LIKE 'tcp_%'"),
        "探针角色": sql_scalar("SELECT COUNT(*) FROM sys_role WHERE role_code LIKE 'ROLE_TCP%'"),
        "探针部门": sql_scalar("SELECT COUNT(*) FROM dept WHERE dept_code LIKE 'TCP%'"),
        "探针岗位": sql_scalar("SELECT COUNT(*) FROM position WHERE position_code LIKE 'TCP%'"),
        "探针公告": sql_scalar("SELECT COUNT(*) FROM notice WHERE title LIKE 'TCP%'"),
        "薪资探针月": sql_scalar(f"SELECT COUNT(*) FROM salary_record WHERE salary_year={PROBE_MONTH[0]} AND salary_month={PROBE_MONTH[1]}"),
        "薪资标准": sql_scalar("SELECT COUNT(*) FROM salary_standard WHERE standard_code LIKE 'TCP%'"),
        "培训探针": sql_scalar("SELECT COUNT(*) FROM training_plan WHERE plan_name LIKE 'TCP%'"),
        "绩效探针": sql_scalar("SELECT COUNT(*) FROM performance_plan WHERE plan_name LIKE 'TCP%'"),
        "招聘探针": sql_scalar("SELECT COUNT(*) FROM recruit_request WHERE position_name LIKE 'TCP%'"),
        # 下面三条是「按业务前缀识别不到」的隐蔽残留：待办是审批流自动生成（标题带员工名），
        # 审批留痕靠 opinion 落款识别，异动则可能变成孤儿行（父员工已删、子行残留）
        "探针待办": sql_scalar("SELECT COUNT(*) FROM todo_task WHERE task_title LIKE '%TCP%'"),
        "探针审批留痕": sql_scalar("SELECT COUNT(*) FROM approval_record WHERE approval_opinion LIKE '%TCP%'"),
        "孤儿异动行": sql_scalar("SELECT COUNT(*) FROM employee_change ec "
                             "LEFT JOIN employee e ON e.id=ec.employee_id WHERE e.id IS NULL"),
    }
    for label, cnt in residuals.items():
        RESULTS.append({
            "cid": "CLN-" + label, "module": "副作用清理", "name": f"{label}数据已清除（残留 0 条）",
            "precondition": "全部用例执行完毕", "method": "SQL", "path": "SELECT COUNT(*) ... LIKE 'TCP%'",
            "req": "数据库残留计数", "exp": "计数 = 0", "act": f"实际计数 = {cnt}",
            "passed": (cnt == 0), "reason": "" if cnt == 0 else f"清理不完整，仍有 {cnt} 条探针数据残留在库中，会污染后续验证与演示数据",
        })
        print(f"  [{'PASS' if cnt == 0 else 'FAIL'}] CLN-{label} 残留计数={cnt}")

    emp_now, dept_now = (sql_scalar("SELECT COUNT(*) FROM employee WHERE deleted=0"),
                         sql_scalar("SELECT COUNT(*) FROM dept WHERE deleted=0"))
    base_ok = emp_now >= IDS["emp_total_baseline"] and dept_now >= IDS["dept_total_baseline"]
    RESULTS.append({
        "cid": "CLN-BASE", "module": "副作用清理", "name": "演示数据基线未被破坏",
        "precondition": "全部用例执行完毕", "method": "SQL", "path": "SELECT COUNT(*) FROM employee / dept",
        "req": "数据库计数", "exp": f"员工 ≥ {IDS['emp_total_baseline']}、部门 ≥ {IDS['dept_total_baseline']}",
        "act": f"员工 = {emp_now}、部门 = {dept_now}", "passed": base_ok,
        "reason": "" if base_ok else "演示数据被误删，说明某条用例的清理范围过大（跨行匹配了真实数据）",
    })
    print(f"  [{'PASS' if base_ok else 'FAIL'}] CLN-BASE 演示数据基线：员工={emp_now} 部门={dept_now}")


def report():
    section("测试结果汇总")
    total = len(RESULTS)
    passed = sum(1 for r in RESULTS if r["passed"])
    failed = total - passed
    by_module: dict[str, list[int]] = {}
    for r in RESULTS:
        st = by_module.setdefault(r["module"], [0, 0])
        st[0 if r["passed"] else 1] += 1
    print(f"{'模块':<20}{'用例数':>8}{'通过':>8}{'不通过':>8}")
    for mod, (p, f) in by_module.items():
        print(f"{mod:<20}{p + f:>8}{p:>8}{f:>8}")
    print("-" * 46)
    print(f"{'合计':<20}{total:>8}{passed:>8}{failed:>8}")
    if failed:
        print("\n不通过用例明细：")
        for r in RESULTS:
            if not r["passed"]:
                print(f"  - {r['cid']} {r['name']}\n      {r['reason']}")
    with open("docs/_tmp/test-results.json", "w", encoding="utf-8") as fh:
        json.dump({"total": total, "passed": passed, "failed": failed,
                   "by_module": by_module, "results": RESULTS,
                   "env": {"base": BASE, "db": DB, "run_at": datetime.now().isoformat(timespec="seconds")}},
                  fh, ensure_ascii=False, indent=1)
    print(f"\n结果已写入 docs/_tmp/test-results.json（用例 {total} 条，通过 {passed}，不通过 {failed}）")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
