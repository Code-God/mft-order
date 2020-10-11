package com.meifute.core.model.jiayicancelorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CancelOrderResponse")
public class CancelOrderResponse implements Serializable {

    private Boolean CancelOrderResult;

    private String result;
}
