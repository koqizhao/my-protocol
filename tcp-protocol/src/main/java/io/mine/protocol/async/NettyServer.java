package io.mine.protocol.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mine.protocol.api.Service;
import io.mine.protocol.data.DataProtocol;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
public class NettyServer<Req, Res> extends AbstractAsyncServer<Req, Res> {

    protected static final AttributeKey<DataProtocol> _dataProtocolAttributeKey = AttributeKey.valueOf("data-protocol");

    private ServerBootstrap _serverBootstrap;

    public NettyServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        super(socketAddress, service);
    }

    @Override
    protected void doStart() throws InterruptedException {
        _serverBootstrap = new ServerBootstrap().channel(NioServerSocketChannel.class).localAddress(getSocketAddress())
                .group(new NioEventLoopGroup(1), new NioEventLoopGroup(10)).option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_RCVBUF, 32 * 1024).childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.SO_SNDBUF, 32 * 1024).childOption(ChannelOption.SO_RCVBUF, 32 * 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new NettyServerEncoder(getService().getResponseType(), _dataProtocolAttributeKey),
                                new NettyServerDecoder(getService().getRequestType()), new NettyServerHandler());
                        ch.attr(_dataProtocolAttributeKey).set(null);
                    }
                });
        ChannelFuture future = _serverBootstrap.bind().sync();
        System.out.println("server started");
        future.channel().closeFuture().sync();
    }

    @Override
    protected void doStop() throws IOException, InterruptedException {
        if (_serverBootstrap != null) {
            _serverBootstrap.config().group().shutdownGracefully().sync();
            _serverBootstrap.config().childGroup().shutdownGracefully().sync();
            _serverBootstrap = null;
            System.out.println("server stopped");
        }
    }

    protected void handleRequest(ChannelHandlerContext ctx, Req request) throws InterruptedException {
        if (isAsyncService()) {
            CompletableFuture<Res> future = getAsyncService().invokeAsync(request);
            future.whenComplete((res, e) -> {
                if (e != null) {
                    System.out.println("async service execution failed");
                    e.printStackTrace();
                    res = null;
                }

                ctx.writeAndFlush(res);
            });

            return;
        }

        getWorkerPool().submit(() -> {
            Res response = null;
            try {
                response = getService().invoke(request);
            } catch (Exception e) {
                System.out.println("service execution failed");
                e.printStackTrace();
            }

            ctx.writeAndFlush(response);
        });
    }

    protected class NettyServerEncoder extends NettyEncoder<Res> {

        public NettyServerEncoder(Class<Res> dataType, AttributeKey<DataProtocol> dataProtocolAttributeKey) {
            super(dataType, dataProtocolAttributeKey);
        }

        @Override
        protected DataProtocol getDataProtocol(Attribute<DataProtocol> dataProtocolAttribute) {
            return dataProtocolAttribute.getAndSet(null);
        }

    }

    protected class NettyServerDecoder extends NettyDecoder<Req> {

        public NettyServerDecoder(Class<Req> dataType) {
            super(dataType);
        }

        @Override
        protected void addToOut(ChannelHandlerContext ctx, DataProtocol dataProtocol, List<Object> out, Req data) {
            out.add(data);
            ctx.channel().attr(_dataProtocolAttributeKey).set(dataProtocol);
        }

    }

    protected class NettyServerHandler extends ChannelInboundHandlerAdapter {

        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            handleRequest(ctx, (Req) msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close().addListener(f -> System.out.println("connection closed by error: " + cause));
        }

    }

}
