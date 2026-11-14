package ro.dsgmedia.cox.nfcoffee;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;


public class ShareData extends AppCompatActivity {

    private static final String CSV_HEADER = "NFCID,CODENAME,UNAME,EMAIL,COFF";

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase mydatabase;

    private EditText exportedData;
    /** Text handed to the document picker when the user confirms "Save". */
    private String csvToSave = "";

    /**
     * Storage Access Framework. The old code wrote straight into
     * /storage/emulated/0/Documents with WRITE_EXTERNAL_STORAGE, which cannot work
     * on any device this app supports (API 30+). The user now picks the target.
     */
    private final ActivityResultLauncher<String> createCsvDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"),
                    uri -> {
                        if (uri != null) {
                            writeCsv(uri);
                        }
                    });

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        WindowInsetsUtil.applySystemBarInsets(this, findViewById(android.R.id.content));

        // define handles for all used items
        dbHelper = new SqlHelper_NFCIDs(this);
        mydatabase = dbHelper.getWritableDatabase();
        exportedData = findViewById(R.id.exportData);
        Button copyDataButton = (Button) findViewById(R.id.copyData);
        Button pasteDataButton = (Button) findViewById(R.id.pasteData);
        Button saveDataButton = (Button) findViewById(R.id.saveData);
        Button closeButton = (Button) findViewById(R.id.closeNoSave);

        int value = -1;
        Bundle action = getIntent().getExtras();
        if (action != null) {
            value = action.getInt("action");
        }
        if (value == 1) {
            // Export action was called
            copyDataButton.setVisibility(View.VISIBLE);
            pasteDataButton.setVisibility(View.GONE);
            saveDataButton.setVisibility(View.GONE);
            closeButton.setVisibility(View.VISIBLE);
            csvToSave = buildCsv();
            exportedData.setText(csvToSave);
            if (savedInstanceState == null) {
                createCsvDocument.launch(defaultFileName());
            }
        } else if (value == 2) {
            // Import was called
            copyDataButton.setVisibility(View.GONE);
            pasteDataButton.setVisibility(View.VISIBLE);
            saveDataButton.setVisibility(View.VISIBLE);
            closeButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        if (mydatabase != null && mydatabase.isOpen())
            mydatabase.close();
        if (dbHelper != null)
            dbHelper.close();
        super.onDestroy();
    }

    public void btnCopyPressed(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("NFCoffee CSV", exportedData.getText()));
        }
    }

    public void btnPastePressed(View v) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, "Clipboard is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "Clipboard is empty.", Toast.LENGTH_SHORT).show();
            return;
        }
        exportedData.setText(clip.getItemAt(0).coerceToText(this));
    }

    public void btnSavePressed(View v) {
        importFromText();
        finish();
    }

    public void btnClosePressed(View v) {
        finish();
    }

    ////////////////////////////////////
    // PRIVATE methods

    private static String defaultFileName() {
        Calendar c = Calendar.getInstance();
        String cYear = String.format(Locale.ROOT, "%04d", c.get(Calendar.YEAR));
        String cMonth = String.format(Locale.ROOT, "%02d", 1 + c.get(Calendar.MONTH));
        String cDay = String.format(Locale.ROOT, "%02d", c.get(Calendar.DAY_OF_MONTH));
        return "export_" + cYear + cMonth + cDay + ".csv";
    }

    /** Builds the same CSV the app always produced, newest code, same columns. */
    private String buildCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append('\n');

        Cursor rawQuerry = mydatabase.rawQuery("SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " ORDER BY " + SqlHelper_NFCIDs.USER_NAME + " ASC;", null);
        rawQuerry.moveToFirst();
        for (int i = 0; i < rawQuerry.getCount(); i++) {
            String NFCIDString = orEmpty(rawQuerry.getString(0));
            String CodeNameString = orEmpty(rawQuerry.getString(1));
            // UNAME/EMAIL are NULL for cards that came straight from the reader,
            // and StringBuilder.append(null) would write the literal "null".
            String UserName = orEmpty(rawQuerry.getString(2));
            String Email = orEmpty(rawQuerry.getString(3));
            String Coffees = String.valueOf(rawQuerry.getInt(4));
            csv.append(NFCIDString).append(',')
                    .append(CodeNameString).append(',')
                    .append(UserName).append(',')
                    .append(Email).append(',')
                    .append(Coffees).append('\n');
            rawQuerry.moveToNext();
        }
        rawQuerry.close();
        return csv.toString();
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private void writeCsv(Uri uri) {
        try (OutputStream os = getContentResolver().openOutputStream(uri, "wt")) {
            if (os == null) {
                throw new IOException("The document provider returned no stream");
            }
            OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);
            writer.write(csvToSave);
            writer.flush();
            Toast.makeText(this, "Export finished.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.w("STORAGE", "Error writing the export", e);
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importFromText() {
        String textString = exportedData.getText().toString();
        if (textString.isEmpty()) {
            return;
        }

        // \R copes with \n, \r\n and \r, e.g. text pasted from a Windows editor.
        String[] allLines = textString.split("\\R");
        if (allLines.length == 0 || !CSV_HEADER.equals(allLines[0].trim())) {
            Toast.makeText(this, "Unknown format: the first line must be " + CSV_HEADER,
                    Toast.LENGTH_LONG).show();
            return;
        }

        ContentValues data = new ContentValues();
        int imported = 0;
        for (int lineNo = 1; lineNo < allLines.length; lineNo++) {
            String line = allLines[lineNo].trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] cells = line.split(",", -1);
            if (cells.length < 5) {
                Log.w("[NFCaffee]", "Skipping malformed line " + lineNo + ": " + line);
                continue;
            }
            String NFCID = cells[0].trim();
            String codeName = cells[1].trim();
            String userName = cells[2].trim();
            String eMail = cells[3].trim();
            int numCoff;
            try {
                numCoff = Integer.parseInt(cells[4].trim());
            } catch (NumberFormatException e) {
                Log.w("[NFCaffee]", "Skipping line " + lineNo + ", bad counter: " + cells[4]);
                continue;
            }

            // always read back the number of coffees stored on the phone
            int Coffees = 0;
            Cursor rawQuerry = mydatabase.rawQuery(
                    "SELECT * FROM " + SqlHelper_NFCIDs.TABLE_NAME + " WHERE " + SqlHelper_NFCIDs.CODE_NAME + " = ?",
                    new String[]{codeName});
            if (0 != rawQuerry.getCount()) {
                // if the name exist read the value otherwise it will be added automatically later
                rawQuerry.moveToFirst();
                Coffees = rawQuerry.getInt(4);
            }
            rawQuerry.close();

            // add the number of coffees from the NFC reader
            Coffees += numCoff;

            // update the data with the new value
            data.put(SqlHelper_NFCIDs.NFC_ID, NFCID);
            data.put(SqlHelper_NFCIDs.CODE_NAME, codeName);
            data.put(SqlHelper_NFCIDs.USER_NAME, userName);
            data.put(SqlHelper_NFCIDs.USER_EMAIL, eMail);
            data.put(SqlHelper_NFCIDs.NB_COFFEES, Coffees);
            if (0 == mydatabase.update(SqlHelper_NFCIDs.TABLE_NAME, data,
                    SqlHelper_NFCIDs.CODE_NAME + " = ?", new String[]{codeName})) {
                mydatabase.beginTransaction();
                try {
                    mydatabase.insert(SqlHelper_NFCIDs.TABLE_NAME, null, data);
                    mydatabase.setTransactionSuccessful();
                } finally {
                    mydatabase.endTransaction();
                }
            }
            data.clear();
            imported++;
        }
        Toast.makeText(this, "Imported " + imported + " entries.", Toast.LENGTH_SHORT).show();
    }

}
