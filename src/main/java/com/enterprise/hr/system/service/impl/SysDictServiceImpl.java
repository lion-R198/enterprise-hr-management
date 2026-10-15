package com.enterprise.hr.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.enterprise.hr.common.BizException;
import com.enterprise.hr.common.PageResult;
import com.enterprise.hr.common.constant.BizConst;
import com.enterprise.hr.employee.entity.SysDictItem;
import com.enterprise.hr.employee.entity.SysDictType;
import com.enterprise.hr.system.dto.vo.SysDictItemVO;
import com.enterprise.hr.system.dto.vo.SysDictTypeVO;
import com.enterprise.hr.system.mapper.SysDictItemMapper;
import com.enterprise.hr.system.mapper.SysDictTypeMapper;
import com.enterprise.hr.system.service.SysDictService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据字典服务实现。
 *
 * @author Enterprise HR Team
 */
@Service
public class SysDictServiceImpl implements SysDictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public SysDictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    @Override
    public PageResult<SysDictTypeVO> listTypes() {
        // 只返回启用中的类型：停用意味着该枚举已从业务上废弃，
        // 前端若仍渲染出下拉项，用户选出来的值在新数据里将无从解释
        List<SysDictTypeVO> list = dictTypeMapper.selectList(Wrappers.<SysDictType>lambdaQuery()
                        .eq(SysDictType::getStatus, BizConst.STATUS_ENABLED)
                        .orderByAsc(SysDictType::getId))
                .stream().map(this::toTypeVO).toList();
        return PageResult.list(list);
    }

    @Override
    public PageResult<SysDictItemVO> listItems(Long typeId) {
        if (typeId == null) {
            throw BizException.badRequest("字典类型 ID 不能为空");
        }
        // 类型不存在时明确报 404，而不是返回空数组
        // ——"空数组"会让前端误以为该字典尚未配置，掩盖调用方传错 ID 的问题
        SysDictType type = dictTypeMapper.selectById(typeId);
        if (type == null) {
            throw BizException.notFound("字典类型不存在");
        }
        List<SysDictItemVO> list = dictItemMapper.selectList(Wrappers.<SysDictItem>lambdaQuery()
                        .eq(SysDictItem::getDictTypeId, typeId)
                        .eq(SysDictItem::getStatus, BizConst.STATUS_ENABLED)
                        .orderByAsc(SysDictItem::getSortNo)
                        .orderByAsc(SysDictItem::getId))
                .stream().map(this::toItemVO).toList();
        return PageResult.list(list);
    }

    private SysDictTypeVO toTypeVO(SysDictType type) {
        SysDictTypeVO vo = new SysDictTypeVO();
        vo.setId(type.getId());
        vo.setDictType(type.getDictType());
        vo.setDictName(type.getDictName());
        vo.setStatus(type.getStatus());
        vo.setRemark(type.getRemark());
        return vo;
    }

    private SysDictItemVO toItemVO(SysDictItem item) {
        SysDictItemVO vo = new SysDictItemVO();
        vo.setId(item.getId());
        vo.setDictTypeId(item.getDictTypeId());
        vo.setItemValue(item.getItemValue());
        vo.setItemLabel(item.getItemLabel());
        vo.setSortNo(item.getSortNo());
        vo.setIsDefault(item.getIsDefault());
        vo.setCssClass(item.getCssClass());
        vo.setStatus(item.getStatus());
        vo.setRemark(item.getRemark());
        return vo;
    }
}
