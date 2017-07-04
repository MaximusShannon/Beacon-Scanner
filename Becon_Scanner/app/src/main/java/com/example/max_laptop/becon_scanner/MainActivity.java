package com.example.max_laptop.becon_scanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //Heavily commented bluetooth scanner

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mScanning;

    private static final int RQS_ENABLE_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Button btnScan;
    Button btnLog;
    ListView listViewLE;

    List<BluetoothDevice> listBluetoothDevice;
    ListAdapter adapterLeScanResult;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;

    public ArrayList<Double> signalStrength = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //DIALOG BUILDER FOR GETTING ACCESS TO LOCATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("We need permission to scan");
                builder.setMessage("Please grant location access");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }

                });

                builder.show();
            }
        }

        // if the device the app is running on does not have bluetooth_LE(low energy) then the app
        //will not work.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth_LE not supported on this device!", Toast.LENGTH_SHORT).show();

            finish();
            //requestPermissions();

        }
        ;

        getBluetoothAdapterAndLeScanner();

        //checks if bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "blutoothManager.getAdapter() == null", Toast.LENGTH_SHORT).show();
            finish();

            return;
        }

        btnScan = (Button) findViewById(R.id.scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {

                //remember to activate
                scanLeDevice(true);
            }
        });

        btnLog = (Button)findViewById(R.id.logData);
        btnLog.setOnClickListener(new View.OnClickListener(){
            @Override

            public void onClick(View v){

               // record300SignalBursts(signalStrength);

            }




        });


        // to list all the devices once scanned.
        listViewLE = (ListView) findViewById(R.id.lelist);

        listBluetoothDevice = new ArrayList<>();
        //long line-itus broke line to make it easier to read
        adapterLeScanResult = new ArrayAdapter<BluetoothDevice>
                (this, android.R.layout.simple_expandable_list_item_1, listBluetoothDevice);


        //adapterLeScanResult = new ArrayAdapter<BluetoothDevice>
        //(this, android.R.layout.simple_expandable_list_item_2, listBluetoothDevice);

        //android.R.layout.simple_expandable_list_item_2;

        listViewLE.setAdapter(adapterLeScanResult);
        listViewLE.setOnItemClickListener(scanResultOnItemClickListener);

        mHandler = new Handler();

    }

    AdapterView.OnItemClickListener scanResultOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

            //this displays the popup dialog when clicking on a device when the user clicks on the device mac addy

            String msg = "device Address " + device.getAddress() + "\n" + "Device Name: "
                    + device.getName() + "\n" + device.getBluetoothClass().toString() +
                    "\n" + getBTDevieType(device) + "\n" + "RSSI_STR: not working yet";

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(device.getName())
                    .setMessage(msg)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }

                    })
                    .show();


                    }

    };//end click listerner


    private String getBTDevieType(BluetoothDevice d) {

        String type = "";

        switch (d.getType()) {

            //all the different types of bluetooth devices.. type will be used elsewhere
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;

            default:
                type = "unknown....";

        }
        return type;
    }


    @Override
    protected void onResume() {

        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {

                //SEND A MESSAGE INTENT TO GET THE USER TO TURN ON THE BLUETOOTH ON THE PHONE
                //an intent in android is like a request like indend to do something
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //if the request code isto turn on the bluetooth
        //but it instead turns of the bluetooth , or doesn't except
        //finish the acitivty.
        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner();

        //check if bluuetooth is supported on the device.

        // if it cant see a device then it isn't really supported
        if (mBluetoothAdapter == null) {

            Toast.makeText(this, "BluetoothManage.getAdapter() == null", Toast.LENGTH_SHORT).show();
            finish();
            return;

        }

        super.onActivityResult(requestCode, resultCode, data);

    }

    //get the bluetooth adapter and scanner for the target device.
    private void getBluetoothAdapterAndLeScanner() {

        //get the bluetooth adapter and the LE_Scanner
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    /*
    * to call startScan (ScanCallBack callback),
    * Requires BLUETOOTH_ADMIN permission
    * Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results
    * */

    private void scanLeDevice(final boolean enable) {

        //passed in from above, so its true to start the scan.
        if (enable) {

            listBluetoothDevice.clear();
            //dont know what invalidate views is - research
            listViewLE.invalidateViews();

            mBluetoothLeScanner.startScan(scanCallBack);
            /*
                        for filter scan:
            http://android-er.blogspot.co.at/2016/06/scan-specified-ble-devices-with.html

            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(BluetoothLeService.ParcelUuid_GENUINO101_ledService)

            */
            mScanning = true;
            btnScan.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallBack);
            mScanning = false;
            btnScan.setEnabled(true);
        }


    }

    public ScanCallback scanCallBack = new ScanCallback() {
       // @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());

            //try calculate the distance
            //cred: https://www.youtube.com/watch?v=oMOktz9fm_s

            if (result.getDevice().getName().equals("Kontakt_FL")) {


                //formula for calculating the number used to represent distance
                String distance = "";
                int count = 0;
                double adverage = 0.0;
                double[] getAverage = new double[5];
                double txPower = 3.0;
                double rssi = result.getRssi();
                double ratio_db = txPower - rssi;
                double ration_linear = Math.pow(10, ratio_db / 10);

                double r = Math.sqrt(ration_linear);
                //double fin = Math.floor(r);


                double calculatedDistance = calculateAccuracy(txPower, rssi);
                System.out.println("FEATURE: " + calculatedDistance /10 + "m");
                /*
                Average the first 5 strengths taken and reuse the array to do it again.
                Notes:
                Seems to be getting a pretty accurate number if the device is stationary
                but if the device is moved the reading seems to be a little bit of

                */
                for (int x = 0; x <= getAverage.length - 1; x++) {

                    getAverage[x] = r;
                    count++;

                    if (count == 5) {

                        for (int y = 0; y < getAverage.length - 1; y++) {

                            adverage = (getAverage[y] + getAverage[y + 1]) / 5;
                        }
                    }
                }



                //sendAverage(adverage);



                /*
                 Using the admin-panel standard apple iBeacon default settings,
                 350ms advertisement
                 Power setting 3.0
                 below is just predefined Strengths after I gathered 5 sets of data
                 and averaged them then devised by 10 to get a smaller number
                 */

                // Printing the distances to monitor
                if (adverage / 10 <= 100.0)
                    distance = "BEACON VERY VERY CLOSE / < 1/2ft";
                if (adverage / 10 > 100.0 && adverage / 10 <= 300.0)
                    distance = "BEACON WITHIN .5M";
                if (adverage / 10 > 300.0 && adverage / 10 <= 800.0)
                    distance = "BEACON WITHIN 1M";
                if (adverage / 10 > 800.0 && adverage / 10 <= 1500.0)
                    distance = "BEACON WITHIN 1.5M";
                if (adverage / 10 > 1500.0 && adverage / 10 <= 2300.0)
                    distance = "BEACON WITHIN 2M";
                if (adverage / 10 > 2300.0 && adverage / 10 <= 2800.0)
                    distance = "BEACON WITHIN 2.5M";
                if (adverage / 10 > 2800.0 && adverage / 10 <= 3300.0)
                    distance = "BEACON WITHIN 3M";
                if (adverage / 10 > 3300.0)
                    distance = "UNKNOWN > 3M";



                /*
                Used to print the results to a console window making
                it easier to read and debug than on the phone
                and also makes it vibrate when the device comes within half a foot (rough measurement)
                and when it is on top of the beacon
                */
                System.out.println("Distance: " + Math.floor(r) + " Average distance: " + adverage / 10 + " " + distance);
                if (adverage / 10 < 300) {

                    Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(500);
                }
                if (adverage / 10 < 100) {

                    Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(1500);
                }
                //Display toast text with the predefined distances, that we have made above.

                Toast.makeText(MainActivity.this, distance, Toast.LENGTH_SHORT).show();
            }

            /*
            This is for debugging to make sure our device is present,
            Kontakt is the name of our device used

            - I could repeat this step multiple times for multiple devices, but I could make some sort of algo
            to add the devices I want to a list, and set there position in a rome relatively.
            - Need more batteries to test this.

            */
            if (result.getDevice().getType() == BluetoothDevice.DEVICE_TYPE_LE /*&& result.getDevice().getName() == "Kontakt"*/) {

                System.out.println(result.getDevice().getName() + " " + result.getRssi() + " ");
            }

            //loging data



        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                addBluetoothDevice(result.getDevice());

            }
        }

        public void onScanFailed(int errorCode) {

            super.onScanFailed(errorCode);
            Toast.makeText(MainActivity.this,
                    "onScanFailed : " + String.valueOf(errorCode),
                    Toast.LENGTH_SHORT).show();
        }

        private void addBluetoothDevice(BluetoothDevice device) {
            if (!listBluetoothDevice.contains(device)) {
                listBluetoothDevice.add(device);
                listViewLE.invalidateViews();


            }
        }


    };

    public static double calculateAccuracy(double txPower, double rssi){

        double x;

        if(rssi == 0){
            return -1;
        }

        double ratio = rssi*1.0/txPower;
        if(ratio<1.0){

            return Math.pow(ratio, 10);
        }else{

            double accuracy = (0.9976)*Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }


    }

}//end main
