package com.enterprise.hr.employee.integration;

import com.enterprise.hr.common.R;
import com.enterprise.hr.common.constant.BizConst;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端 HTTP 集成测试：真实 Spring 容器 + 真实 MySQL + TestRestTemplate。
 *
 * <p>与 {@link com.enterprise.hr.employee.controller.EmployeeControllerTest}（standaloneSetup）的差异：
 * <ul>
 *     <li>这里走真实的 {@code AuthInterceptor}（fail-close：除 {@code /auth/login} 外全部要 Bearer 令牌）</li>
 *     <li>这里走真实的 Jackson 全局配置（蛇形策略 / 日期格式 / 三段式响应）</li>
 *     <li>这里走真实 MySQL（验证 {@code @TableLogic} / 自增主键 / 自动填充在 SQL 层确实生效）</li>
 * </ul>
 *
 * <p>本层专门守护改造后新增的<b>对外契约</b>：
 * <ol>
 *     <li>无令牌 → 401（且 HTTP 状态码与业务 code 同值）</li>
 *     <li>响应蛇形 {@code snake_case} / 请求与查询参数驼峰 {@code camelCase}</li>
 *     <li>{@code PageResult} 四字段分页结构</li>
 *     <li>BR-02-01 工号由系统生成、不可修改；BR-02-02 负薪资被拒；新入职默认待入职</li>
 *     <li>写操作在 {@code sys_oper_log} 留痕</li>
 * </ol>
 *
 * <p>类型擦除陷阱：{@link TestRestTemplate#postForEntity(String, Object, Class, Object...)}
 * 不能接收 {@code ParameterizedTypeReference}，需要泛型化的列表/Map 必须用
 * {@link TestRestTemplate#exchange} 配合原始 {@code R} 类型再手工转型。
 *
 * @author Enterprise HR Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql(scripts = "/sql/test-cleanup.sql")
class EmployeeHttpIntegrationTest {

    /** 种子员工：E0001 / 张三 / 研发部 */
    private static final long SEED_EMPLOYEE_ID = 1001L;
    /** 种子部门：2 = 产品部（仅王五 1003 在岗） */
    private static final long SEED_DEPT_ID = 2L;
    /** 种子岗位：1 = Java开发工程师 */
    private static final long SEED_POSITION_ID = 1L;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    // ------------------------------------------------------------------ 鉴权契约

    @Test
    @DisplayName("无令牌访问受保护接口 → HTTP 401 且响应体 code 同值（fail-close）")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void protected_endpoint_without_token_returns_401() {
        ResponseEntity<R> resp = rest.getForEntity(url("/employees/page?current=1&size=5"), R.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        R body = resp.getBody();
        assertNotNull(body, "401 也必须是 {code,message,data} 三段式");
        assertEquals(401, body.getCode().intValue());
        assertEquals(401, resp.getStatusCode().value(), "HTTP 状态码必须与业务 code 同值");
        assertNotNull(body.getMessage(), "message 必须是可读提示");
    }

    @Test
    @DisplayName("登录 → data.token 可取到令牌，data.user 内部字段保持驼峰（前端 store 契约）")
    @SuppressWarnings({"rawtypes", "unchecked"})
    void login_returns_token_and_camel_case_user() {
        ResponseEntity<R> resp = postJson("/auth/login", loginBody(), null);

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> data = dataOf(resp);
        assertTrue(data.containsKey("token"), "data 必须含 token，实际 keys=" + data.keySet());
        assertNotNull(data.get("token"));

        Object user = data.get("user");
        assertTrue(user instanceof Map, "data.user 必须存在（前端读 data.user，不是 userInfo）");
        Map<String, Object> userMap = (Map<String, Object>) user;
        for (String key : List.of("id", "username", "realName", "employeeId", "deptId", "roles")) {
            assertTrue(userMap.containsKey(key),
                    "user 内部字段为刻意保留的驼峰，缺少 " + key + "，实际=" + userMap.keySet());
        }
    }

    // ------------------------------------------------------------------ 字段命名：响应蛇形

    @Test
    @DisplayName("GET /employees/page 响应为蛇形字段，分页结构统一为 records/total/current/size")
    @SuppressWarnings("rawtypes")
    void page_returns_snake_case_fields_and_unified_pagination() {
        ResponseEntity<R> resp = get("/employees/page?current=1&size=10", adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> page = dataOf(resp);
        assertEquals(4, page.size(), "分页响应只应有 records/total/current/size，实际=" + page.keySet());
        assertEquals(1, ((Number) page.get("current")).intValue());
        assertEquals(10, ((Number) page.get("size")).intValue());
        assertTrue(((Number) page.get("total")).longValue() >= 3, "至少有 3 条种子员工");

        List<Map<String, Object>> records = recordsOf(resp);
        assertFalse(records.isEmpty());
        Map<String, Object> first = records.get(0);
        assertTrue(first.containsKey("emp_no"), "响应必须蛇形 emp_no，实际=" + first.keySet());
        assertTrue(first.containsKey("dept_name"), "响应必须蛇形 dept_name，实际=" + first.keySet());
        assertTrue(first.containsKey("id_card"), "响应必须蛇形 id_card，实际=" + first.keySet());
        assertFalse(first.containsKey("empNo"), "不得出现驼峰 empNo");
        assertFalse(first.containsKey("deleted"), "响应不应泄露逻辑删除标志");
        assertFalse(first.containsKey("created_at"), "响应不应泄露运营字段");
    }

    @Test
    @DisplayName("查询参数使用驼峰 deptId 能正确过滤（误按 dept_id 绑定会返回全量）")
    @SuppressWarnings("rawtypes")
    void page_filters_by_camel_case_query_param() {
        ResponseEntity<R> resp = get("/employees/page?current=1&size=10&deptId=" + SEED_DEPT_ID, adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        List<Map<String, Object>> records = recordsOf(resp);
        assertFalse(records.isEmpty(), "产品部下应有种子员工");
        for (Map<String, Object> row : records) {
            assertEquals(SEED_DEPT_ID, ((Number) row.get("dept_id")).longValue(),
                    "deptId 查询参数未生效，返回了其它部门的员工");
        }
    }

    @Test
    @DisplayName("GET /employees 兼容列表端点同样返回蛇形 VO")
    @SuppressWarnings("rawtypes")
    void list_endpoint_returns_snake_case_vo_list() {
        ResponseEntity<R> resp = get("/employees", adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getBody().getData();
        assertTrue(data.size() >= 3, "至少有 3 条种子员工");
        assertNotNull(data.get(0).get("emp_no"));
        assertNotNull(data.get(0).get("name"));
    }

    @Test
    @DisplayName("GET /employees/{id} 详情：蛇形字段 + 身份证脱敏")
    @SuppressWarnings("rawtypes")
    void detail_returns_snake_case_and_masked_sensitive_fields() {
        ResponseEntity<R> resp = get("/employees/" + SEED_EMPLOYEE_ID, adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> data = dataOf(resp);
        assertEquals("E0001", data.get("emp_no"));
        assertEquals("张三", data.get("name"));
        assertEquals("研发部", data.get("dept_name"));
    }

    @Test
    @DisplayName("GET /employees/by-emp-no/{empNo} 兼容端点按工号返回 VO")
    @SuppressWarnings("rawtypes")
    void by_emp_no_returns_vo() {
        ResponseEntity<R> resp = get("/employees/by-emp-no/E0001", adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        assertEquals("E0001", dataOf(resp).get("emp_no"));
    }

    @Test
    @DisplayName("GET /employees/{id}/changes 非分页列表也统一为 PageResult 结构")
    @SuppressWarnings("rawtypes")
    void changes_endpoint_uses_page_result_shape() {
        ResponseEntity<R> resp = get("/employees/" + SEED_EMPLOYEE_ID + "/changes", adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> data = dataOf(resp);
        for (String key : List.of("records", "total", "current", "size")) {
            assertTrue(data.containsKey(key), "缺少 " + key + "，实际=" + data.keySet());
        }
        assertTrue(data.get("records") instanceof List);
    }

    // ------------------------------------------------------------------ 统一参数校验 / 异常通道

    @Test
    @DisplayName("非法分页参数 current=0 → HTTP 400 且 code 同值")
    @SuppressWarnings("rawtypes")
    void invalid_page_param_returns_400() {
        ResponseEntity<R> resp = get("/employees/page?current=0&size=10", adminToken());

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, 400);
        assertTrue(resp.getBody().getMessage().contains("页码不得小于 1"),
                "message=" + resp.getBody().getMessage());
    }

    @Test
    @DisplayName("详情请求不存在的员工 → 4xx（不落 500、message 可读）")
    @SuppressWarnings("rawtypes")
    void detail_not_found_returns_client_error() {
        ResponseEntity<R> resp = get("/employees/99999999", adminToken());

        assertTrue(resp.getStatusCode().is4xxClientError(),
                "期望 4xx，实际 " + resp.getStatusCode());
        assertEquals(resp.getStatusCode().value(), resp.getBody().getCode().intValue(),
                "HTTP 状态码必须与业务 code 同值");
        assertNotNull(resp.getBody().getMessage());
    }

    @Test
    @DisplayName("按不存在的工号查询 → 4xx 且 message 可读")
    @SuppressWarnings("rawtypes")
    void by_emp_no_not_found_returns_client_error() {
        ResponseEntity<R> resp = get("/employees/by-emp-no/ghost-emp-no", adminToken());

        assertTrue(resp.getStatusCode().is4xxClientError(),
                "期望 4xx，实际 " + resp.getStatusCode());
        assertNotNull(resp.getBody().getMessage());
    }

    @Test
    @DisplayName("新增缺必填（deptId / positionId）→ HTTP 400 且 code 同值")
    @SuppressWarnings("rawtypes")
    void create_missing_required_returns_400() {
        ResponseEntity<R> resp = postJson("/employees", Map.of("name", "张三"), adminToken());

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, 400);
        String message = resp.getBody().getMessage();
        assertTrue(message.contains("所属部门不能为空"), "message=" + message);
        assertTrue(message.contains("所属岗位不能为空"), "message=" + message);
    }

    @Test
    @DisplayName("BR-02-02 新增负薪资 → HTTP 400 且 code 同值")
    @SuppressWarnings("rawtypes")
    void create_negative_salary_returns_400() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "负薪员工");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);
        body.put("salary", -100.00);

        ResponseEntity<R> resp = postJson("/employees", body, adminToken());

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, 400);
        assertTrue(resp.getBody().getMessage().contains("薪资不能为负"),
                "message=" + resp.getBody().getMessage());
    }

    @Test
    @DisplayName("BR-02-01 显式传入已存在的工号 → 400 且提示「工号已存在」")
    @SuppressWarnings("rawtypes")
    void create_duplicate_explicit_emp_no_returns_400() {
        Map<String, Object> body = new HashMap<>();
        body.put("empNo", "E0001");     // 种子已存在
        body.put("name", "重复工号员工");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);

        ResponseEntity<R> resp = postJson("/employees", body, adminToken());

        assertEnvelope(resp, HttpStatus.BAD_REQUEST, 400);
        assertTrue(resp.getBody().getMessage().contains("工号已存在"),
                "message=" + resp.getBody().getMessage());
    }

    // ------------------------------------------------------------------ BR-02-01 真实写路径

    @Test
    @DisplayName("BR-02-01：请求体不传 empNo → 工号由系统生成，返回 {success,id,empNo}，状态默认待入职")
    @SuppressWarnings("rawtypes")
    void create_auto_generates_emp_no_and_pending_status() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "集成入职员工");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);
        body.put("gender", "M");
        body.put("salary", 12345.67);
        body.put("hireDate", LocalDate.now().toString());

        ResponseEntity<R> resp = postJson("/employees", body, adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> data = dataOf(resp);
        assertEquals(3, data.size(), "新增结果契约为 {success,id,empNo}，实际=" + data.keySet());
        assertEquals(Boolean.TRUE, data.get("success"));
        assertNotNull(data.get("id"), "必须回填新员工主键");
        // EmployeeCreateResultVO.empNo 是契约里刻意保留的驼峰字段（前端新增后立即回显）
        Object empNo = data.get("empNo");
        assertNotNull(empNo, "必须回显系统生成的工号，实际 keys=" + data.keySet());
        assertTrue(String.valueOf(empNo).matches("^E\\d{4,}$"), "工号形如 E0051，实际=" + empNo);

        long newId = ((Number) data.get("id")).longValue();
        ResponseEntity<R> detail = get("/employees/" + newId, adminToken());
        assertEnvelope(detail, HttpStatus.OK, 200);
        Map<String, Object> created = dataOf(detail);
        assertEquals(empNo, created.get("emp_no"), "详情里的工号应与新增回显一致");
        assertEquals(BizConst.EMPLOYEE_PENDING, ((Number) created.get("status")).intValue(),
                "新入职默认「待入职」= " + BizConst.EMPLOYEE_PENDING);
    }

    @Test
    @DisplayName("BR-02-01：工号不可修改（PUT 传入不同工号 → 4xx 并提示）")
    @SuppressWarnings("rawtypes")
    void update_emp_no_is_immutable() {
        Map<String, Object> body = new HashMap<>();
        body.put("empNo", "E9999");
        body.put("name", "改名");

        ResponseEntity<R> resp = exchange(HttpMethod.PUT, "/employees/" + SEED_EMPLOYEE_ID, body, adminToken());

        assertTrue(resp.getStatusCode().is4xxClientError(), "期望 4xx，实际 " + resp.getStatusCode());
        assertTrue(resp.getBody().getMessage().contains("工号不可修改"),
                "message=" + resp.getBody().getMessage());

        // 落库工号必须没变
        ResponseEntity<R> detail = get("/employees/" + SEED_EMPLOYEE_ID, adminToken());
        assertEquals("E0001", dataOf(detail).get("emp_no"));
    }

    @Test
    @DisplayName("BR-02-03：DELETE /employees/{id} 是「逻辑离职」，行保留且状态置 0")
    @SuppressWarnings("rawtypes")
    void delete_resigns_instead_of_removing() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "待离职员工");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);
        ResponseEntity<R> created = postJson("/employees", body, adminToken());
        assertEnvelope(created, HttpStatus.OK, 200);
        long id = ((Number) dataOf(created).get("id")).longValue();

        ResponseEntity<R> deleted = exchange(HttpMethod.DELETE, "/employees/" + id, null, adminToken());
        assertEnvelope(deleted, HttpStatus.OK, 200);
        assertEquals(Boolean.TRUE, dataOf(deleted).get("success"));

        // 历史数据要保留可追溯 → 详情仍可读，但状态已离职
        ResponseEntity<R> detail = get("/employees/" + id, adminToken());
        assertEnvelope(detail, HttpStatus.OK, 200);
        Map<String, Object> after = dataOf(detail);
        assertEquals(BizConst.EMPLOYEE_RESIGNED, ((Number) after.get("status")).intValue());
        assertNotNull(after.get("leave_date"), "离职时应写入 leave_date");

        Integer deletedFlag = jdbc.queryForObject(
                "SELECT deleted FROM employee WHERE id = ?", Integer.class, id);
        assertEquals(Integer.valueOf(0), deletedFlag, "BR-02-03 不做逻辑删除，deleted 必须保持 0");
    }

    @Test
    @DisplayName("PUT /employees/{id}/status 状态流转成功并可在详情读回（BR-02-04 留痕）")
    @SuppressWarnings("rawtypes")
    void change_status_reflects_in_detail() {
        ResponseEntity<R> resp = exchange(HttpMethod.PUT, "/employees/" + SEED_EMPLOYEE_ID + "/status",
                Map.of("status", 0, "reason", "个人原因"), adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        assertEquals(Boolean.TRUE, dataOf(resp).get("success"));

        Map<String, Object> after = dataOf(get("/employees/" + SEED_EMPLOYEE_ID, adminToken()));
        assertEquals(BizConst.EMPLOYEE_RESIGNED, ((Number) after.get("status")).intValue());
        assertEquals("个人原因", after.get("leave_reason"));

        Integer changes = jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee_change WHERE employee_id = ? AND change_type = 7",
                Integer.class, SEED_EMPLOYEE_ID);
        assertTrue(changes >= 1, "离职状态变更必须写入异动留痕");
    }

    @Test
    @DisplayName("DELETE /employees/batch 批量逻辑离职成功")
    @SuppressWarnings("rawtypes")
    void batch_delete_success() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "批量离职候选");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);
        ResponseEntity<R> created = postJson("/employees", body, adminToken());
        long id = ((Number) dataOf(created).get("id")).longValue();

        ResponseEntity<R> resp = exchange(HttpMethod.DELETE, "/employees/batch", List.of(id), adminToken());

        assertEnvelope(resp, HttpStatus.OK, 200);
        Map<String, Object> after = dataOf(get("/employees/" + id, adminToken()));
        assertEquals(BizConst.EMPLOYEE_RESIGNED, ((Number) after.get("status")).intValue());
    }

    // ------------------------------------------------------------------ 操作日志

    @Test
    @DisplayName("写操作在 sys_oper_log 留痕（系统级日志契约）")
    @SuppressWarnings("rawtypes")
    void write_operation_is_audited() {
        int before = countEmployeeOperLogs();

        Map<String, Object> body = new HashMap<>();
        body.put("name", "审计留痕员工");
        body.put("deptId", SEED_DEPT_ID);
        body.put("positionId", SEED_POSITION_ID);
        assertEnvelope(postJson("/employees", body, adminToken()), HttpStatus.OK, 200);

        int after = countEmployeeOperLogs();
        assertTrue(after > before, "写操作必须落一条 sys_oper_log（before=" + before + " after=" + after + "）");
    }

    // ------------------------------------------------------------------ 工具

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private static Map<String, String> loginBody() {
        return Map.of("username", "admin", "password", "admin123");
    }

    /** 登录换取 Bearer 令牌；刻意走真实登录接口，顺带验证登录契约本身可用 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private String adminToken() {
        ResponseEntity<R> resp = postJson("/auth/login", loginBody(), null);
        assertEquals(HttpStatus.OK, resp.getStatusCode(), "管理员登录失败，无法继续鉴权相关的用例");
        Map<String, Object> data = dataOf(resp);
        Object token = data.get("token");
        assertNotNull(token, "登录响应缺少 data.token");
        return String.valueOf(token);
    }

    private HttpHeaders headers(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (token != null) {
            h.setBearerAuth(token);
        }
        return h;
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<R> get(String path, String token) {
        return rest.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), R.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<R> postJson(String path, Object body, String token) {
        return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers(token)), R.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<R> exchange(HttpMethod method, String path, Object body, String token) {
        return rest.exchange(url(path), method, new HttpEntity<>(body, headers(token)), R.class);
    }

    /** 断言三段式骨架、HTTP 状态码与业务 code 同值 */
    @SuppressWarnings("rawtypes")
    private void assertEnvelope(ResponseEntity<R> resp, HttpStatus expectedStatus, int expectedCode) {
        assertEquals(expectedStatus, resp.getStatusCode(),
                "HTTP 状态码不符，body=" + resp.getBody());
        R body = resp.getBody();
        assertNotNull(body, "响应体必须是 {code,message,data} 三段式");
        assertNotNull(body.getCode(), "响应体缺少 code");
        assertEquals(expectedCode, body.getCode().intValue(),
                "业务 code 不符，message=" + body.getMessage());
        assertEquals(expectedStatus.value(), body.getCode().intValue(),
                "HTTP 状态码与业务 code 必须同值");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(ResponseEntity<R> resp) {
        assertNotNull(resp.getBody(), "响应体为空");
        Object data = resp.getBody().getData();
        assertTrue(data instanceof Map, "data 应为对象，实际=" + data);
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> recordsOf(ResponseEntity<R> resp) {
        Object records = dataOf(resp).get("records");
        assertTrue(records instanceof List, "分页 data.records 应为数组，实际=" + records);
        return (List<Map<String, Object>>) records;
    }

    private int countEmployeeOperLogs() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE module = '员工管理'", Integer.class);
        return count == null ? 0 : count;
    }
}
