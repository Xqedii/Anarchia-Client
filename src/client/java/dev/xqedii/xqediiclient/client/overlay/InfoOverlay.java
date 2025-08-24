package dev.xqedii.xqediiclient.client.overlay;

import dev.xqedii.xqediiclient.client.gui.GUIHelper;
import dev.xqedii.xqediiclient.client.sniper.AuctionSniper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InfoOverlay {
    public static final Logger LOGGER = LoggerFactory.getLogger("XqediiClientOverlay");
    private static int purchasedItems = 0;
    private static String currentPage = "?";
    private static String maxPage = "?";

    private static final int OVERLAY_WIDTH = 150;
    private static final int OVERLAY_HEIGHT = 55;
    private static final long ANIM_DURATION = 400;
    private static final int BACKGROUND_COLOR = 0xE01A1A1A;
    private static final int OPAQUE_BACKGROUND_COLOR = 0xFF000000 | (BACKGROUND_COLOR & 0x00FFFFFF);

    private static long transitionStartTime = 0;
    private static boolean wasRunning = false;

    public static void onItemPurchased() {
        purchasedItems++;
    }

    public static void render(DrawContext context) {
        boolean isRunning = AuctionSniper.INSTANCE.isRunning();

        // Wykryj zmianę stanu (start/stop) i zresetuj czas animacji
        if (isRunning != wasRunning) {
            transitionStartTime = System.currentTimeMillis();
            wasRunning = isRunning;
        }

        // Jeśli nie ma animacji do wykonania, nie rób nic
        if (!isRunning && (System.currentTimeMillis() - transitionStartTime > ANIM_DURATION)) {
            if (purchasedItems > 0) purchasedItems = 0; // Zresetuj licznik po schowaniu
            return;
        }

        long age = System.currentTimeMillis() - transitionStartTime;
        double progress = Math.min(1.0, (double) age / ANIM_DURATION);

        double slideProgress = isRunning ? easeInOutCubic(progress) : 1.0 - easeInOutCubic(progress);

        drawOverlay(context, slideProgress);

        if (isRunning) {
            updatePageInfo();
        }
    }

    private static void drawOverlay(DrawContext context, double slideProgress) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();

        int startY = -OVERLAY_HEIGHT;
        int endY = 10;
        int currentY = (int) (startY + (endY - startY) * slideProgress);
        int currentX = (screenWidth - OVERLAY_WIDTH) / 2;

        // POPRAWKA: Dwuetapowe rysowanie tła
        GUIHelper.drawAntiAliasedRoundedRect(context, currentX, currentY, OVERLAY_WIDTH, OVERLAY_HEIGHT, 5f, OPAQUE_BACKGROUND_COLOR);
        GUIHelper.drawAntiAliasedRoundedRect(context, currentX, currentY, OVERLAY_WIDTH, OVERLAY_HEIGHT, 5f, BACKGROUND_COLOR);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Information"), currentX + OVERLAY_WIDTH / 2, currentY + 7, 0xFFDEAC25);

        Text pageText = parseColorCodes("Page: &e" + currentPage + "&f/&6" + maxPage);
        context.drawCenteredTextWithShadow(textRenderer, pageText, currentX + OVERLAY_WIDTH / 2, currentY + 22, 0xFFFFFF);

        Text purchasedText = parseColorCodes("Purchased: &a" + purchasedItems + " Items");
        context.drawCenteredTextWithShadow(textRenderer, purchasedText, currentX + OVERLAY_WIDTH / 2, currentY + 35, 0xFFFFFF);
    }

    private static void updatePageInfo() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HandledScreen) {
            Text title = client.currentScreen.getTitle();
            String titleStr = textToString(title);

            Pattern pattern = Pattern.compile("\\((\\d+)/(\\d+)\\)");
            Matcher matcher = pattern.matcher(titleStr);

            if (matcher.find()) {
                currentPage = matcher.group(1);
                maxPage = matcher.group(2);
            }
        }
    }

    private static String textToString(Text text) {
        StringBuilder sb = new StringBuilder();
        text.visit(part -> {
            sb.append(part);
            return java.util.Optional.empty();
        });
        return sb.toString();
    }

    private static Text parseColorCodes(String text) {
        MutableText result = Text.empty();
        String[] parts = text.split("(?=&[0-9a-fk-or])");
        for (String part : parts) {
            if (part.startsWith("&") && part.length() > 1) {
                Formatting formatting = Formatting.byCode(part.charAt(1));
                if (formatting != null) {
                    result.append(Text.literal(part.substring(2)).formatted(formatting));
                } else {
                    result.append(Text.literal(part));
                }
            } else {
                result.append(Text.literal(part).formatted(Formatting.WHITE));
            }
        }
        return result;
    }

    private static double easeInOutCubic(double x) {
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
    }
}