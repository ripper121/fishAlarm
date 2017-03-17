package uk23.ripper121.com.fishalarm;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;

public class MyService extends Service{



    private final static String TAG = MainActivity.class.getSimpleName();
    static final int UdpServerPORT = 8080;
    private boolean serverRunning = true;
    DatagramSocket socket;
    public static NotificationManager notificationManager = null;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

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
        generateNotification(MyService.this, "Fish Alarm Running",0);
    }

    @Override
    public void onDestroy() {
        notificationManager.cancel(0);
        serverRunning = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "onStartCommand", Toast.LENGTH_SHORT).show();

        // call a new service handler. The service ID can be used to identify the service
        Message message = mServiceHandler.obtainMessage();
        message.arg1 = startId;
        mServiceHandler.sendMessage(message);
        generateNotification(MyService.this, "New Fish!",0);
        return START_STICKY;
    }

    protected void showToast(final String msg){
        //gets the main thread
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                // run this code in the main thread
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
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

            showToast("Starting IntentService");

            try {
                //updateState("Starting UDP Server");
                socket = new DatagramSocket(UdpServerPORT);

                //updateState("UDP Server is running");
                Log.e(TAG, "UDP Server is running");

                while(serverRunning){
                    byte[] buf = new byte[256];

                    // receive request
                    DatagramPacket packet = new DatagramPacket(buf, buf.length);

                    socket.receive(packet);     //this code block the program flow

                    // send the response to the client at "address" and "port"
                    InetAddress address = packet.getAddress();
                    int port = packet.getPort();

                    //updatePrompt("Request from: " + address + ":" + port + "\n");

                    String dString = new Date().toString() + "\n"
                            + "Your address " + address.toString() + ":" + String.valueOf(port);
                    buf = dString.getBytes();
                    packet = new DatagramPacket(buf, buf.length, address, port);
                    socket.send(packet);
                    generateNotification(MyService.this, "New Fish!",1);

                }

                Log.e(TAG, "UDP Server ended");

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(socket != null){
                    socket.close();
                    Log.e(TAG, "socket.close()");
                }
            }
            showToast("Finishing Service, id: " + msg.arg1);
            // the msg.arg1 is the startId used in the onStartCommand, so we can track the running sevice here.
            stopSelf(msg.arg1);
        }
    }

    private static void generateNotification(Context context, String message, Integer id) {
        int icon = R.drawable.notification_template_icon_bg;
        long when = System.currentTimeMillis();
        notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(icon, message, when);

        String title = "Fish Alarm!";

        Intent notificationIntent = new Intent(context, MainActivity.class);
        // set intent so it does not start a new activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, title, message, intent);
        if(id==0) {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
        }else {
            notification.ledARGB = 0xFF0000FF;
            notification.ledOnMS = 1000;
            notification.ledOffMS = 300;
            notification.flags |= Notification.FLAG_SHOW_LIGHTS;

            // Play default notification sound
            notification.defaults |= Notification.DEFAULT_SOUND;
            // Vibrate if vibrate is enabled
            notification.defaults |= Notification.DEFAULT_VIBRATE;

            notification.defaults |= Notification.PRIORITY_MAX;
        }
        notificationManager.notify(id, notification);
    }



}