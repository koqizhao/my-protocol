package io.mine.protocol.data;

import java.io.IOException;
import java.net.InetSocketAddress;

import io.mine.protocol.api.sample.SampleRequest;
import io.mine.protocol.api.sample.SampleResponse;
import io.mine.protocol.async.AsyncClient;
import io.mine.protocol.client.Client;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class AsyncRpcTest2 extends RpcTest {

    @Override
    protected Client<SampleRequest, SampleResponse> newClient(InetSocketAddress serverAddress) throws IOException {
        return new AsyncClient<>(SampleRequest.class, SampleResponse.class, serverAddress, dataProtocol);
    }

}
