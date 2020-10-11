package com.meifute.core.service;

import com.meifute.core.dto.MallUserAccountDto;
import com.meifute.core.dto.OrderInfoDto;
import com.meifute.core.dto.SkuSpecAmount;
import com.meifute.core.entity.MallOrderInfo;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallUser;
import com.meifute.core.entity.MallUserAddress;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mmall.common.enums.MallOrderTypeEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.utils.PriceUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.ItemVo;
import com.meifute.core.vo.PreOrderFromGoodsParam;
import com.meifute.core.vo.SkuAndAmountParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.meifute.core.mmall.common.enums.MallReviewEnum.IS_SUB_STOCK_001;

/**
 * @Auther: wxb
 * @Date: 2018/10/16 14:52
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class PlaceGoodsOrderService {

    @Autowired
    private UserFeign userFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private CommonOrderService commonOrderService;
    @Autowired
    private ExpressCompanyService expressCompanyService;


    /**
     * 下单操作
     * @param param
     */
    public OrderInfoDto orderOneHandle(PreOrderFromGoodsParam param, MallSku sku, MallUser mallUser, List<ItemVo> itemVos) {
        //代理等级
        int agentLevel = UserUtils.getAgentLevel(mallUser.getId());
        //运费
        BigDecimal freight = BigDecimal.ZERO;
        //地址
        MallUserAddress address = null;
        SkuSpecAmount skuSpecAmount = new SkuSpecAmount();
        List<SkuAndAmountParam> listSku = new ArrayList<>();
        // 算运费
        if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(param.getOrderType())) {
            //1 产品商城
            address = userFeign.queryAddressById(param.getAddrId());
            if (MallStatusEnum.BELONGS_CODE_000.getCode().equals(sku.getBelongsCode())) {
                SkuAndAmountParam paramSpec = new SkuAndAmountParam();
                paramSpec.setAmount(String.valueOf(param.getAmount()));
                paramSpec.setSkuCode(param.getSkuCode());
                listSku.add(paramSpec);
                skuSpecAmount.setAmountParams(listSku);
                skuSpecAmount.setProvince(address.getProvice());
                skuSpecAmount.setCity(address.getArea().split(" ")[1]);
                skuSpecAmount.setLogisticsType(param.getLogisticsType());
                freight = itemFeign.getPostFee(skuSpecAmount);
            }
        }

        // 产品金额
        BigDecimal priceAmt = BigDecimal.ZERO;
        // 123级可减产品商城的库存金额
        BigDecimal isSubStockAmt = BigDecimal.ZERO;
        // 金额币种
        if (MallStatusEnum.CURRENCY_CODE_000.getCode().equals(sku.getCurrency())) {
            priceAmt = PriceUtil.getPrice(sku.getRetailPrice(), agentLevel);
            priceAmt = priceAmt.multiply(param.getAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);
            if (IS_SUB_STOCK_001.getCode().equals(sku.getIsSubStock())) {
                isSubStockAmt.add(priceAmt).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }

        // 积分币种
        if (MallStatusEnum.CURRENCY_CODE_001.getCode().equals(sku.getCurrency())) {
            priceAmt = new BigDecimal(sku.getRetailPrice());
            priceAmt = priceAmt.multiply(param.getAmount()).setScale(2, BigDecimal.ROUND_HALF_UP);
        }

        //物流方式
        String expressType = screenExpress(itemVos);

        // 定义订单实体
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        BeanUtils.copyProperties(param, mallOrderInfo);
        mallOrderInfo.setLogisticsMode(expressType);

        //记录当前等级
        mallOrderInfo.setCurrentLevel(mallUser.getRoleId());

        // 1.生成订单信息
        MallOrderInfo orderInfo = commonOrderService.createOrderInfo(mallOrderInfo, priceAmt, sku.getBelongsCode(), sku.getCurrency(), address, freight);
        // 2.生成支付信息
        commonOrderService.createPayInfo(itemVos, orderInfo, mallUser.getId());
        // 3.生成商品记录信息
        commonOrderService.createOrderItemInfo(itemVos, Arrays.asList(orderInfo), agentLevel);

        MallUserAccountDto account = userFeign.accountInfo(mallUser.getId());
        //封装返回参数
        OrderInfoDto orderInfoDto = new OrderInfoDto();
        orderInfoDto.setBalance(account.getAmt());
        orderInfoDto.setCredit(account.getCredit());
        orderInfoDto.setPaymentAmt(orderInfo.getPaymentAmt());
        orderInfoDto.setOriginAmt(orderInfo.getOriginAmt());
        orderInfoDto.setPostFee(orderInfo.getPostFeeAmt());
        orderInfoDto.setPayCredit(orderInfo.getCredit());
        List<MallOrderInfo> list = new ArrayList<>();
        list.add(orderInfo);
        orderInfoDto.setOrderInfoList(list);

        return orderInfoDto;
    }


    public String screenExpress(List<ItemVo> itemVos) {
        List<String> express = new ArrayList<>();
        for (ItemVo item : itemVos) {
            MallSku sku = itemFeign.getSkuByCode(item.getSkuCode());
            express.add(sku.getLogisticsMode());
        }
//        List<String> collect = express.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        return expressCompanyService.getHighestExpressCode(express);
    }


}
