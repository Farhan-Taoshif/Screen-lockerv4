package com.screenlock.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.core.app.NotificationCompat;

public class FloatingLockService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View lockOverlayView;
    private boolean isLocked = false;

    private static final String CHANNEL_ID = "ScreenLockChannel";
    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // ===== FLOATING LOCK BUTTON =====
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_lock_button, null);

        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams floatParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        floatParams.gravity = Gravity.TOP | Gravity.LEFT;
        floatParams.x = 50;
        floatParams.y = 300;

        windowManager.addView(floatingView, floatParams);

        // ===== LOCK OVERLAY (full screen block) =====
        lockOverlayView = new View(this);
        lockOverlayView.setBackgroundColor(Color.TRANSPARENT);

        final WindowManager.LayoutParams overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP;

        // Touch listener on overlay — blocks ALL touches except the lock button
        lockOverlayView.setOnTouchListener((v, event) -> true); // consume all touches

        ImageView lockIcon = floatingView.findViewById(R.id.iv_lock_icon);

        // ===== DRAG SUPPORT =====
        floatingView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = floatParams.x;
                    initialY = floatParams.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    floatParams.x = initialX + (int)(event.getRawX() - initialTouchX);
                    floatParams.y = initialY + (int)(event.getRawY() - initialTouchY);
                    windowManager.updateViewLayout(floatingView, floatParams);
                    return true;

                case MotionEvent.ACTION_UP:
                    int deltaX = (int)(event.getRawX() - initialTouchX);
                    int deltaY = (int)(event.getRawY() - initialTouchY);
                    // If barely moved = it's a click
                    if (Math.abs(deltaX) < 10 && Math.abs(deltaY) < 10) {
                        toggleLock(lockIcon, overlayParams, floatParams);
                    }
                    return true;
            }
            return false;
        });
    }

    private void toggleLock(ImageView lockIcon,
                            WindowManager.LayoutParams overlayParams,
                            WindowManager.LayoutParams floatParams) {
        if (!isLocked) {
            // LOCK
            isLocked = true;
            lockIcon.setImageResource(R.drawable.ic_lock_closed);
            floatParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

            // Add blocking overlay BEHIND the floating button
            // Overlay z-order trick: add overlay first, then re-add button on top
            try {
                windowManager.addView(lockOverlayView, overlayParams);
            } catch (Exception ignored) {}

            // Bring lock button to front
            windowManager.removeView(floatingView);
            windowManager.addView(floatingView, floatParams);

        } else {
            // UNLOCK
            isLocked = false;
            lockIcon.setImageResource(R.drawable.ic_lock_open);
            try {
                windowManager.removeView(lockOverlayView);
            } catch (Exception ignored) {}
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🔒 Screen Lock Active")
                .setContentText("Floating lock button চলছে")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Screen Lock Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            try { windowManager.removeView(floatingView); } catch (Exception ignored) {}
        }
        if (lockOverlayView != null) {
            try { windowManager.removeView(lockOverlayView); } catch (Exception ignored) {}
        }
    }
}
