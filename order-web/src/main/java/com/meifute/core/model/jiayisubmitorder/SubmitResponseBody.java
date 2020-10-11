package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "soap:Body")
public class SubmitResponseBody {

    @XmlElement(required = true, name="SubmitOrderResponse")
    protected SubmitOrderResponse submitOrderResponse;
}
