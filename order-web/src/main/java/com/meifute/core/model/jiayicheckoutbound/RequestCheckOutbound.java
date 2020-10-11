package com.meifute.core.model.jiayicheckoutbound;

import lombok.Data;

import javax.xml.bind.annotation.*;

/**
 * @Classname RequestCheckOutbound
 * @Description TODO
 * @Date 2019-11-16 14:09
 * @Created by MR. Xb.Wu
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "body"
})
@XmlRootElement(name = "soap:Envelope")
public class RequestCheckOutbound {

    @XmlAttribute(name="xmlns:xsi")
    protected String xsi="http://www.w3.org/2001/XMLSchema-instance";
    @XmlAttribute(name="xmlns:xsd")
    protected String xsd="http://www.w3.org/2001/XMLSchema";
    @XmlAttribute(name="xmlns:soap")
    protected String soap="http://schemas.xmlsoap.org/soap/envelope/";

    @XmlElement(required = true, name="soap:Body")
    protected CheckOutboundBody body;
}
