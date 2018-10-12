package io.mine.protocol.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import io.mine.protocol.api.Service;

/**
 * @author koqizhao
 *
 * Oct 10, 2018
 */
public interface Server<Req, Res> extends Closeable {

    InetSocketAddress getSocketAddress();

    Service<Req, Res> getService();

    boolean isStarted();

    void start() throws IOException, InterruptedException;

    void stop() throws IOException, InterruptedException;

}
