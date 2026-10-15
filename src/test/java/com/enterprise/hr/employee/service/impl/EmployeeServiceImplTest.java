package com.enterprise.hr.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.common.constant.RoleCode;
import com.enterprise.hr.common.security.LoginUser;
import com.enterprise.hr.common.security.UserContext;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeQuery;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.entity.EmployeeChange;
import com.enterprise.hr.employee.entity.Position;
import com.enterprise.hr.employee.mapper.EmployeeChangeMapper;
import com.enterprise.hr.employee.mapper.EmployeeMapper;
import com.enterprise.hr.employee.mapper.PositionMapper;
import com.enterprise.hr.employee.service.support.DeptScopeSupport;
import com.enterprise.hr.employee.service.support.EmployeeInfoSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工 Service 单元测试：用 Mockito 隔离 Mapper / 数据库，只验证业务规则。
 *
 * <p>改造后必须守住的语义（其余 Mapper 交互细节由集成测试覆盖）：
 * <ul>
 *     <li><b>BR-02-01</b>：工号由系统生成（"现有最大工号序号 + 1"），调用方显式传入时仍须全局唯一；
 *         修改接口不得改变工号</li>
 *     <li><b>BR-02-02</b>：薪资不得为负，抛 {@link BizException}（{@link ErrorCode#NEGATIVE_SALARY}）</li>
 *     <li><b>BR-02-03</b>：删除是"置离职状态 + 离职日期"，<b>不是</b>物理删除，也不走
 *         {@code deleteById} 的 {@code @TableLogic} 通道（历史数据必须保留可追溯）</li>
 *     <li><b>新入职默认状态</b>：{@link BizConst#EMPLOYEE_PENDING}（待入职），转正须走状态变更接口以留痕</li>
 *     <li><b>NF-10 行级数据范围</b>：分页查询必须按角色拼装 WHERE，且"账号无关联员工"时
 *         返回空集而不是全量</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
class EmployeeServiceImplTest {

    private EmployeeMapper mapper;
    private PositionMapper positionMapper;
    private EmployeeChangeMapper changeMapper;
    private EmployeeInfoSupport employeeInfoSupport;
    private DeptScopeSupport deptScopeSupport;
    private EmployeeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mapper = mock(EmployeeMapper.class);
        positionMapper = mock(PositionMapper.class);
        changeMapper = mock(EmployeeChangeMapper.class);
        employeeInfoSupport = mock(EmployeeInfoSupport.class);
        deptScopeSupport = mock(DeptScopeSupport.class);

        service = new EmployeeServiceImpl();
        // baseMapper 在父类 ServiceImpl 上；其余为字段注入的协作方
        inject(service, "baseMapper", mapper);
        inject(service, "positionMapper", positionMapper);
        inject(service, "employeeChangeMapper", changeMapper);
        inject(service, "employeeInfoSupport", employeeInfoSupport);
        inject(service, "deptScopeSupport", deptScopeSupport);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ------------------------------- create：参数校验 -------------------------------

    @Test
    @DisplayName("create：缺少姓名 → 参数校验异常，且不落库")
    void create_name_required() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setDeptId(1L);
        req.setPositionId(1L);

        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals(R.CODE_BAD_REQUEST, ex.getCode(), "参数缺失必须归入 400");
        verify(mapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("create：缺少所属部门 → 参数校验异常（deptId 是新增必填项）")
    void create_deptId_required() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("张三");
        req.setPositionId(1L);

        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals(R.CODE_BAD_REQUEST, ex.getCode(), "参数缺失必须归入 400");
        verify(mapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("create：缺少所属岗位 → 参数校验异常（positionId 是新增必填项）")
    void create_positionId_required() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("张三");
        req.setDeptId(1L);

        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals(R.CODE_BAD_REQUEST, ex.getCode(), "参数缺失必须归入 400");
        verify(mapper, never()).insert(any(Employee.class));
    }

    // ------------------------------- create：BR-02-01 / BR-02-02 -------------------------------

    @Test
    @DisplayName("BR-02-01 create：不传 empNo 时工号自动生成（最大序号 + 1），默认状态待入职")
    void create_auto_generates_emp_no_and_pending_status() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("测试员");
        req.setDeptId(2L);
        req.setPositionId(3L);
        req.setSalary(new BigDecimal("10000.00"));

        // 库里现有最大工号序号为 3 → 下一位是 E0004
        // 取号走 EmployeeMapper.selectMaxEmpNoSeq()：原生 SQL，不受 @TableLogic 的 deleted=0 影响
        when(mapper.selectMaxEmpNoSeq()).thenReturn(3L);
        when(positionMapper.selectById(3L)).thenReturn(position("产品经理"));
        when(mapper.insert(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            assertEquals("E0004", e.getEmpNo(), "工号由服务端按最大序号 +1 生成");
            e.setId(99L); // 模拟自增主键回填
            return 1;
        });

        Employee result = assertDoesNotThrow(() -> service.create(req));
        assertEquals("E0004", result.getEmpNo());
        assertEquals("测试员", result.getName());
        assertEquals(Long.valueOf(99L), result.getId(), "自增主键回填");
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_PENDING), result.getStatus(),
                "新入职默认「待入职」= " + BizConst.EMPLOYEE_PENDING);
        assertEquals(Integer.valueOf(1), result.getEmployeeType(), "用工性质缺省为正式");
        assertNotNull(result.getHireDate(), "入职日期缺省为当天");
        assertEquals("产品经理", result.getPosition(), "职位名按 positionId 取快照");

        verify(mapper, times(1)).insert(any(Employee.class));
        // BR-02-01：入职即产生一条异动留痕，履历从第一天起可追溯
        ArgumentCaptor<EmployeeChange> changeCaptor = ArgumentCaptor.forClass(EmployeeChange.class);
        verify(changeMapper, times(1)).insert(changeCaptor.capture());
        assertEquals(Integer.valueOf(1), changeCaptor.getValue().getChangeType(), "入职异动类型 = 1");
        assertEquals(Long.valueOf(99L), changeCaptor.getValue().getEmployeeId());
    }

    @Test
    @DisplayName("BR-02-01 create：显式传入的工号已存在 → BizException(EMP_NO_DUPLICATED)，不落库")
    void create_explicit_emp_no_duplicated() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setEmpNo("E0001");
        req.setName("张三");
        req.setDeptId(1L);
        req.setPositionId(1L);

        when(mapper.selectCount(any())).thenReturn(1L);

        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals(ErrorCode.EMP_NO_DUPLICATED.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("工号已存在"));
        verify(mapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("BR-02-02 create：薪资为负 → BizException(NEGATIVE_SALARY)，不落库")
    void create_negative_salary_rejected() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setName("张三");
        req.setDeptId(1L);
        req.setPositionId(1L);
        req.setSalary(new BigDecimal("-0.01"));

        BizException ex = assertThrows(BizException.class, () -> service.create(req));
        assertEquals(ErrorCode.NEGATIVE_SALARY.getCode(), ex.getCode());
        assertEquals(400, ex.getCode(), "负薪资必须映射为 400 业务校验失败");
        verify(mapper, never()).insert(any(Employee.class));
    }

    @Test
    @DisplayName("create：DTO 字段正确转换到 Entity（含工号/部门/岗位/岗位名快照）")
    void create_dto_to_entity_conversion() {
        EmployeeCreateReq req = new EmployeeCreateReq();
        req.setEmpNo("E9002");
        req.setName("测试员2");
        req.setGender("F");
        req.setAge(28);
        req.setEmail("t@x.com");
        req.setPhone("13900000001");
        req.setDeptId(2L);
        req.setPositionId(5L);
        req.setSalary(new BigDecimal("8888.88"));

        when(mapper.selectCount(any())).thenReturn(0L);
        when(positionMapper.selectById(5L)).thenReturn(position("前端开发工程师"));
        when(mapper.insert(any(Employee.class))).thenAnswer(inv -> {
            Employee arg = inv.getArgument(0);
            assertEquals("E9002", arg.getEmpNo());
            assertEquals("测试员2", arg.getName());
            assertEquals("F", arg.getGender());
            assertEquals(Integer.valueOf(28), arg.getAge());
            assertEquals("t@x.com", arg.getEmail());
            assertEquals("13900000001", arg.getPhone());
            assertEquals(Long.valueOf(2L), arg.getDeptId());
            assertEquals(Long.valueOf(5L), arg.getPositionId());
            assertEquals("前端开发工程师", arg.getPosition(), "职位名以岗位主数据为准");
            assertEquals(0, new BigDecimal("8888.88").compareTo(arg.getSalary()));
            arg.setId(101L);
            return 1;
        });

        Employee e = service.create(req);
        assertEquals(Long.valueOf(101L), e.getId());
    }

    // ------------------------------- update -------------------------------

    @Test
    @DisplayName("update：id 不存在 → 参数校验异常，不执行更新")
    void update_id_not_found() {
        when(mapper.selectById(any())).thenReturn(null);
        EmployeeUpdateReq patch = new EmployeeUpdateReq();
        patch.setName("新名");

        BizException ex = assertThrows(BizException.class, () -> service.update(123L, patch));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND.getCode(), ex.getCode(), "资源不存在必须归入 404");
        verify(mapper, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("BR-02-01 update：试图改工号 → 拒绝并说明原因，不执行更新")
    void update_emp_no_immutable() {
        Employee existed = new Employee();
        existed.setId(1L);
        existed.setEmpNo("E0001");
        when(mapper.selectById(1L)).thenReturn(existed);

        EmployeeUpdateReq patch = new EmployeeUpdateReq();
        patch.setEmpNo("E9999");
        patch.setName("新名");

        BizException ex = assertThrows(BizException.class, () -> service.update(1L, patch));
        assertEquals(R.CODE_BAD_REQUEST, ex.getCode(), "工号不可修改属于业务校验 400");
        assertTrue(ex.getMessage().contains("工号不可修改"));
        verify(mapper, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("BR-02-02 update：薪资为负 → BizException(NEGATIVE_SALARY)")
    void update_negative_salary_rejected() {
        Employee existed = new Employee();
        existed.setId(1L);
        existed.setEmpNo("E0001");
        when(mapper.selectById(1L)).thenReturn(existed);

        EmployeeUpdateReq patch = new EmployeeUpdateReq();
        patch.setSalary(new BigDecimal("-1.00"));

        BizException ex = assertThrows(BizException.class, () -> service.update(1L, patch));
        assertEquals(ErrorCode.NEGATIVE_SALARY.getCode(), ex.getCode());
        verify(mapper, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("update：只覆盖非空字段，null 字段不会清空数据库原值")
    void update_partial_protects_existing_fields() {
        Employee existed = new Employee();
        existed.setId(1L);
        existed.setEmpNo("E0001");
        existed.setName("原名");
        existed.setEmail("orig@x.com");
        existed.setSalary(new BigDecimal("9000.00"));

        when(mapper.selectById(1L)).thenReturn(existed);
        when(mapper.updateById(any(Employee.class))).thenReturn(1);

        EmployeeUpdateReq patch = new EmployeeUpdateReq();
        patch.setName("新名"); // email / salary 故意不传

        Employee updated = service.update(1L, patch);

        assertEquals("新名", updated.getName());
        assertEquals("orig@x.com", updated.getEmail(), "email 缺失时不被清空");
        assertEquals(0, new BigDecimal("9000.00").compareTo(updated.getSalary()),
                "salary 缺失时不被清空");
    }

    @Test
    @DisplayName("update：改岗位时同步刷新 position 冗余快照，避免与岗位主数据长期不一致")
    void update_positionId_refreshes_snapshot() {
        Employee existed = new Employee();
        existed.setId(1L);
        existed.setEmpNo("E0001");
        existed.setPosition("旧职位");
        when(mapper.selectById(1L)).thenReturn(existed);
        when(positionMapper.selectById(7L)).thenReturn(position("架构师"));
        when(mapper.updateById(any(Employee.class))).thenReturn(1);

        EmployeeUpdateReq patch = new EmployeeUpdateReq();
        patch.setPositionId(7L);

        Employee updated = service.update(1L, patch);
        assertEquals(Long.valueOf(7L), updated.getPositionId());
        assertEquals("架构师", updated.getPosition());
    }

    // ------------------------------- delete（BR-02-03） -------------------------------

    @Test
    @DisplayName("delete：id 为 null → 参数校验异常")
    void delete_id_required() {
        BizException ex = assertThrows(BizException.class, () -> service.delete(null));
        assertEquals(R.CODE_BAD_REQUEST, ex.getCode(), "参数缺失必须归入 400");
    }

    @Test
    @DisplayName("delete：员工不存在 → 参数校验异常，不执行更新")
    void delete_not_found() {
        when(mapper.selectById(9L)).thenReturn(null);
        BizException ex = assertThrows(BizException.class, () -> service.delete(9L));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND.getCode(), ex.getCode(), "资源不存在必须归入 404");
        verify(mapper, never()).updateById(any(Employee.class));
    }

    @Test
    @DisplayName("BR-02-03 delete：置离职状态 + 离职日期，行仍保留（不是物理/逻辑删除）")
    void delete_normal_path_resigns_instead_of_removing() {
        Employee existed = new Employee();
        existed.setId(1L);
        existed.setEmpNo("E0001");
        existed.setStatus(BizConst.EMPLOYEE_ACTIVE);
        when(mapper.selectById(1L)).thenReturn(existed);
        when(mapper.updateById(any(Employee.class))).thenReturn(1);

        assertDoesNotThrow(() -> service.delete(1L));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(mapper, times(1)).updateById(captor.capture());
        Employee saved = captor.getValue();
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_RESIGNED), saved.getStatus(), "置为离职");
        assertEquals(LocalDate.now(), saved.getLeaveDate(), "写入离职日期");
        // 历史考勤/薪资/绩效/合同要保留可追溯 → 绝不能走删除通道
        verify(mapper, never()).deleteById(any());
        verify(mapper, never()).delete(any());
    }

    @Test
    @DisplayName("deleteBatch：空列表 → 参数校验异常，不触碰数据库")
    void deleteBatch_empty_ids() {
        BizException empty = assertThrows(BizException.class, () -> service.deleteBatch(Collections.emptyList()));
        assertEquals(R.CODE_BAD_REQUEST, empty.getCode(), "参数缺失必须归入 400");
        assertThrows(BizException.class, () -> service.deleteBatch(null));
        verify(mapper, never()).update(any(Employee.class), any());
    }

    @Test
    @DisplayName("deleteBatch：批量置离职（一条 UPDATE ... WHERE id IN (...)），不走 deleteBatchIds")
    void deleteBatch_normal_path() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(mapper.update(any(Employee.class), any())).thenReturn(3);

        assertDoesNotThrow(() -> service.deleteBatch(ids));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(mapper, times(1)).update(captor.capture(), any());
        assertEquals(Integer.valueOf(BizConst.EMPLOYEE_RESIGNED), captor.getValue().getStatus());
        assertEquals(LocalDate.now(), captor.getValue().getLeaveDate());
        verify(mapper, never()).deleteBatchIds(any());
    }

    @Test
    @DisplayName("deleteBatch：一条都没更新到 → IllegalStateException（服务端处理未达预期）")
    void deleteBatch_no_rows_updated() {
        when(mapper.update(any(Employee.class), any())).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.deleteBatch(List.of(404L)));
    }

    // ------------------------------- getByEmpNo -------------------------------

    @Test
    @DisplayName("getByEmpNo：空/空白工号 → 参数校验异常")
    void getByEmpNo_blank() {
        BizException empty = assertThrows(BizException.class, () -> service.getByEmpNo(""));
        assertEquals(R.CODE_BAD_REQUEST, empty.getCode(), "参数缺失必须归入 400");
        assertThrows(BizException.class, () -> service.getByEmpNo(null));
        assertThrows(BizException.class, () -> service.getByEmpNo("   "));
    }

    @Test
    @DisplayName("getByEmpNo：未命中 → 参数校验异常")
    void getByEmpNo_not_found() {
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        BizException ex = assertThrows(BizException.class, () -> service.getByEmpNo("ghost"));
        assertEquals(ErrorCode.EMPLOYEE_NOT_FOUND.getCode(), ex.getCode(), "资源不存在必须归入 404");
    }

    @Test
    @DisplayName("getByEmpNo：命中应返回查询结果（工号两端空白会被裁剪）")
    void getByEmpNo_hit() {
        Employee e = new Employee();
        e.setId(1L);
        e.setEmpNo("E0001");
        e.setName("张三");
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(e));
        assertEquals("张三", service.getByEmpNo(" E0001 ").getName());
    }

    // ------------------------------- pageQuery（旧签名，兼容保留） -------------------------------

    @Test
    @DisplayName("pageQuery(旧签名)：参数兜底 current<1 → 1，size<=0 → 10")
    void pageQuery_param_fallback() {
        Page<Employee> echoed = new Page<>();
        when(mapper.selectPage(any(), any())).thenAnswer(inv -> {
            Page<Employee> p = inv.getArgument(0);
            echoed.setCurrent(p.getCurrent());
            echoed.setSize(p.getSize());
            return echoed;
        });

        IPage<Employee> page = service.pageQuery(0, 0, null);
        assertEquals(1, page.getCurrent());
        assertEquals(10, page.getSize());
    }

    @Test
    @DisplayName("pageQuery(旧签名)：带关键字时保留分页参数")
    void pageQuery_with_keyword() {
        Page<Employee> echoed = new Page<>(2, 5);
        when(mapper.selectPage(any(), any())).thenAnswer(inv -> {
            Page<Employee> p = inv.getArgument(0);
            echoed.setCurrent(p.getCurrent());
            echoed.setSize(p.getSize());
            return echoed;
        });

        IPage<Employee> page = assertDoesNotThrow(() -> service.pageQuery(2, 5, "张"));
        assertEquals(2, page.getCurrent());
        assertEquals(5, page.getSize());
    }

    // ------------------------------- pageQuery（新签名 + NF-10） -------------------------------

    @Test
    @DisplayName("pageQuery(新签名)：返回 PageResult 四字段，关键字参与 WHERE，HR 无行级过滤")
    void pageQuery_new_signature_hr_unlimited() {
        UserContext.set(loginUser(RoleCode.HR, 2001L, 1L));
        stubSelectPage(oneEmployee());

        EmployeeQuery query = new EmployeeQuery();
        query.setKeyword("张");
        PageResult<EmployeeVO> result = service.pageQuery(query);

        assertEquals(1, result.getRecords().size());
        assertEquals(Long.valueOf(1L), result.getTotal());
        assertEquals(Long.valueOf(1L), result.getCurrent());
        assertEquals(Long.valueOf(10L), result.getSize());

        String sql = capturedWrapperSql();
        assertTrue(sql.contains("name"), "关键字应参与 WHERE 条件: " + sql);
        assertFalse(sql.contains("dept_id"), "HR 不应带部门级行过滤: " + sql);
        assertFalse(sql.contains("1 = 0"), "HR 不应被限制为不可见: " + sql);
    }

    @Test
    @DisplayName("NF-10 pageQuery：仅本人角色只可见自己（按 id 过滤，不按部门）")
    void pageQuery_self_only_filters_by_own_employee_id() {
        UserContext.set(loginUser(RoleCode.EMPLOYEE, 1001L, 1L));
        stubSelectPage(oneEmployee());

        service.pageQuery(new EmployeeQuery());

        String sql = capturedWrapperSql();
        assertTrue(sql.contains("id"), "应按下发员工 ID 过滤: " + sql);
        assertFalse(sql.contains("dept_id"), "员工角色不应看到同部门他人: " + sql);
    }

    @Test
    @DisplayName("NF-10 pageQuery：账号未关联员工档案时可见范围为空（1=0），而不是全量")
    void pageQuery_self_only_without_employee_profile_sees_nothing() {
        UserContext.set(loginUser(RoleCode.EMPLOYEE, null, 1L));
        stubSelectPage(oneEmployee());

        service.pageQuery(new EmployeeQuery());

        assertTrue(capturedWrapperSql().contains("1 = 0"),
                "未绑定员工档案必须是空集，否则等于越权看到全量: " + capturedWrapperSql());
    }

    @Test
    @DisplayName("NF-10 pageQuery：部门主管按「本部门及下级」过滤")
    void pageQuery_manager_filters_by_dept_subtree() {
        UserContext.set(loginUser(RoleCode.MANAGER, 3001L, 7L));
        when(deptScopeSupport.deptAndDescendantIds(7L)).thenReturn(List.of(7L, 8L, 9L));
        stubSelectPage(oneEmployee());

        service.pageQuery(new EmployeeQuery());

        assertTrue(capturedWrapperSql().contains("dept_id"),
                "主管必须按部门子树过滤: " + capturedWrapperSql());
    }

    @Test
    @DisplayName("pageQuery：未登录（无 UserContext）→ BizException 401，避免匿名按管理员处理")
    void pageQuery_requires_login() {
        BizException ex = assertThrows(BizException.class, () -> service.pageQuery(new EmployeeQuery()));
        assertEquals(401, ex.getCode());
    }

    // ------------------------------- 工具 -------------------------------

    private void stubSelectPage(Employee employee) {
        when(mapper.selectPage(any(), any())).thenAnswer(inv -> {
            Page<Employee> p = inv.getArgument(0);
            p.setRecords(List.of(employee));
            p.setTotal(1L);
            return p;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private String capturedWrapperSql() {
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(mapper, times(1)).selectPage(any(), captor.capture());
        return captor.getValue().getSqlSegment();
    }

    private static Employee oneEmployee() {
        Employee e = new Employee();
        e.setId(1001L);
        e.setEmpNo("E0001");
        e.setName("张三");
        e.setDeptId(1L);
        e.setStatus(BizConst.EMPLOYEE_ACTIVE);
        return e;
    }

    private static Position position(String name) {
        Position p = new Position();
        p.setId(1L);
        p.setPositionName(name);
        return p;
    }

    private static LoginUser loginUser(String role, Long employeeId, Long deptId) {
        return LoginUser.builder()
                .userId(1L)
                .username("tester")
                .realName("测试账号")
                .employeeId(employeeId)
                .deptId(deptId)
                .roles(LoginUser.roleSet(role))
                .build();
    }

    /** 沿继承链查找并注入字段（baseMapper 声明在父类 ServiceImpl 上） */
    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new IllegalStateException("未找到字段: " + fieldName);
    }
}
