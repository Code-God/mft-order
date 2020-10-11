package com.meifute.core.model.qimen;

import lombok.Data;

import java.io.Serializable;

/**
 * @Classname QiMenItem
 * @Description TODO
 * @Date 2020-02-18 13:14
 * @Created by MR. Xb.Wu
 */
@Data
public class QiMenItem implements Serializable {

    private String itemCode;
    private String itemId;
    private String quantity;
}
