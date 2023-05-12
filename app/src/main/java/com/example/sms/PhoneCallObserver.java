package com.example.sms;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.List;

public class PhoneCallObserver extends ContentObserver {
    public PhoneCallObserver(Handler handler) {
        super(handler);
    }
    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange);
        Log.e("phone uri",uri.toString());
    }
}
