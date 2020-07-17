package com.example.fyp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//import com.github.pires.obd.commands.control.TroubleCodesCommand;
//import com.github.pires.obd.*;
//import com.github.pires.obd.commands.protocol.EchoOffCommand;
//import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
//import com.github.pires.obd.commands.protocol.ObdResetCommand;
//import com.github.pires.obd.commands.protocol.ResetTroubleCodesCommand;
//import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
//import com.github.pires.obd.enums.ObdProtocols;
//import com.github.pires.obd.exceptions.MisunderstoodCommandException;
//import com.github.pires.obd.exceptions.NoDataException;
//import com.github.pires.obd.exceptions.UnableToConnectException;

import com.example.fyp.customutilities.SharedPreferencesUtils;
import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.MisunderstoodCommandException;
import com.github.pires.obd.exceptions.NoDataException;
import com.github.pires.obd.exceptions.UnableToConnectException;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Bluetooth extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "Bluetooth";
    ImageView backbutton;


    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public ArrayList<BluetoothDevice> mBTDevices2 = new ArrayList<>();


    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    BluetoothDevice mBTDevice;
    BluetoothConnectionService mBluetoothConnection;

    private GetTroubleCodesTask gtct;


//    private static final int NO_BLUETOOTH_DEVICE_SELECTED = 0;
//    private static final int CANNOT_CONNECT_TO_DEVICE = 1;
//    private static final int NO_DATA = 3;
//    private static final int DATA_OK = 4;
//    private static final int CLEAR_DTC = 5;
//    private static final int OBD_COMMAND_FAILURE = 10;
//    private static final int OBD_COMMAND_FAILURE_IO = 11;
//    private static final int OBD_COMMAND_FAILURE_UTC = 12;
//    private static final int OBD_COMMAND_FAILURE_IE = 13;
//    private static final int OBD_COMMAND_FAILURE_MIS = 14;
//    private static final int OBD_COMMAND_FAILURE_NODATA = 15;
//    private BluetoothSocket sock = null;
//    private BluetoothDevice dev = null;
//    private ProgressDialog progressDialog;
//
//
//    private Handler mHandler = new Handler(new Handler.Callback() {
//
//
//        public boolean handleMessage(Message msg) {
//            Log.e("activity","TroubleCodesActivity.java : Handler : handleMessage");
//            Log.d(TAG, "Message received on handler");
//            switch (msg.what) {
//                case NO_BLUETOOTH_DEVICE_SELECTED:
//                   makeToast("getString(R.string.text_bluetooth_nodevice)");
//                    finish();
//                    break;
//                case CANNOT_CONNECT_TO_DEVICE:
//                    makeToast("getString(R.string.text_bluetooth_error_connecting)");
//                    finish();
//                    break;
//
//                case OBD_COMMAND_FAILURE:
//                  makeToast("getString(R.string.text_obd_command_failure)");
//                    finish();
//                    break;
//                case OBD_COMMAND_FAILURE_IO:
//                  makeToast("getString(R.string.text_obd_command_failure) +  IO");
//                    finish();
//                    break;
//                case OBD_COMMAND_FAILURE_IE:
//                   makeToast("getString(R.string.text_obd_command_failure) +  IE");
//                    finish();
//                    break;
//                case OBD_COMMAND_FAILURE_MIS:
//                   makeToast("getString(R.string.text_obd_command_failure) +  MIS");
//                    finish();
//                    break;
//                case OBD_COMMAND_FAILURE_UTC:
//                  makeToast("getString(R.string.text_obd_command_failure) +  UTC");
//                    finish();
//                    break;
//                case OBD_COMMAND_FAILURE_NODATA:
//                   makeToastLong("getString(R.string.text_noerrors)");
//                    finish();
//                    break;
//
//                case NO_DATA:
//                  makeToast("getString(R.string.text_dtc_no_data)");
//                    ///finish();
//                    break;
//                case DATA_OK:
//                 dataOk((String) msg.obj);
//                    break;
//
//            }
//            return false;
//        }
//    });

    public Bluetooth() {
        gtct = new GetTroubleCodesTask();
    }

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
                    Toast.makeText(getApplicationContext(), "Paired Successfully", Toast.LENGTH_SHORT).show();

                    mBTDevice = mDevice;
                    Bluetooth.this.recreate();
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

        SharedPreferences settings = getSharedPreferences("home_settings", 0);
        boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
        if (!darkModeUi_value) {
            ConstraintLayout constLayout;
            constLayout = findViewById(R.id.bluetooth);
            constLayout.setBackgroundResource(R.drawable.backgroundimage8);
            backbutton.setImageResource(R.drawable.ic_back_button_black);

            TextView tv1 = (TextView) findViewById(R.id.textView2);
            tv1.setTextColor(getResources().getColor(R.color.dark_grey));
            TextView tv2 = (TextView) findViewById(R.id.textView3);
            tv2.setTextColor(getResources().getColor(R.color.dark_grey));
            TextView tv3 = (TextView) findViewById(R.id.lane_text_view);
            tv3.setTextColor(getResources().getColor(R.color.dark_grey));

        }

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
                    //mBTDevices.get(i).createBond();
                    mBTDevice = mBTDevices.get(i);
                    //    mBluetoothConnection = new BluetoothConnectionService(Bluetooth.this);

