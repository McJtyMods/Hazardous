package mcjty.hazardous.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientFxManager {

    private static final Map<String, ActiveFx> ACTIVE = new HashMap<>();

    public static void activate(String fxId, double intensity, int durationTicks) {
        if (fxId == null || fxId.isEmpty()) {
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
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<String, ActiveFx>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActiveFx> entry = it.next();
            if (entry.getValue().tickDown()) {
                it.remove();
            }
        }
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

        float shake = Math.max(getCurrentIntensity("shake"), getCurrentIntensity("shaking"));
        if (shake > 0.0f) {
            float yawJitter = (Mth.sin(time * 17.0f) * 0.9f + Mth.sin(time * 31.0f) * 0.5f) * shake * 0.8f;
            float pitchJitter = (Mth.cos(time * 19.0f) * 0.8f + Mth.sin(time * 29.0f) * 0.4f) * shake * 0.6f;
            event.setYaw(event.getYaw() + yawJitter);
            event.setPitch(event.getPitch() + pitchJitter);
        }

        float warp = Math.max(getCurrentIntensity("warp"), getCurrentIntensity("warping"));
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

        float darken = getCurrentIntensity("darken");
        if (darken > 0.0f) {
            int alpha = (int) (Mth.clamp(darken * 0.65f, 0.0f, 0.85f) * 255.0f);
            event.getGuiGraphics().fill(0, 0, width, height, (alpha << 24));
        }

        float blur = getCurrentIntensity("blur");
        if (blur > 0.0f) {
            int alpha = (int) (Mth.clamp(blur * 0.22f, 0.0f, 0.35f) * 255.0f);
            int color = (alpha << 24) | 0xBBBBBB;
            event.getGuiGraphics().fill(0, 0, width, height, color);
        }
    }

    private static float getCurrentIntensity(String fxId) {
        ActiveFx fx = ACTIVE.get(fxId);
        if (fx == null) {
            return 0.0f;
        }
        return (float) fx.currentIntensity();
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
