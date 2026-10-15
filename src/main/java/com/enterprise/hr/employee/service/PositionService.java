package com.enterprise.hr.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.employee.dto.PositionQuery;
import com.enterprise.hr.employee.dto.PositionSaveReq;
import com.enterprise.hr.employee.dto.vo.PositionVO;
import com.enterprise.hr.employee.entity.Position;

/**
 * 岗位业务接口（F-03-03 岗位维护）。
 *
 * <p>列表需要补 {@code dept_name}（{@code dept_id} 为空时为「通用」）与在编人数
 * {@code employeeCount}，均由本 Service 批量补齐。
 *
 * @author Enterprise HR Team
 */
public interface PositionService extends IService<Position> {

    /** 岗位分页（按 sort_no 升序） */
    PageResult<PositionVO> page(PositionQuery query);

    /**
     * 新增岗位：{@code positionCode / positionName} 必填，岗位编码全局唯一。
     *
     * @return 新岗位 ID
     */
    Long create(PositionSaveReq req);

    /**
     * 修改岗位（局部更新，未传字段保持原值）。
     *
     * <p>可改字段：{@code positionName / jobLevel / headcountLimit / sortNo / status / positionSeq}
     * 以及 {@code deptId / duty / requirement / remark}；岗位编码不可修改。
     */
    void update(Long id, PositionSaveReq req);
}
