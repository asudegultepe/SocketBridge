package com.app.AltisSocket;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import AltisSocket.R;

public class SettingsScreen extends AppCompatActivity {
    private static final String TAG = "SettingsScreen";

    Button saveData;
    SharedPreferences mPreferences;
    SharedPreferences.Editor mEditor;
    EditText mIP;
    EditText mPortClient;
    EditText mPortDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //Back Button
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Shared Preferences
        mIP = (EditText) findViewById(R.id.ipAddress);
        mPortClient = (EditText) findViewById(R.id.port);
        mPortDevice = (EditText) findViewById(R.id.device_port);
        saveData = (Button) findViewById(R.id.save_prenferences);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mEditor = mPreferences.edit();

        checkSharedPreferences();

        saveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save IP Address
                String ip_address = mIP.getText().toString();
                mEditor.putString(getString(R.string.ip_address), ip_address);
                mEditor.commit();
                // Save Client Port
                String port_input = mPortClient.getText().toString();
                mEditor.putString(getString(R.string.port_input), port_input);
                mEditor.commit();
                // Save Device Port
                String port_device = mPortDevice.getText().toString();
                mEditor.putString(getString(R.string.port_device), port_device);
                mEditor.commit();

                Toast.makeText(SettingsScreen.this, "Preferences saved.", Toast.LENGTH_LONG).show();
            }
        });

    }


    private void checkSharedPreferences() {
        String ip_address = mPreferences.getString(getString(R.string.ip_address), "");
        String port_input = mPreferences.getString(getString(R.string.port_input), "");
        String port_device = mPreferences.getString(getString(R.string.port_device), "");

        mIP.setText(ip_address);
        mPortClient.setText(port_input);
        mPortDevice.setText(port_device);
    }
}