//                    gtct = new GetTroubleCodesTask();
//                    gtct.execute(mBTDevice.getAddress());
//                    gtct = new GetTroubleCodesTask();
                    if(mBTDevice.getAddress().equals("00:00:00:33:33:33")||mBTDevice.getName().equalsIgnoreCase("OBDII")) {
                        gtct.execute(mBTDevice.getAddress());
                    }else {
                        Toast.makeText(getApplicationContext(), "Please select the OBDII device from the list.", Toast.LENGTH_SHORT).show();
                    }
                    //          startConnection();

                    //           connectToDevice();

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

        mBluetoothConnection.startClient(device.getAddress(), uuid);
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
            // Toast.makeText(this,"Paired Successfully!",Toast.LENGTH_SHORT).show();
            mBTDevice = mBTDevices2.get(i);


            //        mBluetoothConnection = new BluetoothConnectionService(Bluetooth.this);
//            gtct = new GetTroubleCodesTask();
//            gtct.execute("remoteDevice");

            //    startConnection();
            //connectToDevice();
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
                    SharedPreferences settings = getSharedPreferences("home_settings", 0);
                    boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
                    if (!darkModeUi_value) {
                        deviceName.setTextColor(getResources().getColor(R.color.dark_grey));
                        deviceAdress.setTextColor(getResources().getColor(R.color.dark_grey));
                        ImageView img1 = (ImageView) v.findViewById(R.id.imageView2);
                        img1.setImageResource(R.drawable.ic_bluetooth_blue);
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
                SharedPreferences settings = getSharedPreferences("home_settings", 0);
                boolean darkModeUi_value = settings.getBoolean("ui_settings", false);
                if (!darkModeUi_value) {
                    deviceName.setTextColor(getResources().getColor(R.color.dark_grey));
                    deviceAdress.setTextColor(getResources().getColor(R.color.dark_grey));
                    ImageView img1 = (ImageView) v.findViewById(R.id.imageView2);
                    img1.setImageResource(R.drawable.ic_bluetooth_blue);
                }

            }

            return v;
        }
    }


