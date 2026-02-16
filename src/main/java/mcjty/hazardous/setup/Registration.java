package mcjty.hazardous.setup;

import mcjty.hazardous.Hazardous;
import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.items.GeigerCounterItem;
import mcjty.lib.setup.DeferredItem;
import mcjty.lib.setup.DeferredItems;
import net.minecraftforge.eventbus.api.IEventBus;

public class Registration {

    public static final DeferredItems ITEMS = DeferredItems.create(Hazardous.MODID);

    public static final DeferredItem<GeigerCounterItem> GEIGER_COUNTER = ITEMS.register("geiger_counter",
            () -> new GeigerCounterItem());

    public static void register(IEventBus bus) {
        CustomRegistries.init(bus);
        ITEMS.register(bus);
    }
}
