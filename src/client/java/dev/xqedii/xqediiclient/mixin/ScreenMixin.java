package dev.xqedii.xqediiclient.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class ScreenMixin {

    @Inject(
            method = "handleTextClick",
            at = @At("RETURN")
    )
    private void onTextClick(Style style, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && style != null) {
            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null) {
                    client.getSoundManager().play(PositionedSoundInstance.master(
                            SoundEvent.of(Identifier.of("minecraft", "ui.hud.bubble_pop")), 2.0f, 2.0f
                    ));
                }
            }
        }
    }
}