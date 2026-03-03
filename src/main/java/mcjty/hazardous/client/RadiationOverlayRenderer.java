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
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.ModList;

import java.util.Map;
import java.util.Optional;

public class RadiationOverlayRenderer {

    private static final ResourceLocation GEIGER_UI = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui.png");
    private static final ResourceLocation GEIGER_UI_POINTER = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui_pointer.png");
    private static final ResourceLocation DOSIMETER_UI = new ResourceLocation(Hazardous.MODID, "textures/gui/dosimeter_ui.png");

    private static final int GEIGER_UI_TEX_W = 46;
    private static final int GEIGER_UI_TEX_H = 72;
    private static final int POINTER_TEX_W = 16;
    private static final int POINTER_TEX_H = 16;
    private static final float POINTER_PIVOT_X = POINTER_TEX_W / 2.0f;
    private static final float POINTER_PIVOT_Y = POINTER_TEX_H - 1.0f;

    private static final int DOSIMETER_UI_TEX_W = 40;
    private static final int DOSIMETER_UI_TEX_H = 40;
    private static final int DOSIMETER_BAR_X = 26;
    private static final int DOSIMETER_BAR_Y = 7;
    private static final int DOSIMETER_BAR_W = 5;
    private static final int DOSIMETER_BAR_H = 25;
    private static final int DOSIMETER_BAR_BG_COLOR = 0xFF111111;
    private static final int DOSIMETER_LOW_COLOR = 0xFF39855A;
    private static final int DOSIMETER_MEDIUM_COLOR = 0xFFFFBF36;
    private static final int DOSIMETER_HIGH_COLOR = 0xFFE14141;

    private static final float ZERO_RADIATION_DEG = 225.0f;
    private static final float MAX_RADIATION_DEG = 135.0f;

    private static final float DIAL_CENTER_X_RATIO = 199.0f / 400.0f;
    private static final float DIAL_CENTER_Y_RATIO = 117.0f / 576.0f;

    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            return;
        }

        if (hasActiveGeiger(player)) {
            renderGeiger(event, minecraft);
        }
        if (hasActiveDosimeter(player)) {
            renderDosimeter(event, minecraft);
        }
    }

    private static void renderGeiger(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
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
        float uiScale = Config.GEIGER_HUD_SCALE.get().floatValue();

        int scaledWidth = Mth.floor(GEIGER_UI_TEX_W * uiScale);
        int scaledHeight = Mth.floor(GEIGER_UI_TEX_H * uiScale);
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
        event.getGuiGraphics().pose().scale(uiScale, uiScale, 1.0f);

        event.getGuiGraphics().blit(GEIGER_UI, 0, 0, 0, 0, GEIGER_UI_TEX_W, GEIGER_UI_TEX_H, GEIGER_UI_TEX_W, GEIGER_UI_TEX_H);

        int dialCenterX = Mth.floor(GEIGER_UI_TEX_W * DIAL_CENTER_X_RATIO);
        int dialCenterY = Mth.floor(GEIGER_UI_TEX_H * DIAL_CENTER_Y_RATIO);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(dialCenterX, dialCenterY, 0.0f);
        event.getGuiGraphics().pose().mulPose(Axis.ZP.rotationDegrees(pointerAngle));
        event.getGuiGraphics().pose().translate(-POINTER_PIVOT_X, -POINTER_PIVOT_Y, 0.0f);
        event.getGuiGraphics().blit(GEIGER_UI_POINTER, 0, 0, 0, 0, POINTER_TEX_W, POINTER_TEX_H, POINTER_TEX_W, POINTER_TEX_H);
        event.getGuiGraphics().pose().popPose();

        event.getGuiGraphics().pose().popPose();
    }

    private static void renderDosimeter(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
        Map<ResourceLocation, Double> values = ClientDoseData.getValues();
        if (values == null || values.isEmpty()) {
            return;
        }

        double dose = resolveDoseToDisplay(values);
        float ratio = calculateDosimeterFillRatio(dose);
        float uiScale = Config.DOSIMETER_HUD_SCALE.get().floatValue();

        int scaledWidth = Mth.floor(DOSIMETER_UI_TEX_W * uiScale);
        int scaledHeight = Mth.floor(DOSIMETER_UI_TEX_H * uiScale);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int offsetX = Config.DOSIMETER_HUD_OFFSET_X.get();
        int offsetY = Config.DOSIMETER_HUD_OFFSET_Y.get();
        Config.GeigerHudAnchor anchor = Config.getDosimeterHudAnchor();

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
        event.getGuiGraphics().pose().scale(uiScale, uiScale, 1.0f);

        event.getGuiGraphics().blit(DOSIMETER_UI, 0, 0, 0, 0, DOSIMETER_UI_TEX_W, DOSIMETER_UI_TEX_H, DOSIMETER_UI_TEX_W, DOSIMETER_UI_TEX_H);

        int barColor = getDosimeterBarColor(dose);
        int fillHeight = Mth.floor(DOSIMETER_BAR_H * ratio);
        int fillTop = DOSIMETER_BAR_Y + (DOSIMETER_BAR_H - fillHeight);

        event.getGuiGraphics().fill(DOSIMETER_BAR_X, DOSIMETER_BAR_Y, DOSIMETER_BAR_X + DOSIMETER_BAR_W, DOSIMETER_BAR_Y + DOSIMETER_BAR_H, DOSIMETER_BAR_BG_COLOR);
        if (fillHeight > 0) {
            event.getGuiGraphics().fill(DOSIMETER_BAR_X, fillTop, DOSIMETER_BAR_X + DOSIMETER_BAR_W, DOSIMETER_BAR_Y + DOSIMETER_BAR_H, barColor);
        }

        event.getGuiGraphics().pose().popPose();
    }

    private static float calculatePointerAngle(double radiation) {
        double maxRadiation = Config.GEIGER_MAX_RADIATION.get();
        float ratio = (float) (maxRadiation <= 0 ? 0.0 : Mth.clamp(radiation / maxRadiation, 0.0, 1.0));
        return Mth.lerp(ratio, ZERO_RADIATION_DEG, MAX_RADIATION_DEG);
    }

    private static float calculateDosimeterFillRatio(double dose) {
        double maxDose = Config.DOSIMETER_MAX_DOSE.get();
        if (maxDose <= 0.0) {
            return 0.0f;
        }
        return (float) Mth.clamp(dose / maxDose, 0.0, 1.0);
    }

    private static int getDosimeterBarColor(double dose) {
        double medium = Config.DOSIMETER_MEDIUM_DOSE.get();
        double high = Config.DOSIMETER_HIGH_DOSE.get();
        if (high < medium) {
            high = medium;
        }

        if (dose >= high) {
            return DOSIMETER_HIGH_COLOR;
        }
        if (dose >= medium) {
            return DOSIMETER_MEDIUM_COLOR;
        }
        return DOSIMETER_LOW_COLOR;
    }

    private static double resolveDoseToDisplay(Map<ResourceLocation, Double> values) {
        Optional<ResourceLocation> resource = Config.getDosimeterDisplayResource();
        if (resource.isPresent()) {
            return values.getOrDefault(resource.get(), 0.0);
        }

        double total = 0.0;
        for (Double value : values.values()) {
            if (value != null && value > 0.0) {
                total += value;
            }
        }
        return total;
    }

    private static boolean hasActiveGeiger(Player player) {
        if (player.getInventory().getSelected().is(Registration.GEIGER_COUNTER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveGeigerCounter(player);
    }

    private static boolean hasActiveDosimeter(Player player) {
        if (player.getInventory().getSelected().is(Registration.DOSIMETER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveDosimeter(player);
    }
}
