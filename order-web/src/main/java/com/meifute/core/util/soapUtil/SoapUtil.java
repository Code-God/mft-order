package com.meifute.core.util.soapUtil;


import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.model.jiayicancelorder.CancelOrderResponse;
import com.meifute.core.model.jiayicheckoutbound.CheckOutboundResponse;
import com.meifute.core.model.jiayisubmitorder.SubmitOrderResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Slf4j
public class SoapUtil {

    public static String formatSoap(String deptXML, int tag) {
        String expressCode = null;
        switch (tag) {
            case 0:
                SubmitOrderResponse submitOrderResponse = formatSubmitSoap(deptXML);
                if (!submitOrderResponse.getSubmitOrderResult()) {
                    throw new MallException("020038", new Object[]{submitOrderResponse.getResult()});
                }
                break;
            case 1:
                CancelOrderResponse cancelOrderResponse = formatCancelSoap(deptXML);
                if (!cancelOrderResponse.getCancelOrderResult()) {
                    throw new MallException("020039", new Object[]{cancelOrderResponse.getResult()});
                }
                break;
            case 2:
                CheckOutboundResponse checkOutboundResponse = formatCheckOutboundSoap(deptXML);
                if (!checkOutboundResponse.getCheckOutboundResult()) {
                    return null;
                }
                expressCode = checkOutboundResponse.getResult();
                break;
        }
        return expressCode;
    }

    public static SubmitOrderResponse formatSubmitSoap(String deptXML) {
        SubmitOrderResponse submitOrderResponse = new SubmitOrderResponse();
        try {
            SOAPMessage msg = formatSoapString(deptXML);
            SOAPBody body = msg.getSOAPBody();
            Iterator<SOAPElement> iterator = body.getChildElements();
            printSubmitBody(submitOrderResponse, iterator);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return submitOrderResponse;
    }

    public static CancelOrderResponse formatCancelSoap(String deptXML) {
        CancelOrderResponse cancelOrderResponse = new CancelOrderResponse();
        try {
            SOAPMessage msg = formatSoapString(deptXML);
            SOAPBody body = msg.getSOAPBody();
            Iterator<SOAPElement> iterator = body.getChildElements();
            printCancelBody(cancelOrderResponse, iterator);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cancelOrderResponse;
    }


    public static CheckOutboundResponse formatCheckOutboundSoap(String deptXML) {
        CheckOutboundResponse checkOutboundResponse = new CheckOutboundResponse();
        try {
            SOAPMessage msg = formatSoapString(deptXML);
            SOAPBody body = msg.getSOAPBody();
            Iterator<SOAPElement> iterator = body.getChildElements();
            printCheckOutboundBody(checkOutboundResponse, iterator);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return checkOutboundResponse;
    }

    /**
     * 把soap字符串格式化为SOAPMessage
     *
     * @param soapString
     * @return
     * @see [类、类#方法、类#成员]
     */
    private static SOAPMessage formatSoapString(String soapString) {
        MessageFactory msgFactory;
        try {
            msgFactory = MessageFactory.newInstance();
            SOAPMessage reqMsg = msgFactory.createMessage(new MimeHeaders(),
                    new ByteArrayInputStream(soapString.getBytes(StandardCharsets.UTF_8)));
            reqMsg.saveChanges();
            return reqMsg;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void printSubmitBody(SubmitOrderResponse submitOrderResponse, Iterator<SOAPElement> iterator) {
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o != null) {
                SOAPElement element = null;
                try {
                    element = (SOAPElement) o;
                    log.info("佳一推单:" + element.getNodeName() + "Value:" + element.getValue());
                    if ("SubmitOrderResult".equals(element.getNodeName().trim())) {
                        submitOrderResponse.setSubmitOrderResult(Boolean.valueOf(element.getValue().trim()));
                    }
                    if ("result".equals(element.getNodeName().trim())) {
                        submitOrderResponse.setResult(element.getValue().trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (element != null) {
                    printSubmitBody(submitOrderResponse, element.getChildElements());
                }
            }
        }
    }

    private static void printCancelBody(CancelOrderResponse cancelOrderResponse, Iterator<SOAPElement> iterator) {
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o != null) {
                SOAPElement element = null;
                try {
                    element = (SOAPElement) o;
                    log.info("佳一关闭订单:" + element.getNodeName() + "Value:" + element.getValue());
                    if ("CancelOrderResult".equals(element.getNodeName().trim())) {
                        cancelOrderResponse.setCancelOrderResult(Boolean.valueOf(element.getValue().trim()));
                    }
                    if ("result".equals(element.getNodeName().trim())) {
                        cancelOrderResponse.setResult(element.getValue().trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (element != null) {
                    printCancelBody(cancelOrderResponse, element.getChildElements());
                }
            }
        }
    }

    private static void printCheckOutboundBody(CheckOutboundResponse checkOutboundResponse, Iterator<SOAPElement> iterator) {
        while (iterator.hasNext()) {
            Object o = iterator.next();
            if (o != null) {
                SOAPElement element = null;
                try {
                    element = (SOAPElement) o;
                    log.info("佳一获取物流单号:" + element.getNodeName() + "Value:" + element.getValue());
                    if ("CheckOutboundResult".equals(element.getNodeName().trim())) {
                        checkOutboundResponse.setCheckOutboundResult(Boolean.valueOf(element.getValue().trim()));
                    }
                    if ("result".equals(element.getNodeName().trim())) {
                        checkOutboundResponse.setResult(element.getValue().trim());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (element != null) {
                    printCheckOutboundBody(checkOutboundResponse, element.getChildElements());
                }
            }
        }
    }

    public static String sendPost(String params, String requestUrl) {
        HttpClient httpClient = new HttpClient();// 客户端实例化
        PostMethod postMethod = new PostMethod(requestUrl);
        String result = null;
        try {
            byte[] requestBytes = params.getBytes("utf-8"); // 将参数转为二进制流
            // 设置请求头  Content-Type
            postMethod.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
            InputStream inputStream = new ByteArrayInputStream(requestBytes, 0, requestBytes.length);
            RequestEntity requestEntity = new InputStreamRequestEntity(inputStream, requestBytes.length, "text/xml; charset=utf-8"); // 请求体
            postMethod.setRequestEntity(requestEntity);
            httpClient.executeMethod(postMethod);// 执行请求
            InputStream soapResponseStream = postMethod.getResponseBodyAsStream();// 获取返回的流

            byte[] datas = readInputStream(soapResponseStream);// 从输入流中读取数据

            result = new String(datas, "UTF-8");// 将二进制流转为String
            // 打印返回结果
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            postMethod.releaseConnection();
        }
        return result;
    }

    /**
     * 从输入流中读取数据
     *
     * @param inStream
     * @return
     * @throws Exception
     */
    public static byte[] readInputStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = inStream.read(buffer)) != -1) {
            outStream.write(buffer, 0, len);
        }
        byte[] data = outStream.toByteArray();
        outStream.close();
        inStream.close();
        return data;
    }
}