//    private void  connectBtDevice(){
//        try {
//            BluetoothSocket sockFallback = null;
//
//            Log.d(TAG, "Starting Bluetooth connection..");
//            try {
//                sock = mBTDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
//                sock.connect();
//            } catch (Exception e1) {
//                Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
//                Class<?> clazz = sock.getRemoteDevice().getClass();
//                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
//                try {
//                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
//                    Object[] params = new Object[]{Integer.valueOf(1)};
//                    sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
//                    sockFallback.connect();
//                    sock = sockFallback;
//                } catch (Exception e2) {
//                    Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
//                    throw new IOException(e2.getMessage());
//                }
//            }
//        } catch (Exception e) {
//            Log.e(
//                    TAG,
//                    "There was an error while establishing connection. -> "
//                            + e.getMessage()
//            );
//            Log.d(TAG, "Message received on handler here");
//  ///          mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
//  ///          return true;
//        }
//        try {
//
//            Log.d("TESTRESET", "Trying reset");
//            //new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());
//            ResetTroubleCodesCommand clear = new ResetTroubleCodesCommand();
//            clear.run(sock.getInputStream(), sock.getOutputStream());
//            String result = clear.getFormattedResult();
//            Log.d("TESTRESET", "Trying reset result: " + result);
//        } catch (Exception e) {
//            Log.e(
//                    TAG,
//                    "There was an error while establishing connection. -> "
//                            + e.getMessage()
//            );
//        }
//        gtct.closeSocket(sock);
//        // Refresh main activity upon close of dialog box
//        Intent refresh = new Intent(this, Bluetooth.class);
//        startActivity(refresh);
//        this.finish(); //
//
//    }
//
//    public class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {
//        @Override
//        public String getResult() {
//            // remove unwanted response from output since this results in erroneous error codes
//            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
//        }
//    }
//
//    private ProgressDialog progressDialog;
//    private BluetoothSocket sock = null;
//
//    private class GetTroubleCodesTask extends AsyncTask<String, Integer, String> {
//
//        @Override
//        protected void onPreExecute() {
//            //Create a new progress dialog
//            progressDialog = new ProgressDialog(Bluetooth.this);
//            //Set the progress dialog to display a horizontal progress bar
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            //Set the dialog title to 'Loading...'
//            progressDialog.setTitle("getString(R.string.dialog_loading_title)");
//            //Set the dialog message to 'Loading application View, please wait...'
//            progressDialog.setMessage("getString(R.string.dialog_loading_body)");
//            //This dialog can't be canceled by pressing the back key
//            progressDialog.setCancelable(false);
//            //This dialog isn't indeterminate
//            progressDialog.setIndeterminate(false);
//            //The maximum number of items is 100
//            progressDialog.setMax(5);
//            //Set the current progress to zero
//            progressDialog.setProgress(0);
//            //Display the progress dialog
//            progressDialog.show();
//        }
//
//        @Override
//        protected String doInBackground(String... params) {
//            String result = "kuch nai aya";
//
//            //Get the current thread's token
//            synchronized (this) {
//                Log.d(TAG, "Starting service..");
//                // get the remote Bluetooth device
//
//                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//
//
//                Log.d(TAG, "Stopping Bluetooth discovery.");
//                btAdapter.cancelDiscovery();
//
//                Log.d(TAG, "Starting OBD connection..");
//
//                // Instantiate a BluetoothSocket for the remote device and connect it.
//                try {
//
//                    sock = mBTDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
//                    sock.connect();
//                    Log.d(TAG, "run: ConnectThread connected.");
//                } catch (Exception e) {
//                    Log.e(
//                            TAG,
//                            "There was an error while establishing connection. -> "
//                                    + e.getMessage()
//                    );
//                    Log.d(TAG, "Message received on handler here");
//                    // mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
//                    return null;
//                }
//
//                try {
//                    // Let's configure the connection.
//                    Log.d(TAG, "Queueing jobs for connection configuration..");
//
//                    //    onProgressUpdate(1);
//
//                    new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//
//                    //     onProgressUpdate(2);
//
//                    new EchoOffCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//                    //      onProgressUpdate(3);
//
//                    new LineFeedOffCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//                    //        onProgressUpdate(4);
//
//                    new SelectProtocolCommand(ObdProtocols.AUTO).run(sock.getInputStream(), sock.getOutputStream());
//
//                    //       onProgressUpdate(5);
//
//                    ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
//                    tcoc.run(sock.getInputStream(), sock.getOutputStream());
//                    result = tcoc.getFormattedResult();
//
//                    onProgressUpdate(6);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR1", e.getMessage());
//                    //  mHandler.obtainMessage(OBD_COMMAND_FAILURE_IO).sendToTarget();
//                    return null;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR2", e.getMessage());
//                    //   mHandler.obtainMessage(OBD_COMMAND_FAILURE_IE).sendToTarget();
//                    return null;
//                } catch (UnableToConnectException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR3", e.getMessage());
//                    //   mHandler.obtainMessage(OBD_COMMAND_FAILURE_UTC).sendToTarget();
//                    return null;
//                } catch (MisunderstoodCommandException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR4", e.getMessage());
//                    //     mHandler.obtainMessage(OBD_COMMAND_FAILURE_MIS).sendToTarget();
//                    return null;
//                } catch (NoDataException e) {
//                    Log.e("DTCERR5", e.getMessage());
//                    //     mHandler.obtainMessage(OBD_COMMAND_FAILURE_NODATA).sendToTarget();
//                    return null;
//                } catch (Exception e) {
//                    Log.e("DTCERR6", e.getMessage());
//                    //      mHandler.obtainMessage(OBD_COMMAND_FAILURE).sendToTarget();
//                } finally {
//
//                    // close socket
//                    closeSocket(sock);
//                }
//
//            }
//
//            return result;
//        }
//
//        public void closeSocket(BluetoothSocket sock) {
//            if (sock != null)
//                // close socket
//                try {
//                    sock.close();
//                } catch (IOException e) {
//                    Log.e(TAG, e.getMessage());
//                }
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            super.onProgressUpdate(values);
//            progressDialog.setProgress(values[0]);
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            progressDialog.dismiss();
//
//
//            //  mHandler.obtainMessage(DATA_OK, result).sendToTarget();
//            //    setContentView(R.layout.trouble_codes);
//            Log.d(TAG, "Trouble codes" + result);
//
//        }
//    }


//    Map<String, String> getDict(int keyId, int valId) {
//        Log.e("activity", "TroubleCodesActivity.java : getDict");
//        String[] keys = getResources().getStringArray(keyId);
//        String[] vals = getResources().getStringArray(valId);
//
//        Map<String, String> dict = new HashMap<String, String>();
//        for (int i = 0, l = keys.length; i < l; i++) {
//            dict.put(keys[i], vals[i]);
//        }
//
//        return dict;
//    }


