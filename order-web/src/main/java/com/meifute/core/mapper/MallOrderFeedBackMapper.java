package com.meifute.core.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.entity.MallAdminAgent;
import com.meifute.core.entity.MallOrderFeedBack;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Auther: wuxb
 * @Date: 2019-05-24 12:29
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Repository
public interface MallOrderFeedBackMapper extends BaseMapper<MallOrderFeedBack> {


    List<MallOrderFeedBack> selectPageByParam(@Param("param") MallOrderFeedBack mallOrderFeedBack);

    List<MallOrderFeedBack> doExportAboutFeedback(@Param("param")MallOrderFeedBack mallOrderFeedBack);
}
