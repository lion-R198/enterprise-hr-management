# -*- coding: utf-8 -*-
"""
从 enterprise_hr_schema.sql 生成《数据库 E-R 关系图 + 表结构速查》HTML 单文件。
- SVG 1：模块级 E-R 概览（以「员工信息」为枢纽的模块依赖图，边标注跨模块外键数）
- SVG 2：员工全生命周期数据链（招聘 → 入职 → 考勤/薪资/绩效 → 离职）
- 交互区：49 张表 × 776 字段的可搜索、可按模块筛选速查表
"""
import re
from pathlib import Path

BASE = Path(r"D:\Workspace\enterprise-hr-management")
SQL = BASE / "enterprise_hr_schema.sql"

MODULE_TITLES = {
    "01": "首页", "02": "员工信息", "03": "部门管理", "04": "考勤管理",
    "05": "薪资管理", "06": "招聘管理", "07": "培训管理", "08": "绩效考核",
    "09": "系统设置", "00": "公共组件",
}
MODULE_ORDER = ["01", "02", "03", "04", "05", "06", "07", "08", "09", "00"]
MODULE_COLOR = {
    "01": "#0ea5e9", "02": "#2563eb", "03": "#7c3aed", "04": "#059669",
    "05": "#d97706", "06": "#db2777", "07": "#0891b2", "08": "#dc2626",
    "09": "#475569", "00": "#65a30d",
}

text = SQL.read_text(encoding="utf-8")
lines = text.split("\n")

table_re = re.compile(r"^CREATE TABLE `(?P<name>[^`]+)`\s*\(\s*$")
title_re = re.compile(r"^--\s+(?P<funcs>(?:【F-[^】]+】)+)(?P<rest>.+)$")
col_re = re.compile(r"^\s*`(?P<name>[^`]+)`\s+(?P<type>[A-Za-z]+(?:\([^)]*\))?)\s*(?P<rest>.*?),?\s*$")
pk_re = re.compile(r"^\s*PRIMARY KEY")
uk_re = re.compile(r"^\s*UNIQUE KEY `(?P<name>[^`]+)` \((?P<cols>[^)]*)\)")
key_re = re.compile(r"^\s*(?:KEY|INDEX) `(?P<name>[^`]+)` \((?P<cols>[^)]*)\)")
fk_re = re.compile(r"^\s*CONSTRAINT `(?P<name>[^`]+)` FOREIGN KEY \((?P<col>[^)]*)\) REFERENCES `(?P<ref>[^`]+)`")

tables, cur, pf = [], None, None
for idx, ln in enumerate(lines):
    m = title_re.match(ln)
    if m and "表" in m.group("rest"):
        pf = m.group("funcs")
        continue
    m = table_re.match(ln)
    if m:
        cur = {"name": m.group("name"), "funcs": pf, "cols": [], "uks": [], "keys": [], "fks": []}
        tables.append(cur)
        continue
    if cur is None:
        continue
    if ln.strip().startswith(")"):
        cm = re.search(r"COMMENT\s*=\s*'([^']*)'", ln)
        cur["comment"] = cm.group(1) if cm else ""
        cur = None
        pf = None
        continue
    if pk_re.match(ln):
        continue
    m = uk_re.match(ln)
    if m:
        cur["uks"].append((m.group("name"), m.group("cols").replace("`", "").strip()))
        continue
    m = fk_re.match(ln)
    if m:
        cur["fks"].append((m.group("name"), m.group("col").replace("`", "").strip(),
                           m.group("ref"), "RESTRICT"))
        continue
    m = key_re.match(ln)
    if m:
        cur["keys"].append((m.group("name"), m.group("cols").replace("`", "").strip()))
        continue
    m = col_re.match(ln)
    if m:
        rest = m.group("rest")
        dm = re.search(r"DEFAULT\s+('(?:[^']*)'|[\w()]+)", rest)
        comment = re.search(r"COMMENT\s+'([^']*)'", rest)
        cur["cols"].append({
            "name": m.group("name"), "type": m.group("type"),
            "nullable": "NOT NULL" not in rest,
            "default": (dm.group(1) if dm else ""),
            "pk": "AUTO_INCREMENT" in rest.upper(),
            "comment": comment.group(1) if comment else "",
        })
        continue

