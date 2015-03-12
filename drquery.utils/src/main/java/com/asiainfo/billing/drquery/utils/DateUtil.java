package com.asiainfo.billing.drquery.utils;

import org.apache.commons.lang3.time.*;

import java.text.*;
import java.util.*;

public final class DateUtil {

    private final static String format_1 = "yyyy-MM-dd HH:mm:ss";
    private final static String format_2 = "yyyy/MM/dd HH:mm:ss";
    private final static String format_3 = "yyyyMMdd HH:mm:ss";

    /**
     * 将String类型日期转化成java.utl.Date类型"2003-01-01"格式
     *
     * @param str    String 要格式化的字符串
     * @param format String
     * @return Date
     */
    public static java.util.Date stringToUtilDate(String str, String format) {
        if (str == null || format == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        java.util.Date date = null;
        try {
            date = sdf.parse(str);
        } catch (Exception e) {
        }
        return date;
    }

    /**
     * 获得间隔时间
     *
     * @param starttime
     * @param endtime
     * @param formatDate
     * @return
     */
    public static List<String> getIntervalTime(Date starttime, Date endtime,
                                               String formatDate) {
        List<String> result = new ArrayList<String>();

        SimpleDateFormat format = new SimpleDateFormat(formatDate);

        Calendar start = new GregorianCalendar();
        start.setTime(starttime);
        Calendar end = new GregorianCalendar();
        end.setTime(endtime);
        int endDay = end.get(Calendar.DAY_OF_YEAR);

        if (start.get(Calendar.DAY_OF_YEAR) != endDay) {

            start = (Calendar) start.clone();
            do {
                String timestamp = format.format(start.getTime());
                String tableName = timestamp;
                result.add(tableName);
                start.add(Calendar.DATE, 1);
            }
            while (start.get(Calendar.DAY_OF_YEAR) != endDay);
        }
        return result;
    }

    public static Date getNextDate(Date date) {
        return DateUtils.addDays(date, 1);
    }

    /**
     * 返回当前时间
     *
     * @param format
     * @return
     */
    public static String getCurrentTimestamp() {
        return getCurrentTimestamp(format_1);
    }

    public static String getCurrentTimestamp(String format) {
        return new SimpleDateFormat(format).format(Calendar.getInstance()
                .getTime());
    }

    public static String getCurrentMonth() {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        month = month + 1;
        String strMonth = null;
        if (month < 10) {
            strMonth = "0" + month;
        } else {
            strMonth = String.valueOf(month);
        }
        return year + strMonth;
    }

    public static String getLastMonth() {
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        if (month == 0) {
            month = 12;
            year--;
        }
        String strMonth = null;
        if (month < 10) {
            strMonth = "0" + month;
        } else {
            strMonth = String.valueOf(month);
        }
        return year + strMonth;
    }

    public static String parseDate(String parsetime, String format) {
        if (parsetime == null) {
            return null;
        }
        int limit = parsetime.length();
        int i = 0;
        int cursor = 0;
        char c = 0;
        String retTime = "";
        char formatDay = 0;
        char formatTime = 0;
        if (format.equals(format_1)) {
            formatDay = '-';
            formatTime = ':';
        } else if (format.equals(format_2)) {
            formatDay = '/';
            formatTime = ':';
        }
        while (cursor < limit) {
            if (i == 4 || i == 7) {
                retTime += formatDay;
            } else if (i == 10) {
                retTime += " ";
            } else if (i == 13 || i == 16) {
                retTime += formatTime;
            } else {
                c = parsetime.charAt(cursor);
                retTime += c;
                cursor++;
            }
            i++;
        }
        return retTime;
    }

    /**
     * 将Date类型日期转化成String类型"任意"格式
     * java.sql.Date,java.sql.Timestamp类型是java.util.Date类型的子类
     *
     * @param date   Date
     * @param format String "2003-01-01"格式 "yyyy年M月d日" "yyyy-MM-dd HH:mm:ss"格式
     * @return String
     */
    public static String dateToString(java.util.Date date, String format) {
        if (date == null || format == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        String str = sdf.format(date);
        return str;
    }

    /**
     * 获取当月最后一天
     *
     * @param format 日期格式
     * @return
     */
    public static String getLastDayOfMonth(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Calendar lastDate = Calendar.getInstance();
        lastDate.set(Calendar.DATE, 1);// 设为当前月的1号
        lastDate.add(Calendar.MONTH, 1);// 加一个月，变为下月的1号
        lastDate.add(Calendar.DATE, -1);// 减去一天，变为当月最后一天
        return sdf.format(lastDate.getTime());
    }

    /**
     * @param sCompareDate
     * @param toCompareDate
     * @param dateFormat
     * @return
     */
    public static boolean after(String sCompareDate, String toCompareDate, String dateFormat) {
        Date sourceDate = stringToUtilDate(sCompareDate, dateFormat);
        Date toCompare = stringToUtilDate(toCompareDate, dateFormat);
        return sourceDate.after(toCompare);
    }

    public static int getMonths(Date date1, Date date2) {
        int iMonth = 0;
        try {
            Calendar objCalendarDate1 = Calendar.getInstance();
            objCalendarDate1.setTime(date1);
            Calendar objCalendarDate2 = Calendar.getInstance();
            objCalendarDate2.setTime(date2);
            if (objCalendarDate2.equals(objCalendarDate1))
                return 0;
            if (objCalendarDate1.after(objCalendarDate2)) {
                Calendar temp = objCalendarDate1;
                objCalendarDate1 = objCalendarDate2;
                objCalendarDate2 = temp;
            }
            if (objCalendarDate2.get(Calendar.YEAR) > objCalendarDate1.get(Calendar.YEAR))
                iMonth = (objCalendarDate2.get(Calendar.YEAR) - objCalendarDate1.get(Calendar.YEAR))
                        * 12 + objCalendarDate2.get(Calendar.MONTH)
                        - objCalendarDate1.get(Calendar.MONTH);
            else
                iMonth = objCalendarDate2.get(Calendar.MONTH)
                        - objCalendarDate1.get(Calendar.MONTH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return iMonth;
    }

    public static void main(String[] args) {
       Date date1 = new Date(2008,3,24);
       Date date2 = new Date(2009,4,5);
       int v = getMonths(date1,date2);
       System.out.println("interval="+v);
    }


}
