package com.meifute.core.util;

import com.google.common.base.Joiner;
import com.jd.open.api.sdk.domain.ECLP.EclpOpenService.response.queryPageSerialByBillNo.SerialNumber;
import com.jd.open.api.sdk.response.ECLP.EclpSerialQueryPageSerialByBillNoResponse;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallSkuSpec;
import com.meifute.core.entity.MallUser;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallOrderItemMapper;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.model.LySerialNumberOff;
import com.meifute.core.model.jdtracesource.AddOutOrder;
import com.meifute.core.model.jdtracesource.AddOutRecord;
import com.meifute.core.model.jdtracesource.DETAIL_LIST;
import com.meifute.core.service.*;
import com.meifute.core.vo.OrderInfoMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @Classname TaskService
 * @Description TODO
 * @Date 2020-04-22 14:42
 * @Created by MR. Xb.Wu
 */
@Slf4j
@Component
public class TaskService {

    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private OrderDelayService orderDelayService;
    @Autowired
    private JDExpressPushService jdExpressPushService;

    @Autowired
    private TraceabilityService traceabilityService;
    @Autowired
    private MallOrderItemMapper mallOrderItemMapper;
    @Autowired
    private MallOrderItemService mallOrderItemService;

    /**
     * 轮询推送漏推的订单
     */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void getLeakageSlipOrder() {
        MallUser mallUser = new MallUser();
        mallUser.setId("getLeakageSlipOrder");
        this.getLeakageSlipOrder(mallUser);
    }

    @RedisLock(key = "id", sync = true)
    public void getLeakageSlipOrder(MallUser mallUser) {
        log.info("每10分钟轮询一次，搜索漏单的推单。。。");
        List<String> orderIds = mallOrderInfoMapper.getLeakageSlipOrder();
        if (orderIds == null || orderIds.size() == 0) {
            return;
        }
        log.info("漏推的数量:{}", orderIds.size());
        OrderInfoMessage orderInfoMessage = new OrderInfoMessage();
        orderInfoMessage.setOrderOrigin(0);
        for (String p : orderIds) {
            orderInfoMessage.setOrderId(p);
            orderDelayService.delayExpressListener(orderInfoMessage);
        }
    }

