package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.google.common.base.Joiner;
import com.jd.open.api.sdk.response.ECLP.EclpOrderAddOrderResponse;
import com.meifute.core.entity.*;
import com.meifute.core.entity.orderfeedback.MallChangeFeedback;
import com.meifute.core.entity.orderfeedback.MallFeedbackGoods;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallFeedBackQuestionMapper;
import com.meifute.core.mapper.MallOrderFeedBackMapper;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.date.DateUtil;
import com.meifute.core.mmall.common.enums.MallTeamEnum;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.utils.ExcelUtil;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.StringUtils;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.vo.CloudRegulateParam;
import com.meifute.core.vo.TransferGoodsItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: wuxb
 * @Date: 2019-05-24 12:28
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
@Service
public class OrderFeedBackService extends ServiceImpl<MallOrderFeedBackMapper, MallOrderFeedBack> {

    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private UserFeign userFeign;
    @Autowired
    private MallOrderInfoMapper orderInfoMapper;
    @Autowired
    private MallFeedBackQuestionMapper feedBackQuestionMapper;
    @Autowired
    MallFeedbackGoodsService mallFeedbackGoodsService;
    @Autowired
    MallOrderFeedBackMapper mallOrderFeedBackMapper;
    @Autowired
    ItemFeign itemFeign;
    @Autowired
    JDExpressPushService jdExpressPush;
    @Autowired
    JDCheckPushAddressService jdCheckPushAddressService;
    @Autowired
    OrderDelayService orderDelayService;
    @Autowired
    MallPushItemRuleService mallPushItemRuleService;

    @Transactional
    public void insertFeedBack(MallOrderFeedBack mallOrderFeedBack) {
        MallOrderInfo info = orderInfoService.selectById(mallOrderFeedBack.getOrderId());
        MallAgent agent = agentFeign.getLeaderAgentByUserId(info.getMallUserId());
        mallOrderFeedBack.setId(IDUtils.genId());
        mallOrderFeedBack.setFeedbackPerson(info.getMallUserId());
        mallOrderFeedBack.setFeedbackPersonLeader(agent.getUserId());
        mallOrderFeedBack.setCreateDate(null == mallOrderFeedBack.getCreateDate() ? new Date() : mallOrderFeedBack.getCreateDate());
        this.insert(mallOrderFeedBack);
        mallFeedbackGoodsService.saveGoods(mallOrderFeedBack.getGoodsList(), mallOrderFeedBack.getId());
    }

    @Transactional
    public void addFeedBack(MallOrderFeedBack mallOrderFeedBack) {
        MallUser user = userFeign.getUserByPhone(mallOrderFeedBack.getPhone());
        MallAgent agent = agentFeign.getLeaderAgentByUserId(user.getId());
        mallOrderFeedBack.setId(IDUtils.genId());
        mallOrderFeedBack.setFeedbackPerson(user.getId());
        mallOrderFeedBack.setFeedbackPersonLeader(agent.getUserId());
        mallOrderFeedBack.setCreateDate(mallOrderFeedBack.getCreateDate());
        if (mallOrderFeedBack.getCreateDate() == null) {
            mallOrderFeedBack.setCreateDate(new Date());
        }
        this.insert(mallOrderFeedBack);
        mallFeedbackGoodsService.saveGoods(mallOrderFeedBack.getGoodsList(), mallOrderFeedBack.getId());
    }

