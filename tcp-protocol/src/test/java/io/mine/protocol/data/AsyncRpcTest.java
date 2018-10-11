package io.mine.protocol.data;

import java.net.InetSocketAddress;

import io.mine.protocol.api.sample.SampleRequest;
import io.mine.protocol.api.sample.SampleResponse;
import io.mine.protocol.api.sample.SampleService;
import io.mine.protocol.server.Server;
import io.mine.protocol.server.async.AsyncServer;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class AsyncRpcTest extends RpcTest {

    @Override
    protected Server<SampleRequest, SampleResponse> newServer(InetSocketAddress serverAddress) {
        return new AsyncServer<>(serverAddress, new SampleService());
    }

}
