package ro.dsgmedia.cox.nfcoffee;

/**
 * Created by COX on 24-May-17.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
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

    private DrawerLayout mDrawerLayout;
    private TextView mCurrentStatus;
    private ImageButton buttonGetTCP;
    private ScrollView mScrollView;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private SQLiteDatabase mydatabase;

    /**
     * A transfer keeps running on its executor even if the activity is
     * recreated (rotation is allowed now). The flag keeps the button disabled
     * for the new instance so a second transfer cannot add the counters twice.
     */
    private static volatile boolean sTransferRunning = false;

    Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Edge-to-edge is enforced for apps targeting API 35+; handle the insets ourselves.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        WindowInsetsUtil.applySystemBarInsets(this, findViewById(android.R.id.content));

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
        mScrollView = (ScrollView) findViewById(R.id.mScroll);

        // Setup listeners for the two buttons
        buttonGetTCP = (ImageButton) findViewById(R.id.getTcpData);
        buttonGetTCP.setEnabled(!sTransferRunning);
        buttonGetTCP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // detect the view that was "clicked"
                int v = view.getId();
                if (v == R.id.getTcpData) {
                    buttonGetTCP.setPressed(true);
                    // Prevent a second transfer from being started while this one runs,
                    // otherwise the counters would be added twice.
                    buttonGetTCP.setEnabled(false);
                    sTransferRunning = true;
                    mCurrentStatus.setText("");

                    /* use the hand written TaskRunner class to start the ConnectTask callable class */
                    TaskRunner taskRunner = new TaskRunner();
                    taskRunner.executeAsync(new ConnectTask(), (data) -> {
                        sTransferRunning = false;
                        buttonGetTCP.setPressed(false);
                        buttonGetTCP.setEnabled(true);
                    });
                }
            }
        });

        registerBackCallback();
    }

    /**
     * Replaces the deprecated onBackPressed() override. Closing the drawer first is
     * the only behaviour the app ever had, so this keeps it identical.
     */
    private void registerBackCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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

    /**
     * Appends one progress line to the on-screen status box.
     *
     * <p>Called from the background worker and from ConnectivityManager callback
     * threads, so it always hops to the main looper before touching views.</p>
     */
    private void appendStatus(String message) {
        uiHandler.post(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            mCurrentStatus.append(message);
            mCurrentStatus.append("\n");
            if (mScrollView != null) {
                mScrollView.post(() -> mScrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    public static class TaskRunner {
        private final Executor executor = Executors.newSingleThreadExecutor(); // change according to your requirements
        private final Handler handler = new Handler(Looper.getMainLooper());

        public interface Callback<R> {
            void onComplete(R result);
        }

        public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
            executor.execute(() -> {
                R result = null;
                try {
                    result = callable.call();
                } catch (Exception e) {
                    // Never let a background failure crash the app.
                    Log.e("[NFCaffee]", "Background task failed", e);
                } finally {
                    R finalResult = result;
                    handler.post(() -> callback.onComplete(finalResult));
                }
            });
        }
    }

    /* This is a Callable class
    * @Name: ConnectTask
    * @ReturnType: getTCPData - this is the class from getTCPData.java
    * */
    public class ConnectTask implements Callable<getTCPData> {
        @Override
        public getTCPData call() throws Exception {
            // Call here connect to the wifi and then get tcp
            try {
                if (Boolean.TRUE.equals(WifiConnect.connectWifi((Activity) context, MainActivity.this::appendStatus))) {
                    TCPTransfer.tcpTransfer((Activity) context, mydatabase, MainActivity.this::appendStatus);
                } else {
                    appendStatus("Transfer aborted.");
                }
            } finally {
                // Always hand the process back to the normal network afterwards.
                WifiConnect.disconnectWifi((Activity) context);
            }
            return null;
        }
    }
}
