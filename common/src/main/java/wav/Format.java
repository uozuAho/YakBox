package wav;

import java.util.HashMap;
import java.util.Map;

enum Format {
    PCM(1);

    private int code;
    private static Map<Integer, Format> intToEnum = new HashMap<>();

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

    public static Format fromInt (int formatCode) {
        return intToEnum.get(formatCode);
    }

    public static Format fromShort (short formatCode) {
        return fromInt((int) formatCode);
    }
}
