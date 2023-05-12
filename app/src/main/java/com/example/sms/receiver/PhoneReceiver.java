package com.example.sms.receiver;

import static android.content.Context.TELEPHONY_SERVICE;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.sms.service.PhoneCallService;

public class PhoneReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("action"+intent.getAction());
        //如果是去电
        if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)){
            String phoneNumber = intent
                    .getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }else{

        }
    }
}
