package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import com.meifute.core.service.MallPushItemRuleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Classname MallPushItemRuleController
 * @Description TODO
 * @Date 2020-06-19 17:46
 * @Created by MR. Xb.Wu
 */
@RestController
@RequestMapping("v2/app/push/rule")
@Api(tags = "ItemPushRule", description = "特殊商品发货规则")
@Slf4j
public class MallPushItemRuleController extends BaseController {

    @Autowired
    private MallPushItemRuleService mallPushItemRuleService;


    @ApiOperation(value = "创建特殊发货配置", notes = "创建特殊发货配置")
    @RequestMapping(value = "/add/item/rule", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> queryPushItemRules(@RequestBody MallPushItemRule param) {
        mallPushItemRuleService.createPushItemRule(param);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "查询特殊发货配置列表", notes = "查询特殊发货配置列表")
    @RequestMapping(value = "/query/item/rules", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Page<MallPushItemRule>>> queryPushItemRules(@RequestBody BaseParam param) {
        Page<MallPushItemRule> rulePage = mallPushItemRuleService.queryPushItemRules(param);
        return ResponseEntity.ok(successResult(rulePage));
    }

    @ApiOperation(value = "编辑特殊发货配置", notes = "编辑特殊发货配置")
    @RequestMapping(value = "/edit/item/rule", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> editPushRule(@RequestBody MallPushItemRule param) {
        mallPushItemRuleService.editPushRule(param);
        return ResponseEntity.ok(successResult(true));
    }

    @ApiOperation(value = "删除特殊发货配置", notes = "删除特殊发货配置")
    @RequestMapping(value = "/delete/item/rule", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> deletePushRule(@RequestBody MallPushItemRule param) {
        mallPushItemRuleService.deletePushRule(param);
        return ResponseEntity.ok(successResult(true));
    }

}
