package com.screenlock.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.btn_start);
        Button stopBtn = findViewById(R.id.btn_stop);
        TextView statusText = findViewById(R.id.tv_status);

        startBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
                } else {
                    startLockService();
                    statusText.setText("✅ Lock Button Active!\nFacebook/YouTube দেখতে দেখতে lock icon এ click করো।");
                }
            } else {
                startLockService();
                statusText.setText("✅ Lock Button Active!");
            }
        });

        stopBtn.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingLockService.class));
            statusText.setText("❌ Lock Button Stopped");
            Toast.makeText(this, "Lock service বন্ধ হয়েছে", Toast.LENGTH_SHORT).show();
        });
    }

    private void startLockService() {
        Intent serviceIntent = new Intent(this, FloatingLockService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        Toast.makeText(this, "🔒 Floating lock button চালু হয়েছে!", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startLockService();
            } else {
                Toast.makeText(this, "Permission দাও, নাহলে কাজ করবে না!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
