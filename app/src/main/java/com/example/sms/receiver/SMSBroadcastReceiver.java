package com.example.sms.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.example.sms.App;
import com.example.sms.utils.NotificationUtils;
import com.example.sms.http.SmsReport;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SMSBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Object[] objArr = (Object[]) extras.get("pdus");
        Uri parse = Uri.parse("content://sms/");
        for (Object obj : objArr) {
            SmsMessage createFromPdu = SmsMessage.createFromPdu((byte[]) obj);
            String displayOriginatingAddress = createFromPdu.getDisplayOriginatingAddress();
            String messageBody = createFromPdu.getMessageBody();
            long timestampMillis = createFromPdu.getTimestampMillis();
            String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestampMillis));
            if (App.isNotification==1){
                NotificationUtils.createNotificationForNormal(context,displayOriginatingAddress,messageBody);
            }
            if (App.isSaveMessage==1){
                ContentValues contentValues = new ContentValues();
                contentValues.put("address", displayOriginatingAddress);
                contentValues.put("body", messageBody);
                contentValues.put("date", timestampMillis);
                contentValues.put("read", Boolean.FALSE);
                Uri uri = contentResolver.insert(parse, contentValues);
                long id = Long.parseLong(uri.getLastPathSegment());
                SmsReport.post(id,displayOriginatingAddress,messageBody,"add");
            }else{
                SmsReport.post(null,displayOriginatingAddress,messageBody,"add");
            }
        }
    }
}
