package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.dto.ItemOrderResultDTO;
import com.meifute.core.dto.LoginDto;
import com.meifute.core.dto.OrderInfoDetailDto;
import com.meifute.core.dto.OrderItemDetailDto;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallTransferGoodsMapper;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.MallOrderStatusEnum;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallOrderVerifyStatusEnum;
import com.meifute.core.mmall.common.enums.MallReviewEnum;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ExcelUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.SearchOrderParam;
import com.meifute.core.vo.SelectOrderParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: wuxb
 * @Date: 2019-02-25 11:33
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class MallNewOrderInfoService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private MallOrderInfoMapper orderInfoMapper;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private MallTransferGoodsMapper mallTransferGoodsMapper;

    private static String WAREHOUSE_IMPORT_EXCEL = "order:warehouse:excel:";



    public Page getAllOrder(SearchOrderParam param){
        List<MallOrderInfo> list = new ArrayList<>();
        Page page = MybatisPageUtil.getPage(param.getPageCurrent(), param.getPageSize());
        List<String> orderIds = orderInfoMapper.getAllOrdersByInput(param.getSearchName(), UserUtils.getCurrentUser().getId(), page);
        if(!CollectionUtils.isEmpty(orderIds)){
            for(String orderId:orderIds){
                MallOrderInfo mallOrderInfo = orderInfoMapper.selectOrderById(orderId);
                list.add(mallOrderInfo);
            }
            List<OrderInfoDetailDto> infos = getNewOrderInfos(list);
            page.setRecords(infos);
        }
        return page;
    }

    public Page<OrderInfoDetailDto> searchOrderInfo(SearchOrderParam param, Page page){
        Page<OrderInfoDetailDto> list = null;
        MallUser user = UserUtils.getCurrentUser();
        switch (param.getType()) {
            case 1: //进货
                list = getThisSearchOrderResult(param, user.getId(), page, Arrays.asList("1", "3"),"0");
                break;
            case 2: //出货
                list = getThisSearchOrderResult(param, user.getId(), page, Arrays.asList("0", "2", "4"),"0");
                break;
            case 3: //积分
                list = getThisSearchOrderResult(param, user.getId(), page, Arrays.asList("0"),"1");
                break;
            case 4:
                list = getAllOrder(param, user.getId(), page, Arrays.asList("0","1","2","3","4"),"");
                break;
        }
        //todo 商务要求不在APP展示他们后台的备注
        if(!CollectionUtils.isEmpty(list.getRecords())){
            List<OrderInfoDetailDto> records = list.getRecords();
            records.forEach(orderInfoDetailDto -> {
                orderInfoDetailDto.setCsMemo("");
            });
        }
        return list;
    }

    public Page<OrderInfoDetailDto> getAllOrder(SearchOrderParam param, String userId, Page page, List<String> orderTypes,String belongsCode){
        log.info("=========param:{}",param);
        List<OrderInfoDetailDto> list2 = getOrderByAddressName(param,userId,page,orderTypes,belongsCode);
        page.setRecords(list2);
        return page;
    }



    private Page<OrderInfoDetailDto> getThisSearchOrderResult(SearchOrderParam param, String userId, Page<MallOrderInfo> page, List<String> orderTypes,String belongsCode) {
        List<OrderInfoDetailDto> list = getOrderBySkuCode(param,userId,page,orderTypes,belongsCode);
        if(CollectionUtils.isEmpty(list)) {
            list = getOrderByAddressName(param,userId,page,orderTypes,belongsCode);
        }
        Page pageResult = new Page();
        BeanUtils.copyProperties(page, pageResult, "records");
        pageResult.setRecords(list);
        return pageResult;
    }

    private List<OrderInfoDetailDto> getOrderBySkuCode(SearchOrderParam param, String userId, Page<MallOrderInfo> page, List<String> orderTypes,String belongsCode) {
        List<MallSku> skus = itemFeign.getSkuByTitle(param.getSearchName(),belongsCode);
        List<String> skuCodes = null;
        if(!CollectionUtils.isEmpty(skus)) {
            skuCodes = skus.stream().map(MallSku::getSkuCode).collect(Collectors.toList());
        }
        List<MallOrderInfo> orderInfos = null;
        if(skuCodes != null) {
            SelectOrderParam selectOrderParam = new SelectOrderParam();
            selectOrderParam.setSkuCodeList(skuCodes);
            selectOrderParam.setOrderTypes(orderTypes);
            selectOrderParam.setMallUserId(userId);
            orderInfos = orderInfoMapper.searchOrderInfo(selectOrderParam,page);
        }
        return getNewOrderInfos(orderInfos);
    }

    private List<OrderInfoDetailDto> getOrderByAddressName(SearchOrderParam param, String userId, Page<MallOrderInfo> page, List<String> orderTypes,String belongsCode) {
        List<MallOrderInfo> orderInfos = orderInfoMapper.selectPage(page,
                new EntityWrapper<MallOrderInfo>()
                        .like(!StringUtils.isEmpty(param.getSearchName()),"addr_name", param.getSearchName())
                        .eq(!StringUtils.isEmpty(belongsCode),"belongs_code", belongsCode)
                        .eq("mall_user_id", userId)
                        .eq("is_del",0)
                        .in(!CollectionUtils.isEmpty(orderTypes),"order_type", orderTypes)
                        .orderBy("create_date",false));
        return getNewOrderInfos(orderInfos);
    }

    private List<OrderInfoDetailDto> getNewOrderInfos(List<MallOrderInfo> orderInfos) {
        List<OrderInfoDetailDto> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(orderInfos)) {
            orderInfos.forEach(p -> {
                OrderInfoDetailDto orderInfoDetailDto = new OrderInfoDetailDto();
                List<OrderItemDetailDto> dto = orderInfoService.orderItemByOrderId(p.getOrderId(), 0);
                BigDecimal goodsAmount = BigDecimal.ZERO;
                for (OrderItemDetailDto d : dto) {
                    goodsAmount = goodsAmount.add(d.getAmount().abs());
                }
                String addrName = "";
                String addrPhone = "";
                String addrId = "";
                if (MallOrderTypeEnum.ORDER_TYPE_004.getCode().equals(p.getOrderType())) {
                    List<MallTransferGoods> mallTransferGoods = mallTransferGoodsMapper.selectList(new EntityWrapper<MallTransferGoods>()
                            .eq("relation_id", p.getOrderId())
                            .eq("relation_type", "0"));
                    if (!CollectionUtils.isEmpty(mallTransferGoods)) {
                        String nextProxyId = mallTransferGoods.get(0).getNextProxyId();
                        MallUser userById = userFeign.getUserById(nextProxyId);
                        LoginDto dt = new LoginDto();
                        BeanMapper.copy(userById, dt);
                        dt.setRoleId(userById.getRoleId());
                        orderInfoDetailDto.setTransPortUser(dt);
                        addrName = userById.getName();
                        addrPhone = userById.getPhone();
                        addrId = p.getOrderDescribe();
                    } else {
                        orderInfoDetailDto.setTransPortUser(null);
                    }
                } else if (MallOrderTypeEnum.ORDER_TYPE_003.getCode().equals(p.getOrderType())) {
                    //金额小计
                    BigDecimal inSubtotalPrice = new BigDecimal(0);
                    //金额小计
                    BigDecimal outSubtotalPrice = new BigDecimal(0);
                    if (!CollectionUtils.isEmpty(dto)) {
                        for (OrderItemDetailDto detailDto : dto) {
                            if (MallReviewEnum.ITEM_OUT_001.getCode().equals(detailDto.getType())) {
                                outSubtotalPrice = outSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            } else {
                                inSubtotalPrice = inSubtotalPrice.add(detailDto.getPrice().multiply(detailDto.getAmount().abs()));
                            }
                        }
                    }
                    if (inSubtotalPrice.compareTo(outSubtotalPrice) > 0) {
                        orderInfoDetailDto.setPayExchangeAmt(inSubtotalPrice.subtract(outSubtotalPrice).setScale(2,BigDecimal.ROUND_HALF_UP));
                    } else {
                        orderInfoDetailDto.setSendBackExchangeAmt(outSubtotalPrice.subtract(inSubtotalPrice).setScale(2,BigDecimal.ROUND_HALF_UP));
                    }
                }
                BeanMapper.copy(p, orderInfoDetailDto);
                if(ObjectUtils.isNullOrEmpty(p.getAddrId())) {
                    orderInfoDetailDto.setAddrName(addrName);
                    orderInfoDetailDto.setAddrPhone(addrPhone);
                    orderInfoDetailDto.setAddrId(addrId);
                }
                orderInfoDetailDto.setGoodsAmount(goodsAmount);
                //商品信息
                orderInfoDetailDto.setOrderItemDetailDtos(dto);
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getPayExchangeAmt())) {
                    orderInfoDetailDto.setPayExchangeAmt(new BigDecimal(0));
                }
                if (ObjectUtils.isNullOrEmpty(orderInfoDetailDto.getSendBackExchangeAmt())) {
                    orderInfoDetailDto.setSendBackExchangeAmt(new BigDecimal(0));
                }

                if (!MallOrderTypeEnum.ORDER_TYPE_007.getCode().equals(p.getOrderType())) {
                    list.add(orderInfoDetailDto);
                }
            });
        }
        return list;
    }




    @Transactional
    public String importExcel(InputStream inputStream, HttpServletResponse response) throws Exception {

        Sheet sheet = WorkbookFactory.create(inputStream).getSheetAt(0);

        List<HashMap<String, String>> list = new ArrayList<>();
        List<ItemOrderResultDTO> dtos = new ArrayList<>();
        if (sheet == null) {
            HashMap<String, String> map = new HashMap();
            map.put("000000", "糟了,文件传输失败 -_- ");
            list.add(map);
            ItemOrderResultDTO dto = new ItemOrderResultDTO();
            dto.setOrderId("000000");
            dto.setReason("糟了,文件传输失败 -_-");
            dtos.add(dto);
        } else {
            log.info(sheet.getLastRowNum() + "");
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    //如果是空行（即没有任何数据、格式），直接把它以下的数据往上移动
                    sheet.shiftRows(i + 1, sheet.getLastRowNum(), -1);
                    continue;
                }
                String orderId = ExcelUtil.getCellStringValue(row.getCell(0));
                try {
                    if (ObjectUtils.isNullOrEmpty(orderId)){
                        HashMap<String, String> map = new HashMap<>();
                        map.put("第"+i+"行数据订单编号为空", "订单号不能为空");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId("第"+i+"行数据订单编号为空");
                        dto.setReason("订单号不能为空");
                        dtos.add(dto);
                        continue;
                    }

                    String expressCode = ExcelUtil.getCellStringValue(row.getCell(1));
                    if (ObjectUtils.isNullOrEmpty(expressCode)){
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "物流号不能为空");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("物流号不能为空");
                        dtos.add(dto);
                        continue;
                    }

                    String expressCompany = ExcelUtil.getCellStringValue(row.getCell(2));
                    MallOrderInfo info = orderInfoMapper.selectById(orderId);
                    if (ObjectUtils.isNullOrEmpty(info) || "1".equals(info.getIsDel())) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "未找到订单");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("未找到订单");
                        dtos.add(dto);
                        continue;
                    }

                    //已发货，有物流单号
                    if (MallOrderStatusEnum.ORDER_STATUS_004.getCode().equals(info.getOrderStatus())&& ObjectUtils.isNotNullAndEmpty(info.getExpressCode())) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "该订单为待收货状态，已存在物流单号。");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("该订单为待收货状态，已存在物流单号。");
                        dtos.add(dto);
                        continue;
                    }

                    if (!MallOrderStatusEnum.ORDER_STATUS_003.getCode().equals(info.getOrderStatus())){
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "该订单状态不为待发货");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("该订单状态不为待发货");
                        dtos.add(dto);
                        continue;
                    }

                    if (!"2".equals(info.getLogisticsMode())){
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "该订单不为仓库发货");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("该订单不为仓库发货");
                        dtos.add(dto);
                        continue;
                    }

                    info.setExpressCompany(expressCompany);
                    info.setExpressCode(expressCode);
                    info.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
                    info.setDeliverGoodsDate(new Date());
                    info.setUpdateDate(new Date());
                    info.setIsCanCancel("1");
                    orderInfoMapper.updateById(info);
                } catch (Exception e) {
                    log.info("=========orderId有未知异常！！！:{}", orderId);
                    log.info("Exception:{}", e);
                    HashMap<String, String> map = new HashMap<>();
                    map.put(orderId, "未知异常");
                    list.add(map);
                    ItemOrderResultDTO dto = new ItemOrderResultDTO();
                    dto.setOrderId(orderId);
                    dto.setReason("未知异常");
                    dtos.add(dto);
                }
            }
        }
        String key = UUID.randomUUID().toString();
        RedisUtil.set(WAREHOUSE_IMPORT_EXCEL + key, JSONUtil.obj2json(dtos), 60 * 60);
//        log.info("dtos:{}", dtos);
        return key;
    }

    public String getImport(String key, HttpServletResponse response) throws Exception {
        String s1 = RedisUtil.get(WAREHOUSE_IMPORT_EXCEL + key);
        List<ItemOrderResultDTO> dtos = JSONUtil.json2list(s1, ItemOrderResultDTO.class);
        //处理结果集
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = new LinkedHashMap();
        map.put("订单编号", "orderId");
        map.put("错误原因", "reason");
//        log.info("dtos:{}", dtos);
        String s = excelUtil.buildExcel(map, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;
    }






}
