package ro.dsgmedia.cox.nfcoffee;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class NFCReadCard extends BaseActivity implements NfcAdapter.ReaderCallback {

    public static final String TAG = "NfcDemo";
    public static final String MIME_TEXT_PLAIN = "text/plain";

    /** Every tag technology the reader should accept. */
    private static final int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A
                    | NfcAdapter.FLAG_READER_NFC_B
                    | NfcAdapter.FLAG_READER_NFC_F
                    | NfcAdapter.FLAG_READER_NFC_V
                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    private NfcAdapter mNfcAdapter;
    TextView textViewInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfcread_card);
        WindowInsetsUtil.applySystemBarInsets(this, findViewById(android.R.id.content));

        textViewInfo = (TextView) findViewById(R.id.readIDText);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC is disabled.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            // Reader mode replaces the deprecated enableForegroundDispatch() + PendingIntent setup.
            mNfcAdapter.enableReaderMode(this, this, READER_FLAGS, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(this);
        }
    }

    /**
     * Called on a binder thread whenever a tag is discovered while this activity
     * is in the foreground.
     */
    @Override
    public void onTagDiscovered(@NonNull Tag tag) {
        final byte[] id = tag.getId();
        if (id == null || id.length == 0) {
            Log.w("[NfcDemo]", "Tag without an ID was ignored");
            return;
        }
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Intent editIDIntent = new Intent(NFCReadCard.this, EditID.class);
            editIDIntent.putExtra("NFC_ID", id);
            startActivity(editIDIntent);
        });
    }
}
