package com.meifute.core.model.jdtracesource;

import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @Auther: wxb
 * @Date: 2018/10/30 14:51
 * @Auto: I AM A CODE MAN -_-!
 * @Description:
 */
@Slf4j
public class HttpRequest {

    /**
     * 发送post请求
     *
     * @param url
     * @param json
     * @return
     */
    public static String post(String url, String json, String token) {
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setContentCharset("UTF-8");
        PostMethod method = new PostMethod(url);
        RequestEntity entity = null;
        String returnStr = "";
        try {
            entity = new StringRequestEntity(json, "application/json", "UTF-8");
            method.setRequestEntity(entity);
            if (ObjectUtils.isNotNullAndEmpty(token)) {
                method.setRequestHeader("Cookie", token);
                method.setRequestHeader("Accept", "application/json");
            }
            httpClient.executeMethod(method);
            InputStream in = method.getResponseBodyAsStream();
            //下面将stream转换为String
            StringBuilder sb = new StringBuilder();
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            char[] b = new char[4096];
            for (int n; (n = isr.read(b)) != -1; ) {
                sb.append(new String(b, 0, n));
            }
            returnStr = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnStr;
    }

    public static String login(String url, String json) {
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setContentCharset("UTF-8");
        PostMethod method = new PostMethod(url);
        String returnStr = "";
        try {
            RequestEntity entity = new StringRequestEntity(json, "application/json", "UTF-8");
            method.setRequestEntity(entity);
            method.setRequestHeader("Accept", "application/json");
            httpClient.executeMethod(method);

            InputStream in = method.getResponseBodyAsStream();
            //下面将stream转换为String
            StringBuilder sb = new StringBuilder();
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            char[] b = new char[4096];
            for (int n; (n = isr.read(b)) != -1; ) {
                sb.append(new String(b, 0, n));
            }
            String response = sb.toString();
            log.info("=======reponse:{}", response);
            Map<String, Object> map = JSONUtil.json2map(response);
            String status = String.valueOf(map.get("status"));
            if ("0".equals(status)) {
                Header responseHeader = method.getResponseHeader("Set-Cookie");
                String token = responseHeader.getValue();
                returnStr = token.split(";")[0];
                log.info("=======token:{}", returnStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnStr;
    }


}
