package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.entity.MallTransferGoods;
import com.meifute.core.vo.GetOrderTransferPageListParam;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 *
 * @author liuzh
 * @since 2018-09-25
 */
@Repository
public interface MallTransferGoodsMapper extends BaseMapper<MallTransferGoods> {
    List<MallTransferGoods> queryTransferGoodsPageList(@Param("param") MallTransferGoods mallTransferGoods, Pagination pagination);


    List<MallTransferGoods>  getTransferGoodsPageList(@Param("param") GetOrderTransferPageListParam mallTransferGoods, Pagination pagination);
    BigDecimal sumItem(String relationId);

    Integer updateByOrderId(@Param("param") MallTransferGoods mallTransferGoods);
}
