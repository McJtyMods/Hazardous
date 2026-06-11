package mcjty.hazardous.effects;

import mcjty.hazardous.setup.Config;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.function.Consumer;

public class HazardImmunityEffect extends MobEffect {

    public HazardImmunityEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x4CC9F0);
    }

    @Override
    public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {
        consumer.accept(new IClientMobEffectExtensions() {
            @Override
            public boolean isVisibleInInventory(MobEffectInstance instance) {
                return !Config.HIDE_HAZARD_IMMUNITY_EFFECT.get();
            }

            @Override
            public boolean isVisibleInGui(MobEffectInstance instance) {
                return !Config.HIDE_HAZARD_IMMUNITY_EFFECT.get();
            }
        });
    }
}
