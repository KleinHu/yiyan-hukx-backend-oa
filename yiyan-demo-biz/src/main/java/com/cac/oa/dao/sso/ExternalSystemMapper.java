package com.cac.oa.dao.sso;

import com.cac.oa.entity.sso.ExternalSystemEntity;
import com.cac.yiyan.mybatis.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 外部系统跳转配置持久层
 *
 * @author antigravity
 */
@Mapper
public interface ExternalSystemMapper extends BaseMapperX<ExternalSystemEntity> {
}
