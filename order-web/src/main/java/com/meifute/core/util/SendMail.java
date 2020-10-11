package com.meifute.core.util;

import com.aliyun.openservices.shade.org.apache.commons.lang3.StringUtils;
import com.aliyun.oss.internal.OSSUtils;
import com.meifute.core.mmall.common.utils.OSSClientUtil;
import com.sun.mail.util.MailSSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Properties;

/**
 * @program: m-mall-order
 * @description: 发送邮件
 * @author: Mr.Wang
 * @create: 2020-06-11 10:22
 **/
@Slf4j
public class SendMail {

    /**
     * 发送带附件的邮件
     *
     * @param receive  收件人
     * @param subject  邮件主题
     * @param msg      邮件内容
     * @param filename 附件地址
     * @return
     * @throws GeneralSecurityException
     */
    public static boolean sendMail(InternetAddress[] receive, String subject, String msg, InputStream filename, InputStream filename2)
            throws GeneralSecurityException {

        // 发件人电子邮箱
        final String from = "wzpeng_999@163.com";
        // 发件人电子邮箱密码
        final String pass = "RPIRPRXESSUEPXTZ";

        // 指定发送邮件的主机为 smtp.qq.com
        String host = "smtp.163.com"; // 邮件服务器

        // 获取系统属性
        Properties properties = System.getProperties();

        // 设置邮件服务器
        properties.setProperty("mail.smtp.host", host);

        properties.put("mail.smtp.auth", "true");
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.socketFactory", sf);
        // 获取默认session对象
        Session session = Session.getDefaultInstance(properties, new Authenticator() {
            public PasswordAuthentication getPasswordAuthentication() { // qq邮箱服务器账户、第三方登录授权码
                return new PasswordAuthentication(from, pass); // 发件人邮件用户名、密码
            }
        });

        try {
            // 创建默认的 MimeMessage 对象
            MimeMessage message = new MimeMessage(session);

            // Set From: 头部头字段
            message.setFrom(new InternetAddress(from));

            // Set To: 头部头字段
            message.addRecipients(Message.RecipientType.TO, receive);

            // Set Subject: 主题文字
            message.setSubject(subject);

            // 创建消息部分
            BodyPart messageBodyPart = new MimeBodyPart();

            // 消息
            messageBodyPart.setText(msg);

            // 创建多重消息
            Multipart multipart = new MimeMultipart();

            // 设置文本消息部分
            multipart.addBodyPart(messageBodyPart);

            setMessageBodyPart(messageBodyPart, filename, multipart, "提货明细表.xlsx");//附件1
            setMessageBodyPart(messageBodyPart, filename2, multipart, "稽查表.xlsx");//附件2

            // 发送完整消息
            message.setContent(multipart);

            // 发送消息
            Transport.send(message);
            // System.out.println("Sent message successfully....");
            return true;
        } catch (MessagingException e) {
            log.info("邮件发送失败:{}", e.toString());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            log.info("邮件发送失败:{}", e.toString());
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void setMessageBodyPart(BodyPart messageBodyPart, InputStream inputStream, Multipart multipart, String fileName) throws Exception {
        if (null == inputStream)
            return;
        // 附件部分1
        messageBodyPart = new MimeBodyPart();
        // 设置要发送附件的文件路径
//            DataSource source = new FileDataSource(filename);

        DataSource dataSource1 = new ByteArrayDataSource(inputStream, "application/excel");
        messageBodyPart.setDataHandler(new DataHandler(dataSource1));

        // messageBodyPart.setFileName(filename);
        // 处理附件名称中文（附带文件路径）乱码问题
        messageBodyPart.setFileName(MimeUtility.encodeText(fileName));
        multipart.addBodyPart(messageBodyPart);
    }
}
