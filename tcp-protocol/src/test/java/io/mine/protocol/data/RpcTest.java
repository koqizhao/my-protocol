package io.mine.protocol.data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mydotey.objectpool.facade.ThreadPools;
import org.mydotey.objectpool.threadpool.ThreadPool;
import org.mydotey.objectpool.threadpool.ThreadPoolConfig;

import io.mine.protocol.api.sample.SampleRequest;
import io.mine.protocol.api.sample.SampleResponse;
import io.mine.protocol.api.sample.SampleService;
import io.mine.protocol.client.Client;
import io.mine.protocol.client.SyncClient;
import io.mine.protocol.server.Server;
import io.mine.protocol.server.sync.SyncServer;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
@RunWith(Parameterized.class)
public class RpcTest {

    @Parameters(name = "{index}: port={0}, protocol={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[] { 9999, DataProtocols.V0, "World" },
                new Object[] { 9999, DataProtocols.V0, "World2" }, new Object[] { 9998, DataProtocols.V0, "World" },
                new Object[] { 9998, DataProtocols.V1, "World" },
                new Object[] { 9998, DataProtocols.V1, Util.multiply("World", 128) });
    }

    @Parameter(0)
    public int port;

    @Parameter(1)
    public DataProtocol dataProtocol;

    @Parameter(2)
    public String name;

    private ThreadPool _serverThreadPool;
    private Server<SampleRequest, SampleResponse> _server;
    private Client<SampleRequest, SampleResponse> _client;

    @Before
    public void setUp() throws IOException, InterruptedException {
        InetSocketAddress serverAddress = new InetSocketAddress(port);
        ThreadPoolConfig threadPoolConfig = ThreadPools.newThreadPoolConfigBuilder().setMaxSize(1).setMinSize(1)
                .build();
        _serverThreadPool = ThreadPools.newThreadPool(threadPoolConfig);
        _serverThreadPool.submit(() -> {
            _server = newServer(serverAddress);
            try {
                _server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(1 * 1000);

        _client = newClient(serverAddress);
    }

    protected Server<SampleRequest, SampleResponse> newServer(InetSocketAddress serverAddress) {
        return new SyncServer<>(serverAddress, new SampleService());
    }

    protected Client<SampleRequest, SampleResponse> newClient(InetSocketAddress serverAddress)
            throws UnknownHostException, IOException {
        return new SyncClient<>(SampleRequest.class, SampleResponse.class, serverAddress, dataProtocol);
    }

    @After
    public void tearDown() throws Exception {
        if (_serverThreadPool != null)
            _serverThreadPool.close();

        if (_server != null) {
            _server.close();
        }

        if (_client != null)
            _client.close();

        Thread.sleep(1 * 1000);
    }

    @Test
    public void requestResponseTest() throws IOException, InterruptedException {
        long before = System.currentTimeMillis();
        SampleRequest request = new SampleRequest();
        request.setName(name);
        request.setTime(System.currentTimeMillis());
        System.out.println("request: " + request);
        SampleResponse response = _client.invoke(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getTime() >= request.getTime());
        System.out.println("response: " + response);
        long after = System.currentTimeMillis();
        System.out.println("request latency: " + (after - before));
    }

    @Test
    public void nRequestResponseTest() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++)
            requestResponseTest();
    }

}
