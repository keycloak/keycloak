package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationSyncResult {

    private int added;
    private int updated;
    private int removed;

    public int getAdded() {
        return added;
    }

    public void setAdded(int added) {
        this.added = added;
    }

    public int getUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    public int getRemoved() {
        return removed;
    }

    public void setRemoved(int removed) {
        this.removed = removed;
    }

    public void increaseAdded() {
        added++;
    }

    public void increaseUpdated() {
        updated++;
    }

    public void increaseRemoved() {
        removed++;
    }

    public void add(UserFederationSyncResult other) {
        added += other.added;
        updated += other.updated;
        removed += other.removed;
    }

    public String getStatus() {
        return String.format("%d imported users, %d updated users, %d removed users", added, updated, removed);
    }

    @Override
    public String toString() {
        return String.format("UserFederationSyncResult [ %s ]", getStatus());
    }

    public static UserFederationSyncResult empty() {
        return new UserFederationSyncResult();
    }
}
