package com.hcl.kandy.cpass.call;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.hcl.kandy.cpass.R;


public class ForegroundCallService extends IntentService {
    public static final String ACTION_START_FOREGROUND_CALL_SERVICE = "startService";
    public static final String ACTION_STOP_FOREGROUND_CALL_SERVICE = "stopService";
    private static final String TAG = "ForegroundCallService";
    private static final String CALL_WAKELOCK_TAG = "CallWakeLock";
    private static final int NOTIFICATION_ID = 22637;
    private static final int WAKELOCK_ALLOCATION_DURATION = 15 * 1000; // 15 seconds
    private static PowerManager.WakeLock wakeLockInstance;

    public ForegroundCallService() {
        super("ForegroundCallService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "onHandleIntent: is intent null? " + (intent == null ? "true" : "false"));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Creating");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Stopping");
        stopWakelock();
        wakeLockInstance = null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String intentAction = "";
        if (intent != null) {
            intentAction = intent.getAction();
        }

        assert intentAction != null;
        if (intentAction.equals(ACTION_START_FOREGROUND_CALL_SERVICE)) {
            startForeground(NOTIFICATION_ID, buildForegroundServiceNotification());
            startWakelock();
        } else if (intentAction.equals(ACTION_STOP_FOREGROUND_CALL_SERVICE)) {
            stopForeground(true);
            stopSelf();
        }
        // return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    private synchronized PowerManager.WakeLock getWakeLock() {
        PowerManager powerManager = (PowerManager)
                getSystemService(Context.POWER_SERVICE);

        if (wakeLockInstance == null) {
            wakeLockInstance = powerManager != null ? powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, CALL_WAKELOCK_TAG) : null;
        }

        return wakeLockInstance;
    }

    private synchronized void startWakelock() {
        Log.i(TAG, "startWakelock");
        PowerManager.WakeLock wakeLock = getWakeLock();

        if (wakeLock != null) {
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(WAKELOCK_ALLOCATION_DURATION);
                Log.d(TAG, "startWakelock: Wakelock acquired");
            } else {
                Log.d(TAG, "startWakelock: Not starting, wakelock is already acquired");
            }
        } else {
            Log.d(TAG, "startWakelock: Not starting. Can't acquire wakelock.");
        }
    }

    private synchronized void stopWakelock() {
        Log.i(TAG, "stopWakelock");
        PowerManager.WakeLock wakeLock = getWakeLock();

        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                    Log.d(TAG, "stopWakelock: Wakelock released");
                } catch (Throwable throwable) {
                    Log.e(TAG, "stopWakelock: Cannot stop, wakelock is under-locked", throwable);
                }
            } else {
                Log.d(TAG, "stopWakelock: Wakelock is not helded, not releasing");
            }
        } else {
            Log.d(TAG, "stopWakelock: Not stopping. Wakelock reference is null");
        }
    }

    private Notification buildForegroundServiceNotification() {
        Log.d(TAG, "buildForegroundServiceNotification");
        NotificationCompat.Builder notificationBuilder;
        NotificationManager notifManager = (NotificationManager) getSystemService
                (Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel channel = new NotificationChannel
                    ("ActiveCall", "ActiveCall", importance);
            channel.setDescription("ActiveCall notification");
            channel.enableVibration(true);

            if (notifManager != null) {
                notifManager.createNotificationChannel(channel);
            }

            notificationBuilder = new NotificationCompat.Builder(this, "ActiveCall");
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Intent outbound = new Intent(this, CallActivity.class);

        notificationBuilder.setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.foreground_call_service_content_text))
                .setSmallIcon(R.drawable.ic_call)
                .setPriority(Notification.PRIORITY_MIN)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                        outbound, PendingIntent.FLAG_UPDATE_CURRENT));

        return notificationBuilder.build();
    }
}