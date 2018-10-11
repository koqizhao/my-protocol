package io.mine.protocol.client;

import java.io.Closeable;
import java.net.InetSocketAddress;

import io.mine.protocol.api.Service;
import io.mine.protocol.data.DataProtocol;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public interface ServiceClient<Req, Res> extends Service<Req, Res>, Closeable {

    InetSocketAddress getServerAddress();

    DataProtocol getDataProtocol();

}
