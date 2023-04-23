package com.example.sms.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.example.sms.App;
import com.example.sms.utils.DeviceUtils;
import com.example.sms.http.HttpUtils;
import com.example.sms.utils.MapUtils;
import com.example.sms.R;
import com.google.gson.Gson;
import com.zy.devicelibrary.data.DeviceInfos;
import com.zy.devicelibrary.data.GeneralData;

import org.json.JSONObject;

import java.util.HashMap;

public class DeviceService extends Service{

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DeviceInfos deviceInfos = new DeviceInfos();
        GeneralData generalData = new GeneralData();
        App.android_id = generalData.and_id;
        App.mac =generalData.mac;
        App.gaid = generalData.gaid;
        App.imei = generalData.imei1;
        App.ipv4 = DeviceUtils.getIpAddress(this);
        Gson gson = new Gson();
        App.deviceInfo = gson.fromJson(gson.toJson(deviceInfos), HashMap.class);
        HttpUtils.post((getString(R.string.sms_url)+"/config"),MapUtils.newMap(),new HttpUtils.Callback(){
            @Override
            public void success(JSONObject result) {

            }

            @Override
            public void error() {

            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}