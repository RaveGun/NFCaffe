package ro.dsgmedia.cox.nfcoffee;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class NFCoffeeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Material You: on Android 12+ the brand roles are replaced by the
        // palette derived from the user's wallpaper. On older devices, or when
        // the user has not set a wallpaper theme, the indigo palette in
        // values/colors.xml stays in effect.
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
