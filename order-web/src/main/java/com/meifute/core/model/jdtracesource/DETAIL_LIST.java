package com.meifute.core.model.jdtracesource;

import java.io.Serializable;

/**
 * @Classname DETAIL_LIST
 * @Description TODO
 * @Date 2020-06-09 16:07
 * @Created by MR. Xb.Wu
 */
public class DETAIL_LIST implements Serializable {

    private Long ORDER_DETAIL_NO;

    private Long PRODUCT_ID;

    private String PRODUCT_THIRDSYS_ID;

    private String PRODUCT_NAME;

    private String PRODUCT_NO;

    private String SPEC;

    private Double WEIGHT;

    private Double VOLUME;

    private String UNIT;

    private Integer COUNT;

    private String THIRDSYS_ID;

    private String THIRDSYS_FLAG;

    public Long getORDER_DETAIL_NO() {
        return ORDER_DETAIL_NO;
    }

    public void setORDER_DETAIL_NO(Long ORDER_DETAIL_NO) {
        this.ORDER_DETAIL_NO = ORDER_DETAIL_NO;
    }

    public Long getPRODUCT_ID() {
        return PRODUCT_ID;
    }

    public void setPRODUCT_ID(Long PRODUCT_ID) {
        this.PRODUCT_ID = PRODUCT_ID;
    }

    public String getPRODUCT_THIRDSYS_ID() {
        return PRODUCT_THIRDSYS_ID;
    }

    public void setPRODUCT_THIRDSYS_ID(String PRODUCT_THIRDSYS_ID) {
        this.PRODUCT_THIRDSYS_ID = PRODUCT_THIRDSYS_ID;
    }

    public String getPRODUCT_NAME() {
        return PRODUCT_NAME;
    }

    public void setPRODUCT_NAME(String PRODUCT_NAME) {
        this.PRODUCT_NAME = PRODUCT_NAME;
    }

    public String getPRODUCT_NO() {
        return PRODUCT_NO;
    }

    public void setPRODUCT_NO(String PRODUCT_NO) {
        this.PRODUCT_NO = PRODUCT_NO;
    }

    public String getSPEC() {
        return SPEC;
    }

    public void setSPEC(String SPEC) {
        this.SPEC = SPEC;
    }

    public Double getWEIGHT() {
        return WEIGHT;
    }

    public void setWEIGHT(Double WEIGHT) {
        this.WEIGHT = WEIGHT;
    }

    public Double getVOLUME() {
        return VOLUME;
    }

    public void setVOLUME(Double VOLUME) {
        this.VOLUME = VOLUME;
    }

    public String getUNIT() {
        return UNIT;
    }

    public void setUNIT(String UNIT) {
        this.UNIT = UNIT;
    }

    public Integer getCOUNT() {
        return COUNT;
    }

    public void setCOUNT(Integer COUNT) {
        this.COUNT = COUNT;
    }

    public String getTHIRDSYS_ID() {
        return THIRDSYS_ID;
    }

    public void setTHIRDSYS_ID(String THIRDSYS_ID) {
        this.THIRDSYS_ID = THIRDSYS_ID;
    }

    public String getTHIRDSYS_FLAG() {
        return THIRDSYS_FLAG;
    }

    public void setTHIRDSYS_FLAG(String THIRDSYS_FLAG) {
        this.THIRDSYS_FLAG = THIRDSYS_FLAG;
    }




}
