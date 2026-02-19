package mcjty.hazardous.client;

import com.mojang.math.Axis;
import mcjty.hazardous.Hazardous;
import mcjty.hazardous.compat.CuriosCompat;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.Map;
import java.util.Optional;

public class RadiationOverlayRenderer {

    private static final ResourceLocation GEIGER_UI = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui.png");
    private static final ResourceLocation GEIGER_UI_POINTER = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui_pointer.png");

    private static final int UI_TEX_W = 46;
    private static final int UI_TEX_H = 72;
    private static final int POINTER_TEX_W = 16;
    private static final int POINTER_TEX_H = 16;
    private static final float POINTER_PIVOT_X = POINTER_TEX_W / 2.0f;
    private static final float POINTER_PIVOT_Y = POINTER_TEX_H - 1.0f;

    private static final float UI_SCALE = 1.0f;
    private static final float ZERO_RADIATION_DEG = 225.0f; // South-west
    private static final float MAX_RADIATION_DEG = 135.0f; // South-east

    // Dial center ratios based on the original art layout so this keeps working after resizes.
    private static final float DIAL_CENTER_X_RATIO = 199.0f / 400.0f;
    private static final float DIAL_CENTER_Y_RATIO = 117.0f / 576.0f;

    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || !hasActiveGeiger(player)) {
            return;
        }

        Optional<ResourceLocation> displayResource = Config.getGeigerDisplayResource();
        if (displayResource.isEmpty()) {
            return;
        }

        Map<ResourceLocation, Double> values = ClientRadiationData.getValues();
        if (values == null) {
            return;
        }

        double radiation = values.getOrDefault(displayResource.get(), 0.0);
        float pointerAngle = calculatePointerAngle(radiation);

        int scaledWidth = Mth.floor(UI_TEX_W * UI_SCALE);
        int scaledHeight = Mth.floor(UI_TEX_H * UI_SCALE);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int offsetX = Config.GEIGER_HUD_OFFSET_X.get();
        int offsetY = Config.GEIGER_HUD_OFFSET_Y.get();
        Config.GeigerHudAnchor anchor = Config.getGeigerHudAnchor();

        int x = switch (anchor) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_CENTER, BOTTOM_CENTER -> (screenWidth - scaledWidth) / 2 + offsetX;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> screenWidth - scaledWidth - offsetX;
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> offsetY;
            case CENTER_LEFT, CENTER_RIGHT -> (screenHeight - scaledHeight) / 2 + offsetY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight - scaledHeight - offsetY;
        };

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(x, y, 0.0f);
        event.getGuiGraphics().pose().scale(UI_SCALE, UI_SCALE, 1.0f);

        event.getGuiGraphics().blit(GEIGER_UI, 0, 0, 0, 0, UI_TEX_W, UI_TEX_H, UI_TEX_W, UI_TEX_H);

        int dialCenterX = Mth.floor(UI_TEX_W * DIAL_CENTER_X_RATIO);
        int dialCenterY = Mth.floor(UI_TEX_H * DIAL_CENTER_Y_RATIO);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(dialCenterX, dialCenterY, 0.0f);
        event.getGuiGraphics().pose().mulPose(Axis.ZP.rotationDegrees(pointerAngle));
        event.getGuiGraphics().pose().translate(-POINTER_PIVOT_X, -POINTER_PIVOT_Y, 0.0f);
        event.getGuiGraphics().blit(GEIGER_UI_POINTER, 0, 0, 0, 0, POINTER_TEX_W, POINTER_TEX_H, POINTER_TEX_W, POINTER_TEX_H);
        event.getGuiGraphics().pose().popPose();

        event.getGuiGraphics().pose().popPose();
    }

    private static float calculatePointerAngle(double radiation) {
        double maxRadiation = Config.GEIGER_MAX_RADIATION.get();
        float ratio = (float) (maxRadiation <= 0 ? 0.0 : Mth.clamp(radiation / maxRadiation, 0.0, 1.0));
        return Mth.lerp(ratio, ZERO_RADIATION_DEG, MAX_RADIATION_DEG);
    }

    private static boolean hasActiveGeiger(Player player) {
        if (player.getInventory().getSelected().is(Registration.GEIGER_COUNTER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveGeigerCounter(player);
    }
}
