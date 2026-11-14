package ro.dsgmedia.cox.nfcoffee;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.math.BigInteger;
import java.util.Locale;

public class EditID extends BaseActivity {
    TextView etCardID, etCodeName, etName, etEMAil, etCoffees;
    Button bSaveData;
    Button bCancel;

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_id);
        WindowInsetsUtil.applySystemBarInsets(this, findViewById(android.R.id.content));

        etCardID = (TextView) findViewById(R.id.editID_etCardID);
        etCodeName = (TextView) findViewById(R.id.editID_etCodeName);
        etName = (TextView) findViewById(R.id.editID_etName);
        etEMAil = (TextView) findViewById(R.id.editID_etEmail);
        etCoffees = (TextView) findViewById(R.id.editID_etCoffees);

        bSaveData = (Button) findViewById(R.id.editID_bSave);
        bCancel = (Button) findViewById(R.id.editID_bCancel);

        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();

        bSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ContentValues data = new ContentValues();
                data.put(SqlHelper_NFCIDs.NFC_ID,       String.valueOf(etCardID.getText()));
                data.put(SqlHelper_NFCIDs.CODE_NAME,    String.valueOf(etCodeName.getText()));
                data.put(SqlHelper_NFCIDs.USER_NAME,    String.valueOf(etName.getText()));
                data.put(SqlHelper_NFCIDs.USER_EMAIL,   String.valueOf(etEMAil.getText()));
                data.put(SqlHelper_NFCIDs.NB_COFFEES,   parseCoffees(String.valueOf(etCoffees.getText())));

                String[] whereArgs = new String[]{String.valueOf(etCodeName.getText())};

                if (0 == mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data,
                        SqlHelper_NFCIDs.CODE_NAME + " = ?", whereArgs)) {
                    mydatabase.beginTransaction();
                    try {
                        mydatabase.insert(SqlHelper_NFCIDs.TABLE_NAME, null, data);
                        mydatabase.setTransactionSuccessful();
                    } finally {
                        mydatabase.endTransaction();
                    }
                }

                finish();
            }
        });

        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loadCardFromIntent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // onStop() closes the database; make sure it is usable again if we come back.
        if (mydatabase == null || !mydatabase.isOpen()) {
            mydatabase = dbHelper.getWritableDatabase();
        }
    }

    @Override
    public void onStop() {
        if (mydatabase != null && mydatabase.isOpen())
            mydatabase.close();
        super.onStop();
    }

    private void loadCardFromIntent() {
        byte[] NFCIDValue;
        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            // Unknown caller
            return;
        }

        // We got info
        NFCIDValue = extras.getByteArray("NFC_ID");
        if (NFCIDValue != null) {
            String NFCIDString = ByteArrayToHexString(NFCIDValue);
            // We got an ID. Set the ID field value.
            String localCodeName = ByteToBase36(NFCIDString);
            Log.i("RCVID:", NFCIDString);
            Log.i("RCVCN:", localCodeName);

            Cursor rawQuerry = mydatabase.rawQuery(
                    "SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " WHERE " + SqlHelper_NFCIDs.CODE_NAME + " = ?",
                    new String[]{localCodeName});

            if (0 != rawQuerry.getCount()) {
                rawQuerry.moveToFirst();
                String UserName = rawQuerry.getString(2);
                String Email = rawQuerry.getString(3);
                String Coffees = String.valueOf(rawQuerry.getInt(4));
                SetNFCIDTextField(NFCIDString, localCodeName, UserName, Email, Coffees);
            } else {
                SetNFCIDTextField(NFCIDString, localCodeName, "", "", "0");
            }
            rawQuerry.close();
            return;
        }

        String NFCIDString = extras.getString("CODE_NAME");
        if (NFCIDString != null) {
            // We got an ID. Set the ID field value.
            Cursor rawQuerry = mydatabase.rawQuery(
                    "SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " WHERE " + SqlHelper_NFCIDs.CODE_NAME + " = ?",
                    new String[]{NFCIDString});

            if (0 != rawQuerry.getCount()) {
                rawQuerry.moveToFirst();
                String NFCID = rawQuerry.getString(0);
                String UserName = rawQuerry.getString(2);
                String Email = rawQuerry.getString(3);
                String Coffees = String.valueOf(rawQuerry.getInt(4));
                SetNFCIDTextField(NFCID, NFCIDString, UserName, Email, Coffees);
            }
            rawQuerry.close();
        }
    }

    private static int parseCoffees(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void SetNFCIDTextField(String valueID, String valueCodeName, String valueName, String valueEmail, String coffees) {
        etCardID.setText(valueID);
        etCodeName.setText(valueCodeName);
        etName.setText(valueName);
        etEMAil.setText(valueEmail);
        etCoffees.setText(coffees);
    }

    private String ByteArrayToHexString(byte [] inarray) {
        BigInteger res = BigInteger.valueOf(inarray[inarray.length - 1] & 0xff);
        for(int j = (inarray.length - 2) ; j >= 0 ; j--) {
            res = res.multiply(BigInteger.valueOf(256));
            res = res.add(BigInteger.valueOf(inarray[j] & 0xff));
        }
        return res.toString(16).toUpperCase(Locale.ROOT);
    }

    private String ByteToBase36(String inarray) {
        BigInteger in = new BigInteger(inarray,16);
        String out= in.toString(36).toUpperCase(Locale.ROOT);
        out = String.format(Locale.ROOT, "%1$11s", out).replace(' ', '0');
        String extension = out.substring(out.length() - 3);
        String filename = out.substring(0, out.length() - 3);
        return filename + "." + extension;
    }
}
