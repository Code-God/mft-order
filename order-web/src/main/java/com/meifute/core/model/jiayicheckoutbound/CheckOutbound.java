package com.meifute.core.model.jiayicheckoutbound;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @Classname CheckOutBount
 * @Description TODO
 * @Date 2019-11-16 14:11
 * @Created by MR. Xb.Wu
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CheckOutbound")
public class CheckOutbound {

    @XmlAttribute(name="xmlns")
    protected String xsi="http://tempuri.org/";

    private Integer projectId;

    protected String dn;

    protected String key;
}