    public Page<MallOrderFeedBack> getFeedBackInfo(MallOrderFeedBack mallOrderFeedBack) {
        Page<MallOrderFeedBack> page = MybatisPageUtil.getPage(mallOrderFeedBack.getPageCurrent(), mallOrderFeedBack.getPageSize());
        if (ObjectUtils.isNotNullAndEmpty(mallOrderFeedBack.getEndTime())) {
            mallOrderFeedBack.setEndTime(DateUtil.getNextDay(mallOrderFeedBack.getEndTime(), 1));
        }
        Wrapper<MallOrderFeedBack> wrapper = new EntityWrapper<MallOrderFeedBack>()
                .eq(!StringUtils.isEmpty(mallOrderFeedBack.getFeedBackType()), "feedback_type", mallOrderFeedBack.getFeedBackType())
                .eq(!StringUtils.isEmpty(mallOrderFeedBack.getOrderId()), "order_id", mallOrderFeedBack.getOrderId())
                .eq(!StringUtils.isEmpty(mallOrderFeedBack.getStatus()), "status", mallOrderFeedBack.getStatus())
                .eq(!StringUtils.isEmpty(mallOrderFeedBack.getExpressCode()), "express_code", mallOrderFeedBack.getExpressCode())
                .ge(ObjectUtils.isNotNullAndEmpty(mallOrderFeedBack.getBeginTime()), "create_date", mallOrderFeedBack.getBeginTime())
                .le(ObjectUtils.isNotNullAndEmpty(mallOrderFeedBack.getEndTime()), "create_date", mallOrderFeedBack.getEndTime())
                .eq("is_del", "0")
                .orderBy("create_date", false);
        if (!StringUtils.isEmpty(mallOrderFeedBack.getPhone()) || !StringUtils.isEmpty(mallOrderFeedBack.getName())) {
            List<MallUser> input = userFeign.getUserByInput(null, mallOrderFeedBack.getName(), mallOrderFeedBack.getPhone(), null, null);
            log.info("input:{}", input);
            wrapper.in(!CollectionUtils.isEmpty(input), "feedback_person", input.stream().map(MallUser::getId).collect(Collectors.toList()));
        }
        if (ObjectUtils.isNotNullAndEmpty(mallOrderFeedBack.getNewQuestionTypeUnion())) {
            List<MallFeedBackQuestion> feedBackQuestions = feedBackQuestionMapper.selectList(new EntityWrapper<MallFeedBackQuestion>()
                    .eq("new_question_type", mallOrderFeedBack.getNewQuestionTypeUnion()));
            List<String> collect = feedBackQuestions.stream().map(MallFeedBackQuestion::getFeedBackId).collect(Collectors.toList());
            collect.add("0");
            wrapper.in("id", collect);
        }

        Page<MallOrderFeedBack> selectPage = this.selectPage(page, wrapper);
        for (MallOrderFeedBack p : selectPage.getRecords()) {
            MallUser myUser = userFeign.getUserById(p.getFeedbackPerson());
            p.setName(ObjectUtils.isNullOrEmpty(myUser) ? "" : myUser.getName());
            p.setPhone(ObjectUtils.isNullOrEmpty(myUser) ? "" : myUser.getPhone());
            p.setLevel(MallTeamEnum.explain(ObjectUtils.isNullOrEmpty(myUser) ? "0" : myUser.getRoleId()));
            if (ObjectUtils.isNotNullAndEmpty(p.getFeedbackPersonLeader())) {
                MallUser leaderUser = userFeign.getUserById(p.getFeedbackPersonLeader());
                p.setLeaderName(ObjectUtils.isNullOrEmpty(leaderUser) ? "" : leaderUser.getName());
                p.setLeaderPhone(ObjectUtils.isNullOrEmpty(leaderUser) ? "" : leaderUser.getPhone());
                p.setLeaderLevel(MallTeamEnum.explain(ObjectUtils.isNullOrEmpty(leaderUser) ? "0" : leaderUser.getRoleId()));
            }
            //反馈人专属商务
            log.info("---------------------准备获取总代对应商务:{}", p.getFeedbackPerson());
            MallAgent agent = agentFeign.getAgentByUserId(p.getFeedbackPerson());
            agent = getParent(agent, agent.getParentId());
            log.info("----------------------总代userId:{}", agent.getUserId());
            MallAdminAgent adminAgent = orderInfoMapper.getAdminAgentByUserId(agent.getUserId());
            p.setAdminAgent(adminAgent);
            log.info("----------------------总代userId:{},已设置adminAgent,其中p:{}", agent.getUserId(), p);
            //添加新问题类型
            List<MallFeedBackQuestion> feedBackQuestions = feedBackQuestionMapper.selectList(new EntityWrapper<MallFeedBackQuestion>()
                    .eq("feedback_id", p.getId()));
            log.info("----------------------总代userId:{},查出来的feedBackQuestions:{}", agent.getUserId(), feedBackQuestions);
            if (ObjectUtils.isNotNullAndEmpty(feedBackQuestions)) {
                p.setQuestionList(feedBackQuestions);
            }
        }
        return selectPage;
    }

