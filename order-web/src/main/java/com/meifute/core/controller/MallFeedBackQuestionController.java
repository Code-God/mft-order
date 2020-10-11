package com.meifute.core.controller;

import com.meifute.core.entity.MallFeedBackQuestion;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallFeedBackQuestionService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/app/feedback/question")
public class MallFeedBackQuestionController extends BaseController {

    @Autowired
    private MallFeedBackQuestionService feedBackQuestionService;

    @ApiOperation(value = "新问题 添加&更新",notes = "新问题 添加&更新")
    @PostMapping("/operate/question")
    public ResponseEntity<MallResponse> operate(@RequestBody List<MallFeedBackQuestion> feedBackQuestions){
        Boolean operate = feedBackQuestionService.operate(feedBackQuestions);
        return ResponseEntity.ok(successResult(operate));
    }

    @ApiOperation(value = "新问题查询",notes = "新问题查询")
    @GetMapping("/query/question")
    public ResponseEntity<MallResponse> query(@RequestParam String feedBackId){
        List<MallFeedBackQuestion> query = feedBackQuestionService.query(feedBackId);
        return ResponseEntity.ok(successResult(query));
    }

    @ApiOperation(value = "新问题移除",notes = "新问题移除")
    @GetMapping("/remove/question")
    public ResponseEntity<MallResponse> remove(@RequestParam String questionId){
        Boolean remove = feedBackQuestionService.remove(questionId);
        return ResponseEntity.ok(successResult(remove));
    }

}
