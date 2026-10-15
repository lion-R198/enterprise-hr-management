package com.enterprise.hr.salary.service;

import com.enterprise.hr.common.PageQuery;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.entity.SalaryStandard;
import com.enterprise.hr.salary.dto.SalaryStandardCreateReq;
import com.enterprise.hr.salary.dto.SalaryStandardVO;

/**
 * 薪资标准（salary_standard）业务接口。
 *
 * @author Enterprise HR Team
 */
public interface SalaryStandardService {

    /**
     * 薪资标准分页（F-05-01）。
     *
     * @param query 分页入参（current / size）
     */
    PageResult<SalaryStandardVO> page(PageQuery query);

    /**
     * 新增薪资标准。
     *
     * <p>{@code standard_code} 唯一，重复时抛 {@code ErrorCode.CONFLICT}（409）。
     *
     * @return 落库后的实体（含自增主键）
     */
    SalaryStandard create(SalaryStandardCreateReq req);
}
