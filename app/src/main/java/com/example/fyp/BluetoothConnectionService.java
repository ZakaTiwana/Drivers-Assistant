package com.example.fyp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.ParcelUuid;
import android.util.Log;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

import com.github.pires.obd.commands.protocol.*;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;

import com.github.pires.obd.*;

/**
 * Created by User on 12/21/2016.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "MYAPP";

    //    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
  //  private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;



    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;


        public ConnectThread(String device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = mBluetoothAdapter.getRemoteDevice(device);

            ParcelUuid list[] = mmDevice.getUuids();
            deviceUUID  = UUID.fromString(list[0].toString());
           // deviceUUID = uuid;
            Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket for device: "
                    + device);

        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        + deviceUUID);

                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();

                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                Log.e(TAG, "mConnectThread: run: Unable to connect check " + e.getMessage());
                // Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + deviceUUID);
            }

            //           will talk about this in the 3rd video
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }


    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

    }

    /**
     * AcceptThread starts and sits waiting for a connection.
     * Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/

    public void startClient(String device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                , "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }
//

    /**
     * Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     * receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        //      private final InputStream mmInStream;
        //     private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            //  InputStream tmpIn = null;
            //   OutputStream tmpOut = null;

            //dismiss the progressdialog when connection is established
            try {
                mProgressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }


            try {
//                new EchoOffCommand().run(mmSocket.getInputStream(), socket.getOutputStream());
//
//                new LineFeedOffCommand().run(mmSocket.getInputStream(), socket.getOutputStream());
//
//                new TimeoutCommand(125).run(mmSocket.getInputStream(), socket.getOutputStream());
//
//                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());
//                ResetTroubleCodesCommand clear = new ResetTroubleCodesCommand();
//                clear.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
//                String result = clear.getFormattedResult();
//                Log.d(TAG,"Clear results"+result);


                //            onProgressUpdate(1);

                new ObdResetCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());


                //     onProgressUpdate(2);

                new EchoOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());

                //      onProgressUpdate(3);

                new LineFeedOffCommand().run(mmSocket.getInputStream(), mmSocket.getOutputStream());

                //       onProgressUpdate(4);

                new SelectProtocolCommand(ObdProtocols.AUTO).run(mmSocket.getInputStream(), mmSocket.getOutputStream());


            } catch (Exception e) {
                e.printStackTrace();
            }

            //   mmInStream = tmpIn;
            // mmOutStream = tmpOut;
        }

        public void run() {
            //   byte[] buffer = new byte[1024];  // buffer store for the stream

            //     int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                //       Read from the InputStream
                try {

                    RPMCommand engineRpmCommand = new RPMCommand();

                    SpeedCommand speedCommand = new SpeedCommand();
//
                    AmbientAirTemperatureCommand atc = new AmbientAirTemperatureCommand();
                    ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
                    //      FuelLevelCommand flc=new FuelLevelCommand();
                    while (!Thread.currentThread().isInterrupted()) {
                        engineRpmCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        speedCommand.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        atc.run(mmSocket.getInputStream(), mmSocket.getOutputStream());

                        tcoc.run(mmSocket.getInputStream(), mmSocket.getOutputStream());
                        //     String result = tcoc.getFormattedResult();
                        //       flc.run(mmSocket.getInputStream(),mmSocket.getOutputStream());
                        // TODO handle commands result
                        Log.d(TAG, "RPM: " + engineRpmCommand.getCalculatedResult());
                        Log.d(TAG, "Speed: " + speedCommand.getCalculatedResult());
                        Log.d(TAG, "Temp: " + atc.getCalculatedResult());
                        Log.d(TAG, "Trouble codes: " + tcoc.getFormattedResult());
                        //          Log.d(TAG, "Fuel level: " + flc.getFormattedResult());

                    }
//                    bytes = mmInStream.read(buffer);
//                    String incomingMessage = new String(buffer, 0, bytes);
//                    Log.d(TAG, "InputStream: " + incomingMessage);
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                    break;
                }
            }
        }


        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {
        @Override
        public String getResult() {
            // remove unwanted response from output since this results in erroneous error codes
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }


}




