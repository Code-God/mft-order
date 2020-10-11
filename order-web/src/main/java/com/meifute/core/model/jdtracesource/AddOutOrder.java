package com.meifute.core.model.jdtracesource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname addOutOrder
 * @Description TODO
 * @Date 2020-06-09 16:01
 * @Created by MR. Xb.Wu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddOutOrder implements Serializable {

    private Long ORDER_NO;

    private String FROM_CIRCSITE_ID;

    private String FROM_CIRCSITE_NAME;

    private String TO_CIRCSITE_ID;

    private String TO_CIRCSITE_NAME;

    private Double AMOUNT;

    private Double FREIGHT;

    private String TO_CIRCSITE_NO;

    private String Company;

    private String Name;

    private String Tel;

    private String PostCode;

    private String ProvinceName;

    private String CityName;

    private String ExpAreaName;

    private String THIRDSYS_ID;

    private String THIRDSYS_FLAG;

    private String COMMENT;

    private String Address;

    private List<DETAIL_LIST> DETAIL_LIST;

    private String ACCESS_UUID;

}
