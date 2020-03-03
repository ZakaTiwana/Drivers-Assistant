package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.control.TroubleCodesCommand;
import com.github.pires.obd.*;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnableToConnectException;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "Bluetooth";
    ImageView backbutton;


    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public ArrayList<BluetoothDevice> mBTDevices2 = new ArrayList<>();


    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice mBTDevice;
    BluetoothConnectionService mBluetoothConnection;

    private GetTroubleCodesTask gtct;

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "check2");
            Log.d(TAG, "onReceive: ACTION FOUND.");
//            Toast.makeText(getApplicationContext(), "Check66", Toast.LENGTH_SHORT).show();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                boolean flag = false;
                for (int i = 0; i < mBTDevices2.size(); i++) {
                    if (device.getAddress().equals(mBTDevices2.get(i).getAddress())) {
                        flag = true;
                        break;
                    }
                }
                for (int i = 0; i < mBTDevices.size(); i++) {
                    if (device.getAddress().equals(mBTDevices.get(i).getAddress())) {
                        flag = true;
                        break;
                    }
                }
                if (!flag && device.getName() != null) {
                    mBTDevices2.add(device);
                }


                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                ListView lv12;
                lv12 = (ListView) findViewById(R.id.lv2);

                lv12.setAdapter(new CustomList2(context, R.layout.bt_row, mBTDevices2));
                lv12.setOnItemClickListener(Bluetooth.this);

            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        backbutton = (ImageView) findViewById(R.id.backbtn1);
        backbutton.setOnClickListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        //   List<BluetoothDevice> s = new ArrayList<>();
        for (BluetoothDevice bt : pairedDevices)
            mBTDevices.add(bt);


        ListView lv1 = (ListView) findViewById(R.id.lv);

        lv1.setAdapter(new CustomList(this, R.layout.bt_row, mBTDevices));
        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                Log.d(TAG, "onItemClick: You Clicked on a device.");
                String deviceName = mBTDevices.get(i).getName();
                String deviceAddress = mBTDevices.get(i).getAddress();


                Log.d(TAG, "onItemClick: deviceName = " + deviceName);
                Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

                //create the bond.
                //NOTE: Requires API 17+? I think this is JellyBean
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "Trying to pair with " + deviceName);
                    //  mBTDevices2.get(i).createBond();
                    //             mBTDevices.get(i).createBond();
                    mBTDevice = mBTDevices.get(i);
                    mBluetoothConnection = new BluetoothConnectionService(Bluetooth.this);

//                    gtct = new GetTroubleCodesTask();
//                    gtct.execute();
                    startConnection();

                }
            }
        });


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);


        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");

            //check BT permissions in manifest
            //           checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if (!mBluetoothAdapter.isDiscovering()) {

            //check BT permissions in manifest
            //           checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "onClick: blue = " + mBluetoothAdapter.isDiscovering());

            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);

            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);


        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);

    }

    //create method for starting connection
//***remember the conncction will fail and app will crash if you haven't paired first
    public void startConnection() {
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device, uuid);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName2 = mBTDevices2.get(i).getName();
        String deviceAddress2 = mBTDevices2.get(i).getAddress();


        Log.d(TAG, "onItemClick: deviceName = " + deviceName2);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress2);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "Trying to pair with " + deviceName2);
            //  mBTDevices2.get(i).createBond();
            mBTDevices2.get(i).createBond();
            mBTDevice = mBTDevices2.get(i);
            mBluetoothConnection = new BluetoothConnectionService(Bluetooth.this);
            startConnection();
        }

