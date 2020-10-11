package com.meifute.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class AgentPhoneAndAmount extends BaseRowModel {

    @ExcelProperty(index = 0, value = "用户手机号")
    private String phone;

    @ExcelProperty(index = 1, value = "数量")
    private BigDecimal amount;

    private String userId;

}
