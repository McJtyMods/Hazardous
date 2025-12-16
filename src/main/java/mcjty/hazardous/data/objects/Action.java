package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/** What happens when the trigger fires. */
public sealed interface Action permits Action.Potion, Action.Damage, Action.Fire, Action.Attribute, Action.ClientFx, Action.Command {
    private static Codec<? extends Action> actionCodec(String type) {
        return switch (type) {
            case "potion" -> Action.Potion.CODEC;
            case "damage" -> Action.Damage.CODEC;
            case "fire" -> Action.Fire.CODEC;
            case "attribute" -> Action.Attribute.CODEC;
            case "client_fx" -> Action.ClientFx.CODEC;
            case "command" -> Action.Command.CODEC;
            default -> throw new IllegalStateException("Unknown action type '" + type + "'");
        };
    }

    Codec<Action> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch(
            "type",
            Action::actionType,
            Action::actionCodec
    ));
    String actionType();

    /** Apply this action on the given player with provided dose value and scaling factor. */
    void apply(Player player, double value, double factor);

    /** Apply a MobEffectInstance (vanilla potion effect). */
    record Potion(
            ResourceLocation effect, // mob effect id
            int durationTicks,
            int amplifier,
            boolean ambient,
            boolean showParticles,
            boolean showIcon,
            Scaling intensityToAmplifier // optional scaling hook
    ) implements Action {
        public static final Codec<Potion> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("effect").forGetter(Potion::effect),
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
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effect());
            if (effect != null) {
                int duration = Math.max(1, durationTicks());
                double scaled = intensityToAmplifier().eval(value) * factor;
                int amp = Math.max(0, (int) Math.round(amplifier() * scaled));
                MobEffectInstance inst = new MobEffectInstance(effect, duration, amp, ambient(), showParticles(), showIcon());
                player.addEffect(inst);
            }
        }
    }

    /** Direct damage (custom damage type). */
    record Damage(
            ResourceLocation damageType, // 1.20+ DamageType registry id
            double amount,
            Scaling scaleAmount // scale with intensity/dose
    ) implements Action {
        public static final Codec<Damage> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("damageType").forGetter(Damage::damageType),
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
            String path = damageType().getPath();
            if ("magic".equals(path)) {
                player.hurt(player.damageSources().magic(), amt);
            } else if ("on_fire".equals(path)) {
                player.hurt(player.damageSources().onFire(), amt);
            } else if ("in_fire".equals(path)) {
                player.hurt(player.damageSources().inFire(), amt);
            } else if ("wither".equals(path)) {
                player.hurt(player.damageSources().wither(), amt);
            } else {
                player.hurt(player.damageSources().generic(), amt);
            }
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
            ResourceLocation attribute,
            UUID uuid,
            String name,
            double amount,
            String operation, // "add", "multiply_base", "multiply_total"
            int durationTicks,
            Scaling scaleAmount
    ) implements Action {
        public static final Codec<Attribute> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("attribute").forGetter(Attribute::attribute),
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(Attribute::uuid),
                Codec.STRING.fieldOf("name").forGetter(Attribute::name),
                Codec.DOUBLE.fieldOf("amount").forGetter(Attribute::amount),
                Codec.STRING.fieldOf("operation").forGetter(Attribute::operation),
                Codec.INT.fieldOf("durationTicks").forGetter(Attribute::durationTicks),
                Scaling.CODEC.optionalFieldOf("scaleAmount", new Scaling.Constant(1.0)).forGetter(Attribute::scaleAmount)
        ).apply(i, Attribute::new));

        @Override
        public String actionType() {
            return  "attribute";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            // Not implemented yet; placeholder no-op
        }
    }

    /** Client-side only visuals (screen vignette, blur, particles, geiger clicks). */
    record ClientFx(
            String fxId,
            Scaling intensity,
            int durationTicks
    ) implements Action {
        public static final Codec<ClientFx> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("fxId").forGetter(ClientFx::fxId),
                Scaling.CODEC.optionalFieldOf("intensity", new Scaling.Constant(1.0)).forGetter(ClientFx::intensity),
                Codec.INT.optionalFieldOf("durationTicks", 40).forGetter(ClientFx::durationTicks)
        ).apply(i, ClientFx::new));

        @Override
        public String actionType() {
            return  "client_fx";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            // Server-side: no-op placeholder
        }
    }

    /** Execute a command (server) with placeholders later. */
    record Command(String command) implements Action {
        public static final Codec<Command> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("command").forGetter(Command::command)
        ).apply(i, Command::new));

        @Override
        public String actionType() {
            return "command";
        }

        @Override
        public void apply(Player player, double value, double factor) {
            // Not implemented for safety; placeholder no-op
        }
    }
}
