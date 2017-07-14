package ro.dsgmedia.cox.nfcoffee;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by COX on 25-Jun-17.
 */

public class SqlHelper_NFCIDs extends SQLiteOpenHelper{
    /* NFCIDTable(NFCID TEXT NOT NULL PRIMARY KEY, CODENAME TEXT NOT NULL, UNAME TEXT, EMAIL TEXT, COFF INT) */
    private static final String LOGTAG = "NFCSQL";
    private static final String DATABASE_NAME = "NFCoffee";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "NFCIDTable";
    public static final String NFC_ID = "NFCID";
    public static final String CODE_NAME = "CODENAME";
    public static final String USER_NAME = "UNAME";
    public static final String USER_EMAIL = "EMAIL";
    public static final String NB_COFFEES = "COFF";

    private static final String TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    NFC_ID + " TEXT NOT NULL PRIMARY KEY, " +
                    CODE_NAME + " TEXT NOT NULL, " +
                    USER_NAME + " TEXT, " +
                    USER_EMAIL + " TEXT, " +
                    NB_COFFEES + " INT);";

    public SqlHelper_NFCIDs(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(TABLE_CREATE);
        Log.i(LOGTAG, "The table has been created.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
        Log.i(LOGTAG, "The table has been dropped and recreated.");
    }
}
