import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class BulkString implements RespDataType {
    private final String string;

    public BulkString(String string) {
        this.string = string;
    }

    @Override
    public ByteBuf encode() {
        return Unpooled.copiedBuffer(
                string == null ? "$-1\r\n" : "$" + string.length() + "\r\n" + string + "\r\n"
        , StandardCharsets.UTF_8);
    }
}
