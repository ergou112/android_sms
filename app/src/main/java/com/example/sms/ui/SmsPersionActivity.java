package com.example.sms.ui;

import static android.view.View.GONE;

import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.sms.R;
import com.example.sms.databinding.ActivitySmsPersionBinding;
import com.example.sms.domain.SmsBean;
import com.example.sms.event.SendSmsEvent;
import com.example.sms.receiver.SendSmsReceiver;
import com.example.sms.utils.DateUtils;
import com.example.sms.utils.StringUtils;
import com.gyf.immersionbar.ImmersionBar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import cn.bingoogolapple.baseadapter.BGARecyclerViewAdapter;
import cn.bingoogolapple.baseadapter.BGAViewHolderHelper;
import cn.bingoogolapple.refreshlayout.BGANormalRefreshViewHolder;
import cn.bingoogolapple.refreshlayout.BGARefreshLayout;

public class SmsPersionActivity extends BaseActivity implements BGARefreshLayout.BGARefreshLayoutDelegate {
    private String SMS_SEND_ACTIOIN = "SMS_SEND_ACTIOIN";
    private String SMS_DELIVERED_ACTION = "SMS_DELIVERED_ACTION";
    private ActivitySmsPersionBinding binding;
    private BGARecyclerViewAdapter bgaRefreshLayoutAdapter;
    private List<SmsBean> smsList = new ArrayList<>();
    private String address;
    private boolean isEditing = false ;

