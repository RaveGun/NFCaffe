package ro.dsgmedia.cox.nfcoffee;

import android.content.Context;
import android.content.SharedPreferences;

/** Wrapper for the app's user preferences (currently just one). */
public final class Prefs {

    private static final String FILE = "settings";
    private static final String KEY_DYNAMIC_COLOUR = "dynamic_colour";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Material You is used unless the user explicitly turns it off. */
    public static boolean useDynamicColour(Context context) {
        return prefs(context).getBoolean(KEY_DYNAMIC_COLOUR, true);
    }

    public static void setUseDynamicColour(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DYNAMIC_COLOUR, value).apply();
    }
}
