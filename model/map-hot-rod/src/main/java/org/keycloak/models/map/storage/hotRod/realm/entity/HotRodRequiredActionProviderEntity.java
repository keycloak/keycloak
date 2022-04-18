package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity"
)
public class HotRodRequiredActionProviderEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public String name;
    @ProtoField(number = 3)
    public Boolean defaultAction;
    @ProtoField(number = 4)
    public Boolean enabled;
    @ProtoField(number = 5)
    public Integer priority;
    @ProtoField(number = 6)
    public String alias;
    @ProtoField(number = 7)
    public String providerId;
    @ProtoField(number = 8)
    public Set<HotRodPair<String, String>> config;
    @Override
    public boolean equals(Object o) {
        return HotRodRequiredActionProviderEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodRequiredActionProviderEntityDelegate.entityHashCode(this);
    }
}
