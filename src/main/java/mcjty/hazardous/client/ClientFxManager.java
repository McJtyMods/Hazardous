package mcjty.hazardous.client;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.objects.ClientFxId;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

public class ClientFxManager {

    private static final ResourceLocation BLUR_OVERLAY = new ResourceLocation(Hazardous.MODID, "textures/gui/blur.png");
    private static final ResourceLocation BLUR_RADIAL_OVERLAY = new ResourceLocation(Hazardous.MODID, "textures/gui/blur_radial.png");
    private static final int OVERLAY_TEXTURE_SIZE = 512;
    private static final int CLIENT_NAUSEA_DURATION = 5;

    private static final Map<ClientFxId, ActiveFx> ACTIVE = new EnumMap<>(ClientFxId.class);

    public static void activate(ClientFxId fxId, double intensity, int durationTicks) {
        if (fxId == null) {
            return;
        }
        double clampedIntensity = Mth.clamp(intensity, 0.0, 2.0);
        int clampedDuration = Mth.clamp(durationTicks, 1, 20 * 60);
        if (clampedIntensity <= 0.0) {
            return;
        }
        ActiveFx existing = ACTIVE.get(fxId);
        if (existing == null) {
            ACTIVE.put(fxId, new ActiveFx(clampedIntensity, clampedDuration));
        } else {
            existing.amplify(clampedIntensity, clampedDuration);
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!ACTIVE.isEmpty()) {
            Iterator<Map.Entry<ClientFxId, ActiveFx>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ClientFxId, ActiveFx> entry = it.next();
                if (entry.getValue().tickDown()) {
                    it.remove();
                }
            }
        }
        applyNauseaFx();
    }

    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }

        float time = (float) (minecraft.player.tickCount + event.getPartialTick());

        float shake = getCurrentIntensity(ClientFxId.SHAKE);
        if (shake > 0.0f) {
            float yawJitter = (Mth.sin(time * 17.0f) * 0.9f + Mth.sin(time * 31.0f) * 0.5f) * shake * 0.8f;
            float pitchJitter = (Mth.cos(time * 19.0f) * 0.8f + Mth.sin(time * 29.0f) * 0.4f) * shake * 0.6f;
            event.setYaw(event.getYaw() + yawJitter);
            event.setPitch(event.getPitch() + pitchJitter);
        }

        float warp = getCurrentIntensity(ClientFxId.WARP);
        if (warp > 0.0f) {
            float roll = Mth.sin(time * 2.2f) * warp * 4.5f;
            float yaw = Mth.sin(time * 1.1f) * warp * 1.0f;
            float pitch = Mth.cos(time * 1.6f) * warp * 0.8f;
            event.setRoll(event.getRoll() + roll);
            event.setYaw(event.getYaw() + yaw);
            event.setPitch(event.getPitch() + pitch);
        }
    }

    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || ACTIVE.isEmpty()) {
            return;
        }
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        var graphics = event.getGuiGraphics();

        float darken = getCurrentIntensity(ClientFxId.DARKEN);
        if (darken > 0.0f) {
            int alpha = (int) (Mth.clamp(darken * 0.65f, 0.0f, 0.85f) * 255.0f);
            graphics.fill(0, 0, width, height, (alpha << 24));
        }

        float lighten = getCurrentIntensity(ClientFxId.LIGHTEN);
        if (lighten > 0.0f) {
            int alpha = (int) (Mth.clamp(lighten * 0.22f, 0.0f, 0.35f) * 255.0f);
            int color = (alpha << 24) | 0xBBBBBB;
            graphics.fill(0, 0, width, height, color);
        }

        renderOverlayTexture(graphics, width, height, BLUR_OVERLAY, getCurrentIntensity(ClientFxId.BLUR), 0.85f);
        renderOverlayTexture(graphics, width, height, BLUR_RADIAL_OVERLAY, getCurrentIntensity(ClientFxId.BLUR_RADIAL), 0.9f);
    }

    private static float getCurrentIntensity(ClientFxId fxId) {
        ActiveFx fx = ACTIVE.get(fxId);
        if (fx == null) {
            return 0.0f;
        }
        return (float) fx.currentIntensity();
    }

    private static void renderOverlayTexture(net.minecraft.client.gui.GuiGraphics graphics, int width, int height, ResourceLocation texture, float intensity, float maxAlpha) {
        if (intensity <= 0.0f || maxAlpha <= 0.0f) {
            return;
        }
        float alpha = Mth.clamp(intensity * maxAlpha, 0.0f, maxAlpha);
        graphics.setColor(1.0f, 1.0f, 1.0f, alpha);
        graphics.blit(texture, 0, 0, 0.0f, 0.0f, width, height, OVERLAY_TEXTURE_SIZE, OVERLAY_TEXTURE_SIZE);
        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void applyNauseaFx() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer == null || minecraft.isPaused()) {
            return;
        }
        float nausea = getCurrentIntensity(ClientFxId.NAUSEA);
        MobEffectInstance confusion = localPlayer.getEffect(MobEffects.CONFUSION);
        boolean injectedConfusion = isInjectedClientConfusion(confusion);

        if (nausea <= 0.0f) {
            if (injectedConfusion) {
                localPlayer.spinningEffectIntensity = 0.0f;
                localPlayer.oSpinningEffectIntensity = 0.0f;
            }
            return;
        }

        if (confusion == null || injectedConfusion) {
            localPlayer.addEffect(new MobEffectInstance(MobEffects.CONFUSION, CLIENT_NAUSEA_DURATION, 0, false, false, false));
        }

        float appliedNausea = Math.max(nausea, Math.max(localPlayer.spinningEffectIntensity, localPlayer.oSpinningEffectIntensity));
        localPlayer.spinningEffectIntensity = appliedNausea;
        localPlayer.oSpinningEffectIntensity = appliedNausea;
    }

    private static boolean isInjectedClientConfusion(@Nullable MobEffectInstance effect) {
        return effect != null
                && effect.endsWithin(CLIENT_NAUSEA_DURATION)
                && !effect.isAmbient()
                && !effect.isVisible()
                && !effect.showIcon();
    }

    private static class ActiveFx {
        private double peakIntensity;
        private int remainingTicks;
        private int maxTicks;

        private ActiveFx(double peakIntensity, int durationTicks) {
            this.peakIntensity = peakIntensity;
            this.remainingTicks = durationTicks;
            this.maxTicks = durationTicks;
        }

        private void amplify(double intensity, int durationTicks) {
            peakIntensity = Math.max(peakIntensity, intensity);
            remainingTicks = Math.max(remainingTicks, durationTicks);
            maxTicks = Math.max(maxTicks, durationTicks);
        }

        private boolean tickDown() {
            remainingTicks--;
            return remainingTicks <= 0;
        }

        private double currentIntensity() {
            if (remainingTicks <= 0 || maxTicks <= 0) {
                return 0.0;
            }
            double fade = remainingTicks / (double) maxTicks;
            return peakIntensity * Mth.clamp(fade, 0.0, 1.0);
        }
    }
}
