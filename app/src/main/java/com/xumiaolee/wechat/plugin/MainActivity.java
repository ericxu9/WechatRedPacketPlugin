package com.xumiaolee.wechat.plugin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity {

    private static final String RED_PACKETS_COUNT = "redPacketsCount";
    private static final String RED_PACKETS_AMOUNT = "redPacketsAmount";

    private SharedPreferences mSharedPreferences;
    private AccessibilityReceiver mAccessibilityReceiver;

    private TextView mRedPacketsCountTextView, mRedPacketsAmountTextView;
    private ImageView mWeChatImageView, mAlipayImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getSharedPreferences(WeChatQhbPluginConstant.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        initView();


        mAccessibilityReceiver = new AccessibilityReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.accessibility.service");
        registerReceiver(mAccessibilityReceiver, filter);
    }

    private void initView() {
        mRedPacketsCountTextView = findViewById(R.id.redPacketsCountTextView);
        mRedPacketsAmountTextView = findViewById(R.id.redPacketsAmountTextView);
        mWeChatImageView = findViewById(R.id.weChatImageView);
        mAlipayImageView = findViewById(R.id.alipayImageView);
        mWeChatImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveWeChatQrCodeImage();
                return true;
            }
        });
        mAlipayImageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                saveAlipayQrCodeImage();
                return true;
            }
        });

        int redPacketsCount = mSharedPreferences.getInt(RED_PACKETS_COUNT, -1);
        float redPacketsAmount = mSharedPreferences.getFloat(RED_PACKETS_AMOUNT, -1);
        if (redPacketsAmount != -1 && redPacketsCount != -1) {
            mRedPacketsCountTextView.setText("累计抢到红包：" + redPacketsCount);
            mRedPacketsAmountTextView.setText("共计金额：" + redPacketsAmount);
        }

    }


    public void openAccessibilityServiceSettings(View view) {
        startService(new Intent(this, WeChatQhbPluginAccessibilityService.class));

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    public void saveWeChatQrCodeImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);

                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                }
            } else {
                saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.wechat));
            }
        } else {
            saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.wechat));
        }
    }

    public void saveAlipayQrCodeImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);

                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);
                }
            } else {
                saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.alipay));
            }
        } else {
            saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.alipay));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.wechat));
                } else {
                    Toast.makeText(this, "请到系统设置中打开存储权限~", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case 2: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImageToGallery(this, BitmapFactory.decodeResource(getResources(), R.mipmap.alipay));
                } else {
                    Toast.makeText(this, "请到系统设置中打开存储权限~", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    class AccessibilityReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int redPacketsCount = mSharedPreferences.getInt(RED_PACKETS_COUNT, 0);
            float redPacketsAmount = mSharedPreferences.getFloat(RED_PACKETS_AMOUNT, 0);

            float money = intent.getFloatExtra(AccessibilityEventHandler.EXTRA_WECHAT_MONEY, 0.0f);


            float totalAmount = new BigDecimal(Float.toString(redPacketsAmount)).add(new BigDecimal(Float.toString(money))).floatValue();
            int totalCount = ++redPacketsCount;
            mRedPacketsCountTextView.setText("累计抢到红包：" + totalCount);
            mRedPacketsAmountTextView.setText("共计金额：" + totalAmount);

            SharedPreferences.Editor edit = mSharedPreferences.edit();
            edit.putInt(RED_PACKETS_COUNT, totalCount);
            edit.putFloat(RED_PACKETS_AMOUNT, totalAmount);
            edit.apply();
        }
    }

    public static void saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(file.getAbsolutePath())));
        Toast.makeText(context, "保存到系统相册成功~", Toast.LENGTH_SHORT).show();
    }
}
