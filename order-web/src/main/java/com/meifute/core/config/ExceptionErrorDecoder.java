package com.meifute.core.config;

import com.meifute.core.mmall.common.exception.ExceptionDetail;
import com.meifute.core.mmall.common.exception.MallException;
import com.meifute.core.mmall.common.exception.errorcode.RespCode;
import com.meifute.core.mmall.common.json.JSONUtil;
import com.meifute.core.mmall.common.utils.ObjectUtils;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * @Auther: wxb
 * @Date: 2018/10/16 10:17
 * @Auto: I AM A CODE MAN -_-!
 * @Description: 捕获feign服务端调服务端内部异常
 */
@Configuration
@Slf4j
public class ExceptionErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {

        try {
            if (response.body() != null) {
                String body = Util.toString(response.body().asReader());
                ExceptionDetail exceptionDetail = JSONUtil.json2pojo(body, ExceptionDetail.class);
                if(ObjectUtils.isNullOrEmpty(exceptionDetail) || exceptionDetail.getBaseResponse() == null) {
                    return new Exception(body);
                }
                if (!RespCode.SUCCESS.equals(exceptionDetail.getBaseResponse().getCode())) {
                    return new MallException(exceptionDetail.getBaseResponse().getCode());
                }
            }
        } catch (Exception var4) {
            return new Exception(var4);
        }
        return new MallException(RespCode.SYSTEM_ERROR);
    }

}
