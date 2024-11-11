package changjoopark.com.flutter_foreground_plugin;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;


public class FlutterForegroundService extends Service {
    private static String TAG = "FlutterForegroundService";
    public static int ONGOING_NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_CHANNEL_ID = "CHANNEL_ID";
    public static final String ACTION_STOP_SERVICE = "STOP";

    private boolean userStopForegroundService = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStartCommand: Intent is null");
            return START_NOT_STICKY;
        }
        if (intent.getAction() == null) {
            Log.d(TAG, "onStartCommand: Intent action is null");
            return START_NOT_STICKY;
        }
        final String action = intent.getAction();
        Log.d(TAG, String.format("onStartCommand: %s", action));

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (getApplicationContext().checkSelfPermission( Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions( new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
//            }
//        }
        switch (action) {
            case FlutterForegroundPlugin.START_FOREGROUND_ACTION:
                PackageManager pm = getApplicationContext().getPackageManager();
                Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, PendingIntent.FLAG_MUTABLE);

                Bundle bundle = intent.getExtras();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            "flutter_foreground_service_channel",
                            NotificationManager.IMPORTANCE_HIGH);

                    NotificationManager manager = getSystemService(NotificationManager.class);
                    if (manager != null) {
                        manager.createNotificationChannel(channel);
                    }
                }
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(getNotificationIcon(bundle.getString("icon")))
                       // .setColor(bundle.getInt("color"))
                        .setContentTitle(bundle.getString("title"))
                        .setContentText(bundle.getString("content"))
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setContentIntent(pendingIntent)
                    //    .setUsesChronometer(bundle.getBoolean("chronometer"))
                        .setOngoing(true);

                if (bundle.getBoolean("stop_action")) {
                    Intent stopSelf = new Intent(this, FlutterForegroundService.class);
                    stopSelf.setAction(ACTION_STOP_SERVICE);

                    PendingIntent pStopSelf = PendingIntent
                            .getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT|PendingIntent.FLAG_MUTABLE);
                    builder.addAction(getNotificationIcon(bundle.getString("stop_icon")),
                            bundle.getString("stop_text"),
                            pStopSelf);
                }

                if (bundle.getString("subtext") != null && !bundle.getString("subtext").isEmpty()) {
                    builder.setSubText(bundle.getString("subtext"));
                }
                Log.d(TAG, "startForeground called");
                NotificationManager manager = getSystemService(NotificationManager.class);
                Notification notification = builder.build();
                manager.notify(ONGOING_NOTIFICATION_ID,notification);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.d(TAG, "startForeground called with microphone permissions");
                    startForeground(ONGOING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                } else {
                    Log.d(TAG, "startForeground called without microphone permissions");
                    startForeground(ONGOING_NOTIFICATION_ID, notification);
                }
                break;
            case FlutterForegroundPlugin.STOP_FOREGROUND_ACTION:
                stopFlutterForegroundService();
                break;
            case ACTION_STOP_SERVICE:
                stopFlutterForegroundService();
                break;
            default:
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (!userStopForegroundService) {
            Log.d(TAG, "User close app, kill current process to avoid memory leak in other plugin.");
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getNotificationIcon(String iconName) {
        int resourceId = getApplicationContext().getResources().getIdentifier(iconName, "drawable", getApplicationContext().getPackageName());
        return resourceId;
    }

    private void stopFlutterForegroundService() {
        userStopForegroundService = true;
        stopForeground(true);
        stopSelf();
    }
}
