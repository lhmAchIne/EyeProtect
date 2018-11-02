package cn.lhmachine.eyeprotect;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v4.widget.ImageViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.security.MessageDigest;
import java.security.Permission;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private boolean open_symbol = false;
    private ImageView bt;
    private Timer timer;
    private ConstraintLayout main_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);

        bt = (ImageView) findViewById(R.id.bt_eye);
        main_layout = (ConstraintLayout)findViewById(R.id.mainlayout);

        //检查权限
        checkPermission();

        //护眼模式开启或关闭
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //判断护眼模式是否开启
                if (open_symbol){
                    //关闭护眼
                    open_symbol = false;
                    main_layout.setBackgroundResource(R.drawable.image_day);
                    bt.setImageResource(R.drawable.sun);
                    stopService(new Intent(getApplicationContext(), EyeProtectService.class));
                }else{
                    //开启护眼
                    open_symbol = true;
                    main_layout.setBackgroundResource(R.drawable.image_nig);
                    bt.setImageResource(R.drawable.moon);
                    startService(new Intent(getApplicationContext(), EyeProtectService.class));
                }
                if (isServiceWork(getApplicationContext(), "cn.lhmachine.eyeprotect.TimeService")){
                    stopService(new Intent(getApplicationContext(), TimeService.class));
                    Toast.makeText(MainActivity.this, "定时功能已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //个人设置
        findViewById(R.id.setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        //检查服务是否在运行
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        if(pref.getBoolean("onTimeOpen", false)){
            if (!isServiceWork(getApplicationContext(), "cn.lhmachine.eyeprotect.TimeService")){
                startService(new Intent(getApplicationContext(),TimeService.class));
            }
        }

        if (isServiceWork(getApplicationContext(), "cn.lhmachine.eyeprotect.EyeProtectService")){
            open_symbol = true;
            main_layout.setBackgroundResource(R.drawable.image_nig);
            bt.setImageResource(R.drawable.moon);
        }else{
            open_symbol = false;
            main_layout.setBackgroundResource(R.drawable.image_day);
            bt.setImageResource(R.drawable.sun);
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
            if (isServiceWork(getApplicationContext(), "cn.lhmachine.eyeprotect.EyeProtectService")){
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
            }
        }, 0, 500);
    }

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message message){
            if (message.what == 1){
                open_symbol = true;
                main_layout.setBackgroundResource(R.drawable.image_nig);
                bt.setImageResource(R.drawable.moon);
            }else{
                open_symbol = false;
                main_layout.setBackgroundResource(R.drawable.image_day);
                bt.setImageResource(R.drawable.sun);
            }
        }
    };

    /**
     * 权限检查
     */
    private void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            PackageManager pm = getPackageManager();
            if (!Settings.System.canWrite(this) || !Settings.canDrawOverlays(this) ){
                //未被授予权限
                showDialogTipUserRequestPermission();
            }
        }
    }

    /**
     * 请求权限弹出框
     */
    private void showDialogTipUserRequestPermission() {
        new AlertDialog.Builder(this)
                .setTitle("权限不可用")
                .setMessage("由于本app需要获取您的悬浮框和系统设置授权；\n否则，您将无法使用本App")
                .setPositiveButton("立即开启", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRequestPermission();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).setCancelable(false).show();
    }

    /**
     * 开始申请权限
     */
    private void startRequestPermission(){
        getOverlayPermission();
        getSettingPermission();
    }

    /**
     * 申请悬浮窗权限
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void getOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 0);
    }

    /**
     * 申请系统设置权限
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void getSettingPermission(){
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 0);
    }

    /**
     * 判断权限是否正在运行
     * @param mContext 全局环境
     * @param serviceName 服务名
     * @return 布尔类型变量:true-服务正在运行;false-服务没有运行
     */
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        timer.cancel();
    }
}