//        String deviceName = mBTDevices.get(i).getName();
//        String deviceAddress = mBTDevices.get(i).getAddress();
//        Log.d(TAG, "Device Name: " + deviceAddress);
//        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
//
//        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//
//        BluetoothSocket socket = null;
//        try {
//            socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            socket.connect();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == backbutton.getId()) {
            finish();
        }
    }

    class CustomList extends ArrayAdapter<BluetoothDevice> {

        public CustomList(Context context, int resource, ArrayList<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            View v = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.bt_row, null);

            //    Toast.makeText(getApplicationContext(), "Check", Toast.LENGTH_SHORT).show();

            BluetoothDevice device = mBTDevices.get(position);

            if (device != null) {
                if (device.getName() != null) {
                    TextView deviceName = (TextView) v.findViewById(R.id.tvDeviceName);
                    TextView deviceAdress = (TextView) v.findViewById(R.id.tvDeviceAddress);

                    if (deviceName != null) {
                        deviceName.setText(device.getName());
                    }
                    if (deviceAdress != null) {
                        deviceAdress.setText(device.getAddress());
                    }
                }
            }

            return v;
        }
    }


    class CustomList2 extends ArrayAdapter<BluetoothDevice> {

        public CustomList2(Context context, int resource, ArrayList<BluetoothDevice> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {


            View v = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.bt_row, null);

            //  Toast.makeText(getApplicationContext(), "Check", Toast.LENGTH_SHORT).show();
            BluetoothDevice device = mBTDevices2.get(position);

            if (device != null) {

                TextView deviceName = (TextView) v.findViewById(R.id.tvDeviceName);
                TextView deviceAdress = (TextView) v.findViewById(R.id.tvDeviceAddress);

                if (deviceName != null) {
                    deviceName.setText(device.getName());
                }
                if (deviceAdress != null) {
                    deviceAdress.setText(device.getAddress());
                }

            }

            return v;
        }
    }

    public class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {
        @Override
        public String getResult() {
            // remove unwanted response from output since this results in erroneous error codes
            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
        }
    }

    private ProgressDialog progressDialog;
    private BluetoothSocket sock = null;

    private class GetTroubleCodesTask extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            //Create a new progress dialog
            progressDialog = new ProgressDialog(Bluetooth.this);
            //Set the progress dialog to display a horizontal progress bar
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //Set the dialog title to 'Loading...'
            progressDialog.setTitle("getString(R.string.dialog_loading_title)");
            //Set the dialog message to 'Loading application View, please wait...'
            progressDialog.setMessage("getString(R.string.dialog_loading_body)");
            //This dialog can't be canceled by pressing the back key
            progressDialog.setCancelable(false);
            //This dialog isn't indeterminate
            progressDialog.setIndeterminate(false);
            //The maximum number of items is 100
            progressDialog.setMax(5);
            //Set the current progress to zero
            progressDialog.setProgress(0);
            //Display the progress dialog
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "kuch nai aya";

            //Get the current thread's token
            synchronized (this) {
                Log.d(TAG, "Starting service..");
                // get the remote Bluetooth device

                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();


                Log.d(TAG, "Stopping Bluetooth discovery.");
                btAdapter.cancelDiscovery();

                Log.d(TAG, "Starting OBD connection..");

                // Instantiate a BluetoothSocket for the remote device and connect it.
                try {

                    sock = mBTDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                    sock.connect();
                    Log.d(TAG, "run: ConnectThread connected.");
                } catch (Exception e) {
                    Log.e(
                            TAG,
                            "There was an error while establishing connection. -> "
                                    + e.getMessage()
                    );
                    Log.d(TAG, "Message received on handler here");
                    // mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
                    return null;
                }

                try {
                    // Let's configure the connection.
                    Log.d(TAG, "Queueing jobs for connection configuration..");

                    onProgressUpdate(1);

                    new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());


                    onProgressUpdate(2);

                    new EchoOffCommand().run(sock.getInputStream(), sock.getOutputStream());

                    onProgressUpdate(3);

                    new LineFeedOffCommand().run(sock.getInputStream(), sock.getOutputStream());

                    onProgressUpdate(4);

                    new SelectProtocolCommand(ObdProtocols.AUTO).run(sock.getInputStream(), sock.getOutputStream());

                    onProgressUpdate(5);

                    ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
                    tcoc.run(sock.getInputStream(), sock.getOutputStream());
                    result = tcoc.getFormattedResult();

                    onProgressUpdate(6);

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("DTCERR1", e.getMessage());
                    //  mHandler.obtainMessage(OBD_COMMAND_FAILURE_IO).sendToTarget();
                    return null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e("DTCERR2", e.getMessage());
                    //   mHandler.obtainMessage(OBD_COMMAND_FAILURE_IE).sendToTarget();
                    return null;
                } catch (UnableToConnectException e) {
                    e.printStackTrace();
                    Log.e("DTCERR3", e.getMessage());
                    //   mHandler.obtainMessage(OBD_COMMAND_FAILURE_UTC).sendToTarget();
                    return null;
                } catch (MisunderstoodCommandException e) {
                    e.printStackTrace();
                    Log.e("DTCERR4", e.getMessage());
                    //     mHandler.obtainMessage(OBD_COMMAND_FAILURE_MIS).sendToTarget();
                    return null;
                } catch (NoDataException e) {
                    Log.e("DTCERR5", e.getMessage());
                    //     mHandler.obtainMessage(OBD_COMMAND_FAILURE_NODATA).sendToTarget();
                    return null;
                } catch (Exception e) {
                    Log.e("DTCERR6", e.getMessage());
                    //      mHandler.obtainMessage(OBD_COMMAND_FAILURE).sendToTarget();
                } finally {

                    // close socket
                    closeSocket(sock);
                }

            }

            return result;
        }

        public void closeSocket(BluetoothSocket sock) {
            if (sock != null)
                // close socket
                try {
                    sock.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            progressDialog.dismiss();


            //  mHandler.obtainMessage(DATA_OK, result).sendToTarget();
            //    setContentView(R.layout.trouble_codes);
            Log.d(TAG, "Trouble codes" + result);

        }
    }


}
