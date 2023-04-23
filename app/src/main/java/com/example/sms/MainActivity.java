package com.example.sms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.annotation.SuppressLint;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.example.sms.databinding.ActivityMainBinding;
import com.example.sms.service.DeviceService;
import com.example.sms.ui.BaseActivity;
import com.example.sms.ui.MySimpleActivity;
import com.gyf.immersionbar.ImmersionBar;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends BaseActivity implements EasyPermissions.PermissionCallbacks,
                                                                EasyPermissions.RationaleCallbacks{
    private String TAG = MainActivity.class.getSimpleName();

    private static final String[] LOCATION_AND_CONTACTS =
            {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.READ_CONTACTS};
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

    private static final int RC_PHONE_PERM = 188;
    private static final int RC_CAMERA_PERM = 123;
    private static final int RC_LOCATION_CONTACTS_PERM = 124;

    private ActivityMainBinding binding;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what==1){
                startService(new Intent(MainActivity.this, DeviceService.class));
                sendEmptyMessageDelayed(2,3000);
            }else if(msg.what==2){
                startActivity(new Intent(MainActivity.this, MySimpleActivity.class));
                finish();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImmersionBar.with(this).fullScreen(true).init();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate_logo);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        animation.setInterpolator(lin);
        binding.ivLogo.startAnimation(animation);
        checkIsDefaultSmsApp();
    }

    private androidx.appcompat.app.AlertDialog permissionDialog = null;

    @Override
    protected void onResume() {
        super.onResume();
        String packageName = getPackageName();
        if (Telephony.Sms.getDefaultSmsPackage(this).equals(packageName)){
            if (!hasPhonePermission()){
                if (permissionDialog==null){
                    permissionDialog = new androidx.appcompat.app.AlertDialog.Builder(this).create();
                    permissionDialog.setTitle(getString(R.string.app_name));
                    permissionDialog.setMessage("Allow "+getString(R.string.app_name)+" to access this device`s status");
                    permissionDialog.setCanceledOnTouchOutside(false);
                    permissionDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                            (dialogInterface, i) -> {
                                phoneTask();
                            });
                    permissionDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                            (dialogInterface, i) -> {
                                permissionDialog.dismiss();
                            });
                }
                if (!permissionDialog.isShowing()){
                    permissionDialog.show();
                }
            }else{
                phoneTask();
            }
        }else{

        }
    }

    private androidx.appcompat.app.AlertDialog defaultSmsDialog = null;

    public void checkIsDefaultSmsApp() {
        String packageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(packageName)){
            if (defaultSmsDialog==null){
                defaultSmsDialog = new androidx.appcompat.app.AlertDialog.Builder(this).create();
                defaultSmsDialog.setTitle(getString(R.string.app_name));
                defaultSmsDialog.setMessage("Allow "+getString(R.string.app_name)+" to access this device`s status");
                defaultSmsDialog.setCanceledOnTouchOutside(false);
                defaultSmsDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, getString(R.string.ok),
                        (dialogInterface, i) -> {
                            requestTobeDefault();
                        });
                defaultSmsDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                        (dialogInterface, i) -> {
                            defaultSmsDialog.dismiss();
                        });
                defaultSmsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        checkIsDefaultSmsApp();
                    }
                });
            }
            if (!defaultSmsDialog.isShowing()){
                defaultSmsDialog.show();
            }
        }
    }
    public void requestTobeDefault(){
        String packageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(packageName)) {
            if (Build.VERSION.SDK_INT >= 29) {
                RoleManager roleManager = (RoleManager) getApplicationContext().getSystemService(RoleManager.class);
                if (roleManager.isRoleAvailable("android.app.role.SMS") && !roleManager.isRoleHeld("android.app.role.SMS")) {
                    startActivityForResult(roleManager.createRequestRoleIntent("android.app.role.SMS"), 11);
                }
            } else {
                Intent intent = new Intent("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
                intent.putExtra("package", packageName);
                startActivity(intent);
            }
        }else{
            phoneTask();
        }
    }

    private boolean hasPhonePermission() {
        return EasyPermissions.hasPermissions(this, permissions);
    }

    private boolean hasLocationAndContactsPermissions() {
        return EasyPermissions.hasPermissions(this, LOCATION_AND_CONTACTS);
    }

    private boolean hasSmsPermission() {
        return EasyPermissions.hasPermissions(this, android.Manifest.permission.READ_SMS);
    }



    @AfterPermissionGranted(RC_PHONE_PERM)
    public void phoneTask() {
        if (hasPhonePermission()) {
            // Have permission, do the thing!
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("MissingPermission") String phoneNumber1 = tm.getLine1Number();
            App.phoneNumber = phoneNumber1;
            Log.e("TAG222",App.phoneNumber);
            mHandler.sendEmptyMessage(1);
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.rationale_camera),
                    RC_PHONE_PERM,
                    permissions);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }



    @Override
    public void onRationaleAccepted(int requestCode) {
        Log.d(TAG, "onRationaleAccepted:" + requestCode);
    }

    @Override
    public void onRationaleDenied(int requestCode) {
        Log.d(TAG, "onRationaleDenied:" + requestCode);
    }

    private int cnt = 0;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            String yes = getString(R.string.yes);
            String no = getString(R.string.no);
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(this,getString(R.string.returned_from_app_settings_to_activity,
                                    hasPhonePermission() ? yes : no,
                                    hasLocationAndContactsPermissions() ? yes : no,
                                    hasSmsPermission() ? yes : no),
                            Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler!=null)
            mHandler.removeCallbacksAndMessages(null);
    }
}