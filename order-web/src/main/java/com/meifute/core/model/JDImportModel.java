package com.meifute.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.Data;

/**
 * @Classname JDImportModel
 * @Description TODO
 * @Date 2020-04-16 18:35
 * @Created by MR. Xb.Wu
 */
@Data
public class JDImportModel extends BaseRowModel {

    @ExcelProperty(value = "行号",index = 0)
    private String p1;

    @ExcelProperty(value = "订单号",index = 1)
    private String p2;

    @ExcelProperty(value = "运单号",index = 2)
    private String p3;

    @ExcelProperty(value = "收件人",index = 3)
    private String p4;

    @ExcelProperty(value = "收件人手机号",index = 4)
    private String p5;

    @ExcelProperty(value = "收件人座机",index = 5)
    private String p6;

    @ExcelProperty(value = "收件地址",index = 6)
    private String p7;

    @ExcelProperty(value = "下单状态",index = 7)
    private String p8;

    @ExcelProperty(value = "备注",index = 8)
    private String p9;
}
