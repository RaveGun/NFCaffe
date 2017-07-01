package ro.dsgmedia.cox.nfcoffee;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigInteger;

public class EditID extends AppCompatActivity {
    TextView etCardID, etCodeName, etName, etEMAil, etCoffees;

    Button bSaveData;
    Button bCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // What the hell am I setting here?
        setContentView(R.layout.activity_edit_id);

        etCardID = (TextView) findViewById(R.id.editID_etCardID);
        etCodeName = (TextView) findViewById(R.id.editID_etCodeName);
        etName = (TextView) findViewById(R.id.editID_etName);
        etEMAil = (TextView) findViewById(R.id.editID_etEmail);
        etCoffees = (TextView) findViewById(R.id.editID_etCoffees);

        bSaveData = (Button) findViewById(R.id.editID_bSave);
        bCancel = (Button) findViewById(R.id.editID_bCancel);

        bSaveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SQLiteDatabase mydatabase = openOrCreateDatabase("NFCoffee",MODE_PRIVATE, null);
                mydatabase.execSQL("CREATE TABLE IF NOT EXISTS NFCIDTable(NFCID TEXT NOT NULL PRIMARY KEY, CODENAME TEXT NOT NULL, UNAME TEXT, EMAIL TEXT, COFF INT);");

                Log.i("SQLQuery:", "INSERT OR REPLACE INTO NFCIDTable VALUES (\""+
                        etCardID.getText() + "\",\"" +
                        etCodeName.getText() + "\",\"" +
                        etName.getText() + "\",\"" +
                        etEMAil.getText() + "\",\"" +
                        etCoffees.getText() + "\");");

                mydatabase.execSQL("INSERT OR REPLACE INTO NFCIDTable VALUES (\""+
                        etCardID.getText() + "\",\"" +
                        etCodeName.getText() + "\",\"" +
                        etName.getText() + "\",\"" +
                        etEMAil.getText() + "\",\"" +
                        etCoffees.getText() + "\");");

                mydatabase.close();
                finish();

                /*
                * adb -s 0334a941e0787166 shell
                * run-as ro.dsgmedia.cox.nfcoffee
                * cd /data/data/ro.dsgmedia.cox.nfcoffee
                * ls -l
                * sqlite3 /databases/NFCoffee
                * .tables
                *
                * */
            }
        });

        bCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        byte[] NFCIDValue;
        Bundle extras = getIntent().getExtras();

        if(extras != null)
        {
            // We got info
            NFCIDValue = extras.getByteArray("NFC_ID");
            if (NFCIDValue != null)
            {
                // We got an ID. Set the ID field value.
                String localCodeName = ByteToBase36(NFCIDValue);
                Log.i("RCVID:", ByteArrayToHexString(NFCIDValue));
                Log.i("RCVCN:", localCodeName);

                SQLiteDatabase mydatabase = openOrCreateDatabase("NFCoffee",MODE_PRIVATE, null);
                Cursor rawQuerry = mydatabase.rawQuery("Select * from NFCIDTable where CODENAME='"+localCodeName+"';", null);

                if(0 != rawQuerry.getCount()) {
                    rawQuerry.moveToFirst();
                    String UserName = rawQuerry.getString(2);
                    String Email = rawQuerry.getString(3);
                    String Coffees = String.valueOf(rawQuerry.getInt(4));
                    SetNFCIDTextField(ByteArrayToHexString(NFCIDValue), localCodeName, UserName, Email, Coffees);
                }
                else
                {
                    SetNFCIDTextField(ByteArrayToHexString(NFCIDValue), localCodeName, "", "", "0");
                }
                rawQuerry.close();
                mydatabase.close();
            } else {

                String NFCIDString = extras.getString("CODE_NAME");
                if (NFCIDString != null) {
                    // We got an ID. Set the ID field value.
                    Log.i("RCVCN:", NFCIDString);

                    SQLiteDatabase mydatabase = openOrCreateDatabase("NFCoffee", MODE_PRIVATE, null);
                    Cursor rawQuerry = mydatabase.rawQuery("Select * from NFCIDTable where CODENAME='" + NFCIDString + "';", null);

                    if (0 != rawQuerry.getCount()) {
                        rawQuerry.moveToFirst();
                        String NFCID = rawQuerry.getString(0);
                        String UserName = rawQuerry.getString(2);
                        String Email = rawQuerry.getString(3);
                        String Coffees = String.valueOf(rawQuerry.getInt(4));
                        SetNFCIDTextField(NFCID, NFCIDString, UserName, Email, Coffees);
                    }
                    rawQuerry.close();
                    mydatabase.close();
                }
            }
        }
        else
        {
            // Unknown caller
            return;
        }
    }


    private void SetNFCIDTextField(String valueID, String valueCodeName, String valueName, String valueEmail, String coffees) {
        etCardID.setText(valueID);
        etCodeName.setText(valueCodeName);
        etName.setText(valueName);
        etEMAil.setText(valueEmail);
        etCoffees.setText(coffees);
        //etCardID.setVisibility(View.VISIBLE);
    }

    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    private String ByteToBase36(byte [] inarray) {
        int i, j;
        BigInteger in;
        String [] baseC = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
                            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
                            "U", "V", "W", "X", "Y", "Z"};
        String out= "";

        i =  0;
        in = BigInteger.valueOf(0);
        for(j = 0; j < inarray.length; ++j) {
            BigInteger localByte = BigInteger.valueOf(inarray[j] & 0xFF);
            localByte = localByte.shiftLeft(j*8);
            in = in.or(localByte);
            i++;
        }

        j = 0;
        while (0 != in.compareTo(BigInteger.valueOf(0))) {
            out = baseC[in.mod(BigInteger.valueOf(36)).intValue()] + out;
            in = in.divide(BigInteger.valueOf(36));
            j++;
            if (3 == j) {
                j++;
                out = "." + out;
            }
        }
        while (j <= 11) {
            out = "0" + out;
            j++;
        }
        return out;
    }
}
