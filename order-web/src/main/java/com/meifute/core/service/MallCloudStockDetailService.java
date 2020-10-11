package com.meifute.core.service;

import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.mmall.common.utils.IDUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
public class MallCloudStockDetailService {
    @Autowired
    AgentFeign agentFeign;

    /**
     * 设置入库详情信息
     */
    public MallCloudStockDetail setCloudStockDetail(MallUser user, MallOrderItem mallOrderItem, String orderType, String source) {
        MallCloudStockDetail mallCloudStockDetail = new MallCloudStockDetail();
        // 1.根据用户等级和skuCode查询当前产品价格
        MallPriceDetail mallPriceDetail = new MallPriceDetail();
        mallPriceDetail.setAgentLevel(user.getRoleId());
        mallPriceDetail.setSkuCode(mallOrderItem.getSkuCode());
        mallPriceDetail = agentFeign.queryPriceByParam(mallPriceDetail);

        mallCloudStockDetail.setId(IDUtils.genId());
        mallCloudStockDetail.setMallUserId(user.getId());
        mallCloudStockDetail.setSkuCode(mallOrderItem.getSkuCode());
        mallCloudStockDetail.setPricePlanId(mallPriceDetail.getPricePlanId());
        mallCloudStockDetail.setPutInAmount(Math.abs(mallOrderItem.getAmount().longValue()));
        mallCloudStockDetail.setRemainAmount(Math.abs(mallOrderItem.getAmount().longValue()));
        mallCloudStockDetail.setAgentLevel(user.getRoleId());
        mallCloudStockDetail.setStockSource(source);
        mallCloudStockDetail.setOrderType(orderType);
        mallCloudStockDetail.setRelationId(mallOrderItem.getOrderId());//订单号
        return mallCloudStockDetail;
    }

    public List<MallCloudStockDetail> queryCloudDetail(MallUser user, MallOrderItem mallOrderItem) {
        //1.根据mallUserId+skuCode+relationId查询入库详情记录
        MallCloudStockDetail mallCloudStockDetail = new MallCloudStockDetail();
        mallCloudStockDetail.setMallUserId(user.getId());
        mallCloudStockDetail.setSkuCode(mallOrderItem.getSkuCode());
        //mallCloudStockDetail.setRelationId(mallOrderItem.getOrderId());
        List<MallCloudStockDetail> mallCloudStockDetails = agentFeign.queryCloudDetail(mallCloudStockDetail);
        return mallCloudStockDetails;

    }

    /**
     * 先进先出,逐条扣除剩余库存
     */
    public void reduceRemainAmount(MallUser user, MallUser toUser, MallOrderItem mallOrderItem, String orderType) {
        //绝对值
        long amount = Math.abs(mallOrderItem.getAmount().longValue());
        List<MallCloudStockDetail> collect = this.queryCloudStockDetailOrderByCreateDate(user, mallOrderItem);
        if (!CollectionUtils.isEmpty(collect)) {
            for (MallCloudStockDetail detail : collect) {
                Long remainAmount = detail.getRemainAmount();
                if (remainAmount >= amount) {
                    //一条足以扣减
                    detail.setRemainAmount(detail.getRemainAmount() - amount);
                    agentFeign.updateCloudDetail(detail);
                    this.saveAboutExchange(detail, toUser, mallOrderItem, amount);
                    break;
                } else {
                    //该条的剩余库存不够扣减,扣减这条,再次循环扣减
                    this.saveAboutExchange(detail, toUser, mallOrderItem, remainAmount);
                    detail.setRemainAmount(0L);
                    agentFeign.updateCloudDetail(detail);
                    amount = amount - remainAmount;
                }
            }
        }

    }

    /**
     * 先进先出原则查询cloudStockDetail
     *
     * @param user
     * @param mallOrderItem
     * @return
     */
    public List<MallCloudStockDetail> queryCloudStockDetailOrderByCreateDate(MallUser user, MallOrderItem mallOrderItem) {
        List<MallCloudStockDetail> collect = new ArrayList<>();
        MallCloudStockDetail param = new MallCloudStockDetail();
        param.setMallUserId(user.getId());
        param.setSkuCode(mallOrderItem.getSkuCode());
        //查询该用户下所有进货数据
        List<MallCloudStockDetail> mallCloudStockDetails = agentFeign.queryCloudDetail(param);
        if (!CollectionUtils.isEmpty(mallCloudStockDetails)) {
            //剩余库存大于0并且先进先出原则
            collect = mallCloudStockDetails.stream().filter(detail -> detail.getRemainAmount() > 0).sorted(Comparator.comparing(MallCloudStockDetail::getCreateDate)).collect(Collectors.toList());
        }
        return collect;
    }

    /**
     * 转货相关操作
     *
     * @return
     */
    public void saveAboutExchange(MallCloudStockDetail detail, MallUser toUser, MallOrderItem mallOrderItem, Long amount) {

        //新增转货记录
        MallCloudStockDetail mallCloudStockDetail = this.setCloudStockDetail(toUser, mallOrderItem, "0", "4");
        mallCloudStockDetail.setPutInAmount(amount);
        mallCloudStockDetail.setRemainAmount(amount);
        agentFeign.insertCloudDetail(mallCloudStockDetail);
        //新增换货操作日志 transfer_log
        MallTransformLog mallTransformLog = new MallTransformLog();
        mallTransformLog.setId(IDUtils.genId());
        mallTransformLog.setCloudDetailIdFrom(detail.getId());
        mallTransformLog.setCloudDetailIdTo(mallCloudStockDetail.getId());
        mallTransformLog.setCreateDate(new Date());
        mallTransformLog.setUpdateDate(new Date());
        mallTransformLog.setIsDel("0");
        agentFeign.saveTransferLog(mallTransformLog);

    }

    public void backFillCloudStockDetail(MallUser user,MallOrderItem item){
        List<MallCloudStockDetail> mallCloudStockDetails = this.queryCloudDetail(user, item);
        if (CollectionUtils.isEmpty(mallCloudStockDetails)) {
            //新增一条入库详情
            MallCloudStockDetail detail = this.setCloudStockDetail(user, item, "1", "1");
            agentFeign.insertCloudDetail(detail);
        } else {
            //回填查询出来的记录
            mallCloudStockDetails = mallCloudStockDetails.stream().sorted(Comparator.comparing(MallCloudStockDetail::getCreateDate).reversed()).collect(Collectors.toList());

            long needToSave = Math.abs(item.getAmount().longValue());
            for (MallCloudStockDetail detail : mallCloudStockDetails) {
                //每条记录可回填数量为 进货数量-转货数量-剩余数量
                long canSaveAmount = detail.getPutInAmount() - detail.getChangeAmount() - detail.getRemainAmount();
                if (canSaveAmount >= needToSave && canSaveAmount > 0 && needToSave >0) {
                    //一条记录可以满足
                    detail.setRemainAmount(detail.getRemainAmount() + needToSave);
                    detail.setUpdateDate(new Date());
                    agentFeign.updateCloudDetail(detail);
                    break;
                } else if(canSaveAmount < needToSave && canSaveAmount > 0 && needToSave > 0) {
                    //多条回填
                    detail.setRemainAmount(detail.getPutInAmount());
                    detail.setUpdateDate(new Date());
                    agentFeign.updateCloudDetail(detail);
                    needToSave = needToSave - canSaveAmount;
                }

            }
        }
    }




}
