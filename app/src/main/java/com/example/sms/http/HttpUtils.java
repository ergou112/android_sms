package com.example.sms.http;

import com.example.sms.App;
import com.example.sms.utils.MapUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.http.RequestParams;
import org.xutils.x;

public class HttpUtils {
    public static void post(String url, MapUtils map, Callback callback){
        if (map==null){
            map = MapUtils.newMap();
        }
        map
        .putVal("device_info",App.deviceInfo)
        .putVal("android_id",App.android_id);

        RequestParams params = new RequestParams(url);
        params.setBodyContent(new Gson().toJson(map));
        params.setBodyContentType("application/json");
        x.http().post(params, new org.xutils.common.Callback.CommonCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (callback!=null){
                    try {
                        callback.success(new JSONObject(result));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                }
            }

            @Override
            public void onError(Throwable ex, boolean isOnCallback) {
                if (callback!=null)
                    callback.error();
            }

            @Override
            public void onCancelled(CancelledException cex) {

            }

            @Override
            public void onFinished() {

            }
        });
    }

   public interface Callback{
        public void success(JSONObject result);
        public void error();
    }
}
