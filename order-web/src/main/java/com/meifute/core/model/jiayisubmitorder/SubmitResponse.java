package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlRootElement(name = "soap:Envelope")
@XmlRootElement(name="Envelope")
public class SubmitResponse {

    @XmlElement(required = true, name="Envelope")
    protected String xsdsss="http://www.w3.org/2001/XMLSchema";

//    @XmlAttribute(name="xmlns:xsi")
    @XmlElement(required = true, name="xmlns:xsi")
    protected String xsi;
//    @XmlAttribute(name="xmlns:xsd")
    @XmlElement(required = true, name="xmlns:xsd")
    protected String xsd;
//    @XmlAttribute(name="xmlns:soap")
    @XmlElement(required = true, name="xmlns:soap")
    protected String soap;

    protected String uri;

    @XmlElement(required = true, name="soap:Body")
    protected SubmitResponseBody body;
}
