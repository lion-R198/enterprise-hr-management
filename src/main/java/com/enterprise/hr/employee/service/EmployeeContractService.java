package com.enterprise.hr.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.dto.EmployeeContractCreateReq;
import com.enterprise.hr.employee.dto.EmployeeContractQuery;
import com.enterprise.hr.employee.dto.vo.EmployeeContractVO;
import com.enterprise.hr.employee.entity.EmployeeContract;

/**
 * 劳动合同业务接口（F-02-05 合同管理）。
 *
 * <p>合同编号由服务端按 {@code CT + 年份 + 5 位流水} 生成；
 * 甲方（公司名）取系统参数或内置常量，乙方（员工姓名）取员工档案快照
 * —— 合同主体不接受前端传入，避免被篡改。
 *
 * @author Enterprise HR Team
 */
public interface EmployeeContractService extends IService<EmployeeContract> {

    /** 合同分页（按生效日期倒序），补 {@code employee_name} */
    PageResult<EmployeeContractVO> page(EmployeeContractQuery query);

    /**
     * 签订 / 续签合同。
     *
     * <p>必填 {@code employeeId / startDate}。
     *
     * @return 插入后的合同实体（已回填主键与合同编号，供前端回显）
     */
    EmployeeContract create(EmployeeContractCreateReq req);
}
