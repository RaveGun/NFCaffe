package ro.dsgmedia.cox.nfcoffee;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import com.github.clans.fab.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ManageIDs extends AppCompatActivity {

    private ListView mList;
//    private ArrayList<String> arrayList;
//    private ClientListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_ids);

/*        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);*/


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.NFCReadNewID);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent ReadNFCIntent = new Intent(ManageIDs.this, NFCReadCard.class);
                startActivity(ReadNFCIntent);
            }
        });

        mList = (ListView) findViewById(R.id.listOfIDs);



        final HashMap<String, String> nameNFCID = new HashMap<>();

        // Get the know IDs from the database and put them in the HashMap
        SQLiteDatabase mydatabase = openOrCreateDatabase("NFCoffee",MODE_PRIVATE, null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS NFCIDTable(NFCID TEXT NOT NULL PRIMARY KEY, CODENAME TEXT NOT NULL, UNAME TEXT, EMAIL TEXT);");

        Cursor rawQuerry = mydatabase.rawQuery("select * from NFCIDTable order by UNAME", null);
        rawQuerry.moveToFirst();
        for(int i = 0; i < rawQuerry.getCount(); i++)
        {
            String NFCIDString = rawQuerry.getString(0);
            String CodeNameString = rawQuerry.getString(1);
            String UserName = rawQuerry.getString(2);
            String Email = rawQuerry.getString(3);
            nameNFCID.put(UserName, CodeNameString);
            rawQuerry.moveToNext();
        }
        rawQuerry.close();
        mydatabase.close();

        List<HashMap<String, String>> listHItems = new ArrayList<>();
        SimpleAdapter adapter = new SimpleAdapter(ManageIDs.this, listHItems, R.layout.list_item,
                new String[]{"First Line", "Second Line"},
                new int[]{R.id.list_item_text_view, R.id.list_item_subtext_view});
        Iterator it = nameNFCID.entrySet().iterator();
        while(it.hasNext()) {
            HashMap<String, String> resultsMap = new HashMap<>();
            Map.Entry pair = (Map.Entry)it.next();
            resultsMap.put("First Line", pair.getKey().toString());
            resultsMap.put("Second Line", pair.getValue().toString());
            listHItems.add(resultsMap);
        }
        mList.setAdapter(adapter);


        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<String> selectedID = new ArrayList<String>(nameNFCID.values());
                Log.i("Name:", selectedID.get(position));

                Intent EditIDIntent = new Intent(ManageIDs.this, EditID.class);
                EditIDIntent.putExtra("CODE_NAME", selectedID.get(position));
                startActivity(EditIDIntent);
            }
        });

/*      // notify the adapter that the data set has changed. This means that new message received
        // from server was added to the list
        mAdapter.notifyDataSetChanged();
*/
    }
}
