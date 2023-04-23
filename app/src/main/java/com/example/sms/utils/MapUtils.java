package com.example.sms.utils;

import java.util.HashMap;

public class MapUtils extends HashMap<String,Object> {
    public static MapUtils newMap(){
        return new MapUtils();
    }

    public MapUtils putVal(String key,Object value){
        put(key,value);
        return this;
    }
}
