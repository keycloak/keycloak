package org.keycloak.storage;

import java.util.Objects;

import org.keycloak.cluster.ClusterEvent;

import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

// Send to cluster during each update or remove of federationProvider, so all nodes can update sync periods
@ProtoTypeId(65540)
public class UserStorageProviderClusterEvent implements ClusterEvent {

    private boolean removed;
    private String realmId;
    private UserStorageProviderModel storageProvider;

    @ProtoField(1)
    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    @ProtoField(2)
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @ProtoField(3)
    public UserStorageProviderModel getStorageProvider() {
        return storageProvider;
    }

    public void setStorageProvider(UserStorageProviderModel federationProvider) {
        this.storageProvider = federationProvider;
    }

    public static UserStorageProviderClusterEvent createEvent(boolean removed, String realmId, UserStorageProviderModel provider) {
        UserStorageProviderClusterEvent notification = new UserStorageProviderClusterEvent();
        notification.setRemoved(removed);
        notification.setRealmId(realmId);
        notification.setStorageProvider(provider);
        return notification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStorageProviderClusterEvent that = (UserStorageProviderClusterEvent) o;
        return removed == that.removed && Objects.equals(realmId, that.realmId) && Objects.equals(storageProvider.getId(), that.storageProvider.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(removed, realmId, storageProvider.getId());
    }
}
