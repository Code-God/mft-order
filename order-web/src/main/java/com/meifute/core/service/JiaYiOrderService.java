package com.meifute.core.service;

import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallSkuSpec;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.mmall.common.enums.LogisticsModeEnum;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.model.WaybillNos;
import com.meifute.core.model.jiayicancelorder.CancelOrder;
import com.meifute.core.model.jiayicancelorder.CancelOrderBody;
import com.meifute.core.model.jiayicancelorder.RequestCancelOrder;
import com.meifute.core.model.jiayicheckoutbound.CheckOutbound;
import com.meifute.core.model.jiayicheckoutbound.CheckOutboundBody;
import com.meifute.core.model.jiayicheckoutbound.RequestCheckOutbound;
import com.meifute.core.model.jiayisubmitorder.*;
import com.meifute.core.util.soapUtil.JaxbXmlUtil;
import com.meifute.core.util.soapUtil.SoapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JiaYiOrderService {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private MallOrderItemService orderItemService;
    @Autowired
    private OrderDelayService orderDelayService;
    @Value("${sf_project_id}")
    private int sf_project_id;
    @Value("${sf_uri}")
    private String uri;

    @Transactional
    public void receiveWaybillNos(List<WaybillNos> waybillNos) {
        waybillNos.forEach(p -> {
            MallOrderInfo orderInfo = orderInfoService.selectByIdNew(p.getOrderId());
            if (orderInfo == null) {
                throw new MallException("020035", new Object[]{p.getOrderId()});
            }
            if (ObjectUtils.isNullOrEmpty(p.getWaybillNo())) {
                throw new MallException("020037", new Object[]{p.getOrderId()});
            }
            orderInfo.setExpressCode(p.getWaybillNo());
            orderInfo.setOrderStatus("4");
            orderInfo.setIsCanCancel("1");
            orderInfo.setDeliverGoodsDate(new Date());
            orderInfo.setUpdateDate(new Date());
            orderInfoService.updateOrderById(orderInfo);
        });
    }

    /**
     * 顺丰推单
     *
     * @param orderInfo
     * @param itemList
     */
    public void submitOrder(MallOrderInfo orderInfo, List<MallOrderItem> itemList) {
        String expressName = LogisticsModeEnum.explain(orderInfo.getLogisticsMode());
        String xml = formatPushPojo(orderInfo, expressName, itemList);
        String result = SoapUtil.sendPost(xml, uri);
        log.info("推单佳一顺丰返回值xml: {}", result);
        SoapUtil.formatSoap(result, 0);

        orderInfo.setIsCanCancel("1");
        orderInfo.setUpdateDate(new Date());
        orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
        orderInfo.setExpressCompany(expressName);
        updateOrderById(orderInfo);
        log.info("Congratulations 订单顺丰推单成功:{}", orderInfo);
    }

    private void updateOrderById(MallOrderInfo mallOrderInfo) {
        mallOrderInfo.setUpdateDate(new Date());
        orderInfoService.updateById(mallOrderInfo);
    }

    /**
     * 关闭顺丰订单
     *
     * @param orderId
     */
    public void cancelOrder(String orderId) {
        String xml = formatCancelPojo(orderId);
        String result = SoapUtil.sendPost(xml, uri);
        log.info("关闭佳一顺丰订单返回值xml: {}", result);
        SoapUtil.formatSoap(result, 1);
    }


    /**
     * 获取物流单号
     *
     * @param orderId
     */
    public String checkOutbound(String orderId) {
        String xml = formatCheckOutbound(orderId);
        String result = SoapUtil.sendPost(xml, uri);
        log.info("佳一顺丰获取物流返回值xml: {}", result);
        return SoapUtil.formatSoap(result, 2);
    }

    public String screenExpress(List<MallOrderItem> itemlist) {
        List<String> express = new ArrayList<>();
        for (MallOrderItem item : itemlist) {
            MallSku sku = itemFeign.getSkuByCode(item.getSkuCode());
            express.add(sku.getLogisticsMode());
        }
        List<String> collect = express.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        String explain = LogisticsModeEnum.explain(collect.get(0));
        return explain;
    }

    private String formatCheckOutbound(String orderId) {
        RequestCheckOutbound request = new RequestCheckOutbound();
        CheckOutboundBody body = new CheckOutboundBody();
        CheckOutbound checkOutbound = new CheckOutbound();
        checkOutbound.setDn(orderId);
        checkOutbound.setProjectId(sf_project_id);
        checkOutbound.setKey(getKey(orderId, sf_project_id));
        body.setCheckOutbound(checkOutbound);
        request.setBody(body);
        String xml = JaxbXmlUtil.convertToXml(request);
        log.info("佳一获取物流入参:{}", xml);
        return xml;
    }

    private String formatCancelPojo(String orderId) {
        RequestCancelOrder requestCancelOrder = new RequestCancelOrder();
        CancelOrderBody cancelOrderBody = new CancelOrderBody();
        CancelOrder cancelOrder = new CancelOrder();
        cancelOrder.setDn(orderId);
        cancelOrder.setProjectId(sf_project_id);
        cancelOrder.setKey(getKey(orderId, 8));
        cancelOrderBody.setCancelOrder(cancelOrder);
        requestCancelOrder.setBody(cancelOrderBody);
        String xml = JaxbXmlUtil.convertToXml(requestCancelOrder);
        log.info("佳一关闭订单入参:{}", xml);
        return xml;
    }


    private String formatPushPojo(MallOrderInfo orderInfo, String expressName, List<MallOrderItem> itemlist) {
        List<MallOrderItem> items = new ArrayList<>();
        //商品换算
        itemlist.forEach(p -> {
            p.setAmount(p.getAmount().abs());
            if (p.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                List<MallOrderItem> itemList = orderDelayService.getUnitChange(getSpec(p.getSkuCode()), p.getAmount());
                items.addAll(itemList);
            }
        });
        // 3 送 指套 脚套
        List<MallOrderItem> pItem = orderDelayService.giveThisItem(items);
        if (pItem.size() > 0) {
            items.addAll(pItem);
        }

        RequestSubmitOrder requestSubmitOrder = new RequestSubmitOrder();
        SubmitOrderBody submitOrderBody = new SubmitOrderBody();
        SubmitOrder submitOrder = new SubmitOrder();
        AsnOut asnOut = new AsnOut();

        asnOut.setWarehouseId(1);
        asnOut.setProjectId(sf_project_id);
        asnOut.setPayTime(new Date());
        asnOut.setCarrier(expressName);
        asnOut.setShipper_Person(orderInfoService.getCompanyName(orderInfo.getCompanyId(), orderInfo.getMallUserId()));
        asnOut.setCon_City(orderInfo.getProvincialUrbanArea());
        asnOut.setCon_Address(orderInfo.getStreet());
        asnOut.setCon_Person(orderInfo.getAddrName());
        asnOut.setCon_Tel(orderInfo.getAddrPhone());
        asnOut.setCon_Mobile(orderInfo.getAddrPhone());
        asnOut.setDN(orderInfo.getOrderId());
        asnOut.setMemo("顺丰（佳轶）");

        CargoOutList cargoOutList = new CargoOutList();
        List<CargoOut> list = new ArrayList<>();

        items.forEach(p -> {
            CargoOut cargoOut = new CargoOut();
            cargoOut.setItemNo(p.getSkuCode());
            cargoOut.setQty(p.getAmount());
            cargoOut.setMemo("顺丰（佳轶）");
            list.add(cargoOut);
        });

        cargoOutList.setCargoOutList(list);

        submitOrder.setAsnOut(asnOut);
        submitOrder.setCargoOutList(cargoOutList);

        String key = getKey(orderInfo.getOrderId(), list.size());
        submitOrder.setKey(key);

        submitOrderBody.setSubmitOrder(submitOrder);
        requestSubmitOrder.setBody(submitOrderBody);
        String xml = JaxbXmlUtil.convertToXml(requestSubmitOrder);
        log.info("佳一推单入参:{}", xml);
        return xml;
    }

    private String getKey(String orderId, int lines) {
        String HashSalt = "@UxM%";
        return SHA1(orderId + HashSalt + lines);
    }

    private MallSkuSpec getSpec(String skuCode) {
        return itemFeign.getSkuSpecBySkuCodeAndExpressType(skuCode, "1");
    }

    public String SHA1(String decript) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(decript.getBytes("UTF-8"));
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte message : messageDigest) {
                String shaHex = Integer.toHexString(message & 0xFF);
                if (shaHex.length() < 2)
                    hexString.append(0);

                hexString.append(shaHex);
            }
            return hexString.toString().toLowerCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
