package com.cac.oa.service.sso;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cac.oa.entity.sso.ExternalSystemEntity;
import com.cac.oa.vo.sso.ExternalSystemQuery;
import com.cac.oa.vo.sso.ExternalSystemVO;
import com.cac.yiyan.common.page.PageData;

import java.util.List;

/**
 * 外部系统跳转配置 Service 接口
 *
 * @author antigravity
 */
public interface IExternalSystemService extends IService<ExternalSystemEntity> {

    /**
     * 分页查询外部系统
     *
     * @param query 查询参数
     * @return 分页数据
     */
    PageData<ExternalSystemVO> getPage(ExternalSystemQuery query);

    /**
     * 获取所有外部系统清单
     *
     * @return 外部系统列表
     */
    List<ExternalSystemVO> getListAll();
}
