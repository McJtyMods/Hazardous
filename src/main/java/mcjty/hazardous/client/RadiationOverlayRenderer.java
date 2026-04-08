package mcjty.hazardous.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mcjty.hazardous.Hazardous;
import mcjty.hazardous.compat.CuriosCompat;
import mcjty.hazardous.data.PlayerDoseData;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class RadiationOverlayRenderer {

    private static final ResourceLocation GEIGER_UI = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui.png");
    private static final ResourceLocation GEIGER_UI_POINTER = new ResourceLocation(Hazardous.MODID, "textures/gui/geiger_ui_pointer.png");
    private static final ResourceLocation DOSIMETER_UI = new ResourceLocation(Hazardous.MODID, "textures/gui/dosimeter_ui.png");
    private static final ResourceLocation DOSIMETER_UI_ICON = new ResourceLocation(Hazardous.MODID, "textures/gui/dosimeter_ui_icon.png");

    private static final int GEIGER_UI_TEX_W = 46;
    private static final int GEIGER_UI_TEX_H = 72;
    private static final int POINTER_TEX_W = 16;
    private static final int POINTER_TEX_H = 16;
    private static final float POINTER_PIVOT_X = POINTER_TEX_W / 2.0f;
    private static final float POINTER_PIVOT_Y = POINTER_TEX_H - 1.0f;

    private static final int DOSIMETER_UI_TEX_W = 40;
    private static final int DOSIMETER_UI_TEX_H = 40;
    private static final int DOSIMETER_ICON_X = 11;
    private static final int DOSIMETER_ICON_Y = 7;
    private static final int DOSIMETER_ICON_TEX_W = 9;
    private static final int DOSIMETER_ICON_TEX_H = 9;
    private static final int DOSIMETER_BAR_X = 26;
    private static final int DOSIMETER_BAR_Y = 7;
    private static final int DOSIMETER_BAR_W = 5;
    private static final int DOSIMETER_BAR_H = 25;
    private static final int DOSIMETER_TEXT_X = 11;
    private static final int DOSIMETER_TEXT_Y = 19;
    private static final float DOSIMETER_TEXT_SCALE = 0.5f;
    private static final int DOSIMETER_TEXT_COLOR = 0xFFFFFFFF;
    private static final int DOSIMETER_BAR_BG_COLOR = 0xFF111111;
    private static final int DOSIMETER_LOW_COLOR = 0xFF39855A;
    private static final int DOSIMETER_MEDIUM_COLOR = 0xFFFFBF36;
    private static final int DOSIMETER_HIGH_COLOR = 0xFFE14141;
    private static final float ZERO_RADIATION_DEG = 225.0f; // South-west
    // Use equivalent south-east angle +360 so lerp sweeps over the top arc instead of the bottom.
    private static final float MAX_RADIATION_DEG = 495.0f; // South-east
    private static final float POINTER_TWITCH_MIN_RATIO = 0.27f;
    private static final HudShakeOffset NO_SHAKE = new HudShakeOffset(0.0f, 0.0f);
    private static final int RESISTANCE_PILLS_BG_COLOR = 0xB010141A;
    private static final int RESISTANCE_PILLS_BORDER_COLOR = 0xFF6ED06C;
    private static final int RESISTANCE_PILLS_TITLE_COLOR = 0xFFE8FFF0;
    private static final int RESISTANCE_PILLS_TEXT_COLOR = 0xFFD6EEDD;
    private static final int RESISTANCE_PILLS_PADDING = 4;
    private static final int RESISTANCE_PILLS_LINE_GAP = 2;
    private static final float RESISTANCE_PILLS_TITLE_SCALE = 0.8f;
    private static final float RESISTANCE_PILLS_TEXT_SCALE = 0.7f;

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
        if (isDosimeterHudVisible(player)) {
            renderDosimeter(event, minecraft);
        }
        if (Config.RESISTANCE_PILLS_HUD_ENABLED.get()) {
            renderResistancePills(event, minecraft);
        }
    }

    private static void renderGeiger(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
        Optional<ResourceLocation> displayResource = Config.getGeigerDisplayHazardType();
        if (displayResource.isPresent()) {
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return;
            }
            OptionalDouble radiationValue = getDisplayedRadiation(player);
            if (radiationValue.isEmpty()) {
                return;
            }
            double radiation = radiationValue.getAsDouble();
            float pointerAngle = calculatePointerAngle(radiation) + calculatePointerTwitch(minecraft, radiation);
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

            GuiGraphics graphics = event.getGuiGraphics();
            PoseStack pose = graphics.pose();

            pose.pushPose();
            pose.translate(x, y, 0.0f);
            pose.scale(uiScale, uiScale, 1.0f);

            graphics.blit(GEIGER_UI, 0, 0, 0, 0, GEIGER_UI_TEX_W, GEIGER_UI_TEX_H, GEIGER_UI_TEX_W, GEIGER_UI_TEX_H);

            int dialCenterX = Mth.floor(GEIGER_UI_TEX_W * DIAL_CENTER_X_RATIO);
            int dialCenterY = Mth.floor(GEIGER_UI_TEX_H * DIAL_CENTER_Y_RATIO);

            pose.pushPose();
            pose.translate(dialCenterX, dialCenterY, 0.0f);
            pose.mulPose(Axis.ZP.rotationDegrees(pointerAngle));
            pose.translate(-POINTER_PIVOT_X, -POINTER_PIVOT_Y, 0.0f);
            graphics.blit(GEIGER_UI_POINTER, 0, 0, 0, 0, POINTER_TEX_W, POINTER_TEX_H, POINTER_TEX_W, POINTER_TEX_H);
            pose.popPose();

            pose.popPose();
        }
    }

    private static void renderDosimeter(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
        Map<ResourceLocation, Double> values = ClientData.getDoseValues();
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
        renderDosimeterRadiationIcon(event, minecraft, dose);

        int barColor = getDosimeterBarColor(dose);
        int fillHeight = Mth.floor(DOSIMETER_BAR_H * ratio);
        int fillTop = DOSIMETER_BAR_Y + (DOSIMETER_BAR_H - fillHeight);

        event.getGuiGraphics().fill(DOSIMETER_BAR_X, DOSIMETER_BAR_Y, DOSIMETER_BAR_X + DOSIMETER_BAR_W, DOSIMETER_BAR_Y + DOSIMETER_BAR_H, DOSIMETER_BAR_BG_COLOR);
        if (fillHeight > 0) {
            event.getGuiGraphics().fill(DOSIMETER_BAR_X, fillTop, DOSIMETER_BAR_X + DOSIMETER_BAR_W, DOSIMETER_BAR_Y + DOSIMETER_BAR_H, barColor);
        }

        renderDosimeterDoseText(event, minecraft, dose);

        event.getGuiGraphics().pose().popPose();
    }

    private static void renderResistancePills(RenderGuiOverlayEvent.Post event, Minecraft minecraft) {
        Map<ResourceLocation, PlayerDoseData.ResistancePillStatus> values = ClientData.getResistancePillStatuses();
        if (values == null || values.isEmpty()) {
            return;
        }
        if (minecraft.player == null) {
            return;
        }

        long gameTime = minecraft.player.level().getGameTime();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<ResourceLocation, PlayerDoseData.ResistancePillStatus> entry : values.entrySet()) {
            long remainingTicks = Math.max(0L, entry.getValue().expiresAt() - gameTime);
            if (remainingTicks <= 0L || entry.getValue().stacks() <= 0 || entry.getValue().amount() <= 0.0) {
                continue;
            }
            lines.add(formatResistancePillLine(entry.getKey(), entry.getValue(), remainingTicks));
        }
        if (lines.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        float uiScale = Config.RESISTANCE_PILLS_HUD_SCALE.get().floatValue();
        String title = Component.translatable("message.hazardous.resistance_pills.hud_title").getString();
        int titleHeight = scaledTextHeight(RESISTANCE_PILLS_TITLE_SCALE);
        int lineHeight = scaledTextHeight(RESISTANCE_PILLS_TEXT_SCALE);
        int innerWidth = scaledTextWidth(minecraft, title, RESISTANCE_PILLS_TITLE_SCALE);
        for (String line : lines) {
            innerWidth = Math.max(innerWidth, scaledTextWidth(minecraft, line, RESISTANCE_PILLS_TEXT_SCALE));
        }
        int innerHeight = titleHeight + RESISTANCE_PILLS_LINE_GAP + (lines.size() * lineHeight) + ((lines.size() - 1) * RESISTANCE_PILLS_LINE_GAP);
        int boxWidth = innerWidth + (RESISTANCE_PILLS_PADDING * 2);
        int boxHeight = innerHeight + (RESISTANCE_PILLS_PADDING * 2);

        int scaledWidth = Mth.floor(boxWidth * uiScale);
        int scaledHeight = Mth.floor(boxHeight * uiScale);
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int offsetX = Config.RESISTANCE_PILLS_HUD_OFFSET_X.get();
        int offsetY = Config.RESISTANCE_PILLS_HUD_OFFSET_Y.get();
        Config.GeigerHudAnchor anchor = Config.getResistancePillsHudAnchor();
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

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(uiScale, uiScale, 1.0f);

        graphics.fill(0, 0, boxWidth, boxHeight, RESISTANCE_PILLS_BG_COLOR);
        graphics.fill(0, 0, boxWidth, 1, RESISTANCE_PILLS_BORDER_COLOR);
        graphics.fill(0, boxHeight - 1, boxWidth, boxHeight, RESISTANCE_PILLS_BORDER_COLOR);
        graphics.fill(0, 0, 1, boxHeight, RESISTANCE_PILLS_BORDER_COLOR);
        graphics.fill(boxWidth - 1, 0, boxWidth, boxHeight, RESISTANCE_PILLS_BORDER_COLOR);

        int drawY = RESISTANCE_PILLS_PADDING;
        drawScaledText(graphics, minecraft, title, RESISTANCE_PILLS_PADDING, drawY, RESISTANCE_PILLS_TITLE_SCALE, RESISTANCE_PILLS_TITLE_COLOR);
        drawY += titleHeight + RESISTANCE_PILLS_LINE_GAP;
        for (String line : lines) {
            drawScaledText(graphics, minecraft, line, RESISTANCE_PILLS_PADDING, drawY, RESISTANCE_PILLS_TEXT_SCALE, RESISTANCE_PILLS_TEXT_COLOR);
            drawY += lineHeight + RESISTANCE_PILLS_LINE_GAP;
        }

        graphics.pose().popPose();
    }

    private static void renderDosimeterRadiationIcon(RenderGuiOverlayEvent.Post event, Minecraft minecraft, double dose) {
        HudShakeOffset shake = calculateDosimeterIconShake(minecraft, dose);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(DOSIMETER_ICON_X + shake.x(), DOSIMETER_ICON_Y + shake.y(), 0.0f);
        event.getGuiGraphics().blit(DOSIMETER_UI_ICON, 0, 0, 0, 0, DOSIMETER_ICON_TEX_W, DOSIMETER_ICON_TEX_H, DOSIMETER_ICON_TEX_W, DOSIMETER_ICON_TEX_H);
        event.getGuiGraphics().pose().popPose();
    }

    private static void renderDosimeterDoseText(RenderGuiOverlayEvent.Post event, Minecraft minecraft, double dose) {
        String text = formatDosimeterDose(dose);
        float inverseScale = 1.0f / DOSIMETER_TEXT_SCALE;
        int drawX = Mth.floor(DOSIMETER_TEXT_X * inverseScale);
        int drawY = Mth.floor(DOSIMETER_TEXT_Y * inverseScale);

        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().scale(DOSIMETER_TEXT_SCALE, DOSIMETER_TEXT_SCALE, 1.0f);
        event.getGuiGraphics().drawString(minecraft.font, text, drawX, drawY, DOSIMETER_TEXT_COLOR, false);
        event.getGuiGraphics().pose().popPose();
    }

    private static String formatDosimeterDose(double dose) {
        if (dose >= 100.0) {
            return String.format(Locale.ROOT, "%.0f", dose);
        }
        if (dose >= 10.0) {
            return String.format(Locale.ROOT, "%.1f", dose);
        }
        return String.format(Locale.ROOT, "%.2f", dose);
    }

    private static String formatResistancePillLine(ResourceLocation attributeId, PlayerDoseData.ResistancePillStatus status, long remainingTicks) {
        return String.format(
                Locale.ROOT,
                "%s +%.2f x%d  %s",
                getAttributeName(attributeId),
                status.amount(),
                status.stacks(),
                formatDuration(remainingTicks)
        );
    }

    private static String getAttributeName(ResourceLocation attributeId) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeId);
        if (attribute == null) {
            return attributeId.toString();
        }
        return Component.translatable(attribute.getDescriptionId()).getString();
    }

    private static String formatDuration(long ticks) {
        int totalSeconds = Mth.ceil(ticks / 20.0);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
        }
        return seconds + "s";
    }

    private static int scaledTextWidth(Minecraft minecraft, String text, float scale) {
        return Mth.ceil(minecraft.font.width(text) * scale);
    }

    private static int scaledTextHeight(float scale) {
        return Mth.ceil(9 * scale);
    }

    private static void drawScaledText(GuiGraphics graphics, Minecraft minecraft, String text, int x, int y, float scale, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0f);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(minecraft.font, text, 0, 0, color, false);
        graphics.pose().popPose();
    }

    private static float calculatePointerAngle(double radiation) {
        double maxRadiation = Config.GEIGER_MAX_RADIATION.get();
        if (maxRadiation <= 0.0) {
            return ZERO_RADIATION_DEG;
        }

        double medium = Mth.clamp(Config.GEIGER_MEDIUM_THRESSHOLD.get(), 0.0, maxRadiation);
        double high = Mth.clamp(Config.GEIGER_HIGH_TRESSHOLD.get(), medium, maxRadiation);

        float ratio;
        if (radiation <= medium || medium <= 0.0) {
            double denom = medium <= 0.0 ? 1.0 : medium;
            float local = (float) Mth.clamp(radiation / denom, 0.0, 1.0);
            ratio = local / 3.0f;
        } else if (radiation <= high || high <= medium) {
            double denom = high <= medium ? 1.0 : (high - medium);
            float local = (float) Mth.clamp((radiation - medium) / denom, 0.0, 1.0);
            ratio = (1.0f / 3.0f) + (local / 3.0f);
        } else {
            double denom = maxRadiation <= high ? 1.0 : (maxRadiation - high);
            float local = (float) Mth.clamp((radiation - high) / denom, 0.0, 1.0);
            ratio = (2.0f / 3.0f) + (local / 3.0f);
        }
        return Mth.lerp(ratio, ZERO_RADIATION_DEG, MAX_RADIATION_DEG);
    }

    private static float calculatePointerTwitch(Minecraft minecraft, double radiation) {
        if (radiation <= 0.0 || minecraft.player == null) {
            return 0.0f;
        }

        double maxRadiation = Math.max(Config.GEIGER_MAX_RADIATION.get(), 0.0001);
        float intensity = (float) Mth.clamp(radiation / maxRadiation, 0.0, 1.0);
        float maxAmplitude = (float) Math.max(0.0, Config.GEIGER_NEEDLE_JITTER_ANGLE.get());
        if (maxAmplitude <= 0.0f) {
            return 0.0f;
        }

        float minAmplitude = maxAmplitude * POINTER_TWITCH_MIN_RATIO;
        float amplitude = Mth.lerp((float) Math.sqrt(intensity), minAmplitude, maxAmplitude);
        float speed = (float) Math.max(0.1, Config.GEIGER_NEEDLE_JITTER_SPEED.get());
        float time = (minecraft.player.tickCount + minecraft.getFrameTime()) * speed;

        // Blend two waves so the motion reads as a subtle Geiger flutter instead of a smooth pendulum.
        float waveA = Mth.sin(time * 0.95f);
        float waveB = Mth.sin(time * 3.1f + 0.8f) * 0.45f;
        return (waveA + waveB) * amplitude;
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

    private static HudShakeOffset calculateDosimeterIconShake(Minecraft minecraft, double dose) {
        if (minecraft.player == null) {
            return NO_SHAKE;
        }

        float amplitude = calculateDosimeterIconShakeAmplitude(dose);
        if (amplitude <= 0.0f) {
            return NO_SHAKE;
        }

        float speed = (float) Math.max(0.1, Config.DOSIMETER_ICON_SHAKE_SPEED.get());
        float time = (minecraft.player.tickCount + minecraft.getFrameTime()) * speed;

        // Blend a couple of frequencies so the icon vibrates gently instead of orbiting in a perfect loop.
        float waveX = (Mth.sin(time * 0.93f) * 0.7f) + (Mth.sin(time * 2.41f + 0.6f) * 0.3f);
        float waveY = (Mth.cos(time * 1.11f + 0.4f) * 0.65f) + (Mth.sin(time * 3.17f + 1.2f) * 0.35f);
        return new HudShakeOffset(waveX * amplitude, waveY * amplitude * 0.85f);
    }

    private static float calculateDosimeterIconShakeAmplitude(double dose) {
        double medium = Math.max(0.0, Config.DOSIMETER_MEDIUM_DOSE.get());
        if (dose < medium) {
            return 0.0f;
        }

        float mediumAmplitude = (float) Math.max(0.0, Config.DOSIMETER_ICON_SHAKE_MEDIUM_DISTANCE.get());
        float maxAmplitude = (float) Math.max(mediumAmplitude, Config.DOSIMETER_ICON_SHAKE_MAX_DISTANCE.get());
        if (maxAmplitude <= 0.0f) {
            return 0.0f;
        }

        double maxDose = Math.max(Config.DOSIMETER_MAX_DOSE.get(), medium);
        if (maxDose <= medium) {
            return maxAmplitude;
        }

        float progress = (float) Mth.clamp((dose - medium) / (maxDose - medium), 0.0, 1.0);
        return Mth.lerp(progress, mediumAmplitude, maxAmplitude);
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

    public static OptionalDouble getDisplayedRadiation(Player player) {
        if (!isGeigerHudVisible(player)) {
            return OptionalDouble.empty();
        }
        Optional<ResourceLocation> displayResource = Config.getGeigerDisplayHazardType();
        if (displayResource.isEmpty()) {
            return OptionalDouble.empty();
        }
        Map<ResourceLocation, Double> values = ClientData.getRadiationValues();
        if (values == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(values.getOrDefault(displayResource.get(), 0.0));
    }

    public static OptionalDouble getDisplayedDose(Player player) {
        if (!isDosimeterHudVisible(player)) {
            return OptionalDouble.empty();
        }
        Map<ResourceLocation, Double> values = ClientData.getDoseValues();
        if (values == null || values.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(resolveDoseToDisplay(values));
    }

    public static boolean isGeigerHudVisible(Player player) {
        if (player.getInventory().getSelected().is(Registration.GEIGER_COUNTER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveGeigerCounter(player);
    }

    public static boolean isDosimeterHudVisible(Player player) {
        if (player.getInventory().getSelected().is(Registration.DOSIMETER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveDosimeter(player);
    }

    private static boolean hasActiveGeiger(Player player) {
        if (player.getInventory().getSelected().is(Registration.GEIGER_COUNTER.get())) {
            return true;
        }
        return ModList.get().isLoaded("curios") && CuriosCompat.hasActiveGeigerCounter(player);
    }

    private record HudShakeOffset(float x, float y) {
    }
}
