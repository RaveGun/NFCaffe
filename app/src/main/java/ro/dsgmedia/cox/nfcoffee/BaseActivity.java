package ro.dsgmedia.cox.nfcoffee;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;

/**
 * Applies the Material You overlay before the views are inflated.
 *
 * <p>This lives in an activity base class rather than in Application because
 * the overlay must be re-evaluated every time an activity is created: toggling
 * the preference and recreating the activity has to switch the palette, and
 * {@code DynamicColors.applyToActivitiesIfAvailable()} would freeze the choice
 * at process start.</p>
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (Prefs.useDynamicColour(this)) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
    }
}
