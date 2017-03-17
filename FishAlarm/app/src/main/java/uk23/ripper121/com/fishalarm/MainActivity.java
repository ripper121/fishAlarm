package uk23.ripper121.com.fishalarm;

import android.app.Service;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.List;


public class MainActivity extends Activity {
    public TextView text1;
    public TextView text2;
    public ScrollView mScrollView;
    public static Handler mUiHandler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text1 = (TextView) findViewById(R.id.textView1);
        text1.setMovementMethod(new ScrollingMovementMethod());
        text2 = (TextView) findViewById(R.id.textView2);
        text2.setGravity(Gravity.CENTER_HORIZONTAL);
        text2.setText("IP: " + getIpAddress() + " Port:8080");


        final Intent service = new Intent(this, MyService.class);

        final Button buttonStart = (Button) findViewById(R.id.start_service);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(service);
            }
        });

        final Button buttonStop = (Button) findViewById(R.id.stop_service);
        buttonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(service);
            }
        });

        mScrollView = (ScrollView) findViewById(R.id.SCROLLER_ID);
        mUiHandler = new Handler() // Receive messages from service class
        {
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                    case 0:
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                        text1.append(currentDateTimeString + " // " + msg.obj.toString() + "\n");
                        scrollToBottom();
                        break;

                    default:
                        break;
                }
            }
        };
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
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }


    private void scrollToBottom()
    {
        mScrollView.post(new Runnable()
        {
            public void run()
            {
                mScrollView.smoothScrollTo(0, text1.getBottom());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

}