package com.meifute.core.model.qimen;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname QiMenPackages
 * @Description TODO
 * @Date 2020-02-18 15:58
 * @Created by MR. Xb.Wu
 */
@Data
public class QiMenPackages implements Serializable {

    List<QiMenPackage> packages;
}
