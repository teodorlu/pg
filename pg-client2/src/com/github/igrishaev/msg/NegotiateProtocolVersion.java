package com.github.igrishaev.msg;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import com.github.igrishaev.util.BBTool;
import com.github.igrishaev.util.IClojure;
import java.nio.ByteBuffer;
import java.util.Arrays;

public record NegotiateProtocolVersion(
        int version,
        int paramCount,
        String[] params
) implements IClojure {

    public IPersistentMap toClojure () {
        return PersistentHashMap.create(
                Keyword.intern("version"), version,
                Keyword.intern("param-count"), paramCount,
                Keyword.intern("params"), PersistentVector.create(Arrays.asList(params))
        );
    }

    public static NegotiateProtocolVersion fromByteBuffer(ByteBuffer buf) {
        final int version = buf.getInt();
        final int paramCount = buf.getInt();
        final String[] params = new String[paramCount];
        // TODO: encoding
        for (int i = 0; i < paramCount; i++) {
            params[i] = BBTool.getCString(buf, "UTF-8");
        }
        return new NegotiateProtocolVersion(version, paramCount, params);
    }
}
