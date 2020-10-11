package com.meifute.core.model.jiayisubmitorder;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = {
//        "theCityName"
//})
@XmlRootElement(name = "cargoOutList")
public class CargoOutList {

    @XmlElement(required = true, name="CargoOut")
    private List<CargoOut> cargoOutList;
}

