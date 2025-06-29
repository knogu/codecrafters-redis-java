package resp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Rdb implements RespDataType {
    final byte[] contents;

    public Rdb(byte[] contents) {
        this.contents = contents;
    }

    public static Rdb ofEmpty() {
        return new Rdb(Base64.getDecoder().decode("UkVESVMwMDEx+glyZWRpcy12ZXIFNy4yLjD6CnJlZGlzLWJpdHPAQPoFY3RpbWXCbQi8ZfoIdXNlZC1tZW3CsMQQAPoIYW9mLWJhc2XAAP/wbjv+wP9aog=="));
    }

    @Override
    public ByteBuf encode() {
        final ByteBuf header = Unpooled.copiedBuffer("$" + contents.length + "\r\n", StandardCharsets.UTF_8);
        final ByteBuf content = Unpooled.copiedBuffer(contents);
        return Unpooled.copiedBuffer(header, content);
    }
}