    private HashMap<Long,SmsBean> selectSms = new HashMap<>();
    private ArrayList<SendSmsReceiver> receivers = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this)
                .fitsSystemWindows(true)
                .statusBarColor("#537F2D")
                .keyboardEnable(true)
                .init();
        binding = ActivitySmsPersionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EventBus.getDefault().register(this);
        address = getIntent().getStringExtra("address");
        binding.tvTitle.setText(StringUtils.phoneNumberFormat(address));
        initRefreshLayout();
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        bgaRefreshLayoutAdapter = new BGARecyclerViewAdapter<SmsBean>(binding.recycler, R.layout.item_sms_layout) {
            @Override
            protected void fillData(BGAViewHolderHelper helper, int position, SmsBean model) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                String thisDate = sdf.format(model.date);
                if (position==0){
                    helper.getTextView(R.id.tv_date).setText(thisDate);
                    helper.getTextView(R.id.tv_date).setVisibility(View.VISIBLE);
                }else{
                    SmsBean lastSmsBean = smsList.get(position - 1);
                    String lastDate = sdf.format(lastSmsBean.date);
                    if (lastDate.equals(thisDate)){
                        helper.getTextView(R.id.tv_date).setText(thisDate);
                        helper.getTextView(R.id.tv_date).setVisibility(GONE);
                    }else{
                        helper.getTextView(R.id.tv_date).setText(thisDate);
                        helper.getTextView(R.id.tv_date).setVisibility(View.VISIBLE);
                    }
                }
                helper.getTextView(R.id.tv_time).setText(DateUtils.msgDateFormat(model.date));
                helper.getTextView(R.id.tv_body).setText(model.body);


                LinearLayout llMsgContainer = helper.getView(R.id.ll_msg_container);
                LinearLayout llMsgCon = helper.getView(R.id.ll_msg_con);
                llMsgContainer.setVisibility(View.VISIBLE);
                llMsgContainer.setGravity(model.type==1?Gravity.LEFT:Gravity.RIGHT);
                llMsgCon.setGravity(model.type==1?Gravity.LEFT:Gravity.RIGHT);

                helper.getImageView(R.id.iv_status).setVisibility(model.type==1?GONE:View.VISIBLE);
                if (model.status==64){
                    helper.getImageView(R.id.iv_status).setImageResource(R.drawable.baseline_access_time_24);
                }else if(model.status==128){
                    helper.getImageView(R.id.iv_status).setImageResource(R.drawable.baseline_error_outline_24);
                }else{
                    helper.getImageView(R.id.iv_status).setImageResource(R.drawable.ic_right);
                }
                if (position==0){
                    if (model.type==1){
                        //收到
                        helper.getImageView(R.id.iv_left).setVisibility(View.VISIBLE);
                        helper.getImageView(R.id.iv_right).setVisibility(View.INVISIBLE);
                    }else{
                        //发出
                        helper.getImageView(R.id.iv_right).setVisibility(View.VISIBLE);
                        helper.getImageView(R.id.iv_left).setVisibility(View.INVISIBLE);
                    }
                }else{
                    SmsBean lastSmsBean = smsList.get(position - 1);
                    if (model.type!=lastSmsBean.type){
                        if (model.type==1){
                            //收到
                            helper.getImageView(R.id.iv_left).setVisibility(View.VISIBLE);
                            helper.getImageView(R.id.iv_right).setVisibility(View.INVISIBLE);
                        }else{
                            //发出
                            helper.getImageView(R.id.iv_right).setVisibility(View.VISIBLE);
                            helper.getImageView(R.id.iv_left).setVisibility(View.INVISIBLE);
                        }
                    }else{
                        if ((model.date-lastSmsBean.date)<60*1000){
                            if (model.type==1){
                                helper.getImageView(R.id.iv_left).setVisibility(View.INVISIBLE);
                                helper.getImageView(R.id.iv_right).setVisibility(View.INVISIBLE);
                            }else{
                                helper.getImageView(R.id.iv_left).setVisibility(View.INVISIBLE);
                                helper.getImageView(R.id.iv_right).setVisibility(View.INVISIBLE);
                            }
                        }else{
                            if (model.type==1){
                                //收到
                                helper.getImageView(R.id.iv_left).setVisibility(View.VISIBLE);
                                helper.getImageView(R.id.iv_right).setVisibility(View.INVISIBLE);
                            }else{
                                //发出
                                helper.getImageView(R.id.iv_right).setVisibility(View.VISIBLE);
                                helper.getImageView(R.id.iv_left).setVisibility(View.INVISIBLE);
                            }
                        }

                    }
                }
                helper.getConvertView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (isEditing){
                            if (selectSms.containsKey(model.id)){
                                selectSms.remove(model.id);
                            }else{
                                selectSms.put(model.id,model);
                            }
                            notifyItemChanged(position);
                        }
                    }
                });

                helper.getConvertView().setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        isEditing = true;
                        binding.cvEdit.setVisibility(View.VISIBLE);
                        selectSms.put(model.id,model);
                        notifyItemChanged(position);
                        return true;
                    }
                });

                if (selectSms.containsKey(model.id)){
                    helper.getView(R.id.ll_msg_con).setAlpha(0.6f);
                }else{
                    helper.getView(R.id.ll_msg_con).setAlpha(1f);
                }
            }
        };
        binding.recycler.setAdapter(bgaRefreshLayoutAdapter);
        setAllRead();
        getSmsInPhone();

        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        binding.ivSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = binding.etContent.getText().toString();
                if (TextUtils.isEmpty(content))
                    return;
                ContentResolver contentResolver = getContentResolver();
                SmsManager smsManager = SmsManager.getDefault();
                Uri parse = Uri.parse("content://sms/sent");
                ArrayList<String> conList=smsManager.divideMessage(content);
                ArrayList<PendingIntent> itSends = new ArrayList<>();
                ArrayList<PendingIntent> itDelivers = new ArrayList<>();
                for (String txtCon : conList) {
                    //先插入数据库
                    ContentValues contentValues = new ContentValues();
                    contentValues.put("address", address);
                    contentValues.put("body", txtCon);
                    contentValues.put("status", 64);
                    Uri uri = contentResolver.insert(parse, contentValues);
                    long id = Long.parseLong(uri.getLastPathSegment());
                    IntentFilter mFilter01 = new IntentFilter();
                    mFilter01.addAction(SMS_SEND_ACTIOIN+":"+id);
                    SendSmsReceiver mReceiver01 = new SendSmsReceiver();
                    registerReceiver(mReceiver01, mFilter01);

                    IntentFilter mFilter02 = new IntentFilter();
                    mFilter02.addAction(SMS_DELIVERED_ACTION+":"+id);
                    SendSmsReceiver mReceiver02 = new SendSmsReceiver();
                    registerReceiver(mReceiver02, mFilter02);

                    receivers.add(mReceiver01);
                    receivers.add(mReceiver02);

                    Intent itSend = new Intent(SMS_SEND_ACTIOIN+":"+id);
                    Intent itDeliver = new Intent(SMS_DELIVERED_ACTION+":"+id);
                    PendingIntent mSendPI = PendingIntent.getBroadcast
                            (getApplicationContext(), 5, itSend, 0);
                    PendingIntent mDeliverPI = PendingIntent.getBroadcast
                            (getApplicationContext(), 6, itDeliver, 0);
                    itSends.add(mSendPI);
                    itDelivers.add(mDeliverPI);
                }
                smsManager.sendMultipartTextMessage(address,null, conList , itSends, itDelivers);
                binding.etContent.setText("");
                getSmsInPhone();
            }
        });
        binding.etContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                binding.ivSend.setImageResource(editable.toString().length()==0?R.drawable.ic_user_logo:R.drawable.baseline_send_24);
            }
        });

        binding.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectSms.size()==0)
                    return;
                Set<Long> keySet = selectSms.keySet();
                for (Long id : keySet) {
                    SmsBean smsBean = selectSms.get(id);
                    deleteSms(id);
                }
                selectSms.clear();
                getSmsInPhone();
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
        binding.ivCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectSms.size()==0)
                    return;
                StringBuilder content = new StringBuilder();
                Set<Long> keySet = selectSms.keySet();
                for (Long id : keySet) {
                    SmsBean smsBean = selectSms.get(id);
                    content.append(smsBean.address+"\r\n"+smsBean.body+"\r\n");
                }
                ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("message", content.toString().trim()));
                Toast.makeText(SmsPersionActivity.this,"Copied",Toast.LENGTH_SHORT).show();
                binding.cvEdit.setVisibility(View.GONE);
                selectSms.clear();
                isEditing = false;
                bgaRefreshLayoutAdapter.notifyDataSetChanged();
            }
        });
    }

    private void deleteSms(Long id) {
        final String SMS_URI_ALL = "content://sms/"; // 所有短信
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person",
                    "body", "date", "type", "read", "status"};
            getContentResolver().delete(uri, "_id="+id, null);
        } catch (SQLiteException ex) {
        }
    }

    private void setAllRead() {
        ContentValues contentValues = new ContentValues();
        contentValues.put("read",1);
        getContentResolver().update(Uri.parse("content://sms/"), contentValues, "address='"+address+"'", null);
    }

    private void initRefreshLayout() {
        binding.refreshlayout.setDelegate(this);
        BGANormalRefreshViewHolder refreshViewHolder = new BGANormalRefreshViewHolder(this,true);
        binding.refreshlayout.setRefreshViewHolder(refreshViewHolder);
        binding.refreshlayout.setIsShowLoadingMoreView(false);
        binding.refreshlayout.setPullDownRefreshEnable(false);
        refreshViewHolder.setLoadingMoreText("loading");
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
        smsList.clear();
        try {
            Uri uri = Uri.parse(SMS_URI_ALL);
            String[] projection = new String[] { "_id", "address", "person",
                    "body", "date", "type", "read", "status"};
            Cursor cur = getContentResolver().query(uri, projection, "address='"+address+"'",
                    null, "date asc");
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
        bgaRefreshLayoutAdapter.setData(smsList);
        binding.recycler.scrollToPosition(bgaRefreshLayoutAdapter.getItemCount() - 1);
    }

    @Override
    public void onBGARefreshLayoutBeginRefreshing(BGARefreshLayout refreshLayout) {

    }

    @Override
    public boolean onBGARefreshLayoutBeginLoadingMore(BGARefreshLayout refreshLayout) {
        return false;
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SendSmsEvent event) {
        unregisterReceiver(event.receiver);
        getSmsInPhone();
    }
}
