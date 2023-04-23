package com.example.sms.ui;

import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.view.View;

import com.example.sms.MainActivity;
import com.example.sms.R;
import com.example.sms.databinding.ActivitySmsBinding;
import com.example.sms.domain.SmsBean;
import com.example.sms.utils.DateUtils;
import com.gyf.immersionbar.ImmersionBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.bingoogolapple.baseadapter.BGARecyclerViewAdapter;
import cn.bingoogolapple.baseadapter.BGAViewHolderHelper;
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;
import pub.devrel.easypermissions.EasyPermissions;

public class SmsActivity extends BaseActivity implements BGARefreshLayout.BGARefreshLayoutDelegate {

    private List<SmsBean> smsList = new ArrayList<>();
    private HashMap<Integer,String> smsTypeMap = new HashMap();
     {
         smsTypeMap.put(1, "接收");
         smsTypeMap.put(2,"发送");
         smsTypeMap.put(3, "草稿");
         smsTypeMap.put(4,"发件箱");
         smsTypeMap.put(5,"发送失败");
         smsTypeMap.put(6,"待发送列表");
         smsTypeMap.put(0,"所以短信");
    }
    public String[] permissions = {
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.SEND_SMS",
            "android.permission.READ_PHONE_STATE",
            "android.permission.CALL_PHONE",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALL_LOG",
            "android.permission.WRITE_CALL_LOG",
            "android.permission.PROCESS_OUTGOING_CALLS",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    private ActivitySmsBinding binding;
    private BGARecyclerViewAdapter bgaRefreshLayoutAdapter;
    private boolean isEditing = false ;
    private HashMap<Integer,SmsBean> selectSms = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String packageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(packageName)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        if (!EasyPermissions.hasPermissions(this, permissions)){
            startActivity(new Intent(this,MainActivity.class));
            finish();
            return;
        }
        ImmersionBar.with(this).fitsSystemWindows(true).statusBarColor("#537F2D").init();
        binding = ActivitySmsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initRefreshLayout();
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        bgaRefreshLayoutAdapter = new BGARecyclerViewAdapter<SmsBean>(binding.recycler, R.layout.item_coversation_layout) {
            @Override
            protected void fillData(BGAViewHolderHelper helper, int position, SmsBean model) {
                helper.getTextView(R.id.tv_address).setText(model.address);
                helper.getTextView(R.id.tv_title).setText(model.body);
                helper.getTextView(R.id.tv_date).setText(DateUtils.msgDateFormat(model.date));
                if (model.read==1){
                    helper.getTextView(R.id.tv_address).setTextColor(getColor(R.color.b1));
                    helper.getTextView(R.id.tv_title).setTextColor(getColor(R.color.b1));
                    helper.getTextView(R.id.tv_address).setTypeface( Typeface.defaultFromStyle(Typeface.NORMAL));
                    helper.getTextView(R.id.tv_title).setTypeface( Typeface.defaultFromStyle(Typeface.NORMAL));
                }else{
                    helper.getTextView(R.id.tv_address).setTextColor(Color.BLACK);
                    helper.getTextView(R.id.tv_title).setTextColor(Color.BLACK);
                    helper.getTextView(R.id.tv_address).setTypeface( Typeface.defaultFromStyle(Typeface.BOLD));
                    helper.getTextView(R.id.tv_title).setTypeface( Typeface.defaultFromStyle(Typeface.BOLD));
                }
                if (isEditing&&selectSms.containsKey(position)){
                    helper.getImageView(R.id.iv_avatar).setImageResource(R.drawable.ic_right);
                }else{
                    helper.getImageView(R.id.iv_avatar).setImageResource(R.drawable.ic_user_logo);
                }
                helper.getConvertView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isEditing){
                            if (selectSms.containsKey(position)){
                                selectSms.remove(position);
                            }else{
                                selectSms.put(position,model);
                            }
                            notifyItemChanged(position);
                        }else{
                            startActivity(new Intent(SmsActivity.this,SmsPersionActivity.class).putExtra("address",model.address));
                        }
                    }
                });
                helper.getConvertView().setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        isEditing = true;
                        binding.cvEdit.setVisibility(View.VISIBLE);
                        selectSms.put(position,model);
                        notifyItemChanged(position);
                        return true;
                    }
                });
            }
        };
        binding.recycler.setAdapter(bgaRefreshLayoutAdapter);
        getSmsInPhone();
        binding.floating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SmsActivity.this,SendSmsActivity.class));
            }
        });
        binding.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectSms.size()==0)
                    return;
                Set<Integer> keySet = selectSms.keySet();
                for (Integer pos : keySet) {
                    SmsBean smsBean = selectSms.get(pos);
                    deleteSms(smsBean.address);
                }
                selectSms.clear();
                bgaRefreshLayoutAdapter.notifyDataSetChanged();
            }
        });
        binding.ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.cvEdit.setVisibility(View.GONE);
                isEditing = false;
                selectSms.clear();
                bgaRefreshLayoutAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        getSmsInPhone();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getSmsInPhone();
    }

    private void initRefreshLayout() {
        binding.refreshlayout.setDelegate(this);
        BGANormalRefreshViewHolder refreshViewHolder = new BGANormalRefreshViewHolder(this,true);
        binding.refreshlayout.setRefreshViewHolder(refreshViewHolder);
        binding.refreshlayout.setIsShowLoadingMoreView(false);
        binding.refreshlayout.setPullDownRefreshEnable(false);
        refreshViewHolder.setLoadingMoreText("loading");
    }

    public void deleteSms(String address) {
        final String SMS_URI_ALL = "content://sms/"; // 所有短信
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person",
                    "body", "date", "type", "read", "status"};
            getContentResolver().delete(uri, "address='"+address+"'", null);
        } catch (SQLiteException ex) {
        }

        for (int i = 0; i < smsList.size(); i++) {
            SmsBean smsBean = smsList.get(i);
            if (!smsBean.address.equals(address)){
                continue;
            }
            smsList.remove(i);
            break;
        }
    }

    public void getSmsInPhone() {
        final String SMS_URI_ALL = "content://sms/"; // 所有短信
        final String SMS_URI_INBOX = "content://sms/inbox"; // 收件箱
        final String SMS_URI_SEND = "content://sms/sent"; // 已发送
        final String SMS_URI_DRAFT = "content://sms/draft"; // 草稿
        final String SMS_URI_OUTBOX = "content://sms/outbox"; // 发件箱
        final String SMS_URI_FAILED = "content://sms/failed"; // 发送失败
        final String SMS_URI_QUEUED = "content://sms/queued"; // 待发送列表

        StringBuilder smsBuilder = new StringBuilder();
        HashSet<String> addressSet = new HashSet<>();
        smsList.clear();
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person",
                    "body", "date", "type", "read", "status"};
            Cursor cur = getContentResolver().query(uri, projection, null,
                    null, "date desc"); // 获取手机内部短信

            // 获取短信中最新的未读短信
            // Cursor cur = getContentResolver().query(uri, projection,
            // "read = ?", new String[]{"0"}, "date desc");
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
                    Long id = cur.getLong(id_Address);
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    int intType = cur.getInt(index_Type);
                    int read = cur.getInt(index_Read);
                    int status = cur.getInt(index_Status);

                    if (addressSet.contains(strAddress)){
                        continue;
                    }
                    addressSet.add(strAddress);
                    SmsBean smsBean = new SmsBean();
                    smsBean.id =id;
                    smsBean.address =strAddress;
                    smsBean.person =intPerson;
                    smsBean.body =strbody;
                    smsBean.date =longDate;
                    smsBean.type =intType;
                    smsBean.read =read;
                    smsBean.status =status;
                    smsList.add(smsBean);
                    Log.e("TAG222",smsBean.toString());

                    SimpleDateFormat dateFormat = new SimpleDateFormat(
                            "yyyy-MM-dd hh:mm:ss");
                    Date d = new Date(longDate);
                    String strDate = dateFormat.format(d);


                } while (cur.moveToNext());

                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } else {
                smsBuilder.append("no result!");
            }

            smsBuilder.append("getSmsInPhone has executed!");

        } catch (SQLiteException ex) {
            Log.d("SQLiteException in getSmsInPhone", ex.getMessage());
        }
        Log.e("TAG222",smsBuilder.toString());
        bgaRefreshLayoutAdapter.setData(smsList);
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {

    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        return false;
    }
}