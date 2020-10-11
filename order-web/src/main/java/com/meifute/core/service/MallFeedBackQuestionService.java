package com.meifute.core.service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.meifute.core.entity.MallFeedBackQuestion;
import com.meifute.core.mapper.MallFeedBackQuestionMapper;
import com.meifute.core.mmall.common.utils.IDUtils;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class MallFeedBackQuestionService extends ServiceImpl<MallFeedBackQuestionMapper,MallFeedBackQuestion> {

    public Boolean operate(List<MallFeedBackQuestion> feedBackQuestions){
        feedBackQuestions.forEach(q -> {
            //更新
            if(ObjectUtils.isNotNullAndEmpty(q.getId())){
                q.setUpdateDate(new Date());
                updateById(q);
            }
            //新建
            else{
                q.setId(IDUtils.genId());
                q.setCreateDate(new Date());
                insert(q);
            }
        });
        return Boolean.TRUE;
    }

    public Boolean remove(String id){
        return deleteById(id);
    }

    public List<MallFeedBackQuestion> query(String feedback_id){
        return selectList(new EntityWrapper<MallFeedBackQuestion>().eq("feedback_id", feedback_id));
    }

}
