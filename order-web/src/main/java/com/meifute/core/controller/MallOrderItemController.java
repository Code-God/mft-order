package com.meifute.core.controller;

import com.meifute.core.mmall.common.controller.BaseController;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: wxb
 * @Date: 2018/10/12 15:39
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@RestController
@RequestMapping("v1/app/orderitem")
@Api(tags = "orderItem", description = "订单商品")
@Slf4j
public class MallOrderItemController extends BaseController {
}
