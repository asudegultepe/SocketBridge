package com.app.AltisSocket;


import android.os.AsyncTask;
import java.io.DataOutput;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageSender extends AsyncTask<String, Void, Void>
{
    Socket s;
    DataOutput dos;
    PrintWriter pw;

    @Override
    protected Void doInBackground(String... voids) {
        String message = voids[0];
        String ipAddress = voids[1];
        Integer port = Integer.valueOf(voids[2]);
        System.out.println(ipAddress);
        System.out.println(message);
        System.out.println(port);

        try
        {
            s = new Socket( ipAddress, port);
            pw = new PrintWriter(s.getOutputStream());
            pw.write(message);
            pw.flush();
            pw.close();
            s.close();
        }catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}
