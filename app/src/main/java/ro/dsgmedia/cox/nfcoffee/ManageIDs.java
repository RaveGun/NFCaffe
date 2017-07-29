package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ManageIDs extends AppCompatActivity {

    private ListView mList;
    private MenuItem mDeleteID;
    private LinkedHashMap<String, String> nameNFCID = new LinkedHashMap<>();;
    private int toDeleteItemPosition;
    FloatingActionMenu fabMAIN;

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_ids);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabDELC = (FloatingActionButton) findViewById(R.id.NFCClearCounters);
        FloatingActionButton fabRNID = (FloatingActionButton) findViewById(R.id.NFCReadNewID);
        FloatingActionButton fabEXID = (FloatingActionButton) findViewById(R.id.NFCExportIDs);
        FloatingActionButton fabIMID = (FloatingActionButton) findViewById(R.id.NFCImportIDs);
        fabMAIN = (FloatingActionMenu) findViewById(R.id.openOptionsIDs);

        fabDELC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Delete counters
                // Will be done latter.
                if(true == clearAllCounters()) {
                    updateListView();
                    Toast.makeText(ManageIDs.this, "All counters were cleared!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManageIDs.this, "UNKNOWN ERROR!!!", Toast.LENGTH_SHORT).show();
                }
                fabMAIN.close(true);
            }
        });

        fabEXID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Export here the IDs to Download folder
                if(false == isExternalStorageWritable()) {
                    Toast.makeText(ManageIDs.this, "External storage not available!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Calendar c = Calendar.getInstance();
                String cYear = String.valueOf(c.get(Calendar.YEAR));
                String cMonth = String.valueOf(c.get(Calendar.MONTH));
                String cDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
                String fName = "export_"+cYear+cMonth+cDay+".csv";
                if(true == exportDataToDocumentsStorageDir(fName)) {
                    Toast.makeText(ManageIDs.this, fName+" has been saved.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManageIDs.this, "UNKNOWN ERROR!!!", Toast.LENGTH_SHORT).show();
                }
                fabMAIN.close(true);
            }
        });

        fabIMID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Import here the IDs to Download folder
                // Will be done latter.
                if(false == isExternalStorageReadable()) {
                    Toast.makeText(ManageIDs.this, "External storage not available!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(ManageIDs.this, "Storage available for reading.", Toast.LENGTH_SHORT).show();
                fabMAIN.close(true);
            }
        });

        fabRNID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent ReadNFCIntent = new Intent(ManageIDs.this, NFCReadCard.class);
                startActivity(ReadNFCIntent);
            }
        });

        mList = (ListView) findViewById(R.id.listOfIDs);
    }

    @Override
    protected void onResume() {
        super.onResume();

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        updateListView();

        fabMAIN.close(true);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mDeleteID.setVisible(false);
                mList.setItemChecked(mList.getSelectedItemPosition(), false);
                toDeleteItemPosition = -1;

                ArrayList<String> selectedID = new ArrayList<String>(nameNFCID.keySet());
                Log.i("Name:", selectedID.get(position));

                Intent EditIDIntent = new Intent(ManageIDs.this, EditID.class);
                EditIDIntent.putExtra("CODE_NAME", selectedID.get(position));
                startActivity(EditIDIntent);
            }
        });

        mList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mDeleteID.setVisible(true);
                mList.setItemChecked(position, true);
                //mList.setSelection(position);
                toDeleteItemPosition = position;
                //mList.smoothScrollToPosition(position);
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        if (mydatabase != null && mydatabase.isOpen())
            mydatabase.close();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manage_ids, menu);
        mDeleteID = menu.findItem(R.id.manageids_delete);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manageids_delete:
                if(-1 != toDeleteItemPosition) {
                    String uName = ((Map.Entry)((java.util.HashMap)mList.getItemAtPosition(toDeleteItemPosition)).entrySet().toArray()[0]).getValue().toString();
                    String whereClause = SqlHelper_NFCIDs.CODE_NAME + " ='" + uName + "'";
                    Toast.makeText(ManageIDs.this, uName + " deleted!", Toast.LENGTH_SHORT).show();
                    mydatabase.delete(SqlHelper_NFCIDs.TABLE_NAME, whereClause, null);
                    mDeleteID.setVisible(false);
                    toDeleteItemPosition = -1;
                    updateListView();
                }
                break;

            default:
                break;
        }
        return true;
    }


    private void updateListView() {

        nameNFCID.clear();

        // Get the know IDs from the database and put them in the HashMap
        Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
        rawQuerry.moveToFirst();

        for(int i = 0; i < rawQuerry.getCount(); i++)
        {
            String NFCIDString = rawQuerry.getString(0);
            String CodeNameString = rawQuerry.getString(1);
            String UserName = rawQuerry.getString(2);
            String Email = rawQuerry.getString(3);
            String Coffees = String.valueOf(rawQuerry.getInt(4));

            nameNFCID.put(CodeNameString, UserName + " [" + Coffees + "]");
            rawQuerry.moveToNext();
        }
        rawQuerry.close();

        List<HashMap<String, String>> listHItems = new ArrayList<>();
        SimpleAdapter adapter = new SimpleAdapter(ManageIDs.this, listHItems, R.layout.list_item,
                new String[]{"First Line", "Second Line"},
                new int[]{R.id.list_item_text_view, R.id.list_item_subtext_view});
        Iterator it = nameNFCID.entrySet().iterator();
        while(it.hasNext()) {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry)it.next();
            resultsMap.put("First Line", pair.getValue().toString());
            resultsMap.put("Second Line", pair.getKey().toString());
            listHItems.add(resultsMap);
        }
        mList.setAdapter(adapter);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private boolean clearAllCounters() {
        ContentValues data=new ContentValues();
        data.put(SqlHelper_NFCIDs.NB_COFFEES, 0);
        if(0 != mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data, null, null)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean exportDataToDocumentsStorageDir(String fileName) {
        // Get the directory for the user's public documents directory.
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, fileName);
        try {
            path.mkdir();
            OutputStream os = new FileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(os);

            //Write header
            osw.write("NFCID,CODENAME,UNAME,EMAIL,COFF\n");

            Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
            rawQuerry.moveToFirst();
            for(int i = 0; i < rawQuerry.getCount(); i++)
            {
                String NFCIDString = rawQuerry.getString(0);
                String CodeNameString = rawQuerry.getString(1);
                String UserName = rawQuerry.getString(2);
                String Email = rawQuerry.getString(3);
                String Coffees = String.valueOf(rawQuerry.getInt(4));
                String csvLine = NFCIDString+","+CodeNameString+","+UserName+","+Email+","+Coffees+"\n";
                osw.write(csvLine);
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
                    new String[] { file.toString() }, null,
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
}
