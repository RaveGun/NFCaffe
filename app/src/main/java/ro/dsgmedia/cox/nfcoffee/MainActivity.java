package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 24-May-17.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 0x3141;
    private final int PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE = 0x3142;
    private final int PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0x3143;
    private final int REQUEST_MANAGE_DOCUMENTS = 0x3144;
    private final int REQUEST_OPEN_TREE_FOR_READ_ACCESS = 0x3145;


    private DrawerLayout mDrawerLayout;
    private TextView mCurrentStatus;
    private ImageButton buttonGetTCP;
    private ScrollView mScrollView;

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

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SQLiteOpenHelper dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        // Code here will be triggered once the drawer closes as we don't want anything to happen so we leave this blank
        ActionBarDrawerToggle mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        // Setup the listener for the navigation drawer
        NavigationView mOptionsView = (NavigationView) findViewById(R.id.navigation_drawer);
        mOptionsView.setNavigationItemSelectedListener(this);

        //calling sync state is necessary or else your hamburger icon wont show up
        mActionBarDrawerToggle.syncState();

        mCurrentStatus = (TextView) findViewById(R.id.mCurrentStatus);
        mCurrentStatus.setMovementMethod(new ScrollingMovementMethod());
        mCurrentStatus.setText("DO NOT FORGET to ERASE the counters!");

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

                    /* use the hand written TaskRunner class to start the ConnectTask callable class */
                    TaskRunner taskRunner = new TaskRunner();
                    taskRunner.executeAsync(new ConnectTask(), (data) -> {
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

        if (i == R.id.navigation_item_help) {
            // there is no help implemented
            Toast.makeText(getApplicationContext(), "Help is not available now.", Toast.LENGTH_LONG).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // granted permission for WiFi
        // all good
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MANAGE_DOCUMENTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with reading the file
                // call openDocumentTree(); but not from here
            } else {
                // Permission denied, handle appropriately
                Log.d("MyApp", "Permission denied to manage documents");
            }
        } else if (requestCode == REQUEST_OPEN_TREE_FOR_READ_ACCESS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, read the file from the DocumentTree
                // call readFileFromDocumentTree(); but not from here
            } else {
                // Permission denied, handle appropriately
                Log.d("MyApp", "Permission denied to read documents");
            }
        }
        if (requestCode == PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("[NFCaffee]", "COARSE_LOCATION - Granted");
        }
        if (requestCode == PERMISSIONS_REQUEST_CODE_READ_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("[NFCaffee]", "READ_EXTERNAL_STORAGE - Granted");
        }
        if (requestCode == PERMISSIONS_REQUEST_CODE_WRITE_EXTERNAL_STORAGE
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("[NFCaffee]", "WRITE_EXTERNAL_STORAGE - Granted");
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

    /* This is a Callable class
    * @Name: ConnectTask
    * @ReturnType: getTCPData - this is the class from getTCPData.java
    * */
    public class ConnectTask implements Callable<getTCPData> {
        //private final String input;

        /*public ConnectTask(String input) {
            this.input = input;
        }*/

        @Override
        public getTCPData call() throws Exception {
            // Call here connect to the wifi and then get tcp
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
