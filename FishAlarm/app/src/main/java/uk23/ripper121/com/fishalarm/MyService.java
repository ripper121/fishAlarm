package uk23.ripper121.com.fishalarm;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class MyService extends Service{

    private static final String TAG = "MyService";

    //used for getting the handler from other class for sending messages
    public static Handler 		mMyServiceHandler 			= null;
    public static Handler       serverHandler 			= null;
    //used for keep track on Android running status
    public static Boolean 		mIsServiceRunning 			= false;

    public static boolean runUDP = true;
    public static DatagramSocket socket = null;
    public static DatagramPacket packet = null;

    public static NotificationManager notificationManager = null;



    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try
        {
            mIsServiceRunning = true; // set service running status = true
            MyThread myThread = new MyThread();
            myThread.start();

            runUDP=true;
            Thread myServer = new Thread(new Server());
            myServer.start();
            Thread.sleep(500);
            generateNotification(MyService.this, "Fish Alarm Running",0);
        }
        catch (InterruptedException e)
        {
            updatetrack("Server: " + e.getMessage() +"\n");
        }
        //Toast.makeText(this, "Congrats! My Service Started", Toast.LENGTH_SHORT).show();
        // We need to return if we want to handle this service explicitly.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        runUDP = false;
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            try {
                socket.disconnect();
                socket.close();
            } catch (Exception ignore) {}
            updatetrack("Server: Shutdown!");
            Log.d(TAG, "onDestroy");
            notificationManager.cancelAll();
            notificationManager = null;
            mIsServiceRunning = false; // make it false, as the service is already destroyed.
        } catch (Exception ignore) {}

    }

    //Your inner thread class is here to getting response from Activity and processing them
    class MyThread extends Thread
    {
        private static final String INNER_TAG = "MyThread";

        public void run()
        {
            this.setName(INNER_TAG);

            // Prepare the looper before creating the handler.
            Looper.prepare();
            mMyServiceHandler = new Handler()
            {
                //here we will receive messages from activity(using sendMessage() from activity)
                public void handleMessage(Message msg)
                {
                    Log.i("BackgroundThread","handleMessage(Message msg)" );
                    switch(msg.what)
                    {
                        case 0: // we sent message with what value =0 from the activity. here it is
                            //Reply to the activity from here using same process handle.sendMessage()
                            //So first get the Activity handler then send the message
                            break;
                        case 1:
                            if(null != MainActivity.mUiHandler)
                            {
                                //first build the message and send.
                                //put a integer value here and get it from the Activity handler
                                //For Example: lets use 0 (msg.what = 0;)
                                //for receiving service running status in the activity
                                Message msgToActivity = new Message();
                                msgToActivity.what = 0;
                                if(true ==mIsServiceRunning)
                                    msgToActivity.obj  = "Service Running!" + msg.obj ; // you can put extra message here
                                else
                                    msgToActivity.obj  = "Service is not Running!"; // you can put extra message here

                                MainActivity.mUiHandler.sendMessage(msgToActivity);
                            }
                            break;

                        default:
                            break;
                    }
                }
            };

            serverHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    String text = (String) msg.obj;
                    if(null != MainActivity.mUiHandler)
                    {
                        Message msgToActivity = new Message();
                        msgToActivity.what = 0;
                        if(true ==mIsServiceRunning)
                            msgToActivity.obj  = text ;
                        else
                            msgToActivity.obj  = "Service is not Running";

                        MainActivity.mUiHandler.sendMessage(msgToActivity);
                    }
                    if(text.contains("Alarm")) {
                        generateNotification(MyService.this, "New Fish!",1);
                    }

                }
            };

            Looper.loop();

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

    public void updatetrack(String s){
        Message msg = new Message();
        msg.obj = s;
        serverHandler.sendMessage(msg);
    }


    public class Server implements Runnable {
        @Override
        public void run() {
            String SERVERIP = "0.0.0.0";
            Integer SERVERPORT = 8080;
            InetAddress serverAddr=null;
            try {
                updatetrack("Server: Run");
                serverAddr = InetAddress.getByName(SERVERIP);
                updatetrack("Server: Connecting");
                try {
                    socket.disconnect();
                    socket.close();
                } catch (Exception ignore) {}
                socket = new DatagramSocket(SERVERPORT, serverAddr);

                byte[] buf = new byte[17];
                packet = new DatagramPacket(buf, buf.length);

                updatetrack("Server: Receiving");
                while (runUDP) {
                    if(!socket.isClosed()) {
                        try {
                            socket.receive(packet);
                            String msg = new String(packet.getData());
                            buf = new byte[17];
                            packet = new DatagramPacket(buf, buf.length);
                            updatetrack(msg);
                        } catch (Exception e) {
                            updatetrack("Server: " + e.getMessage() +"\n");
                        }
                    }
                }
            } catch (Exception e) {
                updatetrack("Server: " + e.getMessage() +"\n");
            }
        }}


}