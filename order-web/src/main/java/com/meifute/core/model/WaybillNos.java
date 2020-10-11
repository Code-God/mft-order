package com.meifute.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaybillNos implements Serializable {

    private String orderId;

    private String waybillNo;

    private String expressName;

}
