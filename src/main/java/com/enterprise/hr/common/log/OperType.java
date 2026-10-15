package com.enterprise.hr.common.log;

/**
 * 操作类型：与 {@code sys_oper_log.oper_type} 取值对应，用于日志检索与合规审计筛选。
 *
 * @author Enterprise HR Team
 */
public enum OperType {

    /** 查询 */
    QUERY("查询", 4),
    /** 新增 */
    INSERT("新增", 1),
    /** 修改 */
    UPDATE("修改", 2),
    /** 删除 */
    DELETE("删除", 3),
    /** 审批 */
    APPROVE("审批", 7),
    /** 导入 */
    IMPORT("导入", 6),
    /** 导出 */
    EXPORT("导出", 5),
    /** 授权 */
    GRANT("授权", 7),
    /** 登录 */
    LOGIN("登录", 7),
    /** 登出 */
    LOGOUT("登出", 7),
    /** 其它 */
    OTHER("其它", 7);

    private final String label;
    /** 落库编码，与 sys_oper_log.oper_type 注释一致：1新增 2修改 3删除 4查询 5导出 6导入 7其他 */
    private final int dbCode;

    OperType(String label, int dbCode) {
        this.label = label;
        this.dbCode = dbCode;
    }

    public String getLabel() {
        return label;
    }

    public int getDbCode() {
        return dbCode;
    }
}
