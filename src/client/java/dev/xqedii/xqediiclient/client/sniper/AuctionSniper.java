package dev.xqedii.xqediiclient.client.sniper;

import dev.xqedii.xqediiclient.client.overlay.InfoOverlay;
import dev.xqedii.xqediiclient.client.toast.ToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AuctionSniper {
    public static final AuctionSniper INSTANCE = new AuctionSniper();
    public static final Logger LOGGER = LoggerFactory.getLogger("XqediiClientSniper");

    private volatile boolean running = false;
    private Thread sniperThread;

    private static final int[] SLOTS_TO_CHECK = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int CONFIRM_PURCHASE_SLOT = 11;
    private static final int NEXT_PAGE_RETRIES = 3;
    private static final int NEXT_PAGE_BUTTON_WAIT_MS = 250;

    private AuctionSniper() {}

    public boolean isRunning() { return running; }

    public void start() {
        if (running) return;
        running = true;
        sniperThread = new Thread(this::sniperLogic, "Auction-Sniper-Thread");
        sniperThread.start();
    }

    public void stop() {
        running = false;
        if (sniperThread != null && sniperThread.isAlive()) {
            sniperThread.interrupt();
        }
    }

    private String textToString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit(part -> {
            sb.append(part);
            return java.util.Optional.empty();
        });
        return sb.toString();
    }

    private void sniperLogic() {
        MinecraftClient client = MinecraftClient.getInstance();
        int sessionCount = 1;

        while (running) {
            try {
                if (client.player == null) { stop(); return; }
                if (client.currentScreen != null) { client.execute(() -> client.setScreen(null)); Thread.sleep(100); }
                client.execute(() -> client.player.networkHandler.sendChatCommand("ah"));
                Thread.sleep(500);
                if (!(client.currentScreen instanceof HandledScreen)) {
                    LOGGER.warn("Nie udało się otworzyć GUI domu aukcyjnego. Próbuję ponownie...");
                    continue;
                }

                pageLoop:
                while (running) {
                    Thread.sleep(100);
                    if (!(client.currentScreen instanceof HandledScreen<?> screen)) {
                        LOGGER.warn("GUI zostało zamknięte, zaczynam od nowa z /ah");
                        break;
                    }

                    for (int slotId : SLOTS_TO_CHECK) {
                        if (!running) return;
                        if (slotId >= screen.getScreenHandler().slots.size()) continue;
                        ItemStack stack = screen.getScreenHandler().getSlot(slotId).getStack();
                        if (stack == null || stack.isEmpty()) continue;

                        for (SnipeItem target : SnipingManager.getItems()) {
                            long price = checkItemAndGetPrice(stack, target);
                            if (price != -1) {
                                LOGGER.info("ZNALEZIONO: " + target.name + " za $" + price);
                                Thread.sleep(250);

                                final int itemSlotId = slotId;
                                final int ahScreenSyncId = screen.getScreenHandler().syncId;
                                client.execute(() -> client.interactionManager.clickSlot(ahScreenSyncId, itemSlotId, 0, SlotActionType.PICKUP, client.player));
                                LOGGER.info("Etap 1: Kliknięto na przedmiot w slocie " + itemSlotId);
                                Thread.sleep(300);

                                if (client.currentScreen instanceof HandledScreen<?> confirmationScreen) {
                                    ItemStack confirmButton = confirmationScreen.getScreenHandler().getSlot(CONFIRM_PURCHASE_SLOT).getStack();

                                    if (confirmButton != null && confirmButton.isOf(Items.LIME_DYE)) {
                                        Thread.sleep(250);
                                        final int confirmationScreenSyncId = confirmationScreen.getScreenHandler().syncId;
                                        client.execute(() -> client.interactionManager.clickSlot(confirmationScreenSyncId, CONFIRM_PURCHASE_SLOT, 0, SlotActionType.PICKUP, client.player));
                                        LOGGER.info("Etap 2: Potwierdzono zakup (LIME DYE) w slocie " + CONFIRM_PURCHASE_SLOT);

                                        String itemName = textToString(stack.getName());
                                        PurchaseHistoryManager.addRecord(new PurchaseRecord(itemName, price, System.currentTimeMillis()));
                                        ToastManager.addToast("Purchased!", "Item &e" + itemName + " &fhas been purchased for &6$" + String.format("%,d", price) + "&f!");
                                        InfoOverlay.onItemPurchased();

                                        LOGGER.info("Aktywowano 3-sekundowy cooldown po zakupie.");
                                        Thread.sleep(3000);

                                        continue pageLoop;
                                    } else {
                                        // ZAKUP NIEMOŻLIWY - BRAK ŚRODKÓW
                                        LOGGER.warn("Nie można kupić przedmiotu (prawdopodobnie brak środków). Pomięcie.");

                                        // Czekaj na powrót do AH i na cooldown przed skanowaniem następnego itemu
                                        Thread.sleep(300);
                                        // Pętla `for (int slotId...)` będzie kontynuowana od następnego slotu
                                    }
                                } else {
                                    LOGGER.warn("Nie otworzyło się okno potwierdzenia zakupu! Kontynuuję skanowanie.");
                                    Thread.sleep(300);
                                }
                            }
                        }
                    }

                    boolean nextPageFound = false;
                    for (int retry = 0; retry <= NEXT_PAGE_RETRIES; retry++) {
                        if (!(client.currentScreen instanceof HandledScreen<?> currentScreen)) {
                            LOGGER.warn("Ekran został zamknięty podczas oczekiwania na przycisk następnej strony.");
                            break pageLoop;
                        }
                        if (NEXT_PAGE_SLOT >= currentScreen.getScreenHandler().slots.size()) break;

                        ItemStack nextPageButton = currentScreen.getScreenHandler().getSlot(NEXT_PAGE_SLOT).getStack();
                        if (nextPageButton != null && !nextPageButton.isEmpty()) {
                            final int syncId = currentScreen.getScreenHandler().syncId;
                            client.execute(() -> client.interactionManager.clickSlot(syncId, NEXT_PAGE_SLOT, 0, SlotActionType.PICKUP, client.player));
                            Thread.sleep(250);
                            nextPageFound = true;
                            break;
                        }

                        if (retry < NEXT_PAGE_RETRIES) {
                            LOGGER.info("Przycisk 'Next Page' nieznaleziony, próba " + (retry + 1) + "/" + (NEXT_PAGE_RETRIES + 1));
                            Thread.sleep(NEXT_PAGE_BUTTON_WAIT_MS);
                        }
                    }

                    if (!nextPageFound) {
                        sessionCount++;
                        LOGGER.info("Koniec aukcji w tej sesji. Zaczynam sesję #" + sessionCount);
                        ToastManager.addToast("Sniping Finished", "Starting &e#" + sessionCount + " &fsession");
                        break pageLoop;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); running = false;
            } catch (Exception e) {
                LOGGER.error("Błąd w pętli snajpera", e); running = false;
            }
        }
        LOGGER.info("Logika snajpera zakończona.");
    }

    private long checkItemAndGetPrice(ItemStack stack, SnipeItem target) {
        String itemName = textToString(stack.getName()).toLowerCase();

        String targetName = target.name.toLowerCase();

        if (!itemName.contains(targetName)) return -1;

        if (target.fullDurability && stack.isDamaged()) return -1;

        LoreComponent loreComponent = stack.get(DataComponentTypes.LORE);
        if (loreComponent != null) {
            for (Text line : loreComponent.lines()) {
                String loreLine = textToString(line);
                if (loreLine.contains("Koszt")) {
                    try {
                        String costPart = loreLine.split("\\(\\$")[1].replace(")", "").replace(",", "").trim();
                        long price = Long.parseLong(costPart);
                        if (price <= target.maxCost) {
                            return price;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return -1;
    }
}