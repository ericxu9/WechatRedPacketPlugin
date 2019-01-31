package com.xumiaolee.wechat.plugin;

import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.xumiaolee.wechat.plugin.utils.AccessibilityUtil;
import com.xumiaolee.wechat.plugin.utils.NotificationUtil;

import java.util.List;

/**
 * <p>email:xuyjun@foxmail.com<p/>
 *
 * @author xumiaolee
 */

public class AccessibilityEventHandler {

    private WeChatQhbPluginAccessibilityService service;

    public static final String ACTION_BROADCAST_RECEIVER = "com.accessibility.service";
    public static final String EXTRA_WECHAT_MONEY = "money";

    private static final String TAG = AccessibilityEvent.class.getSimpleName();

    /**
     * 记录当前window标记
     */
    private int mCurrentWindowFlag = WINDOW_DEFAULT;

    /**
     * 微信聊天列表页和详聊（输入框页）
     */
    private static final int WINDOW_SESSION_LIST_OR_DETAIL = 1;
    private static final int WINDOW_LUCKYMONEY_NOT_HOOK_RECEIVE = 2;
    private static final int WINDOW_DEFAULT = -1;

    public AccessibilityEventHandler(WeChatQhbPluginAccessibilityService service) {
        this.service = service;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                notificationStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                windowStateChanged(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                windowContentChanged(event);
                break;
            default:
                break;
        }
    }

    /**
     * todo 通知栏处理
     * 1.判断是否锁屏，解锁
     * 2.判断通知栏是否存在微信红包字眼执行操作
     */
    private void notificationStateChanged(AccessibilityEvent event) {

        if (NotificationUtil.isLockScreen(service)) {
            NotificationUtil.wakeLock(service);
            NotificationUtil.disableKeyguard(service);
        }

        Notification notification = (Notification) event.getParcelableData();
        if (notification.tickerText.toString().contains(WeChatQhbPluginConstant.WX_NOTIFICATION_KEYWORD)) {
            NotificationUtil.openNotification(notification);
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void windowStateChanged(AccessibilityEvent event) {

        String currentUiClassName = event.getClassName().toString();
        Log.d(TAG, "windowStateChanged:" + currentUiClassName);

        delay(100);
        AccessibilityNodeInfo rootNodeInfo = service.getRootInActiveWindow();

        if (currentUiClassName.equals(WeChatQhbPluginConstant.WX_LAUNCHER_UI_CLASSNAME)) {
//            delay(100);
            List<AccessibilityNodeInfo> redPacketNodeInfos = AccessibilityUtil.findNodeInfosByIds(rootNodeInfo, "com.tencent.mm:id/apb");
            if (redPacketNodeInfos != null) {
                for (AccessibilityNodeInfo redPacketNodeInfo : redPacketNodeInfos) {
                    clickSessionListRedPackets(redPacketNodeInfo);
                }
                AccessibilityUtil.performBack(service);
            }
            mCurrentWindowFlag = WINDOW_SESSION_LIST_OR_DETAIL;
        } else if (currentUiClassName.equals(WeChatQhbPluginConstant.UI_LUCKY_MONEY_NOT_HOOK_RECEIVE)) {
            openRedPackets(event);
            mCurrentWindowFlag = WINDOW_LUCKYMONEY_NOT_HOOK_RECEIVE;
        } else if (currentUiClassName.equals(WeChatQhbPluginConstant.UI_LUCKY_MONEY_DETAIL)) {
            //获取红包详情
            AccessibilityNodeInfo moneyNodeInfo = AccessibilityUtil.findNodeInfosById(rootNodeInfo, "com.tencent.mm:id/cqv");
            if (moneyNodeInfo != null) {
                String money = moneyNodeInfo.getText().toString();
                Intent intent = new Intent();
                intent.putExtra(EXTRA_WECHAT_MONEY, Float.valueOf(money));
                intent.setAction(ACTION_BROADCAST_RECEIVER);
                service.sendBroadcast(intent);
            }
            AccessibilityUtil.performBack(service);
        } else {
            mCurrentWindowFlag = WINDOW_DEFAULT;
        }

    }


    private void windowContentChanged(AccessibilityEvent event) {
        /*
        todo
        1. 只处理 WINDOW_SESSION_LIST_OR_DETAIL
         */
        if (mCurrentWindowFlag != WINDOW_SESSION_LIST_OR_DETAIL) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            delay(300);
            AccessibilityNodeInfo rootNodeInfo = service.getRootInActiveWindow();
            //通过判断聊天页面 右下角"+"控件 来区分聊天列表和聊天页面
            if (rootNodeInfo == null) {
                return;
            }
            List<AccessibilityNodeInfo> addNodeInfo = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/alr");
            if (addNodeInfo != null && !addNodeInfo.isEmpty()) {
                List<AccessibilityNodeInfo> redPacketsNodeInfos = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/apb");
                if (redPacketsNodeInfos != null) {
                    for (AccessibilityNodeInfo redPacketsNodeInfo : redPacketsNodeInfos) {
                        clickSessionListRedPackets(redPacketsNodeInfo);
                    }
                }
            } else {
                //聊天列表页面 获取前5个item
                //1.判断是否有小红点未读消息
                //2.判断是否包含微信红包字眼
                //3.执行点击事件
                List<AccessibilityNodeInfo> items = rootNodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/b4m");
                if (items != null && !items.isEmpty()) {
                    int itemCount = items.size() > 2 ? 2 : 1;
                    for (int i = 0; i < itemCount; i++) {
                        //小红点
                        if (AccessibilityUtil.findNodeInfosById(items.get(i), "com.tencent.mm:id/mm") == null) {
                            continue;
                        }
                        //是否包含【微信红包】
                        if (AccessibilityUtil.findNodeInfosById(items.get(i), "com.tencent.mm:id/b4q").toString().contains(WeChatQhbPluginConstant.WX_NOTIFICATION_KEYWORD)) {
                            AccessibilityUtil.performClick(items.get(i));
                            break;
                        }
                    }
                }
            }
            rootNodeInfo.recycle();
        }


    }

    private void openRedPackets(AccessibilityEvent event) {
        delay(100);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cv0");
            Log.d(TAG, "size:" + list == null ? "开为null" : list.size() + "");
            if (list != null && list.size() > 0) {
                AccessibilityUtil.performClick(list.get(0));
                return;
            } else {
                //关闭按钮
                List<AccessibilityNodeInfo> close = root.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cs9");
                if (close != null && !close.isEmpty()) {
                    AccessibilityUtil.performClick(close.get(0));
                }
            }
            root.recycle();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void clickSessionListRedPackets(AccessibilityNodeInfo redPacketNodeInfo) {
        int childCount = redPacketNodeInfo.getChildCount();
        AccessibilityNodeInfo child = redPacketNodeInfo.getChild(childCount - 1);
        if (child != null) {
            int thirdCount = child.getChildCount();
            //没有领取（针对windowStateChanged）
            if (thirdCount == 1) {
                AccessibilityUtil.performClick(redPacketNodeInfo);
            } else {
                //查找已领取控件，不存在说明可以点击领取(针对windowContentChanged)
                List<AccessibilityNodeInfo> receivedNodeInfo = child.getChild(childCount - 1).findAccessibilityNodeInfosByViewId("com.tencent.mm:id/ape");
                if (receivedNodeInfo == null || receivedNodeInfo.isEmpty()) {
                    AccessibilityUtil.performClick(redPacketNodeInfo);
                }
            }
        }
    }

    /**
     * 延迟
     */
    private void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
