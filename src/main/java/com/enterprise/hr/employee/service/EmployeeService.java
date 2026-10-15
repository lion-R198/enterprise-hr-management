package com.enterprise.hr.employee.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeQuery;
import com.enterprise.hr.employee.dto.EmployeeStatusReq;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.entity.Employee;

import java.util.List;

/**
 * 员工业务接口。
 *
 * <p>继承 {@link IService} 复用通用 CRUD（{@code getById} / {@code list} / {@code count} ...），
 * 再补充带业务规则的方法。
 *
 * <p>两种方法风格并存（历史原因，勿删旧方法）：
 * <ul>
 *     <li><b>实体进出</b>：{@code create / update / delete / getByEmpNo / pageQuery(long,long,String)}
 *         —— 早期契约，保留给内部调用与既有测试</li>
 *     <li><b>VO 进出</b>：{@code pageQuery(EmployeeQuery) / getDetail / changeStatus / listChanges}
 *         —— 新契约，直接产出符合前端字段（蛇形 + 脱敏 + 关联字段）的 VO</li>
 * </ul>
 * 关联字段（{@code dept_name} / {@code manager_name}）由本 Service 内部注入 Mapper 批量补齐，
 * 因此 Controller 只需注入本接口即可完成全部对外输出。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeService extends IService<Employee> {

    /**
     * 新增员工（入职办理）。
     *
     * <p>BR-02-01：工号由系统生成，{@code req.empNo} 为空时自动取
     * "现有最大工号序号 + 1"；BR-02-02：薪资不得为负。
     * 初始状态为「待入职」，并同步写入一条入职异动记录（可追溯）。
     *
     * @param req 新增入参
     * @return 插入后的实体（已回填主键与工号）
     */
    Employee create(EmployeeCreateReq req);

    /**
     * 修改员工（局部更新，未传字段保持原值）。
     *
     * @param id  员工 ID
     * @param req 修改入参
     * @return 更新后的实体
     */
    Employee update(Long id, EmployeeUpdateReq req);

    /**
     * 删除员工：<b>BR-02-03 逻辑删除</b>，仅置 {@code status=0} + {@code leave_date=今天}。
     *
     * <p>不做物理删除：员工的考勤、薪资、绩效、合同等历史数据都要保留可追溯。
     *
     * @param id 员工 ID
     */
    void delete(Long id);

    /**
     * 批量删除员工（同样按 BR-02-03 逻辑删除语义：置离职状态 + 离职日期）。
     *
     * @param ids 员工 ID 列表
     */
    void deleteBatch(List<Long> ids);

    /**
     * 按工号查询员工（工号唯一）。
     *
     * @param empNo 工号
     * @return 员工实体
     */
    Employee getByEmpNo(String empNo);

    /**
     * 分页查询，支持按姓名/工号关键字模糊匹配（旧契约，返回实体分页）。
     *
     * @param current 当前页，小于 1 时按 1 处理
     * @param size    每页条数，小于等于 0 时按 10 处理
     * @param keyword 关键字，可为空
     * @return 分页结果
     */
    IPage<Employee> pageQuery(long current, long size, String keyword);

    /**
     * 员工分页查询（新契约 F-02-06）。
     *
     * <p>入参 {@code current/size/keyword/deptId/positionId/status}，
     * 关键字匹配姓名 / 工号 / 手机号，按工号升序；
     * <b>NF-10 行级数据范围</b>：HR/ADMIN/FINANCE/EXECUTIVE 全量、主管本部门及下级、员工仅本人。
     * 返回 VO 已补 {@code dept_name}，并对身份证、银行卡脱敏。
     */
    PageResult<EmployeeVO> pageQuery(EmployeeQuery query);

    /**
     * 员工详情：补 {@code dept_name} / {@code manager_name}，敏感字段脱敏，并做 NF-10 行级校验。
     *
     * @param id 员工 ID
     * @throws com.enterprise.hr.common.BizException 员工不存在（404）或越权查看他人档案（403）
     */
    EmployeeVO getDetail(Long id);

    /**
     * 员工状态变更（离职 / 转正 / 试用等）。
     *
     * <p>BR-02-04：状态变更必须留痕 —— 同步写入 {@code employee_change}
     * （to=0 离职 → change_type=7；to=1 转正 → change_type=2；其它 → 4），
     * 离职时写 {@code leave_date} / {@code leave_reason}，
     * 转正时若 {@code regular_date} 为空则写入当天。
     *
     * @param id  员工 ID
     * @param req 目标状态与原因
     */
    void changeStatus(Long id, EmployeeStatusReq req);

    /**
     * 某员工的异动记录（按生效日期倒序），补员工姓名与异动前后部门名。
     *
     * @param id 员工 ID
     */
    List<EmployeeChangeVO> listChanges(Long id);

    /** 实体 → VO（补 {@code dept_name} / {@code manager_name}，脱敏） */
    EmployeeVO toVO(Employee entity);

    /** 实体列表 → VO 列表（关联字段批量补齐，不做 N+1 查询） */
    List<EmployeeVO> toVOList(List<Employee> entities);
}
