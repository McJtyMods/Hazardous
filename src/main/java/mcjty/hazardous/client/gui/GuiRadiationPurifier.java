package mcjty.hazardous.client.gui;

import mcjty.hazardous.blocks.RadiationPurifierControllerBlock;
import mcjty.hazardous.blocks.RadiationPurifierControllerBlockEntity;
import mcjty.hazardous.setup.Config;
import mcjty.hazardous.setup.Registration;
import mcjty.lib.container.GenericContainer;
import mcjty.lib.gui.GenericGuiContainer;
import mcjty.lib.gui.Window;
import mcjty.lib.gui.layout.VerticalLayout;
import mcjty.lib.gui.widgets.EnergyBar;
import mcjty.lib.gui.widgets.Label;
import mcjty.lib.gui.widgets.Panel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

import static mcjty.lib.gui.widgets.Widgets.label;

public class GuiRadiationPurifier extends GenericGuiContainer<RadiationPurifierControllerBlockEntity, GenericContainer> {

    private static final int WIDTH = 180;
    private static final int HEIGHT = 100;

    private EnergyBar energyBar;
    private Label status;

    public GuiRadiationPurifier(RadiationPurifierControllerBlockEntity tileEntity, GenericContainer container, Inventory inventory) {
        super(tileEntity, container, inventory, Registration.RADIATION_PURIFIER_CONTROLLER.get().getManualEntry());
        imageWidth = WIDTH;
        imageHeight = HEIGHT;
    }

    public static void register() {
        register(Registration.RADIATION_PURIFIER_MENU.get(), GuiRadiationPurifier::new);
    }

    @Override
    public void init() {
        super.init();

        energyBar = new EnergyBar().filledRectThickness(1).horizontal().desiredHeight(14).desiredWidth(168)
                .showText(true).showRfPerTick(true).rfPerTick(Config.RADIATION_PURIFIER_ENERGY_PER_TICK.get());
        status = label("").desiredHeight(14);
        Panel root = new Panel().filledRectThickness(2)
                .layout(new VerticalLayout().setHorizontalMargin(5).setVerticalMargin(5).setSpacing(3))
                .children(
                        label(Component.translatable("gui.hazardous.radiation_purifier").getString()).desiredHeight(14),
                        energyBar,
                        status,
                        label(Component.translatable("gui.hazardous.radiation_purifier.radius",
                                Config.RADIATION_PURIFIER_RADIUS.get()).getString()).desiredHeight(14),
                        label(Component.translatable("gui.hazardous.radiation_purifier.scan_interval",
                                Config.RADIATION_PURIFIER_PLAYER_SCAN_INTERVAL.get()).getString()).desiredHeight(14));
        root.bounds(leftPos, topPos, WIDTH, HEIGHT);
        window = new Window(this, root);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        boolean formed = tileEntity.getBlockState().getValue(RadiationPurifierControllerBlock.FORMED);
        boolean active = tileEntity.getBlockState().getValue(RadiationPurifierControllerBlock.ACTIVE);
        String key = !formed ? "gui.hazardous.radiation_purifier.unformed"
                : active ? "gui.hazardous.radiation_purifier.active" : "gui.hazardous.radiation_purifier.no_power";
        status.text(Component.translatable(key).getString());
        updateEnergyBar(energyBar);
        drawWindow(graphics);
    }
}
