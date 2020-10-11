package com.meifute.core.util.soapUtil;

import com.meifute.core.model.jiayisubmitorder.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JaxbXmlUtil {

    public static final String DEFAULT_ENCODING = "UTF-8";

    /**
     * pojo转换成xml 默认编码UTF-8
     *
     * @param obj 待转化的对象
     * @return xml格式字符串
     * @throws Exception JAXBException
     */
    public static String convertToXml(Object obj) {
        return convertToXml(obj, DEFAULT_ENCODING);
    }

    /**
     * pojo转换成xml
     *
     * @param obj      待转化的对象
     * @param encoding 编码
     * @return xml格式字符串
     * @throws Exception JAXBException
     */
    public static String convertToXml(Object obj, String encoding) {
        String result = null;
        StringWriter writer = null;
        try {
            writer = new StringWriter();
            JAXBContext context = JAXBContext.newInstance(obj.getClass());
            Marshaller marshaller = context.createMarshaller();
            // 指定是否使用换行和缩排对已编组 XML 数据进行格式化的属性名称。
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
            marshaller.marshal(obj, writer);
            result = writer.toString();
        } catch (JAXBException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * xml转换成JavaBean
     *
     * @param xml xml格式字符串
     * @param t   待转化的对象
     * @return 转化后的对象
     * @throws Exception JAXBException
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertToJavaBean(String xml, Class<T> t) throws Exception {
        T obj = null;
        JAXBContext context = JAXBContext.newInstance(t);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        obj = (T) unmarshaller.unmarshal(new StringReader(xml));
        return obj;
    }

    private static String uri = "http://b2c.a56.net/webservice/b2c.asmx";

    public static void main(String[] args) throws Exception {

        RequestSubmitOrder requestSubmitOrder = new RequestSubmitOrder();
        SubmitOrderBody submitOrderBody = new SubmitOrderBody();

        SubmitOrder submitOrder = new SubmitOrder();

        AsnOut asnOut = new AsnOut();

        asnOut.setWarehouseId(1);
        asnOut.setProjectId(8);
        asnOut.setPayTime(new Date());
        asnOut.setCarrier("顺丰");
        asnOut.setShipper_Person("美浮特");
        asnOut.setCon_City("上海");
        asnOut.setCon_Address("白玉兰");
        asnOut.setCon_Person("王晓");
        asnOut.setCon_Tel("1234567890");
        asnOut.setCon_Mobile("123456789");
        asnOut.setDN("1004147776528580611");
//        asnOut.setMemo("测试啊");

        CargoOutList cargoOutList = new CargoOutList();
        List<CargoOut> list = new ArrayList<>();

        CargoOut cargoOut = new CargoOut();
        cargoOut.setItemNo("C035-H");
        cargoOut.setQty(BigDecimal.ONE);
//        cargoOut.setMemo("测试");
        list.add(cargoOut);
        cargoOutList.setCargoOutList(list);

        submitOrder.setAsnOut(asnOut);
        submitOrder.setCargoOutList(cargoOutList);
        submitOrder.setKey("ad1334179c5b5d0fa564ae08024d5058f39fb3e9");

        submitOrderBody.setSubmitOrder(submitOrder);
        requestSubmitOrder.setBody(submitOrderBody);
        String s = convertToXml(requestSubmitOrder);
        System.out.println(s);


        String s1 = SoapUtil.sendPost(s, uri);
        System.out.println(s1);
//        SoapUtil.formatSoap("1004147776528580611",s1, 0);


//        System.out.println(submitOrderResponse);

    }


}
