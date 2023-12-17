package ro.dsgmedia.cox.nfcoffee;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

public class WifiConnect {
    private Boolean isNetworkAvailable(MainActivity application) {
        ConnectivityManager connectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }
    public static Boolean connectWifi(Activity a) {
        WifiManager mainWifiObj;
        // check settings
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(a);
        String ssid = sharedPref.getString("wifiNFCSSID", "NoWiFi");
        String key = sharedPref.getString("wifiNFCPWD", "nofreecoffee");

        // check wifi status
        mainWifiObj = (WifiManager) a.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int wifiState = mainWifiObj.getWifiState();
        if (wifiState != WifiManager.WIFI_STATE_ENABLED) {
            mainWifiObj.setWifiEnabled(true); /* this does not work anymore there is no workaround for API > 29 (Q) */
        }
        Log.i("[NFCaffee]", "WiFI is enabled!");

        Integer timeout = 59;
        // new from API 10 according to Bard
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {

            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid("NFCoffee");
            builder.setWpa2Passphrase("freecoffee");

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);

            NetworkRequest networkRequest = networkRequestBuilder.build();
            ConnectivityManager cm = (ConnectivityManager) a.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    //Use this network object to Send request.
                    //eg - Using OkHttp library to create a service request
                    //Service is an OkHttp interface where we define docs. Please read OkHttp docs
                    super.onAvailable(network);
                    ConnectivityManager connectivityManager = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
                    connectivityManager.bindProcessToNetwork(network);
                    Log.e("[NFCaffee]", "Conn: "+ network);
                }
            };
            cm.requestNetwork(networkRequest, networkCallback);

            WifiInfo wifiInfo;
            wifiInfo = mainWifiObj.getConnectionInfo();
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
                    Log.w("[NFCaffee]", "Waiting "+String.valueOf(timeout));
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (0 == timeout) {
                //publishProgress(" ERROR: " + ssid + " not found" + System.getProperty("line.separator"));
                Log.e("[NFCaffee]", "Error connection timeout!");
                return null;
            }
            int ip = wifiInfo.getIpAddress();
            String result = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
            Log.i("[NFCaffee]", "IP:" + result);
        }
        return true;
    }
}
