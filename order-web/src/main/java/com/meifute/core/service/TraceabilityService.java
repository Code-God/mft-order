package com.meifute.core.service;

import com.alibaba.fastjson.JSONArray;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.redis.RedisUtil;
import com.meifute.core.model.jdtracesource.*;
import com.meifute.core.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @Classname TraceabilityService
 * @Description TODO
 * @Date 2020-06-09 14:36
 * @Created by MR. Xb.Wu
 */
@Slf4j
@Service
public class TraceabilityService {

    @Value("${ly_login_url}")
    private String loginUrl;

    @Value("${ly_addOutOrder_record_url}")
    private String addOutOrderRecordUrl;

    @Value("${ly_username}")
    private String username;

    @Value("${ly_password}")
    private String password;

    @Value("${ly_account}")
    private String A;

    @Value("${ly_addOutOrder_url}")
    private String lyAddOutOrderUrl;


    public String addOutOrder(AddOutOrder addOutOrder) {
        String traceSourceToken = lyLoginIn(A, username, password);
        Map<String, Object> map = new TreeMap<>();
        map.put("ORDER_NO", addOutOrder.getORDER_NO());
        map.put("FROM_CIRCSITE_ID", "302");
        map.put("FROM_CIRCSITE_NAME", "京东");
        map.put("TO_CIRCSITE_ID", addOutOrder.getTO_CIRCSITE_ID());
        map.put("TO_CIRCSITE_NAME", addOutOrder.getTO_CIRCSITE_NAME());
        map.put("AMOUNT", addOutOrder.getAMOUNT());
        map.put("FREIGHT", addOutOrder.getFREIGHT());
        map.put("TO_CIRCSITE_NO", addOutOrder.getTO_CIRCSITE_NO());
        map.put("Name", addOutOrder.getName());
        map.put("Tel", addOutOrder.getTel());
        map.put("ProvinceName", addOutOrder.getProvinceName());
        map.put("CityName", addOutOrder.getCityName());
        map.put("ExpAreaName", addOutOrder.getExpAreaName());
        map.put("Address", addOutOrder.getAddress());

        List<Map<String, Object>> lists = new ArrayList<>();

        for (DETAIL_LIST detail_list : addOutOrder.getDETAIL_LIST()) {
            Map<String, Object> map1 = new TreeMap<>();
            map1.put("ORDER_DETAIL_NO", detail_list.getORDER_DETAIL_NO());
            map1.put("PRODUCT_ID", detail_list.getPRODUCT_ID());
            map1.put("PRODUCT_NAME", detail_list.getPRODUCT_NAME());
            map1.put("PRODUCT_NO", detail_list.getPRODUCT_NO());
            map1.put("SPEC", detail_list.getSPEC());
            map1.put("WEIGHT", detail_list.getWEIGHT());
            map1.put("VOLUME", detail_list.getVOLUME());
            map1.put("UNIT", detail_list.getUNIT());
            map1.put("COUNT", detail_list.getCOUNT());
            lists.add(map1);
        }
        map.put("DETAIL_LIST", lists);
        map.put("ACCESS_UUID", addOutOrder.getORDER_NO().toString());
        String json = JsonUtils.objectToJson(map);
        log.info("------------创建连阳出库单入参..." + json);
        String response = HttpRequest.post(lyAddOutOrderUrl, json, traceSourceToken);
        log.info("------------创建连阳出库单,orderId:{},响应结果:{}...", addOutOrder.getORDER_NO(), response);
        Map<String, Object> stringObjectMap = JSONUtil.json2map(response);
        return String.valueOf(stringObjectMap.get("STATUS"));
    }


    public String addOutRecord(AddOutRecord addOutRecord) {
        String traceSourceToken = lyLoginIn(A, username, password);

        Map<String, Object> paramMap = new TreeMap<>();
        paramMap.put("ORDER_NO", addOutRecord.getORDER_NO());
        paramMap.put("ORDER_MODE", 1);
        paramMap.put("FROM_CIRCSITE_ID", "302");
        paramMap.put("TO_CIRCSITE_ID", addOutRecord.getTO_CIRCSITE_ID());
        paramMap.put("TRACECODELIST", addOutRecord.getTRACECODELIST());
        paramMap.put("ACCESS_UUID", addOutRecord.getACCESS_UUID());

        String json = JsonUtils.objectToJson(paramMap);
        log.info("------------上传连洋系统溯源码入参..." + json);
        String response = HttpRequest.post(addOutOrderRecordUrl, json, traceSourceToken);
        log.info("------------上传连洋系统溯源码,orderId:{},响应结果:{}...", addOutRecord.getORDER_NO(), response);
        Map<String, Object> stringObjectMap = JSONUtil.json2map(response);
        return String.valueOf(stringObjectMap.get("STATUS"));

    }

