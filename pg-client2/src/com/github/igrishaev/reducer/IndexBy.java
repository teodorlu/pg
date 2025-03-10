
package com.github.igrishaev.reducer;

import clojure.core$assoc_BANG_;
import clojure.core$persistent_BANG_;
import clojure.lang.PersistentHashMap;
import clojure.lang.IFn;

public class IndexBy extends MapMixin implements IReducer {

    private final IFn f;

    public IndexBy(IFn f) {
        this.f = f;
    }

    public Object initiate () {
        return PersistentHashMap.EMPTY.asTransient();
    }

    public Object append (Object acc, Object row) {
        return core$assoc_BANG_.invokeStatic(acc, f.invoke(row), row);
    }

    public Object finalize (Object acc) {
        return core$persistent_BANG_.invokeStatic(acc);
    }
}
