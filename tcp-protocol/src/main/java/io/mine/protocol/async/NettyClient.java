package io.mine.protocol.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import io.mine.protocol.client.AbstractClient;
import io.mine.protocol.data.DataProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
public class NettyClient<Req, Res> extends AbstractClient<Req, Res> {

    private Bootstrap _bootstrap;
    private ChannelHandlerContext _channelHandlerContext;
    private volatile CompletableFuture<Res> _completableFuture;

    public NettyClient(Class<Req> requestType, Class<Res> responseType, InetSocketAddress serverAddress,
            DataProtocol dataProtocol) throws IOException, InterruptedException {
        super(requestType, responseType, serverAddress, dataProtocol);

        _bootstrap = new Bootstrap().channel(NioSocketChannel.class).remoteAddress(getServerAddress())
                .group(new NioEventLoopGroup(1)).option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_SNDBUF, 32 * 1024).option(ChannelOption.SO_RCVBUF, 32 * 1024)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        AttributeKey<DataProtocol> dataProtocolAttributeKey = AttributeKey.valueOf("data-protocol");
                        ch.pipeline().addLast(new NettyEncoder<Req>(getRequestType(), dataProtocolAttributeKey),
                                new NettyDecoder<>(getResponseType()), new NettyClientHandler());
                        ch.attr(dataProtocolAttributeKey).set(getDataProtocol());
                    }
                });
        _bootstrap.connect().sync();
        while (_channelHandlerContext == null)
            Thread.sleep(1);
    }

    protected class NettyClientHandler extends ChannelInboundHandlerAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (_completableFuture != null) {
                _completableFuture.complete((Res) msg);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            if (_completableFuture != null) {
                _completableFuture.completeExceptionally(cause);
            }

            cause.printStackTrace();
            ctx.close().addListener(f -> System.out.println("connection closed by error: " + cause));
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            _channelHandlerContext = ctx;
        }
    }

    @Override
    public Res invoke(Req request) {
        _channelHandlerContext.writeAndFlush(request);
        _completableFuture = new CompletableFuture<Res>();
        try {
            return _completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            _completableFuture = null;
        }
    }

    @Override
    public void close() throws IOException {
        if (_bootstrap != null) {
            try {
                _bootstrap.config().group().shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            _bootstrap = null;
        }
    }

}
