import org.apache.commons.lang3.NotImplementedException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

@Sharable
public class RedisServerHandler extends ChannelInboundHandlerAdapter {
    private static String handle(String req) {
        req = req.toLowerCase();
        System.out.println(req);
        if ("*1\r\n$4\r\nping\r\n".equals(req)) {
            return "PONG";
        } else if (req.startsWith("*2\r\n$4\r\necho\r\n")) {
            return req.substring("*2\r\n$4\r\necho\r\n$1\r\n".length(), req.length() - 2);
        }
        throw new NotImplementedException("parse failed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("in: " + in.toString(CharsetUtil.UTF_8));
        ByteBuf response = Unpooled.copiedBuffer("+" + handle(in.toString(CharsetUtil.UTF_8)) + "\r\n", CharsetUtil.UTF_8);
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