    private String lyLoginIn(String A, String username, String password) {
        String traceSourceToken = RedisUtil.get("traceSourceToken");
        if (traceSourceToken == null) {
            LyLoginParam loginParam = new LyLoginParam();
            loginParam.setUsername(username);
            password = encryptThisString(password);
            loginParam.setPassword(password);
            loginParam.setApp("04");
            loginParam.setA(A);
            traceSourceToken = HttpRequest.login(loginUrl + "?A=" + A + "&username=" + username + "&password=" + password + "&app=04", JsonUtils.objectToJson(loginParam));
            if (!StringUtils.isEmpty(traceSourceToken)) {
                RedisUtil.set("traceSourceToken", traceSourceToken, 60 * 60);
            }
        }
        return traceSourceToken;
    }

    private static String encryptThisString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            byte[] messageDigest = md.digest(input.getBytes());

            BigInteger no = new BigInteger(1, messageDigest);

            StringBuilder hashtext = new StringBuilder(no.toString(16));

            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");//hashtext = "0" + hashtext
            }
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSecurityNumber(String serialNumber) {
        String traceSourceToken = lyLoginIn(A, username, password);
        Map<String, Object> paramMap = new TreeMap<>();
        paramMap.put("TRACECODE_ID", serialNumber);
        String json = JsonUtils.objectToJson(paramMap);
        String response = HttpRequest.post("https://sy.uni-m.com:85/serversys/tracecode/api/getCodeInfo.do", json, traceSourceToken);
        Map<String, Object> stringObjectMap = JSONUtil.json2map(response);
        Integer status = (Integer) stringObjectMap.get("STATUS");
        if (status == 0) {
            Object data = stringObjectMap.get("DATA");
            Map<String, Object> map = JSONUtil.json2map(JSONUtil.obj2json(data));
            Object code_list = map.get("CODE_LIST");
            JSONArray objects = JSONArray.parseArray(JSONUtil.obj2json(code_list));
            Object o = objects.get(0);
            Map<String, Object> json2map = JSONUtil.json2map(JSONUtil.obj2json(o));
            return (String) json2map.get("TRACECODE_ID");
        }
        return null;
    }


    public static String addOutRecordTest() {
        LyLoginParam loginParam = new LyLoginParam();
        loginParam.setUsername("datauser");
        String password = encryptThisString("data2224user,./");
        loginParam.setPassword(password);
        loginParam.setApp("04");
        loginParam.setA("mft");
        String traceSourceToken = HttpRequest.login("https://sy.uni-m.com:85/serversys/ajaxLogin?A=" + loginParam.getA() + "&username=" + loginParam.getUsername() + "&password=" + password + "&app=04", JsonUtils.objectToJson(loginParam));

        Map<String, Object> map = new TreeMap<>();
        map.put("ORDER_NO", Long.parseLong("1096925295351803903"));
        map.put("FROM_CIRCSITE_ID", "302");
        map.put("FROM_CIRCSITE_NAME", "京东");
        map.put("TO_CIRCSITE_ID", "1096925295351803903");
        map.put("TO_CIRCSITE_NAME", "白玉兰广场");
        map.put("AMOUNT", Double.parseDouble("7"));
        map.put("FREIGHT", Double.parseDouble("7"));
        map.put("TO_CIRCSITE_NO", "1096925295351803903");
        map.put("Name", "邓明杰");
        map.put("Tel", "17749769907");
        map.put("ProvinceName", "上海");
        map.put("CityName", "上海市");
        map.put("ExpAreaName", "城区");
        map.put("Address", "上海白玉兰广场2903室");


        List<Map<String, Object>> lists = new ArrayList<>();

        Map<String, Object> map1 = new TreeMap<>();
        map1.put("ORDER_DETAIL_NO", "1");
        map1.put("PRODUCT_ID", "2");
        map1.put("PRODUCT_NAME", "美浮特皮肤抗菌液盒装");
        map1.put("PRODUCT_NO", "30010010");
        map1.put("SPEC", "1");
        map1.put("WEIGHT", "0.1");
        map1.put("VOLUME", "0.3");
        map1.put("UNIT", "盒");
        map1.put("COUNT", 1);

        Map<String, Object> map2 = new TreeMap<>();
        map2.put("ORDER_DETAIL_NO", "2");
        map2.put("PRODUCT_ID", "2");
        map2.put("PRODUCT_NAME", "美浮特皮肤抗菌液");
        map2.put("PRODUCT_NO", "C034-H");
        map2.put("SPEC", "1");
        map2.put("WEIGHT", "0.1");
        map2.put("VOLUME", "0.3");
        map2.put("UNIT", "盒");
        map2.put("COUNT", 1);

        lists.add(map1);
        lists.add(map2);

        map.put("DETAIL_LIST", lists);

        map.put("ACCESS_UUID", "1233333");

        log.info("------------参数:{}", JsonUtils.objectToJson(map));

        String response = HttpRequest.post("https://sy.uni-m.com:85/serversys/circOrder/api/addOutOrder.do", "{\"ACCESS_UUID\":\"1272403068359094272\",\"AMOUNT\":147.0,\"Address\":\"上海 虹口区 城区白玉兰广场2903\",\"CityName\":\"虹口区\",\"DETAIL_LIST\":[{\"COUNT\":2,\"ORDER_DETAIL_NO\":\"1272380999491710976\",\"PRODUCT_ID\":\"1272380999491710976\",\"PRODUCT_NAME\":\"美浮特皮肤抗菌液(盒)\",\"PRODUCT_NO\":\"30010010\",\"SPEC\":\"1\",\"UNIT\":\"盒\",\"VOLUME\":0.1,\"WEIGHT\":0.5}],\"ExpAreaName\":\"城区\",\"FREIGHT\":7.0,\"FROM_CIRCSITE_ID\":\"302\",\"FROM_CIRCSITE_NAME\":\"京东\",\"Name\":\"吴小彪\",\"ORDER_NO\":1098346178452492289,\"ProvinceName\":\"上海\",\"TO_CIRCSITE_ID\":\"367544397757153281\",\"TO_CIRCSITE_NAME\":\"吴小彪\",\"TO_CIRCSITE_NO\":\"367544397757153281\",\"Tel\":\"17621938801\"}", traceSourceToken);

        log.info("------------出库任务单创建..." + response);

        Map<String, Object> paramMap = new TreeMap<>();
        paramMap.put("ORDER_NO", "1096925295351803904");
        paramMap.put("ORDER_DETAIL_NO", "1096925295351803902");
        paramMap.put("FROM_CIRCSITE_ID", "302");
        paramMap.put("TO_CIRCSITE_ID", "1026925295351803911");
        paramMap.put("TRACECODELIST", Arrays.asList("0000000011235485"));
        paramMap.put("ACCESS_UUID", "1096925295351803904");

        String json = JsonUtils.objectToJson(paramMap);

        String response1 = HttpRequest.post("https://sy.uni-m.com:85/serversys/circrecord/api/addOutstock.do", json, traceSourceToken);
        log.info("------------上传连洋系统溯源码..." + response1);
        Map<String, Object> stringObjectMap = JSONUtil.json2map(response1);
        return String.valueOf(stringObjectMap.get("STATUS"));
    }




    public static void getSecurityNumberTest() {

        String traceSourceToken = "shw.session.id=4179ca77430c4512b9104fa5798c932d";
        Map<String, Object> paramMap = new TreeMap<>();
        paramMap.put("TRACECODE_ID", "L20570244145");
        String json = JsonUtils.objectToJson(paramMap);
        String response1 = HttpRequest.post("https://sy.uni-m.com:85/serversys/tracecode/api/getCodeInfo.do", json, traceSourceToken);
        System.out.println(response1);

        Map<String, Object> stringObjectMap = JSONUtil.json2map(response1);

        Integer status = (Integer) stringObjectMap.get("STATUS");

        if (status == 0) {
            Object data = stringObjectMap.get("DATA");
            Map<String, Object> map = JSONUtil.json2map(JSONUtil.obj2json(data));
            System.out.println(map);
            Object code_list = map.get("CODE_LIST");
            System.out.println(code_list);

            JSONArray objects = JSONArray.parseArray(JSONUtil.obj2json(code_list));
            System.out.println(objects.get(0));
            Object o = objects.get(0);

            Map<String, Object> json2map = JSONUtil.json2map(JSONUtil.obj2json(o));
            String tracecode_id = (String) json2map.get("TRACECODE_ID");
            System.out.println(tracecode_id);
        }
    }

    public static void main(String[] args) {
        getSecurityNumberTest();
    }

}
