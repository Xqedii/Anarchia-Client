package dev.xqedii.xqediiclient.client.sniper;

public class PurchaseRecord {
    public String fullItemName;
    public long purchasePrice;
    public long timestamp;

    // Dla Gson
    public PurchaseRecord() {}

    public PurchaseRecord(String fullItemName, long purchasePrice, long timestamp) {
        this.fullItemName = fullItemName;
        this.purchasePrice = purchasePrice;
        this.timestamp = timestamp;
    }
}