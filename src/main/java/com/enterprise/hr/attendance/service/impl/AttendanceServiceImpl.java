package com.enterprise.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.enterprise.hr.attendance.dto.AttendanceRecordQuery;
import com.enterprise.hr.attendance.dto.AttendanceRecordVO;
import com.enterprise.hr.attendance.dto.CheckinResultVO;
import com.enterprise.hr.attendance.dto.CheckoutResultVO;
import com.enterprise.hr.attendance.dto.ShiftVO;
import com.enterprise.hr.attendance.dto.TodayAttendanceVO;
import com.enterprise.hr.attendance.entity.AttendanceRecord;
import com.enterprise.hr.attendance.mapper.AttendanceRecordMapper;
import com.enterprise.hr.attendance.service.AttendanceService;
import com.enterprise.hr.attendance.service.support.AttendanceEmployeeSupport;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.config.EhrProperties;
import com.enterprise.hr.employee.entity.Shift;
import com.enterprise.hr.employee.mapper.ShiftMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤打卡业务实现（F-04-01）。
 *
 * <p>打卡判定的时间基准统一取配置 {@code ehr.business.*}，不写死在代码里：
 * <ul>
 *     <li>迟到基准 = {@code workStartTime} + {@code lateThresholdMinute}（默认 09:00 + 5 分 = 09:05）</li>
 *     <li>早退基准 = {@code workEndTime} - 班次 {@code early_tolerance}（默认 18:00 - 5 分 = 17:55）</li>
 * </ul>
 * 阈值一旦调整（如弹性上班改为 09:30），只需改配置，无需改代码。
 *
 * @author Enterprise HR Team
 */
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord>
        implements AttendanceService {

    /** 严重迟到门槛（分钟）：超过该值记 check_in_status = 3 */
    private static final int SEVERE_LATE_MINUTES = 60;
    /** 午休扣除时长（小时），与班次 rest_minutes 的默认值一致 */
    private static final BigDecimal LUNCH_BREAK_HOURS = BigDecimal.ONE;
    /** 未配置班次时的默认下班时间 */
    private static final LocalTime DEFAULT_WORK_END = LocalTime.of(18, 0);
    /** 未配置班次时的默认早退容忍（分钟） */
    private static final int DEFAULT_EARLY_TOLERANCE = 5;

    private final ShiftMapper shiftMapper;
    private final EhrProperties ehrProperties;
    private final AttendanceEmployeeSupport employeeInfoSupport;

    @Override
    public TodayAttendanceVO today() {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        AttendanceRecord record = findTodayRecord(employeeId);
        Shift shift = resolveShift();

        AttendanceRecordVO recordVO = null;
        if (record != null) {
            recordVO = toVO(record);
            // 打卡卡片只展示本人数据，但仍统一补展示字段，避免同一 VO 出现两种形态
            employeeInfoSupport.fillEmployeeFields(List.of(recordVO));
        }
        return new TodayAttendanceVO(recordVO, toShiftVO(shift));
    }

    /**
     * 上班打卡。
     *
     * <p>幂等保护：当日已存在 {@code first_check_in} 直接拒绝，
     * 防止重复点击把首次打卡时间覆盖成更晚的时间（从而虚增迟到分钟）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinResultVO checkin() {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);
        LocalDate today = LocalDate.now();

        AttendanceRecord record = findTodayRecord(employeeId);
        if (record != null && record.getFirstCheckIn() != null) {
            throw new BizException(ErrorCode.ATTENDANCE_DUPLICATED);
        }

        LocalDateTime now = LocalDateTime.now();
        int lateMinutes = calcLateMinutes(now.toLocalTime());
        int checkInStatus = lateMinutes > SEVERE_LATE_MINUTES ? BizConst.CHECK_SEVERE
                : lateMinutes > 0 ? BizConst.CHECK_LATE : BizConst.CHECK_NORMAL;
        int attendanceStatus = lateMinutes > 0 ? BizConst.ATTEND_LATE : BizConst.ATTEND_NORMAL;

        if (record == null) {
            record = new AttendanceRecord();
            record.setEmployeeId(employeeId);
            record.setAttendanceDate(today);
            Shift shift = resolveShift();
            record.setShiftId(shift == null ? null : shift.getId());
            record.setWorkHours(BigDecimal.ZERO);
            record.setEarlyMinutes(0);
            // 尚未下班打卡，先记"缺卡"，下班打卡时再改写为正常/早退
            record.setCheckOutStatus(BizConst.CHECK_SEVERE);
            record.setSource(BizConst.SOURCE_MOBILE);
            record.setRemark("");
            fillCheckIn(record, now, lateMinutes, checkInStatus, attendanceStatus);
            baseMapper.insert(record);
        } else {
            fillCheckIn(record, now, lateMinutes, checkInStatus, attendanceStatus);
            baseMapper.updateById(record);
        }
        return new CheckinResultVO(Boolean.TRUE, now);
    }

    /**
     * 下班打卡。
     *
     * <p>工时口径：{@code 下班 - 上班 - 午休 1 小时}，且不小于 0
     * （异常数据如补卡导致的负时长不应污染月报统计）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultVO checkout() {
        LoginUser user = UserContext.require();
        Long employeeId = requireEmployeeId(user);

        AttendanceRecord record = findTodayRecord(employeeId);
        if (record == null || record.getFirstCheckIn() == null) {
            throw new BizException(ErrorCode.CHECKIN_REQUIRED);
        }
        if (record.getLastCheckOut() != null) {
            throw new BizException(ErrorCode.CHECKOUT_DUPLICATED);
        }

        LocalDateTime now = LocalDateTime.now();
        record.setLastCheckOut(now);
        record.setCheckOutStatus(BizConst.CHECK_NORMAL);
        record.setEarlyMinutes(0);

        BigDecimal hours = BigDecimal.valueOf(Duration.between(record.getFirstCheckIn(), now).getSeconds())
                .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP)
                .subtract(LUNCH_BREAK_HOURS);
        if (hours.signum() < 0) {
            hours = BigDecimal.ZERO;
        }
        // 落库列精度为 decimal(5,1)，此处先按列精度收敛，避免 MySQL 静默四舍五入造成"返回值和落库值不一致"
        record.setWorkHours(hours.setScale(1, RoundingMode.HALF_UP));

        LocalTime earlyBaseline = resolveEarlyBaseline();
        if (now.toLocalTime().isBefore(earlyBaseline)) {
            record.setCheckOutStatus(BizConst.CHECK_LATE);
            record.setEarlyMinutes((int) Duration.between(now.toLocalTime(), earlyBaseline).toMinutes());
        }

        baseMapper.updateById(record);
        return new CheckoutResultVO(Boolean.TRUE, now);
    }

    @Override
    public PageResult<AttendanceRecordVO> pageRecords(AttendanceRecordQuery query) {
        LoginUser user = UserContext.require();
        LambdaQueryWrapper<AttendanceRecord> wrapper = Wrappers.lambdaQuery();

        // 行级数据范围（NF-10）：员工仅本人、主管本部门、人事/财务/管理层不受限
        List<Long> scope = employeeInfoSupport.resolveEmployeeScope(user);
        if (scope != null) {
            if (scope.isEmpty()) {
                return PageResult.empty(query.safeCurrent(), query.safeSize());
            }
            wrapper.in(AttendanceRecord::getEmployeeId, scope);
        }
        if (query.getEmployeeId() != null) {
            wrapper.eq(AttendanceRecord::getEmployeeId, query.getEmployeeId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, query.getEndDate());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AttendanceRecord::getAttendanceStatus, query.getStatus());
        }
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate).orderByDesc(AttendanceRecord::getId);

        Page<AttendanceRecord> page = baseMapper.selectPage(query.toPage(), wrapper);
        List<AttendanceRecordVO> records = new ArrayList<>(page.getRecords().size());
        for (AttendanceRecord record : page.getRecords()) {
            records.add(toVO(record));
        }
        employeeInfoSupport.fillEmployeeFields(records);
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    // ---------------- 内部辅助 ----------------

    private AttendanceRecord findTodayRecord(Long employeeId) {
        List<AttendanceRecord> list = baseMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getEmployeeId, employeeId)
                .eq(AttendanceRecord::getAttendanceDate, LocalDate.now())
                .orderByAsc(AttendanceRecord::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 取一个启用班次（按 sort_no 升序，与前端"班次：标准班"展示一致） */
    private Shift resolveShift() {
        List<Shift> shifts = shiftMapper.selectList(Wrappers.<Shift>lambdaQuery()
                .eq(Shift::getStatus, BizConst.STATUS_ENABLED)
                .orderByAsc(Shift::getSortNo)
                .orderByAsc(Shift::getId)
                .last("LIMIT 1"));
        return shifts.isEmpty() ? null : shifts.get(0);
    }

    /**
     * 迟到分钟数：以"上班时间 + 迟到宽限"为基准，未超过基准记 0。
     */
    private int calcLateMinutes(LocalTime now) {
        LocalTime start = parseTime(ehrProperties.getBusiness().getWorkStartTime(), LocalTime.of(9, 0));
        LocalTime baseline = start.plusMinutes(ehrProperties.getBusiness().getLateThresholdMinute());
        if (!now.isAfter(baseline)) {
            return 0;
        }
        return (int) Duration.between(baseline, now).toMinutes();
    }

    /** 早退判定基准：下班时间 - 班次早退容忍分钟数 */
    private LocalTime resolveEarlyBaseline() {
        LocalTime end = parseTime(ehrProperties.getBusiness().getWorkEndTime(), DEFAULT_WORK_END);
        Shift shift = resolveShift();
        int tolerance = (shift == null || shift.getEarlyTolerance() == null)
                ? DEFAULT_EARLY_TOLERANCE : shift.getEarlyTolerance();
        return end.minusMinutes(tolerance);
    }

    private static LocalTime parseTime(String raw, LocalTime defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    private static void fillCheckIn(AttendanceRecord record, LocalDateTime now, int lateMinutes,
                                    int checkInStatus, int attendanceStatus) {
        record.setFirstCheckIn(now);
        record.setLateMinutes(lateMinutes);
        record.setCheckInStatus(checkInStatus);
        record.setAttendanceStatus(attendanceStatus);
    }

    private static Long requireEmployeeId(LoginUser user) {
        Long employeeId = user.getEmployeeId();
        if (employeeId == null) {
            throw BizException.badRequest("当前登录账号未关联员工档案，无法打卡");
        }
        return employeeId;
    }

    private static AttendanceRecordVO toVO(AttendanceRecord record) {
        AttendanceRecordVO vo = new AttendanceRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    private static ShiftVO toShiftVO(Shift shift) {
        if (shift == null) {
            return null;
        }
        ShiftVO vo = new ShiftVO();
        BeanUtils.copyProperties(shift, vo);
        return vo;
    }
}