//    public void makeToast(String text) {
//        Log.e("activity","TroubleCodesActivity.java : makeToast");
//        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
//        toast.show();
//    }
//    public void makeToastLong(String text) {
//        Log.e("activity","TroubleCodesActivity.java : makeToastLong");
//        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
//        toast.show();
//    }
//    private void dataOk(String res) {
//        Log.e("activity","TroubleCodesActivity.java : dataOk");
//     //   ListView lv = (ListView) findViewById(R.id.listView);
//    //    Map<String, String> dtcVals = getDict(R.array.dtc_keys, R.array.dtc_values);
//        //TODO replace below codes (res) with aboce dtcVals
//        //String tmpVal = dtcVals.get(res.split("\n"));
//        //String[] dtcCodes = new String[]{};
//        ArrayList<String> dtcCodes = new ArrayList<String>();
//        //int i =1;
//        if (res != null) {
//            for (String dtcCode : res.split("\n")) {
//       //         dtcCodes.add(dtcCode + " : " + dtcVals.get(dtcCode));
//     //           Log.d("TEST", dtcCode + " : " + dtcVals.get(dtcCode));
//            }
//        } else {
//            dtcCodes.add("There are no errors");
//        }
//        //ArrayAdapter<String> myarrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dtcCodes);
//        //lv.setAdapter(myarrayAdapter);
//        //lv.setTextFilterEnabled(true);
//    }
//
//
//    public class ModifiedTroubleCodesObdCommand extends TroubleCodesCommand {
//        @Override
//        public String getResult() {
//            Log.e("activity","TroubleCodesActivity.java : ModifiedTroubleCodesObdCommand : getResult");
//            // remove unwanted response from output since this results in erroneous error codes
//            return rawData.replace("SEARCHING...", "").replace("NODATA", "");
//        }
//    }
//
//    public class ClearDTC extends ResetTroubleCodesCommand {
//        @Override
//        public String getResult() {
//            Log.e("activity","TroubleCodesActivity.java : ClearDTC : getResult");
//            return rawData;
//        }
//    }
//
//
//    private class GetTroubleCodesTask extends AsyncTask<String, Integer, String> {
//
//        @Override
//        protected void onPreExecute() {
//            Log.e("activity","TroubleCodesActivity.java : GetTroubleCodesTask : onPreExecute");
//            //Create a new progress dialog
//            progressDialog = new ProgressDialog(Bluetooth.this);
//            //Set the progress dialog to display a horizontal progress bar
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            //Set the dialog title to 'Loading...'
//            progressDialog.setTitle("getString(R.string.dialog_loading_title)");
//            //Set the dialog message to 'Loading application View, please wait...'
//            progressDialog.setMessage("getString(R.string.dialog_loading_body)");
//            //This dialog can't be canceled by pressing the back key
//            progressDialog.setCancelable(false);
//            //This dialog isn't indeterminate
//            progressDialog.setIndeterminate(false);
//            //The maximum number of items is 100
//            progressDialog.setMax(5);
//            //Set the current progress to zero
//            progressDialog.setProgress(0);
//            //Display the progress dialog
//            progressDialog.show();
//        }
//
//        @Override
//        protected String doInBackground(String... params) {
//            Log.e("activity","TroubleCodesActivity.java : GetTroubleCodesTask : doInBackground");
//            String result = "";
//
//            //Get the current thread's token
//            synchronized (this) {
//                Log.d(TAG, "Starting service..");
//                // get the remote Bluetooth device
//
//                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
//                Log.d("Check", "address"+params[0]);
//                dev = btAdapter.getRemoteDevice(params[0]);
//
//                Log.d(TAG, "Stopping Bluetooth discovery.");
//                btAdapter.cancelDiscovery();
//
//                Log.d(TAG, "Starting OBD connection..");
//
//                // Instantiate a BluetoothSocket for the remote device and connect it.
//                try {
//                    sock = BluetoothManager.connect(dev);
//                } catch (Exception e) {
//                    Log.e(
//                            TAG,
//                            "There was an error while establishing connection. -> "
//                                    + e.getMessage()
//                    );
//                    Log.d(TAG, "Message received on handler here");
//                    mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
//                    return null;
//                }
//
//
//                // Instantiate a BluetoothSocket for the remote device and connect it.
////                try {
////                    Log.e("io","BluetoothManager.java : connect");
////                    BluetoothSocket sock = null;
////                    BluetoothSocket sockFallback = null;
////
////                    Log.d(TAG, "Starting Bluetooth connection..");
////                    try {
////                        Log.d(TAG, "device"+mBTDevice.getName());
////                        sock = mBTDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
////                        sock.connect();
////                    } catch (Exception e1) {
////                        Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
////                        Class<?> clazz = sock.getRemoteDevice().getClass();
////                        Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
////                        try {
////                            Method m = clazz.getMethod("createRfcommSocket", paramTypes);
////                            Object[] params = new Object[]{Integer.valueOf(1)};
////                            sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
////                            sockFallback.connect();
////                            sock = sockFallback;
////                        } catch (Exception e2) {
////                            Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
////                            throw new IOException(e2.getMessage());
////                        }
////                    }
////                    //return sock;
////                } catch (Exception e) {
////                    Log.e(
////                            TAG,
////                            "There was an error while establishing connection. -> "
////                                    + e.getMessage()
////                    );
////                    Log.d(TAG, "Message received on handler here");
////                    mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
////                    return null;
////                }
//
//
//                try {
//                    // Let's configure the connection.
//                    Log.d(TAG, "Queueing jobs for connection configuration..");
//
//                    onProgressUpdate(1);
//
//                    new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//
//                    onProgressUpdate(2);
//
//                    new EchoOffCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//                    onProgressUpdate(3);
//
//                    new LineFeedOffCommand().run(sock.getInputStream(), sock.getOutputStream());
//
//                    onProgressUpdate(4);
//
//                    new SelectProtocolCommand(ObdProtocols.AUTO).run(sock.getInputStream(), sock.getOutputStream());
//
//                    onProgressUpdate(5);
//
//                    ModifiedTroubleCodesObdCommand tcoc = new ModifiedTroubleCodesObdCommand();
//                    tcoc.run(sock.getInputStream(), sock.getOutputStream());
//                    result = tcoc.getFormattedResult();
//
//                    onProgressUpdate(6);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_IO).sendToTarget();
//                    return null;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_IE).sendToTarget();
//                    return null;
//                } catch (UnableToConnectException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_UTC).sendToTarget();
//                    return null;
//                } catch (MisunderstoodCommandException e) {
//                    e.printStackTrace();
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_MIS).sendToTarget();
//                    return null;
//                } catch (NoDataException e) {
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_NODATA).sendToTarget();
//                    return null;
//                } catch (Exception e) {
//                    Log.e("DTCERR", e.getMessage());
//                    mHandler.obtainMessage(OBD_COMMAND_FAILURE).sendToTarget();
//                } finally {
//
//                    // close socket
//                    closeSocket(sock);
//                }
//
//            }
//
//            return result;
//        }
//
//        public void closeSocket(BluetoothSocket sock) {
//            Log.e("activity","TroubleCodesActivity.java : GetTroubleCodesTask : closeSocket");
//            if (sock != null)
//                // close socket
//                try {
//                    sock.close();
//                } catch (IOException e) {
//                    Log.e(TAG, e.getMessage());
//                }
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            super.onProgressUpdate(values);
//            Log.e("activity","TroubleCodesActivity.java : GetTroubleCodesTask : onProgressUpdate");
//            progressDialog.setProgress(values[0]);
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            Log.e("activity","TroubleCodesActivity.java : GetTroubleCodesTask : onPostExecute");
//            progressDialog.dismiss();
//
//
//            mHandler.obtainMessage(DATA_OK, result).sendToTarget();
//         //   setContentView(R.layout.trouble_codes);
//
//        }
//    }

    //private static final String TAG = ObdHelper.class.getSimpleName();
    // private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int NO_BLUETOOTH_DEVICE_SELECTED = 0;
    private static final int CANNOT_CONNECT_TO_DEVICE = 1;
    private static final int NO_DATA = 3;
    private static final int OBD_COMMAND_FAILURE = 10;
    private static final int OBD_COMMAND_FAILURE_IO = 11;
    private static final int OBD_COMMAND_FAILURE_UTC = 12;
    private static final int OBD_COMMAND_FAILURE_IE = 13;
    private static final int OBD_COMMAND_FAILURE_MIS = 14;
    private static final int OBD_COMMAND_FAILURE_NODATA = 15;


    private Handler mHandler = new Handler(new Handler.Callback() {

        public boolean handleMessage(Message msg) {
            Log.d(TAG, "Message received on handler");
            Log.d(TAG, "Message: " + msg.what);
            switch (msg.what) {
                case NO_BLUETOOTH_DEVICE_SELECTED:
                    makeToast("getString(R.string.text_bluetooth_nodevice)");
                    Log.d(TAG, "getString(R.string.text_bluetooth_nodevice)");
                    break;
                case CANNOT_CONNECT_TO_DEVICE:
                    makeToast("getString(R.string.text_bluetooth_error_connecting)");
                    Log.d(TAG, "getString(R.string.text_bluetooth_error_connecting)");
                    break;
                case OBD_COMMAND_FAILURE:
                    makeToast("getString(R.string.text_obd_command_failure)");
                    Log.d(TAG, "getString(R.string.text_obd_command_failure)");
                    break;
                case OBD_COMMAND_FAILURE_IO:
                    makeToast("getString(R.string.text_obd_command_failure) +  IO");
                    Log.d(TAG, "getString(R.string.text_obd_command_failure) +  IO");
                    break;
                case OBD_COMMAND_FAILURE_IE:
                    makeToast("getString(R.string.text_obd_command_failure) +  IE");
                    Log.d(TAG, "getString(R.string.text_obd_command_failure) +  IE");
                    break;
                case OBD_COMMAND_FAILURE_MIS:
                    makeToast("getString(R.string.text_obd_command_failure) +  MIS");
                    Log.d(TAG, "getString(R.string.text_obd_command_failure) +  MIS");
                    break;
                case OBD_COMMAND_FAILURE_UTC:
                    makeToast("getString(R.string.text_obd_command_failure) +  UTC");
                    Log.d(TAG, "getString(R.string.text_obd_command_failure) +  UTC");
                    break;
                case OBD_COMMAND_FAILURE_NODATA:
                    makeToastLong("getString(R.string.text_noerrors)");
                    Log.d(TAG, "getString(R.string.text_noerrors)");
                    break;
                case NO_DATA:
                    makeToast("getString(R.string.text_dtc_no_data)");
                    Log.d(TAG, "Message received on handler");
                    break;
            }
            resetAcitivityState();
            return false;
        }
    });

    private void resetAcitivityState() {
        //progressBar.setProgress(0);
        //    sendBtn.setEnabled(true);
    }

    private void makeToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void makeToastLong(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
        toast.show();
    }


    // private RequestActivity requestActivity;
    // private ProgressBar progressBar;
    //  private Handler mHandler;

    // private static BluetoothAdapter bluetoothAdapter;
    // private static Set<BluetoothDevice> pairedDevices;

    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            new Handler().postDelayed(mQueueCommands, 400);
        }
    };

