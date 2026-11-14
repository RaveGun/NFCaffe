package ro.dsgmedia.cox.nfcoffee;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TCPTransfer {

    public static String RevertBase36(String inCodeName) {
        String[] splited;
        splited = inCodeName.split("\\.");
        inCodeName = splited[0]+splited[1];
        BigInteger NFCIdValue = new BigInteger(inCodeName, 36);
        return NFCIdValue.toString(16).toUpperCase(Locale.ROOT);
    }

    public static Boolean tcpTransfer(Activity a, SQLiteDatabase myDB, @Nullable StatusListener status){
        HashMap<String,String> ReceivedData = new HashMap<String, String>();
        ScrollView mScrollView;
        mScrollView = (ScrollView) a.findViewById(R.id.mScroll);
        getTCPData mTcpClient = null;
        // start transfer task
        Log.i("[NFCaffee]", "Starting the transfer!");
        report(status, "Reading data from the reader...");
        //we create a TCPClient object and
        mTcpClient = new getTCPData(new getTCPData.OnMessageReceived() {
            @Override
            //here the messageReceived method is implemented
            public void messageReceived(String message) {
                //this method calls the onProgressUpdate
                String[] separated = message.split(",");
                if (separated.length < 2) {
                    Log.w("[NFCaffee]", "Skipping malformed line: " + message);
                    report(status, "Skipped malformed line: " + message);
                    return;
                }
                Log.i(separated[0], separated[1]);
                ReceivedData.put(separated[0], separated[1]);
                report(status, "  Card " + separated[0] + ": +" + separated[1]);
            }
        });
        mTcpClient.run();

        // process received data on success
        Log.i("[NFCaffee]", "Data received!");

        if (mScrollView != null) {
            mScrollView.post(new Runnable() {
                @Override
                public void run() {
                    mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

        if (ReceivedData.isEmpty()) {
            report(status, "No data was received from the reader.");
            report(status, "Finished.");
            return true;
        }

        int updated = 0;
        int added = 0;

        // start with the SQL database update
        ContentValues data=new ContentValues();
        for (Map.Entry<String,String> entry: ReceivedData.entrySet()) {
            String codeName = entry.getKey();
            String value = entry.getValue();
            String NFCID = RevertBase36(codeName);

            int Coffees = 0;

            /* always read back the number of coffees stored on the phone */
            Cursor rawQuerry = myDB.rawQuery(
                    "SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " WHERE " + SqlHelper_NFCIDs.CODE_NAME + " = ?",
                    new String[]{codeName});
            if (0 != rawQuerry.getCount()) {
                    /* if the name exist read the value
                    otherwise it will be added automatically later */
                rawQuerry.moveToFirst();
                Coffees = rawQuerry.getInt(4);
            }
            rawQuerry.close();

            /* add the number of coffees from the NFC reader */
            Coffees += Integer.valueOf(value);

            /* update the data with the new value */
            data.put(SqlHelper_NFCIDs.NB_COFFEES, Coffees);

            if (0 == myDB.update(SqlHelper_NFCIDs.TABLE_NAME, data,
                    SqlHelper_NFCIDs.CODE_NAME + " = ?", new String[]{codeName})) {
                myDB.beginTransaction();
                try {
                    data.put(SqlHelper_NFCIDs.NFC_ID, NFCID);
                    data.put(SqlHelper_NFCIDs.CODE_NAME, codeName);
                    myDB.insert(SqlHelper_NFCIDs.TABLE_NAME, null, data);
                    myDB.setTransactionSuccessful();
                    added++;
                } finally {
                    myDB.endTransaction();
                }
            } else {
                updated++;
            }
            data.clear();
        }

        report(status, "Saved " + ReceivedData.size() + " card(s): "
                + updated + " updated, " + added + " new.");
        report(status, "Finished.");
        return true;
    }

    private static void report(@Nullable StatusListener status, String message) {
        if (status != null) {
            status.onStatus(message);
        }
    }
}
