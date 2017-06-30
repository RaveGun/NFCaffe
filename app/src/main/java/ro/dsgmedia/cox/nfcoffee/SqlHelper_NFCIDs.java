package ro.dsgmedia.cox.nfcoffee;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by COX on 25-Jun-17.
 */

public abstract class SqlHelper_NFCIDs extends SQLiteOpenHelper{
    /* NFCID VARCHAR PRIMARY KEY, CODENAME VARCHAR, UNAME VARCHAR, EMAIL VARCHAR */
    public static final String TABLE_NAME = "NFCIDTable";
    public static final String NFC_ID = "NFCID";
    public static final String CODE_NAME = "CODENAME";
    public static final String USER_NAME = "UNAME";
    public static final String USER_EMAIL = "EMAIL";

    private static final String DATABASE_NAME = "NFCoffee";
    private static final int DATABASE_VERSION = 1;

    private static final String DATABASE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME + "( " + NFC_ID + "text not null primary key, "
            + CODE_NAME + "text not null, "
            + USER_NAME + "text, "
            + USER_EMAIL + "text);";

    public SqlHelper_NFCIDs(Context context) {
        super(context,  DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
    }

}
