package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 29-Jun-17.
 */
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity {

    /* WiFi and Preferences */
    private int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x3141;
    private WifiManager mainWifiObj;
    private ArrayList<String> wifis;
    BroadcastReceiver BcastRx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mainWifiObj.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mainWifiObj.setWifiEnabled(true);
        }

        registerReceiver(BcastRx = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                Log.w("SSIDs found ", "OK");

                //setContentView(R.layout.activity_wlan_activty);
                // update here the array list

                List<ScanResult> wifiScanList = mainWifiObj.getScanResults();
                wifis = new ArrayList<>();
                for (int i = 0; i < wifiScanList.size(); i++) {
                    if (!wifiScanList.get(i).SSID.isEmpty()) {
                        wifis.add(((wifiScanList.get(i).SSID)));
                    }
                }

                if(0 == wifis.size()) {
                    wifis.add("NO_SSID_FOUND");
                }

                getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }else {
            //do something, permission was previously granted; or legacy device
            startScanning();
        }
    }

    public class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            ListPreference listPreferenceCategory = (ListPreference) this.findPreference("wifiNFCSSID");

            CharSequence entries[] = new String[wifis.size()];
            CharSequence entryValues[] = new String[wifis.size()];
            for (int i = 0; i<wifis.size(); i++) {
                entries[i] = wifis.get(i).toString();
                entryValues[i] = wifis.get(i).toString();
            }
            listPreferenceCategory.setEntries(entries);
            listPreferenceCategory.setEntryValues(entryValues);
        }
    }


    @Override
    protected void onStop() {
        unregisterReceiver(BcastRx);

        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        super.onStop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Do something with granted permission
            startScanning();
        }
    }


    private void startScanning() {
        mainWifiObj.startScan();
        Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
    }


}
