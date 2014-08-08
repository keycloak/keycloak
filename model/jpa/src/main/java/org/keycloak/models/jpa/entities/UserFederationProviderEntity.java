package org.keycloak.models.jpa.entities;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
@Entity
@Table(name="USER_FEDERATION_PROVIDER")
public class UserFederationProviderEntity {

    @Id
    @Column(name="ID", length = 36)
    protected String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROVIDER_NAME")
    private String providerName;
    @Column(name="PRIORITY")
    private int priority;

    @ElementCollection
    @MapKeyColumn(name="name")
    @Column(name="value")
    @CollectionTable(name="USER_FEDERATION_CONFIG", joinColumns={ @JoinColumn(name="USER_FEDERATION_PROVIDER_ID") })
    private Map<String, String> config;

    @Column(name="DISPLAY_NAME")
    private String displayName;

    @Column(name="FULL_SYNC_PERIOD")
    private int fullSyncPeriod;
    @Column(name="CHANGED_SYNC_PERIOD")
    private int changedSyncPeriod;
    @Column(name="LAST_SYNC")
    private int lastSync;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
}
