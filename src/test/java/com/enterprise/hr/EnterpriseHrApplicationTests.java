package com.enterprise.hr;

import com.enterprise.hr.common.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * R 通用响应封装纯单测。
 * 真实数据库 CRUD 流程请按 README 执行。
 */
class EnterpriseHrApplicationTests {

    @Test
    void testROk() {
        R<String> r = R.ok("hello");
        assertEquals(200, r.getCode());
        assertEquals("success", r.getMessage());
        assertEquals("hello", r.getData());
    }

    @Test
    void testRFail() {
        R<String> r = R.fail("oops");
        assertEquals(500, r.getCode());
        assertEquals("oops", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    void testROkWithoutData() {
        R<Void> r = R.ok();
        assertNotNull(r);
        assertEquals(200, r.getCode());
        assertNull(r.getData());
    }
}
