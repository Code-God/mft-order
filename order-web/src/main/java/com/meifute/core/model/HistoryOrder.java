package com.meifute.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Auther: wuxb
 * @Date: 2019-06-19 15:13
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Data
public class HistoryOrder extends BaseRowModel {

    @ExcelProperty(index = 5, value = "用户昵称")
    private String name;

    @ExcelProperty(index = 7, value = "用户手机号")
    private String phone;

    @ExcelProperty(index = 8, value = "金额")
    private BigDecimal amt;

    @ExcelProperty(index = 9, value = "创建时间")
    private Date createDate;

    @ExcelProperty(index = 5, value = "所属人姓名")
    private String subordinate;
}
