package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallSkuSpec;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.mapper.MallOrderItemMapper;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.HttpPostXml;
import com.meifute.core.test.QimenClient;
import com.meifute.core.vo.ItemParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import tech.simmy.qimen.request.ContactInfo;
import tech.simmy.qimen.request.DeliveryOrderCreateRequest;
import tech.simmy.qimen.request.EntryOrderCreateRequest;
import tech.simmy.qimen.request.SingleItemSynchronizeRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @auther liuliang
 * @date 2020/2/6 12:00 PM
 */
@Slf4j
@Service
public class QimenPushService {

//    @Autowired
//    private MallOrderInfoMapper orderInfoMapper;

    @Autowired
    private MallOrderItemMapper orderItemMapper;

    @Autowired
    private ItemFeign itemFeign;

    @Autowired
    private MallOrderInfoService orderInfoService;


    public static void main(String[] args) {
        QimenPushService qimenPushService = new QimenPushService();
        qimenPushService.syncQimenItem("P001");
    }

    private MallSkuSpec getSpec(String skuCode) {
        return itemFeign.getSkuSpecBySkuCodeAndExpressTypeNo(skuCode, "1");
    }

    /**
     * 将我方商品同步到奇门,一次即可
     *
     * @param skuCode
     * @return
     */
    public Boolean syncQimenItem(String skuCode) {
        QimenClient qimenClient = new QimenClient();
        MallSku sku = itemFeign.getSkuByCodeNo(skuCode);
        //同步商品
        SingleItemSynchronizeRequest.Item item = new SingleItemSynchronizeRequest.Item();
        item.setItemCode(skuCode);
        item.setItemName(sku.getTitle());
        item.setBarCode(sku.getId());//条形码，给id吧
        item.setItemType("ZC");
        //调用有错误qimenClient会抛出来，这边不用做处理
        qimenClient.syncItem(item);
        return true;
    }


    /************************我方推单调用***************************
     * **********************我方推单调用***************************
     * **********************我方推单调用***************************
     * 将订单为orderId里面的sku是skuCode的入库奇门
     * @param orderInfo
     * @return
     */
    public Boolean createEntryOrder(MallOrderInfo orderInfo) {
        if (!"3".equals(orderInfo.getOrderStatus())) {
            return true;
        }
        if (!"1".equals(orderInfo.getLogisticsMode())) {
            return true;
        }

        try {
            QimenClient qimenClient = new QimenClient();

            String orderId = orderInfo.getOrderId();

            List<MallOrderItem> orderItems = orderItemMapper.selectList(
                    new EntityWrapper<MallOrderItem>()
                            .eq("order_id", orderId)
                            .eq("is_del", 0));

            if (CollectionUtils.isEmpty(orderItems)) {
                log.info("===================>>>>>>>>>>订单号为:{},没有sku的数据，不用向奇门推单", orderId);
                return false;
            }

            int amount = 0;
            String skuCode = "";
            for (MallOrderItem item : orderItems) {
                if ("C036-Z".equals(item.getSkuCode().trim())) {
                    amount = amount + item.getAmount().abs().intValue();
                    skuCode = item.getSkuCode();
                    break;
                }
                MallSkuSpec spec = getSpec(item.getSkuCode());
                if (item.getSkuCode().equals(spec.getRelationSku())) {
                    amount = amount + item.getAmount().abs().intValue();
                    skuCode = spec.getSixCode();
                } else {
                    amount = amount + item.getAmount().abs().intValue() * Integer.parseInt(spec.getSpec());
                    MallSkuSpec reSpec = getSpec(spec.getRelationSku());
                    skuCode = reSpec.getSixCode();
                }
                if ("30030050".equals(item.getSkuCode().trim())) {
                    skuCode = spec.getRelationSku();
                }
            }

            if (amount == 0) {
                log.info("===================>>>>>>>>>>订单号为:{}, sku有误", orderId);
                return false;
            }

            EntryOrderCreateRequest.EntryOrder entryOrder = new EntryOrderCreateRequest.EntryOrder();
            entryOrder.setEntryOrderCode(orderId);
            entryOrder.setOrderType("CGRK");
            entryOrder.setWarehouseCode(HttpPostXml.warehouseCode);

            //todo 发货地址 建议统一为仓库发货
            ContactInfo senderInfo = new ContactInfo();
            senderInfo.setCompany("美浮特");
            senderInfo.setProvince("上海市");
            senderInfo.setCity("上海市");
            senderInfo.setArea("金山区");
            senderInfo.setName("上海美浮特");
            senderInfo.setDetailAddress("金山工业区天工路285弄18号楼仓储部");
            senderInfo.setMobile("021-33691306");
            entryOrder.setSenderInfo(senderInfo);

            //todo 收货地址解析
            ContactInfo receiverInfo = new ContactInfo();
            receiverInfo.setCompany("新亦源");
            receiverInfo.setProvince("上海市");
            receiverInfo.setCity("上海市");
            receiverInfo.setArea("青浦区");
            receiverInfo.setName("查卫根");
            receiverInfo.setDetailAddress("汇金路889号中华印刷厂2号楼3楼");
            receiverInfo.setMobile("18521737603");
            entryOrder.setReceiverInfo(receiverInfo);

            //入库的商品
            ArrayList<EntryOrderCreateRequest.OrderLine> lines = new ArrayList<>();

            EntryOrderCreateRequest.OrderLine orderLine = new EntryOrderCreateRequest.OrderLine();
            orderLine.setOwnerCode(HttpPostXml.owner);
            orderLine.setItemCode(skuCode);
            orderLine.setPlanQty(amount);
            orderLine.setInventoryType("ZC");
            lines.add(orderLine);

            entryOrder.setTotalOrderLines(orderItems.size());
            entryOrder.setOrderCreateTime(LocalDateTime.now()); // 入库日期

            entryOrder.setRemark("订单入库单" + orderId);
            //调用有错误qimenClient会抛出来，这边不用做处理
            qimenClient.createEntryOrder(entryOrder, lines);

            //成功修改状态
            orderInfo.setOrderStatus("4");
            orderInfo.setQmType("1");
            orderInfo.setIsCanCancel("1");
            orderInfo.setExpressCompany("顺丰物流");
            orderInfo.setUpdateDate(new Date());
            orderInfo.setDeliverGoodsDate(new Date());
            boolean b = orderInfoService.updateOrderByIdNew(orderInfo);
            if (!b) {
                return false;
            }
        } catch (Exception e) {
            log.info("新亦源入库失败" + e);
            return false;
        }
        return true;
    }


