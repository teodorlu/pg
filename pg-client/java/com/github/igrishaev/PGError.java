package com.github.igrishaev;


public class PGError extends Error {

    public PGError (String template, Object... args) {
        super(String.format(template, args));
    }

    public PGError (Throwable e, String template, Object... args) {
        super(String.format(template, args), e);
    }

}
