package com.meifute.core.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.domain.ECLP.EclpOpenService.response.cancelOrder.CancelResult;
import com.jd.open.api.sdk.domain.ECLP.EclpOpenService.response.queryGoodsInfo.GoodsInfo;
import com.jd.open.api.sdk.request.ECLP.*;
import com.jd.open.api.sdk.response.ECLP.*;
import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.UserAddressDto;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallSkuSpec;
import com.meifute.core.entity.MallUser;
import com.meifute.core.entity.activity.MallAcOrder;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.HttpclientProxy;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.StringUtils;
import com.meifute.core.util.JsonUtils;
import com.meifute.core.vo.SkuSpecStockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * @Auther: wxb
 * @Date: 2018/10/26 16:27
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class JDExpressPushService {

//    public static String SERVER_URL = "https://api.jd.com/routerjson";
//    public static String oldAppKey = "6C72B07E36261F273FD88F7989846D80";
//    public static String oldAppSecret = "7c44545f106d4d42bced3144383fdffe";
//    public static String appKey = "0C2AC54D67CD3E5ACD7EC00EFE395BAC";
//    public static String appSecret = "4cb1120cda6f47b2ad46527e226f62ed";

    @Value("${jd_push_server_url}")
    private String SERVER_URL;
    @Value("${jd_push_appKey}")
    private String appKey;
    @Value("${jd_push_appSecret}")
    private String appSecret;
    @Value("${jd_push_warehouseNo}")
    private String warehouseNo;
    @Value("${jd_push_departmentNo}")
    private String departmentNo;
    @Value("${jd_push_shopNo}")
    private String shopNo;
    @Value("${jd_push_isvSource}")
    private String isvSource;
    @Value("${jd_push_salePlatformSource}")
    private String salePlatformSource;
    @Value("${jd_push_orderMark}")
    private String orderMark;

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private CommonOrderService commonOrderService;


    /**
     * 京东推单
     *
     * @return
     */
    public EclpOrderAddOrderResponse jdExpressPush(String uuid, String transportGoodsNo, String goodsAmount, String recipientName, String recipientPhone, String address) {
        log.info("进入京东推单 orderId={},transportGoodsNo={},goodsAmount={}", uuid, transportGoodsNo, goodsAmount);
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpOrderAddOrderRequest request = new EclpOrderAddOrderRequest();
        request.setIsvUUID(uuid);
        request.setOrderMark(orderMark);
        request.setSalePlatformSource(salePlatformSource);
        request.setIsvSource(isvSource);
        request.setShopNo(shopNo);
        request.setDepartmentNo(departmentNo);
        request.setConsigneeName(recipientName);
        request.setConsigneeMobile(recipientPhone);
        request.setConsigneeAddress(address);
        request.setQuantity(goodsAmount);
        request.setGoodsNo(transportGoodsNo);
        request.setWarehouseNo(warehouseNo);//110009487 //110008552
        EclpOrderAddOrderResponse response;
        try {
            response = client.execute(request);
            log.info("jdExcpressPush -->respose={}", JSON.toJSONString(response));
        } catch (Exception e) {
            log.error("jdExpressPush error:{0}", e);
            return null;
        }
        return response;
    }


    /**
     * 销售出库单明细查询
     *
     * @param eclpSoNo
     * @return 快递单号
     */
    public String queryJDOrderWayBill(String eclpSoNo, int originOrder, String orderId) {
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpOrderQueryOrderRequest request = new EclpOrderQueryOrderRequest();
        request.setEclpSoNo(eclpSoNo);
        try {
            EclpOrderQueryOrderResponse response = client.execute(request);
            log.info("----queryJDOrderWayBill --> response={}, eclpSoNo={}", JSON.toJSONString(response), eclpSoNo);
            if (ObjectUtils.isNullOrEmpty(response.getQueryorderResult()) || ObjectUtils.isNullOrEmpty(response)) {
                return null;
            }
            String wayBill = response.getQueryorderResult().getWayBill();
            if (StringUtils.isBlank(wayBill)) {
                return null;
            }
            int orderStatus = 4;
            if ("10034".equals(response.getQueryorderResult().getCurrentStatus())) {//已签收
                orderStatus = 5;
            }

            if (originOrder == 0) {
                MallOrderInfo orderInfo = orderInfoService.selectByIdNew(orderId);
                if (orderInfo == null || !"4".equals(orderInfo.getOrderStatus())) {
                    return null;
                }
                orderInfo.setExpressCode(wayBill);
                orderInfo.setExpressCompany("京东物流");
                orderInfo.setDeliverGoodsDate(new Date());
                orderInfo.setOrderStatus(String.valueOf(orderStatus));
                mallOrderInfoMapper.updateById(orderInfo);
                if (orderInfo.getChannelState() == 1) {
                    MallUser user = userFeign.getUserById(orderInfo.getSubordinateId());
                    UserAddressDto address = userFeign.getLatelyAddress(user.getPhone());
                    commonOrderService.sendDeliverySmsMsg(orderInfo.getMallUserId(), user.getPhone(), address == null ? user.getName() : address.getName(), wayBill);
                }
            } else if (originOrder == 2) {
                itemFeign.updateById(MallAcOrder.builder()
                        .id(orderId)
                        .isCanCancel("1")
                        .orderStatus(String.valueOf(orderStatus))
                        .deliveryTime(new Date())
                        .expressCode(wayBill)
                        .updateTime(new Date())
                        .build());
            }
            log.info("success to get wayBill :{}", wayBill);
            return wayBill;
        } catch (Exception e) {
            log.error("queryJDOrder error:{0}", e);
            return null;
        }
    }


    public boolean queryJdOrder(String eclpSoNo) {
        try {
            String accessToken = RedisUtil.get("jd:access_token");
            JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
            EclpOrderQueryOrderRequest request = new EclpOrderQueryOrderRequest();
            request.setEclpSoNo(eclpSoNo);
            EclpOrderQueryOrderResponse response = client.execute(request);
            if (ObjectUtils.isNullOrEmpty(response.getQueryorderResult()) || ObjectUtils.isNullOrEmpty(response)) {
                return false;
            }
            log.info("查询是否可关闭订单，eclpSoNo={}，response={},status={}", eclpSoNo, JSON.toJSONString(response), response.getQueryorderResult().getCurrentStatus());
            if ("10009".equals(response.getQueryorderResult().getCurrentStatus()) || "10010".equals(response.getQueryorderResult().getCurrentStatus())) {
                return true;
            }
            if ("100130".equals(response.getQueryorderResult().getCurrentStatus()) || "100131".equals(response.getQueryorderResult().getCurrentStatus())) {
                return true;
            }
            if ("100132".equals(response.getQueryorderResult().getCurrentStatus()) || "10014".equals(response.getQueryorderResult().getCurrentStatus())) {
                return true;
            }
            if ("10015".equals(response.getQueryorderResult().getCurrentStatus()) || "10016".equals(response.getQueryorderResult().getCurrentStatus()) || "10028".equals(response.getQueryorderResult().getCurrentStatus())) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 添加主商品
     *
     * @param thirdCateGoryNo 三级分类编码
     * @return goodsNo 事业部商品编码
     */
    public String transportGoodsInfo(String specId, String thirdCateGoryNo, String expressType) {
        MallSkuSpec mallSkuSpec = itemFeign.getSkuSpecById(specId);
        MallSkuSpec mallSkuSpec1 = itemFeign.getSkuSpecBySkuCodeAndExpressType(mallSkuSpec.getSkuCode(), expressType);
        if (ObjectUtils.isNotNullAndEmpty(mallSkuSpec1.getTransportGoodsNo())) {
            throw new MallException(OrderRespCode.TRANSPORTGOODS_IS_NOT_NULL);
        }
        MallSku skuByCode = itemFeign.getSkuByCode(mallSkuSpec.getSkuCode());
        String accessToken = RedisUtil.get("jd:access_token");
        String serverUrl = "https://api.jd.com/routerjson";
        JdClient client = new DefaultJdClient(serverUrl, accessToken, appKey, appSecret);
        EclpGoodsTransportGoodsInfoRequest request = new EclpGoodsTransportGoodsInfoRequest();
        request.setDeptNo(departmentNo);
        request.setIsvGoodsNo(mallSkuSpec.getSkuCode());
        request.setBarcodes(mallSkuSpec.getSixCode());
        request.setThirdCategoryNo(thirdCateGoryNo);
        request.setGoodsName(skuByCode.getTitle());
        request.setSafeDays(Integer.valueOf(mallSkuSpec.getSafeDays()));
        try {
            EclpGoodsTransportGoodsInfoResponse response = client.execute(request);
            log.info("添加主商品 response={},specid={},thirdCateGoryNo={}", JSON.toJSONString(response), specId, thirdCateGoryNo);
            String goodsNo = response.getGoodsNo();
            mallSkuSpec.setTransportGoodsNo(goodsNo);
            //更新
            itemFeign.updateSkuSpecTransportNum(mallSkuSpec);
            return goodsNo;
        } catch (Exception e) {
            log.error("transportGoodsInfo error:{}", e);
            return null;
        }
    }


    /**
     * 主商品信息
     */
    public List<GoodsInfo> getTransportGoodsInfo(BaseParam baseRequest) {
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpGoodsQueryGoodsInfoRequest request = new EclpGoodsQueryGoodsInfoRequest();
        request.setDeptNo(departmentNo);
        request.setPageNo(baseRequest.getPageCurrent());
        request.setPageSize(baseRequest.getPageSize());
        try {
            EclpGoodsQueryGoodsInfoResponse response = client.execute(request);
            List<GoodsInfo> goodsInfoList = response.getGoodsInfoList();
            return goodsInfoList;
        } catch (Exception e) {
            log.error("getTransportGoodsInfo error:{}", e);
            return null;
        }
    }


    /**
     * 取消出库单 jingdong.eclp.order.cancelOrder
     *
     * @param eclpSoNo
     * @return boolean
     */

    public boolean cancelOrder(String eclpSoNo) {
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpOrderCancelOrderRequest request = new EclpOrderCancelOrderRequest();
        request.setEclpSoNo(eclpSoNo);
        try {
            EclpOrderCancelOrderResponse response = client.execute(request);
            CancelResult cancelorderResult = response.getCancelorderResult();
            log.info("取消出库单 cancelorderResult={}", JSON.toJSONString(cancelorderResult));
            return 1 == cancelorderResult.getCode();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 收货
     *
     * @param orderId
     * @return 快递单号
     */
    @Transactional
    public void receivingGoodsFromJd(String eclpSoNo, String orderId) {
        JSONArray parse = JSONArray.parseArray(eclpSoNo);
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpOrderQueryOrderRequest request = new EclpOrderQueryOrderRequest();
        request.setEclpSoNo(parse.get(0).toString());
        try {
            EclpOrderQueryOrderResponse response = client.execute(request);
            log.info("-queryJDOrder  -->response={},eclpSoNo={}", JSON.toJSONString(response), eclpSoNo);
            if (ObjectUtils.isNullOrEmpty(response.getQueryorderResult()) || ObjectUtils.isNullOrEmpty(response)) {
                return;
            }

            if ("10034".equals(response.getQueryorderResult().getCurrentStatus())) {
                MallOrderInfo order = orderInfoService.selectByIdNew(orderId);
                MallOrderInfo orderInfo = new MallOrderInfo();
                orderInfo.setOrderId(orderId);
                orderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_005.getCode());
                orderInfo.setConfirmGoodsDate(new Date());
                mallOrderInfoMapper.updateById(orderInfo);
                if (order.getChannelState() == 1) {
                    MallUser user = userFeign.getUserById(order.getSubordinateId());
                    commonOrderService.sendDeliverySmsMsgEvaluate(user.getPhone(), orderId);
                }
            }
        } catch (Exception e) {
            log.error("queryJDOrder error:{}", e);
        }
    }


    public String querySpSource(String accessToken) {
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);

        EclpMasterQuerySpSourceRequest request = new EclpMasterQuerySpSourceRequest();

        EclpMasterQuerySpSourceResponse response = null;
        try {
            response = client.execute(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.getCode();
    }

    public void getJdAuthorizationCode() {
        String url = "https://oauth.jd.com/oauth/authorize";
        String notifyurl = "http://mall-test.meifute.com/order/v1/app/ordercenter/jd/notify";
        Map<String, String> params = Maps.newHashMap();
        params.put("response_type", "code");
        params.put("client_id", appKey);
        params.put("redirect_uri", notifyurl);
        try {
            String result = HttpclientProxy.execGETMethod(url, params);
            System.out.println("----------" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getJdAccessToken() {
        String url = "https://oauth.jd.com/oauth/token";
        String notifyurl = "http://m-test.meifute.com/order/v1/app/ordercenter/jd/notify";
        Map<String, String> params = Maps.newHashMap();
        params.put("grant_type", "authorization_code");
        params.put("client_id", appKey);
        params.put("redirect_uri", notifyurl);
        params.put("code", "uiCEIH");//gEJvsT
        params.put("state", "20200302");
        params.put("client_secret", appSecret);
        String accessToken = null;
        try {
            String result = HttpclientProxy.execPOSTMethodMParames(url, params);
            Map<String, Object> map = JSONUtil.json2map(result);
            accessToken = (String) map.get("access_token");
            System.out.print(accessToken + "_________________");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return accessToken;//0d5cc377-fbe1-497f-84c6-437c3a4cfe99
    }


    //查询商品库存信息
    public List<SkuSpecStockVO> queryStock(String goodsNo, Integer currentPage, Integer pageSize) {
        List<SkuSpecStockVO> itemStocks = Lists.newArrayList();
        EclpStockQueryStockResponse response = queryJDStock(goodsNo, currentPage, pageSize);
        if (Objects.isNull(response)) {
            return itemStocks;
        }
        response.getQuerystockResult().forEach(stock -> {
            SkuSpecStockVO vo = new SkuSpecStockVO();
            vo.setTransportGoodsNo(stock.getGoodsNo()[0]);
            vo.setTotalAmount(stock.getTotalNum()[0]);
            itemStocks.add(vo);
        });
        return itemStocks;
    }

    private EclpStockQueryStockResponse queryJDStock(String goodsNo, Integer currentPage, Integer pageSize) {
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpStockQueryStockRequest request = new EclpStockQueryStockRequest();
        request.setDeptNo(departmentNo);
        request.setWarehouseNo(warehouseNo);
        request.setStockStatus("1");
        request.setStockType("1");
        request.setGoodsNo(goodsNo);
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);
        request.setReturnZeroStock(1);
        EclpStockQueryStockResponse response = null;
        try {
            response = client.execute(request);
            log.info("==========:{}", JsonUtils.objectToJson(response));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }


    /**
     * 查询序列号
     *
     * @param eclpSoNo
     * @param pageNo
     * @param pageSize
     * @return
     */
    public EclpSerialQueryPageSerialByBillNoResponse queryPageSerialByBillNo(String eclpSoNo, int pageNo, int pageSize) {
        JSONArray parse = JSONArray.parseArray(eclpSoNo);
        String accessToken = RedisUtil.get("jd:access_token");
        JdClient client = new DefaultJdClient(SERVER_URL, accessToken, appKey, appSecret);
        EclpSerialQueryPageSerialByBillNoRequest request = new EclpSerialQueryPageSerialByBillNoRequest();
        request.setBillNo(parse.get(0).toString());
        request.setBillType((byte) 24);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        request.setQueryType((byte) 1);
        EclpSerialQueryPageSerialByBillNoResponse response;
        try {
            response = client.execute(request);
            log.info("queryPageSerialByBillNo -->respose={}", JSON.toJSONString(response));
        } catch (Exception e) {
            log.error("queryPageSerialByBillNo error:{0}", e);
            return null;
        }
        return response;
    }


    public static EclpSerialQueryPageSerialByBillNoResponse queryPageSerialByBillNoTest() {
        JdClient client = new DefaultJdClient("https://api.jd.com/routerjson", "0d5cc377-fbe1-497f-84c6-437c3a4cfe99", "0C2AC54D67CD3E5ACD7EC00EFE395BAC", "4cb1120cda6f47b2ad46527e226f62ed");
        EclpSerialQueryPageSerialByBillNoRequest request = new EclpSerialQueryPageSerialByBillNoRequest();
        request.setBillNo("ESL4419421986409");
        request.setBillType((byte) 24);
        request.setPageNo(1);
        request.setPageSize(10);
        request.setQueryType((byte) 1);
        EclpSerialQueryPageSerialByBillNoResponse response;
        try {
            response = client.execute(request);
            log.info("queryPageSerialByBillNo -->respose={}", JSON.toJSONString(response));
        } catch (Exception e) {
            log.error("queryPageSerialByBillNo error:{0}", e);
            return null;
        }
        return response;
    }

    public static void main(String[] args) {
        queryPageSerialByBillNoTest();
    }
}
