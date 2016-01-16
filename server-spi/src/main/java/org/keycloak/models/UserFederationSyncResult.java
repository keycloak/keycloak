package org.keycloak.models;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationSyncResult {

    private int added;
    private int updated;
    private int removed;
    private int failed;

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

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
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

    public void increaseFailed() {
        failed++;
    }

    public void add(UserFederationSyncResult other) {
        added += other.added;
        updated += other.updated;
        removed += other.removed;
        failed += other.failed;
    }

    public String getStatus() {
        String status = String.format("%d imported users, %d updated users, %d removed users", added, updated, removed);
        if (failed != 0) {
            status += String.format(", %d users failed sync! See server log for more details", failed);
        }
        return status;
    }

    @Override
    public String toString() {
        return String.format("UserFederationSyncResult [ %s ]", getStatus());
    }

    public static UserFederationSyncResult empty() {
        return new UserFederationSyncResult();
    }
}
