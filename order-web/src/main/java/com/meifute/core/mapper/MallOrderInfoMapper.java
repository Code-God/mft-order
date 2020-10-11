package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.dto.*;
import com.meifute.core.dto.report.InputOutputItemReportDTO;
import com.meifute.core.dto.report.MonthlyOrderReportResponseDTO;
import com.meifute.core.dto.report.OrderReportRequest;
import com.meifute.core.entity.*;
import com.meifute.core.entity.activity.MallAcOrder;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.model.AgentPhoneAndAmount;
import com.meifute.core.vo.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author liuzh
 * @since 2018-09-25
 */
@Repository
public interface MallOrderInfoMapper extends BaseMapper<MallOrderInfo> {

    List<MallOrderInfo> queryOrderInfoListByPages(
            @Param("param") MallOrderInfo mallOrderInfo,
            Pagination page
    );

    List<MallOrderInfo> queryExchangeOrderInfoList(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    /**
     * 获取所有的订单列表
     *
     * @param mallOrderInfo
     * @return
     */
    List<MallOrderInfo> queryAllOrderInfoListPages(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    //所有订单,分页
    List<MallOrderInfo> queryOrderList(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    List<MallOrderInfo> doExcel(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderInfo> doWarehouseExcel(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderInfo> doWarehouseExcelToJD(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderInfo> queryCreditOrderInfoPages(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    List<MallOrderInfo> queryUserOrderInfoPages(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    List<MallOrderInfo> queryOrderEclpSONo();

    List<MallOrderVerify> getOrderVerifyByOrderId(String id);

    List<String> getOrderPush();

    int getWaringSku(WaringSkuParam waringSkuParam);

    List<ItemOrderExcelDTO> getItemOrderExcel(@Param("param") GetOrderPageListParam param);

    List<ItemAndSkuDTO> getItemSkuByOrderId(String id);

    List<OrderItemDetailDto> getItemsByOrderID(@Param("orderId") String orderId);

    List<MallOrderItem> queryUserMonthSales(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderItem> queryUserMonthStock(@Param("param") MallOrderInfo mallOrderInfo);

    MallOrderInfo getFirstOutItemOrderInfo(@Param("mallUserId") String mallUserId);

    List<MallOrderInfo> searchOrderInfo(@Param("param") SelectOrderParam param, Pagination page);

    List<MallOrderItem> queryTemMonthSales(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderItem> queryTeamMonthStock(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderInfo> queryJDExpressCode();

    MallOrderInfo selectOrderById(String orderId);

    BigDecimal getSumOrderAmountByAgent(@Param("param") MallAgent agent);

    BigDecimal getOutOrderSumPrice(@Param("param") MallAgent agent);

    List<MallOrderItem> getOrderByAgentList(@Param("param") MallAgent agent);

    List<MallOrderItem> getOutOrderItems(@Param("param") MallAgent agent);

    //订单统计报表
    List<MonthlyOrderReportResponseDTO> orderReportByParams(@Param("param") OrderReportRequest orderReportRequest);

    //订单统计报表之订单数量统计
    List<MonthlyOrderReportResponseDTO> orderReportByTotalAmount(@Param("param") OrderReportRequest orderReportRequest);

    List<OrderItemReportDTO> selectOrderItemReport(@Param("param") BaseParam param);

    Integer selectTotalInputOrderItemCount(@Param("param") BaseParam param);

    Integer selectTotalOutputOrderItemCount(@Param("param") BaseParam param);

    Integer selectInputItem(@Param("param") BaseParam param);

    Integer selectOutputItem(@Param("param") BaseParam param);

    List<InputOutputItemReportDTO> selectOutputItemDetail(@Param("param") BaseParam param);

    //筛选商务code为null的订单,上一级
    List<MallOrderInfo> queryOrderListFirstParent(@Param("param") MallOrderInfo mallOrderInfo);

    MallAdminAgent getAdminAgentByUserId(@Param("userId") String userId);

    //根据商务code查询
    List<MallOrderInfo> queryOrderListByAdminCode(@Param("param") MallOrderInfo mallOrderInfo, Page page);

    List<AgentPhoneAndAmount> getRetailPurchases();

    List<MallOrderInfo> queryWarehouseOrderInfo(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

//    List<MallOrderInfo> queryWarehouseOrderInfo(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallAcOrder> getAcOrderEclpSoNo();


    List<String> getAllOrdersByInput(@Param("key") String key, @Param("userId") String userId, Pagination page);

    Integer getNowDaysC036(@Param("userId") String userId);

    Integer getNowDaysC036New(@Param("userId") String userId);

    List<String> temporaryPushToJd(TemPushToJd temPushToJd);

    List<QueryPushOrderInfoDto> queryOrderToPush(@Param("param") GetOrderPageListParam param, Pagination page);

    List<OrderItemDetailDto> queryOrderItemDetail(@Param("param") MallOrderInfo param);

    Integer getNewGoodsCount(OutGoodsDate outGoodsDate);

    Integer getNewGoodsCount2(OutGoodsDate outGoodsDate);

    Integer getNewGoodsCountExchangeOut(OutGoodsDate outGoodsDate);

    List<String> querySplitOrderIds(@Param("param") MallOrderInfo mallOrderInfo);

    List<MallOrderInfo> querySplitOrderInfo(@Param("orderIds") List<String> orderIds, Page page);

    Integer getTakeOfItemCount(CheckOutGoods checkOutGoods);

    Integer getTakeOfItemCountEveryDay(CheckOutGoods checkOutGoods);

    List<String> querySplitOrderIds2(@Param("param") MallOrderInfo mallOrderInfo);

    List<String> getLeakageSlipOrder();

    List<MallSkuSpec> getMallSkuSpec(@Param("skuCode") String skuCode, @Param("expressType") String expressType);

    List<QueryShipmentMoreThan20Dto> queryShipmentMoreThan10(@Param("dateFrom") String dateFrom, @Param("dateTo") String dateTo);

    List<MallOrderInfo> getNoSerialNumberOrderInfo(@Param("startTime") Date startTime);

    List<MallOrderInfo> queryOrderListBySerialNumber(@Param("param") MallOrderInfo mallOrderInfo, Pagination page);

    List<String> getOrderIdsByPayInfoRefundParam(@Param("param") PayInfoRefundVO param);

    List<String> getOrderIdBySerialNumber(@Param("serialNumber") String serialNumber, @Param("securityNumber") String securityNumber);

    List<OrderInfoByInputDTO> getThreeOrder(@Param("userId") String userId);

    List<MallOrderInfo> getNotFinishOrder(@RequestParam("userId") String userId);

    List<UserOrderAmountDTO> queryOrderAmountByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<UserOrderAmountDTO> queryOrderAmountByUserIds(@Param("param") UserIdsDateParam param);

    List<String> getOrderIdsByStockInfoParam(@Param("userId") String userId, @Param("param") String param);

    Integer updateForceReceivingGoods();

    Integer doExcelCount(@Param("param") MallOrderInfo mallOrderInfo);
}
