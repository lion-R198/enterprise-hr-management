package com.enterprise.hr.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.employee.entity.EmployeeSalary;

/**
 * 员工薪资明细 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeSalaryMapper extends BaseMapper<EmployeeSalary> {
}
