package uk23.ripper121.com.fishalarm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.media.RingtoneManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;


public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private TextView text1;
    private TextView text2;
    private ScrollView mScrollView;
    private TextView textView = null;
    private boolean apOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text1 = (TextView) findViewById(R.id.textView1);
        text1.setMovementMethod(new ScrollingMovementMethod());
        text2 = (TextView) findViewById(R.id.textView2);
        text2.setGravity(Gravity.CENTER_HORIZONTAL);

        textView = (TextView) findViewById(R.id.textView1);
        mScrollView = (ScrollView) findViewById(R.id.SCROLLER_ID);

        final Intent service = new Intent(this, MyService.class);

        final Button buttonStart = (Button) findViewById(R.id.start_service);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!checkAP()) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Access Point")
                                .setMessage("Please turn on the Access Point!")
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent tetherSettings = new Intent();
                                        tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                                        startActivity(tetherSettings);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        text2.setText("IP: " + getWifiApIpAddress() + " Port:8080");
                                        textboxAdd("Access Point / Tether OK");
                                        try {
                                            startService(service);
                                        } catch (Exception e) {
                                            textboxAdd(e.getMessage());
                                        }
                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    } else {
                        text2.setText("IP: " + getWifiApIpAddress() + " Port:8080");
                        textboxAdd("Access Point / Tether OK");
                        try {
                            startService(service);
                        } catch (Exception e) {
                            textboxAdd(e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    textboxAdd(e.getMessage());
                }

            }
        });

        final Button buttonStop = (Button) findViewById(R.id.stop_service);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    stopService(service);
                } catch (Exception e) {
                    textboxAdd(e.getMessage());
                }
            }
        });

        final Button buttonAlarm = (Button) findViewById(R.id.buttonAlarm);
        buttonAlarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    alarmOff();
                } catch (Exception e) {
                    textboxAdd(e.getMessage());
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String text = intent.getStringExtra(MyService.EXTRA_TEXT);
                        textboxAdd(text);
                    }
                }, new IntentFilter(MyService.ACTION_LOCATION_BROADCAST)
        );

        if (checkAP()) {
            text2.setText("IP: " + getWifiApIpAddress() + " Port:8080");
            textboxAdd("Access Point / Tether OK");
        } else {
            text2.setText("IP: " + getIpAddress() + " Port:8080");
            textboxAdd("Please turn on your Access Point / Tether");
        }
    }

    boolean checkAP() {
        WifiManager wifi = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Method[] wmMethods = wifi.getClass().getDeclaredMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isWifiApEnabled")) {
                try {
                    boolean isWifiAPenabled = (Boolean) method.invoke(wifi);
                    if (isWifiAPenabled) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    textboxAdd(e.getMessage());
                    return false;
                }
            }
        }
        return false;
    }

    void textboxAdd(String text) {
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        textView.append(currentDateTimeString + " // " + text + "\n");
        scrollToBottom();
    }

    void alarmOff() {
        try {
            RingtoneManager.getRingtone(MainActivity.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).stop();
            RingtoneManager ringtoneManager = new RingtoneManager(MainActivity.this);
            ringtoneManager.stopPreviousRingtone();
        } catch (Exception e) {
            textboxAdd(e.getMessage());
        }
        try {
            Camera camera = null;
            Camera.Parameters params = null;
            if (camera == null) {
                try {
                    camera = Camera.open();
                    params = camera.getParameters();
                } catch (RuntimeException e) {
                    textboxAdd(e.getMessage());
                }
            }
            if (camera == null || params == null) {
                return;
            }
            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();

            textboxAdd("Alarm turned Off!");

        } catch (Exception e) {
            textboxAdd(e.getMessage());
        }
    }


    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip = inetAddress.getHostAddress();
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip = "-.-.-.-";
            textboxAdd("Something Wrong! " + e.toString());
        }
        return ip;
    }

    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr
                            .hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
            textboxAdd(ex.toString());
        }
        return null;
    }


    private void scrollToBottom() {
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.smoothScrollTo(0, text1.getBottom());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}