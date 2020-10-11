package com.meifute.core.controller;

import com.meifute.core.dto.AutoPushItem;
import com.meifute.core.dto.AutoSku;
import com.meifute.core.entity.MallSku;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.service.MallOrderToPushService;
import com.meifute.core.vo.AutoItemParam;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-04-04 10:11
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v2/app/auto/push")
@Api(tags = "AutoPush", description = "开启自动推单")
@Slf4j
public class AutoPushController extends BaseController {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private MallOrderToPushService mallOrderToPushService;

    @ApiOperation(value = "初始化自动推单数据", notes = "初始化自动推单数据")
    @RequestMapping(value = "/init/push", method = RequestMethod.GET)
    public ResponseEntity<MallResponse<AutoPushItem>> initAutoPush() {
        AutoPushItem autoPushItem = new AutoPushItem();
        String on = RedisUtil.get("autoPush:online");
        String acOn = RedisUtil.get("autoPush:acOnline");
        if (ObjectUtils.isNullOrEmpty(on)) {
            RedisUtil.set("autoPush:online", "1");
            on = "1";
        }
        if (ObjectUtils.isNullOrEmpty(acOn)) {
            RedisUtil.set("autoPush:acOnline", "1");
            acOn = "1";
        }
        String items = RedisUtil.get("autoPush:items");
        List<MallSku> allItems = itemFeign.getAllItems();
        if (ObjectUtils.isNullOrEmpty(items)) {
            List<AutoSku> autoSkus = BeanMapper.mapList(allItems, AutoSku.class);
            autoSkus.forEach(p -> {
                p.setAutoed("1");
            });
            RedisUtil.set("autoPush:items", JSONUtil.obj2json(autoSkus));
            autoPushItem.setAutoSkus(autoSkus);
        } else {
            List<AutoSku> autoSkuNow = BeanMapper.mapList(allItems, AutoSku.class);
            List<AutoSku> autoSkus = JSONUtil.json2list(items, AutoSku.class);

            List<AutoSku> list = BeanMapper.mapList(autoSkus, AutoSku.class);

            for (AutoSku a : autoSkuNow) {
                boolean c = true;
                for (AutoSku s:autoSkus) {
                    if (a.getSkuCode().equals(s.getSkuCode())) {
                        c = false;
                        break;
                    }
                }
                if (c) {
                    AutoSku autoSku = new AutoSku();
                    autoSku.setAutoed("0");
                    autoSku.setSkuCode(a.getSkuCode());
                    autoSku.setTitle(a.getTitle());
                    autoSku.setUnit(a.getUnit());
                    list.add(autoSku);
                }
            }
            if (list.size() != autoSkus.size()) {
                RedisUtil.set("autoPush:items", JSONUtil.obj2json(list));
                autoPushItem.setAutoSkus(list);
            } else {
                autoPushItem.setAutoSkus(autoSkus);
            }
        }
        autoPushItem.setOnline(on);
        autoPushItem.setAcOnline(acOn);
        return ResponseEntity.ok(successResult(autoPushItem));
    }

    @ApiOperation(value = "开启/关闭自动推单,0开启，1关闭", notes = "开启/关闭自动推单，0开启，1关闭")
    @RequestMapping(value = "/on/off", method = RequestMethod.GET)
    public ResponseEntity<MallResponse<String>> onOffPush(@RequestParam("online") String online) {
        RedisUtil.set("autoPush:online", online);
        String on = RedisUtil.get("autoPush:online");
        if ("0".equals(on)) {
            mallOrderToPushService.toPush();
        }
        return ResponseEntity.ok(successResult(on));
    }

    @ApiOperation(value = "（活动订单开关）开启/关闭自动推单,0开启，1关闭", notes = "（活动订单开关）开启/关闭自动推单,0开启，1关闭")
    @RequestMapping(value = "/ac/on/off", method = RequestMethod.GET)
    public ResponseEntity<MallResponse<String>> onAcOffPush(@RequestParam("online") String online) {
        RedisUtil.set("autoPush:acOnline", online);
        String on = RedisUtil.get("autoPush:acOnline");
        if ("0".equals(on)) {
            mallOrderToPushService.toAcPush();
        }
        return ResponseEntity.ok(successResult(on));
    }

    @ApiOperation(value = "选中/取消推单产品 0选中，1取消选中", notes = "选中/取消推单产品，0选中，1取消选中")
    @RequestMapping(value = "/selected/item", method = RequestMethod.POST)
    public ResponseEntity<MallResponse<Boolean>> onOffItem(@RequestBody List<AutoItemParam> autoItemParam) {
        String items = RedisUtil.get("autoPush:items");
        List<AutoSku> autoSkus = JSONUtil.json2list(items, AutoSku.class);
        autoSkus.forEach(p -> {
            autoItemParam.forEach(f -> {
                if (p.getSkuCode().equals(f.getSkuCode())) {
                    p.setAutoed(f.getAutoed());
                }
            });
        });
        RedisUtil.set("autoPush:items", JSONUtil.obj2json(autoSkus));
        return ResponseEntity.ok(successResult(true));
    }




}
