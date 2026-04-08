package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mcjty.hazardous.network.PacketClientFx;
import mcjty.hazardous.data.PlayerDoseDispatcher;
import mcjty.hazardous.setup.Messages;
import mcjty.hazardous.setup.TimedAttributeEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** What happens when the trigger fires */
public sealed interface Action permits Action.Potion, Action.Damage, Action.Fire, Action.Attribute, Action.ClientFx {
    Codec<MobEffect> MOB_EFFECT_CODEC = ResourceLocation.CODEC.comapFlatMap(
            Action::decodeMobEffect,
            Action::encodeMobEffect
    );
    Codec<ResourceKey<DamageType>> DAMAGE_TYPE_KEY_CODEC = ResourceLocation.CODEC.xmap(
            id -> ResourceKey.create(Registries.DAMAGE_TYPE, id),
            ResourceKey::location
    );

    private static Codec<? extends Action> actionCodec(String type) {
        return switch (type) {
            case "potion" -> Action.Potion.CODEC;
            case "damage" -> Action.Damage.CODEC;
            case "fire" -> Action.Fire.CODEC;
            case "attribute" -> Action.Attribute.CODEC;
            case "client_fx" -> Action.ClientFx.CODEC;
            default -> throw new IllegalStateException("Unknown action type '" + type + "'");
        };
    }

