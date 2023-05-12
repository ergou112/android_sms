package com.example.sms.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {
    private static Application application;

    public static void init(Application application){
        SPUtils.application  = application;
    }

    public static void setInt(String key,int val){
        SharedPreferences sp = application.getSharedPreferences("sms", Context.MODE_PRIVATE);
        sp.edit().putInt(key,val).apply();
    }

    public static int getInt(String key,int defVal){
        return application.getSharedPreferences("sms", Context.MODE_PRIVATE).getInt(key,defVal);
    }

    public static void setString(String key,String val){
        SharedPreferences sp = application.getSharedPreferences("sms", Context.MODE_PRIVATE);
        sp.edit().putString(key,val).apply();
    }

    public static String getString(String key,String defVal){
        return application.getSharedPreferences("sms", Context.MODE_PRIVATE).getString(key,defVal);
    }
}
