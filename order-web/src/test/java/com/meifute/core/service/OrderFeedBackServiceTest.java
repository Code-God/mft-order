package com.meifute.core.service;

import com.meifute.core.entity.MallAgent;
import com.meifute.core.entity.MallOrderFeedBack;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.mapper.MallOrderFeedBackMapper;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

public class OrderFeedBackServiceTest {

    @Rule
    public MockitoRule mockitRule = MockitoJUnit.rule();

    private static final int CURRRENT = 0;

    private static final int PAGE_SIZE = 20;

    @InjectMocks
    OrderFeedBackService orderFeedBackService;

    @Mock
    MallOrderFeedBackMapper mallOrderFeedBackMapper;

    @Mock
    AgentFeign agentFeign;


    @Test
    public void testQueryPage() {
        String userId = "0001";
        List<MallOrderFeedBack> mallOrderFeedBacks = Lists.newArrayList();
        mallOrderFeedBacks.add(createOrderFeedBack("001", "1", null, null));
        mallOrderFeedBacks.add(createOrderFeedBack("002", "2", null, null));
        mallOrderFeedBacks.add(createOrderFeedBack("003", "3", null, null));
        when(mallOrderFeedBackMapper.selectPageByParam(createOrderFeedBack(null, "1", CURRRENT, PAGE_SIZE))).thenReturn(mallOrderFeedBacks);
        when(agentFeign.getAgentByUserId(userId)).thenReturn(null);
        Assert.assertEquals(orderFeedBackService.queryPage(createOrderFeedBack(null, "1", 0, 10)).getRecords().size(), 1);
    }

    @Test
    public void testQueryPage1() {
        List<MallOrderFeedBack> mallOrderFeedBacks = Lists.newArrayList();
        mallOrderFeedBacks.add(createOrderFeedBack("001", null, null, null));
        mallOrderFeedBacks.add(createOrderFeedBack("002", null, null, null));
        mallOrderFeedBacks.add(createOrderFeedBack("003", null, null, null));
        List<MallAgent> agents = Lists.newArrayList();
        agents.add(createMallAgent("1", "001"));
        agents.add(createMallAgent("2", "002"));
        agents.add(createMallAgent("3", "003"));
        String[] userIds = {"001", "002", "003"};
        MallOrderFeedBack queryParam = createOrderFeedBack(null, null, CURRRENT, PAGE_SIZE);
        when(mallOrderFeedBackMapper.selectPageByParam(queryParam)).thenReturn(mallOrderFeedBacks);
        when(agentFeign.getAgentsByUserIds(Arrays.asList(userIds))).thenReturn(agents);
        Assert.assertEquals(orderFeedBackService.queryPage(queryParam).getRecords().size(), 3);
    }

    private MallAgent createMallAgent(String adminCode, String userId) {
        MallAgent agent = new MallAgent();
        agent.setAdminCode(adminCode);
        agent.setUserId(userId);
        return agent;
    }

    private MallOrderFeedBack createOrderFeedBack(String userId, String adminCode, Integer current, Integer pageSize) {
        MallOrderFeedBack mallOrderFeedBack = new MallOrderFeedBack();
        mallOrderFeedBack.setFeedbackPerson(userId);
        mallOrderFeedBack.setAdminCode(adminCode);
        mallOrderFeedBack.setPageCurrent(current);
        mallOrderFeedBack.setPageSize(pageSize);
        return mallOrderFeedBack;
    }

    @Test
    public void testQueryPage_noRecords() {
        when(mallOrderFeedBackMapper.selectPageByParam(createOrderFeedBack(null, "1", CURRRENT, PAGE_SIZE))).thenReturn(null);
        Assert.assertEquals(orderFeedBackService.queryPage(createOrderFeedBack(null, "1", 0, 10)).getRecords().size(), 0);
    }
}
