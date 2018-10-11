package io.mine.protocol.async;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class DefaultAsyncResponse implements AsyncResponse {

    private AtomicBoolean _isComplete;
    private byte[] _data;
    private List<Consumer<byte[]>> _completeListeners;

    public DefaultAsyncResponse() {
        _isComplete = new AtomicBoolean();
        _completeListeners = new ArrayList<>();
    }

    @Override
    public void complete(byte[] responseData) {
        if (!_isComplete.compareAndSet(false, true))
            throw new IllegalStateException("Invocation on completed response");

        _data = responseData;

        synchronized (_completeListeners) {
            for (Consumer<byte[]> listener : _completeListeners) {
                listener.accept(_data);
            }
        }
    }

    protected void addCompleteListener(Consumer<byte[]> listener) {
        Objects.requireNonNull(listener, "listener is null");

        synchronized (_completeListeners) {
            _completeListeners.add(listener);
        }
    }

}
