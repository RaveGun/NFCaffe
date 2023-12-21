package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Calendar;


public class ShareData extends AppCompatActivity {

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        // define handles for all used items
        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();
        EditText exportedData = findViewById(R.id.exportData);
        Button copyDataButton = (Button) findViewById(R.id.copyData);
        Button pasteDataButton = (Button) findViewById(R.id.pasteData);
        Button saveDataButton = (Button) findViewById(R.id.saveData);
        Button closeButton = (Button) findViewById(R.id.closeNoSave);

        int value = -1;
        Bundle action = getIntent().getExtras();
        if(action != null) {
            value = action.getInt("action");
        }
        if(value == 1) {
            // Export action was called
            // disable the Save button
            copyDataButton.setVisibility(View.VISIBLE);
            pasteDataButton.setVisibility(View.INVISIBLE);
            saveDataButton.setVisibility(View.INVISIBLE);
            closeButton.setVisibility(View.VISIBLE);
            exportDataToDocumentsStorageDir();
        } else if (value == 2) {
            // Import was called
            copyDataButton.setVisibility(View.INVISIBLE);
            pasteDataButton.setVisibility(View.VISIBLE);
            saveDataButton.setVisibility(View.VISIBLE);
            closeButton.setVisibility(View.VISIBLE);
        }
    }

    public void btnCopyPressed(View v) {

    }
    public void btnPastePressed(View v) {

    }
    public void btnSavePressed(View v) {
        importDataToDocumentsStorageDir();
        finish();
    }
    public void btnClosePressed(View v) {
        finish();
    }

    ////////////////////////////////////
    // PRIVATE methods

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


    private boolean exportDataToDocumentsStorageDir() {
        EditText exportedData = findViewById(R.id.exportData);
        // Export here the IDs to Download folder
        if (false == isExternalStorageWritable()) {
            Toast.makeText(ShareData.this, "External storage not available!", Toast.LENGTH_SHORT).show();
            return false;
        }
        // Generate file name from Date
        Calendar c = Calendar.getInstance();
        String cYear = String.format("%04d", c.get(Calendar.YEAR));
        String cMonth = String.format("%02d", 1 + c.get(Calendar.MONTH));
        String cDay = String.format("%02d", c.get(Calendar.DAY_OF_MONTH));
        String fName = "export_" + cYear + cMonth + cDay + ".csv";
        // Get the directory for the user's public documents directory.
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        // Handle to the file object
        File file = new File(path, fName);
        try {
            path.mkdir();
            OutputStream os = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(os);

            //Write header
            String header = "NFCID,CODENAME,UNAME,EMAIL,COFF\n";
            osw.write(header);
            exportedData.setText(header);

            Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
            rawQuerry.moveToFirst();
            for (int i = 0; i < rawQuerry.getCount(); i++) {
                String NFCIDString = rawQuerry.getString(0);
                String CodeNameString = rawQuerry.getString(1);
                String UserName = rawQuerry.getString(2);
                String Email = rawQuerry.getString(3);
                String Coffees = String.valueOf(rawQuerry.getInt(4));
                String csvLine = NFCIDString + "," + CodeNameString + "," + UserName + "," + Email + "," + Coffees + "\n";
                osw.write(csvLine);
                exportedData.append(csvLine);
                rawQuerry.moveToNext();
            }

            rawQuerry.close();
            osw.flush();
            osw.close();
            os.flush();
            os.close();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(this,
                    new String[]{file.toString()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("STORAGE", "Scanned " + path + ":");
                            Log.i("STORAGE", "-> uri=" + uri);
                        }
                    });
            return true;
        } catch (IOException e) {
            Log.w("STORAGE", "Error writing " + file, e);
            return false;
        }
    }

    private boolean importDataToDocumentsStorageDir() {
        EditText exportedData = findViewById(R.id.exportData);
        if(exportedData.getText().length() > 0){
            String textString = exportedData.getText().toString();
            String[] allLines = textString.split("\n");

            if ("NFCID,CODENAME,UNAME,EMAIL,COFF".equals(allLines[0])) {
                // first line is the correct one
                ContentValues data=new ContentValues();
                for (String line: Arrays.copyOfRange(allLines, 1, allLines.length)) {
                    Log.d("[NFCaffee]", "found: "+line+ " "+ line.length());
                    if (line.length() > 0)
                    {
                        String[] cells = line.split(",");
                        String NFCID = cells[0];
                        String codeName = cells[1];
                        String userName = cells[2];
                        String eMail = cells[3];
                        Integer numCoff = Integer.valueOf(cells[4]);

                        // always read back the number of coffees stored on the phone
                        int Coffees = 0;
                        Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM "+SqlHelper_NFCIDs.TABLE_NAME+" WHERE CODENAME='" + codeName + "';", null);
                        if (0 != rawQuerry.getCount()) {
                            // if the name exist read the value otherwise it will be added automatically later
                            rawQuerry.moveToFirst();
                            Coffees = rawQuerry.getInt(4);
                        }
                        rawQuerry.close();
                        // add the number of coffees from the NFC reader
                        Coffees += Integer.valueOf(numCoff);

                        // update the data with the new value
                        data.put(SqlHelper_NFCIDs.NFC_ID, NFCID);
                        data.put(SqlHelper_NFCIDs.CODE_NAME, codeName);
                        data.put(SqlHelper_NFCIDs.USER_NAME, userName);
                        data.put(SqlHelper_NFCIDs.USER_EMAIL, eMail);
                        data.put(SqlHelper_NFCIDs.NB_COFFEES, Coffees);
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
            }
        }
        return true;
    }

}
