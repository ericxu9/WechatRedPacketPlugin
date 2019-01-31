package com.xumiaolee.wechat.plugin;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by xuyongjun on 2019/1/28.
 */

public class WeChatQhbPluginAccessibilityService extends AccessibilityService {

    public static final String TAG = WeChatQhbPluginAccessibilityService.class.getSimpleName();

    /**
     * 事件处理类
     */
    private AccessibilityEventHandler mAccessibilityEventHandler;

    @Override
    public void onCreate() {
        mAccessibilityEventHandler = new AccessibilityEventHandler(this);
        super.onCreate();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "onServiceConnected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mAccessibilityEventHandler.onAccessibilityEvent(event);
        }
    }

    @Override
    public void onInterrupt() {

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
