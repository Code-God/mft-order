package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SubmitOrderResponse")
public class SubmitOrderResponse implements Serializable {

    private Boolean SubmitOrderResult;

    private String result;
}
