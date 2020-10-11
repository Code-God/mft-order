package com.meifute.core.model.jiayicancelorder;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CancelOrder")
public class CancelOrder {

    @XmlAttribute(name="xmlns")
    protected String xsi="http://tempuri.org/";

    private Integer projectId;

    protected String dn;

    protected String key;
}
