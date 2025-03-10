package com.github.igrishaev.codec;

import clojure.lang.Symbol;
import com.github.igrishaev.Const;
import com.github.igrishaev.enums.OID;

import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.BigInteger;
import clojure.lang.BigInt;
import com.github.igrishaev.util.JSON;

public class EncoderTxt extends ACodec {

    public String encode(Object x, OID oid) {
        return switch (x) {

            case Symbol s -> switch (oid) {
                case TEXT, VARCHAR -> s.toString();
                default -> txtEncodingError(x, oid);
            };
            
            case String s -> switch (oid) {
                case TEXT, VARCHAR -> s;
                default -> txtEncodingError(x, oid);
            };
            
            case Short s -> switch (oid) {
                case INT2 -> String.valueOf(s);
                case INT4 -> String.valueOf(s.intValue());
                case INT8 -> String.valueOf(s.longValue());
                default -> txtEncodingError(x, oid);
            };

            case Integer i -> switch (oid) {
                case INT2 -> String.valueOf(i.shortValue());
                case INT4 -> String.valueOf(i);
                case INT8 -> String.valueOf(i.longValue());
                default -> txtEncodingError(x, oid);
            };

            case Long l -> switch (oid) {
                case INT2 -> String.valueOf(l.shortValue());
                case INT4 -> String.valueOf(l.intValue());
                case INT8 -> String.valueOf(l);
                default -> txtEncodingError(x, oid);
            };

            case Float f -> switch (oid) {
                case FLOAT4 -> String.valueOf(f);
                case FLOAT8 -> String.valueOf(f.doubleValue());
                default -> txtEncodingError(x, oid);
            };

            case Double d -> switch (oid) {
                case FLOAT4 -> String.valueOf(d.floatValue());
                case FLOAT8 -> String.valueOf(d);
                default -> txtEncodingError(x, oid);
            };

            case UUID u -> switch (oid) {
                case TEXT, VARCHAR -> String.valueOf(u);
                default -> txtEncodingError(x, oid);
            };

            case Boolean b -> {
                if (oid == OID.BOOL) {
                    yield b ? "t" : "f";
                }
                else {
                    yield txtEncodingError(x, oid);
                }
            }

            case BigDecimal bd -> switch (oid) {
                case NUMERIC, FLOAT4, FLOAT8 -> bd.toString();
                default -> txtEncodingError(x, oid);
            };

            case BigInteger bi -> switch (oid) {
                case INT2, INT4, INT8 -> bi.toString();
                default -> txtEncodingError(x, oid);
            };

            case BigInt bi -> switch (oid) {
                case INT2, INT4, INT8 -> bi.toString();
                default -> txtEncodingError(x, oid);
            };

            case JSON.Wrapper w -> switch (oid) {
                case JSON, JSONB -> {
                    // TODO: maybe return bytes?
                    // TODO: guess the initial size?
                    StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, w.value());
                    yield writer.toString();
                }
                default -> txtEncodingError(w.value(), oid);
            };

            case Map<?, ?> m -> switch (oid) {
                case JSON, JSONB -> {
                    // TODO: maybe return bytes?
                    // TODO: guess the initial size?
                    StringWriter writer = new StringWriter(Const.JSON_ENC_BUF_SIZE);
                    JSON.writeValue(writer, m);
                    yield writer.toString();
                }
                default -> txtEncodingError(x, oid);
            };

            default -> txtEncodingError(x, oid);
        };
    }
}
