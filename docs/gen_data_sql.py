# -*- coding: utf-8 -*-
"""
生成企业人事管理系统的完整数据初始化脚本 data.sql。

- 50 名员工 / 51 个系统账号 / 13 个部门 / 16 个岗位
- 其余 48 张表按真实业务场景生成（考勤逐日 2 个月、薪资 2 个月含薪资条明细、
  招聘/培训/绩效全链路、公告待办审批流水、系统日志）
- 全表显式主键 ID，可重复执行，输出稳定可复现
- attendance_month 由逐日打卡明细真实聚合，保证两表口径一致
"""
import random
from datetime import date, datetime, timedelta

random.seed(20260914)

BASE = r"D:/Workspace/enterprise-hr-management"
TODAY = date(2026, 9, 14)
BCRYPT = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2"
PAY_MONTHS = [(2026, 7), (2026, 8)]
ATT_MONTHS = [(2026, 7), (2026, 8)]

out = []


def L(s=""):
    out.append(s)


def q(v):
    if v is None:
        return "NULL"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def N(v):
    return "NULL" if v is None else str(v)


def D(d):
    return "NULL" if d is None else "'" + d.isoformat() + "'"


def TS(d):
    return "NULL" if d is None else "'" + d.strftime("%Y-%m-%d %H:%M:%S") + "'"


def M(x, nd=2):
    return "NULL" if x is None else ("%.*f" % (nd, float(x)))


TABLE_COUNTS = {}


def emit(table, cols, rows, batch=100):
    if not rows:
        TABLE_COUNTS.setdefault(table, 0)
        return
    TABLE_COUNTS[table] = TABLE_COUNTS.get(table, 0) + len(rows)
    colsql = ", ".join("`" + c + "`" for c in cols)
    for i in range(0, len(rows), batch):
        chunk = rows[i:i + batch]
        L("INSERT INTO `%s` (%s) VALUES" % (table, colsql))
        L(",\n".join("  (" + ", ".join(r) + ")" for r in chunk) + ";")
        L()


def workdays(y, m):
    d = date(y, m, 1)
    res = []
    while d.month == m:
        if d.weekday() < 5:
            res.append(d)
        d += timedelta(days=1)
    return res


def hm(d, h, mi=0):
    """以 h 点为零点、mi 分钟为偏移构造时间戳（mi 允许为负，用于表示提前打卡）。"""
    return datetime(d.year, d.month, d.day) + timedelta(hours=h, minutes=mi)


def r2(x):
    return round(float(x) + 1e-9, 2)


# ====================================================================================
# 基础主数据
# ====================================================================================
# (id, 部门编码, 名称, 上级, 组织类型, 层级, 层级路径, 编制上限, 电话)
DEPT = [
    (1,  "D1000", "总公司",       None, 1, 1, "/1/",      80, "010-88880000"),
    (2,  "D1001", "技术中心",     1,    2, 2, "/1/2/",    40, "010-88880001"),
    (3,  "D1002", "产品中心",     1,    2, 2, "/1/3/",    12, "010-88880002"),
    (4,  "D1003", "职能中心",     1,    2, 2, "/1/4/",    16, "010-88880003"),
    (5,  "D1004", "后端开发部",   2,    3, 3, "/1/2/5/",  20, "010-88880004"),
    (6,  "D1005", "前端开发部",   2,    3, 3, "/1/2/6/",  10, "010-88880005"),
    (7,  "D1006", "测试与质量部", 2,    3, 3, "/1/2/7/",   8, "010-88880006"),
    (8,  "D1007", "产品设计部",   3,    3, 3, "/1/3/8/",   8, "010-88880007"),
    (9,  "D1008", "人力资源部",   4,    3, 3, "/1/4/9/",   6, "010-88880008"),
    (10, "D1009", "财务部",       4,    3, 3, "/1/4/10/",  5, "010-88880009"),
    (11, "D1010", "行政部",       4,    3, 3, "/1/4/11/",  4, "010-88880010"),
    (12, "D1011", "市场部",       1,    2, 2, "/1/12/",    6, "010-88880011"),
    (13, "D1012", "总裁办",       1,    2, 2, "/1/13/",    4, "010-88880012"),
]

# (id, 岗位编码, 岗位名称, 序列, 职级区间, 所属部门, 职责, 任职要求)
POSITION = [
    (1,  "P-TECH-001", "Java开发工程师", 2, "P4-P7", 5,  "负责后端服务的需求分析、设计、开发与线上维护，参与技术方案评审", "本科及以上，计算机相关专业，3 年以上 Java 后端开发经验，熟悉 Spring 生态与 MySQL"),
    (2,  "P-TECH-002", "前端开发工程师", 2, "P4-P7", 6,  "负责 Web / 移动端页面开发与交互实现，保障多端兼容与性能体验", "本科及以上，计算机相关专业，3 年以上前端开发经验，熟练掌握 Vue/React 技术栈"),
    (3,  "P-TECH-003", "测试工程师",     2, "P4-P6", 7,  "负责功能测试、接口自动化测试、性能测试与质量度量，把控发布质量", "本科及以上，计算机相关专业，2 年以上测试经验，熟悉主流自动化测试框架"),
    (4,  "P-TECH-004", "架构师",         2, "P7-P9", 5,  "负责系统架构设计、技术选型、核心难点攻关与研发规范制定", "本科及以上，8 年以上研发经验，精通分布式架构与高并发系统设计"),
    (5,  "P-TECH-005", "技术总监",       1, "M2-M4", 2,  "统筹技术中心研发管理、团队建设、技术规划与跨部门协作", "本科及以上，10 年以上研发与管理经验，具备 30 人以上团队管理经历"),
    (6,  "P-PROD-001", "产品经理",       3, "P5-P8", 8,  "负责产品规划、需求调研分析、版本迭代管理与上线效果复盘", "本科及以上，5 年以上产品经验，有 B 端系统产品设计经验优先"),
    (7,  "P-PROD-002", "产品助理",       3, "P3-P5", 8,  "协助产品经理完成需求整理、原型绘制与产品文档编写", "本科及以上，1 年以上产品相关经验，逻辑清晰、文档能力强"),
    (8,  "P-PROD-003", "产品总监",       1, "M2-M4", 3,  "统筹产品中心规划、产品矩阵布局与产品团队管理", "本科及以上，10 年以上产品经验，具备完整产品线操盘经历"),
    (9,  "P-HR-001",   "HR专员",         3, "P4-P6", 9,  "负责招聘执行、员工关系、考勤统计与人事手续办理", "本科及以上，2 年以上 HR 相关工作经验，熟悉招聘与员工关系模块"),
    (10, "P-HR-002",   "HR经理",         1, "M1-M3", 9,  "统筹人力资源各模块，制定并落地人力资源政策", "本科及以上，8 年以上 HR 经验，熟悉劳动法与人力六大模块"),
    (11, "P-FIN-001",  "会计",           3, "P4-P7", 10, "负责账务处理、薪资核算、税务申报与财务报表编制", "本科及以上，3 年以上财务核算经验，持有中级会计师职称优先"),
    (12, "P-FIN-002",  "财务经理",       1, "M1-M3", 10, "统筹财务管理、预算控制、资金规划与财务合规", "本科及以上，8 年以上财务经验，熟悉税务筹划与预算管理"),
    (13, "P-ADM-001",  "行政专员",       4, "P3-P5", 11, "负责行政后勤、办公资产管理、会务与员工活动组织", "大专及以上，2 年以上行政相关经验，细致耐心、沟通能力强"),
    (14, "P-MKT-001",  "市场专员",       3, "P4-P6", 12, "负责市场推广、品牌宣传、渠道运营与活动策划执行", "本科及以上，3 年以上市场推广经验，具备活动策划与渠道运营能力"),
    (15, "P-EXE-001",  "总经理",         1, "M4-M5", 13, "统筹公司整体经营，制定战略方向与年度经营目标", "本科及以上，15 年以上工作经验，具备大型企业全面经营管理经验"),
    (16, "P-EXE-002",  "总裁助理",       3, "P4-P6", 13, "协助总经理处理日常事务、会议纪要、跨部门协调与行程安排", "本科及以上，3 年以上总裁办或助理岗位经验，文字与协调能力突出"),
]

# (id, 工号, 姓名, 拼音账号, 性别, 出生日期, 部门, 岗位, 职级, 用工性质,
#  入职日期, 转正日期, 月薪, 在职状态, 学历, 院校, 专业)
EMP = [
    (1001, "E0001", "沈建国", "shenjianguo", "M", "1978-02-14", 13, 15, "M4", 1, "2018-03-01", "2018-06-01", 60000, 1, 2, "清华大学", "工商管理"),
    (1002, "E0002", "林晚晴", "linwanqing", "F", "1996-09-27", 13, 16, "P5", 1, "2021-06-15", "2021-09-15", 18000, 1, 3, "中国人民大学", "汉语言文学"),
    (1003, "E0003", "郑十一", "zhengshiyi", "M", "1985-04-08", 2, 5, "M3", 1, "2019-04-08", "2019-07-08", 45000, 1, 2, "北京航空航天大学", "计算机科学与技术"),
    (1004, "E0004", "冯超", "fengchao", "M", "1988-01-19", 5, 4, "P8", 1, "2019-07-01", "2019-10-01", 38000, 1, 2, "华中科技大学", "软件工程"),
    (1005, "E0005", "张三", "zhangsan", "M", "1998-05-12", 5, 1, "P6", 1, "2023-03-15", "2023-06-15", 22000, 1, 3, "北京邮电大学", "软件工程"),
    (1006, "E0006", "陈浩", "chenhao", "M", "1995-11-23", 5, 1, "P6", 1, "2022-05-09", "2022-08-09", 21000, 1, 3, "大连理工大学", "计算机科学与技术"),
    (1007, "E0007", "刘洋", "liuyang", "M", "1994-07-03", 5, 1, "P6", 1, "2022-08-22", "2022-11-22", 20500, 1, 2, "东南大学", "软件工程"),
    (1008, "E0008", "杨帆", "yangfan", "M", "1997-03-30", 5, 1, "P5", 1, "2023-07-03", "2023-10-03", 18000, 1, 3, "北京理工大学", "计算机科学与技术"),
    (1009, "E0009", "黄磊", "huanglei", "M", "1996-12-05", 5, 1, "P5", 1, "2023-09-11", "2023-12-11", 17500, 1, 3, "西安交通大学", "软件工程"),
    (1010, "E0010", "徐磊", "xulei", "M", "1997-06-18", 5, 1, "P5", 1, "2024-02-19", "2024-05-19", 17000, 1, 3, "哈尔滨工业大学", "计算机科学与技术"),
    (1011, "E0011", "马晓东", "maxiaodong", "M", "1998-10-11", 5, 1, "P5", 1, "2024-04-08", "2024-07-08", 16800, 1, 3, "北京邮电大学", "信息管理与信息系统"),
    (1012, "E0012", "朱伟", "zhuwei", "M", "1999-02-26", 5, 1, "P4", 1, "2024-07-15", "2024-10-15", 14000, 1, 3, "首都经济贸易大学", "信息管理与信息系统"),
    (1013, "E0013", "胡军", "hujun", "M", "1998-08-09", 5, 1, "P4", 1, "2024-09-02", "2024-12-02", 13800, 1, 3, "北京信息科技大学", "计算机科学与技术"),
    (1014, "E0014", "郭鹏", "guopeng", "M", "1999-05-21", 5, 1, "P4", 1, "2025-01-06", "2025-04-06", 13500, 1, 3, "四川大学", "软件工程"),
    (1015, "E0015", "何鑫", "hexin", "M", "2000-01-14", 5, 1, "P4", 1, "2025-03-17", "2025-06-17", 13200, 1, 3, "南京大学", "计算机科学与技术"),
    (1016, "E0016", "高翔", "gaoxiang", "M", "2000-07-08", 5, 1, "P4", 1, "2025-06-09", "2025-09-09", 13000, 1, 3, "武汉大学", "软件工程"),
    (1017, "E0017", "罗宇", "luoyu", "M", "2001-04-02", 5, 1, "P4", 1, "2025-09-01", "2025-12-01", 12800, 1, 3, "同济大学", "信息管理与信息系统"),
    (1018, "E0018", "宋涛", "songtao", "M", "2002-06-11", 5, 1, "P3", 1, "2026-06-01", "2026-09-01", 11000, 1, 3, "北京信息科技大学", "计算机科学与技术"),
    (1019, "E0019", "唐嘉", "tangjia", "F", "2002-02-18", 5, 1, "P3", 2, "2026-07-01", None, 11000, 2, 3, "北京邮电大学", "软件工程"),
    (1020, "E0020", "韩雪", "hanxue", "F", "1994-03-06", 6, 2, "P6", 1, "2022-11-07", "2023-02-07", 21000, 1, 3, "复旦大学", "计算机科学与技术"),
    (1021, "E0021", "李四", "lisi", "F", "2000-08-20", 6, 2, "P5", 1, "2023-05-20", "2023-08-20", 18000, 1, 3, "上海交通大学", "软件工程"),
    (1022, "E0022", "曹雨", "caoyu", "F", "1998-11-15", 6, 2, "P5", 1, "2023-10-16", "2024-01-16", 17500, 1, 3, "浙江大学", "数字媒体技术"),
    (1023, "E0023", "邓佳", "dengjia", "F", "1999-09-03", 6, 2, "P4", 1, "2024-06-03", "2024-09-03", 14000, 1, 3, "北京理工大学", "软件工程"),
    (1024, "E0024", "许静", "xujing", "F", "2000-12-28", 6, 2, "P4", 1, "2025-02-10", "2025-05-10", 13600, 1, 3, "北京信息科技大学", "计算机科学与技术"),
    (1025, "E0025", "谢婷", "xieting", "F", "2001-07-19", 6, 2, "P4", 1, "2025-05-19", "2025-08-19", 13300, 1, 3, "中山大学", "软件工程"),
    (1026, "E0026", "王五", "wangwu", "M", "1994-02-08", 7, 3, "P6", 1, "2022-08-10", "2022-11-10", 20000, 1, 2, "天津大学", "计算机科学与技术"),
    (1027, "E0027", "孙八", "sunba", "M", "2002-03-25", 7, 3, "P5", 1, "2024-06-01", "2024-09-01", 17000, 1, 3, "北京邮电大学", "软件工程"),
    (1028, "E0028", "周九", "zhoujiu", "F", "1999-09-14", 7, 3, "P4", 1, "2024-03-10", "2024-06-10", 13500, 1, 3, "西安电子科技大学", "计算机科学与技术"),
    (1029, "E0029", "吴十", "wushi", "M", "1998-04-17", 7, 3, "P4", 1, "2025-04-14", "2025-07-14", 13200, 1, 3, "北京信息科技大学", "软件工程"),
    (1030, "E0030", "赵六", "zhaoliu", "F", "1986-05-11", 3, 8, "M2", 1, "2020-05-11", "2020-08-11", 32000, 1, 2, "北京大学", "工商管理"),
    (1031, "E0031", "钱七", "qianqi", "M", "1992-09-05", 8, 6, "P7", 1, "2022-09-05", "2022-12-05", 26000, 1, 3, "南京大学", "信息管理与信息系统"),
    (1032, "E0032", "陈静", "chenjing", "F", "2000-03-22", 8, 7, "P4", 1, "2024-08-19", "2024-11-19", 13000, 1, 3, "武汉大学", "市场营销"),
    (1033, "E0033", "王十二", "wangshier", "F", "2001-01-08", 8, 7, "P4", 1, "2025-03-03", "2025-06-03", 12800, 1, 3, "北京师范大学", "心理学"),
    (1034, "E0034", "李娜", "lina", "F", "1997-07-25", 9, 9, "P5", 1, "2023-01-09", "2023-04-09", 15000, 1, 3, "中国人民大学", "人力资源管理"),
    (1035, "E0035", "张伟", "zhangwei", "M", "1999-11-02", 9, 9, "P4", 1, "2024-05-20", "2024-08-20", 13000, 1, 3, "首都经济贸易大学", "人力资源管理"),
    (1036, "E0036", "刘芳", "liufang", "F", "1987-08-13", 9, 10, "M2", 1, "2020-11-02", "2021-02-02", 26000, 1, 2, "中国人民大学", "人力资源管理"),
    (1037, "E0037", "王强", "wangqiang", "M", "1986-12-01", 10, 12, "M2", 1, "2019-09-16", "2019-12-16", 28000, 1, 3, "中央财经大学", "会计学"),
    (1038, "E0038", "李敏", "limin", "F", "1995-06-06", 10, 11, "P5", 1, "2022-06-13", "2022-09-13", 16000, 1, 3, "对外经济贸易大学", "财务管理"),
    (1039, "E0039", "张丽", "zhangli", "F", "1999-03-17", 10, 11, "P4", 1, "2024-10-08", "2025-01-08", 13500, 1, 3, "首都经济贸易大学", "会计学"),
    (1040, "E0040", "刘鹏", "liupeng", "M", "1994-10-24", 11, 13, "P4", 1, "2023-11-27", "2024-02-27", 12000, 1, 3, "北京联合大学", "行政管理"),
    (1041, "E0041", "王娟", "wangjuan", "F", "2001-05-30", 11, 13, "P3", 1, "2025-07-07", "2025-10-07", 10500, 1, 4, "北京城市学院", "行政管理"),
    (1042, "E0042", "李明", "liming", "M", "1996-02-13", 12, 14, "P5", 1, "2023-04-10", "2023-07-10", 16000, 1, 3, "中国传媒大学", "市场营销"),
    (1043, "E0043", "张涛", "zhangtao", "M", "1998-09-28", 12, 14, "P4", 1, "2024-09-23", "2024-12-23", 13500, 1, 3, "北京第二外国语学院", "市场营销"),
    (1044, "E0044", "刘丹", "liudan", "F", "2003-04-09", 5, 1, "P4", 2, "2026-08-03", None, 13000, 2, 3, "北京邮电大学", "计算机科学与技术"),
    (1045, "E0045", "王磊", "wanglei", "M", "2003-08-16", 6, 2, "P3", 2, "2026-08-10", None, 11000, 2, 3, "北京信息科技大学", "软件工程"),
    (1046, "E0046", "李静", "lijing", "F", "2003-01-27", 7, 3, "P3", 2, "2026-08-17", None, 10500, 2, 3, "北京联合大学", "软件工程"),
    (1047, "E0047", "张敏", "zhangmin", "F", "2003-06-23", 9, 9, "P3", 1, "2026-09-15", None, 10000, 3, 3, "首都经济贸易大学", "人力资源管理"),
    (1048, "E0048", "刘杰", "liujie", "M", "1993-11-08", 5, 1, "P5", 1, "2021-05-10", "2021-08-10", 17500, 0, 3, "北京工业大学", "计算机科学与技术"),
    (1049, "E0049", "王芳", "wangfang", "F", "1992-07-04", 6, 2, "P5", 1, "2021-08-16", "2021-11-16", 17200, 0, 3, "北方工业大学", "软件工程"),
    (1050, "E0050", "李强", "liqiang", "M", "1991-03-20", 8, 6, "P6", 1, "2022-07-04", "2022-10-04", 20500, 0, 3, "北京物资学院", "市场营销"),
]

EMP_BY_ID = {e[0]: e for e in EMP}
POS_NAME = {p[0]: p[2] for p in POSITION}

MANAGER = {
    1002: 1001, 1003: 1001, 1030: 1001, 1036: 1001, 1004: 1003, 1020: 1003, 1026: 1003,
    1005: 1004, 1006: 1004, 1007: 1004, 1008: 1004, 1009: 1004, 1010: 1004, 1011: 1004,
    1012: 1004, 1013: 1004, 1014: 1004, 1015: 1004, 1016: 1004, 1017: 1004, 1018: 1004,
    1019: 1004, 1044: 1004, 1048: 1004,
    1021: 1020, 1022: 1020, 1023: 1020, 1024: 1020, 1025: 1020, 1045: 1020, 1049: 1020,
    1027: 1026, 1028: 1026, 1029: 1026, 1046: 1026,
    1031: 1030, 1032: 1031, 1033: 1031, 1050: 1031,
    1034: 1036, 1035: 1036, 1047: 1036, 1040: 1036,
    1037: 1001, 1038: 1037, 1039: 1037,
    1041: 1040, 1042: 1001, 1043: 1042,
}

DEPT_LEADER = {1: 1001, 2: 1003, 3: 1030, 4: 1036, 5: 1004, 6: 1020, 7: 1026,
               8: 1031, 9: 1036, 10: 1037, 11: 1040, 12: 1042, 13: 1001}

LEAVE_DATE = {1048: date(2026, 6, 30), 1049: date(2026, 5, 15), 1050: date(2026, 7, 20)}
LEAVE_REASON = {1048: "个人职业发展，主动离职", 1049: "家庭原因，随迁至外地", 1050: "个人原因，主动离职"}

# 在职（含试用）员工 = 需要生成考勤/薪资的对象；排除待入职与已离职
ACTIVE = [e for e in EMP if e[13] in (1, 2)]
ACTIVE_IDS = [e[0] for e in ACTIVE]

# 当月在职（用于薪资/社保）—— 判断员工在指定月份是否有在册记录
def employed_in(eid, y, m):
    e = EMP_BY_ID[eid]
    d0 = date.fromisoformat(e[10])
    d1 = LEAVE_DATE.get(eid)
    first = date(y, m, 1)
    if d0 > first:
        return False
    if d1 and d1 < first:
        return False
    return True


