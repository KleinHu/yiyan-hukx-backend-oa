package com.cac.oa.service.sso.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cac.oa.convert.sso.ExternalSystemConvert;
import com.cac.oa.dao.sso.ExternalSystemMapper;
import com.cac.oa.entity.sso.ExternalSystemEntity;
import com.cac.oa.service.sso.IExternalSystemService;
import com.cac.oa.vo.sso.ExternalSystemQuery;
import com.cac.oa.vo.sso.ExternalSystemVO;
import com.cac.yiyan.common.page.PageData;
import com.cac.yiyan.mybatis.query.LambdaQueryWrapperX;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 外部系统跳转配置 Service 实现
 *
 * @author antigravity
 */
@Service
@RequiredArgsConstructor
public class ExternalSystemServiceImpl extends ServiceImpl<ExternalSystemMapper, ExternalSystemEntity> implements IExternalSystemService {

    private final ExternalSystemMapper externalSystemMapper;
    private final ExternalSystemConvert converter = ExternalSystemConvert.INSTANCE;

    @Override
    public PageData<ExternalSystemVO> getPage(ExternalSystemQuery query) {
        PageData<ExternalSystemEntity> page = externalSystemMapper.selectPage(query, new LambdaQueryWrapperX<ExternalSystemEntity>()
                .likeIfPresent(ExternalSystemEntity::getName, query.getName())
                .orderByAsc(ExternalSystemEntity::getSortOrder)
                .orderByDesc(ExternalSystemEntity::getCreateTime));
        return converter.convertPage(page);
    }

    @Override
    public List<ExternalSystemVO> getListAll() {
        List<ExternalSystemEntity> list = externalSystemMapper.selectList(new LambdaQueryWrapperX<ExternalSystemEntity>()
                .orderByAsc(ExternalSystemEntity::getSortOrder));
        return converter.convertList(list);
    }
}
