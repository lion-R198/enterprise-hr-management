package com.enterprise.hr.employee.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.ErrorCode;
import com.enterprise.hr.common.GlobalExceptionHandler;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.dto.EmployeeCreateReq;
import com.enterprise.hr.employee.dto.EmployeeQuery;
import com.enterprise.hr.employee.dto.EmployeeStatusReq;
import com.enterprise.hr.employee.dto.EmployeeUpdateReq;
import com.enterprise.hr.employee.dto.EmployeeVO;
import com.enterprise.hr.employee.dto.vo.EmployeeChangeVO;
import com.enterprise.hr.employee.entity.Employee;
import com.enterprise.hr.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EmployeeController MockMvc 单测（standaloneSetup，不启动 Spring 容器）。
 *
 * <p>本层只验证「Controller 的对外契约」，不验证业务规则（业务规则在 Service 层单测与集成测试）：
 * <ul>
 *     <li><b>三段式响应</b>：恒为 {@code {code, message, data}}，且 HTTP 状态码与业务 code 同值</li>
 *     <li><b>字段命名双向约定</b>：响应蛇形（{@code emp_no} / {@code dept_name}），
 *         请求体与查询参数驼峰（{@code deptId} / {@code positionId}）；
 *         {@code EmployeeCreateResultVO.empNo} 是刻意保留的驼峰例外</li>
 *     <li><b>统一分页</b>：{@code PageResult} 四字段 {@code records/total/current/size}，
 *         不再直接暴露 MyBatis-Plus 的 {@code IPage}</li>
 *     <li><b>统一异常</b>：Service 抛 {@link BizException} 时按错误码映射 HTTP 状态
 *         （404 不存在 / 403 越权 / 400 业务校验）</li>
 *     <li><b>BR-02-01</b>：新增请求体不再要求 {@code empNo}，工号由服务端返回</li>
 * </ul>
 *
 * @author Enterprise HR Team
 */
class EmployeeControllerTest {

    private EmployeeService employeeService;
    private MockMvc mvc;
    private ObjectMapper om;

