package uk23.ripper121.com.fishalarm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class MyService extends Service {


    private final static String TAG = MainActivity.class.getSimpleName();
    static final int UdpServerPORT = 8080;
    private boolean serverRunning = true;
    private DatagramSocket socket;
    public static NotificationManager notificationManager = null;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    public static final String ACTION_LOCATION_BROADCAST = MainActivity.class.getName() + "Broadcast", EXTRA_TEXT = "extra_text";

    private void sendBroadcastMessage(String text) {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_TEXT, text);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onCreate() {
        // To avoid cpu-blocking, we create a background handler to run our service
        HandlerThread thread = new HandlerThread("TutorialService", Process.THREAD_PRIORITY_BACKGROUND);
        // start the new handler thread
        thread.start();

        mServiceLooper = thread.getLooper();
        // start the service using the background handler
        mServiceHandler = new ServiceHandler(mServiceLooper);
        serverRunning = true;
        sendBroadcastMessage("Service created");
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(0);
        serverRunning = false;
        generateNotification(MyService.this, "Fish Alarm Running", 0, false);
        notificationManager.cancelAll();
        sendBroadcastMessage("Service stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // call a new service handler. The service ID can be used to identify the service
        Message message = mServiceHandler.obtainMessage();
        message.arg1 = startId;
        mServiceHandler.sendMessage(message);

        generateNotification(MyService.this, "Fish Alarm Running", 0, true);
        sendBroadcastMessage("Service is running");
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Object responsible for
    private final class ServiceHandler extends Handler {


        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            // Well calling mServiceHandler.sendMessage(message); from onStartCommand,
            // this method will be called.

            // Add your cpu-blocking activity here
            sendBroadcastMessage("Server starting...");

            try {
                //updateState("Starting UDP Server");
                socket = new DatagramSocket(UdpServerPORT);
                sendBroadcastMessage("Server socket created");
                //updateState("UDP Server is running");
                Log.e(TAG, "UDP Server is running");

                sendBroadcastMessage("Server listening");
                while (serverRunning) {
                    byte[] buf = new byte[32];
                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);     //this code block the program flow

                    String msgPacket = new String(packet.getData(), packet.getOffset(), packet.getLength());
                    sendBroadcastMessage(msgPacket);
                    if (msgPacket.contains("Alarm"))
                        generateNotification(MyService.this, "New Fish!", 1, false);
                }
                sendBroadcastMessage("Server stopped");
                Log.e(TAG, "UDP Server ended");
            } catch (SocketException e) {
                e.printStackTrace();
                sendBroadcastMessage(e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                sendBroadcastMessage(e.getMessage());
            } finally {
                if (socket != null) {
                    socket.close();
                    Log.e(TAG, "socket.close()");
                    sendBroadcastMessage("Server socket closed");
                }
            }
            // the msg.arg1 is the startId used in the onStartCommand, so we can track the running sevice here.
            stopSelf(msg.arg1);
        }
    }

    private void generateNotification(Context context, String message, Integer id, boolean onGoing) {
        int icon = R.drawable.notification_template_icon_bg;
        long when = System.currentTimeMillis();
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);

        String title = "Fish Alarm!";

        Intent notificationIntent = new Intent(context, MainActivity.class);
        try {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                // set intent so it does not start a new activity
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
                notification.setLatestEventInfo(context, title, message, intent);
                if (onGoing) {
                    notification.flags |= Notification.FLAG_ONGOING_EVENT;
                } else {
                    notification.ledARGB = 0xFF0000FF;
                    notification.ledOnMS = 1000;
                    notification.ledOffMS = 300;
                    notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                    notification.defaults |= Notification.DEFAULT_VIBRATE;
                    notification.defaults |= Notification.PRIORITY_MAX;
                }

            } else {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                notification = builder
                        .setOngoing(onGoing)
                        .setLights(0xFF0000FF, 1000, 300)
                        .setDefaults(Notification.DEFAULT_VIBRATE | Notification.PRIORITY_MAX)
                        .setSmallIcon(icon)
                        .setTicker(message)
                        .setWhen(when)
                        .setContentTitle(title)
                        .setContentText(message)
                        .build();
            }
            notificationManager.notify(id, notification);
        } catch (Exception e) {
            sendBroadcastMessage(e.getMessage());
        }

        if (id > 0) {

            try {
                RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).play();
            } catch (Exception e) {
                sendBroadcastMessage(e.getMessage());
            }

            try {
                turnOnFlash();
            } catch (Exception e) {
                sendBroadcastMessage(e.getMessage());
            }
        }
    }

    private void turnOnFlash() {
        Camera camera = null;
        Camera.Parameters params  = null;
        if (camera == null) {
            try {
                camera = Camera.open();
                params = camera.getParameters();
            } catch (RuntimeException e) {
                sendBroadcastMessage(e.getMessage());
            }
        }

        if (camera == null || params == null) {
            return;
        }

        params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        camera.setParameters(params);
        camera.startPreview();

    }


}