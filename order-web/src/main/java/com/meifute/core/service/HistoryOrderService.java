package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.dto.OrderItemDetailDto;
import com.meifute.core.entity.*;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.ItemFeign;
import com.meifute.core.feignclient.PayFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.model.HistoryOrder;
import com.meifute.core.util.MybatisPageUtil;
import com.meifute.core.vo.HistoryOrderParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import xiong.utils.ExcelUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Auther: wuxb
 * @Date: 2019-06-19 14:14
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Service
@Slf4j
public class HistoryOrderService extends ServiceImpl<MallOrderInfoMapper, MallOrderInfo> {

    @Autowired
    private UserFeign userFeign;
    @Autowired
    private AgentFeign agentFeign;
    @Autowired
    private PayFeign payFeign;
    @Autowired
    private ItemFeign itemFeign;
    @Autowired
    private MallOrderItemService orderItemService;
    @Autowired
    private MallOrderInfoService orderInfoService;
    @Autowired
    private HistoryThreadService historyThreadService;


    public void readOrderExcel(MultipartFile file) {
        try {
            List<HistoryOrder> ho = ExcelUtil.readExcel(file, HistoryOrder.class);
            log.info("历史未支付的订单数量:{}", ho.size());
            ho.forEach(p ->{
                p.setPhone(p.getPhone().trim());
            });

            Map<String, List<HistoryOrder>> groupByPhone = ho.stream().collect(Collectors.groupingBy(HistoryOrder::getPhone));

            List<HistoryOrder> list = new ArrayList<>();
            //遍历分组
            for (Map.Entry<String, List<HistoryOrder>> entryHistory : groupByPhone.entrySet()) {
                String phone = entryHistory.getKey();
                List<HistoryOrder> entryHistoryValue = entryHistory.getValue();
                BigDecimal total = entryHistoryValue.stream().map(HistoryOrder::getAmt).reduce(BigDecimal.ZERO, BigDecimal::add);

                HistoryOrder h = new HistoryOrder();
                h.setName(entryHistoryValue.get(0).getName());
                h.setPhone(phone);
                h.setAmt(total);
                h.setCreateDate(entryHistoryValue.get(0).getCreateDate());
                h.setSubordinate(entryHistoryValue.get(0).getSubordinate());
                list.add(h);
                historyThreadService.makeHistoryOrderInfo(h);
            }
            log.info("去重后的历史未支付的订单数量:{}", list.size());
//            historyThreadService.makeHistoryOrderInfo(list);
//            threadAccess(list);
            log.info("数据导入完毕");
        } catch (Exception e) {
            log.info("处理订单异常:{}",e);
            throw new MallException("00998", new Object[]{"导入数据异常"});
        }
    }


