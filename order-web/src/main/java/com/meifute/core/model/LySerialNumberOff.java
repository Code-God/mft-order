package com.meifute.core.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @Classname LySerailNumberOff
 * @Description TODO
 * @Date 2020-06-17 17:58
 * @Created by MR. Xb.Wu
 */
@Data
public class LySerialNumberOff implements Serializable {

    private Boolean on;

    private Date startTime;
}
