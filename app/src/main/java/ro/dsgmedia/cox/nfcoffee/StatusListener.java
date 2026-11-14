package ro.dsgmedia.cox.nfcoffee;

/**
 * Receives human-readable progress lines for the UI.
 *
 * <p>Implementations must be safe to call from a background thread; the
 * implementation in {@link MainActivity} posts to the main looper itself.</p>
 */
public interface StatusListener {

    /** @param message one line, without a trailing newline. */
    void onStatus(String message);
}
