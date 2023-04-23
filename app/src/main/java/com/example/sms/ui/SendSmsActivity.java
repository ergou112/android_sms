package com.example.sms.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import com.example.sms.databinding.ActivitySendSmsBinding;
import com.gyf.immersionbar.ImmersionBar;

public class SendSmsActivity extends BaseActivity {
    private ActivitySendSmsBinding binding;
    private String address = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).fitsSystemWindows(true).statusBarColor("#537F2D").init();
        binding = ActivitySendSmsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                address = editable.toString().trim();
                binding.tvSendto.setText("Send to "+address);
                if (editable.toString().length()>0){
                    binding.ivDelete.setVisibility(View.VISIBLE);
                    binding.llSendto.setVisibility(View.VISIBLE);
                }else{
                    binding.ivDelete.setVisibility(View.GONE);
                    binding.llSendto.setVisibility(View.GONE);
                }
            }
        });

        binding.ivDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.etSearch.setText("");
            }
        });
        binding.ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        binding.tvSendto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SendSmsActivity.this,SmsPersionActivity.class).putExtra("address",address));
                finish();
            }
        });
    }
}