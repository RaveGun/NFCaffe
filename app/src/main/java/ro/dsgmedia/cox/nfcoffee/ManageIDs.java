package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;


public class ManageIDs extends AppCompatActivity implements CardAdapter.OnCardAction {

    private RecyclerView mList;
    private CardAdapter mAdapter;
    private MenuItem mDeleteID;
    private int toDeleteItemPosition = -1;

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

        mList = (RecyclerView) findViewById(R.id.listOfIDs);
        mAdapter = new CardAdapter(this);
        mList.setAdapter(mAdapter);
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

    /** A tap opens the card for editing, exactly as before. */
    @Override
    public void onCardClick(int position) {
        if (mDeleteID != null) {
            mDeleteID.setVisible(false);
        }
        mAdapter.setSelectedPosition(RecyclerView.NO_POSITION);
        toDeleteItemPosition = -1;

        Intent EditIDIntent = new Intent(ManageIDs.this, EditID.class);
        EditIDIntent.putExtra("CODE_NAME", mAdapter.getItem(position).codeName);
        startActivity(EditIDIntent);
    }

    /** A long press selects the row and reveals the delete action. */
    @Override
    public void onCardLongClick(int position) {
        if (mDeleteID != null) {
            mDeleteID.setVisible(true);
        }
        mAdapter.setSelectedPosition(position);
        toDeleteItemPosition = position;
    }


    @Override
    protected void onResume() {
        super.onResume();

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        updateListView();
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
            if (-1 != toDeleteItemPosition && toDeleteItemPosition < mAdapter.getItemCount()) {
                String uName = mAdapter.getItem(toDeleteItemPosition).codeName;
                mydatabase.delete(SqlHelper_NFCIDs.TABLE_NAME,
                        SqlHelper_NFCIDs.CODE_NAME + " = ?", new String[]{uName});
                Toast.makeText(ManageIDs.this, uName + " deleted!", Toast.LENGTH_SHORT).show();
                mDeleteID.setVisible(false);
                toDeleteItemPosition = -1;
                updateListView();
            }
        }
        return true;
    }


    private void updateListView() {

        List<CardEntry> entries = new ArrayList<>();

        // Get the known IDs from the database
        Cursor rawQuerry = mydatabase.rawQuery(
                "SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME
                        + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
        while (rawQuerry.moveToNext()) {
            entries.add(new CardEntry(
                    rawQuerry.getString(0),
                    rawQuerry.getString(1),
                    rawQuerry.getString(2),
                    rawQuerry.getString(3),
                    rawQuerry.getInt(4)));
        }
        rawQuerry.close();

        mAdapter.submit(entries);
    }

    private boolean clearAllCounters() {
        ContentValues data = new ContentValues();
        data.put(SqlHelper_NFCIDs.NB_COFFEES, 0);
        return 0 != mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data, null, null);
    }

}
