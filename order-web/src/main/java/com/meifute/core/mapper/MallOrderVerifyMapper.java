package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.dto.OrderVerifyDto;
import com.meifute.core.entity.MallOrderVerify;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.access.method.P;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Auther: wxb
 * @Date: 2018/10/15 18:43
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Repository
public interface MallOrderVerifyMapper extends BaseMapper<MallOrderVerify> {
    //敏感订单列表分页
    List<OrderVerifyDto> querySensitiveGoodsVerifyList(@Param("param") MallOrderVerify mallOrderVerify, Pagination pagination);
    //敏感订单列表不分页
    List<OrderVerifyDto> querySensitiveGoodsVerifyList(@Param("param") MallOrderVerify mallOrderVerify);

    List<MallOrderVerify> queryMallOrderVerifyPageList(@Param("param") MallOrderVerify mallOrderVerify, Pagination pagination);

    //敏感订单列表--查询对应商务为null的数据
    List<OrderVerifyDto> queryVerifyOrderListFirstParent(@Param("param") MallOrderVerify mallOrderVerify);

    //根据userIdc查询上上级adminCode
    String querySecondParentAdminCode(@Param("userId") String userId);

    //根据userId查询上级adminCode
    String queryFirstParentAdminCode(@Param("userId") String userId);


}
