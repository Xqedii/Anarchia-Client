package dev.xqedii.xqediiclient.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GUIHelper {
    public static void drawTexture(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        context.drawTexture(RenderLayer::getGuiTextured,
                texture,
                x,
                y,
                0f,
                0f,
                width,
                height,
                width,
                height);
    }

    // Prostsza metoda, która woła bardziej złożoną (dla zachowania kompatybilności)
    public static void drawAntiAliasedRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        drawAntiAliasedRoundedRect(context, x, y, width, height, radius, color, true, true, true, true);
    }

    // NOWA, BARDZIEJ ZAAWANSOWANA METODA
    public static void drawAntiAliasedRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color, boolean tl, boolean tr, boolean bl, boolean br) {
        if (width <= 0 || height <= 0) return;
        float x2 = x + width;
        float y2 = y + height;
        radius = Math.min(Math.min(width, height) / 2.0f, radius);

        // Rysowanie głównych części prostokąta (środek)
        context.fill((int) (x + radius), (int) y, (int) (x2 - radius), (int) y2, color);
        context.fill((int) x, (int) (y + radius), (int) x2, (int) (y2 - radius), color);

        // Rysowanie rogów (warunkowe)
        if (tl) drawOptimizedAntiAliasedQuarterCircle(context, x + radius, y + radius, radius, 0, color);
        else context.fill((int)x, (int)y, (int)(x + radius), (int)(y + radius), color);

        if (tr) drawOptimizedAntiAliasedQuarterCircle(context, x2 - radius, y + radius, radius, 1, color);
        else context.fill((int)(x2 - radius), (int)y, (int)x2, (int)(y + radius), color);

        if (bl) drawOptimizedAntiAliasedQuarterCircle(context, x + radius, y2 - radius, radius, 2, color);
        else context.fill((int)x, (int)(y2 - radius), (int)(x + radius), (int)y2, color);

        if (br) drawOptimizedAntiAliasedQuarterCircle(context, x2 - radius, y2 - radius, radius, 3, color);
        else context.fill((int)(x2 - radius), (int)(y2 - radius), (int)x2, (int)y2, color);
    }

    private static void drawOptimizedAntiAliasedQuarterCircle(DrawContext context, float centerX, float centerY, float radius, int quadrant, int color) {
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF, a = (color >> 24) & 0xFF;
        if (a == 0) a = 255;
        float radiusWithFeatherSq = (radius + 1.0f) * (radius + 1.0f);
        for (int y_offset = 0; y_offset <= Math.ceil(radius) + 1; y_offset++) {
            for (int x_offset = 0; x_offset <= Math.ceil(radius) + 1; x_offset++) {
                float distSq = x_offset * x_offset + y_offset * y_offset;
                if (distSq <= radiusWithFeatherSq) {
                    double dist = Math.sqrt(distSq);
                    double alphaFactor = MathHelper.clamp(1.0 - (dist - (radius - 0.5f)), 0.0, 1.0);
                    if (alphaFactor > 0) {
                        int finalAlpha = (int) (a * alphaFactor);
                        int finalColor = (finalAlpha << 24) | (r << 16) | (g << 8) | b;
                        float drawX = 0, drawY = 0;
                        switch (quadrant) {
                            case 0: drawX = centerX - x_offset; drawY = centerY - y_offset; break;
                            case 1: drawX = centerX + x_offset - 1; drawY = centerY - y_offset; break;
                            case 2: drawX = centerX - x_offset; drawY = centerY + y_offset - 1; break;
                            case 3: drawX = centerX + x_offset - 1; drawY = centerY + y_offset - 1; break;
                        }
                        context.fill((int) drawX, (int) drawY, (int) drawX + 1, (int) drawY + 1, finalColor);
                    }
                }
            }
        }
    }
}