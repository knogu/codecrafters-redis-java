package resp;

import io.netty.buffer.ByteBuf;

public interface RespDataType {
    public ByteBuf encode();
}
