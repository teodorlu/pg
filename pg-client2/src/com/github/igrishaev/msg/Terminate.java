package com.github.igrishaev.msg;

import com.github.igrishaev.Payload;
import com.github.igrishaev.msg.IMessage;

import java.nio.ByteBuffer;

public record Terminate () implements IMessage {
    public ByteBuffer encode(String encoding) {
        return new Payload().toByteBuffer('X');
    }
}
