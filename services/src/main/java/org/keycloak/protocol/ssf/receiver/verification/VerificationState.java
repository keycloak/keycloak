package org.keycloak.protocol.ssf.receiver.verification;

public class VerificationState {

    protected String streamId;

    protected String state;

    protected long timestamp;

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "VerificationState{" +
               "streamId='" + streamId + '\'' +
               ", state='" + state + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}
