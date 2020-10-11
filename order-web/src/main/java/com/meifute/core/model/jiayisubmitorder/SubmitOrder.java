package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = {
//        "asnOut"
//})
@XmlRootElement(name = "SubmitOrder")
public class SubmitOrder {

    @XmlAttribute(name="xmlns")
    protected String xsi="http://tempuri.org/";


    @XmlElement(required = true, name="asnOut")
    protected AsnOut asnOut;

    @XmlElement(required = true, name="cargoOutList")
    protected CargoOutList cargoOutList;

    protected String key;
}
