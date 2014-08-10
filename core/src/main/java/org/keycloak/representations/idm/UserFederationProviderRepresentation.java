package org.keycloak.representations.idm;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserFederationProviderRepresentation {

    private String id;
    private String displayName;
    private String providerName;
    private Map<String, String> config;
    private int priority;
    private int fullSyncPeriod;
    private int changedSyncPeriod;
    private int lastSync;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }


    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getFullSyncPeriod() {
        return fullSyncPeriod;
    }

    public void setFullSyncPeriod(int fullSyncPeriod) {
        this.fullSyncPeriod = fullSyncPeriod;
    }

    public int getChangedSyncPeriod() {
        return changedSyncPeriod;
    }

    public void setChangedSyncPeriod(int changedSyncPeriod) {
        this.changedSyncPeriod = changedSyncPeriod;
    }

    public int getLastSync() {
        return lastSync;
    }

    public void setLastSync(int lastSync) {
        this.lastSync = lastSync;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserFederationProviderRepresentation that = (UserFederationProviderRepresentation) o;

        if (!id.equals(that.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
