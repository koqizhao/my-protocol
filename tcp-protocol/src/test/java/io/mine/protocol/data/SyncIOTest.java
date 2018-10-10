package io.mine.protocol.data;

import java.io.IOException;
import java.net.InetSocketAddress;
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

import io.mine.protocol.api.ServerRequest;
import io.mine.protocol.api.ServerResponse;
import io.mine.protocol.client.SyncClient;
import io.mine.protocol.server.SyncServer;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
@RunWith(Parameterized.class)
public class SyncIOTest {

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
    private SyncServer _server;

    @Before
    public void setUp() throws IOException, InterruptedException {
        ThreadPoolConfig threadPoolConfig = ThreadPools.newThreadPoolConfigBuilder().setMaxSize(1).setMinSize(1)
                .build();
        _serverThreadPool = ThreadPools.newThreadPool(threadPoolConfig);
        _serverThreadPool.submit(() -> {
            _server = new SyncServer(port);
            try {
                _server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(1 * 1000);
    }

    @After
    public void tearDown() throws IOException {
        if (_serverThreadPool != null)
            _serverThreadPool.close();

        if (_server != null)
            _server.close();
    }

    @Test
    public void requestResponseTest() throws IOException, InterruptedException {
        try (SyncClient client = new SyncClient(new InetSocketAddress(port), dataProtocol)) {
            client.connect();
            ServerRequest request = new ServerRequest();
            request.setName(name);
            request.setTime(System.currentTimeMillis());
            System.out.println("request: " + request);
            ServerResponse response = client.send(request);
            Assert.assertNotNull(response);
            Assert.assertTrue(response.getTime() >= request.getTime());
            System.out.println("response: " + response);
        }
    }

    @Test
    public void nRequestResponseTest() throws IOException, InterruptedException {
        for (int i = 0; i < 10; i++)
            requestResponseTest();
    }

}