# 末尾 ALTER 补充的外键
for t, n, c, r in re.findall(
        r"ALTER TABLE `(?P<t>[^`]+)`\s*\n\s*ADD CONSTRAINT `(?P<n>[^`]+)` FOREIGN KEY \((?P<c>[^)]*)\)\s*\n?\s*REFERENCES `(?P<r>[^`]+)`", text):
    for tb in tables:
        if tb["name"] == t:
            tb["fks"].append((n, c.replace("`", "").strip(), r, "RESTRICT"))

# 模块归属：以功能点编号为准
fc = re.compile(r"F-(\d{2})-")
for t in tables:
    codes = fc.findall(t["funcs"] or "")
    t["module"] = max(set(codes), key=lambda c: (codes.count(c), -int(c))) if codes else "00"

assert len(tables) == 49, len(tables)
by_name = {t["name"]: t for t in tables}

# ---- 跨模块外键统计 ----
cross = {}
for t in tables:
    for _, col, ref, _ in t["fks"]:
        if ref in by_name:
            key = (t["module"], by_name[ref]["module"])
            cross[key] = cross.get(key, 0) + 1

mod_of = {c: [t for t in tables if t["module"] == c] for c in MODULE_ORDER}

# ==================== SVG 1：模块级 E-R 概览 ====================
BOX_W, BOX_H = 208, 66


def box(x, y, title, sub, color, w=BOX_W, h=BOX_H, big=False):
    fs = 17 if big else 15
    return (
        f'<g><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="11" fill="#ffffff" '
        f'stroke="{color}" stroke-width="{2.4 if big else 1.6}"/>'
        f'<rect x="{x}" y="{y}" width="6" height="{h}" rx="3" fill="{color}"/>'
        f'<text x="{x + w / 2 + 3}" y="{y + (h / 2) - 5}" text-anchor="middle" font-size="{fs}" '
        f'font-weight="700" fill="#1e293b">{title}</text>'
        f'<text x="{x + w / 2 + 3}" y="{y + (h / 2) + 15}" text-anchor="middle" font-size="11.5" '
        f'fill="#64748b">{sub}</text></g>')


def edge(x1, y1, x2, y2, label="", color="#94a3b8", dash=False, lx=None, ly=None):
    d = ' stroke-dasharray="6 5"' if dash else ""
    mk = ' marker-end="url(#arrow)"'
    s = f'<path d="M {x1} {y1} C {x1} {(y1 + y2) / 2}, {x2} {(y1 + y2) / 2}, {x2} {y2}" fill="none" stroke="{color}" stroke-width="1.8"{d}{mk}/>'
    if label:
        cxp = lx if lx is not None else (x1 + x2) / 2
        cyp = ly if ly is not None else (y1 + y2) / 2
        wpx = 13 + len(label) * 7.2
        s += (f'<rect x="{cxp - wpx / 2}" y="{cyp - 10}" width="{wpx}" height="19" rx="6" '
              f'fill="#ffffff" stroke="{color}" stroke-width="0.9"/>'
              f'<text x="{cxp}" y="{cyp + 4}" text-anchor="middle" font-size="11" fill="{color}" '
              f'font-weight="600">{label}</text>')
    return s