    /**
     * 每5分钟轮训查询已发货的商品序列号
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void getNoSerialNumberOrderInfo() {
        MallUser mallUser = new MallUser();
        mallUser.setId("getLeakageSlipOrder");
        this.getNoSerialNumberOrderInfo(mallUser);
    }

    @RedisLock(key = "id", sync = true)
    public void getNoSerialNumberOrderInfo(MallUser mallUser) {
        log.info("开始执行溯源码记录任务。。。。。。");
        String Ly_SerialNumber_Off = RedisUtil.get("Ly_SerialNumber_Off");
        if (Ly_SerialNumber_Off == null) {
            log.info("执行溯源码记录任务。。开关数据未初始化。。。。");
            return;
        }
        LySerialNumberOff serialNumberOff = JsonUtils.jsonToPojo(Ly_SerialNumber_Off, LySerialNumberOff.class);
        if (serialNumberOff == null || !serialNumberOff.getOn()) {
            log.info("执行溯源码记录任务。。开关已关闭。。。。");
            return;
        }
        if (serialNumberOff.getStartTime() == null) {
            log.info("执行溯源码记录任务。。结算时间不能为空。。。。");
            return;
        }
        if (RedisUtil.get("getNoSerialNumberOrderInfo-lock") == null) {
            RedisUtil.set("getNoSerialNumberOrderInfo-lock", "1", 120);
        } else {
            log.info("***********************************其他服务正在执行此溯源码定时任务");
            return;
        }
        List<MallOrderInfo> noSerialNumberOrderInfo = mallOrderInfoMapper.getNoSerialNumberOrderInfo(serialNumberOff.getStartTime());
        if (CollectionUtils.isEmpty(noSerialNumberOrderInfo)) {
            return;
        }
        log.info("=========noSerialNumberOrderInfo长度:{}", noSerialNumberOrderInfo.size());

        List<MallSkuSpec> itemSpec = mallOrderItemMapper.getItemSpec();
        Map<String, List<MallSkuSpec>> itemSpecMap = itemSpec.stream().collect(Collectors.groupingBy(MallSkuSpec::getTransportGoodsNo));
        Map<String, List<MallOrderInfo>> listMap = noSerialNumberOrderInfo.stream().collect(Collectors.groupingBy(MallOrderInfo::getEclpSoNo));

        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
        try {
            listMap.forEach((k, v) -> {
                pool.execute(() -> {
                    try {
                        List<SerialNumber> serialNumbers = queryPageSerialByBillNo(k);
                        if (serialNumbers.size() > 0) {
                            log.info("=========serialNumbers长度:{}", serialNumbers.size());
                            //创建出库单
                            MallOrderInfo orderInfo = v.get(0);
                            List<String> addressIdByName = mallOrderItemMapper.getAddressIdByName(orderInfo.getMallUserId(), orderInfo.getAddrName().trim());
                            String circsiteId = addressIdByName == null ? orderInfo.getMallUserId() : addressIdByName.get(0);
                            Map<String, List<SerialNumber>> map = serialNumbers.stream().collect(Collectors.groupingBy(SerialNumber::getGoodsNo));
                            String status = addOutOrderNew(itemSpecMap, map, v, circsiteId);
                            if ("0".equals(status)) {
                                //提交出库单
                                addOutRecord(itemSpecMap, orderInfo, map, circsiteId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("执行溯源码记录异常:{0}", e);
                    }
                });
            });
        } catch (Exception e) {
            log.error("" + e);
        } finally {
            pool.shutdown();
        }
    }

    private String addOutOrderNew(Map<String, List<MallSkuSpec>> itemSpecMap, Map<String, List<SerialNumber>> serialMap, List<MallOrderInfo> v, String circsiteId) {
        AddOutOrder addOutOrder = new AddOutOrder();
        MallOrderInfo orderInfo = v.get(0);
        addOutOrder.setORDER_NO(Long.parseLong(orderInfo.getOrderId()));
        addOutOrder.setTO_CIRCSITE_ID(circsiteId);
        addOutOrder.setTO_CIRCSITE_NAME(orderInfo.getAddrName());
        addOutOrder.setAMOUNT(orderInfo.getPaymentAmt().doubleValue());
        addOutOrder.setFREIGHT(orderInfo.getPostFeeAmt().doubleValue());
        addOutOrder.setTO_CIRCSITE_NO(orderInfo.getMallUserId());
        addOutOrder.setName(orderInfo.getAddrName());
        addOutOrder.setTel(orderInfo.getAddrPhone());
        addOutOrder.setProvinceName(orderInfo.getProvincialUrbanArea().split(" ")[0]);
        addOutOrder.setCityName(orderInfo.getProvincialUrbanArea().split(" ")[1]);
        if (orderInfo.getProvincialUrbanArea().split(" ").length < 3) {
            addOutOrder.setExpAreaName(orderInfo.getProvincialUrbanArea().split(" ")[1]);
        } else {
            addOutOrder.setExpAreaName(orderInfo.getProvincialUrbanArea().split(" ")[2]);
        }
        addOutOrder.setAddress(orderInfo.getAddrId());
        List<DETAIL_LIST> detail_lists = new ArrayList<>();

        AtomicInteger index = new AtomicInteger(1);
        boolean result = serialMap.entrySet().stream().anyMatch(e -> {
            MallSkuSpec mallSkuSpec = itemSpecMap.get(e.getKey()).get(0);
            String productNo = mallSkuSpec.getTransportCode();
            if (StringUtils.isEmpty(productNo)) {
                log.error("===============" + mallSkuSpec.getSkuCode() + "没有对应的新编码号。。。。");
                return false;
            }
            DETAIL_LIST detail = new DETAIL_LIST();
            detail.setORDER_DETAIL_NO(Long.parseLong(orderInfo.getOrderId().substring(0, 17) + index));
            detail.setPRODUCT_ID(Long.parseLong(mallSkuSpec.getId()));
            detail.setPRODUCT_NAME(mallSkuSpec.getTitle());
            detail.setPRODUCT_NO(productNo);
            detail.setSPEC(mallSkuSpec.getSpec());
            detail.setWEIGHT(mallSkuSpec.getWeight());
            detail.setVOLUME(0.05);
            detail.setUNIT(mallSkuSpec.getUnit());
            detail.setCOUNT(e.getValue().size());
            detail_lists.add(detail);
            index.incrementAndGet();
            return true;
        });
        if (!result) {
            return "1";
        }
        addOutOrder.setDETAIL_LIST(detail_lists);
        return traceabilityService.addOutOrder(addOutOrder);
    }


    private List<SerialNumber> queryPageSerialByBillNo(String k) {
        int pageNo = 1;
        List<SerialNumber> serialNumbers = new ArrayList<>();
        for (; ; ) {
            EclpSerialQueryPageSerialByBillNoResponse response = jdExpressPushService.queryPageSerialByBillNo(k, pageNo, 20);
            if (response != null && "0".equals(response.getCode())) {
                if (response.getQuerypageserialbybillnoResult().getSerialNumbers().size() == 0) {
                    break;
                }
                serialNumbers.addAll(response.getQuerypageserialbybillnoResult().getSerialNumbers());
            } else {
                break;
            }
            ++pageNo;
        }
        return serialNumbers;
    }


    private void addOutRecord(Map<String, List<MallSkuSpec>> itemSpecMap, MallOrderInfo orderInfo, Map<String, List<SerialNumber>> serialMap, String circsiteId) {
        try {
            AtomicInteger index = new AtomicInteger(1);
            serialMap.forEach((k, v) -> {
                List<MallSkuSpec> skuSpecs = itemSpecMap.get(k);
                if (skuSpecs == null) {
                    log.info("本平台没有相关产品");
                    return;
                }
                if (!"C034-H".equals(skuSpecs.get(0).getSkuCode().trim())
                        && !"C033-H".equals(skuSpecs.get(0).getSkuCode().trim())
                        && !"C033-X".equals(skuSpecs.get(0).getSkuCode().trim())
                        && !"C034-X".equals(skuSpecs.get(0).getSkuCode().trim())) {
                    log.info("不符合有效追溯码:{},{}", skuSpecs.get(0).getSkuCode().trim(), skuSpecs.get(0).getTitle());
                    return;
                }
                List<String> traceCodeList = v.stream().map(SerialNumber::getSerialNumber).collect(Collectors.toList());
                //上传连阳系统
                AddOutRecord addOutRecord = new AddOutRecord();
                addOutRecord.setORDER_NO(Long.parseLong(orderInfo.getOrderId()));
                addOutRecord.setTO_CIRCSITE_ID(circsiteId);
                addOutRecord.setTRACECODELIST(traceCodeList);
                addOutRecord.setACCESS_UUID(orderInfo.getOrderId() + index);
                String status = traceabilityService.addOutRecord(addOutRecord);
                index.incrementAndGet();
                if ("0".equals(status) || "12".equals(status)) {
                    List<String> securityNumbers = new ArrayList<>();
                    traceCodeList.forEach(p -> {
                        String securityNumber = traceabilityService.getSecurityNumber(p);
                        if (securityNumber != null) {
                            securityNumbers.add(securityNumber);
                        }
                    });
                    //防伪码
                    String securityNumber = Joiner.on(",").join(securityNumbers);
                    // 溯源码
                    String serialNumber = Joiner.on(",").join(traceCodeList);
                    // 更新orderItem表
                    updateSerialNumber(itemSpecMap, k, orderInfo, serialNumber, securityNumber);
                } else if ("10".equals(status)) { // 有溯源码，但溯源码与连洋不匹配
                    updateSerialNumber(itemSpecMap, k, orderInfo, "暂无溯源码", "暂无防伪码");
                }
            });
        } catch (Exception e) {
            log.info("=======>上传连阳系统异常:{0}", e);
        }
    }

    private void updateSerialNumber(Map<String, List<MallSkuSpec>> itemSpecMap, String k, MallOrderInfo orderInfo, String serialNumber, String securityNumber) {
        List<String> orderItemIds = null;
        if (itemSpecMap != null) {
            for (MallSkuSpec spec : itemSpecMap.get(k)) {
                orderItemIds = mallOrderItemMapper.getOrderItemBySkuAndOrderId(orderInfo.getOrderId(), spec.getSkuCode());
                if (CollectionUtils.isEmpty(orderItemIds)) {
                    orderItemIds = mallOrderItemMapper.getOrderItemBySkuAndOrderId(orderInfo.getOrderId(), spec.getStructureSku());
                }
                if (CollectionUtils.isEmpty(orderItemIds)) {
                    orderItemIds = mallOrderItemMapper.getOrderItemBySkuAndOrderId(orderInfo.getOrderId(), spec.getRelationSku());
                }
                if (!CollectionUtils.isEmpty(orderItemIds)) {
                    break;
                }
            }
        }
        if (orderItemIds != null) {
            mallOrderItemMapper.updateSerialNumber(serialNumber, orderItemIds.get(0), securityNumber);
        }
    }

    private void updateSerialNumber(List<String> orderItemIds, String serialNumber, String securityNumber) {
        mallOrderItemMapper.updateSerialNumberByBatechId(serialNumber, securityNumber, orderItemIds);
    }

    /*@Scheduled(cron = "0 0 17 * * ?")
    public void sendMail() throws Exception {
        log.info("=============开始给领导发邮件");
        Boolean aBoolean = mallOrderInfoService.sendEMail();
        log.info("=============邮件发送结果:{}", aBoolean);
    }*/

