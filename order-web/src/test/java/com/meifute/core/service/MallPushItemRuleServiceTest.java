package com.meifute.core.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.mapper.MallPushItemRuleMapper;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * @Classname MallPushItemRuleServiceTest
 * @Description TODO
 * @Date 2020-06-22 10:58
 * @Created by MR. Xb.Wu
 */
public class MallPushItemRuleServiceTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    MallPushItemRuleService mallPushItemRuleService;
    @Mock
    MallPushItemRuleMapper pushItemRuleMapper;

    @Test
    public void createPushItemRule() throws ParseException {
        SimpleDateFormat sd = new SimpleDateFormat("YYYY-hh-dd HH:mm:ss");
        MallPushItemRule rule = new MallPushItemRule();
        rule.setSkuCode("C034-X");
        rule.setOnline("1");
        rule.setProportion("1:25");
        rule.setReplaceSkuCode("C034-H");
        rule.setValidStartDate(sd.parse("2020-06-30 00:00:00"));
        rule.setValidEndDate(sd.parse("2020-07-02 00:00:00"));
        mallPushItemRuleService = Mockito.spy(mallPushItemRuleService);
        when(pushItemRuleMapper.getItemRulesByTime(rule)).thenReturn(null);
        Mockito.doReturn(true).when(mallPushItemRuleService).insert(any(MallPushItemRule.class));
        mallPushItemRuleService.createPushItemRule(rule);
        Mockito.verify(mallPushItemRuleService, times(1)).createPushItemRule(rule);
    }

    @Test
    public void queryPushItemRules() {
        BaseParam baseParam = new BaseParam();
        baseParam.setPageCurrent(0);
        baseParam.setPageSize(20);
        MallPushItemRule rule = new MallPushItemRule();
        rule.setId("111");
        rule.setSkuCode("C035-H");
        rule.setReplaceSkuCode("C036-H");
        rule.setProportion("1:3");
        Page<MallPushItemRule> r = new Page<>();
        r.setRecords(Collections.singletonList(rule));
        when(pushItemRuleMapper.selectPage(any(), any())).thenReturn(Collections.singletonList(rule));
        Page<MallPushItemRule> page = mallPushItemRuleService.queryPushItemRules(baseParam);
        Assert.assertEquals(r.getRecords(), page.getRecords());
    }

    @Test
    public void editPushRule() {
        mallPushItemRuleService = Mockito.mock(MallPushItemRuleService.class);
        MallPushItemRule mallPushItemRule = new MallPushItemRule();
        mallPushItemRule.setId("111");
        mallPushItemRule.setProportion("1:4");
        mallPushItemRuleService.editPushRule(mallPushItemRule);
        Mockito.verify(mallPushItemRuleService,times(1)).editPushRule(mallPushItemRule);
    }

    @Test
    public void editPushRule_Has_ThisSkuCode() throws ParseException {
        SimpleDateFormat sd = new SimpleDateFormat("YYYY-hh-dd HH:mm:ss");
        MallPushItemRule rule = new MallPushItemRule();
        rule.setId("111");
        rule.setOnline("0");
        rule.setSkuCode("C034-H");
        rule.setValidStartDate(sd.parse("2020-06-30 00:00:00"));
        rule.setValidEndDate(sd.parse("2020-07-02 00:00:00"));
        when(pushItemRuleMapper.getCheckItemRulesById(any())).thenReturn(Arrays.asList(rule));
        try {
            mallPushItemRuleService.editPushRule(rule);
        }catch (MallException e) {
            Assert.assertEquals("该时间段内已存在该SKU的调整规则", e.getArgs()[0]);
        }
    }

    @Test
    public void deletePushRule() {
        MallPushItemRule mallPushItemRule = new MallPushItemRule();
        mallPushItemRule.setId("111");
        mallPushItemRuleService = Mockito.spy(mallPushItemRuleService);
        mallPushItemRuleService.deletePushRule(mallPushItemRule);
        verify(mallPushItemRuleService, times(1)).deletePushRule(mallPushItemRule);
    }

    @Test
    public void getPushItemRules() throws ParseException {
        List<MallPushItemRule> list = new ArrayList<>();
        SimpleDateFormat sd = new SimpleDateFormat("YYYY-hh-dd HH:mm:ss");
        MallPushItemRule rule = new MallPushItemRule();
        rule.setId("111");
        rule.setOnline("0");
        rule.setSkuCode("C034-H");
        rule.setValidStartDate(sd.parse("2020-06-30 00:00:00"));
        rule.setValidEndDate(sd.parse("2020-07-02 00:00:00"));
        list.add(rule);
        when(pushItemRuleMapper.selectList(any())).thenReturn(list);
        mallPushItemRuleService = Mockito.mock(MallPushItemRuleService.class);
        mallPushItemRuleService.getPushItemRules();
        verify(mallPushItemRuleService, times(1)).getPushItemRules();
    }
}