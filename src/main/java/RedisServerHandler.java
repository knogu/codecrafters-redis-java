import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    public RedisServerHandler(int selfPort, String masterHostname, int masterPort)
            throws IOException, InterruptedException {
        this.masterHostname = masterHostname;
        this.masterPort = masterPort;

        if (!masterHostname.isEmpty()) {
            final Socket masterConn = new Socket(masterHostname, masterPort);
            final OutputStream out = masterConn.getOutputStream();
            final BufferedReader in = new BufferedReader(new InputStreamReader(masterConn.getInputStream(), StandardCharsets.UTF_8));
            out.write("*1\r\n$4\r\nPING\r\n".getBytes());
            out.flush();
            assert in.readLine().contains("PONG");
            final String sendListeningPort = String.format("*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n%d\r\n", selfPort);
            out.write(sendListeningPort.getBytes());
            out.flush();
            while (true) {
                if (in.readLine().contains("OK")) {
                    break;
                }
            }
            out.write("*3\r\n$8\r\nREPLCONF\r\n$4\r\ncapa\r\n$6\r\npsync2\r\n".getBytes());
            out.flush();
            while (true) {
                if (in.readLine().contains("OK")) {
                    break;
                }
            }
            out.write("*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n".getBytes());
            out.flush();
            while (true) {
                if (in.readLine().contains("FULLRESYNC")) {
                    break;
                }
            }

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
        final String[] split = req.split("\r\n");
        final var arrLen = Integer.parseInt(split[0].substring(1));
        // handle only bulk string at this moment
        final List<String> bulkStringArr = new ArrayList<>(List.of());
        for (int i = 0; i < arrLen; i++) {
            final int idx = 1 + 2 * i;
            // assert only bulk string
            assert split[idx].charAt(0) == '$';
            final String str = split[idx + 1];
            bulkStringArr.add(str);
        }

        if ("ping".equals(bulkStringArr.getFirst())) {
            return new SimpleString("PONG");
        } else if ("echo".equals(bulkStringArr.getFirst())) {
            return new SimpleString(bulkStringArr.get(1));
        } else if ("set".equals(bulkStringArr.getFirst())) {
            final String key = bulkStringArr.get(1);
            final String value = bulkStringArr.get(2);
            System.out.println("size: " + bulkStringArr.size());
            System.out.println("check: " + (4 <= bulkStringArr.size()));
            final long expiry;
            if (4 <= bulkStringArr.size()) {
                assert "ex".equals(bulkStringArr.get(3));
                expiry = System.currentTimeMillis() + Long.parseLong(bulkStringArr.get(4));
            } else {
                expiry = Long.MAX_VALUE;
            }
            keyToExpiry.put(key, expiry);
            keyValues.put(key, value);
            return new SimpleString("OK");
        } else if ("get".equals(bulkStringArr.getFirst())) {
            final String key = bulkStringArr.get(1);
            final String value = keyValues.get(key);
            return value == null ? new BulkString(null) : new SimpleString(value) ;
        } else if ("info".equals(bulkStringArr.getFirst())) {
            final StringJoiner joiner = new StringJoiner("\r\n");
            joiner.add("role:" + (isMaster() ? "master" : "slave"));
            joiner.add("master_replid:" + "8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb"); // psuedo random
            joiner.add("master_repl_offset:0"); // todo: fill correct value
            return new BulkString(joiner.toString());
        } else if ("replconf".equals(bulkStringArr.getFirst())) {
            return new SimpleString("OK");
        }
        throw new NotImplementedException("parse failed");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        final ByteBuf in = (ByteBuf) msg;
        System.out.println("in: " + in.toString(CharsetUtil.UTF_8));
        final RespDataType res = handle(in.toString(CharsetUtil.UTF_8));
        final ByteBuf response = Unpooled.copiedBuffer(res.encode(), CharsetUtil.UTF_8);
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
