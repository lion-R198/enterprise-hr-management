package com.enterprise.hr.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.Employee;
import org.apache.ibatis.annotations.Select;

/**
 * 员工 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 * 复杂查询（多表关联、统计报表）再以注解或 XML 方式扩展。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 取全表最大工号序号（工号中 {@code E} 之后的数字部分），无数据时返回 0。
     *
     * <p><b>为什么必须用原生 SQL、而不能用 {@code BaseMapper.selectObjs}：</b>
     * {@code employee} 带 {@link com.baomidou.mybatisplus.annotation.TableLogic}，
     * MyBatis-Plus 会给所有由它生成的查询自动追加 {@code deleted = 0}。而数据库里
     * {@code uk_emp_no} 是<b>物理</b>唯一键，不随逻辑删除让位——于是「已逻辑删除」的行
     * 依然占着工号，却对取号器不可见，下一个自动工号必然撞唯一键（表现为入职办理 409）。
     *
     * <p>这条自定义 SQL 不经过 MyBatis-Plus 的逻辑删除注入，因此能看到全表（含已删除行），
     * 保证自动取号真正跳过被占用的工号。
     *
     * @return 最大工号序号；表空或无数字工号时为 0
     */
    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(emp_no, 2) AS UNSIGNED)), 0) "
            + "FROM employee WHERE emp_no IS NOT NULL")
    long selectMaxEmpNoSeq();
}
