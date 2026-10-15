# -*- coding: utf-8 -*-
"""对 enterprise_hr 库中全部外键做孤儿记录校验（左连接取右表主键为空的计数）。"""
import subprocess

MYSQL = r"D:\Apps\MySQL8.0\bin\mysql.exe"
BACKTICK = chr(96)


def q(sql):
    r = subprocess.run(
        [MYSQL, "-uroot", "-p123456", "--default-character-set=utf8mb4", "-N", "-B", "-e", sql],
        capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        print("SQL ERROR:", r.stderr[:800])
        return []
    return [l.split("\t") for l in r.stdout.strip().split("\n") if l.strip()]


def bt(s):
    return BACKTICK + s + BACKTICK


fks = q("""
SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME, CONSTRAINT_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA='enterprise_hr' AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, COLUMN_NAME;
""")
print("外键总数 =", len(fks))

parts = []
for t, c, rt, rc, cn in fks:
    parts.append(
        "SELECT '{cn}' AS fk_name, '{t}.{c}' AS rel, COUNT(*) AS orphans "
        "FROM enterprise_hr.{t2} x LEFT JOIN enterprise_hr.{rt2} y "
        "ON x.{c2} = y.{rc2} WHERE x.{c2} IS NOT NULL AND y.{rc2} IS NULL".format(
            cn=cn, t=t, c=c, t2=bt(t), c2=bt(c), rt2=bt(rt), rc2=bt(rc)))

sql = " UNION ALL ".join(parts) + ";"
res = q(sql)
bad = [r for r in res if r[2] != "0"]
print("检查条数 =", len(res))
print("存在孤儿的检查 =", len(bad))
for r in bad:
    print("  !!", r)
if len(res) == len(fks) and not bad:
    print("PASS: 全部 %d 个外键约束下均无孤儿记录" % len(fks))
