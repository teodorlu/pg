package com.github.igrishaev.enums;

import com.github.igrishaev.PGError;

public enum Format {
    TXT((short)0), BIN((short)1);

    private final short code;

    Format(short code) {
        this.code = code;
    }

    public short toCode () {
        return code;
    }

    public static Format ofShort (short code) {
        return switch (code) {
            case 0 -> TXT;
            case 1 -> BIN;
            default -> throw new PGError("wrong format code: %s", code);
        };
    }


}
