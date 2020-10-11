package com.meifute.core.service;

import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.entity.MallCart;
import com.meifute.core.entity.MallCloudStock;
import com.meifute.core.entity.MallSku;
import com.meifute.core.entity.MallUser;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.mmall.common.enums.MallReviewEnum;
import com.meifute.core.mmall.common.enums.MallStatusEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.CheckOrderGoodsParam;
import com.meifute.core.vo.ExchangeGoodsItemParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Auther: wxb
 * @Date: 2018/10/29 13:17
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Service
public class CheckOrderService {

    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private AgentFeign agentFeign;

    /**
     * 判断所选的商品是否可以生成订单
     * @param checkOrderGoodsParam
     * @return
     */
    public Boolean checkOrderGoods(CheckOrderGoodsParam checkOrderGoodsParam) {
        MallUser user = UserUtils.getCurrentUser();
        List<ExchangeGoodsItemParam> exchangeGoodsItemParams = checkOrderGoodsParam.getExchangeGoodsItemParams();
        List<String> cartIds = checkOrderGoodsParam.getCartIds();
        int in = 0;
        int out = 0;
        if (ObjectUtils.isNotNullAndEmpty(exchangeGoodsItemParams)) {
            for (ExchangeGoodsItemParam itemIdAndSkuAndType : exchangeGoodsItemParams) {
                MallSku mallSku = itemFeign.getSkuByParam(MallSku.builder().itemId(itemIdAndSkuAndType.getItemId()).skuCode(itemIdAndSkuAndType.getSkuCode()).build());

                if (MallReviewEnum.ITEM_IN_000.getCode().equals(itemIdAndSkuAndType.getType())) {
                    String key = RedisUtil.getItemStockKey(itemIdAndSkuAndType.getItemId(), itemIdAndSkuAndType.getSkuCode());
                    String stock = ObjectUtils.isNullOrEmpty(RedisUtil.get(key)) ? "0" : RedisUtil.get(key);

                    if (new BigDecimal(itemIdAndSkuAndType.getAmount()).compareTo(new BigDecimal(stock)) > 0) {
                        throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{mallSku.getTitle()});
                    }
                    in += Integer.parseInt(itemIdAndSkuAndType.getAmount());
                }
                if (MallReviewEnum.ITEM_OUT_001.getCode().equals(itemIdAndSkuAndType.getType())) {
                    MallCloudStock mallCloudStock = new MallCloudStock();
                    mallCloudStock.setMallUserId(user.getId());
                    mallCloudStock.setItemId(itemIdAndSkuAndType.getItemId());
                    mallCloudStock.setSkuCode(itemIdAndSkuAndType.getSkuCode());
                    MallCloudStock cloudStock = agentFeign.getCloudStock(mallCloudStock);
                    if (new BigDecimal(itemIdAndSkuAndType.getAmount()).compareTo(cloudStock.getStock()) > 0) {
                        throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{mallSku.getTitle()});
                    }
                    out += Integer.parseInt(itemIdAndSkuAndType.getAmount());
                }
            }

            if (in <= 0) {
                throw new MallException(OrderRespCode.IN_IS_NOT);
            }
            if (out <= 0) {
                throw new MallException(OrderRespCode.OUT_IS_NOT);
            }
        }

        if(ObjectUtils.isNotNullAndEmpty(cartIds)) {
            for (String cartId : cartIds) {
                MallCart mallCartInfo = agentFeign.getMallCartByParam(MallCart.builder().id(cartId).status(MallStatusEnum.IS_SETTLEMENT_001.getCode()).build());
                if (ObjectUtils.isNotNullAndEmpty(mallCartInfo)) {
                    MallSku mallSku = itemFeign.getSkuByParam(MallSku.builder().itemId(mallCartInfo.getItemId()).skuCode(mallCartInfo.getSkuCode()).build());
                    throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{mallSku.getTitle()});
                }
            }
        }


        return true;
    }
}
