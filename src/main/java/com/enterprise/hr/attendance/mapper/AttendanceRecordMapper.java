package com.enterprise.hr.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.enterprise.hr.attendance.entity.AttendanceRecord;

/**
 * 考勤打卡记录表 Mapper。
 *
 * <p>基础 CRUD 全部由 {@link BaseMapper} 提供；启动类上的
 * {@code @MapperScan("com.enterprise.hr.**.mapper")} 负责注册。
 * 复杂查询（多表关联、统计报表）再以注解或 XML 方式扩展。
 *
 * @author Enterprise HR Team
 */
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {
}
