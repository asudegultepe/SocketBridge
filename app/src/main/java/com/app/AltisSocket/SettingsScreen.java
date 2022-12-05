package com.app.AltisSocket;

import android.content.Intent;
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
    EditText mPort;

    Button back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Shared Preferences
        mIP = (EditText) findViewById(R.id.ipAddress);
        mPort = (EditText) findViewById(R.id.port);
        saveData = (Button) findViewById(R.id.save_prenferences);
        back = (Button) findViewById(R.id.back_button);

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
                // Save Port
                String port_input = mPort.getText().toString();
                mEditor.putString(getString(R.string.port_input), port_input);
                mEditor.commit();

                Toast.makeText(SettingsScreen.this, "Preferences saved.", Toast.LENGTH_LONG).show();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent reload = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(reload);
            }
        });

    }

    private void checkSharedPreferences() {
        String ip_address = mPreferences.getString(getString(R.string.ip_address), "");
        String port_input = mPreferences.getString(getString(R.string.port_input), "");

        mIP.setText(ip_address);
        mPort.setText(port_input);
    }

    }

