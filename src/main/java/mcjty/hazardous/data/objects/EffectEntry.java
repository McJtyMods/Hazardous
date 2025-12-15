package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One rule that can apply zero or more consequences. */
public record EffectEntry(
        // When does this entry activate?
        Trigger trigger,

        // What does it do?
        Action action) {

    public static final Codec<EffectEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Trigger.CODEC.fieldOf("trigger").forGetter(EffectEntry::trigger),
                    Action.CODEC.fieldOf("action").forGetter(EffectEntry::action)
            ).apply(instance, EffectEntry::new));
}
