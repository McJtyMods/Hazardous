package mcjty.hazardous.data.objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Arrays;
import java.util.Locale;

public enum ClientFxId {
    DARKEN("darken"),
    LIGHTEN("lighten"),
    BLUR("blur"),
    BLUR_RADIAL("blurradial"),
    SHAKE("shake"),
    WARP("warp"),
    GEIGER("geiger");

    public static final Codec<ClientFxId> CODEC = Codec.STRING.comapFlatMap(
            ClientFxId::decode,
            ClientFxId::serializedName
    );

    private final String serializedName;

    ClientFxId(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static DataResult<ClientFxId> decode(String value) {
        ClientFxId fxId = fromSerializedName(value);
        if (fxId != null) {
            return DataResult.success(fxId);
        }
        return DataResult.error(() -> "Unknown client fx id '" + value + "'");
    }

    public static ClientFxId fromSerializedName(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if ("shaking".equals(normalized)) {
            return SHAKE;
        }
        if ("warping".equals(normalized)) {
            return WARP;
        }
        return Arrays.stream(values())
                .filter(fxId -> fxId.serializedName.equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
