package io.mine.protocol.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mydotey.objectpool.facade.ThreadPools;
import org.mydotey.objectpool.threadpool.ThreadPool;
import org.mydotey.objectpool.threadpool.autoscale.AutoScaleThreadPoolConfig;

import io.mine.protocol.api.Service;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public abstract class AbstractServer<Req, Res> implements Server<Req, Res> {

    private Service<Req, Res> _service;

    private InetSocketAddress _socketAddress;

    private AtomicBoolean _started;
    private ThreadPool _workerPool;

    public AbstractServer(InetSocketAddress socketAddress, Service<Req, Res> service) {
        Objects.requireNonNull(socketAddress, "socketAddress is null");
        Objects.requireNonNull(service, "service is null");

        _socketAddress = socketAddress;
        _service = service;

        _started = new AtomicBoolean();
        _workerPool = newThreadPool();
    }

    protected ThreadPool newThreadPool() {
        AutoScaleThreadPoolConfig threadPoolConfig = ThreadPools.newAutoScaleThreadPoolConfigBuilder()
                .setCheckInterval(10).setMaxIdleTime(10 * 1000).setMaxSize(100).setMinSize(10).setQueueCapacity(10)
                .setScaleFactor(10).build();
        return ThreadPools.newAutoScaleThreadPool(threadPoolConfig);
    }

    protected ThreadPool getWorkerPool() {
        return _workerPool;
    }

    @Override
    public Service<Req, Res> getService() {
        return _service;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return _socketAddress;
    }

    @Override
    public boolean isStarted() {
        return _started.get();
    }

    @Override
    public void start() throws IOException, InterruptedException {
        if (!_started.compareAndSet(false, true))
            return;

        doStart();
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        if (!_started.compareAndSet(true, false))
            return;

        doStop();
    }

    protected abstract void doStart() throws IOException, InterruptedException;

    protected abstract void doStop() throws IOException, InterruptedException;

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            _workerPool.close();
        }
    }

}
