package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.entity.MallRegulateGoods;
import com.meifute.core.entity.MallTransferGoods;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

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
public interface MallRegulateGoodsMapper extends BaseMapper<MallRegulateGoods> {
}
