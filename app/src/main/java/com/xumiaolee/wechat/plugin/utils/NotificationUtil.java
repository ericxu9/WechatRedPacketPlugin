package com.xumiaolee.wechat.plugin.utils;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

/**
 * Created by xuyongjun on 2019/1/28.
 */

public class NotificationUtil {

    /**
     * 是否为锁屏或黑屏状态
     */
    public static boolean isLockScreen(Context context) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        return km.inKeyguardRestrictedInputMode() || !isScreenOn(context);
    }

    public static boolean isScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    /**
     * 唤醒屏幕
     */
    public static void wakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.SCREEN_DIM_WAKE_LOCK, "NotificationUtil");//最后的参数是LogCat里用的Tag
        wl.acquire(3000);
        wl.release();
    }

    public static void disableKeyguard(Context context) {
        KeyguardManager km= (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock kl = km.newKeyguardLock("NotificationUtil");//参数是LogCat里用的Tag
        kl.reenableKeyguard();
        kl.disableKeyguard();
    }

    public static void openNotification(Notification notification) {
        try {
            notification.contentIntent.send();
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }
}