# ====================================================================================
# 文件头 + 清理
# ====================================================================================
L("-- ====================================================================================")
L("--  企业人事管理系统 — 完整数据初始化脚本（data.sql）")
L("-- ====================================================================================")
L("--  数据库：enterprise_hr（InnoDB / utf8mb4 / utf8mb4_0900_ai_ci）")
L("--  依赖：必须先执行 enterprise_hr_schema.sql 完成建库建表")
L("-- ------------------------------------------------------------------------------------")
L("--  用法（Windows 必须带 --default-character-set=utf8mb4，否则客户端走 gbk 中文会乱码）：")
L("--      mysql -uroot -p123456 --default-character-set=utf8mb4 < enterprise_hr_schema.sql")
L("--      mysql -uroot -p123456 --default-character-set=utf8mb4 < data.sql")
L("--")
L("--  脚本特性：")
L("--    1. 可重复执行：开头清空全部业务表，不会产生重复数据")
L("--    2. 全表显式指定主键 ID，输出稳定可复现")
L("--    3. 覆盖全部 49 张表，按 9 大功能模块的真实业务场景组织")
L("--")
L("--  数据规模：")
L("--    13 个部门 / 16 个岗位 / 50 名员工 / 51 个系统账号")
L("--    50 份劳动合同 / 异动记录 / 员工附件")
L("--    2 个月逐日考勤明细 + 月度汇总（由明细聚合，口径一致）/ 排班 / 假期余额")
L("--    请假·加班·出差申请 / 2 个月薪资核算（含薪资条逐项明细）/ 社保公积金缴纳")
L("--    招聘全链路：需求→职位发布→候选人→面试→Offer")
L("--    培训全链路：讲师→课程→计划→报名签到→培训档案")
L("--    绩效全链路：考核方案→指标→考核记录→绩效面谈")
L("--    公告待办 / 通用审批流水 / 登录与操作日志")
L("--")
L("--  演示账号（密码统一 123456，BCrypt 存储；生产环境请立即修改）：")
L("--    admin        系统管理员（非员工账号）")
L("--    shenjianguo  总经理            zhengshiyi  技术总监")
L("--    zhaoliu      产品总监          liufang     HR经理")
L("--    wangqiang    财务经理          hanxue      前端开发部负责人")
L("--    wangwu       测试与质量部负责人  huanglei    后端开发工程师（普通员工视角）")
L("-- ====================================================================================")
L()
L("SET NAMES utf8mb4;")
L("SET FOREIGN_KEY_CHECKS = 0;")
L("USE `enterprise_hr`;")
L()
L("-- ------------------------------------------------------------------------------------")
L("--  清理历史数据（FK 检查已关闭，表顺序无关；dept 负责人先置空以解除循环引用）")
L("-- ------------------------------------------------------------------------------------")
CLEAN = ["approval_record", "sys_oper_log", "sys_login_log", "todo_task", "notice_read",
         "notice", "performance_interview", "performance_record", "performance_indicator",
         "performance_plan", "training_record", "training_enroll", "course", "training_plan",
         "trainer", "offer_record", "interview_record", "candidate", "job_posting",
         "recruit_request", "attendance_month", "business_trip", "overtime_request",
         "leave_request", "leave_balance", "employee_schedule", "attendance_record",
         "insurance_record", "salary_item", "salary_record", "employee_salary",
         "insurance_config", "salary_standard", "leave_type", "shift",
         "sys_role_menu", "sys_user_role", "sys_user", "employee_attachment",
         "employee_change", "employee_contract", "employee", "position", "dept",
         "sys_menu", "sys_role", "sys_dict_item", "sys_dict_type", "sys_config"]
L("UPDATE `dept` SET `leader_id` = NULL;")
for t in CLEAN:
    L("DELETE FROM `%s`;" % t)
L()

# ====================================================================================
# 一、系统设置
# ====================================================================================
L("-- ####################################################################################")
L("--  一、系统设置 —— 数据字典 / 菜单 / 角色 / 参数")
L("-- ####################################################################################")
L()
L("-- 数据字典类型（12 个）")
emit("sys_dict_type", ["id", "dict_type", "dict_name", "remark"], [
    ["1", q("gender"), q("性别"), q("与 employee.gender 对应，存 M/F/U")],
    ["2", q("education"), q("学历"), q("与 employee.education 状态码对应")],
    ["3", q("marital_status"), q("婚姻状况"), q("与 employee.marital_status 对应")],
    ["4", q("political_status"), q("政治面貌"), q("与 employee.political_status 对应")],
    ["5", q("employee_status"), q("在职状态"), q("与 employee.status 对应")],
    ["6", q("employee_type"), q("用工性质"), q("与 employee.employee_type 对应")],
    ["7", q("contract_type"), q("合同类型"), q("与 employee_contract.contract_type 对应")],
    ["8", q("change_type"), q("异动类型"), q("与 employee_change.change_type 对应")],
    ["9", q("overtime_type"), q("加班类型"), q("与 overtime_request.overtime_type 对应")],
    ["10", q("approval_status"), q("审批状态"), q("各申请单通用审批状态")],
    ["11", q("position_seq"), q("岗位序列"), q("与 position.position_seq 对应")],
    ["12", q("performance_grade"), q("绩效等级"), q("与 performance_record.grade 对应")],
])

L("-- 数据字典项（12 类共 55 项）")
DICT_ITEMS = [
    (1, [("M", "男"), ("F", "女"), ("U", "未知")]),
    (2, [("1", "博士"), ("2", "硕士"), ("3", "本科"), ("4", "大专"), ("5", "中专"), ("6", "高中"), ("7", "其他")]),
    (3, [("1", "未婚"), ("2", "已婚"), ("3", "离异"), ("4", "丧偶")]),
    (4, [("1", "群众"), ("2", "共青团员"), ("3", "中共党员"), ("4", "民主党派")]),
    (5, [("0", "离职"), ("1", "在职"), ("2", "试用"), ("3", "待入职")]),
    (6, [("1", "正式"), ("2", "试用"), ("3", "实习"), ("4", "兼职"), ("5", "劳务")]),
    (7, [("1", "固定期限"), ("2", "无固定期限"), ("3", "以完成任务为期限"), ("4", "实习协议"), ("5", "劳务协议")]),
    (8, [("1", "入职"), ("2", "转正"), ("3", "调岗"), ("4", "调部门"), ("5", "晋升"), ("6", "降级"), ("7", "离职"), ("8", "返聘")]),
    (9, [("1", "工作日加班"), ("2", "休息日加班"), ("3", "法定节假日加班")]),
    (10, [("0", "草稿"), ("1", "审批中"), ("2", "已通过"), ("3", "已驳回"), ("4", "已撤销")]),
    (11, [("1", "管理"), ("2", "技术"), ("3", "职能"), ("4", "操作")]),
    (12, [("S", "卓越"), ("A", "优秀"), ("B", "良好"), ("C", "待改进"), ("D", "不合格")]),
]
rows, di = [], 0
for tid, items in DICT_ITEMS:
    for i, (val, label) in enumerate(items, 1):
        di += 1
        rows.append([N(di), N(tid), q(val), q(label), N(i),
                     "1" if i == 1 else "0", "NULL", "1", "NULL"])
emit("sys_dict_item", ["id", "dict_type_id", "item_value", "item_label", "sort_no",
                       "is_default", "css_class", "status", "remark"], rows)

L("-- 菜单权限（9 个一级目录 + 52 个功能菜单 + 4 个按钮 = 61 条）")
MENUS = [
    (1, None, "首页", 1, "/dashboard", "dashboard:view", "HomeFilled", 1),
    (2, None, "员工信息", 1, "/employee", "employee:view", "User", 2),
    (3, None, "部门管理", 1, "/dept", "dept:view", "OfficeBuilding", 3),
    (4, None, "考勤管理", 1, "/attendance", "attendance:view", "Clock", 4),
    (5, None, "薪资管理", 1, "/salary", "salary:view", "Money", 5),
    (6, None, "招聘管理", 1, "/recruit", "recruit:view", "Search", 6),
    (7, None, "培训管理", 1, "/training", "training:view", "Reading", 7),
    (8, None, "绩效考核", 1, "/performance", "performance:view", "TrendCharts", 8),
    (9, None, "系统设置", 1, "/system", "system:view", "Setting", 9),
    (11, 1, "数据概览", 2, "/dashboard/index", "dashboard:index", None, 1),
    (12, 1, "待办事项", 2, "/dashboard/todo", "dashboard:todo", None, 2),
    (13, 1, "通知公告", 2, "/dashboard/notice", "dashboard:notice", None, 3),
    (21, 2, "员工档案", 2, "/employee/list", "employee:list", None, 1),
    (22, 2, "入职办理", 2, "/employee/onboard", "employee:onboard", None, 2),
    (23, 2, "调动转岗", 2, "/employee/transfer", "employee:transfer", None, 3),
    (24, 2, "离职办理", 2, "/employee/leave", "employee:leave", None, 4),
    (25, 2, "合同管理", 2, "/employee/contract", "employee:contract", None, 5),
    (31, 3, "组织架构", 2, "/dept/tree", "dept:tree", None, 1),
    (32, 3, "部门维护", 2, "/dept/list", "dept:list", None, 2),
    (33, 3, "岗位管理", 2, "/dept/position", "dept:position", None, 3),
    (34, 3, "人员编制", 2, "/dept/headcount", "dept:headcount", None, 4),
    (41, 4, "打卡记录", 2, "/attendance/record", "attendance:record", None, 1),
    (42, 4, "请假申请", 2, "/attendance/leave", "attendance:leave", None, 2),
    (43, 4, "加班申请", 2, "/attendance/overtime", "attendance:overtime", None, 3),
    (44, 4, "出差管理", 2, "/attendance/trip", "attendance:trip", None, 4),
    (45, 4, "排班管理", 2, "/attendance/shift", "attendance:shift", None, 5),
    (46, 4, "考勤统计", 2, "/attendance/stat", "attendance:stat", None, 6),
    (51, 5, "薪资标准", 2, "/salary/standard", "salary:standard", None, 1),
    (52, 5, "薪资核算", 2, "/salary/calc", "salary:calc", None, 2),
    (53, 5, "薪资发放", 2, "/salary/pay", "salary:pay", None, 3),
    (54, 5, "社保公积金", 2, "/salary/insurance", "salary:insurance", None, 4),
    (55, 5, "个税申报", 2, "/salary/tax", "salary:tax", None, 5),
    (56, 5, "薪资条查询", 2, "/salary/payslip", "salary:payslip", None, 6),
    (61, 6, "招聘需求", 2, "/recruit/request", "recruit:request", None, 1),
    (62, 6, "职位发布", 2, "/recruit/posting", "recruit:posting", None, 2),
    (63, 6, "简历筛选", 2, "/recruit/candidate", "recruit:candidate", None, 3),
    (64, 6, "面试安排", 2, "/recruit/interview", "recruit:interview", None, 4),
    (65, 6, "录用审批", 2, "/recruit/offer", "recruit:offer", None, 5),
    (66, 6, "人才库", 2, "/recruit/talent", "recruit:talent", None, 6),
    (71, 7, "培训计划", 2, "/training/plan", "training:plan", None, 1),
    (72, 7, "课程管理", 2, "/training/course", "training:course", None, 2),
    (73, 7, "讲师管理", 2, "/training/trainer", "training:trainer", None, 3),
    (74, 7, "报名签到", 2, "/training/enroll", "training:enroll", None, 4),
    (75, 7, "培训考核", 2, "/training/exam", "training:exam", None, 5),
    (76, 7, "培训档案", 2, "/training/record", "training:record", None, 6),
    (81, 8, "考核方案", 2, "/performance/plan", "performance:plan", None, 1),
    (82, 8, "指标设置", 2, "/performance/kpi", "performance:kpi", None, 2),
    (83, 8, "绩效评分", 2, "/performance/score", "performance:score", None, 3),
    (84, 8, "绩效面谈", 2, "/performance/interview", "performance:interview", None, 4),
    (85, 8, "结果归档", 2, "/performance/archive", "performance:archive", None, 5),
    (86, 8, "绩效分析", 2, "/performance/analysis", "performance:analysis", None, 6),
    (91, 9, "用户管理", 2, "/system/user", "system:user", None, 1),
    (92, 9, "角色权限", 2, "/system/role", "system:role", None, 2),
    (93, 9, "菜单管理", 2, "/system/menu", "system:menu", None, 3),
    (94, 9, "数据字典", 2, "/system/dict", "system:dict", None, 4),
    (95, 9, "参数配置", 2, "/system/config", "system:config", None, 5),
    (96, 9, "日志审计", 2, "/system/log", "system:log", None, 6),
    (211, 21, "新增员工", 3, None, "employee:add", None, 1),
    (212, 21, "修改员工", 3, None, "employee:edit", None, 2),
    (213, 21, "删除员工", 3, None, "employee:remove", None, 3),
    (214, 21, "导出员工", 3, None, "employee:export", None, 4),
]
emit("sys_menu", ["id", "parent_id", "menu_name", "menu_type", "path", "perms", "icon", "sort_no"],
     [[N(i), N(p), q(n), N(mt), q(pa), q(pe), q(ic), N(so)]
      for i, p, n, mt, pa, pe, ic, so in MENUS])

L("-- 角色（对应《需求规格说明书》R1~R6 六类角色）")
emit("sys_role", ["id", "role_code", "role_name", "description", "data_scope", "role_type", "sort_no"], [
    ["1", q("ROLE_ADMIN"), q("系统管理员"), q("负责系统基础配置：用户、角色、菜单、字典、参数及日志审计，不参与人事业务审批"), "1", "1", "1"],
    ["2", q("ROLE_HR"), q("人事专员"), q("员工全生命周期管理、部门岗位维护、招聘培训组织、考勤汇总、薪资核算与发放"), "1", "1", "2"],
    ["3", q("ROLE_MANAGER"), q("部门主管"), q("本部门员工查看、考勤与请假审批、绩效指标设定与评分、招聘需求提报"), "3", "1", "3"],
    ["4", q("ROLE_FINANCE"), q("财务人员"), q("薪资标准维护、薪资核算复核与发放、社保公积金与个税申报"), "1", "1", "4"],
    ["5", q("ROLE_EMPLOYEE"), q("普通员工"), q("查看个人档案与薪资条、考勤打卡、请假加班出差申请、培训报名、查看绩效结果"), "4", "1", "5"],
    ["6", q("ROLE_EXECUTIVE"), q("决策层"), q("跨部门数据看板与报表分析，查看人力成本、编制占用、绩效分布等汇总指标"), "1", "1", "6"],
])

L("-- 角色菜单授权（6 个角色合计 153 条）")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 1, `id` FROM `sys_menu` WHERE `id` IN (1, 9, 11, 12, 13, 91, 92, 93, 94, 95, 96) OR `id` IN (2, 3, 4, 5, 8);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` <> 9;")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 3, 4, 5, 6, 7, 8);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 2, `id` FROM `sys_menu` WHERE `parent_id` = 21;")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 3, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` IN (1, 2, 4, 6, 8, 9);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 3, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 4, 6, 8, 9);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 4, `id` FROM `sys_menu` WHERE `parent_id` IS NULL AND `id` IN (1, 2, 5);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 4, `id` FROM `sys_menu` WHERE `parent_id` IN (1, 2, 5);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES")
L("(5, 1), (5, 11), (5, 12), (5, 13), (5, 42), (5, 43), (5, 44), (5, 56), (5, 74);")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 6, `id` FROM `sys_menu` WHERE `parent_id` IS NULL;")
L("INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)")
L("SELECT 6, `id` FROM `sys_menu` WHERE `perms` IN")
L("('dashboard:index','dashboard:todo','dashboard:notice','dept:tree','dept:headcount',")
L(" 'attendance:stat','salary:calc','recruit:request','performance:analysis','training:record');")
L()

L("-- 系统参数（16 项）")
emit("sys_config", ["config_key", "config_name", "config_value", "config_group", "value_type",
                    "is_system", "remark"], [
    [q("attendance.lock.day"), q("考勤封账日"), q("5"), q("attendance"), "2", "1", q("每月第几个工作日封账，封账后考勤数据禁改")],
    [q("attendance.late.minutes"), q("迟到判定分钟数"), q("5"), q("attendance"), "2", "1", q("超过上班时间该分钟数判定为迟到")],
    [q("attendance.absent.minutes"), q("旷工判定分钟数"), q("120"), q("attendance"), "2", "1", q("迟到超过该分钟数判定为旷工")],
    [q("attendance.workday.hours"), q("标准日工时"), q("8"), q("attendance"), "2", "1", q("用于折算加班与缺勤扣款")],
    [q("overtime.rate.workday"), q("工作日加班倍率"), q("1.5"), q("attendance"), "1", "0", q("工作日加班费倍率")],
    [q("overtime.rate.weekend"), q("休息日加班倍率"), q("2.0"), q("attendance"), "1", "0", q("休息日加班费倍率")],
    [q("overtime.rate.holiday"), q("法定节假日加班倍率"), q("3.0"), q("attendance"), "1", "0", q("法定节假日加班费倍率")],
    [q("salary.tax.threshold"), q("个税起征点"), q("5000"), q("salary"), "2", "1", q("个税累计预扣法起征点(元/月)")],
    [q("salary.pay.day"), q("薪资发放日"), q("15"), q("salary"), "2", "0", q("每月发薪日")],
    [q("salary.probation.ratio"), q("试用期薪资比例"), q("0.8"), q("salary"), "1", "0", q("试用期工资占转正工资比例")],
    [q("contract.expire.warn.days"), q("合同到期预警天数"), q("30"), q("recruit"), "2", "1", q("合同到期前多少天开始预警")],
    [q("probation.warn.days"), q("转正提醒天数"), q("7"), q("recruit"), "2", "0", q("试用期到期前多少天提醒办理转正")],
    [q("recruit.offer.valid.days"), q("Offer有效期天数"), q("7"), q("recruit"), "2", "0", q("Offer发出后多少天内需候选人确认")],
    [q("performance.appeal.days"), q("绩效申诉期天数"), q("5"), q("system"), "2", "0", q("结果确认后多少天内可申诉")],
    [q("system.login.fail.limit"), q("登录失败锁定次数"), q("5"), q("system"), "2", "1", q("连续失败多少次锁定账号")],
    [q("system.login.lock.minutes"), q("账号锁定时长(分钟)"), q("30"), q("system"), "2", "1", q("账号锁定持续时间")],
])

# ====================================================================================
# 二、部门与岗位
# ====================================================================================
L("-- ####################################################################################")
L("--  二、部门管理 —— 组织架构（3 层）与岗位体系")
L("-- ####################################################################################")
L()
L("-- 部门：总公司 → 中心 → 部门/事业部，共 13 个")
emit("dept", ["id", "dept_code", "name", "parent_id", "dept_type", "dept_level", "dept_path",
             "headcount_limit", "phone", "sort_no", "status"],
     [[N(i), q(c), q(n), N(p), N(t), N(lv), q(pa), N(lim), q(ph), N(i), "1"]
      for i, c, n, p, t, lv, pa, lim, ph in DEPT])

L("-- 岗位（16 个，覆盖管理 / 技术 / 职能 / 操作四个序列）")
POS_HEADCOUNT = {1: 8, 2: 6, 3: 6, 4: 3, 5: 1, 6: 4, 7: 3, 8: 1,
                 9: 4, 10: 1, 11: 3, 12: 1, 13: 2, 14: 3, 15: 1, 16: 1}
emit("position", ["id", "position_code", "position_name", "position_seq", "job_level", "dept_id",
                  "duty", "requirement", "headcount_limit", "sort_no", "status"],
     [[N(i), q(c), q(n), N(sq), q(lv), N(d), q(duty), q(req), N(POS_HEADCOUNT[i]), N(i), "1"]
      for i, c, n, sq, lv, d, duty, req in POSITION])

# ====================================================================================
# 三、员工主数据（50 人）
# ====================================================================================
L("-- ####################################################################################")
L("--  三、员工信息 —— 50 名员工主数据（工号 E0001~E0050）")
L("-- ####################################################################################")
L()
rows = []
for e in EMP:
    (eid, eno, name, user, gender, birth, dept, pos, level, etype, hire, regular,
     salary, status, edu, school, major) = e
    b = date.fromisoformat(birth)
    age = TODAY.year - b.year - ((TODAY.month, TODAY.day) < (b.month, b.day))
    idc = "%s%s%04d" % (["1101", "1102", "1105", "1108"][eid % 4], birth.replace("-", ""), eid % 10000)
    phone = "138%08d" % (10000000 + eid)
    marital = 2 if age >= 30 else 1
    political = 3 if eid % 7 == 0 else (2 if age < 30 else 1)
    grad = date(b.year + 22, 6, 30)
    emerg = "配偶" + name[0] if marital == 2 else "母亲" + name[0]
    remark = None
    if status == 0:
        remark = "已办理离职手续，社保公积金已减员"
    elif status == 2:
        remark = "试用期员工，按转正流程跟踪"
    elif status == 3:
        remark = "待入职，Offer 已接受，等待报到"
    rows.append([
        N(eid), q(eno), q(name), q(gender), N(age), D(b), q(idc),
        q("汉族" if eid % 9 else "回族"), q(["北京市", "上海市", "河北省石家庄市", "山东省济南市",
                                          "河南省郑州市", "江苏省南京市", "浙江省杭州市",
                                          "湖北省武汉市", "四川省成都市", "陕西省西安市"][eid % 10]),
        N(marital), N(political), q(phone), q("%s@enterprise.com" % user),
        q(emerg), q("139%08d" % (20000000 + eid)),
        q(["北京市朝阳区望京街道", "北京市海淀区中关村大街", "北京市西城区金融街",
           "北京市丰台区科技园区", "北京市昌平区回龙观", "北京市大兴区亦庄开发区",
           "北京市通州区新华大街", "北京市石景山区鲁谷路"][eid % 8] + "%d号" % (eid % 99 + 1)),
        N(edu), q(school), q(major), D(grad),
        N(dept), N(pos), q(POS_NAME[pos]), q(level), N(etype),
        D(date.fromisoformat(hire)), D(date.fromisoformat(regular)) if regular else "NULL",
        D(date.fromisoformat(hire) - timedelta(days=[0, 15, 30, 45][eid % 4])),
        D(LEAVE_DATE.get(eid)), q(LEAVE_REASON.get(eid)),
        N(status), N(MANAGER.get(eid)), M(salary),
        q(["中国工商银行北京分行", "中国建设银行北京分行", "招商银行北京分行",
           "中国银行北京分行", "中国农业银行北京分行", "交通银行北京分行"][eid % 6]),
        q("6222%012d" % (300000000000 + eid)), q(remark), "1", "1",
    ])
