import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class SimpleString implements RespDataType {
    private final String string;

    public SimpleString(String string) {
        this.string = string;
    }

    @Override
    public ByteBuf encode() {
        return Unpooled.copiedBuffer('+' + string + "\r\n", StandardCharsets.UTF_8);
    }
}
