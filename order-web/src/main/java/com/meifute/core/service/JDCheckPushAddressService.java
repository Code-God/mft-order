package com.meifute.core.service;

import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.request.etms.EtmsRangeCheckRequest;
import com.jd.open.api.sdk.response.etms.EtmsRangeCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @Auther: wll
 * @Date: 2019/3/13 17:23
 * @Auto: I AM A CODE MAN !
 * @Description:
 */
@Component
@Slf4j
public class JDCheckPushAddressService {

    public final static String serverUrl = "https://api.jd.com/routerjson";
    public final static String accessToken = "0d5cc377-fbe1-497f-84c6-437c3a4cfe99";
    public final static String appkey = "0C2AC54D67CD3E5ACD7EC00EFE395BAC";
    public final static String appkeySecret = "4cb1120cda6f47b2ad46527e226f62ed";

    /**
     * 京东校验地址是否可达   token过期后需要重新授权获取
     *
     * @param address
     * @return
     * @throws Exception
     */
    public boolean checkPushAddress(String orderId, String address) {
        try {
            JdClient client = new DefaultJdClient(serverUrl, accessToken, appkey, appkeySecret);
            EtmsRangeCheckRequest request = new EtmsRangeCheckRequest();
            request.setCustomerCode("021K216164");
            request.setOrderId(orderId);
            request.setGoodsType(1);
            request.setReceiveAddress(address);
            request.setWareHouseCode("01");
            request.setIsCod(0);
            EtmsRangeCheckResponse response = client.execute(request);
            return response.getResultInfo().getRcode().equals(100);
        } catch (Exception e) {
            log.info("===================:{}", e);
            return false;
        }
    }

}

