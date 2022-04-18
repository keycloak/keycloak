package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapIdentityProviderEntity"
)
public class HotRodIdentityProviderEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public Boolean addReadTokenRoleOnCreate;
    @ProtoField(number = 3)
    public Boolean authenticateByDefault;
    @ProtoField(number = 4)
    public Boolean enabled;
    @ProtoField(number = 5)
    public Boolean linkOnly;
    @ProtoField(number = 6)
    public Boolean storeToken;
    @ProtoField(number = 7)
    public Boolean trustEmail;
    @ProtoField(number = 8)
    public String alias;
    @ProtoField(number = 9)
    public String displayName;
    @ProtoField(number = 10)
    public String firstBrokerLoginFlowId;
    @ProtoField(number = 11)
    public String postBrokerLoginFlowId;
    @ProtoField(number = 12)
    public String providerId;
    @ProtoField(number = 13)
    public Set<HotRodPair<String, String>> config;
    @Override
    public boolean equals(Object o) {
        return HotRodIdentityProviderEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodIdentityProviderEntityDelegate.entityHashCode(this);
    }
}
