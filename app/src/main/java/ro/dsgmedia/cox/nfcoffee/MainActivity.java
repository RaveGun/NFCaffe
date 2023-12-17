package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 24-May-17.
 */

import static android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.database.sqlite.*;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ro.dsgmedia.cox.nfcoffee.WifiConnect;
import ro.dsgmedia.cox.nfcoffee.TCPTransfer;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x3141;
    private int PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0x3142;


    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private NavigationView mOptionsView;
    private TextView mCurrentStatus;
    private ImageButton buttonGetTCP;
    private ScrollView mScrollView;

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
        mCurrentStatus.setText("Please disable DATA connection!\n" +
                "DO NOT FORGET to erase the counters!");

        // Setup listeners for the two buttons
        buttonGetTCP = (ImageButton) findViewById(R.id.getTcpData);
        buttonGetTCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // detect the view that was "clicked"
                int v = view.getId();
                if (v == R.id.getTcpData) {
                    buttonGetTCP.setPressed(true);
                    mCurrentStatus.setText("");
                    //new ConnectTask().execute("");
                    TaskRunner taskRunner = new TaskRunner();
                    taskRunner.executeAsync(new ConnectTask(""), (data) -> {
                        // MyActivity activity = activityReference.get();
                        // activity.progressBar.setVisibility(View.GONE);
                        // populateData(activity, data) ;

                        //loadingLiveData.setValue(false);
                        //dataLiveData.setValue(data);
                        //mCurrentStatus.append((CharSequence) data);
                    });
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        /*
        if(null != mTcpClient)
        {
            // disconnect if connected
            mTcpClient.stopClient();
            mTcpClient = null;
        }
        */
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
        int i = item.getItemId();

        if (i == R.id.navigation_item_settings) {
            // open activity setup WiFi
            //Intent WiFiSetupIntent = new Intent(this, wlanActivity.class);
            //startActivity(WiFiSetupIntent);

            Intent in = new Intent(this, PreferencesActivity.class);
            startActivity(in);

        } else if (i == R.id.navigation_item_manIDs) {
            // open manage IDs activity
            Intent ManageIDsIntent = new Intent(this, ManageIDs.class);
            startActivity(ManageIDsIntent);

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
        if (requestCode == PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // granted permission for WiFi
            // all good
        }
    }


    public static class TaskRunner {
        private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
        private final Handler handler = new Handler(Looper.getMainLooper());

        public interface Callback<R> {
            void onComplete(R result);
        }

        public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
            executor.execute(() -> {
                final R result;
                try {
                    result = callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                handler.post(() -> {
                    callback.onComplete(result);
                });
            });
        }
    }


    //public class ConnectTask extends AsyncTask<String, String, getTCPData> {
    class ConnectTask implements Callable<getTCPData> {
        private final String input;
        public ConnectTask(String input) {
            this.input = input;
        }


        //protected getTCPData doInBackground(String... message) {
        @Override
        public getTCPData call() throws Exception {
            /* Call here connect to the wifi and then get tcp*/

            if(true == WifiConnect.connectWifi((Activity) context)) {
                TCPTransfer.tcpTransfer((Activity)context, mydatabase);
            }

            return null;
        }

        /*
        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            buttonGetTCP.setPressed(true);
            if(values[0].contains("Finished"))
                buttonGetTCP.setPressed(false);
            mCurrentStatus.append(values[0]);
            mCurrentStatus.append(System.getProperty("line.separator"));
        }
        */
    }
}
