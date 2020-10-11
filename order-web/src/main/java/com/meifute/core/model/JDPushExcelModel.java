package com.meifute.core.model;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.metadata.BaseRowModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Classname JDExcelModel
 * @Description TODO
 * @Date 2020-04-15 18:10
 * @Created by MR. Xb.Wu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JDPushExcelModel extends BaseRowModel {

    @ExcelProperty(value = {"商家订单号","商家订单号","商家订单号"},index = 0)
    private String p1;

    @ExcelProperty(value = {"平台订单号","平台订单号","平台订单号"},index = 1)
    private String p2;

    @ExcelProperty(value = {"收件人信息","收件人姓名","收件人姓名"},index = 2)
    private String p3;

    @ExcelProperty(value = {"收件人信息","收件人手机号","收件人手机号"},index = 3)
    private String p4;

    @ExcelProperty(value = {"收件人信息","收件人座机","收件人座机"},index = 4)
    private String p5;

    @ExcelProperty(value = {"收件人信息","收件人地址","收件人地址"},index = 5)
    private String p6;

    @ExcelProperty(value = {"收件人信息","收件人公司","收件人公司"},index = 6)
    private String p7;

    @ExcelProperty(value = {"托寄物信息","物品内容","物品内容"},index = 7)
    private String p8;

    @ExcelProperty(value = {"托寄物信息","托寄物类型","托寄物类型"},index = 8)
    private String p9;

    @ExcelProperty(value = {"托寄物信息","托寄物数量","托寄物数量"},index = 9)
    private String p10;

    @ExcelProperty(value = {"托寄物信息","包裹数量","包裹数量"},index = 10)
    private String p11;

    @ExcelProperty(value = {"托寄物信息","商家箱号","商家箱号"},index = 12)
    private String p12;

    @ExcelProperty(value = {"托寄物信息","参考重量(KG)","参考重量(KG)"},index = 13)
    private String p13;

    @ExcelProperty(value = {"订单金额(元)","订单金额(元)","订单金额(元)"},index = 14)
    private String p14;

    @ExcelProperty(value = {"业务类型","业务类型","业务类型"},index = 15)
    private String p15;

    @ExcelProperty(value = {"生鲜温层","生鲜温层","生鲜温层"},index = 16)
    private String p16;

    @ExcelProperty(value = {"付费方式","付费方式","付费方式"},index = 17)
    private String p17;

    @ExcelProperty(value = {"","保价金额(元)","保价金额(元)"},index = 18)
    private String p18;

    @ExcelProperty(value = {"","签单返还","签单返还"},index = 19)
    private String p19;

    @ExcelProperty(value = {"","代收货款金额(元)","代收货款金额(元)"},index = 20)
    private String p20;

    @ExcelProperty(value = {"","京尊达","京尊达"},index = 21)
    private String p21;

    @ExcelProperty(value = {"","防撕码收集","防撕码收集"},index = 22)
    private String p22;

    @ExcelProperty(value = {"","特殊签收方式","特殊签收方式"},index = 23)
    private String p23;

    @ExcelProperty(value = {"","验货方式","验货方式"},index = 24)
    private String p24;

    @ExcelProperty(value = {"","鸡毛信","鸡毛信"},index = 25)
    private String p25;

    @ExcelProperty(value = {"备注","备注","备注"},index = 26)
    private String p26;
}
