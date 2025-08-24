package dev.xqedii.xqediiclient.client.sniper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PurchaseHistoryManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("XqediiClientPurchases");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xqediiclient_purchases.json");

    private static List<PurchaseRecord> records = new ArrayList<>();

    public static List<PurchaseRecord> getRecords() {
        return records;
    }

    public static void addRecord(PurchaseRecord record) {
        records.add(0, record); // Dodaj na początek listy, aby najnowsze były na górze
        saveRecords();
    }

    public static void saveRecords() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(records, writer);
        } catch (IOException e) {
            LOGGER.error("Nie udało się zapisać historii zakupów!", e);
        }
    }

    public static void loadRecords() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type type = new TypeToken<ArrayList<PurchaseRecord>>() {}.getType();
            records = GSON.fromJson(reader, type);
            if (records == null) {
                records = new ArrayList<>();
            }
        } catch (IOException e) {
            LOGGER.error("Nie udało się wczytać historii zakupów!", e);
        }
    }
}