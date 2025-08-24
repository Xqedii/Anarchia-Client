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
import java.util.List;

public class SnipingManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("XqediiClientSniper");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "xqediiclient_sniping_items.json");

    private static List<SnipeItem> items = new ArrayList<>();

    public static List<SnipeItem> getItems() {
        return items;
    }

    public static void addItem(SnipeItem item) {
        items.add(item);
        saveItems();
    }

    public static void removeItem(SnipeItem item) {
        items.remove(item);
        saveItems();
    }

    public static void updateItem(int index, SnipeItem updatedItem) {
        if (index >= 0 && index < items.size()) {
            items.set(index, updatedItem);
            saveItems();
        }
    }

    public static void saveItems() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(items, writer);
        } catch (IOException e) {
            LOGGER.error("Nie udało się zapisać listy przedmiotów do snajpienia!", e);
        }
    }

    public static void loadItems() {
        if (!CONFIG_FILE.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Type type = new TypeToken<ArrayList<SnipeItem>>() {}.getType();
            items = GSON.fromJson(reader, type);
            if (items == null) {
                items = new ArrayList<>();
            }
        } catch (IOException e) {
            LOGGER.error("Nie udało się wczytać listy przedmiotów do snajpienia!", e);
        }
    }
}