def svg_module_map():
    W, H = 1400, 660
    p = [f'<svg viewBox="0 0 {W} {H}" width="100%" xmlns="http://www.w3.org/2000/svg" role="img" '
         f'aria-label="模块级 E-R 概览">']
    p.append('<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" '
             'markerHeight="7" orient="auto-start-reverse">'
             '<path d="M 0 0 L 10 5 L 0 10 z" fill="#94a3b8"/></marker></defs>')

    # 中心枢纽
    cx, cy = 596, 288
    # 顶部两个
    t1 = (196, 62)     # 09 系统设置
    t2 = (996, 62)     # 00 公共组件
    # 左侧 03 部门管理
    lt = (66, 288)
    # 底部六个
    bottoms = [("04", 34), ("05", 262), ("06", 490), ("07", 718), ("08", 946), ("01", 1174)]
    by = 528

    def mod_sub(code):
        ts = mod_of[code]
        return f"{len(ts)} 张表"

    # 先画边（在下层）
    edges = []
    up = "#2563eb"
    # 部门管理 <-> 员工信息（双向引用）
    edges.append(edge(lt[0] + BOX_W, lt[1] + BOX_H / 2 - 6, cx, cy + 14,
                      f"{cross.get(('03','02'),0)} 外键", MODULE_COLOR["03"]))
    edges.append(edge(cx, cy + BOX_H / 2 + 6, lt[0] + BOX_W, lt[1] + BOX_H / 2 + 14,
                      f"{cross.get(('02','03'),0)} 外键", MODULE_COLOR["03"]))
    # 系统设置 / 公共组件 -> 员工信息
    edges.append(edge(t1[0] + BOX_W / 2 + 30, t1[1] + BOX_H, cx + 30, cy,
                      f"{cross.get(('09','02'),0)} 外键", MODULE_COLOR["09"]))
    edges.append(edge(t2[0] + BOX_W / 2 - 30, t2[1] + BOX_H, cx + 170, cy,
                      f"{cross.get(('00','02'),0)} 外键", MODULE_COLOR["00"]))
    # 各业务模块 -> 员工信息（业务明细引用员工主数据）
    for code, bx in bottoms:
        n = cross.get((code, "02"), 0)
        edges.append(edge(bx + BOX_W / 2, by, cx + 44 + bottoms.index((code, bx)) * 24, cy + BOX_H,
                          f"{n} 外键", MODULE_COLOR[code]))
    p += edges

    # 再画节点
    p.append(box(cx, cy, "员工信息", mod_sub("02") + " · 全系统枢纽", MODULE_COLOR["02"], big=True))
    p.append(box(t1[0], t1[1], "系统设置", mod_sub("09") + " · 用户/权限/字典", MODULE_COLOR["09"]))
    p.append(box(t2[0], t2[1], "公共组件", mod_sub("00") + " · 通用审批流", MODULE_COLOR["00"]))
    p.append(box(lt[0], lt[1], "部门管理", mod_sub("03") + " · 组织/岗位", MODULE_COLOR["03"]))
    for code, bx in bottoms:
        p.append(box(bx, by, MODULE_TITLES[code], mod_sub(code), MODULE_COLOR[code]))

    p.append('<text x="700" y="622" text-anchor="middle" font-size="12" fill="#94a3b8">'
             '箭头 = 外键引用方向；「n 外键」表示该方向上共有 n 个外键约束'
             '（如「10 外键」= 考勤模块有 10 个外键指向 employee）</text>')
    p.append('<text x="700" y="642" text-anchor="middle" font-size="12" fill="#94a3b8">'
             'dept.leader_id 与 employee.dept_id 互为引用，构成组织闭环，建表时以 ALTER TABLE 在脚本末尾补加</text>')
    p.append("</svg>")
    return "\n".join(p)


# ==================== SVG 2：员工全生命周期数据链 ====================
def svg_lifecycle():
    W, H = 1400, 320
    p = [f'<svg viewBox="0 0 {W} {H}" width="100%" xmlns="http://www.w3.org/2000/svg" '
         f'role="img" aria-label="员工全生命周期数据链">']
    p.append('<defs><marker id="arrow2" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" '
             'markerHeight="8" orient="auto-start-reverse">'
             '<path d="M 0 0 L 10 5 L 0 10 z" fill="#2563eb"/></marker></defs>')

    stages = [
        ("招聘", ["recruit_request", "job_posting", "candidate", "interview_record", "offer_record"], "#db2777"),
        ("入职建档", ["employee", "employee_contract", "employee_attachment", "sys_user"], "#2563eb"),
        ("在职管理", ["employee_change", "employee_salary", "attendance_record", "leave_request", "overtime_request"], "#059669"),
        ("发展与评价", ["training_record", "performance_record", "performance_interview"], "#dc2626"),
        ("离职", ["employee(status=0)", "employee_change(type=离职)"], "#475569"),
    ]
    gw, gap = 244, 34
    x = 30
    for i, (name, tabs, color) in enumerate(stages):
        p.append(f'<rect x="{x}" y="52" width="{gw}" height="212" rx="13" fill="#ffffff" '
                 f'stroke="{color}" stroke-width="1.6"/>')
        p.append(f'<rect x="{x}" y="52" width="{gw}" height="34" rx="13" fill="{color}"/>')
        p.append(f'<rect x="{x}" y="72" width="{gw}" height="14" fill="{color}"/>')
        p.append(f'<text x="{x + gw / 2}" y="75" text-anchor="middle" font-size="14.5" '
                 f'font-weight="700" fill="#ffffff">{name}</text>')
        ty = 112
        for tb in tabs:
            p.append(f'<text x="{x + 16}" y="{ty}" font-size="12.6" fill="#334155" '
                     f'font-family="Consolas, monospace">• {tb}</text>')
            ty += 27
        if i < len(stages) - 1:
            ax = x + gw + 6
            p.append(f'<line x1="{ax}" y1="158" x2="{ax + gap - 14}" y2="158" stroke="#2563eb" '
                     f'stroke-width="2.2" marker-end="url(#arrow2)"/>')
        x += gw + gap
    p.append('<text x="700" y="292" text-anchor="middle" font-size="12" fill="#94a3b8">'
             '同一位员工（employee）随生命周期推进，在各模块产生关联记录；主数据始终一份，业务明细按需引用</text>')
    p.append("</svg>")
    return "\n".join(p)