    /**
     * 奇门真正的推单发货
     *
     * @param orderInfo
     * @param itemList
     * @return
     */
    public Boolean DeliveryOrder(MallOrderInfo orderInfo, List<MallOrderItem> itemList) {

        if (!"3".equals(orderInfo.getOrderStatus())) {
            return true;
        }
        if (!"1".equals(orderInfo.getLogisticsMode())) {
            return true;
        }

        try {
            QimenClient qimenClient = new QimenClient();

            List<ItemParam> allItems = new ArrayList<>();

            //todo 买一赠一
//            buyOneGetOneFree(orderInfo.getPayDate(), itemList);

            for (MallOrderItem item : itemList) {
                int amount = 0;
                MallSkuSpec spec = getSpec(item.getSkuCode());
                if (item.getSkuCode().equals(spec.getRelationSku())) {
                    amount = amount + item.getAmount().abs().intValue();
                } else {
                    amount = amount + item.getAmount().abs().intValue() * Integer.parseInt(spec.getSpec());
                }
                if (amount == 0) {
                    log.info("===================>>>>>>>>>>订单号为:{}, sku有误", orderInfo.getOrderId());
                    return false;
                }
                ItemParam item1 = new ItemParam();
                item1.setNumber(amount);
                item1.setSkuCode(spec.getRelationSku());
                allItems.add(item1);
            }

            Map<String, Integer> itemMap = allItems.stream().collect(Collectors.groupingBy(ItemParam::getSkuCode, Collectors.summingInt(ItemParam::getNumber)));

            //推单的商品
            ArrayList<DeliveryOrderCreateRequest.OrderLine> lines = new ArrayList<>();

            for (Map.Entry<String, Integer> map : itemMap.entrySet()) {
                String skuCode = map.getKey();
                Integer number = map.getValue();

                String itemCode = skuCode;
//                if (!"30030050".equals(skuCode.trim())) {
                MallSkuSpec reSpec = getSpec(skuCode);
                itemCode = reSpec.getSixCode();
//                }
                DeliveryOrderCreateRequest.OrderLine orderLine = new DeliveryOrderCreateRequest.OrderLine();
                orderLine.setOwnerCode(HttpPostXml.owner);
                orderLine.setItemCode(itemCode);
                orderLine.setPlanQty(number);
                orderLine.setInventoryType("ZC");
                lines.add(orderLine);
            }

            //送脚套，指套，空瓶，泵头
            giveThisItem(lines, itemMap);

            DeliveryOrderCreateRequest.DeliveryOrder deliveryOrder = new DeliveryOrderCreateRequest.DeliveryOrder();
            deliveryOrder.setDeliveryOrderCode(orderInfo.getOrderId());//这里填我方业务单号
            deliveryOrder.setOrderType("JYCK"); //一般交易出库单
            deliveryOrder.setWarehouseCode(HttpPostXml.warehouseCode); //仓库编码
            deliveryOrder.setShopNick(HttpPostXml.storeName); // 店铺
            deliveryOrder.setSourcePlatformCode("OTHER");


            //todo 发货地址 建议统一为仓库发货
            ContactInfo senderInfo = new ContactInfo();
            senderInfo.setCompany("新亦源");
            senderInfo.setProvince("上海市");
            senderInfo.setCity("上海市");
            senderInfo.setArea("青浦区");
            senderInfo.setName("查卫根");
            senderInfo.setDetailAddress("汇金路889号中华印刷厂2号楼3楼");
            senderInfo.setMobile("18521737603");
            deliveryOrder.setSenderInfo(senderInfo);

            String[] s = orderInfo.getProvincialUrbanArea().split(" ");
            String province = s[0];
            String city = s[1];
            String area = s[2];

            //todo 收货地址解析
            ContactInfo receiverInfo = new ContactInfo();
            receiverInfo.setCompany("美浮特");
            receiverInfo.setProvince(province);
            receiverInfo.setCity(city);
            receiverInfo.setArea(area);
            receiverInfo.setName(orderInfo.getAddrName());
            receiverInfo.setDetailAddress(orderInfo.getStreet());
            receiverInfo.setMobile(orderInfo.getAddrPhone());
            deliveryOrder.setReceiverInfo(receiverInfo);

            //这里指定物流方式 不指定
//            deliveryOrder.setLogisticsCode("POSTB5");
//            deliveryOrder.setLogisticsName("邮政快递包裹");


            String expCode = RedisUtil.get("xyy:exp:code:");
            String expName = RedisUtil.get("xyy:exp:name:");

            if (ObjectUtils.isEmpty(expCode)) {
                expCode = "SFTHZP";
            }
            if (ObjectUtils.isEmpty(expName)) {
                expName = "顺丰专配";
            }

            if ("1".equals(orderInfo.getLogisticsType())) {
                expCode = "SFTHZP";
                expName = "顺丰专配";
            }

            deliveryOrder.setLogisticsCode(expCode);
            deliveryOrder.setLogisticsName(expName);

            deliveryOrder.setTotalOrderLines(1); //订单行数量
            deliveryOrder.setCreateTime(LocalDateTime.now());
            deliveryOrder.setRemark("推单");

            qimenClient.createDeliveryOrderLines(deliveryOrder, lines);

            //成功修改状态
            orderInfo.setOrderStatus("4");
            orderInfo.setQmType("2");
            orderInfo.setIsCanCancel("1");
            orderInfo.setExpressCompany(expName);
            orderInfo.setUpdateDate(new Date());
            orderInfo.setDeliverGoodsDate(new Date());
            boolean b = orderInfoService.updateOrderByIdNew(orderInfo);
            if (!b) {
                return false;
            }
        } catch (Exception e) {
            log.info("新亦源入库失败" + e);
            return false;
        }
        return true;
    }

