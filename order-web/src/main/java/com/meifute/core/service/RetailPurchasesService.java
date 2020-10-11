package com.meifute.core.service;

import com.meifute.core.entity.MallPropelNews;
import com.meifute.core.feignclient.NotifyFeign;
import com.meifute.core.mapper.MallOrderInfoMapper;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.model.AgentPhoneAndAmount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xiong.utils.ExcelUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RetailPurchasesService {

    @Autowired
    private NotifyFeign notifyFeign;
    @Autowired
    private MallOrderInfoMapper orderInfoMapper;

    public void readExcel(MultipartFile file) {
        try {
            //需要减去的量
            List<AgentPhoneAndAmount> a = ExcelUtil.readExcel(file, AgentPhoneAndAmount.class);
            log.info("-------------------数据1:{}", a.size());
            //过滤0
            List<AgentPhoneAndAmount> f = a.stream().filter(p -> p.getAmount().compareTo(BigDecimal.ZERO) > 0).collect(Collectors.toList());
            log.info("-------------------数据2:{}", f.size());
            //进货量
            List<AgentPhoneAndAmount> r = orderInfoMapper.getRetailPurchases();
            log.info("-------------------数据3:{}", r.size());
            for (AgentPhoneAndAmount p : r) {
                for (AgentPhoneAndAmount d : f) {
                    if (Objects.equals(p.getPhone(), d.getPhone())) {
                        p.setAmount(p.getAmount().subtract(d.getAmount()));
                    }
                }
                sendNotify(p.getUserId(), p.getAmount());
            }
            log.info("数据导入完毕");
        } catch (Exception e) {
            log.info("处理导入异常" + e);
            throw new MallException("00998", new Object[]{"导入数据异常"});
        }
    }

    private void sendNotify(String userId, BigDecimal num) {
        MallPropelNews mallPropelNews = new MallPropelNews();
        mallPropelNews.setMallUserId(userId);
        mallPropelNews.setTitle("进货量提醒");
        mallPropelNews.setSubTitle("温馨提醒：尊敬的代理！您好！您本月进货数量为【" + num + "】盒。马上就到月底了，您可以自行选择进货数量，公司会根据进货量发放相应的奖金，如进货数量不足，平台无法生成奖励并发放的，请知晓哦~~~");
        mallPropelNews.setType("0");
        mallPropelNews.setStatus("0");
        mallPropelNews.setNewsType("3");
        notifyFeign.createNewPropelNews(mallPropelNews);
    }
}
