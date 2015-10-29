package wav;

import java.util.HashMap;
import java.util.Map;

enum Format {
    PCM(1);

    private final int code;
    private static final Map<Integer, Format> intToEnum = new HashMap<>();

    static {
        for (Format f : values()) {
            intToEnum.put(f.code(), f);
        }
    }

    Format(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    private static Format fromInt(int formatCode) {
        return intToEnum.get(formatCode);
    }

    public static Format fromShort (short formatCode) {
        return fromInt((int) formatCode);
    }
}
