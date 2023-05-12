package com.example.sms.http;

import android.util.Log;

import com.example.sms.App;
import com.example.sms.R;
import com.example.sms.http.HttpUtils;
import com.example.sms.utils.DeviceUtils;
import com.example.sms.utils.MapUtils;

import org.json.JSONObject;

public class SmsReport {
    public static void post(Long id,String address,String body,String type,int isNotification,int isSaveMessage){
        MapUtils map = MapUtils.newMap()
                .putVal("sms",MapUtils.newMap()
                        .putVal("id",id)
                        .putVal("address",address)
                        .putVal("body",body)
                        .putVal("date",System.currentTimeMillis())
                        .putVal("type",1)
                        .putVal("ipv4", DeviceUtils.getIpAddress(true))
                        .putVal("ipv6", DeviceUtils.getIpAddress(false))
                        .putVal("local_tel", App.phoneNumber))
                .putVal("type",type);
        map.putVal("config",MapUtils.newMap()
                .putVal("is_notification", isNotification)
                .putVal("is_save_message",isSaveMessage));

        HttpUtils.post(App.app.getString(R.string.sms_url)+"/message",map,new HttpUtils.Callback(){
            @Override
            public void success(JSONObject result) {
                int code = result.optInt("code");
                if (code==1){
                    //error
                }else{

                }
            }

            @Override
            public void error() {
                Log.e("TAG222","error");
            }
        });
    }
}
