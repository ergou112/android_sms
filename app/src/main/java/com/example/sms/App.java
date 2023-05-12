package com.example.sms;

import android.app.Application;

import androidx.multidex.MultiDex;

import com.example.sms.utils.SPUtils;
import com.zy.devicelibrary.UtilsApp;

import org.xutils.x;

import java.util.HashMap;

public class App extends Application {
    public static Application app;
    public static String phoneNumber = "";
    public static String android_id = "";
    public static String gaid = "";
    public static String mac = "";
    public static String imei = "";
    public static String ipv4 = "";

    public static HashMap<String,Object> deviceInfo;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        SPUtils.init(this);
        UtilsApp.init(this);
        MultiDex.install(this);
        x.Ext.init(this);
    }
}
