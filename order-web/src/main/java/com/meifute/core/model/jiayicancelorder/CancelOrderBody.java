package com.meifute.core.model.jiayicancelorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "soap:Body")
public class CancelOrderBody {

    @XmlElement(required = true, name="CancelOrder")
    public CancelOrder cancelOrder;
}
