package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.codingapi.tx.annotation.TxTransaction;
import com.meifute.core.component.errorcode.OrderRespCode;
import com.meifute.core.dto.*;
import com.meifute.core.dto.cloudStockDetail.TransferCloudDTO;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.*;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mapper.MallOrderVerifyMapper;
import com.meifute.core.mftAnnotation.distributedLock.annotation.RedisLock;
import com.meifute.core.mmall.common.check.MallPreconditions;
import com.meifute.core.mmall.common.dto.BeanMapper;
import com.meifute.core.mmall.common.enums.*;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ExcelUtil;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.util.UserUtils;
import com.meifute.core.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: liuzh
 * @Date: 2018/10/15 18:43
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class MallOrderVerifyService extends ServiceImpl<MallOrderVerifyMapper, MallOrderVerify> {

    @Autowired
    private MallOrderVerifyMapper mallOrderVerifyMapper;
    @Autowired
    private MallOrderInfoMapper mallOrderInfoMapper;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private NotifyFeign notifyFeign;
    @Autowired
    private MallOrderInfoService mallOrderInfoService;
    @Autowired
    private MallOrderItemService mallOrderItemService;
    @Autowired
    private CommonOrderService commonOrderService;
    @Autowired
    private MallTransferItemService transferItemService;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private AdminFeign adminFeign;
    @Autowired
    MallCloudStockDetailService mallCloudStockDetailService;

    private static String key = "order:vertify:excel:";

    @TxTransaction
    @Transactional
    public boolean insertOrderVerify(MallOrderVerify mallOrderVerify) {
        mallOrderVerify.setId(IDUtils.genId());
        mallOrderVerify.setCreateDate(new Date());
        return this.insert(mallOrderVerify);
    }

    /**
     * 敏感药审核
     *
     * @param param
     * @return
     */
    public String sensitiveGoodsVerify(ReviewGoodsVerifyParam param) {
        MallOrderVerify mallOrderVerify = new MallOrderVerify();
        mallOrderVerify.setId(param.getVerifyId());
        mallOrderVerify.setAdminId(param.getAdminId());
        mallOrderVerify.setVerifyMemo(param.getVerifyMemo());
        mallOrderVerify.setVerifyStatus(param.getVerifyStatus());
        return sensitiveGoodsVerify(mallOrderVerify);
    }

    /**
     * 敏感药审核
     *
     * @param mallOrderVerify
     * @return
     */
    @Transactional
    @TxTransaction(isStart = true)
    @RedisLock
    public String sensitiveGoodsVerify(MallOrderVerify mallOrderVerify) {

        // 查询该条审核单信息
        MallOrderVerify result = mallOrderVerifyMapper.selectById(mallOrderVerify.getId());
        // 查询订单信息
        MallOrderInfo orderInfo = mallOrderInfoMapper.selectById(result.getOrderId());

        if (MallOrderStatusEnum.ORDER_STATUS_007.getCode().equals(orderInfo.getOrderStatus()) || MallOrderStatusEnum.ORDER_STATUS_006.getCode().equals(orderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.ORDER_AREAY_END);
        }

        if (!MallOrderStatusEnum.ORDER_STATUS_008.getCode().equals(orderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.ORDER_AREADY_VERIFY);
        }
        if (!MallOrderVerifyEnum.VERIFY_STATUS_000.getCode().equals(result.getVerifyStatus())) {
            throw new MallException(OrderRespCode.ORDER_AREADY_VERIFY);
        }

        MallOrderVerify verify = new MallOrderVerify();
        verify.setId(mallOrderVerify.getId());
        verify.setAdminId(mallOrderVerify.getAdminId());
        verify.setVerifyMemo(mallOrderVerify.getVerifyMemo());
        verify.setUpdateDate(new Date());

        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(orderInfo.getOrderId());
        mallOrderInfo.setCsMemo(mallOrderVerify.getVerifyMemo());

        boolean push = false;
        // 商务审核通过
        if (MallOrderVerifyEnum.VERIFY_STATUS_001.getCode().equals(mallOrderVerify.getVerifyStatus())) {
            mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_003.getCode());
            // 更新审核单
            verify.setVerifyStatus(MallOrderVerifyEnum.VERIFY_STATUS_001.getCode());
            mallOrderVerifyMapper.updateById(verify);
            push = true;
        }

        // 商务审核不通过
        if (MallOrderVerifyEnum.VERIFY_STATUS_002.getCode().equals(mallOrderVerify.getVerifyStatus())) {
            mallOrderInfo.setIsCanCancel("1");
            mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_009.getCode());
            // 更新审核单
            verify.setVerifyStatus(MallOrderVerifyEnum.VERIFY_STATUS_002.getCode());
            // 回退
            backTakeItemOrDeliver(orderInfo);
            mallOrderVerifyMapper.updateById(verify);

            //todo 任意一款口腔泡沫18支上限下单限制,审核拒绝是恢复数量
//            List<MallOrderItem> mallOrderItems = mallOrderItemService.selectByOrderId(orderInfo.getOrderId());
//            commonOrderService.set18RecoveryItems(orderInfo.getMallUserId(),orderInfo.getOrderType(),mallOrderItems);
        }

        // 更新订单表
        mallOrderInfo.setUpdateDate(new Date());
        mallOrderInfoMapper.updateById(mallOrderInfo);

        if (push) {
            // 延迟一个小时推物流单，一个小时之内都可以取消订单
            commonOrderService.sendExpressMQ(mallOrderInfo.getOrderId());
        }
        // 推送
        sendNotify(orderInfo, result.getProposerId(), mallOrderVerify.getVerifyStatus(), verify.getId());
        return Const.VERIFY_ORDER;
    }

    private void backTakeItemOrDeliver(MallOrderInfo mallOrderInfo) {
        //查询用户信息
        MallUser user = userFeign.getUserById(mallOrderInfo.getMallUserId());
        // 订单对应的商品
        List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(mallOrderInfo.getOrderId());
        // 总代直发 退运费->并将商品入云
        if (MallOrderTypeEnum.ORDER_TYPE_000.getCode().equals(mallOrderInfo.getOrderType())) {

            // 退运费
            mallOrderInfoService.backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, true);
            //4 更新审核单
            mallOrderInfoService.cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
            //5 生成入云单
            MallOrderInfo orderInfo = mallOrderInfoService.createOrderInfoAndOrderItem(itemList, mallOrderInfo);
            // 入云
            itemList.forEach(p -> {
                commonOrderService.addCloudStock(mallOrderInfo.getMallUserId(), p, orderInfo.getOrderId());
                //生成入库详情
                mallCloudStockDetailService.backFillCloudStockDetail(user, p);
            });
        }

        // 提货 -> 退运费
        if (MallOrderTypeEnum.ORDER_TYPE_002.getCode().equals(mallOrderInfo.getOrderType())) {
            // 回退
            mallOrderInfoService.backCloud(mallOrderInfo,"9");
            // 退运费
            mallOrderInfoService.backToPay(mallOrderInfo.getPostFeeAmt(), mallOrderInfo, itemList, false);
            //4 更新审核单
            mallOrderInfoService.cancelVerifyOrderInfo(mallOrderInfo.getOrderId());
            //回退cloudStockDetail库存
            try {
                for (MallOrderItem item : itemList) {
                    if ("1".equals(item.getType())) {
                        //生成入库详情
                        mallCloudStockDetailService.backFillCloudStockDetail(user, item);
                    }
                }
            } catch (Exception e) {
                log.info("商务审核提货订单不通过时,保存cloudStockDetail相关异常:{}", e);
            }
        }
    }

    /**
     * 敏感产品审核列表
     *
     * @param adminReviewVerifyParam
     * @return
     */
    public PageDto<OrderVerifyDto> sensitiveOrderInfoList(@RequestBody AdminReviewVerifyParam adminReviewVerifyParam) {
        MallOrderVerify mallOrderVerify = new MallOrderVerify();
        Page page = MybatisPageUtil.getPage(adminReviewVerifyParam.getPageCurrent(), adminReviewVerifyParam.getPageSize());
        BeanMapper.copy(adminReviewVerifyParam, mallOrderVerify);
        List<String> idList = new ArrayList<>();
        List<MallUser> userList = null;
        if (!StringUtils.isEmpty(adminReviewVerifyParam.getNickName()) || !StringUtils.isEmpty(adminReviewVerifyParam.getName()) || !StringUtils.isEmpty(adminReviewVerifyParam.getPhone())) {
            userList = userFeign.getUserByInput(adminReviewVerifyParam.getNickName(), adminReviewVerifyParam.getName(), adminReviewVerifyParam.getPhone(), null, null);
            if (CollectionUtils.isEmpty(userList)) {
                return null;
            }
        }
        if (!CollectionUtils.isEmpty(userList)) {
            for (MallUser mallUser : userList) {
                idList.add(mallUser.getId());
            }
            mallOrderVerify.setMallUserIdList(idList);
        } else {
            mallOrderVerify.setMallUserIdList(null);
        }

        List<OrderVerifyDto> result = new ArrayList<>();
        //1.查询出表中有直接对应商务的数据
        List<OrderVerifyDto> orderListHasAdminCode = mallOrderVerifyMapper.querySensitiveGoodsVerifyList(mallOrderVerify);
        if (!CollectionUtils.isEmpty(orderListHasAdminCode)) {
            List<OrderVerifyDto> collect = orderListHasAdminCode.stream().filter(orderVerifyDto -> !StringUtils.isEmpty(orderVerifyDto.getAdminId())).collect(Collectors.toList());
            result.addAll(collect);
        }
        //2.筛选商务code为null的订单,上一级
        List<OrderVerifyDto> orderListFirstParentHasCode = mallOrderVerifyMapper.queryVerifyOrderListFirstParent(mallOrderVerify);
        if (!CollectionUtils.isEmpty(orderListFirstParentHasCode)) {
            List<OrderVerifyDto> collect = orderListFirstParentHasCode.stream().filter(orderVerifyDto -> !StringUtils.isEmpty(orderVerifyDto.getAdminId())).collect(Collectors.toList());
            result.addAll(collect);
            //3.上二级
            //提出adminCode为空的数据
            List<String> userIdList = orderListFirstParentHasCode.stream().filter(orderVerifyDto -> StringUtils.isEmpty(orderVerifyDto.getAdminId())).map(OrderVerifyDto::getProposerId).collect(Collectors.toList());
            List<OrderVerifyDto> list3 = orderListFirstParentHasCode.stream().filter(orderVerifyDto -> StringUtils.isEmpty(orderVerifyDto.getAdminId())).collect(Collectors.toList());
            //根据userId查询上上级代理对应的adminCode
            if (!CollectionUtils.isEmpty(userIdList)) {
                userIdList.stream().forEach(str -> {
                    String adminCode = mallOrderVerifyMapper.querySecondParentAdminCode(str);
                    list3.stream().forEach(orderVerifyDto -> {
                        if (orderVerifyDto.getProposerId().equals(str)) {
                            orderVerifyDto.setAdminId(adminCode);
                        }
                    });
                });
            }
            result.addAll(list3);
        }

        if (!StringUtils.isEmpty(mallOrderVerify.getAdminId())) {
            if (!CollectionUtils.isEmpty(result)) {
                result = result.stream().filter(orderVerifyDto -> adminReviewVerifyParam.getAdminId().equals(orderVerifyDto.getAdminId())).collect(Collectors.toList());
            }
        }

        PageDto pageDto = new PageDto();
        if (!CollectionUtils.isEmpty(result)) {
            result = result.stream().sorted(Comparator.comparing(OrderVerifyDto::getCreateDate).reversed()).collect(Collectors.toList());
            pageDto = MybatisPageUtil.pageHelper(result, page, pageDto);
            page.setTotal(result.size());
        }
        result = pageDto.getRecords();


        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(p -> {
                List<OrderItemDetailDto> dto = mallOrderInfoService.orderItemByOrderId(p.getOrderId(), 0);
                p.setNextProxyRoleName(MallTeamEnum.explain(p.getNextProxyName()));
                p.setOrderItemDetailDtos(dto);
//                MallUser mallUser = UserUtils.getUserInfoByCacheOrId(p.getProposerId());
                MallUser mallUser = userFeign.getUserDetailInfoById(p.getProposerId());
                p.setNextProxyIcon(mallUser.getIcon());
                p.setNextProxyName(mallUser.getName());
                p.setNextProxyPhone(mallUser.getPhone());
                p.setNextProxyRoleName(MallTeamEnum.explain(mallUser.getRoleId()));
                BigDecimal totalAmt = dto.stream().map(OrderItemDetailDto::getAmount).filter(q -> ObjectUtils.isNotNullAndEmpty(q)).reduce(BigDecimal.ZERO, BigDecimal::add);
                p.setTotalAmt(totalAmt);
                List<MallOrderInfo> mallOrderInfos = mallOrderInfoMapper.selectList(new EntityWrapper<MallOrderInfo>().eq("is_del", "0").eq("order_id", p.getOrderId()));
                if (!CollectionUtils.isEmpty(mallOrderInfos)) {
                    p.setMallOrderInfo(mallOrderInfos.get(0));
                    p.setOrderAmt(mallOrderInfos.get(0).getSummaryAmt());
                    p.setCompanyName(getCompanyName(mallOrderInfos.get(0).getCompanyId(), mallOrderInfos.get(0).getMallUserId()));
                }

                p.setMallUser(mallUser);

//                MallOrderInfo orderInfo = mallOrderInfoService.selectById(p.getOrderId());
                //查询商务code
                //p.setAdminId(mallUser.getAdminCode());
            });

        }
        // PageDto pageDto = new PageDto();
        pageDto.setRecords(result);
        pageDto.setTotal(page.getTotal());
        return pageDto;

    }

    public List<OrderItemDetailDto> queryOrderItemDetail(String orderId) {
        List<OrderItemDetailDto> result = mallOrderInfoService.orderItemByOrderId(orderId, 1);
        return result;
    }

    //查询公司信息
    private String getCompanyName(String companyId, String userId) {
        String companyName = "";
        if (ObjectUtils.isNotNullAndEmpty(companyId)) {
            String company = RedisUtil.get("companyInfo:id_" + companyId);
            if (company != null) {
                MallBranchOffice office = JSONUtil.json2pojo(company, MallBranchOffice.class);
                return office.getCompanyName();
            }
        }
        if (ObjectUtils.isNullOrEmpty(companyId)) {
            companyName = agentFeign.getCompanyNameByUserId(userId);
        }
        return companyName;
    }


    /**
     * 减少上级库存
     *
     * @param userId
     * @param mallOrderItem
     */
    private void subLeaderCloudStock(String userId, MallOrderItem mallOrderItem, String orderId) {
        //查询云库存
        MallCloudStock stock = new MallCloudStock();
        stock.setMallUserId(userId);
        stock.setItemId(mallOrderItem.getItemId());
        stock.setSkuCode(mallOrderItem.getSkuCode());
        MallCloudStock cloudStock = agentFeign.getCloudStock(stock);
        //查询sku
        MallSku sku = itemFeign.getSkuByCode(mallOrderItem.getSkuCode());

        //判断库存是否存在
        if (ObjectUtils.isNullOrEmpty(cloudStock)) {
            throw new MallException(OrderRespCode.STOCK_NOT_FOUND, new Object[]{sku.getTitle()});
        }
        //判断库存是否充足
        if (cloudStock.getStock().compareTo(mallOrderItem.getAmount()) < 0) {
            throw new MallException(OrderRespCode.LACK_OF_STOCK, new Object[]{sku.getTitle()});
        }

        //更新库存
        MallCloudStock cloudParam = new MallCloudStock();
        cloudParam.setId(cloudStock.getId());
        cloudParam.setStock(mallOrderItem.getAmount().abs().negate());
        agentFeign.updateCloudStockByLock(cloudParam);

        //添加云库存记录 库存单号 用户信息 商品 交易前数量	交易数量	交易后数量 日志时间
        MallCloudStockLog mallCloudStockLog = new MallCloudStockLog();
        mallCloudStockLog.setId(IDUtils.genId());
        mallCloudStockLog.setCloudStockId(cloudStock.getId());
        mallCloudStockLog.setSkuCode(mallOrderItem.getSkuCode());
        mallCloudStockLog.setItemId(mallOrderItem.getItemId());
        mallCloudStockLog.setPayStock(mallOrderItem.getAmount().abs().negate());
        mallCloudStockLog.setRelationId(orderId);
        mallCloudStockLog.setMallUserId(userId);
        mallCloudStockLog.setPayAgoStock(cloudStock.getStock());
        mallCloudStockLog.setPayAfterStock(cloudStock.getStock().subtract(mallOrderItem.getAmount()).setScale(2, BigDecimal.ROUND_HALF_UP));
        mallCloudStockLog.setCreateDate(new Date());
        agentFeign.addCloudStockLog(mallCloudStockLog);
    }

    /**
     * 后台订单审核列表
     *
     * @param param
     * @return
     */
    public PageDto<OrderVerifyPageDto> queryOrderVerifyPageList(GetOrderVerifyPageListParam param) {
        MallOrderVerify mallOrderVerify = new MallOrderVerify();
        BeanMapper.copy(param, mallOrderVerify);
        mallOrderVerify.setPageCurrent(param.getPageCurrent());
        mallOrderVerify.setPageSize(param.getPageSize());
        return queryOrderVerifyPageList(mallOrderVerify);
    }

    /**
     * 后台获取审核单列表
     *
     * @return
     */
    public PageDto<OrderVerifyPageDto> queryOrderVerifyPageList(MallOrderVerify mallOrderVerify) {
        List<OrderVerifyPageDto> result = new ArrayList<>();
        PageDto pageResult = new PageDto();
        Page page = MybatisPageUtil.getPage(mallOrderVerify.getPageCurrent(), mallOrderVerify.getPageSize());
        if (StringUtils.isEmpty(mallOrderVerify.getPhone()) && StringUtils.isEmpty(mallOrderVerify.getName())) {
            mallOrderVerify.setMallUserIdList(null);
        } else {
            List<String> idList = new ArrayList<>();
            List<MallUser> userList = null;
            if (MallPreconditions.checkNullBoolean(Arrays.asList(mallOrderVerify.getName(), mallOrderVerify.getPhone()))) {
                userList = userFeign.getUserByInput(null, mallOrderVerify.getName(), mallOrderVerify.getPhone(), null, null);
            }
            if (!CollectionUtils.isEmpty(userList)) {
                for (MallUser mallUser : userList) {
                    idList.add(mallUser.getId());
                }
                mallOrderVerify.setMallUserIdList(idList);
            }
        }
        List<MallOrderVerify> list = mallOrderVerifyMapper.queryMallOrderVerifyPageList(mallOrderVerify, page);
        if (CollectionUtils.isEmpty(list)) {
            return pageResult;
        }
        for (MallOrderVerify record : list) {
            OrderVerifyPageDto dto = new OrderVerifyPageDto();
            BeanMapper.copy(record, dto);
            MallUser proposerUser = UserUtils.getUserInfoByCacheOrId(record.getProposerId());
            dto.setProposerUser(proposerUser);
            MallUser accepterUser = UserUtils.getUserInfoByCacheOrId(record.getAccepterId());
            dto.setAccepterUser(accepterUser);
            List<OrderItemDetailDto> itemlist = queryOrderItemDetail(record.getOrderId());
            dto.setSkulist(itemlist);
            MallTransferGoods goods = new MallTransferGoods();
            goods.setRelationId(record.getOrderId());
            goods.setPageSize(100);
            goods.setPageCurrent(0);
            PageDto<OrderTransferInfoPageDto> mallTransferGoods = mallOrderInfoService.queryTransferGoodsPageList(goods);
            dto.setMallTransferGoods(mallTransferGoods);
            result.add(dto);
        }
        pageResult.setTotal(page.getTotal());
        pageResult.setRecords(result);
        return pageResult;
    }


    public List<MallOrderVerify> getMallOrderVerifyByOrderId(String orderId) {
        List<MallOrderVerify> list = this.selectList(
                new EntityWrapper<MallOrderVerify>()
                        .eq("order_id", orderId)
                        .eq("is_del", 0)
                        .orderBy("create_date", false)

        );
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list;
    }

    public List<MallOrderVerify> getMallOrderVerifyByOrderIdNew(String orderId) {
        List<MallOrderVerify> list = this.selectList(
                new EntityWrapper<MallOrderVerify>()
                        .eq("order_id", orderId)
                        .eq("is_del", 0)
                        .in("verify_status", new String[]{"1", "2"})
                        .orderBy("create_date", false)

        );
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list;
    }

    @TxTransaction
    @Transactional
    public Boolean updateVerifyOrder(MallOrderVerify mallOrderVerify) {
        mallOrderVerify.setUpdateDate(new Date());
        Boolean result = this.updateById(mallOrderVerify);
        return result;
    }


    public Page<MallOrderVerify> getMallOrderVerifyByAccepterId(Page<MallOrderVerify> page, String accepterId, String verifyStatus) {
        Wrapper<MallOrderVerify> eq = new EntityWrapper<MallOrderVerify>()
                .eq("accepter_id", accepterId)
                .eq("verify_type", 0)
                .gt("create_date", "2019-01-01") //todo 隐藏19年之前的数据
                .eq("is_del", 0);

        if (StringUtils.isEmpty(verifyStatus) || "4".equals(verifyStatus)) {
            eq = eq.andNew()
                    .in("verify_status", Arrays.asList("0", "1", "2", "3"))
                    .orderBy("create_date", false);
        } else {
            eq = eq.andNew()
                    .eq("verify_status", verifyStatus)
                    .orderBy("create_date", false);
        }

        Page<MallOrderVerify> list = this.selectPage(page, eq);
        return list;
    }

    public List<MallOrderVerify> getMallOrderVerifyLikeProposerId(String accepterId, String verifyStatus, List<String> proposerIds) {

        Wrapper<MallOrderVerify> eq = new EntityWrapper<MallOrderVerify>()
                .eq("accepter_id", accepterId)
                .eq("verify_type", 0)
                .eq("is_del", 0)
                .in("proposer_id", proposerIds);

        if (StringUtils.isEmpty(verifyStatus) || "4".equals(verifyStatus)) {
            eq = eq.andNew()
                    .eq("verify_status", "0")
                    .or()
                    .eq("verify_status", "1")
                    .or()
                    .eq("verify_status", "2")
                    .or()
                    .eq("verify_status", "3")
                    .orderBy("create_date");
        } else {
            eq = eq.andNew()
                    .eq("verify_status", verifyStatus)
                    .orderBy("create_date");
        }

        List<MallOrderVerify> list = this.selectList(eq);
        return list;
    }


    /**
     * 查询下级申请审核单列表
     *
     * @param getOrderVerifyParam
     * @return
     */
    public Page<OrderVerifyDto> queryMallOrderVerifyByAccepterId(GetOrderVeifyParam getOrderVerifyParam) {
        MallUser user = UserUtils.getCurrentUser();
        Page page = MybatisPageUtil.getPage(getOrderVerifyParam.getPageCurrent(), getOrderVerifyParam.getPageSize());
        Page<MallOrderVerify> result = getMallOrderVerifyByAccepterId(page, user.getId(), getOrderVerifyParam.getVerifyStatus());
        return getOrderVerifyDto(result);
    }

    /**
     * 根据下级手机号搜索审核单
     *
     * @param getOrderVerifyParam
     * @return
     */
    public Page<OrderVerifyDto> queryMallOrderVerifyLikeProposerId(GetOrderVeifyByPhoneParam getOrderVerifyParam) {
        MallUser user = UserUtils.getCurrentUser();
        MallAgent agent = agentFeign.getAgentByUserId(user.getId());
        GetUserByPhoneParam getUserByPhoneParam = new GetUserByPhoneParam();
        BeanMapper.copy(getOrderVerifyParam, getUserByPhoneParam);
        //查询所有下级代理
        List<MallAgent> mallAgents = agentFeign.queryAllNextAgentInfo(agent.getId());
        if (ObjectUtils.isNullOrEmpty(mallAgents)) {
            throw new MallException(OrderRespCode.IS_NOT_HAVE_DATA);
        }
        //抽出userId
        List<String> list = mallAgents.stream().map(p -> {
            return p.getUserId();
        }).collect(Collectors.toList());

        //查询所有下级代理用户信息
        List<MallUser> nextUser = userFeign.queryUserByListId(list);

        if (ObjectUtils.isNullOrEmpty(nextUser)) {
            throw new MallException(OrderRespCode.IS_NOT_HAVE_DATA);
        }

        List<String> proposerIds = new ArrayList<>();

        nextUser.forEach(p -> {
            if (p.getPhone().contains(getOrderVerifyParam.getPhone())) {
                proposerIds.add(p.getId());
            }
        });
        if (proposerIds.size() == 0) {
            throw new MallException(OrderRespCode.IS_NOT_HAVE_DATA);
        }

        List<MallOrderVerify> result = getMallOrderVerifyLikeProposerId(user.getId(), getOrderVerifyParam.getVerifyStatus(), proposerIds);

        Page<MallOrderVerify> page = new Page<>();
        page.setRecords(result);
        return getOrderVerifyDto(page);
    }

    private Page<OrderVerifyDto> getOrderVerifyDto(Page<MallOrderVerify> result) {
        Page<OrderVerifyDto> dtoPage = new Page<>();
        List<OrderVerifyDto> list = new ArrayList<>();
        if (ObjectUtils.isNotNullAndEmpty(result.getRecords())) {
            result.getRecords().forEach(p -> {
                MallUser nextUser = UserUtils.getUserInfoByCacheOrId(p.getProposerId());
                OrderVerifyDto orderVerifyDto = new OrderVerifyDto();
                List<OrderItemDetailDto> dto = queryOrderItemDetail(p.getOrderId());
                BeanMapper.copy(p, orderVerifyDto);
                orderVerifyDto.setVerifyStatusName(MallOrderVerifyStatusEnum.explain(p.getVerifyStatus()));
                MallOrderInfo orderInfo = mallOrderInfoService.selectById(p.getOrderId());
                orderVerifyDto.setProofPath(orderInfo.getProofPath());
                orderVerifyDto.setLastVerifyTime(orderInfo.getVerifyEndDate());
                List<MallOrderItem> itemList = mallOrderItemService.selectByOrderId(p.getOrderId());
                BigDecimal amt = new BigDecimal(0);
                for (MallOrderItem i : itemList) {
                    amt = amt.add(i.getAmount().multiply(i.getPrice()));
                }
                orderVerifyDto.setTotalAmt(amt);
                orderVerifyDto.setOrderAmt(amt);
                orderVerifyDto.setNextProxyName(nextUser.getName());
                orderVerifyDto.setNextProxyPhone(nextUser.getPhone());
                orderVerifyDto.setNextProxyRoleName(MallTeamEnum.explain(nextUser.getRoleId()));
                orderVerifyDto.setNextProxyIcon(nextUser.getIcon());
                orderVerifyDto.setOrderItemDetailDtos(dto);
                //EnCodeUserInfo.encodeMallUserinfo(orderVerifyDto);
                list.add(orderVerifyDto);
            });
        }
        dtoPage.setRecords(list);
        return dtoPage;
    }


    /**
     * 下级申请购入产品订单审核
     *
     * @param orderVerifyParam
     */
    @TxTransaction(isStart = true)
    @Transactional
    public boolean orderVerify(OrderVerifyParam orderVerifyParam) {

        MallUser user = UserUtils.getCurrentUser();

        // 查询该条审核单信息
        MallOrderVerify result = this.selectById(orderVerifyParam.getVerifyId());
        if (!user.getId().equals(result.getAccepterId())) {
            throw new MallException(OrderRespCode.CAN_NOT_VERIFY_ORDER);
        }
        // 查询订单信息
        MallOrderInfo orderInfo = mallOrderInfoService.selectById(result.getOrderId());

        if (MallOrderStatusEnum.ORDER_STATUS_006.getCode().equals(orderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.IS_CALCEL);
        }
        if (!MallOrderStatusEnum.ORDER_STATUS_001.getCode().equals(orderInfo.getOrderStatus())) {
            throw new MallException(OrderRespCode.ORDER_AREADY_VERIFY);
        }
        // 查询订单商品信息
        List<MallOrderItem> itemlist = mallOrderItemService.selectByOrderId(orderInfo.getOrderId());

        if (!MallOrderVerifyEnum.VERIFY_STATUS_000.getCode().equals(result.getVerifyStatus())) {
            throw new MallException(OrderRespCode.ORDER_AREADY_VERIFY);
        }
        if (!orderInfo.getLeaderId().equals(user.getId())) {
            throw new MallException(OrderRespCode.CAN_NOT_VERIFY_ORDER);
        }

        MallOrderVerify verify = new MallOrderVerify();
        verify.setId(result.getId());
        verify.setAccepterId(result.getAccepterId());
        verify.setVerifyMemo(orderVerifyParam.getVerifyMemo());
        verify.setUpdateDate(new Date());

        BigDecimal allAmount = BigDecimal.ZERO;
        for (MallOrderItem p : itemlist) {
            MallSku sku = itemFeign.getSkuByCode(p.getSkuCode());
            if (MallReviewEnum.IS_SHAREPROFIT_000.getCode().equals(sku.getIsShareProfit())) {
                allAmount = allAmount.add(p.getAmount());
            }
        }

        //订单类型
        String orderType = orderInfo.getOrderType();

        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(orderInfo.getOrderId());
        mallOrderInfo.setIsCanCancel("1");

        //审核通过
        if (MallOrderVerifyEnum.VERIFY_STATUS_001.getCode().equals(orderVerifyParam.getVerifyStatus())) {

            mallOrderInfo.setCompleteDate(new Date());
            mallOrderInfo.setOrderStatus(MallStatusEnum.ORDER_STATUS_005.getCode());
            mallOrderInfo.setSystemMemo("上级审核通过");
            //1 入云库存
            if (MallStatusEnum.ORDER_TYPE_001.getCode().equals(orderType)) {
                // 添加云库存 有该商品则累加数量 没有则创建新商品
                itemlist.forEach(p -> {
                    commonOrderService.addCloudStock(orderInfo.getMallUserId(), p, p.getOrderId());
                });

                // 生成转货订单
                String orderId = IDUtils.genOrderId();
                MallOrderInfo transferOrderInfo = new MallOrderInfo();
                transferOrderInfo.setOrderId(orderId);
                transferOrderInfo.setMallUserId(result.getAccepterId());
                transferOrderInfo.setBelongsCode("0");
                transferOrderInfo.setOrderDescribe("审核通过，自动转云库存到下级");
                transferOrderInfo.setPayDate(new Date());
                transferOrderInfo.setCompleteDate(new Date());
                transferOrderInfo.setOrderStatus(MallStatusEnum.ORDER_STATUS_005.getCode());
                transferOrderInfo.setOrderType(MallStatusEnum.ORDER_TYPE_004.getCode());
                Date now = new Date();
                Date afterDate = new Date(now.getTime() + 30 * 60 * 1000);
                transferOrderInfo.setCreateDate(now);
                transferOrderInfo.setPayEndDate(afterDate);//支付截止时间30分钟后
                mallOrderInfoService.insertOrderInfo(transferOrderInfo);

                //减少上级云库存
                itemlist.forEach(p -> {
                    subLeaderCloudStock(orderInfo.getLeaderId(), p, orderId);
                    //添加转货商品记录
                    MallTransferItem mallTransferGoods = new MallTransferItem();
                    mallTransferGoods.setId(IDUtils.genId());
                    mallTransferGoods.setTitle("审核通过，自动转云库存到下级");
                    mallTransferGoods.setMallUserId(result.getAccepterId());
                    mallTransferGoods.setNextProxyId(result.getProposerId());
                    mallTransferGoods.setItemId(p.getItemId());
                    mallTransferGoods.setSkuCode(p.getSkuCode());
                    mallTransferGoods.setMemo(verify.getVerifyMemo());
                    mallTransferGoods.setAmount(p.getAmount().multiply(BigDecimal.valueOf(-1)));
                    mallTransferGoods.setCreateDate(new Date());
                    mallTransferGoods.setType("1");
                    mallTransferGoods.setRelationId(orderId);
                    transferItemService.insert(mallTransferGoods);


                });
                //加当日进货量
                if (allAmount.compareTo(BigDecimal.ZERO) > 0) {
                    setTodayInItem(orderInfo.getMallUserId(), itemlist);
                }


                //cloudStockDetail相关
                //想当于转货操作
                MallUser userById = userFeign.getUserById(orderInfo.getMallUserId());
                TransferCloudDTO transferCloudDTO = new TransferCloudDTO();
                transferCloudDTO.setItemList(itemlist);
                transferCloudDTO.setAgentLevel(null == userById ? null : userById.getRoleId());
                transferCloudDTO.setMallUserId(orderInfo.getLeaderId());
                transferCloudDTO.setNextUserId(orderInfo.getMallUserId());
                transferCloudDTO.setNextOrderId(orderId);
                agentFeign.transferCloud(transferCloudDTO);
            }
            // 更新审核单
            verify.setVerifyStatus(MallOrderVerifyEnum.VERIFY_STATUS_001.getCode());
            this.updateVerifyOrder(verify);
        }

        //审核不通过
        if (MallOrderVerifyEnum.VERIFY_STATUS_002.getCode().equals(orderVerifyParam.getVerifyStatus())) {
            mallOrderInfo.setOrderStatus(MallStatusEnum.ORDER_STATUS_002.getCode());
            // 更新审核单
            verify.setVerifyStatus(MallOrderVerifyStatusEnum.VERIFY_STATUS_002.getCode());
            this.updateVerifyOrder(verify);
        }
        // 更新订单表
        mallOrderInfo.setUpdateDate(new Date());
        mallOrderInfoService.updateOrderById(mallOrderInfo);
        // 推送
        sendNotify(orderInfo, result.getProposerId(), orderVerifyParam.getVerifyStatus(), verify.getId());
        return true;
    }

    //当日进货量
    public void setTodayInItem(String userId, List<MallOrderItem> itemList) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date zero = calendar.getTime();
        long v = zero.getTime() - new Date().getTime();
        RedisUtil.lSet("oneDaysToBuyItemToCloud:" + userId, JSONUtil.obj2json(itemList), v / 1000);
    }


    /**
     * 重新发起审核
     *
     * @param orderId
     */
    @TxTransaction(isStart = true)
    @Transactional
    public boolean reToVerify(String orderId, String proofPath) {
        //获取订单信息
        MallOrderInfo mallOrderInfo = mallOrderInfoService.selectById(orderId);
        //只有审核拒绝才可以重新发起审核
        if (MallOrderStatusEnum.ORDER_STATUS_002.getCode().equals(mallOrderInfo.getOrderStatus())) {
            if (new Date().getTime() > mallOrderInfo.getVerifyEndDate().getTime()) {
                throw new MallException(OrderRespCode.NOT_TO_VERIFY);
            }
            List<MallOrderVerify> list = getMallOrderVerifyByOrderId(orderId);
            MallOrderVerify mallOrderVerify = new MallOrderVerify();
            BeanMapper.copy(list.get(0), mallOrderVerify);
            mallOrderVerify.setAdminId(null);
            mallOrderVerify.setCreateDate(new Date());
            mallOrderVerify.setUpdateDate(new Date());
            mallOrderVerify.setVerifyStatus("0");

            //更新订单表为审核中
            mallOrderInfo.setUpdateDate(new Date());
            if (MallOrderStatusEnum.ORDER_STATUS_002.getCode().equals(mallOrderInfo.getOrderStatus())) {
                mallOrderInfo.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_001.getCode());
                mallOrderVerify.setVerifyType(MallOrderVerifyEnum.VERIFY_TYPE_000.getCode());
            }
            insertOrderVerify(mallOrderVerify);
            mallOrderInfo.setProofPath(proofPath);
            mallOrderInfoService.updateOrderById(mallOrderInfo);
        } else {
            throw new MallException(OrderRespCode.NOT_TO_VERIFY);
        }

        return true;
    }


    public void sendNotify(MallOrderInfo orderInfo, String mallUserId, String verifyStatus, String verifyId) {
        MallPropelNews mallPropelNews = new MallPropelNews();
        mallPropelNews.setMallUserId(mallUserId);
        mallPropelNews.setTitle("审核结果");
        mallPropelNews.setSubTitle("您的订单" + MallOrderVerifyStatusEnum.explain(verifyStatus));
        mallPropelNews.setAmt(orderInfo.getPaymentAmt());
        mallPropelNews.setRelationId(verifyId);
        mallPropelNews.setType("2");//审核通知
        mallPropelNews.setRelationType("0");
        mallPropelNews.setStatus("0");
        mallPropelNews.setNewsType("1");
        notifyFeign.createNewPropelNews(mallPropelNews);
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
                    if (ObjectUtils.isNullOrEmpty(orderId)) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("第" + i + "行数据订单编号为空", "订单号不能为空");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId("第" + i + "行数据订单编号为空");
                        dto.setReason("订单号不能为空");
                        dtos.add(dto);
                        continue;
                    }

                    String expressCode = ExcelUtil.getCellStringValue(row.getCell(1));
                    if (ObjectUtils.isNullOrEmpty(expressCode)) {
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
                    MallOrderInfo info = mallOrderInfoMapper.selectById(orderId);
                    if (ObjectUtils.isNullOrEmpty(info)) {
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
                    if (MallOrderStatusEnum.ORDER_STATUS_004.getCode().equals(info.getOrderStatus()) && ObjectUtils.isNotNullAndEmpty(info.getExpressCode())) {
                        continue;
                    }
                    if (MallOrderStatusEnum.ORDER_STATUS_009.getCode().equals(info.getOrderStatus())) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "该条订单审核未通过，不可发货");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("该条订单审核未通过，不可发货");
                        dtos.add(dto);
                        continue;
                    }

                    MallOrderVerify mallOrderVerify = mallOrderVerifyMapper.selectOne(MallOrderVerify.builder().orderId(orderId).build());
                    //校验审核状态
                    if (ObjectUtils.isNullOrEmpty(mallOrderVerify)) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put(orderId, "未找到相应的审核单");
                        list.add(map);
                        ItemOrderResultDTO dto = new ItemOrderResultDTO();
                        dto.setOrderId(orderId);
                        dto.setReason("未找到相应的审核单");
                        dtos.add(dto);
                        continue;
                    }
                    mallOrderVerify.setVerifyStatus(MallOrderVerifyStatusEnum.VERIFY_STATUS_001.getCode());
                    mallOrderVerify.setUpdateDate(new Date());
                    mallOrderVerifyMapper.updateById(mallOrderVerify);
                    info.setExpressCompany(expressCompany);
                    info.setExpressCode(expressCode);
                    info.setOrderStatus(MallOrderStatusEnum.ORDER_STATUS_004.getCode());
                    info.setDeliverGoodsDate(new Date());
                    info.setUpdateDate(new Date());
                    info.setIsCanCancel("1");
                    mallOrderInfoMapper.updateById(info);
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
        RedisUtil.set(MallOrderVerifyService.key + key, JSONUtil.obj2json(dtos), 60 * 60);
        log.info("dtos:{}", dtos);
        //处理结果集
//        ExcelUtil excelUtil = new ExcelUtil();
//        LinkedHashMap<String,String> map = new LinkedHashMap();
//        map.put("订单编号","orderId");
//        map.put("错误原因","reason");
//        String ret = excelUtil.buildExcel(map, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return key;
    }

    public String getImport(String key, HttpServletResponse response) throws Exception {
        String s1 = RedisUtil.get(MallOrderVerifyService.key + key);
        List<ItemOrderResultDTO> dtos = JSONUtil.json2list(s1, ItemOrderResultDTO.class);
        //处理结果集
        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = new LinkedHashMap();
        map.put("订单编号", "orderId");
        map.put("错误原因", "reason");
        log.info("dtos:{}", dtos);
        String s = excelUtil.buildExcel(map, dtos, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;
    }


}