    /**
     * 更新原来没有防伪码的记录
     */
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public void updateSecurityNumber() {
        MallUser mallUser = new MallUser();
        mallUser.setId("updateSecurityNumber");
        this.updateSecurityNumber(mallUser);
    }

    @RedisLock(key = "id", sync = true)
    public void updateSecurityNumber(MallUser mallUser) {
        log.info("开始执行溯源码记录任务。。。。。。");
        String Ly_SerialNumber_Off = RedisUtil.get("Ly_SerialNumber_Off");
        if (Ly_SerialNumber_Off == null) {
            log.info("执行溯源码记录任务。。开关数据未初始化。。。。");
            return;
        }
        LySerialNumberOff serialNumberOff = JsonUtils.jsonToPojo(Ly_SerialNumber_Off, LySerialNumberOff.class);
        if (serialNumberOff == null || !serialNumberOff.getOn()) {
            log.info("执行溯源码记录任务。。开关已关闭。。。。");
            return;
        }
        List<MallOrderItem> serialNumbers = mallOrderItemMapper.getIdByNullSecurityNumberAndSerialNumber();
        if (!CollectionUtils.isEmpty(serialNumbers)) {
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);
            try {
                serialNumbers.forEach(p -> {
                    pool.execute(() -> {
                        try {
                            String[] strings = p.getSerialNumber().split(",");
                            List<String> securityNumbers = new ArrayList<>();
                            for (String s : strings) {
                                String securityNumber = traceabilityService.getSecurityNumber(s);
                                if (securityNumber != null) {
                                    securityNumbers.add(securityNumber);
                                }
                            }
                            if (securityNumbers.size() > 0) {
                                String securityNumber = Joiner.on(",").join(securityNumbers);
                                mallOrderItemMapper.updateSerialNumber(p.getSerialNumber(), p.getId(), securityNumber);
                            }
                        } catch (Exception e) {
                            log.info("更新异常:{0}", e);
                        }
                    });
                });
            } finally {
                pool.shutdown();
            }
        }
    }

}
