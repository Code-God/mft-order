package com.meifute.core.model.jiayicheckoutbound;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @Classname CheckoutboundResponse
 * @Description TODO
 * @Date 2019-11-16 14:22
 * @Created by MR. Xb.Wu
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CheckOutboundResponse")
public class CheckOutboundResponse {

    private Boolean CheckOutboundResult;

    private String carrier;

    private String result;

}
