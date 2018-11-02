package cn.lhmachine.eyeprotect;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * @author lhmachine
 * 声明一个服务用来增加一个护眼悬浮罩，覆盖在整个view上
 */

public class EyeProtectService extends Service {

    private FrameLayout mView;
    private static final int NOTIFY_ID = 1;

    public EyeProtectService() {
    }

    @Override
    public void onCreate(){
        super.onCreate();
        SharedPreferences pref = getSharedPreferences("data", MODE_PRIVATE);
        startProtect(pref.getInt("dim", 50));
        showNotification();
    }

    //开启护眼
    private void startProtect(int dim){
        //新建一个FramLayout，并设置背景颜色
        mView = new FrameLayout(this);
        mView.setBackgroundColor(Color.argb(dim,199,237,204));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        params.height = WindowManager.LayoutParams.MATCH_PARENT;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        //设置效果透明，否则全黑
        params.format=PixelFormat.RGBA_8888;
        //最后两个标志用来覆盖状态栏
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    //通知栏显示信息
    private void showNotification(){
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Eye Protect").setContentText("护眼模式已开启");
        //创建通知背点击时触发的Intent
        Intent resultIntent = new Intent(this, MainActivity.class);
        //创建任务栈Builder
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotifyMgr = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        //构建通知
        final Notification notification = mBuilder.build();
        //显示通知
        mNotifyMgr.notify(NOTIFY_ID, notification);
        //启动为前台服务
        startForeground(NOTIFY_ID, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy(){
        if (mView != null){
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(mView);
            mView = null;
        }
    }
}
