import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import org.apache.commons.lang3.NotImplementedException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

@Sharable
public class RedisServerHandler extends ChannelInboundHandlerAdapter {
    private final Map<String, String> keyValues = new HashMap<>();

    private String handle(String req) {
        req = req.toLowerCase();
        System.out.println(req);
        String[] split = req.split("\r\n");
        var arrLen = Integer.valueOf(split[0].substring(1));
        // handle only bulk string at this moment
        List<String> bulkStringArr = new ArrayList<>(List.of());
        for (int i = 0; i < arrLen; i++) {
            int idx = 1 + 2 * i;
            // assert only bulk string
            assert split[idx].charAt(0) == '$';
            String str = split[idx + 1];
            bulkStringArr.add(str);
        }
        if (bulkStringArr.getFirst().equals("ping")) {
            return "PONG";
        } else if (bulkStringArr.getFirst().equals("echo")) {
            return bulkStringArr.get(1);
        } else if (bulkStringArr.getFirst().equals("set")) {
            final String key = bulkStringArr.get(1);
            final String value = bulkStringArr.get(2);
            keyValues.put(key, value);
            return "OK";
        } else if (bulkStringArr.getFirst().equals("get")) {
            final String key = bulkStringArr.get(1);
            return keyValues.get(key);
        }
        throw new NotImplementedException("parse failed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("in: " + in.toString(CharsetUtil.UTF_8));
        String res = handle(in.toString(CharsetUtil.UTF_8));
        String finalStringRes = res == null ?
                                "$-1\r\n" :
                                "+" + res + "\r\n";
        ByteBuf response = Unpooled.copiedBuffer(finalStringRes, CharsetUtil.UTF_8);
        ctx.writeAndFlush(response);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
//        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
//                .addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
