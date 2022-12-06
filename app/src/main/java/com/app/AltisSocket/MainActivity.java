package com.app.AltisSocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.app.p2lbridge.bridge.StreamBridge;
import com.app.p2lbridge.sockets.SocketClient;
import com.app.p2lbridge.sockets.SocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import AltisSocket.R;

public class MainActivity extends AppCompatActivity {

    EditText e1;
    static TextView t1;

    TextView mIP;
    TextView mPortClient;

    Button settingsBtn;
    static TextView textView;

    NotificationManagerCompat notificationManagerCompat;
    Notification notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        e1 = (EditText) findViewById(R.id.editText);
        t1 = (TextView) findViewById(R.id.textView1);

        mIP = (TextView) findViewById(R.id.ipAddress_MA);
        mPortClient = (TextView) findViewById(R.id.port_MA);
        settingsBtn = (Button) findViewById(R.id.idBtnSettings);


        // Get IP and Port info
        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = mPreferences.edit();

        String ip_address = mPreferences.getString(getString(R.string.ip_address), "");
        mIP.setText(ip_address);
        String port_input = mPreferences.getString(getString(R.string.port_input), "");
        mPortClient.setText(port_input);

        // Settings Page
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intentSetting = new Intent(MainActivity.this, SettingsScreen.class);
                startActivity(intentSetting);
            }
        });

        // MessageSender Thread
        Thread myThread = new Thread(new MyServerThread());
        myThread.start();

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        if (VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("myChannel", "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "myChannel")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Socket App")
                .setContentText("The service is running.");

        notification = builder.build();
        notificationManagerCompat = NotificationManagerCompat.from(this);

        Button button4 = findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                apiCall();
                System.out.println("hello");
            }
        });


    }

    class MyServerThread implements Runnable {
        Socket s;
        ServerSocket ss;
        InputStreamReader isr;
        BufferedReader bufferedReader;
        Handler h = new Handler();
        String message;
        String ipAddress;
        String port;

        @Override
        public void run() {
            try {
                ss = new ServerSocket(7801);
                while (true) {
                    s = ss.accept();
                    isr = new InputStreamReader(s.getInputStream());
                    bufferedReader = new BufferedReader(isr);
                    message = bufferedReader.readLine();
                    ipAddress = bufferedReader.readLine();
                    port = bufferedReader.readLine();

                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            if (t1.getText().equals("")) {
                                t1.setText("Android:  " + message);
                            } else {
                                t1.setText(t1.getText() + "\nAndroid:  " + message);
                            }
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onBackPressed() {
        /// put nothing
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void send(View v) {
        MessageSender messageSender = new MessageSender();
        messageSender.execute(e1.getText().toString(), mIP.getText().toString(), mPortClient.getText().toString());
    }

    public void clear(View v) {
        t1.setText("");
    }

    private void apiCall() {

        new AsyncCaller().execute(mIP.getText().toString(), mPortClient.getText().toString());
        notificationManagerCompat.notify(1, notification);

    }

    private class AsyncCaller extends AsyncTask<String, Void, Void>
    {
        @Override
        protected Void doInBackground(String... params) {

            //this method will be running on background thread so don't update UI frame here
            //do your long running http tasks here,you don't want to pass argument and u can access the parent class' variable url over here

            String ipAddress = params[0];
            Integer port = Integer.valueOf(params[1]);

            SocketClient atop = new SocketClient( ipAddress, port );
            SocketServer hostServer = new SocketServer(4661);

            System.out.println("Socket started.");

            new Thread(hostServer).start(); // start the server on a separate thread
            new Thread(atop).start();
            new Thread(new StreamBridge(atop, hostServer)).start(); // from serial to wifi
            new Thread(new StreamBridge(hostServer, atop)).start(); // from wifi to serial

            return null;
        }

    }

}