    private void threadAccess(List<HistoryOrder> list) {
        try {
            long start = System.currentTimeMillis();
            // 每500条数据开启一条线程
            int threadSize = 500;
            // 总数据条数
            int dataSize = list.size();
            // 线程数
            int threadNum = dataSize / threadSize + 1;
            // 定义标记,过滤threadNum为整数
            boolean special = dataSize % threadSize == 0;

            List<HistoryOrder> cutList = null;

            // 确定每条线程的数据
            for (int i = 0; i < threadNum; i++) {
                if (i == threadNum - 1) {
                    if (special) {
                        break;
                    }
                    cutList = list.subList(threadSize * i, dataSize);
                } else {
                    cutList = list.subList(threadSize * i, threadSize * (i + 1));
                }
                final List<HistoryOrder> listStr = cutList;

                log.info(Thread.currentThread().getName() + "线程：" + i);
                try {
                    historyThreadService.makeHistoryOrderInfo(listStr);
                } catch (Exception e) {
                    log.info("执行异常:{}", e);
                }
            }

            log.info("线程任务执行结束");
            log.info("执行任务消耗了 ：" + (System.currentTimeMillis() - start) + "毫秒");
        } catch (Exception e) {
            throw new MallException("00998", new Object[]{"导入数据异常"});
        }

    }

//    private void threadAccess(List<HistoryOrder> list) {
//        try{
//            long start = System.currentTimeMillis();
//            // 每500条数据开启一条线程
//            int threadSize = 500;
//            // 总数据条数
//            int dataSize = list.size();
//            // 线程数
//            int threadNum = dataSize / threadSize + 1;
//            // 定义标记,过滤threadNum为整数
//            boolean special = dataSize % threadSize == 0;
//            // 创建一个线程池
//            ExecutorService exec = Executors.newFixedThreadPool(threadNum);
//            // 定义一个任务集合
//            List<Callable<Integer>> tasks = new ArrayList<Callable<Integer>>();
//            Callable<Integer> task = null;
//            List<HistoryOrder> cutList = null;
//
//            // 确定每条线程的数据
//            for (int i = 0; i < threadNum; i++) {
//                if (i == threadNum - 1) {
//                    if (special) {
//                        break;
//                    }
//                    cutList = list.subList(threadSize * i, dataSize);
//                } else {
//                    cutList = list.subList(threadSize * i, threadSize * (i + 1));
//                }
//                final List<HistoryOrder> listStr = cutList;
//                task = new Callable<Integer>() {
//                    @Override
//                    public Integer call() {
//                        log.info(Thread.currentThread().getName() + "线程start：" + listStr);
//                        try{
//                            historyThreadService.makeHistoryOrderInfo(listStr);
//                        }catch (Exception e) {
//                            log.info("执行异常",e);
//                        }
//                        log.info(Thread.currentThread().getName() + "线程end：" + listStr);
//                        return 1;
//                    }
//                };
//                // 这里提交的任务容器列表和返回的Future列表存在顺序对应的关系
//                tasks.add(task);
//            }
//
//            exec.invokeAll(tasks);
//            // 关闭线程池
//            exec.shutdown();
//            log.info("线程任务执行结束");
//            log.info("执行任务消耗了 ：" + (System.currentTimeMillis() - start) + "毫秒");
//        }catch (Exception e) {
//            throw new MallException("00998", new Object[]{"导入数据异常"});
//        }
//
//    }


