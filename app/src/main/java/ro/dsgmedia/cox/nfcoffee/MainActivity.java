package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 24-May-17.
 */

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.util.Log;

import android.net.wifi.*;
import android.nfc.*;
import android.database.sqlite.*;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.net.Socket;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x3141;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private NavigationView mOptionsView;
    private TextView mCurrentStatus;
    private ImageButton buttonGetTCP;
    private ScrollView mScrollView;

    private WifiManager mainWifiObj;

    private getTCPData mTcpClient = null;

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mScrollView = (ScrollView) findViewById(R.id.mScroll);

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        // Code here will be triggered once the drawer closes as we don't want anything to happen so we leave this blank
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // Setup the listener for the navigation drawer
        mOptionsView = (NavigationView) findViewById(R.id.navigation_drawer);
        mOptionsView.setNavigationItemSelectedListener(this);

        //calling sync state is necessary or else your hamburger icon wont show up
        mActionBarDrawerToggle.syncState();

        mCurrentStatus = (TextView) findViewById(R.id.mCurrentStatus);
        mCurrentStatus.setMovementMethod(new ScrollingMovementMethod());
        mCurrentStatus.setText("");

        // Setup listeners for the two buttons
        buttonGetTCP = (ImageButton) findViewById(R.id.getTcpData);
        buttonGetTCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // detect the view that was "clicked"
                switch (view.getId()) {
                    case R.id.getTcpData:
                            if (mTcpClient == null) {
                                buttonGetTCP.setPressed(true);
                                mCurrentStatus.setText("");
                                new ConnectTask().execute("");
                            }
                            //mTcpClient = null;
                            break;
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(null != mTcpClient)
        {
            // disconnect if connected
            mTcpClient.stopClient();
            mTcpClient = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()) {
            case R.id.navigation_item_settings: {
                // open activity setup WiFi
                //Intent WiFiSetupIntent = new Intent(this, wlanActivity.class);
                //startActivity(WiFiSetupIntent);

                Intent i = new Intent(this, PreferencesActivity.class);
                startActivity(i);

                break;
            }
            case R.id.navigation_item_manIDs: {
                // open manage IDs activity
                Intent ManageIDsIntent = new Intent(this, ManageIDs.class);
                startActivity(ManageIDsIntent);
                break;
            }
        }
        //close navigation drawer
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // granted permission for WiFi
            // all good
        }
    }

    public class ConnectTask extends AsyncTask<String, String, getTCPData> {
        private HashMap<String,String> ReceivedData = new HashMap<String, String>();

        @Override
        protected getTCPData doInBackground(String... message) {
            // check settings
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            String ssid = sharedPref.getString("wifiNFCSSID", "NoWiFi");
            String key = sharedPref.getString("wifiNFCPWD", "nofreecoffee");

            // check wifi status
            mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (mainWifiObj.isWifiEnabled() == false) {
                publishProgress("Enable WiFi..");
                mainWifiObj.setWifiEnabled(true);
                publishProgress(" Enabled" + System.getProperty("line.separator"));
            }

            // connect to wifi
            publishProgress("Setting WiFi...");
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", key);
            publishProgress(" Done" + System.getProperty("line.separator"));

            int netId = mainWifiObj.addNetwork(wifiConfig);
            publishProgress("Connecting to WiFi...");
            //mainWifiObj.disconnect();
            mainWifiObj.enableNetwork(netId, true);
            mainWifiObj.reconnect();

            WifiInfo wifiInfo;
            wifiInfo = mainWifiObj.getConnectionInfo();

            Integer timeout = 20;
            while ((timeout > 0) &&
                    (
                            (!wifiInfo.getSSID().replaceAll("\"", "").equals(ssid)) ||
                                    (wifiInfo.getBSSID().equals("00:00:00:00:00:00")) ||
                                    (wifiInfo.getIpAddress() == 0)
                    )
                    ) {
                wifiInfo = mainWifiObj.getConnectionInfo();
                timeout--;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.w("Compare return", String.valueOf((wifiInfo.getSSID().contains(ssid))));
            Log.w("SSID", wifiInfo.getSSID());
            Log.w("Status", wifiInfo.getBSSID());

            if (0 == timeout) {
                publishProgress(" ERROR: " + ssid + " not found" + System.getProperty("line.separator"));
            } else {
                SupplicantState wifiSupp;
                wifiSupp = wifiInfo.getSupplicantState();
                while (wifiSupp != SupplicantState.COMPLETED) {
                    wifiSupp = wifiInfo.getSupplicantState();
                }

                Log.i("SSID", wifiInfo.getSSID() + " " + ssid);
                Log.i("IP", String.valueOf(wifiInfo.getIpAddress()));
                Log.i("SUPP", String.valueOf(wifiInfo.getSupplicantState()));

                ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = null;
                if (connectivityManager != null) {
                    networkInfo = connectivityManager.getActiveNetworkInfo();
                }

                while (!(networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED)) {
                    networkInfo = connectivityManager.getActiveNetworkInfo();
                    //Wait
                }

                publishProgress(" Connected" + System.getProperty("line.separator"));

                // start transfer task
                publishProgress("Starting the transfer...");

                //we create a TCPClient object and
                mTcpClient = new getTCPData(new getTCPData.OnMessageReceived() {
                    @Override
                    //here the messageReceived method is implemented
                    public void messageReceived(String message) {
                        //this method calls the onProgressUpdate

                        String[] separated = message.split(",");
                        Log.i(separated[0], separated[1]);
                        ReceivedData.put(separated[0], separated[1]);

                        publishProgress(message);
                    }
                });
                Log.i("Connecting...", "OK");
                mTcpClient.run();

                // process received data on success
                publishProgress(" Done" + System.getProperty("line.separator"));

                mScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });

                // start with the SQL database update
                ContentValues data=new ContentValues();
                for (Map.Entry<String,String> entry: ReceivedData.entrySet()) {
                    String codeName = entry.getKey();
                    String value = entry.getValue();
                    data.put(SqlHelper_NFCIDs.NB_COFFEES, Integer.valueOf(value));
                    String NFCID = RevertBase36(codeName);

                    if(0 == mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data, SqlHelper_NFCIDs.CODE_NAME+"=\""+codeName+"\"", null )) {
                        mydatabase.beginTransaction();
                        data.put(SqlHelper_NFCIDs.NFC_ID, NFCID);
                        data.put(SqlHelper_NFCIDs.CODE_NAME, codeName);
                        mydatabase.insert(SqlHelper_NFCIDs.TABLE_NAME, null, data);
                        mydatabase.setTransactionSuccessful();
                        mydatabase.endTransaction();
                    }
                    data.clear();
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            mCurrentStatus.append(values[0]);
            mCurrentStatus.append(System.getProperty("line.separator"));

            //in the arrayList we add the messaged received from server
            //arrayList.add(values[0]);
            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            //mAdapter.notifyDataSetChanged();
        }
    }

    private String RevertBase36(String inCodeName) {
        String[] splited;
        splited = inCodeName.split("\\.");
        inCodeName = splited[0]+splited[1];
        BigInteger NFCIdValue = new BigInteger(inCodeName, 36);
        return NFCIdValue.toString(16).toUpperCase();
    }
}
