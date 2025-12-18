package mcjty.hazardous.setup;

import mcjty.hazardous.compat.LostCityCompat;
import mcjty.lib.setup.DefaultModSetup;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ModSetup extends DefaultModSetup {

    @Override
    public void init(FMLCommonSetupEvent e) {
        super.init(e);
        Messages.registerMessages();
    }

    @Override
    protected void setupModCompat() {
        LostCityCompat.register();
    }
}
