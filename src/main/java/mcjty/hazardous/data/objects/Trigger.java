package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

/** Activation logic; supports thresholds + scaling curves. */
public sealed interface Trigger permits Trigger.Threshold, Trigger.Range, Trigger.Probability {
    // dispatch helpers
    private static Codec<? extends Trigger> triggerCodec(String type) {
        return switch (type) {
            case "threshold" -> Trigger.Threshold.CODEC;
            case "range" -> Trigger.Range.CODEC;
            case "probability" -> Trigger.Probability.CODEC;
            default -> throw new IllegalStateException("Unknown trigger type '" + type + "'");
        };
    }

    Codec<Trigger> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch(
            "type",
            Trigger::triggerType,
            Trigger::triggerCodec
    ));

    String triggerType();

    /** Decide if this trigger should fire for the given value. Probability may use random. */
    boolean shouldTrigger(double value, net.minecraft.util.RandomSource random);

    /** Compute an intensity factor (0..1 typically) for actions to scale with this trigger. */
    double factor(double value);

    /** Fires when value >= min (optionally with hysteresis). */
    record Threshold(double min, double hysteresis) implements Trigger {
        public static final Codec<Threshold> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Threshold::min),
                Codec.DOUBLE.optionalFieldOf("hysteresis", 0.0).forGetter(Threshold::hysteresis)
        ).apply(i, Threshold::new));

        @Override
        public String triggerType() {
            return "threshold";
        }

        @Override
        public boolean shouldTrigger(double value, net.minecraft.util.RandomSource random) {
            return value >= min();
        }

        @Override
        public double factor(double value) {
            return value >= min() ? 1.0 : 0.0;
        }
    }

    /** Fires when min <= value <= max; can also provide a normalized t (0..1) to scale actions. */
    record Range(double min, double max) implements Trigger {
        public static final Codec<Range> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Range::max)
        ).apply(i, Range::new));

        @Override
        public String triggerType() {
            return "range";
        }

        @Override
        public boolean shouldTrigger(double value, net.minecraft.util.RandomSource random) {
            return value >= min();
        }

        @Override
        public double factor(double value) {
            if (value > max()) {
                value = max();
            }
            double denom = (max() - min());
            if (denom <= 0) return value >= min() ? 1.0 : 0.0;
            double f = (value - min()) / denom;
            return Math.max(0.0, Math.min(1.0, f));
        }
    }

    /** Chance per evaluation; chance can be constant or derived from value via simple curve. */
    record Probability(Scaling scaling) implements Trigger {
        public static final Codec<Probability> CODEC = RecordCodecBuilder.create(i -> i.group(
                Scaling.CODEC.fieldOf("scaling").forGetter(Probability::scaling)
        ).apply(i, Probability::new));

        @Override
        public String triggerType() {
            return "probability";
        }

        @Override
        public boolean shouldTrigger(double value, net.minecraft.util.RandomSource random) {
            double chance = scaling().eval(value);
            if (Double.isNaN(chance)) chance = 0.0;
            chance = Math.max(0.0, Math.min(1.0, chance));
            return random.nextDouble() < chance;
        }

        @Override
        public double factor(double value) {
            double f = scaling().eval(value);
            if (Double.isNaN(f)) f = 0.0;
            return Math.max(0.0, Math.min(1.0, f));
        }
    }
}