    @BeforeEach
    void setUp() {
        employeeService = mock(EmployeeService.class);

        om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter conv = new MappingJackson2HttpMessageConverter();
        conv.setObjectMapper(om);

        mvc = MockMvcBuilders.standaloneSetup(new EmployeeController(employeeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(conv)
                .build();
    }

    /** 造一个员工实体（Service 内部数据结构，出参必须经过 VO 转换） */
    private Employee mockEntity(long id, String empNo, String name) {
        Employee e = new Employee();
        e.setId(id);
        e.setEmpNo(empNo);
        e.setName(name);
        e.setEmail(name + "@x.com");
        e.setIdCard("110101199001010011");
        e.setBankAccount("6222021234567890011");
        e.setSalary(new BigDecimal("15000.00"));
        e.setDeptId(1L);
        e.setStatus(BizConst.EMPLOYEE_ACTIVE);
        return e;
    }

    /** 造一个员工出参 VO（蛇形序列化的目标形态） */
    private EmployeeVO mockVO(long id, String empNo, String name) {
        EmployeeVO vo = new EmployeeVO();
        vo.setId(id);
        vo.setEmpNo(empNo);
        vo.setName(name);
        vo.setEmail(name + "@x.com");
        vo.setDeptId(1L);
        vo.setDeptName("研发部");
        vo.setPosition("Java开发工程师");
        vo.setStatus(BizConst.EMPLOYEE_ACTIVE);
        return vo;
    }

    // ------------------------------- 列表（兼容端点） -------------------------------

    @Test
    @DisplayName("GET /employees 返回三段式 R<List<EmployeeVO>>，字段为蛇形且不泄露 deleted/created_at")
    void list_returns_wrapped_vo_list() throws Exception {
        Employee entity = mockEntity(1L, "E0001", "张三");
        when(employeeService.list(any(Wrapper.class))).thenReturn(Collections.singletonList(entity));
        when(employeeService.toVOList(any())).thenReturn(Collections.singletonList(mockVO(1L, "E0001", "张三")));

        mvc.perform(get("/employees"))
                .andExpect(status().isOk())
                // 三段式骨架：code / message / data 三个字段必须齐备
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.message", equalTo("success")))
                .andExpect(jsonPath("$.data", hasSize(1)))
                // 响应蛇形字段
                .andExpect(jsonPath("$.data[0].emp_no", equalTo("E0001")))
                .andExpect(jsonPath("$.data[0].dept_name", equalTo("研发部")))
                .andExpect(jsonPath("$.data[0].name", equalTo("张三")))
                // 驼峰字段名必须不存在（否则前端 prop="emp_no" 全部取不到值）
                .andExpect(jsonPath("$.data[0].empNo").doesNotExist())
                .andExpect(jsonPath("$.data[0].deptName").doesNotExist())
                // 运营字段不外露
                .andExpect(jsonPath("$.data[0].deleted").doesNotExist())
                .andExpect(jsonPath("$.data[0].created_at").doesNotExist());
    }

    // ------------------------------- 详情 -------------------------------

    @Test
    @DisplayName("GET /employees/{id} 返回 R<EmployeeVO>（蛇形字段）")
    void detail_returns_vo() throws Exception {
        when(employeeService.getDetail(1L)).thenReturn(mockVO(1L, "E0001", "张三"));

        mvc.perform(get("/employees/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data.id", equalTo(1)))
                .andExpect(jsonPath("$.data.emp_no", equalTo("E0001")))
                .andExpect(jsonPath("$.data.name", equalTo("张三")))
                .andExpect(jsonPath("$.data.status", equalTo(1)));
    }

    @Test
    @DisplayName("GET /employees/{id} 员工不存在 → HTTP 404 且 code 同值（BizException 通道）")
    void detail_not_found_returns_404() throws Exception {
        when(employeeService.getDetail(999L))
                .thenThrow(new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: 999"));

        mvc.perform(get("/employees/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(404)))
                .andExpect(jsonPath("$.message", equalTo("员工不存在: 999")))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("GET /employees/{id} 越权查看他人档案 → HTTP 403 且 code 同值（NF-10）")
    void detail_forbidden_returns_403() throws Exception {
        when(employeeService.getDetail(2L)).thenThrow(BizException.dataScopeDenied());

        mvc.perform(get("/employees/{id}", 2L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", equalTo(403)))
                .andExpect(jsonPath("$.message", equalTo(ErrorCode.DATA_SCOPE_DENIED.getMessage())));
    }

    // ------------------------------- 按工号 -------------------------------

    @Test
    @DisplayName("GET /employees/by-emp-no/{empNo} 返回 R<EmployeeVO>")
    void by_emp_no_returns_vo() throws Exception {
        when(employeeService.getByEmpNo("E0001")).thenReturn(mockEntity(1L, "E0001", "张三"));
        when(employeeService.toVO(any(Employee.class))).thenReturn(mockVO(1L, "E0001", "张三"));

        mvc.perform(get("/employees/by-emp-no/{empNo}", "E0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data.emp_no", equalTo("E0001")));
    }

    @Test
    @DisplayName("GET /employees/by-emp-no 工号不存在 → 404（统一异常通道，不再依赖 IllegalArgumentException）")
    void by_emp_no_error_returns_404() throws Exception {
        when(employeeService.getByEmpNo("ghost"))
                .thenThrow(new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: ghost"));

        mvc.perform(get("/employees/by-emp-no/{empNo}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(404)))
                .andExpect(jsonPath("$.message", equalTo("员工不存在: ghost")));
    }

    // ------------------------------- 分页 -------------------------------

    @Test
    @DisplayName("GET /employees/page 返回 PageResult 四字段（records/total/current/size），且查询参数按驼峰绑定")
    void page_returns_paged_vo() throws Exception {
        when(employeeService.pageQuery(any(EmployeeQuery.class)))
                .thenReturn(new PageResult<>(Collections.singletonList(mockVO(1L, "E0001", "张三")),
                        91L, 1L, 10L));

        mvc.perform(get("/employees/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "张")
                        .param("deptId", "2")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data", aMapWithSize(4)))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].emp_no", equalTo("E0001")))
                .andExpect(jsonPath("$.data.total", equalTo(91)))
                .andExpect(jsonPath("$.data.current", equalTo(1)))
                .andExpect(jsonPath("$.data.size", equalTo(10)))
                // 不再暴露 MyBatis-Plus IPage 的实现细节
                .andExpect(jsonPath("$.data.pages").doesNotExist())
                .andExpect(jsonPath("$.data.optimizeCountSql").doesNotExist());

        // 请求参数是驼峰：deptId 必须绑定到 EmployeeQuery.deptId（若误按 dept_id 绑定会拿到 null）
        ArgumentCaptor<EmployeeQuery> captor = ArgumentCaptor.forClass(EmployeeQuery.class);
        verify(employeeService).pageQuery(captor.capture());
        EmployeeQuery bound = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("张", bound.getKeyword());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), bound.getDeptId());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(1), bound.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), bound.getCurrent());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(10L), bound.getSize());
    }

    @Test
    @DisplayName("GET /employees/page 非法分页参数（current=0）→ 400，且不进 Service")
    void page_invalid_current_returns_400() throws Exception {
        mvc.perform(get("/employees/page").param("current", "0").param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("页码不得小于 1")));

        verify(employeeService, never()).pageQuery(any(EmployeeQuery.class));
    }

    // ------------------------------- 异动记录 -------------------------------

    @Test
    @DisplayName("GET /employees/{id}/changes 非分页列表同样用 PageResult 结构")
    void changes_wrapped_as_page_result() throws Exception {
        EmployeeChangeVO change = new EmployeeChangeVO();
        change.setId(7L);
        change.setEmployeeId(1L);
        change.setEmployeeName("张三");
        change.setChangeType(1);
        when(employeeService.listChanges(1L)).thenReturn(List.of(change));

        mvc.perform(get("/employees/{id}/changes", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].employee_id", equalTo(1)))
                .andExpect(jsonPath("$.data.records[0].change_type", equalTo(1)))
                .andExpect(jsonPath("$.data.total", equalTo(1)));
    }

    // ------------------------------- 新增（BR-02-01） -------------------------------

    @Test
    @DisplayName("POST /employees 请求体不传 empNo，仅 name/deptId/positionId 即可通过校验并进入 Service")
    void create_without_empNo_passes_validation() throws Exception {
        when(employeeService.create(any(EmployeeCreateReq.class)))
                .thenReturn(mockEntity(99L, "E0092", "新员工"));

        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新员工\",\"deptId\":1,\"positionId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                // EmployeeCreateResultVO 契约：{success, id, empNo}，empNo 为刻意保留的驼峰字段
                .andExpect(jsonPath("$.data", aMapWithSize(3)))
                .andExpect(jsonPath("$.data.success", equalTo(true)))
                .andExpect(jsonPath("$.data.id", equalTo(99)))
                .andExpect(jsonPath("$.data.empNo", equalTo("E0092")))
                .andExpect(jsonPath("$.data.emp_no").doesNotExist());

        // 请求体驼峰：deptId / positionId 必须绑定成功
        ArgumentCaptor<EmployeeCreateReq> captor = ArgumentCaptor.forClass(EmployeeCreateReq.class);
        verify(employeeService).create(captor.capture());
        EmployeeCreateReq sent = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), sent.getDeptId());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), sent.getPositionId());
        org.junit.jupiter.api.Assertions.assertNull(sent.getEmpNo(), "工号由服务端生成，请求体不应携带");
    }

    @Test
    @DisplayName("POST /employees 缺 deptId/positionId → @Valid 触发 400，不进 Service")
    void create_missing_required_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("所属部门不能为空")))
                .andExpect(jsonPath("$.message", containsString("所属岗位不能为空")));

