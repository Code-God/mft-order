package com.meifute.core.util;

import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @Auther: wll
 * @Date: 2019/1/25 23:22
 * @Auto: I AM A CODE MAN !
 * @Description:
 */
@Configuration
public class DateUtil {

    /**
     * 获取月内的第一天日期  本月为0
     * @param month
     * @return
     */
    public static Date getFristOfMonth(Integer month) {
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, month);
        cale.set(Calendar.DAY_OF_MONTH, 1);
        Date firstday = cale.getTime();
        cale.setTime(firstday);
        cale.set(Calendar.HOUR_OF_DAY, 0);
        cale.set(Calendar.MINUTE, 0);
        cale.set(Calendar.SECOND, 0);
        return cale.getTime();
    }


    public static Date getLastOfMonth(Integer month) {
        Calendar cale = Calendar.getInstance();
        cale.add(Calendar.MONTH, month);
        cale.set(Calendar.DAY_OF_MONTH, 0);

        Date firstday = cale.getTime();

        cale.setTime(firstday);
        cale.set(Calendar.HOUR_OF_DAY, 0);
        cale.set(Calendar.MINUTE, 0);
        cale.set(Calendar.SECOND, 0);
        cale.add(Calendar.DATE,1);

        return cale.getTime();
    }




    public static void main(String[] args) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println(format.format(getLastOfMonth(1)));
            System.out.println(format.format(getFristOfMonth(0)));
    }



}