    public void giveThisItem(ArrayList<DeliveryOrderCreateRequest.OrderLine> lines, Map<String, Integer> itemMap) {

        for (Map.Entry<String, Integer> map : itemMap.entrySet()) {
            String skuCode = map.getKey();
            Integer number = map.getValue();

            String itemCode = skuCode;

            DeliveryOrderCreateRequest.OrderLine orderLine = new DeliveryOrderCreateRequest.OrderLine();

            if (Arrays.asList("C034-H", "C033-H").contains(skuCode)) {
                MallSkuSpec reSpec = getSpec("P001");
                itemCode = reSpec.getSixCode();

                orderLine.setOwnerCode(HttpPostXml.owner);
                orderLine.setItemCode(itemCode);
                orderLine.setPlanQty(number);
                orderLine.setInventoryType("ZC");
                lines.add(orderLine);
            }
            if (Arrays.asList("C034-X", "C033-X").contains(skuCode)) {
                MallSkuSpec reSpec = getSpec("P001-X");
                itemCode = reSpec.getSixCode();

                orderLine.setOwnerCode(HttpPostXml.owner);
                orderLine.setItemCode(itemCode);
                orderLine.setPlanQty(number);
                orderLine.setInventoryType("ZC");
                lines.add(orderLine);
            }

            switch (skuCode) {
                case "C035-H":
                    MallSkuSpec reSpec = getSpec("P002");
                    itemCode = reSpec.getSixCode();

                    orderLine.setOwnerCode(HttpPostXml.owner);
                    orderLine.setItemCode(itemCode);
                    orderLine.setPlanQty(number);
                    orderLine.setInventoryType("ZC");
                    lines.add(orderLine);
                    break;
                case "C035-X":
                    reSpec = getSpec("P002");
                    itemCode = reSpec.getSixCode();

                    orderLine.setOwnerCode(HttpPostXml.owner);
                    orderLine.setItemCode(itemCode);
                    orderLine.setPlanQty(number * 40);
                    orderLine.setInventoryType("ZC");
                    lines.add(orderLine);
                    break;
                case "30290030":
                    reSpec = getSpec("40000061");
                    itemCode = reSpec.getSixCode();
                    orderLine.setOwnerCode(HttpPostXml.owner);
                    orderLine.setItemCode(itemCode);
                    orderLine.setInventoryType("ZC");
                    if (LocalDateTime.now().isBefore(LocalDateTime.of(2020, 2, 27, 20, 0, 0, 0))) {
                        orderLine.setPlanQty(number);
                    } else {
                        orderLine.setPlanQty(number * 2);
                    }
                    lines.add(orderLine);
                    break;
                case "30290010":
                    reSpec = getSpec("40000062");
                    itemCode = reSpec.getSixCode();

                    int n = (number / 2);
                    if (n > 0) {
                        orderLine.setOwnerCode(HttpPostXml.owner);
                        orderLine.setItemCode(itemCode);
                        orderLine.setPlanQty(n);
                        orderLine.setInventoryType("ZC");
                        lines.add(orderLine);
                        if (LocalDateTime.now().isAfter(LocalDateTime.of(2020, 2, 27, 20, 0, 0, 0))) {
                            DeliveryOrderCreateRequest.OrderLine orderLine1 = new DeliveryOrderCreateRequest.OrderLine();
                            reSpec = getSpec("40000061");
                            itemCode = reSpec.getSixCode();
                            orderLine1.setOwnerCode(HttpPostXml.owner);
                            orderLine1.setItemCode(itemCode);
                            orderLine1.setPlanQty(n);
                            orderLine1.setInventoryType("ZC");
                            lines.add(orderLine1);
                        }
                    }
                    break;
                case "30030040":
                    reSpec = getSpec("C036-H");
                    itemCode = reSpec.getSixCode();

                    orderLine.setOwnerCode(HttpPostXml.owner);
                    orderLine.setItemCode(itemCode);
                    orderLine.setPlanQty(number);
                    orderLine.setInventoryType("ZC");
                    lines.add(orderLine);
                    break;
                case "30030050":
                    reSpec = getSpec("C036-H");
                    itemCode = reSpec.getSixCode();
                    orderLine.setOwnerCode(HttpPostXml.owner);
                    orderLine.setItemCode(itemCode);
                    orderLine.setPlanQty(number);
                    orderLine.setInventoryType("ZC");
                    lines.add(orderLine);
                    break;
            }
        }
    }


