package dev.xqedii.xqediiclient.client.toast;

import dev.xqedii.xqediiclient.client.gui.GUIHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToastManager {
    private static final List<Toast> activeToasts = new CopyOnWriteArrayList<>();
    private static final int TOAST_WIDTH = 180;
    private static final int TOAST_HEIGHT = 40;
    private static final int TOAST_PADDING = 10;
    private static final int BACKGROUND_COLOR = 0xE01A1A1A;
    private static final int OPAQUE_BACKGROUND_COLOR = 0xFF000000 | (BACKGROUND_COLOR & 0x00FFFFFF);


    public static void addToast(String title, String message) {
        activeToasts.add(new Toast(title, message, 3000));
    }

    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || activeToasts.isEmpty()) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int yOffset = 0;

        for (int i = activeToasts.size() - 1; i >= 0; i--) {
            Toast toast = activeToasts.get(i);
            if (toast.isExpired()) {
                activeToasts.remove(i);
                continue;
            }

            long age = toast.getAge();
            double slideProgress = 0;

            if (age < Toast.ANIM_DURATION) {
                slideProgress = easeInOutCubic((double) age / Toast.ANIM_DURATION);
            } else if (age > toast.getDuration() + Toast.ANIM_DURATION) {
                long timeOut = age - (toast.getDuration() + Toast.ANIM_DURATION);
                slideProgress = 1.0 - easeInOutCubic((double) timeOut / Toast.ANIM_DURATION);
            } else {
                slideProgress = 1.0;
            }

            int startX = screenWidth;
            int endX = screenWidth - TOAST_WIDTH - TOAST_PADDING;
            int currentX = (int) (startX + (endX - startX) * slideProgress);
            int currentY = TOAST_PADDING + yOffset;

            // ZMIANA: Wywołanie nowej metody rysującej z flagami dla rogów
            // tl=false, tr=true, bl=false, br=true
            GUIHelper.drawAntiAliasedRoundedRect(context, currentX, currentY, TOAST_WIDTH, TOAST_HEIGHT, 5f, OPAQUE_BACKGROUND_COLOR, false, true, false, true);
            GUIHelper.drawAntiAliasedRoundedRect(context, currentX, currentY, TOAST_WIDTH, TOAST_HEIGHT, 5f, BACKGROUND_COLOR, false, true, false, true);

            context.fill(currentX, currentY, currentX + 3, currentY + TOAST_HEIGHT, 0xFFDEAC25);

            context.drawTextWithShadow(textRenderer, Text.literal(toast.getTitle()).formatted(Formatting.GOLD, Formatting.BOLD), currentX + 8, currentY + 7, 0xFFFFFF);

            Text messageText = parseColorCodes(toast.getMessage());
            List<OrderedText> wrappedLines = textRenderer.wrapLines(messageText, TOAST_WIDTH - 16);
            int textY = currentY + 20;
            for (OrderedText line : wrappedLines) {
                if (textY >= currentY + TOAST_HEIGHT - 5) break;
                context.drawTextWithShadow(textRenderer, line, currentX + 8, textY, 0xFFFFFF);
                textY += textRenderer.fontHeight;
            }

            yOffset += TOAST_HEIGHT + 5;
        }
    }

    private static double easeInOutCubic(double x) {
        return x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2;
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
}