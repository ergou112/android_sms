package com.example.sms.utils;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.sms.R;
import com.example.sms.ui.SmsActivity;

public class NotificationUtils {
    public static void createNotificationForNormal(Context context,String title,String msg) {
        NotificationManager mManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        // 适配8.0及以上 创建渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(context.getString(R.string.app_name), context.getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.app_name));
            channel.setShowBadge(true);
            mManager.createNotificationChannel(channel);
        }
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        // 设置通知的提示音
        // 点击意图 // setDeleteIntent 移除意图
        Intent intent = new Intent(context, SmsActivity.class);
        intent.putExtra("address",title);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        // 构建配置
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.app_name))
                .setContentTitle(title)
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_notification) // 小图标
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_notification)) // 大图标
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(alarmSound)
                .setContentIntent(pendingIntent) // 跳转配置
                .addAction(new NotificationCompat.Action(R.mipmap.ic_launcher,"Reply",pendingIntent))
                .setAutoCancel(true);
        mManager.notify(3, builder.build());





    }

}