emit("employee", ["id", "emp_no", "name", "gender", "age", "birth_date", "id_card", "nation",
                  "native_place", "marital_status", "political_status", "phone", "email",
                  "emergency_contact", "emergency_phone", "address", "education", "graduate_school",
                  "major", "graduate_date", "dept_id", "position_id", "position", "job_level",
                  "employee_type", "hire_date", "regular_date", "work_start_date", "leave_date",
                  "leave_reason", "status", "manager_id", "salary", "bank_name", "bank_account",
                  "remark", "created_by", "updated_by"], rows)

L("-- 回填部门负责人（dept.leader_id ↔ employee.dept_id 互为引用，员工建档后再补）")
for d, eid in sorted(DEPT_LEADER.items()):
    L("UPDATE `dept` SET `leader_id` = %d, `leader` = %s WHERE `id` = %d;"
      % (eid, q(EMP_BY_ID[eid][2]), d))
L()

# ====================================================================================
# 四、合同 / 异动 / 附件
# ====================================================================================
L("-- ####################################################################################")
L("--  四、员工信息 —— 劳动合同 / 异动记录 / 员工附件")
L("-- ####################################################################################")
L()
L("-- 劳动合同（一人一份；M 序列与 P7/P8 签无固定期限，其余签 3 年固定期限；离职者状态为已终止）")
rows, cid = [], 0
for e in EMP:
    (eid, eno, name, user, gender, birth, dept, pos, level, etype, hire, regular,
     salary, status, edu, school, major) = e
    cid += 1
    hd = date.fromisoformat(hire)
    no_fixed = level.startswith("M") or level in ("P7", "P8")
    ctype = 2 if no_fixed else 1
    end = None if no_fixed else date(hd.year + 3, hd.month, hd.day)
    rows.append([
        N(cid), q("HT%s%03d" % (hd.strftime("%Y"), cid)), N(eid), N(ctype),
        q("北京示例科技股份有限公司"), q(name), D(hd - timedelta(days=3)), D(hd), D(end),
        M(None if no_fixed else 3.0, 1), M(salary), N(0 if no_fixed else 3), "NULL",
        N(5 if status == 0 else 1), q("https://files.enterprise.com/contract/%s.pdf" % eno),
        q("无固定期限合同，按司龄递增年功补贴" if no_fixed else "固定期限 3 年，试用期 3 个月"),
    ])
emit("employee_contract", ["id", "contract_no", "employee_id", "contract_type", "first_party",
                          "second_party", "sign_date", "start_date", "end_date", "term_years",
                          "salary_agreed", "probation_months", "renew_from_id", "status",
                          "attachment_url", "remark"], rows)

L("-- 员工异动记录：全员入职 50 条 + 转正 + 调岗晋升 8 条 + 离职 3 条")
rows, chid = [], 0
for e in EMP:
    eid, dept, pos, salary = e[0], e[6], e[7], e[12]
    chid += 1
    hd = date.fromisoformat(e[10])
    rows.append([N(chid), N(eid), "1", "NULL", N(dept), "NULL", q(POS_NAME[pos]), "NULL", M(salary),
                 D(hd), q("新员工入职建档，签订劳动合同"), q("IN%s%03d" % (hd.strftime("%Y%m"), chid)),
                 "2", N(1036), q("https://files.enterprise.com/change/%s_in.pdf" % e[1]), "NULL"])
for e in EMP:
    eid, regular, status = e[0], e[11], e[13]
    if not regular or status == 3 or eid >= 1044:
        continue
    if eid % 5 in (1, 3, 4):
        chid += 1
        rd = date.fromisoformat(regular)
        rows.append([N(chid), N(eid), "2", "NULL", "NULL", "NULL", q(POS_NAME[e[7]]),
                     M(e[12]), M(e[12]), D(rd), q("试用期考核合格，按期转正"),
                     "NULL", "2", N(1036), "NULL", q("试用期绩效达标，同意转正")])
CHANGES = [
    (1007, 5, 5, 5, "P5", "P6", 18000, 20500, date(2026, 4, 1), "年度晋升评审通过，晋升为 P6"),
    (1010, 5, 5, 5, "P4", "P5", 15000, 17000, date(2026, 3, 1), "年度晋升评审通过，晋升为 P5"),
    (1015, 5, 5, 5, "P4", "P4", 13200, 13200, date(2026, 6, 1), "技术方向调整，转入平台组"),
    (1022, 6, 6, 6, "P5", "P5", 17500, 17500, date(2026, 5, 1), "业务线调整，转入中台前端组"),
    (1028, 6, 7, 7, "P4", "P4", 13500, 13500, date(2026, 4, 15), "组织架构调整，由前端开发部转入测试与质量部"),
    (1033, 8, 8, 8, "P4", "P4", 12800, 12800, date(2026, 6, 10), "产品线调整，转入用户增长方向"),
    (1038, 10, 10, 10, "P5", "P5", 16000, 16000, date(2026, 7, 1), "岗位职责调整，转岗负责成本核算"),
    (1042, 12, 12, 12, "P5", "P5", 16000, 16000, date(2026, 8, 1), "市场部业务调整，负责品牌线"),
]
for eid, bd, ad, pos, bl, al, bs, as_, eff, reason in CHANGES:
    chid += 1
    rows.append([N(chid), N(eid), "5" if as_ > bs else "3", N(bd), N(ad),
                 q(POS_NAME[pos]), q(POS_NAME[pos]), M(bs), M(as_), D(eff), q(reason),
                 q("BG%s%03d" % (eff.strftime("%Y%m"), chid)), "2", N(1036),
                 q("https://files.enterprise.com/change/document_%d.pdf" % chid), "NULL"])
for eid, ld in sorted(LEAVE_DATE.items()):
    chid += 1
    rows.append([N(chid), N(eid), "7", N(EMP_BY_ID[eid][6]), "NULL", q(POS_NAME[EMP_BY_ID[eid][7]]),
                 "NULL", M(EMP_BY_ID[eid][12]), "NULL", D(ld), q(LEAVE_REASON[eid]),
                 q("LZ%s%03d" % (ld.strftime("%Y%m"), chid)), "2", N(1036), "NULL",
                 q("工作交接已完成，离职证明已开具")])
emit("employee_change", ["id", "employee_id", "change_type", "before_dept_id", "after_dept_id",
                        "before_position", "after_position", "before_salary", "after_salary",
                        "effect_date", "reason", "apply_no", "approval_status", "operator_id",
                        "attachment_url", "remark"], rows)

L("-- 员工附件（身份证 / 学历证书 / 学位证书 / 入职体检报告 / 合同扫描件）")
ATTACH_TYPES = [(1, "身份证正面", "jpg"), (2, "学历证书", "pdf"), (3, "学位证书", "pdf"),
                (5, "入职体检报告", "pdf"), (6, "劳动合同扫描件", "pdf")]
rows, aid = [], 0
for e in EMP:
    eid, eno, name = e[0], e[1], e[2]
    types = ATTACH_TYPES[:2] if eid > 1035 and eid % 3 == 0 else ATTACH_TYPES
    for at, tname, ext in types:
        aid += 1
        rows.append([N(aid), N(eid), N(at), q("%s_%s.%s" % (name, tname, ext)),
                     q("https://files.enterprise.com/emp/%s/%s_%s.%s" % (eno, eno, tname, ext)),
                     q(ext), N(90000 + (eid * 37 + aid * 13) % 3000000), N(2 if eid % 2 else 1)])
emit("employee_attachment", ["id", "employee_id", "attach_type", "file_name", "file_url",
                            "file_ext", "file_size", "uploader_id"], rows)

# ====================================================================================
# 五、系统用户与授权
# ====================================================================================
L("-- ####################################################################################")
L("--  五、系统设置 —— 系统用户与角色授权")
L("-- ####################################################################################")
L()
L("-- 系统账号：1 个非员工管理员 + 50 个员工自助账号（共 51 个），密码统一 123456 的 BCrypt")
rows = [["1", q("admin"), q(BCRYPT), q("系统管理员"), "NULL", "NULL", q("13800000000"),
         q("admin@enterprise.com"), "NULL", "1", "0", "NULL",
         TS(datetime(2026, 9, 14, 8, 32, 15)), q("10.20.31.8"),
         q("系统内置超级管理员，不关联员工档案")]]
USER_IDS = {}
for e in EMP:
    eid, name, user, dept, status = e[0], e[2], e[3], e[6], e[13]
    uid = eid - 1000 + 1
    USER_IDS[eid] = uid
    if status == 0:
        ustatus, remark, last = "0", q("员工已离职，账号已停用"), TS(datetime(2026, 6, 30, 17, 45, 12))
    elif status == 3:
        ustatus, remark, last = "0", q("待入职，账号待报到后启用"), "NULL"
    else:
        ustatus, remark = "1", "NULL"
        last = TS(datetime(2026, 9, 13, 9, eid % 60, (eid * 7) % 60))
    rows.append([N(uid), q(user), q(BCRYPT), q(name), N(eid), N(dept),
                 q("138%08d" % (10000000 + eid)), q("%s@enterprise.com" % user), "NULL",
                 ustatus, "0", "NULL", last,
                 q("10.20.%d.%d" % (20 + (eid % 6), 30 + (eid * 3) % 200)) if ustatus == "1" else "NULL",
                 remark])
emit("sys_user", ["id", "username", "password", "real_name", "employee_id", "dept_id", "phone",
                  "email", "avatar", "status", "login_fail_count", "lock_until",
                  "last_login_time", "last_login_ip", "remark"], rows)

L("-- 用户角色绑定（管理员 + 50 名员工：主管授 ROLE_MANAGER、HR 授 ROLE_HR、财务授 ROLE_FINANCE）")
HR_SET = {1034, 1035, 1047}
FIN_SET = {1037, 1038, 1039}
MGR_SET = set(DEPT_LEADER.values())
EXE_SET = {1001, 1030}
ROLE_OF_EMP = {}
rows, urid = [], 0
urid += 1
rows.append([N(urid), N(1), N(1)])                  # admin（非员工账号）绑定系统管理员角色
for e in EMP:
    eid = e[0]
    if eid in MGR_SET:
        role = 3
    elif eid in HR_SET:
        role = 2
    elif eid in FIN_SET:
        role = 4
    elif eid in EXE_SET:
        role = 6
    else:
        role = 5
    ROLE_OF_EMP[eid] = role
    urid += 1
    rows.append([N(urid), N(USER_IDS[eid]), N(role)])
emit("sys_user_role", ["id", "user_id", "role_id"], rows, batch=60)

# ====================================================================================
# 六、考勤与薪资基础规则
# ====================================================================================
L("-- ####################################################################################")
L("--  六、考勤管理与薪资管理 —— 基础规则数据")
L("-- ####################################################################################")
L()
L("-- 班次（3 种）")
emit("shift", ["id", "shift_code", "shift_name", "work_start", "work_end", "rest_minutes",
               "standard_hours", "late_tolerance", "early_tolerance", "sort_no", "remark"], [
    ["1", q("STD"), q("标准班(9:00-18:00)"), q("09:00:00"), q("18:00:00"), "60", "8.0", "5", "5", "1",
     q("研发 / 职能 / 市场默认班次")],
    ["2", q("EARLY"), q("早班(8:00-17:00)"), q("08:00:00"), q("17:00:00"), "60", "8.0", "5", "5", "2",
     q("测试与质量部值班班次")],
    ["3", q("FLEX"), q("弹性班(10:00-19:00)"), q("10:00:00"), q("19:00:00"), "60", "8.0", "10", "10", "3",
     q("产品与设计岗弹性班次")],
])

L("-- 假别（8 种）")
emit("leave_type", ["id", "type_code", "type_name", "is_paid", "unit", "min_unit",
                    "max_days_per_year", "valid_months", "can_carry_over", "need_proof",
                    "sort_no", "remark"], [
    ["1", q("ANNUAL"), q("年假"), "1", "1", "0.5", "15.0", "12", "1", "0", "1", q("按司龄折算，可结转至次年 3 月")],
    ["2", q("SICK"), q("病假"), "1", "1", "0.5", "30.0", "12", "0", "1", "2", q("需提供医院证明")],
    ["3", q("PERSONAL"), q("事假"), "0", "1", "0.5", "0.0", "12", "0", "0", "3", q("不带薪，按日扣减工资")],
    ["4", q("MARRIAGE"), q("婚假"), "1", "1", "1.0", "10.0", "12", "0", "1", "4", q("需提供结婚证")],
    ["5", q("MATERNITY"), q("产假"), "1", "1", "1.0", "158.0", "24", "0", "1", "5", q("按国家规定执行，含产前检查")],
    ["6", q("PATERNITY"), q("陪产假"), "1", "1", "0.5", "15.0", "12", "0", "1", "6", q("男职工陪产假")],
    ["7", q("BEREAVE"), q("丧假"), "1", "1", "1.0", "3.0", "12", "0", "1", "7", q("直系亲属去世")],
    ["8", q("ANNUAL_ADJ"), q("调休"), "1", "2", "1.0", "0.0", "12", "0", "0", "8", q("加班换取的调休，按小时计算")],
])

L("-- 薪资标准模板（6 档）")
emit("salary_standard", ["id", "standard_code", "standard_name", "position_seq", "job_level",
                        "base_salary", "post_salary", "performance_salary", "allowance",
                        "effective_date", "status", "remark"], [
    ["1", q("STD-TECH-P4"), q("P4 技术岗标准"), "2", q("P4"), "8400.00", "2800.00", "1800.00", "1000.00", q("2026-01-01"), "1", q("初级技术岗")],
    ["2", q("STD-TECH-P6"), q("P6 技术岗标准"), "2", q("P6"), "12600.00", "4200.00", "3000.00", "1200.00", q("2026-01-01"), "1", q("资深技术岗")],
    ["3", q("STD-TECH-P8"), q("P8 技术岗标准"), "2", q("P8"), "21960.00", "7320.00", "5250.00", "1470.00", q("2026-01-01"), "1", q("技术专家岗")],
    ["4", q("STD-PROD-P6"), q("P6 产品岗标准"), "3", q("P6-P7"), "12480.00", "4160.00", "3120.00", "1240.00", q("2026-01-01"), "1", q("资深产品岗")],
    ["5", q("STD-FUNC-P5"), q("P5 职能岗标准"), "3", q("P4-P5"), "9360.00", "3120.00", "1560.00", "960.00", q("2026-01-01"), "1", q("职能支撑岗")],
    ["6", q("STD-MGMT-M2"), q("M2 管理岗标准"), "1", q("M2"), "15120.00", "5040.00", "3780.00", "2060.00", q("2026-01-01"), "1", q("中层管理岗")],
])

L("-- 社保公积金配置（北京 2026 年度 6 个险种）")
emit("insurance_config", ["id", "config_name", "city", "social_type", "company_rate",
                         "personal_rate", "base_min", "base_max", "effective_date", "status", "remark"], [
    ["1", q("2026年北京社保-养老"), q("北京"), "1", "0.1600", "0.0800", "6821.00", "35283.00", q("2026-07-01"), "1", q("企业 16% / 个人 8%")],
    ["2", q("2026年北京社保-医疗"), q("北京"), "2", "0.0980", "0.0200", "6821.00", "35283.00", q("2026-07-01"), "1", q("企业 9.8%（含生育）/ 个人 2%")],
    ["3", q("2026年北京社保-失业"), q("北京"), "3", "0.0050", "0.0050", "6821.00", "35283.00", q("2026-07-01"), "1", q("企业 0.5% / 个人 0.5%")],
    ["4", q("2026年北京社保-工伤"), q("北京"), "4", "0.0020", "0.0000", "6821.00", "35283.00", q("2026-07-01"), "1", q("企业 0.2% / 个人不缴")],
    ["5", q("2026年北京社保-生育"), q("北京"), "5", "0.0080", "0.0000", "6821.00", "35283.00", q("2026-07-01"), "1", q("企业 0.8% / 个人不缴")],
    ["6", q("2026年北京住房公积金-12%"), q("北京"), "6", "0.1200", "0.1200", "2420.00", "35283.00", q("2026-07-01"), "1", q("企业与个人各 12%")],
])

L("-- 员工薪资标准（入职定薪 + 2 条调薪历史，体现留痕）")
rows, esid = [], 0


def std_id_of(level):
    if level.startswith("M"):
        return 6
    if level in ("P7", "P8"):
        return 3
    if level == "P4":
        return 1
    if level == "P6":
        return 2
    return 5


for e in EMP:
    eid, level, hire, salary = e[0], e[8], e[10], e[12]
    esid += 1
    base, post = round(salary * 0.60, 2), round(salary * 0.20, 2)
    perf = round(salary * 0.15, 2)
    allow = r2(salary - base - post - perf)
    sbase = min(max(salary, 6821.00), 35283.00)
    rows.append([N(esid), N(eid), N(std_id_of(level)), M(base), M(post), M(perf), M(allow),
                 M(sbase), M(sbase), "12.00", D(date.fromisoformat(hire)), "1", "1",
                 q("入职定薪，套用%s" % level), N(2)])
for eid, old, eff, reason in [(1007, 18000, date(2026, 4, 1), "年度晋升调薪（P5→P6）"),
                              (1010, 15000, date(2026, 3, 1), "年度晋升调薪（P4→P5）")]:
    esid += 1
    base, post = round(old * 0.60, 2), round(old * 0.20, 2)
    perf = round(old * 0.15, 2)
    allow = r2(old - base - post - perf)
    sbase = min(max(old, 6821.00), 35283.00)
    rows.append([N(esid), N(eid), N(std_id_of(EMP_BY_ID[eid][8])), M(base), M(post), M(perf),
                 M(allow), M(sbase), M(sbase), "12.00", D(eff), "0", "1", q(reason), N(2)])
emit("employee_salary", ["id", "employee_id", "standard_id", "base_salary", "post_salary",
                        "performance_salary", "allowance", "social_base", "fund_base", "fund_rate",
                        "effective_date", "is_current", "approval_status", "adjust_reason",
                        "created_by"], rows, batch=40)

# ====================================================================================
# 七、考勤：排班 / 逐日打卡 / 假期余额 / 月度汇总
# ====================================================================================
L("-- ####################################################################################")
L("--  七、考勤管理 —— 排班 / 逐日打卡明细 / 假期余额 / 月度汇总")
L("-- ####################################################################################")
L()
SHIFT_BY_DEPT = {7: 2, 3: 3, 8: 3, 13: 3, 2: 1, 5: 1, 6: 1, 9: 1, 10: 1, 11: 1, 12: 1, 4: 1, 1: 1}
SHIFT_OF = {e[0]: SHIFT_BY_DEPT.get(e[6], 1) for e in EMP}

# 考勤/排班对象：在统计区间内有过在册记录的员工（含 7 月离职者，排除 9 月才入职者）
ATT_EMP = [e for e in EMP
           if date.fromisoformat(e[10]) <= date(2026, 8, 31)
           and (e[0] not in LEAVE_DATE or LEAVE_DATE[e[0]] >= date(2026, 7, 1))]

L("-- 排班表（2026 年 8 月后两周工作日，按员工所属部门套用对应班次）")
rows, sid = [], 0
for d in workdays(2026, 8):
    if d < date(2026, 8, 17):
        continue
    for e in ATT_EMP:
        eid = e[0]
        if d < date.fromisoformat(e[10]):
            continue
        if LEAVE_DATE.get(eid) and d > LEAVE_DATE[eid]:
            continue
        sid += 1
        rows.append([N(sid), N(eid), N(SHIFT_OF[eid]), D(d),
                     q("弹性班次按部门默认套用") if SHIFT_OF[eid] == 3 else "NULL"])
emit("employee_schedule", ["id", "employee_id", "shift_id", "schedule_date", "remark"], rows)

