package io.mine.protocol.api;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class ServerRequest {

    private String name;
    private long time;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "ServerRequest [name=" + name + ", time=" + time + "]";
    }

}
