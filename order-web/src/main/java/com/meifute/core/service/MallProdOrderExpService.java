package com.meifute.core.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.google.common.collect.Lists;
import com.meifute.core.entity.ExpressCompany;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallUser;
import com.meifute.core.entity.order.AsyncTaskInfo;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.ExpressCompanyMapper;
import com.meifute.core.mapper.MallOrderFeedBackMapper;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallOrderItemMapper;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.date.DateUtils;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.enums.MallTeamEnum;
import com.meifute.core.mmall.common.utils.ExcelUtil;
import com.meifute.core.mmall.common.utils.OSSClientUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.vo.BranchOfficeRp;
import com.meifute.core.vo.GetOrderPageListParam;
import com.meifute.core.vo.order.AsyncTaskParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MallProdOrderExpService {
    @Autowired
    private ItemFeign itemFeign;

    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    MallOrderItemMapper mallOrderItemMapper;

    @Autowired
    ExpressCompanyMapper expressCompanyMapper;

    @Autowired
    MallOrderFeedBackMapper mallOrderFeedBackMapper;

    @Autowired
    private AgentFeign agentFeign;

    @Autowired
    private OSSClientUtil ossClientUtil;

    @Autowired
    AsyncTaskInfoService asyncTaskInfoService;

    private Lock lock = new ReentrantLock();

    private static final Integer MAX_DEAL_TIME = 60;//以分为单位

    public void exportExecl(AsyncTaskParam exportParam) throws Exception {
        if (lock.tryLock()) {
            try {
                asyncTaskInfoService.updateById(AsyncTaskInfo.builder()
                        .id(exportParam.getTaskId())
                        .dealStartTime(new Date())
                        .maxDealTime(MAX_DEAL_TIME)
                        .status(MallStatusEnum.TASK_STATUS_002.getCode()).build());
                MallOrderInfo mallOrderInfo = new MallOrderInfo();
                GetOrderPageListParam param = JSONObject.parseObject(exportParam.getParam(), GetOrderPageListParam.class);
                BeanUtils.copyProperties(param, mallOrderInfo);
                mallOrderInfo.setOrderStatusList(param.getOrderStatusList());
                mallOrderInfo.setPageCurrent(param.getPageCurrent());
                mallOrderInfo.setPageSize(param.getPageSize());
                mallOrderInfo.setBeginTime(param.getBeginTime());
                mallOrderInfo.setEndTime(param.getEndTime());
                log.info(">>>>>>>产品订单导出，参数：{}", JSONObject.toJSONString(mallOrderInfo));
                List<MallUser> userByInput = null;
                Integer i = new Random().nextInt(9999 - 1000 + 1) + 1000;
                if (MallPreconditions.checkNullBoolean(Arrays.asList(param.getNickName(), param.getName(), param.getPhone()))) {
                    userByInput = userFeign.getUserByInput(param.getNickName(), param.getName(), param.getPhone(), null, null);
                    if (CollectionUtils.isEmpty(userByInput)) {
                        updateAsyncTaskInfo(exportParam.getTaskId(), "not found user ", MallStatusEnum.TASK_STATUS_003.getCode());
                        return;
                    }
                }
                List<String> userIdList = new ArrayList<>();
                if (!CollectionUtils.isEmpty(userByInput)) {
                    for (MallUser user : userByInput) {
                        userIdList.add(user.getId());
                    }
                    mallOrderInfo.setMallUserIdList(userIdList);
                }
                Integer count = mallOrderInfoMapper.doExcelCount(mallOrderInfo);
                if (count ==0) {
                    updateAsyncTaskInfo(exportParam.getTaskId(), "no data to export ", MallStatusEnum.TASK_STATUS_003.getCode());
                    return;
                }
                if (count > ExcelUtil.ROW_MAX) {
                    log.info("MallOrderInfoService.doExcel infos size:{}", count);
                    updateAsyncTaskInfo(exportParam.getTaskId(), "data too large", MallStatusEnum.TASK_STATUS_003.getCode());
                    return;
                }
                List<MallOrderInfo> infos = mallOrderInfoMapper.doExcel(mallOrderInfo);
                log.info(">>>>>>>产品订单导出，结果：{}", JSONObject.toJSONString(infos));
                fillValues(infos);
                infos = detailInfos(infos);
                ExcelUtil excelUtil = new ExcelUtil();
                LinkedHashMap<String, String> map = getMap();
                HSSFWorkbook workbook = excelUtil.buildWorkbook(map, infos, ExcelUtil.DEFAULT_ROW_MAX);
                updateAsyncTaskInfo(exportParam.getTaskId(), uploadExeclToOss(i, workbook), MallStatusEnum.TASK_STATUS_004.getCode());
            } catch (Exception e) {
                throw e;
            } finally {
                lock.unlock();
            }
        } else {
            return;
        }

    }

    private String uploadExeclToOss(Integer i, HSSFWorkbook workbook) throws IOException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        workbook.write(stream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
        StringBuffer sb = new StringBuffer();
        sb.append("产品订单")
                .append(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now()))
                .append("_")
                .append(i)
                .append(".xlsx");
        return ossClientUtil.uploadExcelFile2OSS(inputStream, sb.toString());
    }

    private void updateAsyncTaskInfo(Integer taskId, String result, String status) {
        asyncTaskInfoService.updateById(AsyncTaskInfo.builder()
                .id(taskId)
                .result(result)
                .dealEndTime(new Date())
                .status(status).build());
    }

    private void fillValues(List<MallOrderInfo> infos) {
        List<String> skuCodes = infos.stream().map(vo -> vo.getSkuCode()).distinct().collect(Collectors.toList());
        List<String> userIds = infos.stream().map(vo -> vo.getMallUserId()).distinct().collect(Collectors.toList());
        Map<String, MallSku> skuMap = null;
        Map<String, MallUser> userMap = null;
        if (ObjectUtils.isNotNullAndEmpty(skuCodes)) {
            skuMap = itemFeign.getSkusBySkuCodes(skuCodes);
        }
        if (ObjectUtils.isNotNullAndEmpty(userIds)) {
            userMap = userFeign.queryUsersByUserIds(userIds);
        }
        Map<String, BigDecimal> totalAmountMap = new HashMap<>();
        Map<String, BigDecimal> totalPriceMap = new HashMap<>();
        for (MallOrderInfo orderInfo : infos) {
            if (totalAmountMap.containsKey(orderInfo.getOrderId())) {
                totalAmountMap.put(orderInfo.getOrderId(), totalAmountMap.get(orderInfo.getOrderId()).add(new BigDecimal(orderInfo.getItemNumber())));
            } else {
                totalAmountMap.put(orderInfo.getOrderId(), new BigDecimal(orderInfo.getItemNumber()));
            }
            if (totalPriceMap.containsKey(orderInfo.getOrderId())) {
                totalPriceMap.put(orderInfo.getOrderId(), totalPriceMap.get(orderInfo.getOrderId()).add(new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber()))));
            } else {
                totalPriceMap.put(orderInfo.getOrderId(), new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber())));
            }
        }
        doFillValues(infos, skuMap, userMap, totalAmountMap, totalPriceMap);
    }

    private void doFillValues(List<MallOrderInfo> infos, Map<String, MallSku> skuMap, Map<String, MallUser> userMap, Map<String, BigDecimal> totalAmountMap, Map<String, BigDecimal> totalPriceMap) {
        for (MallOrderInfo orderInfo : infos) {
            if (null != skuMap) {
                if (skuMap.containsKey(orderInfo.getSkuCode())) {
                    orderInfo.setTitle(skuMap.get(orderInfo.getSkuCode()).getTitle());
                }
            }
            if (null != userMap) {
                if (userMap.containsKey(orderInfo.getMallUserId())) {
                    orderInfo.setNickName(userMap.get(orderInfo.getMallUserId()).getNickName());
                    orderInfo.setPhone(userMap.get(orderInfo.getMallUserId()).getPhone());
                    orderInfo.setRoleId(userMap.get(orderInfo.getMallUserId()).getRoleId());
                    orderInfo.setNickName(userMap.get(orderInfo.getMallUserId()).getNickName());
                }
            }
            if (null != totalAmountMap) {
                if (totalAmountMap.containsKey(orderInfo.getOrderId())) {
                    orderInfo.setItemTotalNum(totalAmountMap.get(orderInfo.getOrderId()).toString());
                }
            }
            if (null != totalPriceMap) {
                if (totalPriceMap.containsKey(orderInfo.getOrderId())) {
                    orderInfo.setItemTotalAmount(totalPriceMap.get(orderInfo.getOrderId()).toString());
                }
            }
            orderInfo.setItemAmount(new BigDecimal(orderInfo.getPrice()).multiply(new BigDecimal(orderInfo.getItemNumber())).toString());
        }
    }


    public List<MallOrderInfo> detailInfos(List<MallOrderInfo> infos) {

        HashMap<String, MallSku> giftRelationSku = new HashMap<>();
        giftRelationSku.put("30290030", MallSku.builder().skuCode("40000061").title("米浮喷雾瓶50ml（空瓶）").build());
        giftRelationSku.put("30290020", MallSku.builder().skuCode("40000062").title("米浮手枪泵").build());
        giftRelationSku.put("30030040", MallSku.builder().skuCode("C036-H").title("米浮泡沫皮肤抗菌液(支)").build());

        List<ExpressCompany> expressCompanyList = expressCompanyMapper.selectList(new EntityWrapper<ExpressCompany>().eq("is_del", "0"));
        Map<String, String> expressCompanyMap = expressCompanyList.stream().collect(Collectors.toMap(ExpressCompany::getCode, ExpressCompany::getCompanyName));

        Date d1 = DateUtils.parseDate("2020-02-20 05:18:00");
//        Map<String, List<MallOrderInfo>> listMap = infos.stream().filter(p -> Arrays.asList("C036-H", "C036-Z").contains(p.getSkuCode())).collect(Collectors.groupingBy(MallOrderInfo::getOrderId));
        List<String> companyIds = infos.stream().map(vo -> vo.getCompanyId()).distinct().collect(Collectors.toList());
        List<BranchOfficeRp> branchOfficeRps = getCompanys(companyIds);
        for (MallOrderInfo info : infos) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH时mm分ss秒");
            String createDate = "";
            if (info.getCreateDate() != null) {
                createDate = simpleDateFormat.format(info.getCreateDate());
                info.setCreateDateString(createDate);
            }
            if ("0".equals(info.getOrderType()) || "1".equals(info.getOrderType())) {
                info.setDirectlyBuy("直购");
            } else {
                info.setDirectlyBuy("非直购");
            }

            info.setOrderStatus(MallOrderStatusEnum.explain(info.getOrderStatus()));
            info.setRoleId(MallTeamEnum.explain(info.getRoleId()));
            // 所属分公司
            info.setCompanyName(getCompanyName(branchOfficeRps, info.getCompanyId()));
            if ("0".equals(info.getOrderType()) || "1".equals(info.getOrderType())) {
                info.setDirectlyBuy("直购");
            } else {
                info.setDirectlyBuy("非直购");
            }
            if (ObjectUtils.isNotNullAndEmpty(info.getLogisticsMode())) {
                String logisticsModeName = expressCompanyMap.get(info.getLogisticsMode());
                info.setLogisticsModeName(ObjectUtils.isNotNullAndEmpty(logisticsModeName) ? logisticsModeName : "");
            }
            if (ObjectUtils.isNotNullAndEmpty(info.getLogisticsType())) {
                switch (info.getLogisticsType()) {
                    case "0":
                        info.setLogisticsTypeName("京东");
                        break;
                    case "1":
                        info.setLogisticsTypeName("顺丰");
                        break;
                }
            } else {
                info.setLogisticsTypeName("");
            }
            MallSku giftSku = giftRelationSku.get(info.getSkuCode());
            if (ObjectUtils.isNotNullAndEmpty(giftSku)) {
                if (!"30290020".equals(info.getSkuCode()) || info.getCreateDate().after(d1)) {
                    info.setGiftSku(giftSku.getSkuCode());
                    info.setGiftName(giftSku.getTitle());
                    info.setGiftQuantity(info.getItemNumber());
                }
            }
//            if (Arrays.asList("0", "2").contains(info.getOrderType())) {
//                getGiftSku(info);
//            }
            info.setOrderType(MallOrderTypeEnum.explain(info.getOrderType()));

        }
        return infos;
    }

    public LinkedHashMap<String, String> getMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("订单编号", "orderId");
        map.put("下单时间", "createDateString");
        map.put("状态", "orderStatus");
        map.put("订单类型", "orderType");
        map.put("代理昵称", "nickName");
        map.put("代理手机号", "phone");
        map.put("代理等级", "roleId");
        map.put("收件人姓名", "addrName");
        map.put("收件人手机号", "addrPhone");
        map.put("收件人地址", "addrId");
        map.put("商品数", "itemNumber");
        map.put("运费", "postFeeAmt");
        map.put("物流单号", "expressCode");
        map.put("商品名称", "title");
        map.put("商品SKU", "skuCode");
        map.put("商品价格", "price");
        map.put("数量", "itemNumber");
        map.put("小计", "itemAmount");
        map.put("赠品名称", "giftName");
        map.put("赠品Sku", "giftSku");
        map.put("赠品数量", "giftQuantity");
        map.put("本单数量总计", "itemTotalNum");
        map.put("本单总金额", "itemTotalAmount");
        map.put("买家备注", "buyerMemo");
        map.put("卖家备注", "csMemo");
        map.put("所属公司", "companyName");
        map.put("是否直购", "directlyBuy");
        map.put("物流方式", "logisticsModeName");
        map.put("物流发货方式", "logisticsTypeName");
        map.put("溯源码", "serialNumber");
        return map;
    }

    private String getCompanyName(List<BranchOfficeRp> branchOfficeRps, String companyId) {
        String companyName = "";
        if (CollectionUtils.isEmpty(branchOfficeRps)) {
            return companyName;
        }
        for (BranchOfficeRp branchOfficeRp : branchOfficeRps) {
            if (com.meifute.core.mmall.common.utils.StringUtils.equals(branchOfficeRp.getCompanyId(), companyId)) {
                companyName = branchOfficeRp.getCompanyName();
                break;
            }
        }
        return companyName;
    }

    private List<BranchOfficeRp> getCompanys(List<String> companyIds) {
        List<BranchOfficeRp> branchOfficeRps = Lists.newArrayList();
        if (ObjectUtils.isNotNullAndEmpty(companyIds)) {
            branchOfficeRps = agentFeign.getCompanyNamesByIds(companyIds);
        }
        return branchOfficeRps;
    }
}
