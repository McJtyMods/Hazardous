package mcjty.hazardous.data.objects;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

/** Scaling functions: maps value -> factor (often 0..1 but not required). */
public sealed interface Scaling permits Scaling.Constant, Scaling.Linear01, Scaling.Clamp, Scaling.Power {
    private static Codec<? extends Scaling> scalingCodec(String type) {
        return switch (type) {
            case "constant" -> Scaling.Constant.CODEC;
            case "linear01" -> Scaling.Linear01.CODEC;
            case "clamp" -> Scaling.Clamp.CODEC;
            case "power" -> Scaling.Power.CODEC;
            default -> throw new IllegalStateException("Unknown scaling type '" + type + "'");
        };
    }

    Codec<Scaling> CODEC = ExtraCodecs.lazyInitializedCodec(() -> Codec.STRING.dispatch(
            "type",
            Scaling::scalingType,
            Scaling::scalingCodec
    ));

    String scalingType();

    /** Evaluate this scaling at the given input value. */
    default double eval(double v) {
        if (this instanceof Constant c) {
            return c.value();
        } else if (this instanceof Linear01 l) {
            double denom = (l.max() - l.min());
            if (denom == 0) return v >= l.min() ? 1.0 : 0.0;
            double f = (v - l.min()) / denom;
            return Mth.clamp(f, 0.0, 1.0);
        } else if (this instanceof Clamp c) {
            double inner = c.inner().eval(v);
            return Mth.clamp(inner, c.min(), c.max());
        } else if (this instanceof Power p) {
            double inner = p.inner().eval(v);
            return Math.pow(inner, p.exponent());
        }
        return 1.0;
    }

    record Constant(double value) implements Scaling {
        public static final Codec<Constant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("value").forGetter(Constant::value)
        ).apply(i, Constant::new));

        @Override
        public String scalingType() {
            return "constant";
        }
    }

    /** factor = clamp((v - min) / (max - min), 0..1) */
    record Linear01(double min, double max) implements Scaling {
        public static final Codec<Linear01> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Linear01::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Linear01::max)
        ).apply(i, Linear01::new));

        @Override
        public String scalingType() {
            return "linear01";
        }
    }

    record Clamp(Scaling inner, double min, double max) implements Scaling {
        public static final Codec<Clamp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Scaling.CODEC.fieldOf("inner").forGetter(Clamp::inner),
                Codec.DOUBLE.fieldOf("min").forGetter(Clamp::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Clamp::max)
        ).apply(i, Clamp::new));

        @Override
        public String scalingType() {
            return "clamp";
        }
    }

    /** factor = pow(inner, exponent) */
    record Power(Scaling inner, double exponent) implements Scaling {
        public static final Codec<Power> CODEC = RecordCodecBuilder.create(i -> i.group(
                Scaling.CODEC.fieldOf("inner").forGetter(Power::inner),
                Codec.DOUBLE.fieldOf("exponent").forGetter(Power::exponent)
        ).apply(i, Power::new));

        @Override
        public String scalingType() {
            return "power";
        }
    }
}

