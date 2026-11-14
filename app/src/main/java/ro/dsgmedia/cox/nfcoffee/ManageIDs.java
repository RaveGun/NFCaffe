package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private int toDeleteItemPosition;

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_ids);
        WindowInsetsUtil.applySystemBarInsets(this, findViewById(android.R.id.content));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabMenu = (FloatingActionButton) findViewById(R.id.openOptionsIDs);
        fabMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptions();
            }
        });

        mList = (ListView) findViewById(R.id.listOfIDs);
    }

    /**
     * The four actions used to live in a com.github.clans FloatingActionMenu.
     * They are now a Material 3 bottom sheet, with the same behaviour.
     */
    private void showOptions() {
        final BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = getLayoutInflater().inflate(R.layout.sheet_manage_ids, null);
        sheet.setContentView(content);

        content.findViewById(R.id.sheetDeleteCounters).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                onDeleteCounters();
            }
        });
        content.findViewById(R.id.sheetExportIds).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                openShareData(1);
            }
        });
        content.findViewById(R.id.sheetImportIds).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                openShareData(2);
            }
        });
        content.findViewById(R.id.sheetScanId).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sheet.dismiss();
                startActivity(new Intent(ManageIDs.this, NFCReadCard.class));
            }
        });

        sheet.show();
    }

    private void onDeleteCounters() {
        if (true == clearAllCounters()) {
            updateListView();
            Toast.makeText(ManageIDs.this, "All counters were cleared!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ManageIDs.this, "UNKNOWN ERROR!!!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openShareData(int action) {
        Intent shareDate = new Intent(ManageIDs.this, ShareData.class);
        Bundle bundle = new Bundle();
        bundle.putInt("action", action);
        shareDate.putExtras(bundle);
        startActivity(shareDate);
    }


    @Override
    protected void onResume() {
        super.onResume();

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        updateListView();

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mDeleteID != null) {
                    mDeleteID.setVisible(false);
                }
                mList.setItemChecked(position, false);
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
                toDeleteItemPosition = position;
                return true;
            }
        });
    }

    @Override
    public void onStop() {
        if (mydatabase != null && mydatabase.isOpen())
            mydatabase.close();
        if (dbHelper != null)
            dbHelper.close();
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
                Object entry = mList.getItemAtPosition(toDeleteItemPosition);
                String uName = entry instanceof Map
                        ? String.valueOf(((Map<?, ?>) entry).get("Second Line"))
                        : null;
                if (uName != null) {
                    mydatabase.delete(SqlHelper_NFCIDs.TABLE_NAME,
                            SqlHelper_NFCIDs.CODE_NAME + " = ?", new String[]{uName});
                    Toast.makeText(ManageIDs.this, uName + " deleted!", Toast.LENGTH_SHORT).show();
                }
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
        Iterator<Map.Entry<String, String>> it = nameNFCID.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry<String, String> pair = it.next();
            resultsMap.put("First Line", pair.getValue());
            resultsMap.put("Second Line", pair.getKey());
            listHItems.add(resultsMap);
        }
        mList.setAdapter(adapter);
    }

    private boolean clearAllCounters() {
        ContentValues data = new ContentValues();
        data.put(SqlHelper_NFCIDs.NB_COFFEES, 0);
        return 0 != mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data, null, null);
    }

}
