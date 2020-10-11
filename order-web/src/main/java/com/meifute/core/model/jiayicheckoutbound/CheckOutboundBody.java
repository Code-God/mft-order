package com.meifute.core.model.jiayicheckoutbound;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @Classname CheckOutbound
 * @Description TODO
 * @Date 2019-11-16 14:09
 * @Created by MR. Xb.Wu
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "soap:Body")
public class CheckOutboundBody {

    @XmlElement(required = true, name="CheckOutbound")
    public CheckOutbound checkOutbound;
}