    Codec<Action> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch(
            "type",
            Action::actionType,
            Action::actionCodec
    ));
    String actionType();

    /** Apply this action on the given player with provided dose value and scaling factor */
    void apply(Player player, double value, double factor);

    /** Apply a MobEffectInstance (vanilla potion effect) */
    record Potion(
            MobEffect effect,
            int durationTicks,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            Scaling intensityToAmplifier // optional scaling hook
    ) implements Action {
        public static final Codec<Potion> CODEC = RecordCodecBuilder.create(i -> i.group(
                MOB_EFFECT_CODEC.fieldOf("effect").forGetter(Potion::effect),
                Codec.INT.fieldOf("durationTicks").forGetter(Potion::durationTicks),
                Codec.INT.fieldOf("amplifier").forGetter(Potion::amplifier),
                Codec.BOOL.optionalFieldOf("ambient", false).forGetter(Potion::ambient),
                Codec.BOOL.optionalFieldOf("showParticles", true).forGetter(Potion::showParticles),
                Codec.BOOL.optionalFieldOf("showIcon", true).forGetter(Potion::showIcon),
                Scaling.CODEC.optionalFieldOf("intensityToAmplifier", new Scaling.Constant(1.0)).forGetter(Potion::intensityToAmplifier)
        ).apply(i, Potion::new));

        @Override
        public String actionType() {
            return "potion";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            if (factor <= 0) return;
            int duration = Math.max(1, durationTicks());
            double scaled = intensityToAmplifier().eval(value) * factor;
            int amp = Math.max(0, (int) Math.round(amplifier() * scaled));
            MobEffectInstance inst = new MobEffectInstance(effect(), duration, amp, ambient(), showParticles(), showIcon());
            player.addEffect(inst);
        }
    }

    /** Direct damage (custom damage type). */
    record Damage(
            ResourceKey<DamageType> damageType,
            double amount,
            Scaling scaleAmount // scale with intensity/dose
    ) implements Action {
        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(i -> i.group(
                DAMAGE_TYPE_KEY_CODEC.fieldOf("damageType").forGetter(Damage::damageType),
                Codec.DOUBLE.fieldOf("amount").forGetter(Damage::amount),
                Scaling.CODEC.optionalFieldOf("scaleAmount", new Scaling.Constant(1.0)).forGetter(Damage::scaleAmount)
        ).apply(i, Damage::new));

        @Override
        public String actionType() {
            return "damage";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            if (factor <= 0) return;
            double scaled = amount() * scaleAmount().eval(value) * factor;
            float amt = (float) Math.max(0.0, scaled);
            if (amt <= 0f) return;
            var damageTypeRegistry = player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            if (!damageTypeRegistry.containsKey(damageType())) {
                return;
            }
            player.hurt(new DamageSource(damageTypeRegistry.getHolderOrThrow(damageType())), amt);
        }
    }

    /** Ignite the entity for some seconds. */
    record Fire(int seconds, Scaling scaleSeconds) implements Action {
        public static final Codec<Fire> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("seconds").forGetter(Fire::seconds),
                Scaling.CODEC.optionalFieldOf("scaleSeconds", new Scaling.Constant(1.0)).forGetter(Fire::scaleSeconds)
        ).apply(i, Fire::new));

        @Override
        public String actionType() {
            return "fire";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            if (factor <= 0) return;
            double scaled = scaleSeconds().eval(value) * factor * seconds();
            int secs = Mth.clamp((int) Math.round(scaled), 0, 600);
            if (secs > 0) player.setSecondsOnFire(secs);
        }
    }

    /** Add temporary attribute modifiers (e.g. max health, movement). */
    record Attribute(
            net.minecraft.world.entity.ai.attributes.Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation,
            int durationTicks,
            Scaling scaleAmount
    ) implements Action {
        public static final Codec<Attribute> CODEC = RecordCodecBuilder.create(i -> i.group(
                TimedAttributeEffects.ATTRIBUTE_CODEC.fieldOf("attribute").forGetter(Attribute::attribute),
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(Attribute::uuid),
                Codec.STRING.fieldOf("name").forGetter(Attribute::name),
                Codec.DOUBLE.fieldOf("amount").forGetter(Attribute::amount),
                TimedAttributeEffects.ATTRIBUTE_MODIFIER_OPERATION_CODEC.fieldOf("operation").forGetter(Attribute::operation),
                Codec.INT.fieldOf("durationTicks").forGetter(Attribute::durationTicks),
                Scaling.CODEC.optionalFieldOf("scaleAmount", new Scaling.Constant(1.0)).forGetter(Attribute::scaleAmount)
        ).apply(i, Attribute::new));

        @Override
        public String actionType() {
            return  "attribute";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            if (factor <= 0.0) {
                return;
            }
            if (player.getAttribute(attribute()) == null) {
                return;
            }
            double scaled = amount() * scaleAmount().eval(value) * factor;
            if (scaled == 0.0) {
                return;
            }
            int duration = Math.max(1, durationTicks());
            long gameTime = player.level().getGameTime();
            PlayerDoseDispatcher.getPlayerDose(player).ifPresent(store -> {
                if (store.addTimedAttributeEffect(attribute(), uuid(), name(), scaled, operation(), gameTime + duration, gameTime)) {
                    TimedAttributeEffects.syncPlayer(player, store, gameTime);
                }
            });
        }
    }

    /** Client-side only visuals (screen vignette, blur, particles). */
    record ClientFx(
            ClientFxId fxId,
            Scaling intensity,
            int durationTicks
    ) implements Action {
        public static final Codec<ClientFx> CODEC = RecordCodecBuilder.create(i -> i.group(
                ClientFxId.CODEC.fieldOf("fxId").forGetter(ClientFx::fxId),
                Scaling.CODEC.optionalFieldOf("intensity", new Scaling.Constant(1.0)).forGetter(ClientFx::intensity),
                Codec.INT.optionalFieldOf("durationTicks", 40).forGetter(ClientFx::durationTicks)
        ).apply(i, ClientFx::new));

        @Override
        public String actionType() {
            return  "client_fx";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            if (factor <= 0 || !(player instanceof ServerPlayer serverPlayer)) {
                return;
            }
            double scaledIntensity = Math.max(0.0, intensity().eval(value) * factor);
            if (scaledIntensity <= 0.0) {
                return;
            }
            int ticks = Mth.clamp(durationTicks(), 1, 20 * 60);
            Messages.sendToPlayer(new PacketClientFx(fxId(), scaledIntensity, ticks), serverPlayer);
        }
    }

    private static DataResult<MobEffect> decodeMobEffect(ResourceLocation effectId) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
        if (effect == null) {
            return DataResult.error(() -> "Unknown mob effect '" + effectId + "'");
        }
        return DataResult.success(effect);
    }

    private static ResourceLocation encodeMobEffect(MobEffect effect) {
        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        if (effectId == null) {
            throw new IllegalStateException("Cannot encode unregistered mob effect " + effect);
        }
        return effectId;
    }
}
