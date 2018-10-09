package io.mine.protocol.data;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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
public class SyncIOTest {

    private int _port = 9999;
    private ThreadPool _serverThreadPool;
    private SyncServer _server;
    private SyncClient _client;

    @Before
    public void setUp() throws IOException, InterruptedException {
        ThreadPoolConfig threadPoolConfig = ThreadPools.newThreadPoolConfigBuilder().setMaxSize(1).setMinSize(1)
                .build();
        _serverThreadPool = ThreadPools.newThreadPool(threadPoolConfig);
        _serverThreadPool.submit(() -> {
            _server = new SyncServer(_port);
            try {
                _server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(1 * 1000);
        _client = new SyncClient(new InetSocketAddress(_port));
        _client.connect();
    }

    @After
    public void tearDown() throws IOException {
        if (_serverThreadPool != null)
            _serverThreadPool.close();

        if (_server != null)
            _server.close();

        if (_client != null)
            _client.close();
    }

    @Test
    public void requestResponseTest() throws IOException {
        ServerRequest request = new ServerRequest();
        request.setName("World");
        request.setTime(System.currentTimeMillis());
        System.out.println("request: " + request);
        ServerResponse response = _client.send(request);
        Assert.assertNotNull(response);
        Assert.assertTrue(response.getTime() >= request.getTime());
        System.out.println("response: " + response);
    }

    @Test
    public void nRequestResponseTest() throws IOException {
        for (int i = 0; i < 10; i++)
            requestResponseTest();
    }

}
