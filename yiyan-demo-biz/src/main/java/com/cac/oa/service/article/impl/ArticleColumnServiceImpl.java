package com.cac.oa.service.article.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cac.oa.convert.article.ArticleColumnConvert;
import com.cac.oa.dao.article.ArticleColumnMapper;
import com.cac.oa.entity.article.ArticleColumnEntity;
import com.cac.oa.service.article.IArticleColumnService;
import com.cac.oa.vo.article.ArticleColumnQuery;
import com.cac.oa.vo.article.ArticleColumnVO;
import com.cac.yiyan.common.page.PageData;
import com.cac.yiyan.mybatis.query.LambdaQueryWrapperX;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文章专栏 Service 实现
 */
@Service
@RequiredArgsConstructor
public class ArticleColumnServiceImpl extends ServiceImpl<ArticleColumnMapper, ArticleColumnEntity> implements IArticleColumnService {

    private final ArticleColumnMapper columnMapper;
    private final ArticleColumnConvert converter = ArticleColumnConvert.INSTANCE;

    @Override
    public PageData<ArticleColumnVO> getPage(ArticleColumnQuery query) {
        PageData<ArticleColumnEntity> page = columnMapper.selectPage(query, new LambdaQueryWrapperX<ArticleColumnEntity>()
                .likeIfPresent(ArticleColumnEntity::getName, query.getName())
                .eqIfPresent(ArticleColumnEntity::getStatus, query.getStatus())
                .eqIfPresent(ArticleColumnEntity::getIsHomeShow, query.getIsHomeShow())
                .orderByAsc(ArticleColumnEntity::getHomeSort)
                .orderByAsc(ArticleColumnEntity::getSortOrder)
                .orderByDesc(ArticleColumnEntity::getCreateTime));
        return converter.convertPage(page);
    }

    @Override
    public List<ArticleColumnVO> getListAll() {
        List<ArticleColumnEntity> list = columnMapper.selectList(new LambdaQueryWrapperX<ArticleColumnEntity>()
                .eq(ArticleColumnEntity::getStatus, 1) // 1-启用
                .orderByAsc(ArticleColumnEntity::getSortOrder));
        return converter.convertList(list);
    }

    @Override
    public List<ArticleColumnVO> getListTree(ArticleColumnQuery query) {
        // 1. 查询所有符合条件的节点
        List<ArticleColumnEntity> list = columnMapper.selectList(new LambdaQueryWrapperX<ArticleColumnEntity>()
                .likeIfPresent(ArticleColumnEntity::getName, query.getName())
                .eqIfPresent(ArticleColumnEntity::getStatus, query.getStatus())
                .eqIfPresent(ArticleColumnEntity::getIsHomeShow, query.getIsHomeShow())
                .orderByAsc(ArticleColumnEntity::getHomeSort)
                .orderByAsc(ArticleColumnEntity::getSortOrder));
        
        List<ArticleColumnVO> voList = converter.convertList(list);
        if (voList.isEmpty()) {
            return voList;
        }

        // 2. 组装成树形结构
        Map<Long, List<ArticleColumnVO>> childrenMap = voList.stream()
                .filter(vo -> vo.getParentId() != null && vo.getParentId() != 0)
                .collect(Collectors.groupingBy(ArticleColumnVO::getParentId));
        
        voList.forEach(vo -> vo.setChildren(childrenMap.get(vo.getId())));

        // 3. 返回根节点 (parentId 为 0 或 null，或者在当前结果集中找不到父节点的)
        List<Long> allIds = voList.stream().map(ArticleColumnVO::getId).collect(Collectors.toList());
        return voList.stream()
                .filter(vo -> vo.getParentId() == null || vo.getParentId() == 0 || !allIds.contains(vo.getParentId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getChildIds(Long columnId) {
        // 1. 查询所有已启用的专栏 (全量以便组装)
        List<ArticleColumnEntity> allColumns = columnMapper.selectList(new LambdaQueryWrapperX<ArticleColumnEntity>()
                .eq(ArticleColumnEntity::getStatus, 1)); // 1-启用
        
        if (allColumns.isEmpty()) {
            return java.util.Collections.singletonList(columnId);
        }

        // 2. 将数据分组: parentId -> List<Id>
        Map<Long, List<Long>> childrenIdMap = allColumns.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(ArticleColumnEntity::getParentId,
                        Collectors.mapping(ArticleColumnEntity::getId, Collectors.toList())));

        // 3. 递归寻找所有子孙节点
        List<Long> resultIds = new java.util.ArrayList<>();
        collectChildIdsRecursive(columnId, childrenIdMap, resultIds);
        
        return resultIds;
    }

    private void collectChildIdsRecursive(Long parentId, Map<Long, List<Long>> childrenIdMap, List<Long> resultIds) {
        resultIds.add(parentId);
        List<Long> children = childrenIdMap.get(parentId);
        if (children != null && !children.isEmpty()) {
            for (Long childId : children) {
                collectChildIdsRecursive(childId, childrenIdMap, resultIds);
            }
        }
    }
}
