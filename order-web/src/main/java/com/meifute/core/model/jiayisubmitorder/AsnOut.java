package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = {
//        "theCityName"
//})
@XmlRootElement(name = "asnOut")
public class AsnOut {

    private Integer WarehouseId;
    private Integer ProjectId;
    private Date PayTime;  //付款时间
    private String Carrier; //快递公司
    private String Shipper_Person; //发货店铺
    private String Con_City;    //省市区
    private String Con_Address; //地址
    private String Con_Person; //收货人
    private String Con_Tel; //电话
    private String Con_Mobile; //手机
    private String DN; //订单号
    private String Memo; //备注
}