L("-- 逐日打卡明细：2026 年 7 月 + 8 月全部工作日 × 在岗员工，含迟到/早退/缺卡/旷工/休假等真实分布")
SHIFT_TIME = {1: (9, 18), 2: (8, 17), 3: (10, 19)}
att_rows, att_id, att_idx, att_meta = [], 0, {}, {}
att_stat = {}          # (eid, y, m) -> dict of aggregates
for (yy, mm) in ATT_MONTHS:
    for d in workdays(yy, mm):
        for e in ATT_EMP:
            eid = e[0]
            if d < date.fromisoformat(e[10]):
                continue
            if LEAVE_DATE.get(eid) and d > LEAVE_DATE[eid]:
                continue
            sh = SHIFT_OF[eid]
            wh_start, wh_end = SHIFT_TIME[sh]
            r = random.random()
            if r < 0.893:
                st, ci_s, co_s, late, early = 1, 1, 1, 0, 0
                cin = hm(d, wh_start, random.randint(-15, 5))
                cout = hm(d, wh_end, random.randint(2, 28))
                wh, remark = 8.0, None
            elif r < 0.941:
                late = random.choice([6, 8, 12, 15, 18, 22, 25, 31])
                st, ci_s, co_s, early = 2, 2, 1, 0
                cin = hm(d, wh_start, late)
                cout = hm(d, wh_end, random.randint(20, 50))
                wh, remark = r2(8.0 - late / 60.0), q("迟到 %d 分钟" % late)
            elif r < 0.962:
                early = random.choice([10, 15, 20, 25, 30])
                st, ci_s, co_s, late = 3, 1, 2, 0
                cin = hm(d, wh_start, random.randint(-10, 4))
                cout = hm(d, wh_end, -early)
                wh, remark = r2(8.0 - early / 60.0), q("早退 %d 分钟" % early)
            elif r < 0.976:
                st, ci_s, co_s, late, early = 4, 1, 3, 0, 0
                cin, cout, wh = hm(d, wh_start, 2), None, 4.0
                remark = q("下班未打卡，已提交补卡申请")
            elif r < 0.988:
                st, ci_s, co_s, late, early = 5, 1, 1, 0, 0
                cin, cout, wh = None, None, 0.0
                remark = q("未打卡且未提交申请，记旷工 1 天")
            else:
                st, ci_s, co_s, late, early = 6, 1, 1, 0, 0
                cin, cout, wh = None, None, 0.0
                remark = q("已批准的休假，考勤计为休假")
            att_id += 1
            att_rows.append([N(att_id), N(eid), D(d), N(sh), TS(cin), TS(cout), M(wh, 1),
                             N(late), N(early), N(ci_s), N(co_s), N(st),
                             N(random.choice([1, 1, 1, 2, 2, 3])),
                             q(remark) if remark else "NULL"])
            att_idx[(eid, d)] = len(att_rows) - 1
            att_meta[(eid, d)] = eid

# ------------------------------------------------------------------------------------
# 请假 / 加班 / 出差：先生成申请单，再把「已通过」的申请落到逐日打卡明细上
# ------------------------------------------------------------------------------------
LEAVE_REQS = [
    # (员工, 假别, 开始, 结束, 天数, 状态, 事由)
    (1005, 1, date(2026, 7, 6), date(2026, 7, 8), 3.0, 2, "年假休假，回老家探亲"),
    (1006, 2, date(2026, 7, 13), date(2026, 7, 14), 2.0, 2, "感冒发烧，医嘱休息两天"),
    (1007, 1, date(2026, 7, 20), date(2026, 7, 24), 5.0, 2, "年假休假，外出旅行"),
    (1008, 3, date(2026, 7, 15), date(2026, 7, 15), 1.0, 2, "家中急事，请事假一天"),
    (1009, 2, date(2026, 8, 3), date(2026, 8, 5), 3.0, 2, "腰椎间盘突出，遵医嘱休养"),
    (1010, 1, date(2026, 8, 10), date(2026, 8, 12), 3.0, 2, "年假休假"),
    (1011, 4, date(2026, 8, 17), date(2026, 8, 21), 5.0, 2, "结婚，申请婚假"),
    (1012, 3, date(2026, 8, 6), date(2026, 8, 7), 2.0, 2, "办理个人事务"),
    (1013, 1, date(2026, 8, 24), date(2026, 8, 26), 3.0, 2, "年假休假，陪伴家人"),
    (1014, 2, date(2026, 8, 11), date(2026, 8, 11), 1.0, 2, "肠胃不适，病假一天"),
    (1015, 3, date(2026, 8, 19), date(2026, 8, 19), 1.0, 3, "事由不充分，予以驳回"),
    (1016, 6, date(2026, 8, 20), date(2026, 8, 21), 2.0, 2, "配偶生产，申请陪产假"),
    (1017, 1, date(2026, 7, 27), date(2026, 7, 29), 3.0, 2, "年假休假"),
    (1020, 5, date(2026, 7, 1), date(2026, 7, 31), 23.0, 2, "产假，已按规定提交证明"),
    (1021, 3, date(2026, 7, 9), date(2026, 7, 9), 0.5, 2, "上午办理银行事务"),
    (1022, 1, date(2026, 8, 13), date(2026, 8, 14), 2.0, 2, "年假休假"),
    (1023, 2, date(2026, 7, 21), date(2026, 7, 21), 1.0, 2, "牙科手术后休息"),
    (1024, 3, date(2026, 8, 25), date(2026, 8, 25), 1.0, 2, "个人事务"),
    (1025, 1, date(2026, 8, 27), date(2026, 8, 28), 2.0, 1, "年假休假，待主管审批"),
    (1026, 1, date(2026, 7, 10), date(2026, 7, 10), 1.0, 2, "年假休假"),
    (1027, 7, date(2026, 7, 16), date(2026, 7, 17), 2.0, 2, "亲属去世，申请丧假"),
    (1028, 2, date(2026, 8, 4), date(2026, 8, 4), 1.0, 2, "身体不适"),
    (1029, 1, date(2026, 8, 18), date(2026, 8, 19), 2.0, 2, "年假休假"),
    (1031, 1, date(2026, 8, 6), date(2026, 8, 7), 2.0, 2, "年假休假"),
    (1032, 3, date(2026, 8, 12), date(2026, 8, 12), 0.5, 2, "下午办理个人事务"),
    (1033, 2, date(2026, 7, 23), date(2026, 7, 24), 2.0, 2, "病假，已提交门诊证明"),
    (1034, 1, date(2026, 8, 3), date(2026, 8, 4), 2.0, 2, "年假休假"),
    (1035, 3, date(2026, 8, 20), date(2026, 8, 20), 1.0, 2, "个人事务"),
    (1036, 1, date(2026, 7, 13), date(2026, 7, 17), 5.0, 2, "年假休假"),
    (1038, 1, date(2026, 8, 10), date(2026, 8, 11), 2.0, 2, "年假休假"),
    (1039, 3, date(2026, 8, 26), date(2026, 8, 26), 1.0, 2, "个人事务"),
    (1040, 1, date(2026, 7, 6), date(2026, 7, 7), 2.0, 2, "年假休假"),
    (1041, 3, date(2026, 8, 14), date(2026, 8, 14), 1.0, 4, "行程变更，主动撤销申请"),
    (1042, 1, date(2026, 8, 24), date(2026, 8, 28), 5.0, 2, "年假休假，外出旅行"),
    (1043, 2, date(2026, 7, 28), date(2026, 7, 28), 1.0, 2, "病假一天"),
    (1044, 3, date(2026, 8, 21), date(2026, 8, 21), 1.0, 3, "入职未满试用期，暂不批事假"),
    (1002, 1, date(2026, 8, 27), date(2026, 8, 28), 2.0, 1, "年假休假，待审批"),
    (1019, 2, date(2026, 8, 25), date(2026, 8, 26), 2.0, 1, "身体不适，待主管审批"),
]
LEAVE_APPROVER = {1: 1036, 2: 1004, 3: 1004, 4: 1004, 5: 1004}
rows, lrid = [], 0
leave_used = {}                 # (eid, type_id) -> days
for eid, lt, d1, d2, days, st, reason in LEAVE_REQS:
    lrid += 1
    boss = MANAGER.get(eid, 1001)
    rows.append([N(lrid), q("LV%s%03d" % (d1.strftime("%Y%m"), lrid)), N(eid), N(lt),
                 TS(hm(d1, 9, 0)), TS(hm(d2, 18, 0)), M(days, 1), q(reason),
                 q("https://files.enterprise.com/leave/proof_%d.pdf" % lrid) if lt in (2, 4, 5, 6, 7) else "NULL",
                 N(st), N(boss) if st in (1, 2) else "NULL",
                 TS(hm(d1 - timedelta(days=1), 17, 30))])
    if st == 2:
        leave_used[(eid, lt)] = leave_used.get((eid, lt), 0.0) + days
        # 把已通过的休假落到逐日打卡明细
        d = d1
        while d <= d2:
            k = (eid, d)
            if k in att_idx:
                i = att_idx[k]
                att_rows[i][4] = "NULL"
                att_rows[i][5] = "NULL"
                att_rows[i][6] = "0.0"
                att_rows[i][7] = "0"
                att_rows[i][8] = "0"
                att_rows[i][9] = "1"
                att_rows[i][10] = "1"
                att_rows[i][11] = "6"
                att_rows[i][13] = q("已批准休假（%s）" % {1: "年假", 2: "病假", 3: "事假", 4: "婚假",
                                                           5: "产假", 6: "陪产假", 7: "丧假", 8: "调休"}[lt])
            d += timedelta(days=1)
emit("leave_request", ["id", "request_no", "employee_id", "leave_type_id", "start_time",
                      "end_time", "days", "reason", "attachment_url", "approval_status",
                      "current_approver_id", "apply_time"], rows, batch=40)

L("-- 假期余额（年假 / 病假 / 事假，已使用天数由已批准的请假单汇总而来）")
rows, lbid = [], 0
for e in ATT_EMP:
    eid, hire = e[0], e[10]
    seniority = (TODAY - date.fromisoformat(hire)).days / 365.0
    annual_total = 5.0 if seniority < 1 else (10.0 if seniority < 3 else 15.0)
    for lt, total in ((1, annual_total), (2, 30.0), (3, 0.0)):
        lbid += 1
        used = leave_used.get((eid, lt), 0.0)
        carry = 3.0 if (lt == 1 and seniority >= 3) else 0.0
        remain = r2(total + carry - used)
        rows.append([N(lbid), N(eid), N(lt), "2026", M(total, 1), M(carry, 1), M(used, 1),
                     M(max(remain, 0.0), 1), D(date(2027, 3, 31))])
emit("leave_balance", ["id", "employee_id", "leave_type_id", "balance_year", "total_days",
                      "carry_over_days", "used_days", "remain_days", "expire_date"], rows, batch=50)

OVERTIME = [
    (1005, date(2026, 7, 2), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "版本发版值守，处理线上问题"),
    (1006, date(2026, 7, 9), "19:00:00", "21:30:00", 2.5, 1, 2, 2, "接口联调与缺陷修复"),
    (1009, date(2026, 7, 18), "10:00:00", "18:00:00", 8.0, 2, 2, 1, "季度版本上线，周末加班"),
    (1010, date(2026, 7, 25), "10:00:00", "17:00:00", 7.0, 2, 2, 1, "生产环境性能压测"),
    (1012, date(2026, 8, 1), "10:00:00", "16:00:00", 6.0, 2, 1, 1, "数据迁移脚本验证"),
    (1013, date(2026, 8, 5), "19:00:00", "22:30:00", 3.5, 1, 2, 2, "客户现场问题远程支持"),
    (1014, date(2026, 8, 8), "10:00:00", "18:00:00", 8.0, 2, 2, 1, "系统割接，周末加班"),
    (1016, date(2026, 8, 12), "19:30:00", "23:00:00", 3.5, 1, 1, 2, "紧急缺陷修复"),
    (1017, date(2026, 8, 15), "10:00:00", "18:00:00", 8.0, 2, 2, 1, "灰度发布与验证"),
    (1021, date(2026, 8, 18), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "前端兼容性问题排查"),
    (1022, date(2026, 8, 20), "19:00:00", "22:00:00", 3.0, 1, 1, 2, "活动页面赶工"),
    (1023, date(2026, 7, 11), "10:00:00", "17:00:00", 7.0, 2, 2, 1, "移动端适配联调"),
    (1026, date(2026, 7, 16), "19:00:00", "21:30:00", 2.5, 1, 2, 2, "回归测试执行"),
    (1027, date(2026, 7, 30), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "自动化用例补充"),
    (1028, date(2026, 8, 6), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "缺陷复测"),
    (1029, date(2026, 8, 22), "10:00:00", "16:00:00", 6.0, 2, 2, 1, "性能测试环境搭建"),
    (1031, date(2026, 7, 24), "19:00:00", "21:30:00", 2.5, 1, 2, 2, "需求评审材料准备"),
    (1032, date(2026, 8, 13), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "原型稿修订"),
    (1034, date(2026, 8, 7), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "校园招聘宣讲材料准备"),
    (1035, date(2026, 8, 19), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "考勤月度核对"),
    (1036, date(2026, 7, 17), "10:00:00", "15:00:00", 5.0, 2, 2, 1, "季度人力盘点"),
    (1038, date(2026, 8, 8), "10:00:00", "17:00:00", 7.0, 2, 2, 1, "月度结账加班"),
    (1039, date(2026, 8, 21), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "薪资数据复核"),
    (1040, date(2026, 8, 14), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "年会场地对接"),
    (1042, date(2026, 7, 21), "19:00:00", "21:30:00", 2.5, 1, 2, 2, "市场活动方案打磨"),
    (1043, date(2026, 8, 26), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "渠道数据整理"),
    (1008, date(2026, 8, 25), "19:00:00", "22:00:00", 3.0, 1, 2, 1, "技术分享会筹备"),
    (1011, date(2026, 7, 8), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "代码评审"),
    (1015, date(2026, 8, 4), "19:00:00", "21:30:00", 2.5, 1, 3, 2, "线上故障复盘"),
    (1018, date(2026, 8, 11), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "新人培训作业辅导"),
    (1020, date(2026, 7, 3), "10:00:00", "15:00:00", 5.0, 2, 2, 1, "前端基建方案落地"),
    (1024, date(2026, 8, 28), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "组件库文档补充"),
    (1025, date(2026, 8, 27), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "国际化适配"),
    (1030, date(2026, 7, 14), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "产品路线图评审"),
    (1033, date(2026, 7, 28), "19:00:00", "21:00:00", 2.0, 1, 4, 2, "用户访谈纪要整理"),
    (1037, date(2026, 8, 6), "19:00:00", "22:00:00", 3.0, 1, 2, 2, "预算执行分析"),
    (1041, date(2026, 8, 13), "19:00:00", "21:00:00", 2.0, 1, 2, 2, "办公用品盘点"),
    (1044, date(2026, 8, 20), "19:00:00", "21:00:00", 2.0, 1, 4, 2, "入职培训自学"),
]
rows, otid = [], 0
ot_hours = {}                   # (eid, y, m) -> hours
for eid, d, t1, t2, hours, otype, comp, st, reason in OVERTIME:
    otid += 1
    rows.append([N(otid), q("OT%s%03d" % (d.strftime("%Y%m"), otid)), N(eid), D(d), q(t1), q(t2),
                 M(hours, 1), N(otype), N(comp), q(reason), N(st),
                 N(MANAGER.get(eid, 1001)) if st in (1, 2) else "NULL",
                 TS(hm(d - timedelta(days=1), 18, 30))])
    if st == 2:
        ot_hours[(eid, d.year, d.month)] = ot_hours.get((eid, d.year, d.month), 0.0) + hours
emit("overtime_request", ["id", "request_no", "employee_id", "overtime_date", "start_time",
                         "end_time", "hours", "overtime_type", "compensate_type", "reason",
                         "approval_status", "current_approver_id", "apply_time"], rows, batch=40)

TRIPS = [
    (1003, "上海市", "参加 2026 云原生技术峰会并拜访合作方", date(2026, 7, 8), date(2026, 7, 10), 3.0, 2, 2, 6800),
    (1031, "深圳市", "客户现场需求调研与方案汇报", date(2026, 7, 14), date(2026, 7, 17), 4.0, 2, 2, 9200),
    (1042, "杭州市", "参加行业展会并洽谈渠道合作", date(2026, 7, 22), date(2026, 7, 24), 3.0, 2, 2, 5600),
    (1038, "天津市", "分公司财务账务现场核查", date(2026, 8, 5), date(2026, 8, 6), 2.0, 2, 2, 2200),
    (1004, "成都市", "研发中心选址考察与团队沟通", date(2026, 8, 11), date(2026, 8, 14), 4.0, 2, 2, 8400),
    (1005, "南京市", "客户现场问题排查与技术支持", date(2026, 8, 18), date(2026, 8, 19), 2.0, 2, 2, 3100),
    (1036, "武汉市", "校园招聘宣讲与院校合作洽谈", date(2026, 8, 25), date(2026, 8, 27), 3.0, 2, 2, 4800),
    (1027, "广州市", "产品交付现场支持", date(2026, 7, 6), date(2026, 7, 7), 2.0, 2, 2, 3900),
    (1040, "济南市", "办公场地租赁与供应商比选", date(2026, 8, 12), date(2026, 8, 13), 2.0, 2, 2, 2100),
    (1034, "西安市", "高校宣讲会组织", date(2026, 7, 20), date(2026, 7, 21), 2.0, 2, 2, 3500),
    (1043, "长沙市", "区域渠道商拜访", date(2026, 8, 20), date(2026, 8, 21), 2.0, 2, 1, 2800),
    (1006, "杭州市", "技术交流与开源社区活动", date(2026, 8, 28), date(2026, 8, 29), 2.0, 2, 1, 3200),
    (1032, "苏州市", "用户调研与可用性测试", date(2026, 8, 6), date(2026, 8, 7), 2.0, 2, 2, 2600),
    (1039, "郑州市", "税务事项现场办理", date(2026, 7, 27), date(2026, 7, 27), 1.0, 2, 2, 1200),
    (1033, "青岛市", "设计展会参观学习", date(2026, 8, 13), date(2026, 8, 14), 2.0, 2, 4, 2400),
]
rows, tid = [], 0
trip_days = {}
for eid, dest, purpose, d1, d2, days, transport, st, budget in TRIPS:
    tid += 1
    rows.append([N(tid), q("BT%s%03d" % (d1.strftime("%Y%m"), tid)), N(eid), q(dest), q(purpose),
                 TS(hm(d1, 9, 0)), TS(hm(d2, 18, 0)), M(days, 1), N(transport), M(budget),
                 N(st), N(MANAGER.get(eid, 1001)) if st in (1, 2) else "NULL",
                 TS(hm(d1 - timedelta(days=2), 16, 0))])
    if st == 2:
        d = d1
        while d <= d2:
            k = (eid, d)
            if k in att_idx:
                i = att_idx[k]
                att_rows[i][4] = "NULL"
                att_rows[i][5] = "NULL"
                att_rows[i][6] = "0.0"
                att_rows[i][7] = "0"
                att_rows[i][8] = "0"
                att_rows[i][9] = "1"
                att_rows[i][10] = "1"
                att_rows[i][11] = "7"
                att_rows[i][13] = q("出差（%s）" % dest)
            trip_days[(eid, d.year, d.month)] = trip_days.get((eid, d.year, d.month), 0.0) + 1.0
            d += timedelta(days=1)
emit("business_trip", ["id", "request_no", "employee_id", "destination", "purpose", "start_time",
                      "end_time", "days", "transport", "budget_amount", "approval_status",
                      "current_approver_id", "apply_time"], rows, batch=30)

L("-- 逐日打卡明细（含已批准的休假 / 出差回填，与请假单、出差单口径一致）")
emit("attendance_record", ["id", "employee_id", "attendance_date", "shift_id", "first_check_in",
                          "last_check_out", "work_hours", "late_minutes", "early_minutes",
                          "check_in_status", "check_out_status", "attendance_status", "source",
                          "remark"], att_rows, batch=50)

L("-- 月度考勤汇总（由逐日打卡明细真实聚合；7 月已封账、8 月未封账）")
rows, amid = [], 0
agg = {}
for r in att_rows:
    eid = int(r[1])
    d = date.fromisoformat(r[2].strip("'"))
    key = (eid, d.year, d.month)
    if key not in agg:
        agg[key] = {"should": 0.0, "actual": 0.0, "late": 0, "early": 0, "absent": 0,
                    "missing": 0, "leave": 0.0, "work": 0.0}
    a = agg[key]
    a["should"] += 1.0
    st = r[11]
    if st in ("1", "2"):
        a["actual"] += 1.0
    elif st == "3":
        a["actual"] += 1.0
    elif st == "4":
        a["actual"] += 0.5
    if st == "2":
        a["late"] += 1
    elif st == "3":
        a["early"] += 1
    elif st == "4":
        a["missing"] += 1
    elif st == "5":
        a["absent"] += 1
    elif st == "6":
        a["leave"] += 1.0
    elif st == "7":
        a["actual"] += 1.0
    a["work"] += float(r[6])
for (eid, yy, mm), a in sorted(agg.items()):
    amid += 1
    locked = (yy, mm) == (2026, 7)
    rows.append([N(amid), N(eid), N(EMP_BY_ID[eid][6]), N(yy), N(mm),
                 M(a["should"], 1), M(a["actual"], 1), N(a["late"]), N(a["early"]), N(a["absent"]),
                 N(a["missing"]), M(a["leave"], 1),
                 M(ot_hours.get((eid, yy, mm), 0.0), 1), M(trip_days.get((eid, yy, mm), 0.0), 1),
                 "1" if locked else "0", TS(datetime(2026, 8, 5, 10, 30, 0)) if locked else "NULL",
                 N(2) if locked else "NULL",
                 q("2026 年 %d 月考勤已封账，数据锁定" % mm) if locked
                 else q("2026 年 %d 月考勤未封账，可继续修正" % mm)])
