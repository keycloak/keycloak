package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity"
)
public class HotRodIdentityProviderMapperEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public String name;
    @ProtoField(number = 3)
    public String identityProviderAlias;
    @ProtoField(number = 4)
    public String identityProviderMapper;
    @ProtoField(number = 5)
    public Set<HotRodPair<String, String>> config;
    @Override
    public boolean equals(Object o) {
        return HotRodIdentityProviderMapperEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodIdentityProviderMapperEntityDelegate.entityHashCode(this);
    }
}
