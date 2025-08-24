package dev.xqedii.xqediiclient.client.sniper;

// Prosta klasa do przechowywania danych o itemie
public class SnipeItem {
    public String name;
    public boolean fullDurability; // true = Full, false = Not Full
    public long maxCost;

    // Konstruktor bezargumentowy potrzebny dla biblioteki Gson
    public SnipeItem() {}

    public SnipeItem(String name, boolean fullDurability, long maxCost) {
        this.name = name;
        this.fullDurability = fullDurability;
        this.maxCost = maxCost;
    }

    @Override
    public String toString() {
        return name + " | " + (fullDurability ? "Full" : "Not Full") + " | Max: $" + String.format("%,d", maxCost);
    }
}