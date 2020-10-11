package com.meifute.core.service;

import com.alibaba.fastjson.JSONObject;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallOrderItem;
import com.meifute.core.entity.MallSku;
import com.meifute.core.feignclient.ItemFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @program: m-mall-order
 * @description: 无忧供应链
 * @author: Mr.Wang
 * @create: 2020-02-15 12:29
 **/
@Service
@Slf4j
public class WuYouService {

    @Value("${wu_you_app_key}")
    private String appKey;

    @Value("${wu_you_app_secret}")
    private String appSecret;

    @Value("${wu_you_app_url}")
    private String appUrl;

    @Autowired
    ItemFeign itemFeign;
    @Autowired
    MallOrderInfoService mallOrderInfoService;

    /**
     * 推单接口
     * 通过JSON报文格式将订单信息推送给WMS，同一个报文中可包含多个订单。
     *
     * @return
     */
    public Boolean orderReceive(MallOrderInfo orderInfo, List<MallOrderItem> itemList) {
        log.info("==============无忧推单入参 orderInfo:[{}],itemList:[{}]",orderInfo,itemList);
        if (!"3".equals(orderInfo.getOrderStatus())) {
            return true;
        }
        if (!"3".equals(orderInfo.getLogisticsMode())) {
            return true;
        }

        HttpHeaders httpHeaders = new HttpHeaders();
        //请求体
        RestTemplate restTemplate = new RestTemplate();
        JSONObject json = new JSONObject();
        json.put("Action", "OrderReceive");//操作类型，定值OrderReceive
        json.put("CustomerID", "MFT"); //合作方，定值：DFW
        json.put("WHCode", "SH01"); //仓库编码，定值A
        json.put("SOReference1", orderInfo.getOrderId()); //订单编号
        json.put("OrderTime", orderInfo.getCreateDate()); //订单创建时间
        json.put("SOReference2", "Express"); //物流类型： Express快递订单 Logistics物流订单

        String[] s = orderInfo.getProvincialUrbanArea().split(" ");
        String province = s[0];
        String city = s[1];
        String area = s[2];
        json.put("C_Province", province); //收货方省份
        if ("北京市上海市天津市重庆市".contains(city)) {
            json.put("C_City", province); //收货方城市
            json.put("C_District", city); //收货方区县

        } else{
            json.put("C_City", city); //收货方城市
            json.put("C_District", area); //收货方区县
        }
        json.put("C_Address1", orderInfo.getAddrId()); //收货方详细地址
        json.put("C_Tel1", orderInfo.getAddrPhone()); //收货联系人电话
        json.put("C_Contact", orderInfo.getAddrName()); //收货联系人
        json.put("C_Zip", ""); //收货邮编
        json.put("Notes", orderInfo.getBuyerMemo()); //备注

        List<JSONObject> detailList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(itemList)) {
            itemList.forEach(item -> {
                JSONObject detail = new JSONObject();
                detail.put("SKU", item.getSkuCode());//产品编码
                MallSku skuByCode = itemFeign.getSkuByCode(item.getSkuCode());
                detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                detail.put("Notes", "");//明细备注
                detailList.add(detail);
            });
        }

        // 送脚套指套，空瓶，泵头
        giveThisItem(detailList, itemList);


        json.put("CarrierID", "JD"); //快递公司的代码 SFEXPRESS	顺丰 YUNDA	韵达
        json.put("CarrierName", "京东"); //快递公司的名字


        json.put("details", detailList); //订单商品详情 可填多个

        String timestamp = String.valueOf(System.currentTimeMillis());

        List<JSONObject> list1 = new ArrayList<>();
        list1.add(json);
        String requestParam = appSecret + appKey + timestamp + list1.toString();
        String sign = getSign(requestParam);

        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<String, Object>();
        paramMap.add("method", "OrderReceive.sig");
        paramMap.add("appkey", appKey);
        paramMap.add("data", list1.toString());
        paramMap.add("timestamp", timestamp);
        paramMap.add("sign", sign);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<MultiValueMap<String, Object>>(paramMap, httpHeaders);
        String str = restTemplate.exchange(appUrl, HttpMethod.POST, httpEntity, String.class).getBody();
        log.info("===========================无忧推单接口返回值:[{}]", str);

        if (StringUtils.isEmpty(str)) {
            log.info("无忧推单接口返回值为空");
            return false;
        }

        JSONObject result = JSONObject.parseObject(str);

