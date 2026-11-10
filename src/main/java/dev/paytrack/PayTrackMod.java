package dev.paytrack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class PayTrackMod implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        PaymentTracker.load();

        // Press P to open the PayTrack menu (change in Controls if it clashes).
        // Since 1.21.9 key categories are objects, not strings.
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("paytrack", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.paytrack.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                category));

        // Chat / game messages. overlay == true is the action bar, which we ignore to avoid double counting.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handleChat(message.getString());
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new PayTrackScreen());
            }
            EffectRenderer.tick();
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> EffectRenderer.render(drawContext));
    }

    private static void handleChat(String text) {
        Optional<PaymentParser.Payment> parsed = PaymentParser.parse(text);
        if (parsed.isEmpty()) {
            return;
        }
        PaymentParser.Payment payment = parsed.get();
        PaymentTracker.record(payment.player(), payment.amount());

        EffectTier tier = EffectTier.forAmount(payment.amount());
        if (tier != null) {
            EffectRenderer.play(tier, payment.player(), payment.amount());
        }
    }
}
