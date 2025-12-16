package mcjty.hazardous.setup;

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
        // No external compat yet
    }
}
