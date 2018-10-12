package io.mine.protocol.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.mine.protocol.api.Service;
import io.mine.protocol.codec.LengthCodec;
import io.mine.protocol.codec.TransferCodec;
import io.mine.protocol.data.DataProtocol;
import io.mine.protocol.data.DataProtocolException;
import io.mine.protocol.data.DataProtocols;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author koqizhao
 *
 * Oct 12, 2018
 */
public class NettyServer<Req, Res> extends AbstractAsyncServer<Req, Res> {

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
                        ch.pipeline().addLast(new NettyEncoder(), new NettyDecoder(), new NettyServerHandler());
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

    protected void handleRequest(ChannelHandlerContext ctx, NettyRequestContext<Req, Res> requestContext)
            throws InterruptedException {
        Req request = requestContext.getRequest();
        if (isAsyncService()) {
            CompletableFuture<Res> future = getAsyncService().invokeAsync(request);
            future.whenComplete((res, e) -> {
                if (e != null) {
                    System.out.println("async service execution failed");
                    e.printStackTrace();
                    res = null;
                }

                requestContext.setResponse(res);
                ctx.writeAndFlush(requestContext);
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

            requestContext.setResponse(response);
            System.out.println("write response started");
            ctx.writeAndFlush(requestContext);
        });
    }

    protected class NettyDecoder extends ByteToMessageDecoder {

        private volatile NettyRequestContext<Req, Res> _requestContext;
        private volatile DefaultAsyncRequest _asyncRequest;

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            if (_requestContext == null) {
                if (in.readableBytes() < 1)
                    return;

                byte version = in.readByte();
                DataProtocol dataProtocol = DataProtocols.ALL.get(version);
                if (dataProtocol == null)
                    throw new DataProtocolException("Unknown protocol: " + version);

                _requestContext = new NettyRequestContext<Req, Res>(dataProtocol);
                _asyncRequest = new DefaultAsyncRequest();
            }

            DataProtocol dataProtocol = _requestContext.getDataProtocol();
            LengthCodec lengthCodec = dataProtocol.getLengthCodec();
            TransferCodec transferCodec = dataProtocol.getTransferCodec();
            while (in.readableBytes() > 0) {
                if (_asyncRequest.needNextTrunk()) {
                    if (in.readableBytes() < lengthCodec.getLengthByteCount())
                        break;

                    byte[] lengthBytes = new byte[lengthCodec.getLengthByteCount()];
                    in.readBytes(lengthBytes);
                    _asyncRequest.setCurrentTrunk(lengthBytes);
                    _asyncRequest.completeCurrentTrunk();
                    int length = lengthCodec.decode(lengthBytes);
                    if (length == TransferCodec.FIN_LENGTH) {
                        _asyncRequest.ready();
                        Req request = NettyServer.this.decode(transferCodec, _asyncRequest.getData());
                        _requestContext.setRequest(request);
                        out.add(_requestContext);
                        System.out.println("decode success");
                        reset();
                        break;
                    }

                    _asyncRequest.setCurrentTrunk(new byte[length]);
                } else {
                    byte[] dataBytes = _asyncRequest.getCurrentTrunk();
                    int needRead = _asyncRequest.getCurrentTrunkRemaining();
                    if (needRead > in.readableBytes())
                        needRead = in.readableBytes();
                    in.readBytes(dataBytes, _asyncRequest.getCurrentTrunkRead(), needRead);
                    _asyncRequest.addCurrentTrunkRead(needRead);
                }
            }
        }

        private void reset() {
            _requestContext = null;
            _asyncRequest = null;
        }
    }

    protected class NettyServerHandler extends ChannelInboundHandlerAdapter {
        @SuppressWarnings("unchecked")
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            handleRequest(ctx, (NettyRequestContext<Req, Res>) msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close().addListener(f -> System.out.println("connection closed by error: " + cause));
        }
    }

    protected class NettyEncoder extends MessageToByteEncoder<NettyRequestContext<Req, Res>> {
        @Override
        protected void encode(ChannelHandlerContext ctx, NettyRequestContext<Req, Res> msg, ByteBuf out)
                throws Exception {
            DataProtocol dataProtocol = msg.getDataProtocol();
            byte[] responseData = dataProtocol.getTransferCodec().encode(msg.getResponse());
            out.writeByte(dataProtocol.getVersion()).writeBytes(responseData);
            System.out.println("encode success");
        }
    }

}
