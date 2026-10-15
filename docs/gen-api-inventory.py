"""
从后端源码 + 前端 api/*.js 自动生成《接口清单》，用于交付文档。

产出：docs/api-inventory.md
用法：python docs/gen-api-inventory.py
"""

import glob
import io
import os
import re
import sys

sys.stdout.reconfigure(encoding="utf-8")

BACKEND = r"D:\Workspace\enterprise-hr-management"
FRONTEND = r"D:\Workspace\enterprise-hr-management-frontend"
OUT = os.path.join(BACKEND, "docs", "api-inventory.md")

MODULE_TITLE = {
    "auth": "认证与令牌",
    "dashboard": "首页看板",
    "employee": "组织与员工",
    "attendance": "考勤与假勤",
    "salary": "薪资管理",
    "recruit": "招聘管理",
    "training": "培训管理",
    "performance": "绩效管理",
    "notice": "通知公告",
    "system": "系统管理",
}
MODULE_ORDER = list(MODULE_TITLE.keys())


def norm(p):
    """把 {id} / {xxx} / ${xxx} 统一成占位符，便于与前端契约比对。"""
    p = re.sub(r"\$\{[^}]*\}", "{x}", p)
    return re.sub(r"\{[^}]*\}", "{x}", p)


def parse_backend():
    """扫描所有 Controller，抽出端点及其权限/日志注解。"""
    eps = []
    for f in glob.glob(os.path.join(BACKEND, "src/main/java/com/enterprise/hr/**/*Controller.java"),
                       recursive=True):
        src = io.open(f, encoding="utf-8").read()
        module = os.path.relpath(f, os.path.join(BACKEND, "src/main/java/com/enterprise/hr")).split(os.sep)[0]
        cls = re.search(r"class\s+(\w+)", src)
        cls = cls.group(1) if cls else os.path.basename(f)
        base_m = re.search(r'@RequestMapping\("([^"]*)"\)', src)
        base = base_m.group(1) if base_m else ""

        # 类级 @RequireRoles
        cls_roles = None
        head = src.split("class ", 1)[0]
        m = re.search(r"@RequireRoles\(\{?([^)}]*)\}?\)", head)
        if m:
            cls_roles = re.findall(r"RoleCode\.(\w+)", m.group(1))

        # 逐个端点：抓注解块 + 方法签名
        for mm in re.finditer(
                r"((?:\s*@\w+(?:\([^)]*\))?\s*)*)\s*public\s+[\w<>,.\s\[\]]+\s+(\w+)\s*\(",
                src):
            block, method_name = mm.group(1), mm.group(2)
            # 注意：注解名是驼峰（@GetMapping），HTTP 动词是全大写（GET）——两者不能混用
            ann = None
            for v in ("Get", "Post", "Put", "Delete", "Patch"):
                if re.search(r"@%sMapping" % v, block):
                    ann = v
                    break
            if not ann:
                continue
            verb = ann.upper()
            path_m = re.search(r'@%sMapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)")?' % ann, block)
            sub = (path_m.group(1) if path_m and path_m.group(1) else "")
            roles = cls_roles
            m = re.search(r"@RequireRoles\(\{?([^)}]*)\}?\)", block)
            if m:
                roles = re.findall(r"RoleCode\.(\w+)", m.group(1))
            module_m = re.search(r'@OperLog\([^)]*module\s*=\s*"([^"]*)"', block)
            oper_m = re.search(r"@OperLog\([^)]*type\s*=\s*OperType\.(\w+)", block)
            eps.append({
                "module": module,
                "cls": cls,
                "method": method_name,
                "verb": verb,
                "path": base + sub,
                "roles": roles,
                "log_module": module_m.group(1) if module_m else None,
                "log_type": oper_m.group(1) if oper_m else None,
            })
    return eps


def parse_frontend():
    """前端 api/*.js 里声明的 91 个请求地址（契约源）。"""
    fe = set()
    for f in glob.glob(os.path.join(FRONTEND, "src/api/*.js")):
        src = io.open(f, encoding="utf-8").read()
        for m in re.finditer(r"url:\s*[`'\"]([^`'\"]+)[`'\"]\s*,\s*method:\s*'(\w+)'", src):
            fe.add((m.group(2).upper(), norm(m.group(1))))
    return fe


def main():
    eps = parse_backend()
    fe = parse_frontend()
    fe_norm = set(fe)

    # 后端归一化集合，用于标注「契约内 / 兼容扩展」
    be_norm = {(e["verb"], norm(e["path"])) for e in eps}

    lines = []
    lines.append("# 后端接口清单（自动生成）\n")
    lines.append("> 由 `docs/gen-api-inventory.py` 扫描 Controller 源码生成，"
                 "并与前端 `src/api/*.js` 的请求地址逐一比对。\n")
    lines.append(f"- 后端端点总数：**{len(eps)}**（去重后 {len(be_norm)}）")
    lines.append(f"- 前端契约请求地址：**{len(fe_norm)}**")
    missing = sorted(fe_norm - be_norm)
    extra = sorted(be_norm - fe_norm)
    lines.append(f"- 前端契约未覆盖：**{len(missing)}**")
    lines.append(f"- 后端额外提供（兼容/扩展端点）：**{len(extra)}**\n")

    if missing:
        lines.append("\n## ⚠ 前端契约未覆盖的地址\n")
        for v, p in missing:
            lines.append(f"- `{v} {p}`")

    for mod in MODULE_ORDER:
        rows = [e for e in eps if e["module"] == mod]
        if not rows:
            continue
        rows.sort(key=lambda e: (e["path"], e["verb"]))
        lines.append(f"\n## {MODULE_TITLE[mod]}（`{mod}`）\n")
        lines.append("| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 | 契约 |")
        lines.append("|---|---|---|---|---|---|")
        for e in rows:
            roles = "、".join(e["roles"]) if e["roles"] else "—"
            log = f"{e['log_module']}/{e['log_type']}" if e["log_module"] else "—"
            in_contract = "✔ 契约内" if (e["verb"], norm(e["path"])) in fe_norm else "扩展"
            lines.append(f"| `{e['verb']}` | `{e['path']}` | {e['cls']}#{e['method']} "
                         f"| {roles} | {log} | {in_contract} |")

    # 其它模块（未在映射表里的）
    others = sorted({e["module"] for e in eps} - set(MODULE_ORDER))
    for mod in others:
        rows = [e for e in eps if e["module"] == mod]
        lines.append(f"\n## {mod}（其他）\n")
        lines.append("| 方法 | 路径 | 处理类#方法 | 角色要求 | 操作日志 |")
        lines.append("|---|---|---|---|---|")
        for e in sorted(rows, key=lambda x: (x["path"], x["verb"])):
            roles = "、".join(e["roles"]) if e["roles"] else "—"
            log = f"{e['log_module']}/{e['log_type']}" if e["log_module"] else "—"
            lines.append(f"| `{e['verb']}` | `{e['path']}` | {e['cls']}#{e['method']} "
                         f"| {roles} | {log} |")

    io.open(OUT, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    print(f"已生成 {OUT}")
    print(f"后端端点 {len(eps)} 个；前端契约 {len(fe_norm)} 个；未覆盖 {len(missing)} 个")
    if missing:
        for v, p in missing:
            print("  MISSING", v, p)


if __name__ == "__main__":
    main()
