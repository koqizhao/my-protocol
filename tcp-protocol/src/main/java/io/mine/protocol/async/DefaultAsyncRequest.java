package io.mine.protocol.async;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author koqizhao
 *
 * Oct 11, 2018
 */
public class DefaultAsyncRequest implements AsyncRequest {

    private AtomicBoolean _isReady;
    private byte[] _data;

    private List<byte[]> _parts;
    private volatile byte[] _currentTrunk;
    private volatile int _currentTrunkRead;
    private volatile int _totalSize;

    public DefaultAsyncRequest() {
        _parts = new ArrayList<>();
        _isReady = new AtomicBoolean();
    }

    protected void setCurrentTrunk(byte[] bytes) {
        completeCurrentTrunk();
        _currentTrunk = bytes;

        _parts.add(bytes);
        _totalSize += bytes.length;
    }

    protected byte[] getCurrentTrunk() {
        return _currentTrunk;
    }

    protected int getCurrentTrunkRead() {
        return _currentTrunkRead;
    }

    protected int getCurrentTrunkRemaining() {
        return _currentTrunk.length - _currentTrunkRead;
    }

    protected void addCurrentTrunkRead(int read) {
        _currentTrunkRead += read;
    }

    protected void completeCurrentTrunk() {
        _currentTrunk = null;
        _currentTrunkRead = 0;
    }

    protected boolean needNextTrunk() {
        return _currentTrunk == null || _currentTrunk.length == _currentTrunkRead;
    }

    protected void ready() {
        if (!_isReady.compareAndSet(false, true))
            throw new IllegalStateException("Invocation on ready request.");

        if (_totalSize == 0)
            return;

        _data = new byte[_totalSize];
        for (int i = 0, offset = 0; i < _parts.size(); i++) {
            byte[] part = _parts.get(i);
            System.arraycopy(part, 0, _data, offset, part.length);
            offset += part.length;
        }

        _parts.clear();
        _currentTrunk = null;
    }

    @Override
    public byte[] getData() {
        return _data;
    }

}
