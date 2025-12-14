package mcjty.hazardous.setup;

import mcjty.hazardous.data.CustomRegistries;
import net.minecraftforge.eventbus.api.IEventBus;

public class Registration {

    public static void register(IEventBus bus) {
        CustomRegistries.init(bus);
    }
}