    public boolean checkHistoryOrder(String userId, String orderStatus) {
        List<MallOrderInfo> list = this.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("history_freight_status", "1")
                .eq("mall_user_id", userId)
                .eq(orderStatus != null, "order_status", orderStatus)
                .eq("is_del", "0"));
        if(!CollectionUtils.isEmpty(list)) {
            for (MallOrderInfo l : list) {
                Date time = l.getAuthTime();
                if(time != null) {
                    if(new Date().getTime() <= time.getTime()) {
                        return false;
                    }
                }
            }

        }
        return !CollectionUtils.isEmpty(list);
    }

    @Transactional
    public void adminToUpdateHistoryOrderInfo(HistoryOrderParam historyOrderParam) {
        MallOrderInfo mallOrderInfo = new MallOrderInfo();
        mallOrderInfo.setOrderId(historyOrderParam.getOrderId());
        mallOrderInfo.setCsMemo(historyOrderParam.getCsMemo());
        if(ObjectUtils.isNotNullAndEmpty(historyOrderParam.getPayStatus())) {
            if("0".equals(historyOrderParam.getPayStatus())) {
                mallOrderInfo.setOrderStatus("0");
//                MallPayInfo info = payFeign.getPayInfoByOrderId(historyOrderParam.getOrderId());
//                info.setPayStatus("0");
//                payFeign.updatePayInfo(info);
            }else {
                mallOrderInfo.setOrderStatus("5");
            }
        }
        mallOrderInfo.setAuthTime(historyOrderParam.getAuthTime());
        this.updateById(mallOrderInfo);
    }


    public List<MallOrderInfo> getHistoryOrderInfo(MallUser user) {
        MallAccount account = userFeign.getAccountInfo(user.getId());
        MallAgent agent = agentFeign.getAgentByUserId(user.getId());
        List<Integer> payType = orderInfoService.getPayType("0", agent.getCompanyId());
        List<MallOrderInfo> list = this.selectList(new EntityWrapper<MallOrderInfo>()
                .eq("history_freight_status", "1")
                .eq("mall_user_id", user.getId())
                .eq("order_status", "0")
                .eq("is_del", "0"));
        if (!CollectionUtils.isEmpty(list)) {
            List<OrderItemDetailDto> itemDto = new ArrayList<>();
            for (MallOrderInfo l : list) {
                List<MallOrderItem> items = orderItemService.selectByOrderId(l.getOrderId());
                items.forEach(p -> {
                    MallSku sku = itemFeign.getSkuByCode(p.getSkuCode());
                    OrderItemDetailDto i = new OrderItemDetailDto();
                    i.setTitle(sku.getTitle());
                    i.setPrice(p.getPrice());
                    i.setAmount(p.getAmount());
                    itemDto.add(i);
                });
                l.setOrderItemDetailDtos(itemDto);
                l.setName(user.getName());
                l.setPhone(user.getPhone());
                l.setBalance(account.getAmt());
                l.setPayTypeKey(payType);
            }
        }
        return list;
    }

    public Page<HistoryOrderParam> getHistoryOrderDetail(HistoryOrderParam historyOrderParam) {
        Page<MallOrderInfo> page = MybatisPageUtil.getPage(historyOrderParam.getPageCurrent(),historyOrderParam.getPageSize());
        List<String> userIds = null;
        if(historyOrderParam.getName() != null) {
            List<MallUser> name = userFeign.getUserByNickName(historyOrderParam.getName());
            if(name != null && name.size()>0) {
                userIds = name.stream().map(MallUser::getId).collect(Collectors.toList());
            }

        }
        if(historyOrderParam.getPhone() != null) {
            MallUser user = userFeign.getUserByPhone(historyOrderParam.getPhone());
            if(user != null) {
                if(userIds != null) {
                    userIds.add(user.getId());
                }else {
                    userIds = new ArrayList<>();
                    userIds.add(user.getId());
                }
            }
        }
        List<String> payStatus = null;
        if ("0".equals(historyOrderParam.getPayStatus())) {
            payStatus = Collections.singletonList("0");
        } else if(!"0".equals(historyOrderParam.getPayStatus()) && historyOrderParam.getPayStatus() != null){
            payStatus = Arrays.asList("3","4","5");
        }

        Page<MallOrderInfo> list = this.selectPage(page, new EntityWrapper<MallOrderInfo>()
                .eq("history_freight_status", "1")
                .eq(historyOrderParam.getOrderId() != null,"order_id",historyOrderParam.getOrderId())
                .in(userIds != null, "mall_user_id", userIds)
                .eq("is_del", "0")
                .in(payStatus != null,"order_status", payStatus)
                .ge(historyOrderParam.getBeginTime() != null,"pay_date", historyOrderParam.getBeginTime())
                .le(historyOrderParam.getEndTime() != null,"pay_date", historyOrderParam.getEndTime()));

        Page<HistoryOrderParam> pageDto = new Page<>();
        List<HistoryOrderParam> r = new ArrayList<>();
        if (!CollectionUtils.isEmpty(list.getRecords())) {
            for (MallOrderInfo l : list.getRecords()) {
                MallUser userById = userFeign.getUserById(l.getMallUserId());
                HistoryOrderParam dto = new HistoryOrderParam();

                MallAgent father = agentFeign.getLeaderAgentByUserId(userById.getId());
                if(father != null) {
                    MallUser fatherUser = userFeign.getUserById(father.getUserId());
                    dto.setFatherIcon(fatherUser.getIcon());
                    dto.setFatherName(fatherUser.getName());
                    dto.setFatherPhone(fatherUser.getPhone());
                    dto.setFatherLevel(father.getAgentLevel());
                }

                dto.setOrderId(l.getOrderId());
                dto.setIcon(userById.getIcon());
                dto.setName(userById.getName());
                dto.setPhone(userById.getPhone());
                dto.setAgentLevel(userById.getRoleId());
                dto.setPayStatus(l.getOrderStatus());
                dto.setPayDate(l.getPayDate());
                dto.setAmt(l.getPaymentAmt());
                dto.setCsMemo(l.getCsMemo());
                dto.setAuthTime(l.getAuthTime());

                MallAgent agent = agentFeign.getAgentByUserId(l.getMallUserId());
                String companyName = getCompanyName(agent.getCompanyId(), l.getMallUserId());
                dto.setCompanyName(companyName);

                r.add(dto);
            }
        }
        BeanUtils.copyProperties(list, pageDto, "records");
        pageDto.setRecords(r);
        return pageDto;
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
}