    public Integer getActualAmount(String orderId) {
        List<MallOrderItem> orderItems = orderItemMapper.selectList(
                new EntityWrapper<MallOrderItem>()
                        .eq("order_id", orderId)
                        .eq("is_del", 0));

        int amount = 0;
        for (MallOrderItem item : orderItems) {
//            if ("C036-Z".equals(item.getSkuCode().trim())) {
//                amount = amount + item.getAmount().abs().intValue();
//                break;
//            }
            MallSkuSpec spec = getSpec(item.getSkuCode());
            if (item.getSkuCode().equals(spec.getRelationSku())) {
                amount = amount + item.getAmount().abs().intValue();
            } else {
                amount = amount + item.getAmount().abs().intValue() * Integer.parseInt(spec.getSpec());
            }
        }
        return amount;
    }

    private void buyOneGetOneFree(Date orderPayDate, List<MallOrderItem> itemList) {
        LocalDateTime payDate = orderPayDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime start = LocalDateTime.of(2020, 3, 20, 0, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2020, 4, 21, 0, 0, 0, 0);
        if (payDate.isAfter(start) && payDate.isBefore(end)) {
            for (MallOrderItem item : itemList) {
                if (Arrays.asList("123456789","30030070","30030080").contains(item.getSkuCode())) {
                    item.setAmount(item.getAmount().multiply(BigDecimal.valueOf(2)));
                }
            }
        }
    }
}
