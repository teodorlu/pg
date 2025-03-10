package com.github.igrishaev.msg;

import java.nio.ByteBuffer;

public record CancelRequest(
        int code,
        int pid,
        int secretKey) implements IMessage {

    public ByteBuffer encode(String encoding) {
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.putInt(16);
        buf.putInt(code);
        buf.putInt(pid);
        buf.putInt(secretKey);
        return buf;
    }
}
