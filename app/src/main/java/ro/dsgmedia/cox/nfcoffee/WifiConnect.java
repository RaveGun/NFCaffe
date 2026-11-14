package ro.dsgmedia.cox.nfcoffee;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Connects the phone to the coffee machine's access point.
 *
 * Replaces the old implementation which polled WifiManager.getConnectionInfo()
 * (deprecated in API 31, and returns "unknown" without location permission on
 * newer Android versions) and never released the network binding.
 */
public final class WifiConnect {

    private static final String TAG = "[NFCaffee]";
    private static final String SSID = "NFCoffee";
    private static final String PASSPHRASE = "freecoffee";

    /** The system dialog needs a moment; the old code effectively waited ~30 s too. */
    private static final int CONNECT_TIMEOUT_MS = 30_000;

    /** Shown after a failed attempt: the reader only broadcasts its AP in transfer mode. */
    private static final String RETRY_HINT =
            "If the reader was idle, put it in transfer mode and press the cup again.";

    private static ConnectivityManager connectivityManager;
    private static ConnectivityManager.NetworkCallback networkCallback;
    private static volatile Network boundNetwork;

    private WifiConnect() {
    }

    /**
     * @param status receives progress lines; may be {@code null}.
     * @return {@code true} when the process is bound to the NFCoffee network,
     *         {@code false} on timeout/refusal. Callers must always call
     *         {@link #disconnectWifi(Activity)} afterwards.
     */
    public static Boolean connectWifi(Activity a, @Nullable StatusListener status) {
        ConnectivityManager cm =
                (ConnectivityManager) a.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            report(status, "Error: no connectivity service on this phone.");
            return false;
        }

        WifiNetworkSpecifier specifier = new WifiNetworkSpecifier.Builder()
                .setSsid(SSID)
                .setWpa2Passphrase(PASSPHRASE)
                .build();

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                // The coffee machine has no internet uplink: without this the request
                // would keep waiting for validation that can never succeed.
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifier)
                .build();

        final CountDownLatch done = new CountDownLatch(1);

        report(status, "Connecting to " + SSID + "...");

        connectivityManager = cm;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                boundNetwork = network;
                cm.bindProcessToNetwork(network);
                Log.i(TAG, "Connected to " + SSID + " as " + network);
                String ip = firstIpAddress(cm, network);
                report(status, "Connected to " + SSID + (ip != null ? " (IP " + ip + ")" : "") + ".");
                done.countDown();
            }

            @Override
            public void onUnavailable() {
                Log.e(TAG, "ERROR: " + SSID + " not found / connection refused");
                report(status, SSID + " was not found, or the connection was refused.");
                report(status, RETRY_HINT);
                done.countDown();
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.w(TAG, "Lost connection to " + network);
                if (network.equals(boundNetwork)) {
                    cm.bindProcessToNetwork(null);
                    boundNetwork = null;
                }
            }
        };

        try {
            cm.requestNetwork(request, networkCallback, CONNECT_TIMEOUT_MS);
            if (!done.await(CONNECT_TIMEOUT_MS + 5_000L, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Error connection timeout!");
                report(status, "Connection to " + SSID + " timed out.");
                report(status, RETRY_HINT);
                disconnectWifi(a);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            disconnectWifi(a);
            return false;
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Unable to request the " + SSID + " network", e);
            report(status, "Could not request the " + SSID + " network: " + e.getMessage());
            disconnectWifi(a);
            return false;
        }

        Network network = boundNetwork;
        if (network == null) {
            return false;
        }
        logIpAddress(cm, network);
        return true;
    }

    /** Unbinds the process and releases the request. Safe to call more than once. */
    public static void disconnectWifi(Activity a) {
        ConnectivityManager cm =
                (ConnectivityManager) a.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.bindProcessToNetwork(null);
        }
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException ignored) {
                // Already released by the timeout.
            }
        }
        connectivityManager = null;
        networkCallback = null;
        boundNetwork = null;
    }

    private static void logIpAddress(ConnectivityManager cm, Network network) {
        String ip = firstIpAddress(cm, network);
        if (ip != null) {
            Log.i(TAG, "IP: " + ip);
        }
    }

    /** @return the first IPv4 address of the network, or {@code null}. */
    private static String firstIpAddress(ConnectivityManager cm, Network network) {
        LinkProperties linkProperties = cm.getLinkProperties(network);
        if (linkProperties == null) {
            return null;
        }
        String fallback = null;
        for (LinkAddress address : linkProperties.getLinkAddresses()) {
            String host = address.getAddress().getHostAddress();
            if (host == null) {
                continue;
            }
            if (address.getAddress() instanceof java.net.Inet4Address) {
                return host;
            }
            if (fallback == null) {
                fallback = host;
            }
        }
        return fallback;
    }

    private static void report(@Nullable StatusListener status, String message) {
        if (status != null) {
            status.onStatus(message);
        }
    }
}
