package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.meifute.core.model.pushItemrule.MallPushItemRule;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Classname MallPushItemRuleMapper
 * @Description TODO
 * @Date 2020-06-19 17:42
 * @Created by MR. Xb.Wu
 */
@Repository
public interface MallPushItemRuleMapper extends BaseMapper<MallPushItemRule> {

    List<MallPushItemRule> getItemRulesByTime(MallPushItemRule rule);

    List<MallPushItemRule> getCheckItemRulesById(MallPushItemRule rule);
}
