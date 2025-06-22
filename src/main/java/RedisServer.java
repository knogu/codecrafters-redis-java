import java.net.InetSocketAddress;

import org.apache.commons.lang3.StringUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class RedisServer {
    private final int port;
    private final String masterHostname;
    private final int masterPort;

    public RedisServer(int port, String masterHostname, int masterPort) {
        this.port = port;
        this.masterHostname = masterHostname;
        this.masterPort = masterPort;
    }

    public void start() throws Exception {
        final RedisServerHandler handler = new RedisServerHandler(port, masterHostname, masterPort);
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(handler);
                        }
                    });
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
