package com.meifute.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaybillNosRequest implements Serializable {

    private String clientId;

    private String clientSecret;

    List<WaybillNos> waybillNos;
}