emit("attendance_month", ["id", "employee_id", "dept_id", "record_year", "record_month",
                         "should_days", "actual_days", "late_count", "early_count", "absent_count",
                         "missing_count", "leave_days", "overtime_hours", "trip_days",
                         "lock_status", "lock_time", "lock_by", "remark"], rows, batch=50)

# ====================================================================================
# 八、薪资管理：月度薪资核算（含薪资条逐项明细）+ 社保公积金缴纳
# ====================================================================================
L("-- ####################################################################################")
L("--  八、薪资管理 —— 月度薪资核算（含薪资条明细）/ 社保公积金缴纳")
L("-- ####################################################################################")
L()


def user_id_of(eid):
    """员工自助账号的用户ID：admin 占 1，员工 1001~1050 依次为 2~51。"""
    return 2 + (eid - 1001)


PERF_FACTOR = {"S": 1.20, "A": 1.10, "B": 1.00, "C": 0.85, "D": 0.70}


def perf_grade_of(eid):
    """稳定分档：S≈8% / A≈27% / B≈45% / C≈15% / D≈5%，与十一、绩效模块共用同一口径。"""
    x = (eid * 37 + 11) % 100
    if x < 8:
        return "S"
    if x < 35:
        return "A"
    if x < 80:
        return "B"
    if x < 95:
        return "C"
    return "D"


def iit(taxable):
    """个人所得税（月度综合所得预扣，含速算扣除数）。"""
    if taxable <= 0:
        return 0.0
    for cap, rate, quick in ((3000, 0.03, 0), (12000, 0.10, 210), (25000, 0.20, 1410),
                             (35000, 0.25, 2660), (55000, 0.30, 4410), (80000, 0.35, 7160)):
        if taxable <= cap:
            return r2(taxable * rate - quick)
    return r2(taxable * 0.45 - 15160)


def on_payroll(eid, y, m):
    """当月只要有一天在册即需核算薪资（新入职按天折算、离职当月只算到离职日）。"""
    e = EMP_BY_ID[eid]
    d0 = date.fromisoformat(e[10])
    last = date(y, m + 1, 1) - timedelta(days=1) if m < 12 else date(y, 12, 31)
    d1 = LEAVE_DATE.get(eid)
    if d0 > last:
        return False
    if d1 and d1 < date(y, m, 1):
        return False
    return True


# 社保 / 公积金费率（与 insurance_config 保持一致）
SOCIAL_RATE_CO = {"pension": 0.1600, "medical": 0.0980, "unemploy": 0.0050,
                  "injury": 0.0020, "maternity": 0.0080}
SOCIAL_RATE_PE = {"pension": 0.0800, "medical": 0.0200, "unemploy": 0.0050}
FUND_RATE = 0.1200

# 加班费只对「补偿方式=计薪」的已通过加班单计算（补偿方式=调休的不计薪）
ot_pay_hours = {}
for eid, d, t1, t2, hours, otype, comp, st, reason in OVERTIME:
    if st == 2 and comp == 2:
        k = (eid, d.year, d.month)
        ot_pay_hours[k] = ot_pay_hours.get(k, 0.0) + hours

SALARY_RECORDS = []             # [(record_id, eid, y, m, ...)] 供 salary_item / insurance_record 引用
sal_id = 0
rows = []
for (yy, mm) in PAY_MONTHS:
    month_days = len(workdays(yy, mm))
    for e in EMP:
        eid = e[0]
        if not on_payroll(eid, yy, mm):
            continue
        sal_id += 1
        dept_id, salary = e[6], float(e[12])
        base, post = r2(salary * 0.60), r2(salary * 0.20)
        perf_base = r2(salary * 0.15)
        allow = r2(salary - base - post - perf_base)

        # 按当月在册出勤天数折算（新人入职当月 / 离职当月）
        should = agg.get((eid, yy, mm), {}).get("should", 0.0)
        ratio = min(should / float(month_days), 1.0) if should else 1.0
        base, post = r2(base * ratio), r2(post * ratio)
        allow = r2(allow * ratio)

        grade = perf_grade_of(eid)
        perf = r2(perf_base * ratio * PERF_FACTOR[grade])

        day_wage = salary / 21.75
        hour_wage = day_wage / 8.0
        ot_h = ot_pay_hours.get((eid, yy, mm), 0.0)
        ot_pay = r2(ot_h * hour_wage * 1.5)

        a = agg.get((eid, yy, mm), {})
        att_deduct = r2(a.get("late", 0) * 20.0 + a.get("early", 0) * 20.0
                        + a.get("missing", 0) * 15.0 + a.get("absent", 0) * day_wage)

        other_add = 500.00 if eid % 11 == 3 else 0.0
        add_rm = "补发上月调薪差额" if other_add else None
        other_deduct = 120.00 if eid % 13 == 4 else 0.0
        ded_rm = "工会会费与宿舍水电代扣" if other_deduct else None
        if yy == 2026 and mm == 8 and eid == 1019:
            other_add = 1500.00
            add_rm = "试用期转正一次性奖励"

        gross = r2(base + post + perf + allow + ot_pay + other_add)

        social_base = min(max(salary, 6821.00), 35283.00)
        fund_base = social_base
        social_pe = r2(social_base * (SOCIAL_RATE_PE["pension"] + SOCIAL_RATE_PE["medical"]
                                      + SOCIAL_RATE_PE["unemploy"]))
        social_co = r2(social_base * (SOCIAL_RATE_CO["pension"] + SOCIAL_RATE_CO["medical"]
                                      + SOCIAL_RATE_CO["unemploy"] + SOCIAL_RATE_CO["injury"]
                                      + SOCIAL_RATE_CO["maternity"]))
        fund_pe = r2(fund_base * FUND_RATE)
        fund_co = r2(fund_base * FUND_RATE)

        tax = iit(gross - social_pe - fund_pe - 5000.00)
        net = r2(gross - social_pe - fund_pe - tax - att_deduct - other_deduct)

        if mm == 7:
            pay_status, pay_date, pay_batch, lock = 3, date(2026, 8, 10), "PAY202607", 1
        elif eid in (1012, 1029):
            # 银行卡信息有误导致发放失败，待重新发放
            pay_status, pay_date, pay_batch, lock = 4, None, "PAY202608", 0
        else:
            pay_status, pay_date, pay_batch, lock = 3, date(2026, 9, 10), "PAY202608", 1

        rows.append([N(sal_id), N(eid), N(dept_id), N(yy), N(mm),
                     M(base), M(post), M(perf), M(allow), M(ot_pay), M(other_add),
                     M(att_deduct), M(other_deduct), M(gross),
                     M(social_pe), M(social_co), M(fund_pe), M(fund_co), M(tax), M(net),
                     N(pay_status), D(pay_date), q(pay_batch), N(lock),
                     N(user_id_of(1036)) if pay_status != 0 else "NULL",
                     q("%d 年 %d 月薪资，绩效等级 %s，出勤折算系数 %.2f" % (yy, mm, grade, ratio))])
        SALARY_RECORDS.append({
            "id": sal_id, "eid": eid, "y": yy, "m": mm, "grade": grade, "lock": lock,
            "base": base, "post": post, "perf": perf, "allow": allow, "ot": ot_pay,
            "other_add": other_add, "add_rm": add_rm, "att_deduct": att_deduct,
            "other_deduct": other_deduct, "ded_rm": ded_rm, "gross": gross,
            "social_pe": social_pe, "social_co": social_co, "fund_pe": fund_pe,
            "fund_co": fund_co, "tax": tax, "net": net, "pay_status": pay_status,
            "social_base": social_base, "fund_base": fund_base,
        })
emit("salary_record", ["id", "employee_id", "dept_id", "salary_year", "salary_month",
                       "base_salary", "post_salary", "performance_salary", "allowance",
                       "overtime_pay", "other_add", "attendance_deduct", "other_deduct",
                       "gross_pay", "social_personal", "social_company", "fund_personal",
                       "fund_company", "tax_amount", "net_pay", "pay_status", "pay_date",
                       "pay_batch_no", "lock_status", "audit_by", "remark"], rows, batch=40)

L("-- 薪资条逐项明细：应发项 / 扣款项 / 企业承担项，薪资条与薪资记录合计口径一致")
ITEM_NAMES = {"BASE": "基本工资", "POST": "岗位工资", "PERF": "绩效工资", "ALLOW": "固定补贴",
              "OT": "加班费", "OTHER_ADD": "其他加项", "ATT_DED": "考勤扣款",
              "OTHER_DED": "其他扣款", "SOCIAL": "社保个人缴纳", "FUND": "公积金个人缴纳",
              "TAX": "个人所得税", "SOCIAL_CO": "社保企业缴纳", "FUND_CO": "公积金企业缴纳"}
rows, item_id = [], 0
for s in SALARY_RECORDS:
    detail = [
        (1, "BASE", s["base"], "月薪的 60% 固定部分"),
        (1, "POST", s["post"], "按职级对应的岗位工资"),
        (1, "PERF", s["perf"], "绩效工资基数 × 绩效等级系数（%s）" % s["grade"]),
        (1, "ALLOW", s["allow"], "交通 / 餐补 / 通讯等固定补贴"),
        (1, "OT", s["ot"], "已通过且补偿方式为计薪的加班时长 × 1.5 倍时薪"),
        (1, "OTHER_ADD", s["other_add"], s["add_rm"] or "无"),
        (2, "ATT_DED", s["att_deduct"], "迟到 / 早退 20 元每次，缺卡 15 元每次，旷工按日薪扣减"),
        (2, "OTHER_DED", s["other_deduct"], s["ded_rm"] or "无"),
        (2, "SOCIAL", s["social_pe"], "养老 8%% + 医疗 2%% + 失业 0.5%%，缴费基数 %.2f" % s["social_base"]),
        (2, "FUND", s["fund_pe"], "住房公积金 12%%，缴存基数 %.2f" % s["fund_base"]),
        (2, "TAX", s["tax"], "月度综合所得预扣，起征点 5000 元"),
        (3, "SOCIAL_CO", s["social_co"], "企业承担：养老 16% + 医疗 9.8% + 失业 0.5% + 工伤 0.2% + 生育 0.8%"),
        (3, "FUND_CO", s["fund_co"], "企业承担住房公积金 12%，与个人同比例"),
    ]
    seq = 0
    for t, code, amount, remark in detail:
        if amount <= 0:
            continue
        seq += 1
        item_id += 1
        rows.append([N(item_id), N(s["id"]), N(t), q(code), q(ITEM_NAMES[code]),
                     M(amount), N(seq * 10), q(remark)])
emit("salary_item", ["id", "salary_record_id", "item_type", "item_code", "item_name",
                     "item_amount", "sort_no", "remark"], rows, batch=100)

L("-- 社保公积金缴纳记录：逐人逐月，明细项与薪资记录中的社保 / 公积金口径一致")
rows, ins_id = [], 0
for s in SALARY_RECORDS:
    eid, yy, mm = s["eid"], s["y"], s["m"]
    ins_id += 1
    sb, fb = s["social_base"], s["fund_base"]
    pen_pe, pen_co = r2(sb * SOCIAL_RATE_PE["pension"]), r2(sb * SOCIAL_RATE_CO["pension"])
    med_pe, med_co = r2(sb * SOCIAL_RATE_PE["medical"]), r2(sb * SOCIAL_RATE_CO["medical"])
    une_pe, une_co = r2(sb * SOCIAL_RATE_PE["unemploy"]), r2(sb * SOCIAL_RATE_CO["unemploy"])
    inj_co = r2(sb * SOCIAL_RATE_CO["injury"])
    mat_co = r2(sb * SOCIAL_RATE_CO["maternity"])
    fun_pe, fun_co = r2(fb * FUND_RATE), r2(fb * FUND_RATE)
    rows.append([N(ins_id), N(eid), N(1), N(yy), N(mm), M(sb), M(fb),
                 M(pen_pe), M(pen_co), M(med_pe), M(med_co), M(une_pe), M(une_co),
                 M(inj_co), M(mat_co), M(fun_pe), M(fun_co),
                 M(s["social_pe"] + fun_pe), M(s["social_co"] + fun_co),
                 N(0 if s["pay_status"] == 4 else 1),
                 q("缴费基数 %s（%d 年 %d 月），%s" % (
                     "按实际月薪" if sb == float(EMP_BY_ID[eid][12]) else "已按社平上下限调整",
                     yy, mm, "银行卡异常暂停缴纳" if s["pay_status"] == 4 else "正常缴纳"))])
emit("insurance_record", ["id", "employee_id", "config_id", "record_year", "record_month",
                          "social_base", "fund_base", "pension_personal", "pension_company",
                          "medical_personal", "medical_company", "unemploy_personal",
                          "unemploy_company", "injury_company", "maternity_company",
                          "fund_personal", "fund_company", "total_personal", "total_company",
                          "pay_status", "remark"], rows, batch=50)

# ====================================================================================
# 九、招聘管理：需求提报 → 职位发布 → 候选人筛选 → 面试 → 录用通知
# ====================================================================================
L("-- ####################################################################################")
L("--  九、招聘管理 —— 需求提报 / 职位发布 / 候选人 / 面试 / 录用通知（全链路）")
L("-- ####################################################################################")
L()

# (id, 需求单号, 需求部门, 岗位, 人数, 已录用, 职级, 薪资下限, 薪资上限, 学历要求,
#  经验年限, 期望到岗, 紧急程度, 提报人, 提报时间, 审批状态, 状态)
REQUESTS = [
    (1, "RC20260701001", 5, 1, 3, 1, "P4-P6", 14000, 22000, 3, 3, date(2026, 7, 15), 2, 1004, date(2026, 7, 1), 2, 1),
    (2, "RC20260705002", 6, 2, 2, 1, "P4-P6", 13500, 21000, 3, 3, date(2026, 7, 20), 2, 1020, date(2026, 7, 5), 2, 1),
    (3, "RC20260710003", 7, 3, 1, 1, "P4-P5", 12000, 18000, 3, 2, date(2026, 8, 1), 1, 1026, date(2026, 7, 10), 2, 2),
    (4, "RC20260801004", 9, 9, 1, 0, "P3-P4", 9500, 13000, 3, 1, date(2026, 9, 1), 3, 1036, date(2026, 8, 1), 2, 1),
    (5, "RC20260805005", 8, 6, 1, 0, "P6-P7", 24000, 32000, 3, 5, date(2026, 9, 15), 2, 1031, date(2026, 8, 5), 2, 1),
    (6, "RC20260810006", 12, 14, 1, 0, "P4-P5", 13000, 18000, 3, 3, date(2026, 9, 20), 1, 1042, date(2026, 8, 10), 1, 1),
    (7, "RC20260815007", 10, 11, 1, 0, "P4-P5", 13000, 17000, 3, 3, date(2026, 10, 1), 1, 1037, date(2026, 8, 15), 1, 1),
    (8, "RC20260820008", 2, 4, 1, 0, "P7-P8", 32000, 45000, 3, 8, date(2026, 10, 15), 1, 1003, date(2026, 8, 20), 0, 1),
]
REQ_JOB_DESC = {
    1: "1、负责集团人事管理系统后端服务的设计与开发；\n2、参与需求评审、技术方案评审与代码评审；\n3、负责线上问题定位与性能优化。",
    2: "1、负责管理系统前端页面开发与组件库建设；\n2、与设计、产品协作还原交互稿；\n3、持续优化页面性能与用户体验。",
    3: "1、负责功能测试、接口自动化测试用例设计与执行；\n2、参与版本发布质量把控与缺陷跟踪；\n3、参与测试工具与平台建设。",
    4: "1、负责招聘渠道运营与候选人初步筛选；\n2、办理入离职手续与员工档案管理；\n3、协助考勤统计与员工活动组织。",
    5: "1、负责 B 端人事产品规划与需求调研；\n2、主导版本迭代管理与上线效果复盘；\n3、输出产品方案与原型文档。",
    6: "1、负责市场推广活动策划与执行；\n2、负责渠道运营与品牌内容产出；\n3、跟踪投放效果并输出数据分析。",
    7: "1、负责日常账务处理与凭证编制；\n2、负责薪资核算与个税申报；\n3、参与月度结账与财务报表编制。",
    8: "1、负责集团级技术架构设计与技术选型；\n2、主导核心系统重构与高并发难点攻关；\n3、制定研发规范并推动落地。",
}
REQ_JOB_REQ = {
    1: "本科及以上，计算机相关专业，3 年以上 Java 后端经验，熟悉 Spring Boot / MySQL / Redis，有中大型系统经验优先。",
    2: "本科及以上，计算机相关专业，3 年以上前端经验，熟练掌握 Vue3 / TypeScript / 工程化工具链。",
    3: "本科及以上，计算机相关专业，2 年以上测试经验，熟悉接口自动化与性能测试工具。",
    4: "本科及以上，人力资源相关专业，1 年以上招聘或员工关系经验，沟通表达能力强。",
    5: "本科及以上，5 年以上 B 端产品经验，具备完整产品线规划经验，有 HR SaaS 背景优先。",
    6: "本科及以上，市场营销相关专业，3 年以上市场推广经验，具备活动策划与渠道资源。",
    7: "本科及以上，会计学 / 财务管理相关专业，3 年以上核算经验，持中级会计师职称优先。",
    8: "本科及以上，8 年以上研发经验，精通分布式架构与高并发设计，具备 10 人以上技术团队管理经验。",
}
rows = []
for (rid, rno, dept, pos, cnt, hired, level, smin, smax, edu, exp,
     hope, urg, applicant, apply_t, appr, st) in REQUESTS:
    rows.append([N(rid), q(rno), N(dept), N(pos), q(POS_NAME[pos]), N(cnt), N(hired), q(level),
                 M(smin), M(smax), N(edu), N(exp), q(REQ_JOB_DESC[rid]), q(REQ_JOB_REQ[rid]),
                 D(hope), N(urg), N(applicant), TS(hm(apply_t, 9, 30)), N(appr),
                 N(1001) if appr == 1 else "NULL", N(st)])
emit("recruit_request", ["id", "request_no", "request_dept_id", "position_id", "position_name",
                         "recruit_count", "hired_count", "job_level", "salary_min", "salary_max",
                         "education_req", "experience_years", "job_description", "job_requirement",
                         "hope_arrive_date", "urgency", "applicant_id", "apply_time",
                         "approval_status", "current_approver_id", "status"], rows, batch=20)

L("-- 职位发布：每个需求发布到 1~2 个渠道")
CHANNELS = {1: [2, 4], 2: [2, 4], 3: [1, 2], 4: [2, 3], 5: [5, 2], 6: [2, 1], 7: [2, 3], 8: [5]}
rows, pid = [], 0
for (rid, rno, dept, pos, cnt, hired, level, smin, smax, edu, exp,
     hope, urg, applicant, apply_t, appr, st) in REQUESTS:
    if st == 0:
        continue                                   # 草稿需求未发布
    for ch in CHANNELS[rid]:
        pid += 1
        pub = apply_t + timedelta(days=2)
        closed = date(2026, 8, 31) if rid == 3 else None
        rows.append([N(pid), N(rid), N(ch), q("%s（%s）" % (POS_NAME[pos], level)),
                     q("https://jobs.enterprise.com/%s/%d" % (rno.lower(), ch)),
                     TS(hm(pub, 10, 0)), TS(hm(closed, 18, 0)) if closed else "NULL",
                     N(random.randint(320, 2600)), N(random.randint(6, 58)),
                     N(2 if closed else 1)])
emit("job_posting", ["id", "recruit_request_id", "channel", "posting_title", "posting_url",
                     "publish_time", "close_time", "view_count", "apply_count", "status"],
     rows, batch=20)

