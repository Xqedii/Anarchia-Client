package dev.xqedii.xqediiclient.client.gui;

import dev.xqedii.xqediiclient.client.XqediiclientClient;
import dev.xqedii.xqediiclient.client.sniper.AuctionSniper;
import dev.xqedii.xqediiclient.client.sniper.PurchaseHistoryManager;
import dev.xqedii.xqediiclient.client.sniper.PurchaseRecord;
import dev.xqedii.xqediiclient.client.sniper.SnipeItem;
import dev.xqedii.xqediiclient.client.sniper.SnipingManager;
import dev.xqedii.xqediiclient.client.toast.ToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CustomGuiScreen extends Screen {
    // ... (stałe bez zmian, oprócz BALANCE_BOX_COLOR)
    private static final float SAFE_AREA_PERCENTAGE = 0.90f;
    private static final Identifier GUI_LOGO = Identifier.of("xqediiclient", "textures/ui/logo.png");
    private static final Identifier GUI_AUCTION = Identifier.of("xqediiclient", "textures/ui/auction.png");
    private static final int MAIN_CONTENT_COLOR = 0xFF161616;
    private static final int BUTTON_COLOR = 0xFFDEAC25;
    private static final int BUTTON_HOVER_COLOR = 0xFFF0D656;
    private static final int TEXT_COLOR_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_COLOR_BUTTON = 0xFFFFFFFF;
    private static final float TITLE_SCALE = 2.5f;
    private static final float BUTTON_TEXT_SCALE = 1.7f;
    private static final float ANIMATION_TIME_SECONDS = 0.10f;
    private static final int SNIPE_ITEM_BOX_COLOR = 0xFF212121;
    private static final int DURABILITY_FULL_COLOR = 0xFF32A852;
    private static final int DURABILITY_NOT_FULL_COLOR = 0xFFEB4034;
    private static final int PRICE_BOX_COLOR = 0xFF4A3C00;
    private static final int DELETE_BUTTON_COLOR = 0xFFD92121;
    private static final int DELETE_BUTTON_HOVER_COLOR = 0xFFF24444;
    private static final int EDIT_BUTTON_COLOR = 0xFF2182D9;
    private static final int EDIT_BUTTON_HOVER_COLOR = 0xFF44A0F2;
    private static final int DATE_BOX_COLOR = 0xFF424242;

    private long screenOpenTime;
    private static final long UI_ANIMATION_DURATION_MS = 200;
    private static final long LIST_ITEM_DELAY_MS = 30;
    private final float[] buttonHoverProgress = new float[2];
    private float startButtonLeft, startButtonTop, startButtonWidth, startButtonHeight;
    private float addButtonLeft, addButtonTop, addButtonWidth, addButtonHeight;
    private float totalGameScale;
    private long lastRenderTime;
    private double snipeListScrollY = 0;
    private double purchaseListScrollY = 0;
    private int hoveredDeleteButtonIndex = -1;
    private int hoveredEditButtonIndex = -1;
    private int hoveredDurabilityButtonIndex = -1;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM HH:mm");
    private float mainContentHeight;
    private float mainContentGap;

    public CustomGuiScreen() {
        super(Text.empty());
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < buttonHoverProgress.length; i++) {
            buttonHoverProgress[i] = 0.0f;
        }
        this.lastRenderTime = System.nanoTime();
        this.screenOpenTime = System.nanoTime();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (XqediiclientClient.getOpenGuiKey().matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ... (reszta metod bez zmian)

    private void playClickSound() {
        if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of("minecraft", "ui.hud.bubble_pop")), 2.0f, 2.0f));
    }

    private int lerpColor(int from, int to, float progress) {
        float r1=(from>>16)&0xFF, g1=(from>>8)&0xFF, b1=from&0xFF; float r2=(to>>16)&0xFF, g2=(to>>8)&0xFF, b2=to&0xFF;
        return 0xFF000000 | ((int)MathHelper.lerp(progress,r1,r2)<<16) | ((int)MathHelper.lerp(progress,g1,g2)<<8) | (int)MathHelper.lerp(progress,b1,b2);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        long currentTime = System.nanoTime(); float elapsedSeconds = (currentTime - this.lastRenderTime) / 1_000_000_000.0f; this.lastRenderTime = currentTime;
        MinecraftClient client = MinecraftClient.getInstance(); Window window = client.getWindow(); TextRenderer textRenderer = client.textRenderer;

        long timeSinceOpen = (currentTime - screenOpenTime) / 1_000_000;
        float animationProgress = Math.min(1.0f, (float) timeSinceOpen / UI_ANIMATION_DURATION_MS);
        float currentScale = 0.95f + (0.05f * animationProgress);

        float framebufferWidth = window.getFramebufferWidth(); float framebufferHeight = window.getFramebufferHeight();
        float designWidth = 1366.0f; float designHeight = 682.0f; float safeAreaWidth = framebufferWidth * SAFE_AREA_PERCENTAGE; float safeAreaHeight = framebufferHeight * SAFE_AREA_PERCENTAGE;
        float scaleX = safeAreaWidth / designWidth; float scaleY = safeAreaHeight / designHeight;
        float finalContentScale = Math.min(1.0f, Math.min(scaleX, scaleY));
        float finalPhysicalWidth = designWidth * finalContentScale; float finalPhysicalHeight = designHeight * finalContentScale;
        float baseRatioWidth = 385.0f; float finalPhysicalRadius = (10.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarPadding = (9.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarWidth = (23.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalSidebarRadius = (6.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalLogoPadding = (3.0f / baseRatioWidth) * finalPhysicalWidth;
        float finalPhysicalLeft = (framebufferWidth - finalPhysicalWidth) / 2.0f; float finalPhysicalTop = (framebufferHeight - finalPhysicalHeight) / 2.0f;

        MatrixStack matrices = context.getMatrices(); matrices.push();
        this.totalGameScale = framebufferWidth / window.getScaledWidth(); matrices.scale(1.0f / totalGameScale, 1.0f / totalGameScale, 1.0f);

        matrices.translate(finalPhysicalLeft + finalPhysicalWidth / 2f, finalPhysicalTop + finalPhysicalHeight / 2f, 0);
        matrices.scale(currentScale, currentScale, 1.0f);
        matrices.translate(-(finalPhysicalLeft + finalPhysicalWidth / 2f), -(finalPhysicalTop + finalPhysicalHeight / 2f), 0);

        GUIHelper.drawAntiAliasedRoundedRect(context, finalPhysicalLeft, finalPhysicalTop, finalPhysicalWidth, finalPhysicalHeight, finalPhysicalRadius, 0xFF0D0D0D);
        float finalPhysicalSidebarLeft = finalPhysicalLeft + finalPhysicalSidebarPadding; float finalPhysicalSidebarTop = finalPhysicalTop + finalPhysicalSidebarPadding; float finalPhysicalSidebarHeight = finalPhysicalHeight - (finalPhysicalSidebarPadding * 2);
        GUIHelper.drawAntiAliasedRoundedRect(context, finalPhysicalSidebarLeft, finalPhysicalSidebarTop, finalPhysicalSidebarWidth, finalPhysicalSidebarHeight, finalPhysicalSidebarRadius, MAIN_CONTENT_COLOR);
        float logoContainerWidth = finalPhysicalSidebarWidth; float iconX = finalPhysicalSidebarLeft + finalPhysicalLogoPadding; float iconY = finalPhysicalSidebarTop + finalPhysicalLogoPadding; float iconSize = logoContainerWidth - (finalPhysicalLogoPadding * 2);
        if (iconSize > 0) GUIHelper.drawTexture(context, GUI_LOGO, (int) iconX, (int) iconY, (int) iconSize, (int) iconSize);
        float gap = finalPhysicalLogoPadding * 2; float secondIconBoxLeft = finalPhysicalSidebarLeft + finalPhysicalLogoPadding; float secondIconBoxTop = finalPhysicalSidebarTop + logoContainerWidth + gap; float secondIconBoxWidth = finalPhysicalSidebarWidth - (finalPhysicalLogoPadding * 2);
        GUIHelper.drawAntiAliasedRoundedRect(context, secondIconBoxLeft, secondIconBoxTop, secondIconBoxWidth, secondIconBoxWidth, finalPhysicalSidebarRadius, 0xFF1D1D1D);
        float secondIconX = secondIconBoxLeft + finalPhysicalLogoPadding; float secondIconY = secondIconBoxTop + finalPhysicalLogoPadding; float secondIconSize = secondIconBoxWidth - (finalPhysicalLogoPadding * 2);
        if (secondIconSize > 0) GUIHelper.drawTexture(context, GUI_AUCTION, (int) secondIconX, (int) secondIconY, (int) secondIconSize, (int) secondIconSize);

        this.mainContentGap = finalPhysicalSidebarPadding;
        this.mainContentHeight = finalPhysicalSidebarHeight;
        float mainContentLeft = finalPhysicalSidebarLeft + finalPhysicalSidebarWidth + mainContentGap;
        float mainContentTop = finalPhysicalSidebarTop;
        float remainingWidth = finalPhysicalWidth - (finalPhysicalSidebarPadding * 3) - finalPhysicalSidebarWidth;
        float leftBoxWidth = (remainingWidth - mainContentGap) / 2.0f;
        float auctionToolsHeight = (mainContentHeight - mainContentGap) / 2.0f;

        GUIHelper.drawAntiAliasedRoundedRect(context, mainContentLeft, mainContentTop, leftBoxWidth, auctionToolsHeight, finalPhysicalSidebarRadius, MAIN_CONTENT_COLOR);
        drawScaledText(context, "Auction Tools", mainContentLeft + leftBoxWidth / 2, mainContentTop + mainContentGap, TITLE_SCALE * finalContentScale, true);
        float baseButtonHeight = 16.0f; float finalPhysicalButtonHeight = (baseButtonHeight / baseRatioWidth) * finalPhysicalWidth; float buttonPadding = mainContentGap;
        this.startButtonWidth = leftBoxWidth - (buttonPadding * 2); this.startButtonHeight = finalPhysicalButtonHeight; this.startButtonLeft = mainContentLeft + buttonPadding;
        this.startButtonTop = mainContentTop + (auctionToolsHeight - startButtonHeight) / 2.0f + 10; // Przesunięte w dół

        float purchasesTop = mainContentTop + auctionToolsHeight + mainContentGap;
        GUIHelper.drawAntiAliasedRoundedRect(context, mainContentLeft, purchasesTop, leftBoxWidth, auctionToolsHeight, finalPhysicalSidebarRadius, MAIN_CONTENT_COLOR);
        drawScaledText(context, "Recent Purchases", mainContentLeft + leftBoxWidth / 2, purchasesTop + mainContentGap, TITLE_SCALE * finalContentScale, true);
        float purchaseListTopY = purchasesTop + (textRenderer.fontHeight * TITLE_SCALE * finalContentScale) + mainContentGap * 2;
        float purchaseListHeight = auctionToolsHeight - (purchaseListTopY - purchasesTop) - mainContentGap;
        renderPurchaseHistory(context, mainContentLeft, purchaseListTopY, leftBoxWidth, purchaseListHeight, timeSinceOpen);

        float snipingInfoLeft = mainContentLeft + leftBoxWidth + mainContentGap;
        GUIHelper.drawAntiAliasedRoundedRect(context, snipingInfoLeft, mainContentTop, leftBoxWidth, mainContentHeight, finalPhysicalSidebarRadius, MAIN_CONTENT_COLOR);
        drawScaledText(context, "Sniping Info", snipingInfoLeft + leftBoxWidth / 2, mainContentTop + mainContentGap, TITLE_SCALE * finalContentScale, true);
        this.addButtonWidth = leftBoxWidth - (buttonPadding * 2); this.addButtonHeight = finalPhysicalButtonHeight; this.addButtonLeft = snipingInfoLeft + buttonPadding; this.addButtonTop = mainContentTop + mainContentHeight - addButtonHeight - buttonPadding;
        float listTopY = mainContentTop + (textRenderer.fontHeight * TITLE_SCALE * finalContentScale) + mainContentGap * 2;
        float listBottomY = addButtonTop - mainContentGap;
        renderSnipeList(context, snipingInfoLeft, listTopY, leftBoxWidth, listBottomY - listTopY, mouseX, mouseY, timeSinceOpen);

        float physicalMouseX = mouseX * this.totalGameScale; float physicalMouseY = mouseY * this.totalGameScale; float animationChange = elapsedSeconds / ANIMATION_TIME_SECONDS;
        boolean isHoveringStart = isMouseOver(physicalMouseX, physicalMouseY, startButtonLeft, startButtonTop, startButtonWidth, startButtonHeight); buttonHoverProgress[0] = MathHelper.clamp(buttonHoverProgress[0] + (isHoveringStart ? animationChange : -animationChange), 0.0f, 1.0f);
        boolean isHoveringAdd = isMouseOver(physicalMouseX, physicalMouseY, addButtonLeft, addButtonTop, addButtonWidth, addButtonHeight); buttonHoverProgress[1] = MathHelper.clamp(buttonHoverProgress[1] + (isHoveringAdd ? animationChange : -animationChange), 0.0f, 1.0f);
        float buttonRadius = finalPhysicalSidebarRadius / 1.5f; float dynamicButtonTextScale = BUTTON_TEXT_SCALE * finalContentScale;
        drawButton(context, textRenderer, "Start", startButtonLeft, startButtonTop, startButtonWidth, startButtonHeight, buttonRadius, dynamicButtonTextScale, 0);
        drawButton(context, textRenderer, "Add", addButtonLeft, addButtonTop, addButtonWidth, addButtonHeight, buttonRadius, dynamicButtonTextScale, 1);

        matrices.pop();

        renderWatermark(context);
    }

    private void renderSnipeList(DrawContext context, float x, float y, float width, float height, int mouseX, int mouseY, long timeSinceOpen) {
        context.enableScissor((int)x, (int)y, (int)(x + width), (int)(y + height));
        List<SnipeItem> items = SnipingManager.getItems();
        float currentY = y + (float)snipeListScrollY;
        hoveredDeleteButtonIndex = -1;
        hoveredEditButtonIndex = -1;
        hoveredDurabilityButtonIndex = -1;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MatrixStack matrices = context.getMatrices();
        float listPadding = 15f;

        for (int i = 0; i < items.size(); i++) {
            SnipeItem item = items.get(i);
            float boxHeight = 95;
            float elementScale = 1.8f;

            if (currentY + boxHeight > y && currentY < y + height) {
                long itemDelay = i * LIST_ITEM_DELAY_MS;
                float itemAnimProgress = Math.min(1.0f, Math.max(0, (float)(timeSinceOpen - itemDelay) / UI_ANIMATION_DURATION_MS));
                float itemScale = 0.8f + 0.2f * itemAnimProgress;

                matrices.push();
                matrices.translate(x + width/2f, currentY + boxHeight/2f, 0);
                matrices.scale(itemScale, itemScale, 1f);
                matrices.translate(-(x + width/2f), -(currentY + boxHeight/2f), 0);

                GUIHelper.drawAntiAliasedRoundedRect(context, x + listPadding, currentY, width - listPadding * 2, boxHeight, 5, SNIPE_ITEM_BOX_COLOR);

                matrices.push();
                matrices.translate(x + listPadding + 10, currentY + 10, 0);
                matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, Text.literal(item.name).formatted(Formatting.BOLD), 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                float bottomRowY = currentY + boxHeight - 30;

                String durabilityText = item.fullDurability ? "Full" : "Not Full";
                float durabilityTextWidth = textRenderer.getWidth(durabilityText) * elementScale;
                float durabilityBoxWidth = durabilityTextWidth + 10;
                float durabilityBoxHeight = (textRenderer.fontHeight * elementScale) + 8;
                float durabilityBoxX = x + listPadding + 10;
                boolean isHoveringDurability = isMouseOver(mouseX * totalGameScale, mouseY * totalGameScale, durabilityBoxX, bottomRowY, durabilityBoxWidth, durabilityBoxHeight);
                if(isHoveringDurability) hoveredDurabilityButtonIndex = i;
                int durabilityColor = item.fullDurability ? DURABILITY_FULL_COLOR : DURABILITY_NOT_FULL_COLOR;
                if(isHoveringDurability) durabilityColor = lerpColor(durabilityColor, 0xFFFFFFFF, 0.2f);
                GUIHelper.drawAntiAliasedRoundedRect(context, durabilityBoxX, bottomRowY, durabilityBoxWidth, durabilityBoxHeight, 3, durabilityColor);
                matrices.push();
                matrices.translate(durabilityBoxX + 5, bottomRowY + 4, 0);
                matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, durabilityText, 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                String priceText = "$" + String.format("%,d", item.maxCost);
                float priceTextWidth = textRenderer.getWidth(priceText) * elementScale;
                float priceBoxWidth = priceTextWidth + 10;
                float priceBoxHeight = (textRenderer.fontHeight * elementScale) + 8;
                float priceBoxX = durabilityBoxX + durabilityBoxWidth + 8;
                GUIHelper.drawAntiAliasedRoundedRect(context, priceBoxX, bottomRowY, priceBoxWidth, priceBoxHeight, 3, PRICE_BOX_COLOR);
                matrices.push();
                matrices.translate(priceBoxX + 5, bottomRowY + 4, 0);
                matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, Text.literal(priceText).formatted(Formatting.GOLD), 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                float buttonY = currentY + 10;
                float elementHeight = (textRenderer.fontHeight * elementScale) + 8;

                String deleteText = "Delete";
                float deleteTextWidth = textRenderer.getWidth(deleteText) * elementScale;
                float deleteButtonWidth = deleteTextWidth + 12;
                float deleteButtonX = x + width - listPadding - 10 - deleteButtonWidth;
                boolean isHoveringDelete = isMouseOver(mouseX * totalGameScale, mouseY * totalGameScale, deleteButtonX, buttonY, deleteButtonWidth, elementHeight);
                if (isHoveringDelete) hoveredDeleteButtonIndex = i;
                int deleteButtonColor = isHoveringDelete ? DELETE_BUTTON_HOVER_COLOR : DELETE_BUTTON_COLOR;
                GUIHelper.drawAntiAliasedRoundedRect(context, deleteButtonX, buttonY, deleteButtonWidth, elementHeight, 3, deleteButtonColor);
                matrices.push(); matrices.translate(deleteButtonX + 6, buttonY + 4, 0); matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, deleteText, 0, 0, TEXT_COLOR_PRIMARY, true); matrices.pop();

                buttonY += elementHeight + 5;
                String editText = "Edit";
                float editTextWidth = textRenderer.getWidth(editText) * elementScale;
                float editButtonWidth = editTextWidth + 12;
                float editButtonX = x + width - listPadding - 10 - editButtonWidth;
                boolean isHoveringEdit = isMouseOver(mouseX * totalGameScale, mouseY * totalGameScale, editButtonX, buttonY, editButtonWidth, elementHeight);
                if (isHoveringEdit) hoveredEditButtonIndex = i;
                int editButtonColor = isHoveringEdit ? EDIT_BUTTON_HOVER_COLOR : EDIT_BUTTON_COLOR;
                GUIHelper.drawAntiAliasedRoundedRect(context, editButtonX, buttonY, editButtonWidth, elementHeight, 3, editButtonColor);
                matrices.push(); matrices.translate(editButtonX + 6, buttonY + 4, 0); matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, editText, 0, 0, TEXT_COLOR_PRIMARY, true); matrices.pop();

                matrices.pop();
            }
            currentY += boxHeight + 8;
        }
        context.disableScissor();
    }

    private void renderPurchaseHistory(DrawContext context, float x, float y, float width, float height, long timeSinceOpen) {
        context.enableScissor((int)x, (int)y, (int)(x + width), (int)(y + height));
        List<PurchaseRecord> records = PurchaseHistoryManager.getRecords();
        float currentY = y + (float)purchaseListScrollY;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        MatrixStack matrices = context.getMatrices();
        float listPadding = 15f;

        for (int i = 0; i < records.size(); i++) {
            PurchaseRecord record = records.get(i);
            float boxHeight = 60;
            float elementScale = 1.3f;

            if (currentY + boxHeight > y && currentY < y + height) {
                long itemDelay = i * LIST_ITEM_DELAY_MS;
                float itemAnimProgress = Math.min(1.0f, Math.max(0, (float)(timeSinceOpen - itemDelay) / UI_ANIMATION_DURATION_MS));
                float itemScale = 0.8f + 0.2f * itemAnimProgress;

                matrices.push();
                matrices.translate(x + width/2f, currentY + boxHeight/2f, 0);
                matrices.scale(itemScale, itemScale, 1f);
                matrices.translate(-(x + width/2f), -(currentY + boxHeight/2f), 0);

                GUIHelper.drawAntiAliasedRoundedRect(context, x + listPadding, currentY, width - listPadding * 2, boxHeight, 5, SNIPE_ITEM_BOX_COLOR);

                matrices.push();
                matrices.translate(x + listPadding + 10, currentY + 8, 0);
                matrices.scale(elementScale, elementScale, 1);
                String trimmedName = textRenderer.trimToWidth(record.fullItemName, (int)((width - (listPadding*2) - 20) / elementScale));
                context.drawText(textRenderer, Text.literal(trimmedName).formatted(Formatting.WHITE), 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                float bottomRowY = currentY + boxHeight - 22;

                String priceText = "$" + String.format("%,d", record.purchasePrice);
                float priceTextWidth = textRenderer.getWidth(priceText) * elementScale;
                float priceBoxWidth = priceTextWidth + 10;
                float priceBoxHeight = (textRenderer.fontHeight * elementScale) + 8;
                float priceBoxX = x + listPadding + 10;
                GUIHelper.drawAntiAliasedRoundedRect(context, priceBoxX, bottomRowY, priceBoxWidth, priceBoxHeight, 3, PRICE_BOX_COLOR);
                matrices.push();
                matrices.translate(priceBoxX + 5, bottomRowY + 4, 0);
                matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, Text.literal(priceText).formatted(Formatting.GOLD), 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                String dateText = dateFormat.format(new Date(record.timestamp));
                float dateTextWidth = textRenderer.getWidth(dateText) * elementScale;
                float dateBoxWidth = dateTextWidth + 10;
                float dateBoxHeight = (textRenderer.fontHeight * elementScale) + 8;
                float dateBoxX = x + width - listPadding - 10 - dateBoxWidth;
                GUIHelper.drawAntiAliasedRoundedRect(context, dateBoxX, bottomRowY, dateBoxWidth, dateBoxHeight, 3, DATE_BOX_COLOR);
                matrices.push();
                matrices.translate(dateBoxX + 5, bottomRowY + 4, 0);
                matrices.scale(elementScale, elementScale, 1);
                context.drawText(textRenderer, dateText, 0, 0, TEXT_COLOR_PRIMARY, true);
                matrices.pop();

                matrices.pop();
            }
            currentY += boxHeight + 8;
        }
        context.disableScissor();
    }

    private void renderWatermark(DrawContext context) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int screenWidth = this.width;
        int screenHeight = this.height;
        int padding = 5;

        Text text1 = Text.literal("Auction Tools").formatted(Formatting.YELLOW);
        int text1Width = textRenderer.getWidth(text1);
        context.drawTextWithShadow(textRenderer, text1, screenWidth - text1Width - padding, screenHeight - textRenderer.fontHeight - padding, 0);

        Text text2 = Text.literal("Created by Xqedii").formatted(Formatting.WHITE);
        int text2Width = textRenderer.getWidth(text2);
        context.drawTextWithShadow(textRenderer, text2, screenWidth - text2Width - padding, screenHeight - (textRenderer.fontHeight * 2) - padding - 2, 0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<SnipeItem> items = SnipingManager.getItems();
            if (hoveredDurabilityButtonIndex != -1 && hoveredDurabilityButtonIndex < items.size()) {
                playClickSound();
                SnipeItem item = items.get(hoveredDurabilityButtonIndex);
                item.fullDurability = !item.fullDurability;
                SnipingManager.updateItem(hoveredDurabilityButtonIndex, item);
                hoveredDurabilityButtonIndex = -1;
                return true;
            }
            if (hoveredDeleteButtonIndex != -1 && hoveredDeleteButtonIndex < items.size()) {
                playClickSound();
                SnipingManager.removeItem(items.get(hoveredDeleteButtonIndex));
                hoveredDeleteButtonIndex = -1;
                return true;
            }
            if (hoveredEditButtonIndex != -1 && hoveredEditButtonIndex < items.size()) {
                playClickSound();
                client.setScreen(new EditSnipeItemScreen(this, hoveredEditButtonIndex));
                hoveredEditButtonIndex = -1;
                return true;
            }

            double physicalMouseX = mouseX * this.totalGameScale; double physicalMouseY = mouseY * this.totalGameScale;
            if (isMouseOver(physicalMouseX, physicalMouseY, startButtonLeft, startButtonTop, startButtonWidth, startButtonHeight)) {
                playClickSound();
                if (!AuctionSniper.INSTANCE.isRunning()) {
                    AuctionSniper.INSTANCE.start();
                    ToastManager.addToast("Started!", "&fPress &eRIGHT SHIFT &fto stop!");
                    this.client.setScreen(null);
                }
                return true;
            }
            if (isMouseOver(physicalMouseX, physicalMouseY, addButtonLeft, addButtonTop, addButtonWidth, addButtonHeight)) {
                playClickSound(); this.client.setScreen(new AddSnipeItemScreen(this));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float midX = this.width / 2.0f;
        if (mouseX < midX) {
            List<PurchaseRecord> records = PurchaseHistoryManager.getRecords();
            if (!records.isEmpty()) {
                float listVisibleHeight = ((this.mainContentHeight - this.mainContentGap) / 2.0f) * 0.7f;
                float listContentHeight = records.size() * (60 + 8);
                if(listContentHeight > listVisibleHeight) {
                    purchaseListScrollY += verticalAmount * 7;
                    purchaseListScrollY = MathHelper.clamp(purchaseListScrollY, -(listContentHeight - listVisibleHeight + 10), 0);
                }
            }
        } else {
            List<SnipeItem> items = SnipingManager.getItems();
            if (!items.isEmpty()) {
                float listVisibleHeight = this.mainContentHeight * 0.7f;
                float listContentHeight = items.size() * (95 + 8);
                if (listContentHeight > listVisibleHeight) {
                    snipeListScrollY += verticalAmount * 7;
                    snipeListScrollY = MathHelper.clamp(snipeListScrollY, -(listContentHeight - listVisibleHeight + 10), 0);
                }
            }
        }
        return true;
    }

    private void drawScaledText(DrawContext context, String text, float x, float y, float scale, boolean centered) {
        MatrixStack matrices = context.getMatrices(); matrices.push();
        float textWidth = textRenderer.getWidth(text) * scale;
        float finalX = centered ? x - textWidth / 2.0f : x;
        matrices.translate(finalX, y, 0); matrices.scale(scale, scale, 1);
        context.drawText(textRenderer, text, 0, 0, TEXT_COLOR_PRIMARY, true); matrices.pop();
    }
    private boolean isMouseOver(double mouseX, double mouseY, float x, float y, float width, float height) { return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height; }
    private void drawButton(DrawContext context, TextRenderer textRenderer, String text, float x, float y, float width, float height, float radius, float textScale, int buttonIndex) { int finalButtonColor = lerpColor(BUTTON_COLOR, BUTTON_HOVER_COLOR, buttonHoverProgress[buttonIndex]); GUIHelper.drawAntiAliasedRoundedRect(context, x, y, width, height, radius, finalButtonColor); Text buttonText = Text.of(text); float buttonTextWidth = textRenderer.getWidth(buttonText) * textScale; float buttonTextX = x + (width - buttonTextWidth) / 2.0f; float buttonTextY = y + (height - (textRenderer.fontHeight * textScale)) / 2.0f; MatrixStack matrices = context.getMatrices(); matrices.push(); matrices.translate(buttonTextX, buttonTextY, 0); matrices.scale(textScale, textScale, 1); context.drawText(textRenderer, buttonText, 0, 0, TEXT_COLOR_BUTTON, true); matrices.pop(); }
    @Override public boolean shouldPause() { return false; }
}