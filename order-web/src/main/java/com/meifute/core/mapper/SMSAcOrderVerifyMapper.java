package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.entity.SMSAcOrderVerify;
import com.meifute.core.vo.QuerySMSVerify;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Classname SMSRecordHitsMapper
 * @Description
 * @Date 2019-11-19 15:52
 * @Created by MR. Xb.Wu
 */
@Repository
public interface SMSAcOrderVerifyMapper extends BaseMapper<SMSAcOrderVerify> {

    List<SMSAcOrderVerify> queryByOrderStatus(QuerySMSVerify querySMSVerify, Pagination page);

    List<SMSAcOrderVerify> queryToBeAudited(QuerySMSVerify querySMSVerify);
}
