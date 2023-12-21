package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ManageIDs extends AppCompatActivity {

    private ListView mList;
    private MenuItem mDeleteID;
    private LinkedHashMap<String, String> nameNFCID = new LinkedHashMap<>();
    ;
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
                if (true == clearAllCounters()) {
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
                // Start the edit window to be able to copy and or paste data into the app from the files.
                Intent shareDate = new Intent(ManageIDs.this, ShareData.class);
                Bundle action = new Bundle();
                action.putInt("action", 1);
                shareDate.putExtras(action);
                startActivity(shareDate);
                // Close the menu
                fabMAIN.close(true);
            }
        });

        fabIMID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Import here the IDs to Download folder
                /*
                // Will be done latter.
                // Probably not need as it does not work the edit screen will be used
                if (false == isExternalStorageReadable()) {
                    Toast.makeText(ManageIDs.this, "External storage not available!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(ManageIDs.this, "Storage available for reading.", Toast.LENGTH_SHORT).show();
                }
                */

                // Start the edit window to be able to copy and or paste data into the app from the files.
                Intent shareDate = new Intent(ManageIDs.this, ShareData.class);
                Bundle action = new Bundle();
                action.putInt("action", 2);
                shareDate.putExtras(action);
                startActivity(shareDate);
                // Close the menu
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
        int i = item.getItemId();

        if (i == R.id.manageids_delete) {
            if (-1 != toDeleteItemPosition) {
                String uName = ((Map.Entry) ((java.util.HashMap) mList.getItemAtPosition(toDeleteItemPosition)).entrySet().toArray()[0]).getValue().toString();
                String whereClause = SqlHelper_NFCIDs.CODE_NAME + " ='" + uName + "'";
                Toast.makeText(ManageIDs.this, uName + " deleted!", Toast.LENGTH_SHORT).show();
                mydatabase.delete(SqlHelper_NFCIDs.TABLE_NAME, whereClause, null);
                mDeleteID.setVisible(false);
                toDeleteItemPosition = -1;
                updateListView();
            }
        }
        return true;
    }


    private void updateListView() {

        nameNFCID.clear();

        // Get the know IDs from the database and put them in the HashMap
        Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
        rawQuerry.moveToFirst();

        for (int i = 0; i < rawQuerry.getCount(); i++) {
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
        while (it.hasNext()) {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry) it.next();
            resultsMap.put("First Line", pair.getValue().toString());
            resultsMap.put("Second Line", pair.getKey().toString());
            listHItems.add(resultsMap);
        }
        mList.setAdapter(adapter);
    }

    private boolean clearAllCounters() {
        ContentValues data = new ContentValues();
        data.put(SqlHelper_NFCIDs.NB_COFFEES, 0);
        if (0 != mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data, null, null)) {
            return true;
        } else {
            return false;
        }
    }


    private void openDocumentTree() {
        DocumentFile rootDirectory = DocumentFile.fromTreeUri(this, Uri.parse("content://downloads/public_downloads"));

        if (rootDirectory != null) {
            if (rootDirectory.isDirectory()) {
                // Request access to the root directory of the Downloads folder
                //rootDirectory.requestPermissions(this, REQUEST_OPEN_TREE_FOR_READ_ACCESS);
            } else {
                // Handle the file as a regular file
            }
        } else {
            // Handle the error
        }
    }

    /*
    private void readFileFromDocumentTree() {
        DocumentFile rootDirectory = DocumentFile.fromTreeUri(this, Uri.parse("content://downloads/public_downloads"));

        if (rootDirectory != null) {
            // Locate the file you want to read from the root directory
            DocumentFile file = rootDirectory.findFile("my_file.txt");

            if (file != null) {
                try (InputStream inputStream = file.getContentResolver().openInputStream(file.getUri())) {
                    // Read the file contents into a String
                    String fileContent = readInputStream(inputStream);
                    // Do something with the file content
                    Log.d("MyApp", "File content: " + fileContent);
                } catch (IOException e) {
                }
            }
        }
    }
    */

}