//    private GetTroubleCodesTask gtct;

//    public ObdHelper(Handler mHandler, RequestActivity requestActivity) {
//        this.requestActivity = requestActivity;
//        this.mHandler = mHandler;
//        gtct = new GetTroubleCodesTask();
//    }

    public void connectToDevice() {
        String remoteDevice = mBTDevice.getAddress();
        if (remoteDevice == null | "".equals(remoteDevice))
            Log.e(TAG, "No bt device is paired.");
        else
            gtct.execute(remoteDevice);
    }

//    private static void connectViaBluetooth() {
//        if (bluetoothAdapter == null)
//            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//    }

//    public static int checkBluetoothEnabled() {
//        connectViaBluetooth();
//        if (bluetoothAdapter == null)
//            return -1;
//        else if (!bluetoothAdapter.isEnabled()) {
//            return 0;
//        } else {
//            return 1;
//        }
//    }
//
//    public static Set<BluetoothDevice> getPairedDevice() {
//        if(bluetoothAdapter==null)
//            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_ON)
//            pairedDevices = bluetoothAdapter.getBondedDevices();
//        return pairedDevices;
//    }

    private class GetTroubleCodesTask extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "OBD II connecting.", Toast.LENGTH_SHORT).show();
            //   progressBar = requestActivity.getProgressBar();
