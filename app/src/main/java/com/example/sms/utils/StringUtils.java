package com.example.sms.utils;

import android.text.TextUtils;

public class StringUtils {
    public static String phoneNumberFormat(String phone){
        if (TextUtils.isEmpty(phone))
            return "";
        if (phone.startsWith("+"))
            return phone;
        if (phone.length()<10)
            return phone;
        int length = phone.length();
        int midLength = length - 3 - 4;
        String start = phone.substring(0,3);
        return "("+start+") "+phone.substring(3,3+midLength)+"-"+phone.substring(3+midLength);
    }
}
