package com.cac.oa.controller.sso;

import com.cac.oa.convert.sso.ExternalSystemConvert;
import com.cac.oa.entity.sso.ExternalSystemEntity;
import com.cac.oa.service.sso.IExternalSystemService;
import com.cac.oa.vo.sso.ExternalSystemQuery;
import com.cac.oa.vo.sso.ExternalSystemVO;
import com.cac.yiyan.common.page.PageData;
import com.cac.yiyan.common.vo.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 外部系统跳转配置 Controller
 *
 * @author antigravity
 */
@Api(tags = "OA-外部系统跳转管理")
@RestController
@RequestMapping("/api/240/oa/external-system")
@RequiredArgsConstructor
public class ExternalSystemController {

    private final IExternalSystemService externalSystemService;
    private final ExternalSystemConvert converter = ExternalSystemConvert.INSTANCE;

    @GetMapping("/page")
    @ApiOperation("分页查询外部系统")
    public Result<PageData<ExternalSystemVO>> page(ExternalSystemQuery query) {
        return Result.ok(externalSystemService.getPage(query));
    }

    @GetMapping("/list-all")
    @ApiOperation("获取所有外部系统清单")
    public Result<List<ExternalSystemVO>> listAll() {
        return Result.ok(externalSystemService.getListAll());
    }

    @PostMapping
    @ApiOperation("新增外部系统")
    public Result<Boolean> save(@RequestBody ExternalSystemVO vo) {
        ExternalSystemEntity entity = converter.convert(vo);
        return Result.ok(externalSystemService.save(entity));
    }

    @PutMapping
    @ApiOperation("修改外部系统")
    public Result<Boolean> update(@RequestBody ExternalSystemVO vo) {
        ExternalSystemEntity entity = converter.convert(vo);
        return Result.ok(externalSystemService.updateById(entity));
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除外部系统")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        return Result.ok(externalSystemService.removeById(id));
    }
}
