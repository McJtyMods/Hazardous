package mcjty.hazardous;

import mcjty.hazardous.data.CustomRegistries;
import mcjty.hazardous.data.HazardSource;
import mcjty.hazardous.data.HazardType;
import mcjty.lib.varia.Tools;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

public class EventHandlers {

    public static void onPlayerTickEvent(TickEvent.PlayerTickEvent event) {
        Level level = event.player.level();
        long gameTime = level.getGameTime();
        Registry<HazardSource> sources = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_SOURCE_REGISTRY_KEY);
        Registry<HazardType> types = Tools.getRegistryAccess(level).registryOrThrow(CustomRegistries.HAZARD_TYPE_REGISTRY_KEY);
        var visitor = new PlayerTickVisitor(event);

        for (HazardSource source : sources) {
            ResourceLocation hazardId = source.hazardType();
            HazardType type = types.get(hazardId);
            if (type == null) {
                throw new RuntimeException("Cannot find hazard with type '" + hazardId.toString() + "'!");
            }
            int intervalTicks = type.exposure().applyIntervalTicks();
            if (gameTime % intervalTicks == 0) {
                source.association().accept(type, visitor);
            }
        }
    }

    private static class PlayerTickVisitor implements HazardSource.Association.Visitor<Object> {
        private final TickEvent.PlayerTickEvent event;

        public PlayerTickVisitor(TickEvent.PlayerTickEvent event) {
            this.event = event;
        }

        @Override
        public Object level(HazardType type, HazardSource.Association.Level a) {
            return null;
        }

        @Override
        public Object entityType(HazardType type, HazardSource.Association.EntityType a) {
            return null;
        }

        @Override
        public Object locations(HazardType type, HazardSource.Association.Locations a) {
            return null;
        }

        @Override
        public Object biome(HazardType type, HazardSource.Association.Biome a) {
            return null;
        }
    }
}
