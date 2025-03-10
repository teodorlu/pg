package com.github.igrishaev.reducer;

import clojure.core$persistent_BANG_;
import clojure.core$conj_BANG_;
import clojure.lang.PersistentVector;

public class Default extends MapMixin implements IReducer {

    public Object initiate() {
        return PersistentVector.EMPTY.asTransient();
    }

    public Object append(Object acc, Object row) {
        return core$conj_BANG_.invokeStatic(acc, row);
    }

    public Object finalize(Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
