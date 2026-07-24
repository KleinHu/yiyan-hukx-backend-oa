package com.cac.oa.convert.sso;

import com.cac.oa.entity.sso.ExternalSystemEntity;
import com.cac.oa.vo.sso.ExternalSystemVO;
import com.cac.yiyan.common.page.PageData;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 外部系统跳转配置 Convert
 *
 * @author antigravity
 */
@Mapper
public interface ExternalSystemConvert {

    ExternalSystemConvert INSTANCE = Mappers.getMapper(ExternalSystemConvert.class);

    ExternalSystemEntity convert(ExternalSystemVO bean);

    ExternalSystemVO convert(ExternalSystemEntity bean);

    List<ExternalSystemVO> convertList(List<ExternalSystemEntity> list);

    PageData<ExternalSystemVO> convertPage(PageData<ExternalSystemEntity> page);
}
