package com.example.sms.receiver;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.sms.domain.SmsBean;
import com.example.sms.event.SendSmsEvent;
import com.example.sms.http.SmsReport;

import org.greenrobot.eventbus.EventBus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

public class SendSmsReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        String[] split = action.split(":");
        long id = Long.parseLong(split[1]);
        int status = 0;
        /* android.content.BroadcastReceiver.getResultCode()方法 */
        switch(getResultCode())
        {
            case Activity.RESULT_OK:
                status = 0;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                /* 发送短信失败 */
                status = 128;
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                status = 128;
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                status = 128;
                break;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("status",status);
        context.getContentResolver().update(Uri.parse("content://sms/"), contentValues, "_id="+id, null);
        report(context,id);
        EventBus.getDefault().post(new SendSmsEvent(this));
    }

    public void report(Context context,long id) {
        final String SMS_URI_ALL = "content://sms/"; // 所有短信
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person",
                    "body", "date", "type", "read", "status"};
            Cursor cur = context.getContentResolver().query(uri, projection, "_id="+id,
                    null, null); // 获取手机内部短信
            if (cur.moveToFirst()) {
                int id_Address = cur.getColumnIndex("_id");
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                int index_Read = cur.getColumnIndex("read");
                int index_Status = cur.getColumnIndex("status");

                do {
                    Long _id = cur.getLong(id_Address);
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    int read = cur.getInt(index_Read);
                    int status = cur.getInt(index_Status);

                    SmsBean smsBean = new SmsBean();
                    smsBean.id =_id;
                    smsBean.address =strAddress;
                    smsBean.person =intPerson;
                    smsBean.body =strbody;
                    smsBean.date =longDate;
                    smsBean.type =intType;
                    smsBean.read =read;
                    smsBean.status =status;
                    SmsReport.post(_id,strAddress,strbody,"add");
                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } else {
            }

        } catch (SQLiteException ex) {
        }
    }

}