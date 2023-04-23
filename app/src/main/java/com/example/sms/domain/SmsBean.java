package com.example.sms.domain;

public class SmsBean {
    public Long id;
    public String address;
    public int person;
    public String body;
    public Long date;
    public int type;
    public int read;

    public int status;

    @Override
    public String toString() {
        return "SmsBean{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", person=" + person +
                ", body='" + body + '\'' +
                ", date=" + date +
                ", type=" + type +
                ", read=" + read +
                ", status=" + status +
                '}';
    }
}