        Boolean success = (Boolean) result.get("success");
        if (success) {
            log.info("无忧推单接口成功");
            //成功修改状态
            orderInfo.setOrderStatus("4");
            orderInfo.setIsCanCancel("1");
            orderInfo.setExpressCompany("京东");
            orderInfo.setUpdateDate(new Date());
            orderInfo.setDeliverGoodsDate(new Date());
            boolean b = mallOrderInfoService.updateOrderByIdNew(orderInfo);
            if (b) {
                return true;
            }
        } else {
            log.info("无忧推单接口调用失败:[{}]", result.get("msg"));
        }

        return false;
    }

    public void giveThisItem(List<JSONObject> detailList, List<MallOrderItem> itemList) {

        for (MallOrderItem item : itemList) {

            String skuCode = item.getSkuCode();
            JSONObject detail = new JSONObject();
            if (Arrays.asList("C034-H", "C033-H").contains(skuCode)) {
                detail.put("SKU", "P001");//产品编码
                MallSku skuByCode = itemFeign.getSkuByCode("P001");
                detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                detail.put("Notes", "");//明细备注
                detailList.add(detail);
            }

            if (Arrays.asList("C034-X", "C033-X").contains(skuCode)) {
                detail.put("SKU", "P001-X");//产品编码
                MallSku skuByCode = itemFeign.getSkuByCode("P001-X");
                detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                detail.put("Notes", "");//明细备注
                detailList.add(detail);
            }

            switch (skuCode) {
                case "C035-H":
                    detail.put("SKU", "P002");//产品编码
                    MallSku skuByCode = itemFeign.getSkuByCode("P002");
                    detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                    detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                    detail.put("Notes", "");//明细备注
                    detailList.add(detail);
                    break;
                case "C035-X":
                    detail.put("SKU", "P002");//产品编码
                    skuByCode = itemFeign.getSkuByCode("P002");
                    detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                    detail.put("QtyOrdered_Each", item.getAmount().abs().multiply(BigDecimal.valueOf(40)) + "");//最小单位数量
                    detail.put("Notes", "");//明细备注
                    detailList.add(detail);
                    break;
                case "30290030":
                    detail.put("SKU", "40000061");//产品编码
                    skuByCode = itemFeign.getSkuByCode("40000061");
                    detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                    detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                    detail.put("Notes", "");//明细备注
                    detailList.add(detail);
                    break;
                case "30290020":
                    detail.put("SKU", "40000062");//产品编码
                    skuByCode = itemFeign.getSkuByCode("40000062");
                    detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                    detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                    detail.put("Notes", "");//明细备注
                    detailList.add(detail);
                    break;
                case "30030040":
                    detail.put("SKU", "C036-H");//产品编码
                    skuByCode = itemFeign.getSkuByCode("C036-H");
                    detail.put("SKU_DescrC", null == skuByCode ? "" : skuByCode.getTitle());//产品名称
                    detail.put("QtyOrdered_Each", item.getAmount().abs() + "");//最小单位数量
                    detail.put("Notes", "");//明细备注
                    detailList.add(detail);
                    break;
            }
        }
    }

    /**
     * 取消(拦截)订单物流
     *
     * @return
     */
    @Transactional
    public Boolean orderCanCel(List<String> orderIds) {
        HttpHeaders httpHeaders = new HttpHeaders();
        //请求体
        RestTemplate restTemplate = new RestTemplate();
        List<JSONObject> list1 = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderIds)) {
            orderIds.forEach(orderId -> {
                JSONObject json = new JSONObject();
                json.put("Action", "OrderCanCel");//操作类型，定值OrderReceive
                json.put("CustomerID", "MFT"); //合作方，定值：DFW
                json.put("WHCode", "SH01"); //仓库编码，定值A
                json.put("SOReference1", orderId); //订单编号
                list1.add(json);
            });
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String requestParam = appSecret + appKey + timestamp + list1.toString();
        String sign = getSign(requestParam);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<String, Object>();
        paramMap.add("method", "OrderCanCel.sig");
        paramMap.add("appkey", appKey);
        paramMap.add("data", list1.toString());
        paramMap.add("timestamp", timestamp);
        paramMap.add("sign", sign);
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<MultiValueMap<String, Object>>(paramMap, httpHeaders);
        String str = restTemplate.exchange(appUrl, HttpMethod.POST, httpEntity, String.class).getBody();
        log.info("===========================取消无忧物流接口返回值:[{}]", str);
        if (StringUtils.isEmpty(str)) {
            log.info("无忧推单接口返回值为空");
            return false;
        }

        JSONObject result = JSONObject.parseObject(str);

        Boolean success = (Boolean) result.get("success");

        if (success) {
            List<JSONObject> data = (List<JSONObject>) result.get("data");
            data.forEach(json -> {
                String code = (String) json.get("code");
                String SOReference1 = (String) json.get("SOReference1");
                if ("0".equals(code)) {
                    //取消失败
                    String message = (String) json.get("message");
                    log.info("================订单号:[{}],[{}]", SOReference1, message);
                }

                //拦截物流成功,关闭订单
                MallOrderInfo mallOrderInfo = new MallOrderInfo();
                mallOrderInfo.setOrderId(SOReference1);
                mallOrderInfoService.closeOrderFromAdmin(mallOrderInfo);
            });

        } else
            log.info("===========================[{}]", result.get("msg") + "");

        return success;
    }


    /**
     * 换成16进制字符串
     *
     * @param bytes
     * @return
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 签名码生成规则具体算法为md5str=appsecret+appkey+timestamp+data后，
     * 把md5str进行UTF-8编码 ，用MD5加密后转换成16进制字符串
     *
     * @param sign
     * @return
     */
    public static String getSign(String sign) {
        byte[] info1 = null;
        try {
            info1 = sign.getBytes("UTF-8");//把md5str进行UTF-8编码
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        MessageDigest messagedigest = null;
        try {
            messagedigest = MessageDigest.getInstance("MD5");//用MD5加密
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        messagedigest.update(info1);
        byte[] b = messagedigest.digest();
        return bytesToHex(b);//转换成16进制字符串
    }

    public static void main(String[] args) {
        String appkey = "dc3a1c19-ef41-488a-83b7-43e36b38e858";
        String appsecret = "8095e3c649a64aa5ee4b7f6ca7bd6fac";
        String timestamp = String.valueOf(System.currentTimeMillis());
        System.out.println(timestamp);
        //String data = "{\"CustomerID\":\"NS\",\"OrgID\":\"SH04\",\"Type\":\"0\",\"orders\":[{\"CusOrderNo\":\"19070669711652401000\"},{\"CusOrderNo\":\"19071051670202221000\"}]}";
        /*String data = "{\"Action\":\"OrderReceive\",\"C_Address1\":\"上海 虹口区 城区 白玉兰广场2903\"," +
                "\"C_City\":\"虹口区\",\"C_Contact\":\"王振鹏\",\"C_Province\":\"上海\"," +
                "\"C_Tel1\":\"15900849974\",\"C_Zip\":\"\",\"CarrierID\":\"JD\",\"CustomerID\":\"MFT\"," +
                "\"OrderTime\":1581836626000,\"SOReference1\":\"1054903176961056768\"," +
                "\"SOReference2\":\"Express\",\"WHCode\":\"A\",\"details\":" +
                "[{\"Notes\":\"\",\"QtyOrdered_Each\":1,\"SKU\":\"30290010\",\"SKU_DescrC\":\"米浮衣物家居消毒液（瓶）\"}]}";*/
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Action", "OrderReceive");
        jsonObject.put("C_Address1", "上海 虹口区 城区 白玉兰广场2903");
        jsonObject.put("C_City", "虹口区");
        jsonObject.put("C_Contact", "王振鹏");
        jsonObject.put("C_Province", "上海");
        jsonObject.put("C_Tel1", "15900849974");
        jsonObject.put("C_Zip", "");
        jsonObject.put("CarrierID", "JD");
        jsonObject.put("CustomerID", "MFT");
        jsonObject.put("OrderTime", "1581836626000");
        jsonObject.put("SOReference1", "1054903176961056768");
        jsonObject.put("SOReference2", "Express");
        jsonObject.put("WHCode", "SH01");
        JSONObject j2 = new JSONObject();
        j2.put("Notes", "");
        j2.put("QtyOrdered_Each", "1");
        j2.put("details", "A");
        j2.put("SKU", "30290010");
        j2.put("SKU_DescrC", "米浮衣物家居消毒液（瓶）");
        List<JSONObject> list = new ArrayList<>();
        list.add(j2);
        jsonObject.put("details", list);
//        jsonObject.put("details", j2);
        List<JSONObject> list1 = new ArrayList<>();
        list1.add(jsonObject);
        String md5str = appsecret + appkey + timestamp + list1.toString();
        String sign = getSign(md5str);
        System.out.println(sign);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<String, Object>();
        paramMap.add("method", "OrderReceive.sig");
        paramMap.add("appkey", "dc3a1c19-ef41-488a-83b7-43e36b38e858");
        paramMap.add("data", list1.toString());
        paramMap.add("timestamp", timestamp);
        paramMap.add("sign", sign);
        System.out.println(jsonObject);
//        String appUrl = "http://test.bvp-l.com/WMS-MFT-Server/OrderReceive.sig" + "?sign=" + sign + "&data=" + jsonObject.toString() + "&method=OrderReceive.sig&appkey=" + appkey + "&timestamp=" + timestamp;
        String appUrl = "http://test.bvp-l.com/WMS-MFT-Server/OrderReceive.sig";
        HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<MultiValueMap<String, Object>>(paramMap, httpHeaders);
//        HttpEntity<String> httpEntity = new HttpEntity<String>(httpHeaders);
        ResponseEntity<String> exchange = restTemplate.exchange(appUrl, HttpMethod.POST, httpEntity, String.class);
        String body = exchange.getBody();
        System.out.println(body);


    }

    /**
     * 给第三方提供的更新物流单号接口
     *
     * @param json
     * @return
     */
    public JSONObject updateOrderExpressInfo(String json) {
        JSONObject result = new JSONObject();
        JSONObject jsonObject = new JSONObject();
        List<JSONObject> requestList = new ArrayList<>();
        try {
            jsonObject = JSONObject.parseObject(json);
            log.info(jsonObject.toJSONString());
            requestList = (List<JSONObject>) jsonObject.get("list");

        } catch (Exception e) {
            log.info("json格式异常");
            result.put("success", false);
            result.put("msg", "json格式异常");
            return result;
        }

        List<JSONObject> returnList = new ArrayList<>();

        if (CollectionUtils.isEmpty(requestList)) {
            log.info("==============集合为空");
            result.put("success", false);
            result.put("msg", "参数异常");
            return result;
        }

        requestList.forEach(request -> {
            JSONObject falseResult = new JSONObject();
            String s = checkParams(request);
            if (StringUtils.isEmpty(s)) {
                String orderId = (String) request.get("orderId");
                String expressCode = (String) request.get("expressCode");
                String expressCompany = (String) request.get("expressCompany");

                falseResult.put("orderId", orderId);
                //根据订单号查询订单信息
                MallOrderInfo mallOrderInfo = mallOrderInfoService.selectByIdNew(orderId);
                if (null != mallOrderInfo) {
                    String code = mallOrderInfo.getExpressCode(); //物流编号

                    if (StringUtils.isEmpty(code)) {
                        //无物流单号
                        MallOrderInfo orderInfo = new MallOrderInfo();
                        orderInfo.setOrderId(orderId);
                        orderInfo.setExpressCode(expressCode);
                        orderInfo.setExpressCompany(expressCompany);
                        boolean b = mallOrderInfoService.updateOrderByIdNew(orderInfo);
                        if (!b) {
                            falseResult.put("code", "0");
                            falseResult.put("message", "订单更新失败");
                        } else {
                            falseResult.put("message", "");
                            falseResult.put("code", "1");
                        }

                    } else {
                        //有物流单号,不再跟新
                        falseResult.put("orderId", orderId);
                        falseResult.put("code", "0");
                        falseResult.put("message", "订单号:[" + orderId + "]无法重复更新物流信息");
                    }
                } else {
                    log.info("订单号:[{}] 无对应订单", orderId);
                    falseResult.put("code", "0");
                    falseResult.put("message", "订单号:[" + orderId + "]无对应订单");
                }

            } else {
                log.info("参数异常:[{}]", s);
                falseResult.put("code", "0");
                falseResult.put("message", s);
            }
            returnList.add(falseResult);
        });

        result.put("success", true);
        result.put("data", returnList);
        return result;
    }

    public static String checkParams(JSONObject jsonObject) {
        Object orderId = jsonObject.get("orderId");
        Object expressCode = jsonObject.get("expressCode");
        Object expressCompany = jsonObject.get("expressCompany");
        if (null == orderId)
            return "orderId不能为空";
        if (null == expressCode)
            return "expressCode不能为空";
        if (null == expressCompany)
            return "expressCompany不能为空";
        return null;
    }


}