# ==================== 表结构速查区 ====================
cards = []
for t in tables:
    color = MODULE_COLOR[t["module"]]
    rows = []
    for c in t["cols"]:
        dft = "AUTO_INCREMENT" if c["pk"] else (c["default"] if c["default"] not in ("", "NULL") else "—")
        tags = []
        if c["pk"]:
            tags.append('<span class="tg tg-pk">PK</span>')
        if any(c["name"] == cn for _, cn in t["uks"]):
            tags.append('<span class="tg tg-uk">UQ</span>')
        if any(c["name"] == fc_ for _, fc_, _, _ in t["fks"]):
            tags.append('<span class="tg tg-fk">FK</span>')
        rows.append(
            f'<tr><td class="fn">{c["name"]}{"".join(tags)}</td><td class="ft">{c["type"]}</td>'
            f'<td class="fo">{"是" if c["nullable"] else "否"}</td>'
            f'<td class="fd">{dft}</td><td class="fc">{c["comment"]}</td></tr>')
    uks = "".join(f'<code>{n}({cc})</code>' for n, cc in t["uks"]) or "—"
    keys = "".join(f'<code>{n}({cc})</code>' for n, cc in t["keys"]) or "—"
    fks = "".join(f'<code>{n}: {cc} → {r}</code>' for n, cc, r, _ in t["fks"]) or "—"
    cards.append(f'''
<details class="tcard" data-mod="{t["module"]}" data-key="{t["name"]} {t["comment"]} {" ".join(c["name"] + " " + c["comment"] for c in t["cols"])}">
  <summary>
    <span class="dot" style="background:{color}"></span>
    <span class="tname">{t["name"]}</span>
    <span class="tcn">{t["comment"]}</span>
    <span class="badges"><span class="badge" style="background:{color}1f;color:{color}">模块 {t["module"]} {MODULE_TITLES[t["module"]]}</span>
    <span class="badge">{len(t["cols"])} 字段</span><span class="badge">{len(t["fks"])} 外键</span></span>
  </summary>
  <div class="tbody">
    <table><thead><tr><th>字段名</th><th>类型</th><th>可空</th><th>默认值</th><th>字段注释</th></tr></thead>
    <tbody>{"".join(rows)}</tbody></table>
    <div class="meta"><p><b>唯一键</b>：{uks}</p><p><b>普通索引</b>：{keys}</p><p><b>外键</b>：{fks}</p></div>
  </div>
</details>''')

chips = "".join(
    f'<button class="chip" data-m="{m}" style="--c:{MODULE_COLOR[m]}">{m} {MODULE_TITLES[m]}'
    f'<i>{len(mod_of[m])}</i></button>' for m in MODULE_ORDER)

total_cols = sum(len(t["cols"]) for t in tables)
total_fks = sum(len(t["fks"]) for t in tables)
total_uks = sum(len(t["uks"]) for t in tables)