        verify(employeeService, never()).create(any(EmployeeCreateReq.class));
    }

    @Test
    @DisplayName("POST /employees 姓名为空 → 400")
    void create_blank_name_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"deptId\":1,\"positionId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("姓名不能为空")));
    }

    @Test
    @DisplayName("POST /employees email 格式错 → @Valid 触发 400")
    void create_invalid_email_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\",\"deptId\":1,\"positionId\":2,"
                                + "\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("邮箱格式错误")));
    }

    @Test
    @DisplayName("POST /employees 年龄越界 → @Valid 触发 400")
    void create_age_out_of_range_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\",\"deptId\":1,\"positionId\":2,\"age\":200}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)));
    }

    @Test
    @DisplayName("POST /employees 薪资为负 → @DecimalMin 触发 400（BR-02-02）")
    void create_negative_salary_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\",\"deptId\":1,\"positionId\":2,"
                                + "\"salary\":-1.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("薪资不能为负")));
    }

    @Test
    @DisplayName("POST /employees Service 层业务拦截（BR-02-02 负薪资）→ 400 且 message 透传")
    void create_service_biz_error_returns_400() throws Exception {
        when(employeeService.create(any(EmployeeCreateReq.class)))
                .thenThrow(new BizException(ErrorCode.NEGATIVE_SALARY));

        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"张三\",\"deptId\":1,\"positionId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", equalTo(ErrorCode.NEGATIVE_SALARY.getMessage())));
    }

    // ------------------------------- 修改 -------------------------------

    @Test
    @DisplayName("PUT /employees/{id} 成功 → 200 + {success:true}，入参为 DTO 驼峰绑定")
    void update_success() throws Exception {
        when(employeeService.update(eq(1L), any(EmployeeUpdateReq.class)))
                .thenReturn(mockEntity(1L, "E0001", "新名"));

        mvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名\",\"deptId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data", aMapWithSize(1)))
                .andExpect(jsonPath("$.data.success", equalTo(true)));

        ArgumentCaptor<EmployeeUpdateReq> captor = ArgumentCaptor.forClass(EmployeeUpdateReq.class);
        verify(employeeService).update(eq(1L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("新名", captor.getValue().getName());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), captor.getValue().getDeptId());
    }

    @Test
    @DisplayName("PUT /employees/{id} 试图修改工号 → 400（BR-02-01 工号不可修改）")
    void update_empNo_immutable_returns_400() throws Exception {
        when(employeeService.update(eq(1L), any()))
                .thenThrow(BizException.badRequest("工号不可修改"));

        mvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"empNo\":\"E9999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", equalTo("工号不可修改")));
    }

    @Test
    @DisplayName("PUT /employees/{id} email 格式错 → @Valid 400")
    void update_invalid_email_returns_400() throws Exception {
        mvc.perform(put("/employees/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("邮箱格式错误")));
    }

    // ------------------------------- 状态变更 -------------------------------

    @Test
    @DisplayName("PUT /employees/{id}/status 成功 → 200，状态走独立接口以便留痕（BR-02-04）")
    void change_status_success() throws Exception {
        doNothing().when(employeeService).changeStatus(eq(1L), any(EmployeeStatusReq.class));

        mvc.perform(put("/employees/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0,\"reason\":\"个人原因\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data.success", equalTo(true)));

        ArgumentCaptor<EmployeeStatusReq> captor = ArgumentCaptor.forClass(EmployeeStatusReq.class);
        verify(employeeService).changeStatus(eq(1L), captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(0), captor.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("个人原因", captor.getValue().getReason());
    }

    @Test
    @DisplayName("PUT /employees/{id}/status 缺 status → 400")
    void change_status_missing_status_returns_400() throws Exception {
        mvc.perform(put("/employees/{id}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", containsString("目标状态不能为空")));
    }

    // ------------------------------- 删除 -------------------------------

    @Test
    @DisplayName("DELETE /employees/{id} 成功 → 200（BR-02-03 逻辑删除语义在 Service 层）")
    void delete_success() throws Exception {
        doNothing().when(employeeService).delete(1L);

        mvc.perform(delete("/employees/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data.success", equalTo(true)));

        verify(employeeService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("DELETE /employees/{id} Service 抛 404 → HTTP 404 且 code 同值")
    void delete_not_found_returns_404() throws Exception {
        doThrow(new BizException(ErrorCode.EMPLOYEE_NOT_FOUND, "员工不存在: id=1"))
                .when(employeeService).delete(1L);

        mvc.perform(delete("/employees/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", equalTo(404)))
                .andExpect(jsonPath("$.message", equalTo("员工不存在: id=1")));
    }

    @Test
    @DisplayName("DELETE /employees/batch 批量成功（/batch 优先于 /{id} 匹配）")
    void delete_batch_success() throws Exception {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        doNothing().when(employeeService).deleteBatch(ids);

        mvc.perform(delete("/employees/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data.success", equalTo(true)));

        verify(employeeService).deleteBatch(ids);
    }

    @Test
    @DisplayName("DELETE /employees/batch 空列表 → 400（Service 业务拦截）")
    void delete_batch_empty_returns_400() throws Exception {
        doThrow(BizException.badRequest("待删除 id 列表不能为空"))
                .when(employeeService).deleteBatch(Collections.emptyList());

        mvc.perform(delete("/employees/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", equalTo("待删除 id 列表不能为空")));
    }

    // ------------------------------- 响应骨架 -------------------------------

    @Test
    @DisplayName("成功响应恒为 {code:200, message:'success', data} 三段式")
    void response_envelope_is_consistent() throws Exception {
        when(employeeService.list(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(employeeService.toVOList(any())).thenReturn(Collections.emptyList());

        mvc.perform(get("/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.message", equalTo("success")))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("请求体 JSON 结构错误 → 走统一 400 通道，不外露堆栈")
    void malformed_body_returns_400() throws Exception {
        mvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", equalTo(400)))
                .andExpect(jsonPath("$.message", equals400Message()));
    }

    /** 统一异常通道对不可读请求体的固定提示（与 GlobalExceptionHandler 契约一致） */
    private static org.hamcrest.Matcher<String> equals400Message() {
        return equalTo("请求体格式错误，请检查 JSON 结构");
    }
}
