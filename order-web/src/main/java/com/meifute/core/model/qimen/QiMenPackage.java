package com.meifute.core.model.qimen;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname QiMenPackage
 * @Description TODO
 * @Date 2020-02-18 13:12
 * @Created by MR. Xb.Wu
 */
@Data
public class QiMenPackage implements Serializable {

    private String expressCode;

    private String invoiceNo;

    private String logisticsCode;

    private String packageCode;

    private List<QiMenItem> items;
}
