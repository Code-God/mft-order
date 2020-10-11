package com.meifute.core.util;

import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.plugins.pagination.PageHelper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.meifute.core.dto.PageDto;
import com.meifute.core.dto.report.AgentTotalAmountSortResponseDTO;
import org.apache.commons.collections4.ListUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;

import java.net.URISyntaxException;
import java.util.List;

/**
 * @Auther: wxb
 * @Date: 2018/9/24 13:11
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
public class MybatisPageUtil {


    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";

    private MybatisPageUtil(){

    }

    public static Page getPage(Pagination pagination, List records){
        Page page = new Page();
        page.setSize(pagination.getSize());
        page.setCurrent(pagination.getCurrent());
        page.setRecords(records);
        page.setTotal(pagination.getTotal());
        return page;
    }

    public static void startPage(int page,int size){
        PageHelper.startPage(page + 1, size);
    }

    public static Page getPage(int page,int size){
        return new Page(page + 1,size);
    }

    public static void remove(){
        PageHelper.remove();
    }

    public static Page getPage(Pageable pageable){
        return  new Page(pageable.getPageNumber()+1,pageable.getPageSize());
    }

    public static HttpHeaders generateHttpHeaders(Page<?> page, String baseURL) throws URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-total-count", "" + page.getTotal());
        headers.add("Link", baseURL);
        return headers;
    }

    public static PageDto pageHelper(List result, Page page, PageDto pageResult) {
        List<List> partition = ListUtils.partition(result, page.getSize());
        List list = partition.get(page.getCurrent() - 1);
        pageResult.setRecords(list);
        if (!CollectionUtils.isEmpty(result))
            pageResult.setTotal(result.size());
        return pageResult;
    }
}
