package com.enel400.xiang.hydroping;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by xiang on 2019-02-21.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "HC-05";
    //UUID could be different based on what bluetooth module is used.
    //For serial board: 00001101-0000-1000-8000-00805F9B34FB
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter serviceBluetoothAdapter;
    Context myContext;
    private AcceptThread myInsecureAcceptThread;
    private ConnectThread myConnectThread;
    private BluetoothDevice myDevice;
    private UUID deviceUUID;
    ProgressDialog myProgressDialog;
    private ConnectedThread myConnectedThread;

    public BluetoothConnectionService (Context context)
    {
        myContext = context;
        serviceBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    private class AcceptThread extends Thread
    {
        private final BluetoothServerSocket myServerSocket;
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = serviceBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG,"AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
            }
            catch(IOException e)
            {
                Log.e(TAG,"AcceptThread: IOException: "+ e.getMessage());
            }
            myServerSocket = tmp;
        }
        public void run()
        {
            Log.d(TAG,"run: AcceptThread Running.");
            BluetoothSocket socket = null;
            try
            {
                //This is a blocking call and will only return on a successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start......");
                socket = myServerSocket.accept();
                Log.d(TAG,"RFCOM server socket accepted connection.");
            }
            catch(IOException e)
            {
                Log.d(TAG,"AcceptThread: IOException: " + e.getMessage());
            }
            if (socket != null)
            {
                connected(socket,myDevice);
            }
            Log.i(TAG,"END myAcceptThread");
        }
        public void cancel()
        {
            Log.d(TAG,"cancel: Cancelling AcceptThread.");
            try
            {
                myServerSocket.close();
            }
            catch(IOException e)
            {
                Log.e(TAG,"cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }
    private class ConnectThread extends Thread
    {
        private BluetoothSocket mySocket;
        public ConnectThread(BluetoothDevice device, UUID uuid)
        {
            Log.d(TAG,"ConnectThread: started");
            myDevice = device;
            deviceUUID = uuid;
        }
        public void run()
        {
            BluetoothSocket tmp = null;
            Log.i(TAG,"RUN mConnectThread");
            try
            {

                Log.d(TAG,"ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + MY_UUID_INSECURE);
                tmp = myDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }
            catch(IOException e)
            {
                Log.e(TAG,"ConnectThread: Could not create InsecureRfcommSocket" + e.getMessage());
            }
            mySocket = tmp;
            //Always cancel discovery because it will slow down a connection
            serviceBluetoothAdapter.cancelDiscovery();
            //Blocking call, only returns on successful connection or an exception
            try
            {
                mySocket.connect();
                Log.d(TAG,"run: ConnectThread connected");
            }
            catch (IOException e)
            {
                //Close socket.
                try
                {
                    mySocket.close();
                    Log.d(TAG,"run: Closed socket.");
                }
                catch (IOException e1)
                {
                    Log.e(TAG,"mConnectThread: run: unable to close connection in socket "+ e1.getMessage());
                }
                Log.d(TAG,"run: ConnectThread: Could not connect to UUID: "+ MY_UUID_INSECURE);

            }
            connected(mySocket,myDevice);
        }
        public void cancel()
        {
            try
            {
                Log.d(TAG,"cancel: Closing client socket.");
                mySocket.close();
            }
            catch (IOException e)
            {
                Log.e(TAG,"cancel: close() of mySocket in ConnectThread failed." + e.getMessage());
            }
        }
    }
    public synchronized void start()
    {
        Log.d(TAG,"Start");
        if(myConnectThread!=null)
        {
            myConnectThread.cancel();
            myConnectThread = null;
        }
        if(myInsecureAcceptThread == null)
        {
            myInsecureAcceptThread = new AcceptThread();
            myInsecureAcceptThread.start();
        }
    }
    public void startClient(BluetoothDevice device, UUID uuid)
    {
        Log.d(TAG,"startClient: started.");
        myProgressDialog = ProgressDialog.show(myContext,"Connecting Bluetooth","Please Wait...",true);
        myConnectThread = new ConnectThread(device,uuid);
        myConnectThread.start();
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mySocket;
        private final InputStream myInStream;
        private final OutputStream myOutStream;
        public ConnectedThread(BluetoothSocket socket)
        {
            Log.d(TAG,"ConnectedThread: starting.");
            mySocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                myProgressDialog.dismiss();
            }
            catch (NullPointerException e)
            {
                e.printStackTrace();
            }
            try
            {
                tmpIn = mySocket.getInputStream();
                tmpOut = mySocket.getOutputStream();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            myInStream = tmpIn;
            myOutStream = tmpOut;
        }
        public void run()
        {
            byte[] buffer = new byte[1024]; //buffer store for the stream
            int bytes; // bytes returned from read()
            while (true)
            {
                try
                {
                    bytes = myInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0 , bytes);
                    Log.d(TAG,"InputStream: " + incomingMessage);
                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(myContext).sendBroadcast(incomingMessageIntent);
                }
                catch (IOException e)
                {
                    Log.e(TAG,"write: Error reading from inputStream.." + e.getMessage());
                    break;
                }
            }
        }
        public void write(byte[] bytes)
        {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"write: Writing to outputStream: " + text);
            try
            {
                myOutStream.write(bytes);
            }
            catch (IOException e) {
               Log.e(TAG,"write: Error writing to outputStream." + e.getMessage());
            }
        }
        public void cancel()
        {
            try
            {
                mySocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private void connected(BluetoothSocket mySocket, BluetoothDevice myDevice)
    {
        Log.d(TAG,"connected: starting");
        myConnectedThread = new ConnectedThread(mySocket);
        myConnectedThread.start();
    }

    public void write(byte[] out)
    {
        ConnectedThread r;
        Log.d(TAG,"write: write called.");
        myConnectedThread.write(out);
    }

}