L("-- 候选人：覆盖线上投递 / 内部推荐 / 猎头 / 校园招聘等来源，含人才库沉淀")
# (需求ID, 岗位关键字, 姓名, 性别, 学历, 院校, 专业, 工作年限, 当前公司, 当前职位, 期望薪资,
#  来源, 内推人, 阶段, 是否人才库, 标签, 备注)
CAND_FIXED = [
    (1, "后端", "刘丹", "F", 3, "北京邮电大学", "计算机科学与技术", 3, "北京某科技有限公司", "Java开发工程师", 13500, 3, 1005, 5, 0, None, None),
    (2, "前端", "王磊", "M", 3, "北京信息科技大学", "软件工程", 3, "北京某科技有限公司", "前端开发工程师", 11500, 3, 1021, 5, 0, None, None),
    (3, "测试", "李静", "F", 3, "北京联合大学", "软件工程", 2, "深圳某互联网公司", "测试工程师", 10800, 2, None, 5, 0, None, None),
    (4, "HR", "张敏", "F", 3, "首都经济贸易大学", "人力资源管理", 2, "北京某咨询服务公司", "招聘专员", 10000, 6, None, 5, 0, None, "校园招聘转社招"),
    (1, "后端", "郑博文", "M", 2, "北京邮电大学", "计算机科学与技术", 5, "杭州某软件公司", "高级Java工程师", 21000, 2, None, 6, 1, "Java,后端,5年经验,高并发", "技术能力突出但拒绝了 Offer，已纳入人才库长期跟进"),
    (5, "产品", "孙婉清", "F", 2, "北京大学", "工商管理", 6, "北京某科技有限公司", "高级产品经理", 30000, 5, None, 3, 1, "B端产品,HR SaaS", None),
    (8, "架构", "吴承恩", "M", 2, "上海交通大学", "计算机科学与技术", 9, "上海某网络科技公司", "技术专家", 42000, 5, None, 3, 1, "架构,分布式,团队管理", "猎头推荐，薪资预期偏高"),
    (5, "产品", "钱佳宁", "F", 3, "中国农业大学", "信息管理与信息系统", 4, "北京某信息技术公司", "产品经理", 26000, 2, None, 2, 0, None, None),
    (6, "市场", "周嘉豪", "M", 3, "郑州大学", "市场营销", 3, "广州某信息技术公司", "市场主管", 15000, 6, 1042, 2, 0, None, None),
    (7, "财务", "陈雅静", "F", 3, "中央财经大学", "会计学", 4, "北京某咨询服务公司", "会计", 15500, 2, None, 2, 0, None, None),
    (1, "后端", "杨俊杰", "M", 3, "合肥工业大学", "软件工程", 4, "成都某数据科技公司", "后端开发工程师", 18000, 2, None, 3, 0, None, None),
    (2, "前端", "许芷若", "F", 3, "北京理工大学", "数字媒体技术", 3, "武汉某云计算公司", "前端开发工程师", 16000, 2, None, 3, 0, None, None),
    (3, "测试", "何一鸣", "M", 4, "北京信息科技大学", "软件工程", 2, "北京某科技有限公司", "测试工程师", 11000, 1, None, 6, 0, None, "自动化测试经验不足，暂不匹配"),
    (1, "后端", "吕志远", "M", 4, "大连理工大学", "计算机科学与技术", 2, "北京某科技有限公司", "Java开发工程师", 15000, 1, None, 6, 1, "Java,后端,潜力人选", "技术深度不足，纳入人才库后续跟进"),
    (1, "后端", "施逸辰", "M", 7, "哈尔滨工程大学", "软件工程", 1, "北京某信息技术公司", "初级开发工程师", 12000, 2, None, 6, 0, None, "经验与岗位要求差距较大"),
    (2, "前端", "张语嫣", "F", 3, "南京大学", "软件工程", 4, "南京某软件公司", "前端工程师", 17500, 2, None, 1, 0, None, None),
    (2, "前端", "李佳琪", "F", 3, "武汉大学", "数字媒体技术", 3, "武汉某云计算公司", "前端开发工程师", 16000, 3, 1022, 1, 0, None, None),
    (5, "产品", "王思妍", "F", 3, "北京邮电大学", "信息管理与信息系统", 4, "北京某科技有限公司", "产品经理", 27000, 3, 1031, 1, 0, None, None),
    (6, "市场", "赵欣怡", "F", 3, "中国传媒大学", "市场营销", 3, "北京某文化传播公司", "市场专员", 14000, 2, None, 6, 0, None, "期望薪资超出预算区间"),
    (7, "财务", "钱梦琪", "F", 3, "对外经济贸易大学", "财务管理", 3, "北京某科技有限公司", "财务专员", 14500, 2, None, 6, 1, "财务,中级职称在读", "综合排名第二，纳入人才库"),
    (4, "HR", "孙露露", "F", 3, "北京师范大学", "心理学", 2, "北京某教育科技公司", "HR专员", 10500, 2, None, 6, 0, None, "沟通能力好但招聘经验偏弱"),
    (3, "测试", "周泽宇", "M", 3, "西安电子科技大学", "软件工程", 4, "西安某软件公司", "测试开发工程师", 17000, 2, None, 1, 0, None, None),
    (8, "架构", "郑睿哲", "M", 2, "清华大学", "计算机科学与技术", 11, "北京某互联网公司", "首席架构师", 45000, 4, None, 1, 1, "架构,高并发,技术管理", "猎头定向寻访"),
    (1, "后端", "陈文康", "M", 3, "北京交通大学", "计算机科学与技术", 4, "北京某信息技术公司", "后端开发工程师", 19000, 1, None, 6, 0, None, "面试表现一般，暂不推进"),
    (2, "前端", "杨悦然", "F", 3, "北京信息科技大学", "计算机科学与技术", 2, "北京某科技有限公司", "前端开发工程师", 13000, 1, None, 6, 0, None, "简历与岗位匹配度不足"),
    (5, "产品", "吴诗涵", "F", 3, "北京理工大学", "信息管理与信息系统", 3, "北京某软件公司", "产品经理", 24000, 2, None, 1, 0, None, None),
    (6, "市场", "赵子轩", "M", 3, "首都经济贸易大学", "市场营销", 4, "北京某广告公司", "市场策划", 16000, 3, 1043, 1, 0, None, None),
    (7, "财务", "许雅琳", "F", 3, "北京物资学院", "会计学", 3, "北京某贸易公司", "会计", 14000, 1, None, 1, 0, None, None),
]
rows, cid = [], 0
for (rid, kw, name, g, edu, school, major, wy, comp, cur_pos, expect, src, referrer,
     stage, talent, tags, remark) in CAND_FIXED:
    cid += 1
    birth = date(1992 + (cid * 3) % 9, (cid * 5) % 12 + 1, (cid * 7) % 27 + 1)
    rows.append([N(cid), N(rid), q(name), q(g), D(birth),
                 q("139%08d" % (10000000 + cid * 137 % 89999999)),
                 q("%s%02d@example.com" % (("cand%02d" % cid), (cid * 7) % 90 + 10)),
                 N(edu), q(school), q(major), N(wy), q(comp), q(cur_pos), M(expect),
                 q("https://files.enterprise.com/resume/c_%04d.pdf" % cid),
                 N(src), N(referrer) if referrer else "NULL", N(stage), N(talent),
                 q(tags) if tags else "NULL", q(remark) if remark else "NULL"])
emit("candidate", ["id", "recruit_request_id", "name", "gender", "birth_date", "phone", "email",
                   "education", "graduate_school", "major", "work_years", "current_company",
                   "current_position", "expect_salary", "resume_url", "source", "referrer_id",
                   "stage", "is_talent", "talent_tags", "remark"], rows, batch=30)

L("-- 面试记录：候选人按轮次推进，含面试官评分与结论")
CAND_INFO = []
for i, c in enumerate(CAND_FIXED, start=1):
    CAND_INFO.append((i, c[0], c[2], c[13]))      # (候选人ID, 需求ID, 姓名, 阶段)
INTERVIEWERS = {1: [1004, 1003, 1005], 2: [1020, 1003, 1021], 3: [1026, 1003, 1027],
                4: [1036, 1034, 1001], 5: [1031, 1030, 1001], 6: [1042, 1001, 1043],
                7: [1037, 1001, 1038], 8: [1003, 1004, 1001]}
EVAL_PASS = ["技术基础扎实，项目经验与岗位匹配，沟通表达清晰，建议推进下一轮。",
             "对系统设计有独立思考，问题定位思路清晰，团队协作意识良好。",
             "业务理解到位，学习能力强，职业规划与岗位发展方向一致。"]
EVAL_HOLD = ["整体表现尚可，但对核心技术的理解深度不足，建议再安排一轮技术加面。",
             "经验基本匹配，薪资预期略高于岗位区间，需进一步沟通。"]
EVAL_FAIL = ["核心技术能力与岗位要求存在明显差距，项目描述无法深入展开，本轮不通过。",
             "沟通表达尚可，但缺乏岗位所需的实战经验，综合评估暂不匹配。"]
rows, irid = [], 0
for (cid, rid, name, stage) in CAND_INFO:
    ivs = INTERVIEWERS[rid]
    if stage == 5 or cid == 5:
        # 5 号候选人虽已淘汰，但走完了全部面试并发放过 Offer（最终拒绝）
        rounds = [(1, 1, 1), (2, 2, 1), (3, 3, 1)]
    elif stage == 4:
        rounds = [(1, 1, 1), (2, 2, 1)]
    elif stage == 3:
        rounds = [(1, 1, 1), (2, 2, 2 if cid % 2 else 1)]
    elif stage == 2:
        rounds = [(1, 1, None)]
    elif stage == 1:
        rounds = []
    else:
        rounds = [(1, 1, 3)]
    for (rno, itype, concl) in rounds:
        irid += 1
        if concl is None:
            score, ev, st_concl = None, None, None
            itime = date(2026, 9, 18) + timedelta(days=rno)
            ev = None
        else:
            score = random.randint(86, 94) if concl == 1 else (random.randint(72, 78) if concl == 2 else random.randint(52, 62))
            pool = EVAL_PASS if concl == 1 else (EVAL_HOLD if concl == 2 else EVAL_FAIL)
            ev = pool[cid % len(pool)]
            itime = None
        iv_time = (hm(date(2026, 8, 6) + timedelta(days=(cid * 2 + rno * 3) % 34), 10 + rno, 0)
                   if concl is not None else hm(itime, 14, 0))
        rows.append([N(irid), N(cid), N(rid), N(rno), N(itype), TS(iv_time),
                     q("北京市朝阳区总部 A 座 12 层会议室 %d" % (rno + 1)),
                     N(ivs[rno - 1]), N(score) if score else "NULL",
                     q(ev) if ev else "NULL", N(concl) if concl else "NULL"])
emit("interview_record", ["id", "candidate_id", "recruit_request_id", "round_no", "interview_type",
                          "interview_time", "interview_place", "interviewer_id", "score",
                          "evaluation", "conclusion"], rows, batch=40)

L("-- 录用通知：4 人已入职（与员工主数据 E0044~E0047 对应），1 人拒绝 Offer")
# (Offer编号, 候选人, 需求, 部门, 岗位, 定级, 转正月薪, 试用月数, 入职日期, 审批状态, 发送日, 确认日, 状态, 备注)
OFFERS = [
    ("OF20260723001", 1, 1, 5, 1, "P4", 13000, 3, date(2026, 8, 3), 2, date(2026, 7, 23), date(2026, 7, 25), 6, "已按期入职，工号 E0044"),
    ("OF20260801002", 2, 2, 6, 2, "P3", 11000, 3, date(2026, 8, 10), 2, date(2026, 8, 1), date(2026, 8, 3), 6, "已按期入职，工号 E0045"),
    ("OF20260805003", 3, 3, 7, 3, "P3", 10500, 3, date(2026, 8, 17), 2, date(2026, 8, 5), date(2026, 8, 7), 6, "已按期入职，工号 E0046"),
    ("OF20260815004", 4, 4, 9, 9, "P3", 10000, 3, date(2026, 9, 15), 2, date(2026, 8, 15), date(2026, 8, 18), 3, "已确认，待 9 月 15 日入职，工号 E0047"),
    ("OF20260825005", 5, 1, 5, 1, "P6", 21000, 3, date(2026, 9, 1), 2, date(2026, 8, 25), date(2026, 8, 29), 4, "候选人已接受其他公司 Offer，放弃入职"),
]
rows = []
for (ono, cand, rid, dept, pos, level, sal, prob_m, entry, appr, send_t, confirm_t, st, remark) in OFFERS:
    # offer_record 无 remark 字段，入职说明以 offer_url 指向的 Offer 文件承载
    rows.append([q(ono), N(cand), N(rid), N(dept), N(pos), q(level), M(sal), N(prob_m),
                 M(r2(sal * 0.80)), D(entry), N(appr),
                 N(1001) if appr == 1 else "NULL", TS(hm(send_t, 15, 0)),
                 TS(hm(confirm_t, 10, 30)) if confirm_t else "NULL",
                 q("https://files.enterprise.com/offer/%s.pdf" % ono.lower()), N(st)])
emit("offer_record", ["offer_no", "candidate_id", "recruit_request_id", "dept_id", "position_id",
                      "job_level", "offer_salary", "probation_months", "probation_salary",
                      "entry_date", "approval_status", "current_approver_id", "send_time",
                      "confirm_time", "offer_url", "status"], rows, batch=20)

# ====================================================================================
# 十、培训管理：讲师 → 课程 → 培训计划 → 报名签到 → 培训档案
# ====================================================================================
L("-- ####################################################################################")
L("--  十、培训管理 —— 讲师 / 课程 / 培训计划 / 报名签到 / 培训档案（全链路）")
L("-- ####################################################################################")
L()

# 讲师：(id, 编号, 姓名, 类型, 关联员工, 擅长领域, 等级, 电话, 邮箱, 所属机构, 课时费)
TRAINERS = [
    (1, "TR001", "郑十一", 1, 1003, "系统架构 / 技术管理 / 研发规范", 4, "13801000001", "zhengshiyi@enterprise.com", None, 800.00),
    (2, "TR002", "冯超", 1, 1004, "分布式架构 / 高并发 / 性能调优", 4, "13801000002", "fengchao@enterprise.com", None, 800.00),
    (3, "TR003", "张三", 1, 1005, "Java / Spring 生态 / 工程实践", 3, "13801000003", "zhangsan@enterprise.com", None, 500.00),
    (4, "TR004", "刘芳", 1, 1036, "人力资源 / 劳动关系 / 绩效管理", 4, "13801000004", "liufang@enterprise.com", None, 600.00),
    (5, "TR005", "韩雪", 1, 1020, "前端工程化 / Vue3 / 组件库建设", 3, "13801000005", "hanxue@enterprise.com", None, 500.00),
    (6, "TR006", "王强", 1, 1037, "财务核算 / 税务合规 / 预算管理", 3, "13801000006", "wangqiang@enterprise.com", None, 500.00),
    (7, "TR007", "李建华", 2, None, "信息安全 / 数据合规 / 等保测评", 4, "13911110001", "lijianhua@anxin-consulting.com", "北京安信信息技术咨询有限公司", 3000.00),
    (8, "TR008", "周敏", 2, None, "通用管理 / 沟通表达 / 教练技术", 4, "13911110002", "zhoumin@bosi-consulting.com", "北京博思管理顾问有限公司", 3500.00),
]

# 课程：(id, 编码, 名称, 形式, 分类, 学时, 讲师, 人数上限)
COURSES = [
    (1, "CRS001", "新员工入职引导", 2, "通用素质", 8.0, 4, 60),
    (2, "CRS002", "人事系统操作实务", 1, "通用素质", 4.0, 4, 0),
    (3, "CRS003", "Spring Boot 微服务实战", 2, "技术类", 16.0, 3, 40),
    (4, "CRS004", "Vue3 组件化开发实践", 2, "技术类", 12.0, 5, 30),
    (5, "CRS005", "分布式架构设计进阶", 2, "技术类", 16.0, 2, 25),
    (6, "CRS006", "数据安全与合规红线", 1, "合规培训", 4.0, 7, 0),
    (7, "CRS007", "高效沟通与向上管理", 2, "管理类", 8.0, 8, 40),
    (8, "CRS008", "目标拆解与绩效管理", 2, "管理类", 8.0, 4, 40),
    (9, "CRS009", "财务报销与预算意识", 1, "通用素质", 4.0, 6, 0),
    (10, "CRS010", "消防安全与应急演练", 2, "通用素质", 4.0, 8, 80),
]
COURSE_NAME = {c[0]: c[2] for c in COURSES}
COURSE_HOURS = {c[0]: c[5] for c in COURSES}
COURSE_TRAINER = {c[0]: c[6] for c in COURSES}

# 培训计划：(id, 编号, 名称, 年度, 季度, 类型, 对象范围, 指定部门, 预算, 实际支出, 开始, 结束,
#            组织人, 状态, 课程, 参训员工列表)
PLANS = [
    (1, "PLAN202607-01", "2026年7月新员工入职培训", 2026, 3, 1, 4, None, 12000, 11800,
     date(2026, 7, 6), date(2026, 7, 8), 1034, 4, 1, [1018, 1019]),
    (2, "PLAN202608-02", "2026年8月新员工入职培训", 2026, 3, 1, 4, None, 18000, 17600,
     date(2026, 8, 19), date(2026, 8, 21), 1034, 4, 1, [1044, 1045, 1046]),
    (3, "PLAN2026Q1-03", "2026年Q1中层管理能力提升", 2026, 1, 3, 2, "1,2,3,4,12,13", 60000, 58200,
     date(2026, 3, 9), date(2026, 3, 13), 1036, 4, 8,
     [1003, 1004, 1020, 1026, 1030, 1031, 1036, 1037, 1040, 1042]),
    (4, "PLAN2026Q2-04", "2026年Q2技术专项训练营", 2026, 2, 2, 2, "2,5,6,7", 90000, 86400,
     date(2026, 5, 11), date(2026, 6, 19), 1003, 4, 3,
     [1005, 1006, 1007, 1008, 1009, 1010, 1011, 1021, 1022, 1023, 1027, 1028, 1029]),
    (5, "PLAN2026Q2-05", "2026年Q2合规与信息安全培训", 2026, 2, 5, 1, None, 30000, 27600,
     date(2026, 6, 15), date(2026, 6, 30), 1036, 4, 6, ACTIVE_IDS[:20]),
    (6, "PLAN2026Q3-06", "2026年Q3通用素质提升（沟通与绩效）", 2026, 3, 4, 1, None, 45000, 21000,
     date(2026, 8, 17), date(2026, 9, 25), 1036, 3, 7, ACTIVE_IDS[10:28]),
    (7, "PLAN2026Q4-07", "2026年Q4技术架构进阶", 2026, 4, 2, 1, None, 80000, 0,
     date(2026, 10, 19), date(2026, 11, 20), 1003, 2, 5, []),
]

# 讲师累计授课次数 / 平均满意度：由以下报名与培训档案真实汇总
_teach = {}
for (pid, pcode, pname, py, pq, ptype, pscope, pdepts, budget, actual,
     pstart, pend, org, pst, course_id, members) in PLANS:
    if not members:
        continue
    t = COURSE_TRAINER[course_id]
    d = _teach.setdefault(t, {"n": 0, "scores": []})
    d["n"] += 1
    for _eid in members:
        d["scores"].append(random.choice([4, 4, 5, 5, 5, 3, 4, 5]))

rows = []
for (tid, tcode, tname, ttype, emp_id, expertise, level, phone, email, org_name, rate) in TRAINERS:
    st = _teach.get(tid, {"n": 0, "scores": []})
    avg = r2(sum(st["scores"]) / len(st["scores"])) if st["scores"] else None
    rows.append([N(tid), q(tcode), q(tname), N(ttype), N(emp_id) if emp_id else "NULL",
                 q(expertise), N(level), q(phone), q(email),
                 q(org_name) if org_name else "NULL", M(rate), N(st["n"]),
                 M(avg, 1) if avg else "NULL", N(1),
                 q("内部讲师，由业务骨干兼任") if ttype == 1 else q("外部签约讲师，按课时结算")])
emit("trainer", ["id", "trainer_code", "trainer_name", "trainer_type", "employee_id", "expertise",
                 "trainer_level", "phone", "email", "org_name", "hour_rate", "teach_count",
                 "avg_score", "status", "remark"], rows, batch=20)

L("-- 课程库（10 门，涵盖新员工 / 技术 / 管理 / 合规四类）")
rows = []
for (cid_, ccode, cname, ctype, cat, hours, tid, maxs) in COURSES:
    rows.append([N(cid_), q(ccode), q(cname), N(ctype), q(cat), M(hours, 1), N(tid),
                 q("https://files.enterprise.com/course/%s.pptx" % ccode.lower()),
                 q("https://video.enterprise.com/course/%s.mp4" % ccode.lower()) if ctype == 1 else "NULL",
                 q("1、课程目标与适用人群；\n2、核心知识点讲解；\n3、案例演练与实操；\n4、考核与复盘。"),
                 N(maxs), N(1), q("年度课程体系内课程")])
emit("course", ["id", "course_code", "course_name", "course_type", "category", "hours", "trainer_id",
                "courseware_url", "video_url", "outline", "max_students", "status", "remark"],
     rows, batch=20)

L("-- 培训计划（年度计划体系：新员工 / 技术 / 管理 / 合规四类）")
rows = []
for (pid, pcode, pname, py, pq, ptype, pscope, pdepts, budget, actual,
     pstart, pend, org, pst, course_id, members) in PLANS:
    desc = ("面向 %s 的专项培养计划，由 %s 组织，共 %d 人报名，"
            "课程《%s》%.1f 学时。" % (
                "全体员工" if pscope == 1 else "指定部门/人员",
                EMP_BY_ID[org][2], len(members),
                COURSE_NAME[course_id], COURSE_HOURS[course_id]))
    rows.append([N(pid), q(pcode), q(pname), N(py), N(pq), N(ptype), N(pscope),
                 q(pdepts) if pdepts else "NULL", M(budget), M(actual), D(pstart), D(pend),
                 N(org), q(desc), N(pst)])
emit("training_plan", ["id", "plan_code", "plan_name", "plan_year", "plan_quarter", "train_type",
                       "target_scope", "target_dept_ids", "budget_amount", "actual_amount",
                       "start_date", "end_date", "organizer_id", "description", "status"],
     rows, batch=20)

L("-- 培训报名与签到：已完成计划的考核成绩与满意度已回填，进行中计划部分未签到")
ENROLL_DONE = {}          # (plan_id, employee_id) -> (exam_score, pass_status, satisfaction)
rows, enid = [], 0
for (pid, pcode, pname, py, pq, ptype, pscope, pdepts, budget, actual,
     pstart, pend, org, pst, course_id, members) in PLANS:
    if pst == 2:
        continue                                    # 已发布但未开始报名
    for eid in members:
        enid += 1
        if pst == 4:
            si, si_time = 1, hm(pstart, 9, 15)
            score = random.choice([78, 82, 85, 88, 90, 92, 95, 68, 74, 96])
            pass_st = 1 if score >= 70 else 2
            sat = random.choice([4, 4, 5, 5, 5, 3])
        else:
            # 进行中：前一半已签到并考核，后一半尚未签到
            if enid % 2 == 0:
                si, si_time = 1, hm(pstart, 9, 20)
                score = random.choice([80, 85, 88, 92])
                pass_st, sat = 1, random.choice([4, 5, 5])
            else:
                si, si_time, score, pass_st, sat = 0, None, None, 0, None
        ENROLL_DONE[(pid, eid)] = (score, pass_st, sat)
        rows.append([N(enid), N(pid), N(course_id), N(eid),
                     TS(hm(pstart - timedelta(days=5), 10, 0)), N(si), TS(si_time),
                     M(score, 1) if score else "NULL", N(pass_st),
                     N(sat) if sat else "NULL",
                     q("线上课程已完成学习") if COURSE_TRAINER[course_id] == 7 else "NULL"])
