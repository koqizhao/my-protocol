package io.mine.protocol.data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

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

import io.mine.protocol.api.Service;
import io.mine.protocol.api.sample.SampleAsyncService;
import io.mine.protocol.api.sample.SampleRequest;
import io.mine.protocol.api.sample.SampleResponse;
import io.mine.protocol.api.sample.SampleService;
import io.mine.protocol.async.AsyncClient;
import io.mine.protocol.async.AsyncServer;
import io.mine.protocol.async.NettyServer;
import io.mine.protocol.client.Client;
import io.mine.protocol.server.Server;
import io.mine.protocol.sync.SyncClient;
import io.mine.protocol.sync.SyncServer;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
@RunWith(Parameterized.class)
public class RpcTest {

    private static Supplier<Service<SampleRequest, SampleResponse>> _syncServiceFactory = new Supplier<Service<SampleRequest, SampleResponse>>() {
        @Override
        public Service<SampleRequest, SampleResponse> get() {
            return new SampleService();
        }

        @Override
        public String toString() {
            return "sync";
        }
    };

    private static Supplier<Service<SampleRequest, SampleResponse>> _asyncServiceFactory = new Supplier<Service<SampleRequest, SampleResponse>>() {
        @Override
        public Service<SampleRequest, SampleResponse> get() {
            return new SampleAsyncService();
        }

        @Override
        public String toString() {
            return "async";
        }
    };

    private static BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>> _syncServerFactory = new BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>>() {
        @Override
        public Server<SampleRequest, SampleResponse> apply(InetSocketAddress t,
                Service<SampleRequest, SampleResponse> u) {
            return new SyncServer<>(t, u);
        }

        @Override
        public String toString() {
            return "sync";
        }
    };

    private static BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>> _asyncServerFactory = new BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>>() {
        @Override
        public Server<SampleRequest, SampleResponse> apply(InetSocketAddress t,
                Service<SampleRequest, SampleResponse> u) {
            return new AsyncServer<>(t, u);
        }

        @Override
        public String toString() {
            return "async";
        }
    };

    private static BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>> _nettyServerFactory = new BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>>() {
        @Override
        public Server<SampleRequest, SampleResponse> apply(InetSocketAddress t,
                Service<SampleRequest, SampleResponse> u) {
            return new NettyServer<>(t, u);
        }

        @Override
        public String toString() {
            return "netty";
        }
    };

    private static BiFunction<InetSocketAddress, DataProtocol, Client<SampleRequest, SampleResponse>> _syncClientFactory = new BiFunction<InetSocketAddress, DataProtocol, Client<SampleRequest, SampleResponse>>() {
        @Override
        public Client<SampleRequest, SampleResponse> apply(InetSocketAddress t, DataProtocol u) {
            try {
                return new SyncClient<>(SampleRequest.class, SampleResponse.class, t, u);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "sync";
        }
    };

    private static BiFunction<InetSocketAddress, DataProtocol, Client<SampleRequest, SampleResponse>> _asyncClientFactory = new BiFunction<InetSocketAddress, DataProtocol, Client<SampleRequest, SampleResponse>>() {
        @Override
        public Client<SampleRequest, SampleResponse> apply(InetSocketAddress t, DataProtocol u) {
            try {
                return new AsyncClient<>(SampleRequest.class, SampleResponse.class, t, u);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return "async";
        }
    };

    @Parameters(name = "{index}: service={0}, server={1}, client={2}, port={3}, protocol={4}, name={5}")
    public static Collection<Object[]> data() {
        List<List<Object>> parameterValues = new ArrayList<>();
        parameterValues.add(Arrays.asList(_syncServiceFactory, _asyncServiceFactory));
        parameterValues.add(Arrays.asList(_syncServerFactory, _asyncServerFactory, _nettyServerFactory));
        parameterValues.add(Arrays.asList(_syncClientFactory, _asyncClientFactory));
        parameterValues.add(Arrays.asList(9999, 9998));
        parameterValues.add(new ArrayList<>(DataProtocols.ALL.values()));
        parameterValues.add(Arrays.asList("World", "World2", Util.multiply("World", 128)));
        return Util.generateParametersCombination(parameterValues);
    }

    //@Parameters(name = "{index}: service={0}, server={1}, client={2}, port={3}, protocol={4}, name={5}")
    public static Collection<Object[]> simpleData() {
        List<List<Object>> parameterValues = new ArrayList<>();
        parameterValues.add(Arrays.asList(_syncServiceFactory));
        parameterValues.add(Arrays.asList(_nettyServerFactory));
        parameterValues.add(Arrays.asList(_syncClientFactory));
        parameterValues.add(Arrays.asList(9999));
        parameterValues.add(Arrays.asList(DataProtocols.V0));
        parameterValues.add(Arrays.asList("World"));
        return Util.generateParametersCombination(parameterValues);
    }

    @Parameter(0)
    public Supplier<Service<SampleRequest, SampleResponse>> serviceFactory;

    @Parameter(1)
    public BiFunction<InetSocketAddress, Service<SampleRequest, SampleResponse>, Server<SampleRequest, SampleResponse>> serverFactory;

    @Parameter(2)
    public BiFunction<InetSocketAddress, DataProtocol, Client<SampleRequest, SampleResponse>> clientFactory;

    @Parameter(3)
    public int port;

    @Parameter(4)
    public DataProtocol dataProtocol;

    @Parameter(5)
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
            _server = serverFactory.apply(serverAddress, serviceFactory.get());
            try {
                _server.start();
            } catch (InterruptedException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        Thread.sleep(1 * 1000);

        _client = clientFactory.apply(serverAddress, dataProtocol);
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