HTML = f'''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>企业人事管理系统 — 数据库 E-R 关系图与表结构速查</title>
<style>
  :root {{
    --bg: #eef2f7; --panel: #fff; --line: #d8e0ea; --ink: #1e293b;
    --ink-2: #475569; --ink-3: #64748b; --mono: Consolas, "Cascadia Mono", Menlo, monospace;
  }}
  * {{ box-sizing: border-box; }}
  body {{
    margin: 0; padding: 30px 24px 56px; background: var(--bg); color: var(--ink);
    font-family: "Microsoft YaHei", "PingFang SC", "Hiragino Sans GB", "Segoe UI", sans-serif;
    -webkit-font-smoothing: antialiased;
  }}
  .wrap {{ max-width: 1400px; margin: 0 auto; }}
  .page-head {{ display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; flex-wrap: wrap; margin-bottom: 22px; }}
  .page-head h1 {{ margin: 0 0 6px; font-size: 25px; letter-spacing: .5px; }}
  .page-head p {{ margin: 0; font-size: 13px; color: var(--ink-3); }}
  .stats {{ display: flex; gap: 10px; flex-wrap: wrap; }}
  .stat {{ background: var(--panel); border: 1px solid var(--line); border-radius: 10px; padding: 8px 16px; text-align: center; min-width: 86px; box-shadow: 0 1px 2px rgba(30,41,59,.05); }}
  .stat b {{ display: block; font-size: 20px; color: #1d4ed8; line-height: 1.25; }}
  .stat span {{ font-size: 11px; color: var(--ink-3); }}

  section {{ background: var(--panel); border: 1px solid var(--line); border-radius: 16px; padding: 24px 26px 20px; margin-bottom: 22px; box-shadow: 0 6px 24px rgba(30,41,59,.06); }}
  h2 {{ margin: 0 0 4px; font-size: 18px; }}
  h2 .n {{ color: #1d4ed8; margin-right: 8px; }}
  .hint {{ margin: 0 0 18px; font-size: 12.5px; color: var(--ink-3); }}

  /* --- 速查区 --- */
  .toolbar {{ display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }}
  #q {{ flex: 1 1 300px; min-width: 240px; padding: 10px 14px; font-size: 14px; border: 1px solid var(--line); border-radius: 10px; outline: none; background: #f8fafc; }}
  #q:focus {{ border-color: #2563eb; background: #fff; box-shadow: 0 0 0 3px rgba(37,99,235,.12); }}
  .chips {{ display: flex; gap: 7px; flex-wrap: wrap; }}
  .chip {{ border: 1px solid var(--c); background: #fff; color: var(--c); border-radius: 999px; padding: 5px 12px; font-size: 12px; cursor: pointer; font-weight: 600; transition: .15s; }}
  .chip i {{ font-style: normal; opacity: .65; margin-left: 5px; }}
  .chip.on, .chip:hover {{ background: var(--c); color: #fff; }}
  .chip.on i, .chip:hover i {{ opacity: .85; }}

  .tcard {{ border: 1px solid var(--line); border-radius: 12px; margin-bottom: 9px; overflow: hidden; background: #fff; }}
  .tcard[hidden] {{ display: none; }}
  .tcard > summary {{ list-style: none; cursor: pointer; padding: 12px 16px; display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }}
  .tcard > summary::-webkit-details-marker {{ display: none; }}
  .tcard > summary::before {{ content: "▸"; color: var(--ink-3); font-size: 12px; transition: .15s; }}
  .tcard[open] > summary::before {{ transform: rotate(90deg); }}
  .tcard[open] > summary {{ background: #f8fafc; border-bottom: 1px solid var(--line); }}
  .dot {{ width: 9px; height: 9px; border-radius: 50%; flex: none; }}
  .tname {{ font-family: var(--mono); font-size: 14.5px; font-weight: 700; color: #1e3a8a; }}
  .tcn {{ font-size: 13px; color: var(--ink-2); }}
  .badges {{ margin-left: auto; display: flex; gap: 6px; flex-wrap: wrap; }}
  .badge {{ font-size: 11px; padding: 3px 9px; border-radius: 999px; background: #f1f5f9; color: var(--ink-3); }}
  .tbody {{ padding: 4px 16px 16px; }}
  table {{ width: 100%; border-collapse: collapse; font-size: 12.7px; }}
  th {{ text-align: left; padding: 7px 9px; color: var(--ink-3); font-weight: 600; border-bottom: 1px solid var(--line); font-size: 11.5px; }}
  td {{ padding: 6px 9px; border-bottom: 1px solid #f1f5f9; vertical-align: top; }}
  tr:last-child td {{ border-bottom: none; }}
  .fn {{ font-family: var(--mono); color: #0f172a; font-weight: 600; white-space: nowrap; }}
  .ft {{ font-family: var(--mono); color: #0369a1; white-space: nowrap; }}
  .fo {{ text-align: center; color: var(--ink-3); }}
  .fd {{ font-family: var(--mono); color: var(--ink-3); }}
  .fc {{ color: var(--ink-2); }}
  .tg {{ font-size: 9.5px; font-weight: 700; padding: 1px 5px; border-radius: 4px; margin-left: 5px; vertical-align: 1px; }}
  .tg-pk {{ background: #fef3c7; color: #b45309; }}
  .tg-uk {{ background: #dbeafe; color: #1d4ed8; }}
  .tg-fk {{ background: #dcfce7; color: #15803d; }}
  .meta {{ margin-top: 12px; padding: 10px 12px; background: #f8fafc; border-radius: 9px; font-size: 12.2px; color: var(--ink-2); }}
  .meta p {{ margin: 0 0 5px; }}
  .meta p:last-child {{ margin-bottom: 0; }}
  .meta code {{ font-family: var(--mono); font-size: 11.6px; background: #fff; border: 1px solid var(--line); border-radius: 5px; padding: 1px 6px; margin-right: 5px; display: inline-block; margin-bottom: 3px; }}
  .empty {{ text-align: center; color: var(--ink-3); padding: 40px; font-size: 13.5px; }}
  .legend {{ display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: var(--ink-3); margin-top: 6px; }}
  @media (max-width: 900px) {{ .badges {{ margin-left: 0; }} }}
</style>
</head>
<body>
<div class="wrap">
  <header class="page-head">
    <div>
      <h1>企业人事管理系统 — 数据库 E-R 关系图与表结构速查</h1>
      <p>数据库 <code>enterprise_hr</code> · MySQL 8.0 · InnoDB · utf8mb4 / utf8mb4_0900_ai_ci ｜ 数据源：<code>enterprise_hr_schema.sql</code>（本页由脚本自动生成，与 DDL 严格一致）</p>
    </div>
    <div class="stats">
      <div class="stat"><b>{len(tables)}</b><span>数据表</span></div>
      <div class="stat"><b>{total_cols}</b><span>字段</span></div>
      <div class="stat"><b>{total_fks}</b><span>外键</span></div>
      <div class="stat"><b>{total_uks}</b><span>唯一键</span></div>
      <div class="stat"><b>10</b><span>功能模块</span></div>
    </div>
  </header>

  <section>
    <h2><span class="n">一</span>模块级 E-R 概览</h2>
    <p class="hint">「员工信息」是全系统的引用枢纽 —— 其余 9 个模块的业务数据几乎都通过外键挂到 <code>employee</code> 上；<code>dept</code> 与 <code>employee</code> 互为引用构成组织闭环。</p>
    {svg_module_map()}
  </section>

  <section>
    <h2><span class="n">二</span>员工全生命周期数据链</h2>
    <p class="hint">从招聘到离职，同一位员工在各模块持续产生关联记录；员工主数据只维护一份，业务明细按需引用。</p>
    {svg_lifecycle()}
  </section>

  <section>
    <h2><span class="n">三</span>表结构速查（{len(tables)} 张表 · {total_cols} 字段）</h2>
    <p class="hint">点击任意表展开字段明细。支持按表名、字段名、注释关键字搜索，并可按模块筛选。</p>
    <div class="toolbar">
      <input id="q" type="search" placeholder="搜索：表名 / 字段名 / 中文注释，例如「身份证」「请假」「salary」">
      <div class="chips"><button class="chip on" data-m="*" style="--c:#475569">全部<i>{len(tables)}</i></button>{chips}</div>
    </div>
    <div class="legend">
      <span><span class="tg tg-pk">PK</span> 主键</span>
      <span><span class="tg tg-uk">UQ</span> 唯一键</span>
      <span><span class="tg tg-fk">FK</span> 外键</span>
    </div>
    <div id="list">{''.join(cards)}</div>
    <div class="empty" id="empty" hidden>没有匹配的表，试试其他关键字。</div>
  </section>
</div>

<script>
  const q = document.getElementById('q');
  const cards = [...document.querySelectorAll('.tcard')];
  const chips = [...document.querySelectorAll('.chip')];
  const empty = document.getElementById('empty');
  let mod = '*';

  function apply() {{
    const kw = q.value.trim().toLowerCase();
    let shown = 0;
    cards.forEach(c => {{
      const okMod = mod === '*' || c.dataset.mod === mod;
      const okKw = !kw || c.dataset.key.toLowerCase().includes(kw);
      const vis = okMod && okKw;
      c.hidden = !vis;
      if (vis) shown++;
      if (okKw && kw && vis) c.open = true;
    }});
    empty.hidden = shown > 0;
  }}

  q.addEventListener('input', apply);
  chips.forEach(ch => ch.addEventListener('click', () => {{
    chips.forEach(x => x.classList.remove('on'));
    ch.classList.add('on');
    mod = ch.dataset.m;
    apply();
  }}));
</script>
</body>
</html>'''

dest = BASE / "docs" / "er-diagram.html"
dest.write_text(HTML, encoding="utf-8")
print(f"OK -> {dest}")
print(f"tables={len(tables)} cols={total_cols} fks={total_fks} uks={total_uks} chars={len(HTML)}")
print("cross-module FK:")
for (s, t), v in sorted(cross.items()):
    print(f"  {s} -> {t} : {v}")
