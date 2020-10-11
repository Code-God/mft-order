package com.meifute.core.util;


import com.alibaba.fastjson.JSON;
import com.meifute.core.entity.Admin;
import com.meifute.core.entity.MallAgent;
import com.meifute.core.entity.MallUser;
import com.meifute.core.feignclient.AgentFeign;
import com.meifute.core.feignclient.AuthFeign;
import com.meifute.core.feignclient.UserFeign;
import com.meifute.core.mmall.common.enums.Const;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import com.meifute.core.mmall.common.utils.StringUtils;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by liuliang on 2018/9/29.
 */
@Component
public class UserUtils {


    private static AuthFeign authFeign;

    private static AgentFeign agentFeign;

    private static UserFeign userFeign;

    @Autowired
    public UserUtils(AuthFeign authFeign, AgentFeign agentFeign,UserFeign userFeign) {
        UserUtils.authFeign = authFeign;
        UserUtils.agentFeign = agentFeign;
        UserUtils.userFeign = userFeign;
    }

    /**
     * 获取当前用户
     * @return
     */
    public static MallUser getCurrentUser() {
        Object o = authFeign.getCurrentUsers();
        MallUser user = ObjectUtils.parseMap2Object((Map<String, Object>) o, MallUser.class);
        if (user.getPhone() == null) {
            return null;
        }
        return user;
    }

    public static int getAgentLevel(String userId) {
        //查询用户的代理信息
        MallAgent agent = agentFeign.getAgentByUserId(userId);
        if (ObjectUtils.isNotNullAndEmpty(agent)) {
            return Integer.parseInt(agent.getAgentLevel());
        }
        return 0;
    }


    /**
     * 获取当前admin
     * @return
     */
    public static Admin getCurrentAdmin() {
        Object object = authFeign.getCurrentUsers();
        Admin admin = ObjectUtils.parseMap2Object((Map<String, Object>) object, Admin.class);
        if (admin.getUserType() == null) {
            return null;
        }
        return admin;
    }

    /**
     * 通过缓存id 获取用户信息 缓存查不到时查库
     * @param userId
     * @return
     */
    public static MallUser getUserInfoByCacheOrId(String userId) {
        MallUser mallUser = null;
       /* String userString = RedisUtil.get(Const.USER_INFO_MALLUSERID + userId);
        if (StringUtils.isNotEmpty(userString)) {
            JSONObject object = JSONObject.fromObject(userString);
            mallUser = (MallUser) JSONObject.toBean(object,MallUser.class);
            if (StringUtils.isBlank(mallUser.getPhone()) || StringUtils.isBlank(mallUser.getName())||StringUtils.isBlank(mallUser.getNickName())){
                mallUser = userFeign.getUserById(userId);
                RedisUtil.set(Const.USER_INFO_MALLUSERID + mallUser.getId(), JSON.toJSONString(mallUser), 60  * 10);
            }
            return mallUser;
        }*/
        mallUser = userFeign.getUserById(userId);
        if (ObjectUtils.isNotNullAndEmpty(mallUser)) {
            RedisUtil.set(Const.USER_INFO_MALLUSERID + mallUser.getId(), JSON.toJSONString(mallUser), 60 * 10);
        }
        return mallUser;
    }

}
