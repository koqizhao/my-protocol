package io.mine.protocol.api;

/**
 * @author koqizhao
 *
 * Oct 9, 2018
 */
public class ServerResponse {

    private String feedback;
    private long time;

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "ServerResponse [feedback=" + feedback + ", time=" + time + "]";
    }

}