    public String doExportFeedBackInfo(MallOrderFeedBack mallOrderFeedBack, HttpServletResponse response) throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<MallOrderFeedBack> mallOrderFeedBacks = mallOrderFeedBackMapper.doExportAboutFeedback(mallOrderFeedBack);

        if (!CollectionUtils.isEmpty(mallOrderFeedBacks)) {
            mallOrderFeedBacks.forEach(q -> {
                if ("0".equals(q.getStatus())) {
                    q.setStateStr("待处理");
                } else {
                    q.setStateStr("已处理");
                }
                q.setLevel(MallTeamEnum.explain(q.getLevel()));
                q.setLeaderLevel(MallTeamEnum.explain(q.getLeaderLevel()));

                q.setDateStr(format.format(q.getCreateDate()));
                switch (q.getAfterSaleType()) {
                    case "0":
                        q.setAfterSaleType("不处理库存");
                        break;
                    case "1":
                        q.setAfterSaleType("退回云库存");
                        break;
                    case "2":
                        q.setAfterSaleType("补发");
                        break;
                }
            });
        }


        ExcelUtil excelUtil = new ExcelUtil();
        LinkedHashMap<String, String> map = getMap();
        String s = excelUtil.buildExcel(map, mallOrderFeedBacks, ExcelUtil.DEFAULT_ROW_MAX, response);
        return s;
    }

    public LinkedHashMap<String, String> getMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("问题类型", "feedBackType");
        map.put("关联订单单号", "orderId");
        map.put("反馈人姓名", "name");
        map.put("反馈人手机号", "phone");
        map.put("反馈人代理等级", "level");
        map.put("反馈人上级姓名", "leaderName");
        map.put("反馈人上级手机号", "leaderPhone");
        map.put("反馈人上级代理等级", "leaderLevel");
        map.put("反馈人专属商务编号", "adminCode");
        map.put("状态", "stateStr");
        map.put("反馈时间", "dateStr");
        map.put("处理结果", "remark");
        map.put("产品售后方式", "afterSaleType");
        map.put("补发运单号", "expressCode");
        map.put("备注", "remark");
        map.put("描述", "feedbackDetail");
        map.put("产品名称", "productTitle");
        map.put("skuCode", "skuCode");
        map.put("售后数量", "afterSaleAmount");

        return map;
    }

    private MallAgent getParent(MallAgent agent, String parentAgentId) {
        if ("4".equals(agent.getAgentLevel())) {
            return agent;
        }
        agent = agentFeign.getAgentById(parentAgentId);
        agent = getParent(agent, agent.getParentId());
        if (agent != null && "4".equals(agent.getAgentLevel())) {
            return agent;
        }
        log.info("--------------代理等级:{}, 代理UserId:{}", agent.getAgentLevel(), agent.getUserId());
        return null;
    }

    @Transactional
    public void updateFeedBackInfo(MallOrderFeedBack mallOrderFeedBack) {
        mallOrderFeedBack.setRemark(mallOrderFeedBack.getRemark());
        mallOrderFeedBack.setUpdateDate(new Date());
        this.updateById(mallOrderFeedBack);
    }

    /**
     * 查看反馈详情
     *
     * @param id
     * @return
     */
    public MallOrderFeedBack queryDetail(String id) {
        MallOrderFeedBack mallOrderFeedBack = this.selectById(id);
        List<MallFeedbackGoods> goodsList = mallFeedbackGoodsService.selectList(new EntityWrapper<MallFeedbackGoods>().eq("feedback_id", id));
        if (!CollectionUtils.isEmpty(goodsList)) {
            goodsList.forEach(good -> {
                MallSku skuByCode = itemFeign.getSkuByCode(good.getSkuCode());
                good.setProductIcon(skuByCode.getSkuImg());
                good.setProductName(skuByCode.getTitle());
                good.setProductUnit(skuByCode.getUnit());
            });
        }
        mallOrderFeedBack.setGoodsList(goodsList);
        //订单信息
        if (StringUtils.isEmpty(mallOrderFeedBack.getAddress())) {
            MallOrderInfo mallOrderInfo = orderInfoService.selectById(mallOrderFeedBack.getOrderId());
            mallOrderFeedBack.setAddrPhone(mallOrderInfo.getAddrPhone());
            mallOrderFeedBack.setAddrName(mallOrderInfo.getAddrName());
            mallOrderFeedBack.setAddress(mallOrderInfo.getAddrId());
        }
        return mallOrderFeedBack;
    }

    public Page<MallOrderFeedBack> queryPage(MallOrderFeedBack mallOrderFeedBack) {
        Page<MallOrderFeedBack> page = MybatisPageUtil.getPage(mallOrderFeedBack.getPageCurrent(), mallOrderFeedBack.getPageSize());
        List<MallOrderFeedBack> mallOrderFeedBacks = mallOrderFeedBackMapper.selectPageByParam(mallOrderFeedBack);
        if (!CollectionUtils.isEmpty(mallOrderFeedBacks)) {
            fillAdminCode(mallOrderFeedBacks);
            pageResult(page, mallOrderFeedBack, mallOrderFeedBacks);
        }
        return page;
    }

    private void fillAdminCode(List<MallOrderFeedBack> mallOrderFeedBacks) {
        List<String> userIds = mallOrderFeedBacks.stream()
                .filter(vo -> StringUtils.isEmpty(vo.getAdminCode()))
                .map(vo -> vo.getFeedbackPerson())
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }
        List<MallAgent> agents = agentFeign.getAgentsByUserIds(userIds);
        if (CollectionUtils.isEmpty(agents)) {
            return;
        }
        for (MallOrderFeedBack back : mallOrderFeedBacks) {//关联商务
            for (MallAgent agent : agents) {
                if (StringUtils.equals(back.getFeedbackPerson(), agent.getUserId())) {
                    back.setAdminCode(agent.getAdminCode());
                    break;
                }
            }
        }

    }

    private List<MallOrderFeedBack> pageResult(Page<MallOrderFeedBack> page, MallOrderFeedBack mallOrderFeedBack, List<MallOrderFeedBack> mallOrderFeedBacks) {
        if (StringUtils.isNotEmpty(mallOrderFeedBack.getAdminCode())) {
            mallOrderFeedBacks = mallOrderFeedBacks.stream()
                    .filter(value -> mallOrderFeedBack.getAdminCode()
                            .equals(value.getAdminCode()))
                    .collect(Collectors.toList());
        }
        List<MallOrderFeedBack> result = mallOrderFeedBacks.stream()
                .skip(getStartNum(mallOrderFeedBack.getPageCurrent(), mallOrderFeedBack.getPageSize()))
                .limit(mallOrderFeedBack.getPageSize())
                .collect(Collectors.toList());
        page.setRecords(result);
        page.setTotal(mallOrderFeedBacks.size());
        return result;
    }

    private int getStartNum(Integer pageCurrent, Integer pageSize) {
        return pageCurrent * pageSize;
    }

    @Transactional
    public Boolean changeFeedBackInfo(MallChangeFeedback mallChangeFeedback) {
        MallOrderFeedBack mallOrderFeedBack = mallOrderFeedBackMapper.selectById(mallChangeFeedback.getId());
        if (null == mallOrderFeedBack)
            return false;

        if (mallOrderFeedBack.getStatus().equals("1")) {
            log.info("已处理过的订单问题反馈不允许重复处理");
            return false;
        }

        mallOrderFeedBack.setRemark(mallChangeFeedback.getRemark());
        mallOrderFeedBack.setStatus(mallChangeFeedback.getStatus());
        mallOrderFeedBack.setAfterSaleType(mallChangeFeedback.getType());
        mallOrderFeedBack.setUpdateDate(new Date());
        mallOrderFeedBackMapper.updateById(mallOrderFeedBack);

        List<MallFeedbackGoods> goodsList = mallChangeFeedback.getGoodsList();
        if (mallOrderFeedBack.getAfterSaleType().equals("1")) {
            //退回云库存
            backReturnToCloudStock(goodsList, mallOrderFeedBack.getFeedbackPerson(), mallChangeFeedback.getRemark());
        } else if (mallOrderFeedBack.getAfterSaleType().equals("2")) {
            //补发
            if (StringUtils.isEmpty(mallChangeFeedback.getAddress()) || StringUtils.isEmpty(mallChangeFeedback.getName()) || StringUtils.isEmpty(mallChangeFeedback.getPhone()))
                throw new MallException("020038", new Object[]{"收货信息不能为空!"});
            boolean b = jdCheckPushAddressService.checkPushAddress(mallChangeFeedback.getId(), mallChangeFeedback.getAddress());
            if (!b)
                throw new MallException("020038", new Object[]{"收货地址不可达!"});
            mallOrderFeedBack.setAddress(mallChangeFeedback.getAddress());
            mallOrderFeedBack.setAddrName(mallChangeFeedback.getName());
            mallOrderFeedBack.setAddrPhone(mallChangeFeedback.getPhone());
            Boolean result = JDPushAgain(mallOrderFeedBack, goodsList);
            if (!result) {
                throw new MallException("020038", new Object[]{"补发-京东退单失败"});
            }
        }

        if (CollectionUtils.isEmpty(goodsList))
            return true;

        goodsList.forEach(good -> {
            good.setUpdateDate(new Date());
        });
        mallFeedbackGoodsService.updateBatchById(goodsList);
        return true;
    }

    /**
     * 订单问题反馈处理类型：退回云库存
     */
    public void backReturnToCloudStock(List<MallFeedbackGoods> goodsList, String mallUserId, String memo) {
        //加库存
        List<TransferGoodsItem> transferGoodsItemList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(goodsList)) {
            goodsList.forEach(detail -> {
                TransferGoodsItem transferGoodsItem = new TransferGoodsItem();
                transferGoodsItem.setAmount(detail.getAfterSaleAmount());
                transferGoodsItem.setSkuCode(detail.getSkuCode());
                if (detail.getAfterSaleAmount().compareTo(new BigDecimal(BigInteger.ZERO)) > 0)
                    transferGoodsItemList.add(transferGoodsItem);

            });
        }
        CloudRegulateParam cloudRegulateParam = new CloudRegulateParam();

        cloudRegulateParam.setGoodsItemList(transferGoodsItemList);
        cloudRegulateParam.setMallUserId(mallUserId);
        cloudRegulateParam.setMemo(memo);
        cloudRegulateParam.setType("0");
        agentFeign.cloudAddOrSubRegulate(cloudRegulateParam);
    }

    /**
     * 订单问题反馈处理类型：补发
     */
    public Boolean JDPushAgain(MallOrderFeedBack mallOrderFeedBack, List<MallFeedbackGoods> goodsList) {
        //  京东推单
        List<String> goodsNo = new ArrayList<>(); // 商品编码
        List<String> amount = new ArrayList<>(); // 数量

        List<MallFeedbackGoods> gifts = this.giveThisItem(goodsList);//补发时加上赠品
        log.info("补发时加上赠品:{}", gifts);
        goodsList.addAll(gifts);

        // 5 智能组合推单
        List<MallPushItemRule> rules = mallPushItemRuleService.getPushItemRules();

        goodsList.forEach(good -> {
            if (rules != null) {
                rules.forEach(r -> {
                    if (good.getSkuCode().trim().equals(r.getSkuCode())) {
                        good.setSkuCode(r.getReplaceSkuCode());
                        good.setAfterSaleAmount(good.getAfterSaleAmount().multiply(new BigDecimal(r.getProportion().split(":")[1])));
                        return;
                    }
                });
            }
            MallSkuSpec mallSkuSpec = orderInfoMapper.getMallSkuSpec(good.getSkuCode(), "1").get(0);
            goodsNo.add(mallSkuSpec.getTransportGoodsNo());
            amount.add(String.valueOf(good.getAfterSaleAmount().intValue()));
        });

        String goodsNoStr = Joiner.on(",").join(goodsNo);
        String amountStr = Joiner.on(",").join(amount);
        EclpOrderAddOrderResponse response = jdExpressPush.jdExpressPush(mallOrderFeedBack.getId(), goodsNoStr, amountStr, mallOrderFeedBack.getAddrName()
                , mallOrderFeedBack.getAddrPhone(), mallOrderFeedBack.getAddress());
        if (ObjectUtils.isNullOrEmpty(response)) {
            log.info("订单问题反馈 补发处理-京东推单失败！");
            return false;
        }
        if (StringUtils.isEmpty(response.getEclpSoNo())) {
            log.info("订单问题反馈 补发处理-京东推单库存不足！");
            return false;
        }
        mallOrderFeedBack.setSingleNum(response.getEclpSoNo());
        mallOrderFeedBackMapper.updateById(mallOrderFeedBack);
        return true;
    }

    public List<MallFeedbackGoods> giveThisItem(List<MallFeedbackGoods> list) {
        List<MallFeedbackGoods> gifts = new ArrayList<>();
        Map<String, List<MallSku>> giftsGoods = orderDelayService.getGiftsGoods();
        list.forEach(p -> {
            giftsGoods.forEach((k, v) -> {
                if (p.getSkuCode().trim().equals(k)) {
                    v.forEach(i -> {
                        MallFeedbackGoods item = new MallFeedbackGoods();
                        item.setSkuCode(i.getSkuCode());
                        item.setAfterSaleAmount(p.getAfterSaleAmount().multiply(i.getStock()));
                        gifts.add(item);
                    });
                    return;
                }
            });
        });
        return gifts;
    }

    public void getExpressCodeFeedback() {
        //查询所有已处理的 补发的 ,并且没有物流单号的订单问题反馈
        List<MallOrderFeedBack> mallOrderFeedBacks = mallOrderFeedBackMapper.selectList(new EntityWrapper<MallOrderFeedBack>()
                .eq("is_del", "0")
                .eq("status", "1")
                .eq("after_sale_type", "2")
                .isNull("express_code")
                .isNotNull("single_num")
        );

        List<MallOrderFeedBack> update = new ArrayList<>();

        if (!CollectionUtils.isEmpty(mallOrderFeedBacks)) {
            mallOrderFeedBacks.forEach(back -> {
                String expressCode = jdExpressPush.queryJDOrderWayBill(back.getSingleNum(), 10, back.getId());

                if (!StringUtils.isEmpty(expressCode)) {
                    MallOrderFeedBack mallOrderFeedBack = new MallOrderFeedBack();
                    mallOrderFeedBack.setId(back.getId());
                    mallOrderFeedBack.setExpressCode(expressCode);
                    mallOrderFeedBack.setUpdateDate(new Date());
                    update.add(mallOrderFeedBack);
                }
            });

            if (!CollectionUtils.isEmpty(update))
                this.updateBatchById(update);

        }

    }
}
