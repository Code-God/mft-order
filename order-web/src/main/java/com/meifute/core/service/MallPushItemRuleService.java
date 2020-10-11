package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.mapper.MallPushItemRuleMapper;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import com.meifute.core.util.MybatisPageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Classname MallPushItemRuleService
 * @Description TODO
 * @Date 2020-06-19 17:44
 * @Created by MR. Xb.Wu
 */
@Slf4j
@Service
public class MallPushItemRuleService extends ServiceImpl<MallPushItemRuleMapper, MallPushItemRule> {

    @Autowired
    private MallPushItemRuleMapper pushItemRuleMapper;

    public void createPushItemRule(MallPushItemRule mallPushItemRule) {
        if (mallPushItemRule.getSkuCode().equals(mallPushItemRule.getReplaceSkuCode())) {
            throw new MallException("00998", new Object[]{"调整前后的sku无变化"});
        }
        if (mallPushItemRule.getValidEndDate().before(mallPushItemRule.getValidStartDate())) {
            throw new MallException("00998", new Object[]{"截止时间不能小于起始时间"});
        }
        List<MallPushItemRule> rulesByTime = pushItemRuleMapper.getItemRulesByTime(mallPushItemRule);
        if (!CollectionUtils.isEmpty(rulesByTime)) {
            throw new MallException("00998", new Object[]{"该时间段内已存在该SKU的调整规则"});
        }
        mallPushItemRule.setId(IDUtils.genId());
        mallPushItemRule.setCreateDate(new Date());
        this.insert(mallPushItemRule);
    }

    public Page<MallPushItemRule> queryPushItemRules(BaseParam baseParam) {
        Page page = MybatisPageUtil.getPage(baseParam.getPageCurrent(), baseParam.getPageSize());
        return this.selectPage(page, new EntityWrapper<MallPushItemRule>().eq("is_del", "0"));
    }

    public void editPushRule(MallPushItemRule mallPushItemRule) {
        if (mallPushItemRule.getValidStartDate() != null) {
            if (mallPushItemRule.getValidEndDate().before(mallPushItemRule.getValidStartDate())) {
                throw new MallException("00998", new Object[]{"截止时间不能小于起始时间"});
            }
            List<MallPushItemRule> rulesByTime = pushItemRuleMapper.getCheckItemRulesById(mallPushItemRule);
            if (!CollectionUtils.isEmpty(rulesByTime)) {
                throw new MallException("00998", new Object[]{"该时间段内已存在该SKU的调整规则"});
            }
        }

        if ("1".equals(mallPushItemRule.getOnline())) {
            MallPushItemRule itemRule = this.selectById(mallPushItemRule.getId());
            Date now = new Date();
            if (now.before(itemRule.getValidStartDate()) || now.after(itemRule.getValidEndDate())) {
                throw new MallException("00998", new Object[]{"该规则已过期失效，无法开启"});
            }
        }


        this.updateById(mallPushItemRule);
    }

    public void deletePushRule(MallPushItemRule mallPushItemRule) {
        mallPushItemRule.setIsDel("1");
        this.updateById(mallPushItemRule);
    }

    public List<MallPushItemRule> getPushItemRules() {
        List<MallPushItemRule> itemRules = this.selectList(new EntityWrapper<MallPushItemRule>()
                .eq("is_del", "0")
                .eq("online", "1"));
        if (CollectionUtils.isEmpty(itemRules)) {
            return null;
        }
        List<MallPushItemRule> list = new ArrayList<>();
        itemRules.forEach(p ->{
            Date now = new Date();
            if (now.after(p.getValidStartDate()) && now.before(p.getValidEndDate())) {
                list.add(p);
            }
        });
        if (list.size() == 0) {
            return null;
        }
        return list;
    }

    public void checkValid() {
        List<MallPushItemRule> itemRules = this.selectList(new EntityWrapper<MallPushItemRule>()
                .eq("is_del", "0")
                .eq("online", "1"));
        if (!CollectionUtils.isEmpty(itemRules)) {
            Date now = new Date();
            List<MallPushItemRule> rules = itemRules.stream().filter(p -> now.before(p.getValidStartDate()) || now.after(p.getValidEndDate())).collect(Collectors.toList());
            rules.forEach(p ->{
                p.setOnline("0");
            });
            if (rules.size() != 0) {
                this.updateBatchById(rules);
            }
        }
    }
}
