package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = {
//        "theCityName"
//})
@XmlRootElement(name = "CargoOut")
public class CargoOut {

    private String ItemNo;  //sku

    private BigDecimal Qty; //数量

    private String Memo; //备注
}
