package com.meifute.core.model.jdtracesource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @Classname AddOutRecord
 * @Description TODO
 * @Date 2020-06-09 16:36
 * @Created by MR. Xb.Wu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddOutRecord implements Serializable {

    private Long ORDER_NO;

    @JsonProperty("FROM_CIRCSITE_ID")
    private String FROM_CIRCSITE_ID;

    @JsonProperty("FROM_CIRCSITE_NAME")
    private String FROM_CIRCSITE_NAME;

    @JsonProperty("TO_CIRCSITE_ID")
    private String TO_CIRCSITE_ID;

    @JsonProperty("TO_CIRCSITE_NAME")
    private String TO_CIRCSITE_NAME;

    @JsonProperty("TRACECODE_ID")
    private String TRACECODE_ID;

    @JsonProperty("ACCESS_UUID")
    private String ACCESS_UUID;

    private Long ORDER_DETAIL_NO;

    private List<String> TRACECODELIST;
}
