package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 24-May-17.
 */

import android.Manifest;
import android.content.BroadcastReceiver;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.net.Socket;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private NavigationView mOptionsView;
    private TextView mCurrentStatus;
    private getTCPData mTcpClient = null;
    private int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x3141;
    private WifiManager mainWifiObj;

 /*    private void setNavigationViewListner() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_item_settings);
        navigationView.setNavigationItemSelectedListener(this);
    }*/

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

        // Code here will be triggered once the drawer closes as we don't want anything to happen so we leave this blank
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // Setup the listener for the navigation drawer
        mOptionsView = (NavigationView) findViewById(R.id.navigation_drawer);
        mOptionsView.setNavigationItemSelectedListener(this);

        //calling sync state is necessary or else your hamburger icon wont show up
        mActionBarDrawerToggle.syncState();

        mCurrentStatus = (TextView) findViewById(R.id.mCurrentStatus);

        // Setup listeners for the two buttons
        final ImageButton buttonGetTCP = (ImageButton) findViewById(R.id.getTcpData);
        buttonGetTCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // detect the view that was "clicked"
                switch (view.getId()) {
                    case R.id.getTcpData:
                        mCurrentStatus.setText("We roll..");
                        mCurrentStatus.append(System.getProperty("line.separator"));

                        // check settings
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                        String ssid = sharedPref.getString("wifiNFCSSID", "NoWiFi");
                        String key = sharedPref.getString("wifiNFCPWD", "nofreecoffee");
                        // check wifi status
                        mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (mainWifiObj.isWifiEnabled() == false)
                        {
                            mCurrentStatus.append("Enable WiFi..");
                            mainWifiObj.setWifiEnabled(true);
                            mCurrentStatus.append(" Enabled");
                            mCurrentStatus.append(System.getProperty("line.separator"));

                        }
                        // connect to wifi
                        mCurrentStatus.append("Setting WiFi...");
                        WifiConfiguration wifiConfig = new WifiConfiguration();
                        wifiConfig.SSID = String.format("\"%s\"", ssid);
                        wifiConfig.preSharedKey = String.format("\"%s\"", key);
                        mCurrentStatus.append(" Done");
                        mCurrentStatus.append(System.getProperty("line.separator"));


                        int netId = mainWifiObj.addNetwork(wifiConfig);
                        mCurrentStatus.append("Connecting to WiFi...");
                        //mainWifiObj.disconnect();
                        mainWifiObj.enableNetwork(netId, true);
                        mainWifiObj.reconnect();

                        WifiInfo wifiInfo;
                        wifiInfo = mainWifiObj.getConnectionInfo();

                        while(! wifiInfo.getSSID().contains(ssid)) {
                            wifiInfo = mainWifiObj.getConnectionInfo();
                        }

                        while( wifiInfo.getIpAddress() == 0) {
                            wifiInfo = mainWifiObj.getConnectionInfo();
                        }

                        SupplicantState wifiSupp;
                        wifiSupp = wifiInfo.getSupplicantState();

                        while (wifiSupp != SupplicantState.COMPLETED)
                        {
                            wifiSupp = wifiInfo.getSupplicantState();
                        }

                        Log.i("SSID: ", wifiInfo.getSSID() + " " + ssid);
                        Log.i("IP  : ", String.valueOf(wifiInfo.getIpAddress()));
                        Log.i("SUPP: ", String.valueOf(wifiInfo.getSupplicantState()));


                        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo networkInfo = null;
                        if (connectivityManager != null) {
                            networkInfo = connectivityManager.getActiveNetworkInfo();
                        }

                        while (!(networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED)) {
                            networkInfo = connectivityManager.getActiveNetworkInfo();
                            //Wait
                        }
/**/
                        mCurrentStatus.append(" Connected");
                        mCurrentStatus.append(System.getProperty("line.separator"));

                        // start transfer task
                        mCurrentStatus.append("Starting the transfer...");
                        if (mTcpClient == null) {
                            new ConnectTask().execute("");
                        }

                        // process received data on success
                        mCurrentStatus.append(" Done");
                        mCurrentStatus.append(System.getProperty("line.separator"));
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

        @Override
        protected getTCPData doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new getTCPData(new getTCPData.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            Log.i("Connecting...", "OK");
            mTcpClient.run();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            Date cDate = new Date();
            String dateNow = new SimpleDateFormat("yyyy-MM-dd").format(cDate);

            mCurrentStatus.append(values[0]);
            mCurrentStatus.append(System.getProperty("line.separator"));
            Log.i("Rcv: ", values[0]);
/*            try {
                FileWriter fOut = new FileWriter(dateNow + ".log",  true);
                fOut.write(values[0]);
                fOut.flush();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
*/

            //in the arrayList we add the messaged received from server
            //arrayList.add(values[0]);
            // notify the adapter that the data set has changed. This means that new message received
            // from server was added to the list
            //mAdapter.notifyDataSetChanged();
        }
    }
}