//            progressBar.setMax(8);
        }

        @Override
        protected String doInBackground(String... params) {
            String result = "";
            BluetoothDevice dev;
            //Get the current thread's token
            synchronized (this) {
                Log.d(TAG, "Starting service..");
                onProgressUpdate(1);
                // get the remote Bluetooth device
                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                dev = btAdapter.getRemoteDevice(params[0]);

                Log.d(TAG, "Stopping Bluetooth discovery.");
                btAdapter.cancelDiscovery();

                Log.d(TAG, "Starting OBD connection..");
                onProgressUpdate(2);
                BluetoothSocket sock;
                // Instantiate a BluetoothSocket for the remote device and connect it.
                try {
                    Log.d("Checkaddress", "device address:" + dev);
//                    if (isWorkAroundSamsungS9()) {
//                        sock=initLocalConnectionS9(dev);
//              //          sock = connectS9(dev);
//                    } else {
//                        sock = connect(dev);
//                    }
                   sock = connect(dev);
                //    Toast.makeText(getApplicationContext(), "OBD II connected successfully.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "There was an error while establishing connection. -> " + e.getMessage());
                    Log.d(TAG, "Message received on handler here");
                    mHandler.obtainMessage(CANNOT_CONNECT_TO_DEVICE).sendToTarget();
                    return null;
                }

                try {
                    // Let's configure the connection.
                    Log.d(TAG, "Queueing jobs for connection configuration..");

                    onProgressUpdate(3);

                    new ObdResetCommand().run(sock.getInputStream(), sock.getOutputStream());
                    Log.d(TAG, "ObdResetCommand successs");

                    onProgressUpdate(4);

                    new EchoOffCommand().run(sock.getInputStream(), sock.getOutputStream());
                    Log.d(TAG, " EchoOffCommand succcess");

                    onProgressUpdate(5);

                    onProgressUpdate(6);

                    new SelectProtocolCommand(ObdProtocols.AUTO).run(sock.getInputStream(), sock.getOutputStream());
                    Log.d(TAG, "SelectProtocolCommand success");

                    onProgressUpdate(7);
                    Log.d(TAG, "nothing");

//                    MyTroubleCodesCommand tcoc = new MyTroubleCodesCommand();
//                    tcoc.run(sock.getInputStream(), sock.getOutputStream());
//                    onProgressUpdate(8);
//                    result = tcoc.getFormattedResult();

                    ///  >>>>>>>>>>>>>>>> Masla idhar hoga agar hoga. <<<<<<<<<<<<<<
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                         Toast.makeText(getApplicationContext(), "OBD II connected", Toast.LENGTH_SHORT).show();
                        }
                    });

                    while (true) {
                        //       Read from the InputStream
                        try {

                            RPMCommand engineRpmCommand = new RPMCommand();

                            SpeedCommand speedCommand = new SpeedCommand();
//
                            AmbientAirTemperatureCommand atc = new AmbientAirTemperatureCommand();


                            //         FuelLevelCommand flc = new FuelLevelCommand();
//                            while (!Thread.currentThread().isInterrupted()) {

                            MyTroubleCodesCommand tcoc = new MyTroubleCodesCommand();
                            engineRpmCommand.run(sock.getInputStream(), sock.getOutputStream());
                            speedCommand.run(sock.getInputStream(), sock.getOutputStream());
                            atc.run(sock.getInputStream(), sock.getOutputStream());
                            //            flc.run(sock.getInputStream(), sock.getOutputStream());

                            tcoc.run(sock.getInputStream(), sock.getOutputStream());
                            result = tcoc.getFormattedResult();

                            // TODO handle commands result
                            Log.d(TAG, "RPM: " + engineRpmCommand.getCalculatedResult());
                            Log.d(TAG, "Speed: " + speedCommand.getCalculatedResult());
                            Log.d(TAG, "Temp: " + atc.getCalculatedResult());
                            //      Log.d(TAG, "Fuel level: " + flc.getFormattedResult());

                            Log.d(TAG, "Trouble codes: " + result);

                            // save speed to sharedPreferences
                            String speed = speedCommand.getCalculatedResult();
                            SharedPreferences sp_bt = getSharedPreferences(getString(R.string.sp_blueTooth),0);
                            String key_bt_conn = getString(R.string.sp_bt_key_isDeviceConnected);
                            String key_bt_speed = getString(R.string.sp_bt_key_car_speed);
                            SharedPreferencesUtils.saveBool(sp_bt,key_bt_conn,true);
                            SharedPreferencesUtils.saveString(sp_bt,key_bt_speed,speed);

                            //          if(result.contains("B1904")||result.contains("B1902")){
//                            Intent intent = new Intent(getApplicationContext(), Sms.class);
//                            startActivity(intent);
//                            break;
                            //        };

                            //       }
                            if (result.contains("B1904") || result.contains("B1902")) {
                                Intent intent = new Intent(getApplicationContext(), Sms.class);
                                startActivity(intent);
                                break;
                            }


                        } catch (IOException | InterruptedException e) {
                            Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                            break;
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_IO).sendToTarget();
                    return null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_IE).sendToTarget();
                    return null;
                } catch (UnableToConnectException e) {
                    e.printStackTrace();
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_UTC).sendToTarget();
                    return null;
                } catch (MisunderstoodCommandException e) {
                    e.printStackTrace();
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_MIS).sendToTarget();
                    return null;
                } catch (NoDataException e) {
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE_NODATA).sendToTarget();
                    return null;
                } catch (Exception e) {
                    Log.e("DTCERR", e.getMessage());
                    mHandler.obtainMessage(OBD_COMMAND_FAILURE).sendToTarget();
                } finally {
                    closeSocket(sock);
                }
            }
            return result;
        }

        private void closeSocket(BluetoothSocket sock) {
            if (sock != null)
                try {
                    sock.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
//            progressBar.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Log.d(TAG, "Result obtained" + result);
                //      requestActivity.startResultActivity(result);
                //  resultDtcs(result);
            } else {
                Log.e(TAG, "No result (Nullpointer).");
            }
        }
    }

    /**
     * Instantiates a BluetoothSocket for the remote device and connects it.
     * See http://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3/18786701#18786701
     *
     * @param dev The remote device to connect to
     * @return The BluetoothSocket
     * @throws IOException
     */
    private static BluetoothSocket connect(BluetoothDevice dev) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback;

        Log.d(TAG, "Starting Bluetooth connection..");
        try {
            sock = dev.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            sock.connect();
        } catch (Exception e1) {
            Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
            if (sock != null) {
                Class<?> clazz = sock.getRemoteDevice().getClass();
                Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                try {
                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                    Object[] params = new Object[]{Integer.valueOf(1)};
                    sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                    sockFallback.connect();
                    sock = sockFallback;
                } catch (Exception e2) {
                    Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                    throw new IOException(e2.getMessage());
                }
            }
        }

        return sock;

    }

    private boolean isWorkAroundSamsungS9() {
        return Build.MANUFACTURER.toLowerCase().contains("samsung") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    Thread localConnect;
    BluetoothSocket sockett;

    private synchronized BluetoothSocket initLocalConnectionS9(final BluetoothDevice dev) {
        localConnect = new Thread() {
            public void run() {
                final BluetoothDevice btDevice = dev;
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                int connectionRetries = 0;
//                bluetoothState != BT_CONNECTED &&
                while (connectionRetries < 3) {
                    try {
                        //  bluetoothState = BT_CONNECTING;
                        sockett = null;
 //                       final BluetoothAdapter BT = BluetoothAdapter.getDefaultAdapter();
//                        btDevice = BT.getRemoteDevice(btMac);
                        final UUID MY_UUID = UUID
                                .fromString("00001101-0000-1000-8000-00805F9B34FB");
                        sockett = btDevice
                                .createInsecureRfcommSocketToServiceRecord(MY_UUID);
//                        in = sock.getInputStream();
//                        out = sock.getOutputStream();
                        Log.d(TAG, "localConnect() INI: sock.connect()");
                        sockett.connect();
                        Log.d(TAG, "localConnect() END: sock.connect()");
//                        setBluetoothState(BT_CONNECTED, true);
//                        startReadThread();
//                        refreshMainActivity();
                    } catch (final Exception e) {
                        connectionRetries++;
//                        closeSocket();
                        if (sockett != null)
                            try {
                                sockett.close();
                            } catch (IOException ex) {
                                Log.e(TAG, ex.getMessage());
                            }
//                        bluetoothState = BT_DISCONNECTED;
                        Log.e(TAG, "localConnect() EXCEPTION: sock.connect()" + e);
                        if (connectionRetries >= 3) {
                            // setBluetoothState(BT_DISCONNECTED, false);
                            Log.e(TAG, "Unable to connect");
                        } else {
                            Log.d(TAG, "Bluetooth connection retries: " + connectionRetries);
                         //   delay(500);
                            new CountDownTimer(500, 1) {

                                @Override
                                public void onTick(long millisUntilFinished) {
//                            seconds = (millisUntilFinished / 1000);
//                            //     Toast.makeText(getApplicationContext(), "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
//                            if (sendMsgFlag) {
//                                timer.setText("Seconds left: " + seconds + "");
//                            } else {
//                                timer.setText("");
//                            }
                                }

                                @Override
                                public void onFinish() {
//                            if (sendMsgFlag) {
//                                text1.setText("Sms Sent to Emergency Contacts Successfully!");
//                                cancel.setVisibility(View.GONE);
//                                Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
//                                //    SendSms(fromPosition);
//                            }
                                }
                            }.start();
                        }
                    }
                }
            }
        };
        return sockett;
        //threadPoolDevelopers.submit(localConnect);
    }

    private synchronized BluetoothSocket connectS9(BluetoothDevice dev) throws IOException {
        BluetoothSocket sock = null;
        BluetoothSocket sockFallback;

        Log.d(TAG, "Starting Bluetooth connection..");
        int connectionRetries = 0;
        while (connectionRetries < 3) {
            try {
                sock = dev.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                sock.connect();
            } catch (Exception e1) {
                connectionRetries++;
                Log.e(TAG, "There was an error while establishing Bluetooth connection. Falling back..", e1);
                if (sock != null) {
                    Class<?> clazz = sock.getRemoteDevice().getClass();
                    Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
                    try {
                        Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                        Object[] params = new Object[]{Integer.valueOf(1)};
                        sockFallback = (BluetoothSocket) m.invoke(sock.getRemoteDevice(), params);
                        sockFallback.connect();
                        sock = sockFallback;
                    } catch (Exception e2) {
                        Log.e(TAG, "Couldn't fallback while establishing Bluetooth connection.", e2);
                        throw new IOException(e2.getMessage());
                    }
                }
                if (connectionRetries >= 3) {
                    // setBluetoothState(BT_DISCONNECTED, false);
                    Log.e(TAG, "Unable to connect");
                } else {
                    Log.d(TAG, "Bluetooth connection retries: " + connectionRetries);
                    //   delay(500);
                    new CountDownTimer(500, 1000) {

                        @Override
                        public void onTick(long millisUntilFinished) {
//                            seconds = (millisUntilFinished / 1000);
//                            //     Toast.makeText(getApplicationContext(), "seconds remaining: " + millisUntilFinished / 1000, Toast.LENGTH_SHORT).show();
//                            if (sendMsgFlag) {
//                                timer.setText("Seconds left: " + seconds + "");
//                            } else {
//                                timer.setText("");
//                            }
                        }

                        @Override
                        public void onFinish() {
//                            if (sendMsgFlag) {
//                                text1.setText("Sms Sent to Emergency Contacts Successfully!");
//                                cancel.setVisibility(View.GONE);
//                                Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_SHORT).show();
//                                //    SendSms(fromPosition);
//                            }
                        }
                    }.start();
                }
            }
        }

        return sock;

    }


//
//    public void resultDtcs(String dtcs) {
//
//        if (dtcs == null)
//            dtcs = "";
//        if (!dtcs.equals("")) {
//            String[] dtcArray = dtcs.split("\\n");
//            String language = "english";
//            String vin = "NHP102360619";
//            //   ApiHelper api = new ApiHelper(getApplicationContext());
//            //   api.getErrorCodeTranslation(dtcArray, vin , language);
//        } else {
//            Log.d(TAG, "No error code received");
//        }
//
//    }

}
