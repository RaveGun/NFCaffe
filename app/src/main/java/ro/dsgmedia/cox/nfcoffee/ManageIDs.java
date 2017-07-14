package ro.dsgmedia.cox.nfcoffee;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Collections;
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

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_ids);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.NFCReadNewID);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent ReadNFCIntent = new Intent(ManageIDs.this, NFCReadCard.class);
                startActivity(ReadNFCIntent);
            }
        });

        mList = (ListView) findViewById(R.id.listOfIDs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateListView();

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
                mList.setSelection(position);
                toDeleteItemPosition = position;
                return true;
            }
        });
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
                //menuItem = item;
                //menuItem.setActionView(R.layout.progressbar);
                //menuItem.expandActionView();
                //TestTask task = new TestTask();
                //task.execute("test");
                if(-1 != toDeleteItemPosition) {
                    Toast.makeText(ManageIDs.this, "Deleting " + ((Map.Entry)((java.util.HashMap)mList.getItemAtPosition(toDeleteItemPosition)).entrySet().toArray()[1]).getValue().toString(), Toast.LENGTH_SHORT).show();
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
        Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM NFCIDTable order by UNAME asc;", null);
        rawQuerry.moveToFirst();

        for(int i = 0; i < rawQuerry.getCount(); i++)
        {
            String NFCIDString = rawQuerry.getString(0);
            String CodeNameString = rawQuerry.getString(1);
            String UserName = rawQuerry.getString(2);
            String Email = rawQuerry.getString(3);
            String Coffees = "0";
            if(rawQuerry.getColumnCount() > 4) {
                Coffees = String.valueOf(rawQuerry.getInt(4));
            } else {
                Coffees = "NA";
            }

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

}
