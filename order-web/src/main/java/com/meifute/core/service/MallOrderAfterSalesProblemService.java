package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.MallOrderAfterSalesProblem;
import com.meifute.core.mapper.MallOrderAfterSalesProblemMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author lizz
 * @date 2020/3/18 13:53
 */
@Service
public class MallOrderAfterSalesProblemService extends ServiceImpl<MallOrderAfterSalesProblemMapper,MallOrderAfterSalesProblem> {

    public Boolean create(MallOrderAfterSalesProblem problem){
        problem.setId(IDUtils.genId());
        problem.setCreateDate(LocalDateTime.now());
        return this.insert(problem);
    }

    public List<MallOrderAfterSalesProblem> query(String orderId){
        return this.selectList(new EntityWrapper<MallOrderAfterSalesProblem>().eq("order_id", orderId));
    }

    public boolean delete(String id){
        return this.deleteById(id);
    }

    public boolean update(MallOrderAfterSalesProblem problem){
        problem.setUpdateDate(LocalDateTime.now());
        return this.updateById(problem);
    }
}
