package org.keycloak.models.sessions.infinispan.entities;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ClientInitialAccessEntity extends SessionEntity {

    private int timestamp;

    private int expires;

    private int count;

    private int remainingCount;

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getExpiration() {
        return expires;
    }

    public void setExpiration(int expires) {
        this.expires = expires;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }

}
