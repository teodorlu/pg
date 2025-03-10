package com.github.igrishaev.msg;

import com.github.igrishaev.PGError;
import com.github.igrishaev.util.BBTool;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FieldParser {

    public static String parseTag (char tag) {
        return switch (tag) {
            case 'S' -> "severity";
            case 'V' -> "verbosity";
            case 'C' -> "code";
            case 'M' -> "message";
            case 'D' -> "detail";
            case 'H' -> "hint";
            case 'P' -> "position";
            case 'p' -> "position-internal";
            case 'q' -> "query";
            case 'W' -> "stacktrace";
            case 's' -> "schema";
            case 't' -> "table";
            case 'c' -> "column";
            case 'd' -> "datatype";
            case 'n' -> "constraint";
            case 'F' -> "file";
            case 'L' -> "line";
            case 'R' -> "function";
            default -> throw new PGError("unknown tag: %s", tag);
        };
    }

    public static Map<String, String> parseFields(ByteBuffer buf, String encoding) {
        HashMap<String, String> fields = new HashMap<>();
        while (true) {
            byte tag = buf.get();
            if (tag == 0) {
                break;
            }
            else {
                String field = parseTag((char)tag);
                String message = BBTool.getCString(buf, encoding);
                fields.put(field, message);
            };
        };
        return fields;
    }
}
