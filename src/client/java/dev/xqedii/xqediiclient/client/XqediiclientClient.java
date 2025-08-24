package dev.xqedii.xqediiclient.client;

import dev.xqedii.xqediiclient.client.gui.CustomGuiScreen;
import dev.xqedii.xqediiclient.client.overlay.InfoOverlay;
import dev.xqedii.xqediiclient.client.sniper.AuctionSniper;
import dev.xqedii.xqediiclient.client.sniper.PurchaseHistoryManager;
import dev.xqedii.xqediiclient.client.sniper.SnipingManager;
import dev.xqedii.xqediiclient.client.toast.ToastManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class XqediiclientClient implements ClientModInitializer {

    private static KeyBinding openGuiKey;

    public static KeyBinding getOpenGuiKey() { return openGuiKey; }

    @Override
    public void onInitializeClient() {
        SnipingManager.loadItems();
        PurchaseHistoryManager.loadRecords();

        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.xqediiclient.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "key.category.xqediiclient"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            ToastManager.render(drawContext);
            InfoOverlay.render(drawContext);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scancode, modifiers) -> {
                if (openGuiKey.matchesKey(key, scancode) && AuctionSniper.INSTANCE.isRunning()) {
                    AuctionSniper.INSTANCE.stop();
                    ToastManager.addToast("Stopped!", "&fSniper has been &cdisabled&f.");
                    // ZAMKNIJ GUI
                    if (client.player != null) {
                        client.player.closeHandledScreen();
                    }
                }
            });
        });
    }

    private void onClientTick(MinecraftClient client) {
        if (openGuiKey.wasPressed()) {
            if (AuctionSniper.INSTANCE.isRunning()) {
                AuctionSniper.INSTANCE.stop();
                ToastManager.addToast("Stopped!", "&fSniper has been &cdisabled&f.");
                // ZAMKNIJ GUI
                if (client.player != null) {
                    client.player.closeHandledScreen();
                }
            } else if (client.currentScreen == null) {
                client.setScreen(new CustomGuiScreen());
            }
        }
    }
}