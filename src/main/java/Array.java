import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Array implements RespDataType {
    final List<RespDataType> elements;

    public Array(List<RespDataType> elements) {
        this.elements = elements;
    }

    @Override
    public ByteBuf encode() {
        ByteBuf buf = Unpooled.copiedBuffer("*" + elements.size() + "\r\n", StandardCharsets.UTF_8);
        for (int i = 0; i < elements.size(); i++) {
            buf = Unpooled.copiedBuffer(buf, elements.get(i).encode());
//            if (i < elements.size() - 1) {
//                buf = Unpooled.copiedBuffer(buf, Unpooled.copiedBuffer("\r\n", StandardCharsets.UTF_8));
//            }
        }
        return buf;
    }
}
