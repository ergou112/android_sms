package com.example.sms.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {
    public static String msgDateFormat(Long date){
        long now = System.currentTimeMillis();
        long timeGap = now-date;
        if (timeGap<60*1000){
            return "just now";
        }else if(timeGap<3*60*1000){
            return timeGap/60000 + " min";
        }else if(date>=getHourTime()){
            return new SimpleDateFormat("mm:ss").format(date);
        }else if(date>=getTodayTime()){
            return new SimpleDateFormat("EEE H:mm:ss").format(date);
        }else{
            return  new SimpleDateFormat("MMM/d H:mm:ss").format(date);
        }
    }
    public static long getHourTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }
    public static long getTodayTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis();
    }
}
