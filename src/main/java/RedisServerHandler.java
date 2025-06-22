import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

@Sharable
public class RedisServerHandler extends ChannelInboundHandlerAdapter {
    private final String masterHostname;
    private final int masterPort;
    private final Map<String, Long> keyToExpiry = new HashMap<>();
    private final Map<String, String> keyValues = new TreeMap<>((key1, key2) -> {
        var expiry1 = keyToExpiry.get(key1);
        var expiry2 = keyToExpiry.get(key2);
        return Long.compare(expiry1, expiry2);
    });

    private boolean isMaster() {
        return StringUtils.isEmpty(masterHostname);
    }

    public RedisServerHandler(String masterHostname, int masterPort) throws IOException {
        this.masterHostname = masterHostname;
        this.masterPort = masterPort;

        if (!masterHostname.isEmpty()) {
            Socket masterConn = new Socket(masterHostname, masterPort);
            masterConn.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes());
            masterConn.getOutputStream().flush();
            masterConn.close();
        }
    }

    private RespDataType handle(String req) {
        for (var key : keyValues.keySet()) {
            final var expiry = keyToExpiry.get(key);
            if (System.currentTimeMillis() < expiry) {
                break;
            }
            keyValues.remove(key);
            keyToExpiry.remove(key);
        }

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
            return new SimpleString("PONG");
        } else if (bulkStringArr.getFirst().equals("echo")) {
            return new SimpleString(bulkStringArr.get(1));
        } else if (bulkStringArr.getFirst().equals("set")) {
            final String key = bulkStringArr.get(1);
            final String value = bulkStringArr.get(2);
            System.out.println("size: " + bulkStringArr.size());
            System.out.println("check: " + (4 <= bulkStringArr.size()));
            long expiry;
            if (4 <= bulkStringArr.size()) {
                assert "ex".equals(bulkStringArr.get(3));
                expiry = System.currentTimeMillis() + Long.parseLong(bulkStringArr.get(4));
            } else {
                expiry = Long.MAX_VALUE;
            }
            keyToExpiry.put(key, expiry);
            keyValues.put(key, value);
            return new SimpleString("OK");
        } else if (bulkStringArr.getFirst().equals("get")) {
            final String key = bulkStringArr.get(1);
            final String value = keyValues.get(key);
            return value == null ? new BulkString(null) : new SimpleString(value) ;
        } else if (bulkStringArr.getFirst().equals("info")) {
            StringJoiner joiner = new StringJoiner("\r\n");
            joiner.add("role:" + (isMaster() ? "master" : "slave"));
            joiner.add("master_replid:" + "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"); // psuedo random
            joiner.add("master_repl_offset:0"); // todo: fill correct value
            return new BulkString(joiner.toString());
        }
        throw new NotImplementedException("parse failed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        System.out.println("in: " + in.toString(CharsetUtil.UTF_8));
        RespDataType res = handle(in.toString(CharsetUtil.UTF_8));
        ByteBuf response = Unpooled.copiedBuffer(res.encode(), CharsetUtil.UTF_8);
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
