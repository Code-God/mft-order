package com.meifute.core.controller;

import com.baomidou.mybatisplus.plugins.Page;
import com.meifute.core.mmall.common.dto.BaseParam;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import com.meifute.core.service.MallPushItemRuleService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.Mockito.when;

/**
 * @Classname MallPushItemRuleControllerTest
 * @Description TODO
 * @Date 2020-06-22 10:58
 * @Created by MR. Xb.Wu
 */
public class MallPushItemRuleControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private MallPushItemRuleController controller;

    @Mock
    MallPushItemRuleService mallPushItemRuleService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void queryPushItemRules() throws Exception {
        BaseParam param = new BaseParam();
        param.setPageCurrent(0);
        param.setPageSize(20);
        Page<MallPushItemRule> page = new Page<>();
        MallPushItemRule rule = new MallPushItemRule();
        rule.setOnline("1");
        rule.setSkuCode("C034-H");
        page.setRecords(Arrays.asList(rule));
        when(mallPushItemRuleService.selectPage(Mockito.any())).thenReturn(page);
        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/v2/app/push/rule/query/item/rules")
                .content(JSONUtil.obj2json(param))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Assert.assertNotNull(mvcResult.getResponse());

    }
}