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
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity {

    /* WiFi and Preferences */
    private WifiManager mainWifiObj;
    static private ArrayList<String> wifis;
    BroadcastReceiver BcastRx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mainWifiObj = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (mainWifiObj.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mainWifiObj.setWifiEnabled(true);
        }

        registerReceiver(BcastRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                Log.w("SSIDs found ", "OK");

                //setContentView(R.layout.activity_wlan_activty);
                // update here the array list

                if (ActivityCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
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

        // Check for WiFi permission. If not allowed the activity will show a toast.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission to access the location is not granted.\nPlease restart " +
                    "the application and grant access to the location services.", Toast.LENGTH_SHORT).show();
        } else {
            startScanning();
        }
    }

    static public class MyPreferenceFragment extends PreferenceFragment
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

    private void startScanning() {
        mainWifiObj.startScan();
        Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show();
    }


}
