package ro.dsgmedia.cox.nfcoffee;

/** One row in the Manage IDs list. */
public class CardEntry {

    public final String nfcId;
    public final String codeName;
    public final String userName;
    public final String email;
    public final int coffees;

    public CardEntry(String nfcId, String codeName, String userName, String email, int coffees) {
        this.nfcId = nfcId;
        this.codeName = codeName;
        this.userName = userName;
        this.email = email;
        this.coffees = coffees;
    }
}
