package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.entity.MallRegulateInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Auther: liuzh
 * @Date: 2018/10/26 13:20
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Repository
public interface MallRegulateInfoMapper extends BaseMapper<MallRegulateInfo> {

    List<MallRegulateInfo> queryMallRegulatePageList(@Param("param") MallRegulateInfo param, Pagination pagination);
}