emit("training_enroll", ["id", "plan_id", "course_id", "employee_id", "enroll_time",
                         "sign_in_status", "sign_in_time", "exam_score", "pass_status",
                         "satisfaction_score", "remark"], rows, batch=50)

L("-- 培训档案：已完成计划归档，形成员工个人培训学时与证书记录")
rows, trid = [], 0
for (pid, pcode, pname, py, pq, ptype, pscope, pdepts, budget, actual,
     pstart, pend, org, pst, course_id, members) in PLANS:
    if pst != 4:
        continue
    for eid in members:
        score, pass_st, sat = ENROLL_DONE[(pid, eid)]
        trid += 1
        rows.append([N(trid), N(eid), N(pid), N(course_id), q(COURSE_NAME[course_id]), N(ptype),
                     M(COURSE_HOURS[course_id], 1), D(pend), M(score, 1), N(pass_st),
                     q("CERT-2026-%04d" % trid) if pass_st == 1 else "NULL", N(2026)])
emit("training_record", ["id", "employee_id", "plan_id", "course_id", "course_name", "train_type",
                         "hours", "train_date", "exam_score", "pass_status", "certificate_no",
                         "archive_year"], rows, batch=50)

# ====================================================================================
# 十一、绩效管理：考核方案 → 考核指标 → 考核记录 → 绩效面谈
# ====================================================================================
L("-- ####################################################################################")
L("--  十一、绩效管理 —— 考核方案 / 考核指标 / 考核记录 / 绩效面谈（全链路）")
L("-- ####################################################################################")
L()

PERF_PLANS = [
    (1, "PP2026-Q1", "2026年第一季度绩效考核", 2, 2026, 1, None, date(2026, 4, 1), date(2026, 4, 20), 4),
    (2, "PP2026-Q2", "2026年第二季度绩效考核", 2, 2026, 2, None, date(2026, 7, 1), date(2026, 7, 20), 4),
    (3, "PP2026-08", "2026年8月绩效考核", 1, 2026, None, 8, date(2026, 9, 1), date(2026, 9, 20), 3),
    (4, "PP2026-Q3", "2026年第三季度绩效考核", 2, 2026, 3, None, date(2026, 10, 8), date(2026, 10, 25), 2),
]
rows = []
for (ppid, pcode, pname, ptype, py, pq, pm, ps, pe, pst) in PERF_PLANS:
    rows.append([N(ppid), q(pcode), q(pname), N(ptype), N(py),
                 N(pq) if pq else "NULL", N(pm) if pm else "NULL", D(ps), D(pe),
                 N(1), "NULL",
                 q("S:90 分以上，A:85-89 分，B:75-84 分，C:65-74 分，D:65 分以下"),
                 N(1), q("S:10,A:25,B:45,C:15,D:5"), "20.00", "70.00", "10.00",
                 N(1036),
                 q("%s，考核范围覆盖全体在职员工，采用员工自评 + 主管评分 + 隔级复核的三维加权方式。" % pname),
                 N(pst)])
emit("performance_plan", ["id", "plan_code", "plan_name", "period_type", "plan_year",
                          "plan_quarter", "plan_month", "start_date", "end_date", "scope_type",
                          "scope_dept_ids", "grade_rule", "force_distribution", "distribution_rule",
                          "self_weight", "leader_weight", "review_weight", "creator_id",
                          "description", "status"], rows, batch=20)

L("-- 考核指标：按岗位序列下达 3 项指标（KPI / 能力 / 态度），权重合计 100%")
INDICATOR_TPL = {
    1: [("团队目标达成率", 1, "≥95%", 40), ("核心人才保留率", 1, "≥90%", 30), ("团队建设与人才培养", 3, "完成 2 场以上培训", 30)],
    2: [("需求交付及时率", 1, "≥95%", 40), ("线上缺陷密度", 1, "≤0.5 个/千行", 30), ("技术方案质量", 3, "方案一次评审通过", 30)],
    3: [("版本交付达成率", 1, "≥90%", 40), ("需求响应及时性", 1, "≤2 个工作日", 30), ("跨部门协作满意度", 4, "≥4.5 分", 30)],
    4: [("流程执行合规率", 1, "100%", 40), ("事务处理准确率", 1, "≥99%", 40), ("服务响应满意度", 4, "≥4.5 分", 20)],
}
EMP_Q1 = [e[0] for e in EMP if on_payroll(e[0], 2026, 3)]
EMP_Q2 = [e[0] for e in EMP if on_payroll(e[0], 2026, 6)]
# 8 月绩效考核对象：8 月在册的在职员工（不含已离职者与 9 月才报到的待入职人员）
EMP_Q3 = [e[0] for e in EMP if e[13] in (1, 2) and on_payroll(e[0], 2026, 8)]
PLAN_MEMBERS = {1: EMP_Q1, 2: EMP_Q2, 3: EMP_Q3, 4: []}
PLAN_GRADE_FN = {1: lambda e: perf_grade_of(e + 1), 2: lambda e: perf_grade_of(e),
                 3: lambda e: perf_grade_of(e * 3 + 5), 4: lambda e: perf_grade_of(e)}
# 等级 ↔ 分数区间：与 performance_plan.grade_rule 中「S:90+，A:85-89，B:75-84，C:65-74，D:<65」严格对应
GRADE_BAND = {"S": (90.5, 98.0), "A": (85.0, 89.5), "B": (75.0, 84.5),
              "C": (65.0, 74.5), "D": (55.0, 64.5)}
rows, iid = [], 0
for ppid, member_list in PLAN_MEMBERS.items():
    if ppid == 4:
        continue
    for eid in member_list:
        seq = 1 if EMP_BY_ID[eid][8].startswith("M") else (2 if EMP_BY_ID[eid][6] in (5, 6) else
                                                          (3 if EMP_BY_ID[eid][6] in (3, 8, 9, 10, 12) else 4))
        confirmed = 1 if (ppid in (1, 2) or eid % 3) else 0
        for sort_no, (iname, itype, target, weight) in enumerate(INDICATOR_TPL[seq], start=1):
            iid += 1
            rows.append([N(iid), N(ppid), N(eid), q(iname), N(itype), M(weight, 2), q(target),
                         "NULL", q("按实际完成值与目标值对比评分，达成率低于 70% 计不及格"),
                         N(1), N(confirmed)])
emit("performance_indicator", ["id", "plan_id", "employee_id", "indicator_name", "indicator_type",
                               "weight", "target_value", "actual_value", "score_standard", "source",
                               "confirm_status"], rows, batch=100)

L("-- 绩效考核记录：Q1 / Q2 已归档锁定，8 月考核处于评分中（部分员工尚未自评）")
RECORDS = []                    # 供绩效面谈引用
prid = 0
for ppid in (1, 2, 3):
    gfn = PLAN_GRADE_FN[ppid]
    members = PLAN_MEMBERS[ppid]
    archived = ppid in (1, 2)
    for idx, eid in enumerate(members):
        prid += 1
        e = EMP_BY_ID[eid]
        leader = MANAGER.get(eid, 1001)
        if archived:
            flow = 5
        elif idx % 3 == 0:
            flow = 1                                    # 待自评
        elif idx % 3 == 1:
            flow = 2                                    # 待主管评分
        else:
            flow = 5                                    # 已完成待归档
        rec = {"id": prid, "plan": ppid, "eid": eid, "dept": e[6], "leader": leader,
               "flow": flow, "grade": None, "final": None, "archived": archived}
        if flow == 1:
            rec.update(self_s=None, leader_s=None, review_s=None, confirm=0,
                       appeal=None, appeal_rs=None)
        else:
            band = GRADE_BAND[gfn(eid)]
            final = r2(random.uniform(band[0], band[1]))
            rec["grade"], rec["final"] = gfn(eid), final
            rec["self_s"] = r2(max(final - random.uniform(0.5, 4.0), 50.0))
            rec["leader_s"] = r2(min(max(final + random.uniform(-1.0, 1.0), 50.0), 100.0))
            rec["review_s"] = (r2(min(final + random.uniform(0.0, 2.5), 100.0))
                               if flow == 5 else None)
            if archived and idx % 23 == 7:
                rec.update(confirm=2, appeal="对 8 月加班较多月份的绩效评分存在疑问，申请复核",
                           appeal_rs="经复核，主管评分依据充分，维持原结果，已与员工沟通达成一致")
            else:
                rec.update(confirm=1 if flow == 5 else 0, appeal=None, appeal_rs=None)
        RECORDS.append(rec)

# 部门内排名：按最终得分降序
_by_dept = {}
for r in RECORDS:
    if r["final"] is not None:
        _by_dept.setdefault((r["plan"], r["dept"]), []).append(r)
for lst in _by_dept.values():
    lst.sort(key=lambda x: -x["final"])
    for i, r in enumerate(lst, start=1):
        r["rank"] = i

rows = []
for r in RECORDS:
    if r["flow"] == 5:
        remark = "已完成自评、主管评分与隔级复核" if r["archived"] else "评分完成，待 HR 归档"
    elif r["flow"] == 2:
        remark = "员工自评已提交，等待主管评分"
    else:
        remark = "考核已启动，等待员工自评"
    rows.append([N(r["id"]), N(r["plan"]), N(r["eid"]), N(r["dept"]), N(r["leader"]),
                 M(r["self_s"]) if r["self_s"] is not None else "NULL",
                 M(r["leader_s"]) if r["leader_s"] is not None else "NULL",
                 M(r["review_s"]) if r["review_s"] is not None else "NULL",
                 M(r["final"]) if r["final"] is not None else "NULL",
                 q(r["grade"]) if r["grade"] else "NULL",
                 N(r.get("rank")) if r.get("rank") else "NULL",
                 N(r["confirm"]), q(r["appeal"]) if r["appeal"] else "NULL",
                 q(r["appeal_rs"]) if r["appeal_rs"] else "NULL",
                 N(1 if r["archived"] else 0),
                 TS(hm(date(2026, 4, 25), 17, 0)) if r["archived"] and r["plan"] == 1
                 else (TS(hm(date(2026, 7, 24), 17, 0)) if r["archived"] else "NULL"),
                 N(r["flow"]), q(remark)])
emit("performance_record", ["id", "plan_id", "employee_id", "dept_id", "leader_id", "self_score",
                            "leader_score", "review_score", "final_score", "grade", "dept_rank",
                            "employee_confirm_status", "appeal_reason", "appeal_result",
                            "archive_status", "archive_time", "status", "remark"], rows, batch=40)

L("-- 绩效面谈：S/A 档做正向反馈与目标共创，C/D 档做绩效改进计划（PIP）")
ACH = {"S": "本周期关键指标全部超额达成，牵头完成核心模块重构，对团队交付效率提升贡献显著。",
       "A": "各项指标达成良好，在跨部门协作中主动补位，交付质量稳定。",
       "C": "基本完成岗位职责，但部分指标达成率偏低，问题定位与推进节奏有待加强。",
       "D": "关键指标未达成，交付延期较为集中，需要在方法与投入度上做系统性改进。"}
SHT = {"S": "可承担更复杂的架构性任务，建议下一步承担小组技术负责人角色。",
       "A": "在技术深度与影响力上仍有提升空间，建议输出更多可复用沉淀。",
       "C": "需加强需求理解与任务拆解能力，建议建立日报机制并接受主管双周辅导。",
       "D": "需制定 3 个月绩效改进计划，明确每周可量化目标，主管每周复盘一次。"}
NXT = {"S": "下周期牵头一个跨部门重点项目，并培养 1 名后备骨干。",
       "A": "下周期保持交付质量，同时在技术分享与文档沉淀上达成 2 项目标。",
       "C": "下周期核心指标达成率提升至 90% 以上，缺陷密度下降 30%。",
       "D": "下周期完成 PIP 全部里程碑，季度末复评仍不达标将调整岗位。"}
FDB = ["希望增加参与技术方案评审的机会，提升个人在团队中的影响力。",
       "希望公司能提供更清晰的晋升通道说明与对应的能力模型。",
       "当前工作量与节奏基本匹配，希望减少临时插入需求带来的返工。",
       "希望能参加外部技术大会，拓宽视野。"]
rows, piid = [], 0
for r in RECORDS:
    if r["grade"] not in ("S", "A", "C", "D"):
        continue
    piid += 1
    base = date(2026, 4, 22) if r["plan"] == 1 else date(2026, 7, 22)
    itime = hm(base + timedelta(days=(r["eid"] * 3) % 6), 15, 0)
    rows.append([N(piid), N(r["id"]), N(r["eid"]), N(r["leader"]), TS(itime),
                 q("总部 A 座 12 层 1208 洽谈室"),
                 q("目标完成情况：%s；最终得分 %.2f 分，等级 %s。" % (ACH[r["grade"]], r["final"], r["grade"])),
                 q(SHT[r["grade"]]), q(NXT[r["grade"]]), q(FDB[r["eid"] % 4])])
emit("performance_interview", ["id", "record_id", "employee_id", "interviewer_id", "interview_time",
                               "interview_place", "achievement", "shortcoming", "next_goal",
                               "employee_feedback"], rows, batch=40)

# ====================================================================================
# 十二、公告 / 待办 / 通用审批流水
# ====================================================================================
L("-- ####################################################################################")
L("--  十二、首页与公共 —— 通知公告 / 公告已读 / 待办任务 / 通用审批流水")
L("-- ####################################################################################")
L()

# (标题, 正文, 类型, 发布人, 发布时间, 失效时间, 范围, 指定部门, 置顶, 状态, 已读人数上限)
NOTICES = [
    ("关于 2026 年中秋节放假安排的通知",
     "各部门：\n根据国务院办公厅通知精神，结合公司实际，现将 2026 年中秋节放假安排通知如下：\n一、放假时间：9 月 25 日至 9 月 27 日，共 3 天；\n二、请各部门提前做好工作安排与值班部署，确保业务连续性；\n三、放假期间请保持通讯畅通。\n特此通知。",
     2, 1036, date(2026, 9, 1), date(2026, 9, 28), 1, None, 1, 2, 46),
    ("《员工考勤与休假管理办法》修订发布",
     "为规范考勤管理、保障员工休假权益，公司对《员工考勤与休假管理办法》进行了修订，自 2026 年 9 月 1 日起执行。\n主要变化：\n1、弹性班次适用范围扩大至产品与设计岗位；\n2、迟到 30 分钟以上按半日事假处理；\n3、年假结转有效期延长至次年 3 月 31 日。\n请全体员工知悉并遵照执行。",
     1, 1036, date(2026, 8, 28), date(2026, 12, 31), 1, None, 1, 2, 44),
    ("2026 年第三季度绩效考核工作启动",
     "各位同事：\n2026 年 8 月绩效考核已于 9 月 1 日启动，请于 9 月 10 日前完成员工自评，9 月 20 日前主管完成评分，9 月 25 日前完成隔级复核。\n逾期未提交将影响绩效工资核算，请务必按时完成。",
     3, 1036, date(2026, 9, 1), date(2026, 9, 30), 1, None, 1, 2, 46),
    ("关于 2026 年下半年全员体检的通知",
     "公司将为全体员工安排年度健康体检，具体安排：\n时间：9 月 15 日 - 9 月 30 日（工作日上午 8:00-11:00）\n地点：美年大健康朝阳体检中心\n请各部门在 9 月 10 日前汇总参检人员名单报行政部。体检当天需空腹。",
     5, 1040, date(2026, 9, 3), date(2026, 9, 30), 1, None, 0, 2, 41),
    ("关于 2026 年国庆节后团建活动的通知",
     "公司定于 10 月 16 日 - 10 月 17 日组织全员团建活动，地点为怀柔雁栖湖拓展基地。\n活动包含团队拓展、户外徒步与总结晚宴。请各部门提前协调工作，具体乘车安排另行通知。",
     4, 1040, date(2026, 9, 8), date(2026, 10, 17), 1, None, 0, 2, 24),
    ("技术中心 Q3 技术分享会安排",
     "技术中心 Q3 技术分享会安排如下：\n第一场：9 月 18 日 16:00 《高并发场景下的缓存设计》 主讲：冯超\n第二场：9 月 25 日 16:00 《前端性能优化实践》 主讲：韩雪\n地点：A 座 12 层多功能厅，欢迎其他部门同事参加。",
     5, 1003, date(2026, 9, 9), date(2026, 9, 30), 2, "2,5,6,7", 0, 2, 21),
    ("关于规范加班申请与调休使用的说明",
     "近期发现部分员工存在先加班后补申请的情况，为规范管理，现明确：\n1、加班须事前提交申请，经直属主管审批通过后方可执行；\n2、工作日加班按 1.5 倍、休息日加班按 2 倍折算调休或计薪；\n3、调休需在 3 个月内使用完毕，逾期作废。",
     1, 1036, date(2026, 8, 20), date(2026, 12, 31), 1, None, 0, 2, 39),
    ("2026 年校园招聘正式启动",
     "2027 届校园招聘已正式启动，本次计划招聘 15 人，岗位涵盖研发、产品、测试与职能序列。\n内部推荐成功入职的，推荐人可获得 2000 元推荐奖金。\n简历请发送至 hr@enterprise.com 或直接联系人力资源部李娜。",
     3, 1034, date(2026, 9, 5), date(2026, 12, 31), 1, None, 1, 2, 43),
    ("关于办公区消防演练的通知",
     "为提升全员消防安全意识，公司将于 9 月 20 日上午 10:00 组织消防疏散演练，预计时长 40 分钟。\n演练期间请听从引导员指挥，按指定路线有序疏散至集合点。",
     5, 1040, date(2026, 9, 12), date(2026, 9, 21), 1, None, 0, 2, 18),
    ("财务部关于 9 月报销单提交截止时间的提醒",
     "9 月费用报销单请于 9 月 25 日前提交至财务部，逾期将顺延至下月处理。\n差旅报销需附出差申请单与发票原件，电子发票请打印后提交。",
     3, 1037, date(2026, 9, 11), date(2026, 9, 26), 1, None, 0, 2, 27),
    ("关于调整社保公积金缴费基数的通知",
     "根据北京市社保中心通知，2026 年度社保及公积金缴费基数上下限已调整，公司将于 9 月起按新基数申报，个人缴纳部分相应变动，请留意工资条明细。",
     3, 1036, date(2026, 8, 25), date(2026, 12, 31), 1, None, 0, 3, 0),
    ("【草稿】2026 年度评优评先工作方案",
     "为激励先进、树立标杆，公司拟开展 2026 年度评优评先工作，设置优秀员工、优秀团队、技术创新奖等奖项。本方案尚在讨论中，待定稿后正式发布。",
     5, 1036, None, None, 1, None, 0, 1, 0),
]
rows = []
for i, (t, c, ntype, pub, pt, et, scope, depts, top, st, reads) in enumerate(NOTICES, start=1):
    rows.append([N(i), q(t), q(c), N(ntype), N(pub), TS(hm(pt, 9, 0)) if pt else "NULL",
                 TS(hm(et, 18, 0)) if et else "NULL", N(scope),
                 q(depts) if depts else "NULL", N(top), N(reads), N(st)])
emit("notice", ["id", "title", "content", "notice_type", "publisher_id", "publish_time",
                "expire_time", "target_scope", "target_dept_ids", "is_top", "read_count", "status"],
     rows, batch=10)

L("-- 公告已读记录：read_count 与明细条数严格一致")
rows, nrid = [], 0
for i, (t, c, ntype, pub, pt, et, scope, depts, top, st, reads) in enumerate(NOTICES, start=1):
    if st != 2 or not reads:
        continue
    target = ACTIVE_IDS if scope == 1 else [e[0] for e in EMP if str(e[6]) in (depts or "").split(",") and e[13] in (1, 2)]
    picked = target[:reads]
    for k, eid in enumerate(picked):
        nrid += 1
        rows.append([N(nrid), N(i), N(eid), TS(hm(pt + timedelta(days=1), 9, 20 + (k * 7) % 38))])
emit("notice_read", ["id", "notice_id", "employee_id", "read_time"], rows, batch=100)

