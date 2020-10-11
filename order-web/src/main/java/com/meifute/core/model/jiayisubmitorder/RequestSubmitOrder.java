package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "body"
})
@XmlRootElement(name = "soap:Envelope")
public class RequestSubmitOrder {

    @XmlAttribute(name="xmlns:xsi")
    protected String xsi="http://www.w3.org/2001/XMLSchema-instance";
    @XmlAttribute(name="xmlns:xsd")
    protected String xsd="http://www.w3.org/2001/XMLSchema";
    @XmlAttribute(name="xmlns:soap")
    protected String soap="http://schemas.xmlsoap.org/soap/envelope/";

    @XmlElement(required = true, name="soap:Body")
    protected SubmitOrderBody body;
}
