package com.example.sms.event;

import android.content.BroadcastReceiver;

public class SendSmsEvent extends BaseEvent{
    public BroadcastReceiver receiver;
    public SendSmsEvent(BroadcastReceiver receiver){
        this.receiver = receiver;
    }
}