L("-- 待办任务：审批待办 / 合同到期 / 转正提醒 / 绩效评分 / 培训报名")
rows, tdid = [], 0
TODO_EXTRA = [
    (1036, 2, "员工劳动合同到期提醒", "刘杰（E0048）、王芳（E0049）、李强（E0050）等人劳动合同已于 6-7 月终止", 2, 2, "已办理离职手续，无需续签", date(2026, 6, 20)),
    (1036, 2, "劳动合同到期提醒", "王磊（E0045）劳动合同将于 2026-08-10 到期，请提前 30 日办理续签", 2, None, "已完成续签，期限 3 年", date(2026, 7, 10)),
    (1036, 3, "试用期转正提醒", "唐嘉（E0019）试用期至 2026-09-30，请按期组织转正考核", 3, None, None, date(2026, 9, 30)),
    (1036, 3, "试用期转正提醒", "刘丹（E0044）、王磊（E0045）、李静（E0046）试用期将于 2026-11 月结束，请提前准备考核材料", 3, None, None, date(2026, 10, 31)),
    (1034, 5, "新员工培训报名提醒", "2026 年 9 月新员工入职培训待组织，请安排讲师与场地", 5, None, "已协调讲师，计划 9 月下旬开班", date(2026, 9, 20)),
    (1040, 6, "办公用品季度盘点", "请各部门于 9 月 30 日前完成办公用品盘点并提交清单", 6, None, None, date(2026, 9, 30)),
    (1003, 4, "Q2 技术序列绩效复核", "技术中心 Q2 绩效评分已完成，请完成隔级复核", 4, None, "已完成复核", date(2026, 7, 20)),
    (1037, 4, "8 月薪资复核", "8 月薪资核算已完成，请复核后提交发放", 4, None, "已复核并提交发放，其中 2 人发放失败待重发", date(2026, 9, 8)),
]
for (rid_, ttype, title, content, biz_type, biz_id, handle, deadline) in TODO_EXTRA:
    tdid += 1
    rows.append([N(tdid), N(rid_), N(ttype), q(title), q(content),
                 N(biz_type) if biz_type else "NULL", N(biz_id) if biz_id else "NULL", "NULL",
                 N(2), TS(hm(deadline, 18, 0)),
                 N(1 if handle else 0), TS(hm(deadline - timedelta(days=1), 16, 0)) if handle else "NULL"])
# 审批待办：与待审批状态的请假 / 加班 / 出差单一一对应
for i, (eid, lt, d1, d2, days, st, reason) in enumerate(LEAVE_REQS, start=1):
    if st != 1:
        continue
    tdid += 1
    rows.append([N(tdid), N(MANAGER.get(eid, 1001)), N(1), q("请假申请待审批"),
                 q("%s 提交%s申请，%s 至 %s，共 %.1f 天，事由：%s" % (
                     EMP_BY_ID[eid][2], {1: "年假", 2: "病假", 3: "事假", 4: "婚假", 5: "产假",
                                         6: "陪产假", 7: "丧假", 8: "调休"}[lt],
                     d1.isoformat(), d2.isoformat(), days, reason)),
                 N(1), N(i), q("LV%s%03d" % (d1.strftime("%Y%m"), i)), N(2), TS(hm(d1 - timedelta(days=1), 18, 0)),
                 N(0), "NULL"])
for i, (eid, d, t1, t2, hours, otype, comp, st, reason) in enumerate(OVERTIME, start=1):
    if st != 1:
        continue
    tdid += 1
    rows.append([N(tdid), N(MANAGER.get(eid, 1001)), N(1), q("加班申请待审批"),
                 q("%s 提交 %s 加班申请，%s 至 %s，共 %.1f 小时，事由：%s" % (
                     EMP_BY_ID[eid][2], d.isoformat(), t1, t2, hours, reason)),
                 N(2), N(i), q("OT%s%03d" % (d.strftime("%Y%m"), i)), N(2),
                 TS(hm(d - timedelta(days=1), 18, 0)), N(0), "NULL"])
for i, (eid, dest, purpose, d1, d2, days, transport, st, budget) in enumerate(TRIPS, start=1):
    if st != 1:
        continue
    tdid += 1
    rows.append([N(tdid), N(MANAGER.get(eid, 1001)), N(1), q("出差申请待审批"),
                 q("%s 提交出差申请，目的地 %s，%s 至 %s，共 %.1f 天，事由：%s" % (
                     EMP_BY_ID[eid][2], dest, d1.isoformat(), d2.isoformat(), days, purpose)),
                 N(3), N(i), q("BT%s%03d" % (d1.strftime("%Y%m"), i)), N(2),
                 TS(hm(d1 - timedelta(days=2), 18, 0)), N(0), "NULL"])
for i, (rid_, rno, dept, pos, cnt, hired, level, smin, smax, edu, exp,
        hope, urg, applicant, apply_t, appr, st) in enumerate(REQUESTS, start=1):
    if appr != 1:
        continue
    tdid += 1
    rows.append([N(tdid), N(1001), N(1), q("招聘需求待审批"),
                 q("%s 提交《%s》招聘需求，计划招聘 %d 人，期望到岗 %s" % (
                     EMP_BY_ID[applicant][2], POS_NAME[pos], cnt, hope.isoformat())),
                 N(6), N(i), q(rno), N(3), TS(hm(apply_t + timedelta(days=2), 18, 0)), N(0), "NULL"])
emit("todo_task", ["id", "receiver_id", "task_type", "task_title", "task_content", "biz_type",
                   "biz_id", "biz_no", "priority", "deadline", "handle_status", "handle_time"],
     rows, batch=50)

L("-- 通用审批流水：请假 / 加班 / 出差 / 员工异动 / 离职 / 招聘需求 / 录用 / 调薪 全覆盖")
# employee_change 的 ID 需要与「四、员工异动」段严格对应：50 条入职 → N 条转正 → 8 条调岗晋升 → 3 条离职
_reg_approvals = 0
for e in EMP:
    if e[11] and e[13] != 3 and e[0] < 1044 and e[0] % 5 in (1, 3, 4):
        _reg_approvals += 1
CHANGE_BASE = 50 + _reg_approvals
rows, apid = [], 0


def ap(biz_type, biz_id, biz_no, level, approver, status, opinion, atime):
    global apid
    apid += 1
    rows.append([N(apid), N(biz_type), N(biz_id), q(biz_no) if biz_no else "NULL", N(level),
                 N(approver), q(EMP_BY_ID[approver][2]), N(status),
                 q(opinion) if opinion else "NULL", TS(atime) if atime else "NULL"])


for i, (eid, lt, d1, d2, days, st, reason) in enumerate(LEAVE_REQS, start=1):
    boss = MANAGER.get(eid, 1001)
    if st == 2:
        ap(1, i, "LV%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 2, "同意，注意工作交接", hm(d1 - timedelta(days=1), 17, 0))
    elif st == 3:
        ap(1, i, "LV%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 3, "不同意，当前项目处于关键交付期", hm(d1 - timedelta(days=1), 17, 0))
    else:
        ap(1, i, "LV%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 1, None, None)

for i, (eid, d, t1, t2, hours, otype, comp, st, reason) in enumerate(OVERTIME, start=1):
    boss = MANAGER.get(eid, 1001)
    if st == 2:
        ap(2, i, "OT%s%03d" % (d.strftime("%Y%m"), i), 1, boss, 2, "同意，按加班时长折算调休/计薪", hm(d - timedelta(days=1), 20, 0))
    elif st == 3:
        ap(2, i, "OT%s%03d" % (d.strftime("%Y%m"), i), 1, boss, 3, "不同意，工作量已在正常工作时间内，无需加班", hm(d - timedelta(days=1), 20, 0))
    else:
        ap(2, i, "OT%s%03d" % (d.strftime("%Y%m"), i), 1, boss, 1, None, None)

for i, (eid, dest, purpose, d1, d2, days, transport, st, budget) in enumerate(TRIPS, start=1):
    boss = MANAGER.get(eid, 1001)
    if st == 2:
        ap(3, i, "BT%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 2, "同意出差，差旅费按标准报销", hm(d1 - timedelta(days=2), 17, 30))
    elif st == 1:
        ap(3, i, "BT%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 1, None, None)
    else:
        ap(3, i, "BT%s%03d" % (d1.strftime("%Y%m"), i), 1, boss, 4, "转交总经理审批", hm(d1 - timedelta(days=2), 17, 30))

# 调岗晋升（biz_type=4）与离职（biz_type=5）审批：与 employee_change 一一对应
_ch_idx = CHANGE_BASE
for (eid, bd, ad, pos, bl, al, bs, as_, eff, reason) in CHANGES:
    _ch_idx += 1
    ap(4, _ch_idx, "BG%s%03d" % (eff.strftime("%Y%m"), _ch_idx), 1, 1036, 2,
       "异动申请材料齐全，同意办理", hm(eff - timedelta(days=3), 16, 0))
    if as_ > bs:
        ap(8, _ch_idx, "TG%s%03d" % (eff.strftime("%Y%m"), _ch_idx), 2, 1001, 2,
           "调薪幅度符合年度调薪预算，同意", hm(eff - timedelta(days=2), 11, 0))
for eid, ld in sorted(LEAVE_DATE.items()):
    _ch_idx += 1
    ap(5, _ch_idx, "LZ%s%03d" % (ld.strftime("%Y%m"), _ch_idx), 1, 1036, 2, "同意离职，工作交接已完成", hm(ld - timedelta(days=8), 15, 0))
    ap(5, _ch_idx, "LZ%s%03d" % (ld.strftime("%Y%m"), _ch_idx), 2, 1001, 2, "同意", hm(ld - timedelta(days=5), 10, 0))

for i, (rid_, rno, dept, pos, cnt, hired, level, smin, smax, edu, exp,
        hope, urg, applicant, apply_t, appr, st) in enumerate(REQUESTS, start=1):
    if appr == 2:
        ap(6, i, rno, 1, 1001, 2, "同意招聘，按计划推进", hm(apply_t + timedelta(days=1), 10, 30))
    elif appr == 1:
        ap(6, i, rno, 1, 1001, 1, None, None)

for i, (ono, cand, rid_, dept, pos, level, sal, prob_m, entry, appr, send_t, confirm_t, st, remark) in enumerate(OFFERS, start=1):
    ap(7, i, ono, 1, 1001, 2, "同意录用，薪酬水平符合岗位薪资区间", hm(send_t, 11, 0))

emit("approval_record", ["id", "biz_type", "biz_id", "biz_no", "approval_level", "approver_id",
                         "approver_name", "approval_status", "approval_opinion", "approval_time"],
     rows, batch=100)

# ====================================================================================
# 十三、系统日志：登录日志 / 操作日志
# ====================================================================================
L("-- ####################################################################################")
L("--  十三、系统日志 —— 登录日志 / 操作日志")
L("-- ####################################################################################")
L()

L("-- 登录日志：覆盖管理员与员工自助账号的成功 / 失败登录（工作日 09:00 前后集中）")
BROWSERS = [("Chrome 128", "Windows 11"), ("Edge 128", "Windows 11"),
            ("Chrome 127", "macOS 14.5"), ("Safari 17", "macOS 14.5"),
            ("Firefox 129", "Windows 10"), ("Chrome 126", "Windows 10")]
LOGIN_USERS = [("admin", 1)] + [(e[3], 2 + (e[0] - 1001)) for e in EMP if e[13] in (1, 2)]
rows, lgl_id = [], 0
for d in [date(2026, 9, 7), date(2026, 9, 8), date(2026, 9, 9), date(2026, 9, 10), date(2026, 9, 11)]:
    for k, (uname, uid) in enumerate(LOGIN_USERS):
        if (k + d.day) % 4 == 0:
            continue                                    # 并非每天每个账号都登录
        lgl_id += 1
        br, os_ = BROWSERS[(k + d.day) % len(BROWSERS)]
        rows.append([N(lgl_id), N(uid), q(uname), q("192.168.%d.%d" % (10 + (k % 8), 20 + (k * 7) % 200)),
                     q("北京市"), q(br), q(os_), N(1), q("登录成功"),
                     TS(hm(d, 8 + (k % 2), (k * 13) % 60))])
# 失败登录：密码错误 / 账号锁定
for k, (uname, uid) in enumerate(LOGIN_USERS[:12]):
    lgl_id += 1
    rows.append([N(lgl_id), N(uid) if k % 3 else "NULL", q(uname),
                 q("192.168.%d.%d" % (30 + k % 5, 60 + (k * 11) % 150)), q("北京市"),
                 q("Chrome 128"), q("Windows 11"), N(0),
                 q("密码错误，请重新输入（剩余尝试次数 %d）" % (5 - k % 4)),
                 TS(hm(date(2026, 9, 9 + k % 2), 22, (k * 17) % 60))])
emit("sys_login_log", ["id", "user_id", "username", "login_ip", "login_location", "browser",
                       "os", "login_status", "message", "login_time"], rows, batch=100)

L("-- 操作日志：覆盖员工 / 部门 / 考勤 / 薪资 / 招聘 / 培训 / 绩效 / 公告 / 字典等模块")
OPER_CATALOG = [
    ("员工信息", 1, "新增员工档案", "POST", "/api/employees", '{"name":"刘丹","deptId":5,"jobLevel":"P4"}'),
    ("员工信息", 2, "修改员工基本信息", "PUT", "/api/employees/1005", '{"phone":"13801000105","email":"zhangsan@enterprise.com"}'),
    ("员工信息", 3, "删除员工档案（逻辑删除）", "DELETE", "/api/employees/1050", "{}"),
    ("员工信息", 4, "分页查询员工列表", "GET", "/api/employees?pageNum=1&pageSize=10", "{}"),
    ("员工信息", 5, "导出员工花名册", "POST", "/api/employees/export", '{"deptId":2}'),
    ("员工信息", 6, "批量导入员工档案", "POST", "/api/employees/import", '{"fileName":"employee_2026Q3.xlsx"}'),
    ("部门管理", 1, "新增部门", "POST", "/api/depts", '{"deptName":"数据平台部","parentId":2}'),
    ("部门管理", 2, "调整部门负责人", "PUT", "/api/depts/7", '{"leaderId":1026}'),
    ("劳动合同", 1, "新增劳动合同", "POST", "/api/contracts", '{"employeeId":1044,"contractType":1}'),
    ("劳动合同", 2, "续签劳动合同", "PUT", "/api/contracts/46", '{"endDate":"2029-08-09"}'),
    ("员工异动", 1, "提交员工异动申请", "POST", "/api/changes", '{"employeeId":1007,"changeType":5}'),
    ("考勤管理", 2, "修正逐日打卡记录", "PUT", "/api/attendance/1013", '{"attendanceStatus":1,"remark":"补卡申请已批准"}'),
    ("考勤管理", 4, "查询月度考勤汇总", "GET", "/api/attendance/month?year=2026&month=7", "{}"),
    ("考勤管理", 5, "导出月度考勤表", "POST", "/api/attendance/month/export", '{"year":2026,"month":7}'),
    ("考勤管理", 6, "导入考勤机打卡数据", "POST", "/api/attendance/import", '{"fileName":"machine_202608.xlsx"}'),
    ("休假管理", 2, "审批请假申请", "PUT", "/api/leave/12/approve", '{"approvalStatus":2,"opinion":"同意"}'),
    ("休假管理", 3, "撤销请假申请", "DELETE", "/api/leave/31", "{}"),
    ("加班管理", 2, "审批加班申请", "PUT", "/api/overtime/9/approve", '{"approvalStatus":2}'),
    ("出差管理", 2, "审批出差申请", "PUT", "/api/trips/3/approve", '{"approvalStatus":2}'),
    ("薪资管理", 1, "新增员工薪资标准", "POST", "/api/salary/employees", '{"employeeId":1019,"baseSalary":6600}'),
    ("薪资管理", 2, "调整员工薪资标准", "PUT", "/api/salary/employees/51", '{"baseSalary":12300,"adjustReason":"晋升调薪"}'),
    ("薪资管理", 5, "导出薪资核算表", "POST", "/api/salary/records/export", '{"salaryYear":2026,"salaryMonth":8}'),
    ("薪资管理", 2, "复核月度薪资", "PUT", "/api/salary/records/88/audit", '{"payStatus":2}'),
    ("薪资管理", 7, "触发薪资发放", "POST", "/api/salary/records/pay", '{"salaryYear":2026,"salaryMonth":8}'),
    ("社保管理", 2, "维护社保缴纳记录", "PUT", "/api/insurance/46", '{"payStatus":1}'),
    ("社保管理", 5, "导出社保缴纳明细", "POST", "/api/insurance/export", '{"recordYear":2026,"recordMonth":8}'),
    ("招聘管理", 1, "提报招聘需求", "POST", "/api/recruit/requests", '{"positionId":1,"recruitCount":3}'),
    ("招聘管理", 2, "审批招聘需求", "PUT", "/api/recruit/requests/6/approve", '{"approvalStatus":2}'),
    ("招聘管理", 1, "登记候选人简历", "POST", "/api/recruit/candidates", '{"name":"钱佳宁","recruitRequestId":5}'),
    ("招聘管理", 2, "更新候选人阶段", "PUT", "/api/recruit/candidates/11", '{"stage":3}'),
    ("招聘管理", 1, "录入面试评价", "POST", "/api/recruit/interviews", '{"candidateId":1,"roundNo":2,"score":90}'),
    ("招聘管理", 1, "发放录用通知", "POST", "/api/recruit/offers", '{"candidateId":4,"offerSalary":10000}'),
    ("招聘管理", 6, "导入招聘网站简历", "POST", "/api/recruit/candidates/import", '{"channel":2}'),
    ("培训管理", 1, "新增培训计划", "POST", "/api/training/plans", '{"planCode":"PLAN2026Q4-08"}'),
    ("培训管理", 2, "发布培训计划", "PUT", "/api/training/plans/7/publish", "{}"),
    ("培训管理", 1, "登记培训签到", "POST", "/api/training/enroll/sign-in", '{"planId":6,"employeeId":1011}'),
    ("培训管理", 5, "导出培训档案", "POST", "/api/training/records/export", '{"archiveYear":2026}'),
    ("绩效管理", 1, "创建考核方案", "POST", "/api/performance/plans", '{"planCode":"PP2026-Q3","periodType":2}'),
    ("绩效管理", 2, "下发考核指标", "PUT", "/api/performance/plans/3/indicators", '{"planId":3,"scopeType":1}'),
    ("绩效管理", 2, "提交员工自评", "PUT", "/api/performance/records/92/self", '{"selfScore":86.5}'),
    ("绩效管理", 2, "提交主管评分", "PUT", "/api/performance/records/92/leader", '{"leaderScore":88.0}'),
    ("绩效管理", 7, "触发强制分布校验", "POST", "/api/performance/plans/2/distribute", "{}"),
    ("公告管理", 1, "发布通知公告", "POST", "/api/notices", '{"title":"关于 2026 年中秋节放假安排的通知"}'),
    ("公告管理", 2, "修改通知公告", "PUT", "/api/notices/2", '{"isTop":1}'),
    ("公告管理", 3, "下架通知公告", "DELETE", "/api/notices/11", "{}"),
    ("系统设置", 1, "新增数据字典项", "POST", "/api/dict/items", '{"dictType":"leave_type","itemValue":"9"}'),
    ("系统设置", 2, "修改系统参数", "PUT", "/api/configs/6", '{"configValue":"15"}'),
    ("系统设置", 2, "分配角色菜单权限", "PUT", "/api/roles/2/menus", '{"menuIds":[100,101,102]}'),
    ("系统设置", 2, "重置用户密码", "PUT", "/api/users/28/password/reset", "{}"),
    ("系统设置", 3, "禁用系统账号", "DELETE", "/api/users/45", "{}"),
]
OPER_USERS = [(e[3], 2 + (e[0] - 1001), e[0]) for e in EMP if e[13] in (1, 2) and e[0] <= 1043]
rows, olg_id = [], 0
for d in [date(2026, 9, 7), date(2026, 9, 8), date(2026, 9, 9), date(2026, 9, 10), date(2026, 9, 11)]:
    for k, (mod, otype, desc, method, url, param) in enumerate(OPER_CATALOG):
        if (k + d.day) % 3 == 0:
            continue
        olg_id += 1
        uname, uid, eid = OPER_USERS[(k + d.day) % len(OPER_USERS)]
        fail = (k % 17 == 5)
        rows.append([N(olg_id), N(uid), q(uname), q(mod), N(otype), q(desc), q(method), q(url),
                     q(param), q('{"code":0,"message":"操作成功"}') if not fail else "NULL",
                     q("192.168.%d.%d" % (10 + (k % 8), 20 + (k * 7) % 200)),
                     N(0 if fail else 1),
                     q("org.springframework.dao.DuplicateKeyException: 唯一约束冲突，请检查数据是否重复") if fail else "NULL",
                     N(35 + (k * 13) % 420), TS(hm(d, 9 + (k % 9), (k * 23) % 60))])
emit("sys_oper_log", ["id", "user_id", "username", "module", "oper_type", "oper_desc",
                      "request_method", "request_url", "request_param", "response_result",
                      "oper_ip", "oper_status", "error_msg", "cost_time", "oper_time"],
     rows, batch=100)

L("SET FOREIGN_KEY_CHECKS = 1;")
L()
L("-- ====================================================================================")
L("--  数据初始化完成。各表写入行数统计（sys_role_menu 见下方说明）：")
L("-- ====================================================================================")
for t in sorted(TABLE_COUNTS.keys()):
    L("--    %-24s %6d 行" % (t, TABLE_COUNTS[t]))
L("--    %-24s %6d 行   （基于 sys_menu 的 SELECT 授权语句生成）" % ("sys_role_menu", 153))
L("-- ------------------------------------------------------------------------------------")
L("--  合计覆盖 49 张表，%d 行业务数据。" % (sum(TABLE_COUNTS.values()) + 153))
L("-- ====================================================================================")

with open(BASE + "/data.sql", "w", encoding="utf-8") as f:
    f.write("\n".join(out) + "\n")

print("data.sql 生成完毕：%d 行 SQL 文本，%d 张表，%d 行数据" % (
    len(out), len([k for k, v in TABLE_COUNTS.items() if v]), sum(TABLE_COUNTS.values())))
for t in sorted(TABLE_COUNTS.keys()):
    print("  %-24s %6d" % (t, TABLE_COUNTS[t]))
