package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity"
)
public class HotRodAuthenticatorConfigEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public String alias;
    @ProtoField(number = 3)
    public Set<HotRodPair<String, String>> config;
    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticatorConfigEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticatorConfigEntityDelegate.entityHashCode(this);
    }
}
