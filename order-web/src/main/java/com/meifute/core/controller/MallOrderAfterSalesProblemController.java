package com.meifute.core.controller;

import com.meifute.core.entity.MallOrderAfterSalesProblem;
import com.meifute.core.mmall.common.controller.BaseController;
import com.meifute.core.mmall.common.response.MallResponse;
import com.meifute.core.service.MallOrderAfterSalesProblemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author lizz
 * @date 2020/3/18 13:51
 */
@RestController("/v2/after/sales")
public class MallOrderAfterSalesProblemController extends BaseController {


    @Autowired
    private MallOrderAfterSalesProblemService problemService;

    @PostMapping(value = "/create")
    public ResponseEntity<MallResponse> create(@RequestBody MallOrderAfterSalesProblem problem){
        return ResponseEntity.ok(successResult(problemService.create(problem)));
    }


    @GetMapping(value = "/query")
    public ResponseEntity<MallResponse> query(@RequestParam String orderId){
        return ResponseEntity.ok(successResult(problemService.query(orderId)));
    }

    @GetMapping(value = "/delete")
    public ResponseEntity<MallResponse> delete(@RequestParam String id){
        return ResponseEntity.ok(successResult(problemService.delete(id)));
    }

    @PostMapping(value = "/update")
    public ResponseEntity<MallResponse> update(@RequestBody MallOrderAfterSalesProblem problem){
        return ResponseEntity.ok(successResult(problemService.update(problem)));
    }
}
