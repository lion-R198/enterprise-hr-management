package com.enterprise.hr.employee.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体（对应表 {@code sys_user}）。
 *
 * @author Enterprise HR Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录账号（唯一） */
    private String username;

    /** 密码（BCrypt 加盐哈希，禁止明文） */
    private String password;

    /** 姓名 */
    @TableField("real_name")
    private String realName;

    /** 关联员工ID（NULL=非员工账号，如外包运维） */
    @TableField("employee_id")
    private Long employeeId;

    /** 所属部门ID（冗余，便于数据范围过滤） */
    @TableField("dept_id")
    private Long deptId;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 头像地址 */
    private String avatar;

    /** 账号状态：1 正常 / 0 停用 / 2 锁定 */
    private Integer status;

    /** 连续登录失败次数 */
    @TableField("login_fail_count")
    private Integer loginFailCount;

    /** 锁定到期时间 */
    @TableField("lock_until")
    private LocalDateTime lockUntil;

    /** 最后登录时间 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    @TableField("last_login_ip")
    private String lastLoginIp;

    /** 备注 */
    private String remark;

    /** 逻辑删除：0 未删除 / 1 已删除 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
