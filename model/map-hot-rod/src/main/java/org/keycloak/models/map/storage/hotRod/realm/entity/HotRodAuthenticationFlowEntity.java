package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity"
)
public class HotRodAuthenticationFlowEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public Boolean builtIn;
    @ProtoField(number = 3)
    public Boolean topLevel;
    @ProtoField(number = 4)
    public String alias;
    @ProtoField(number = 5)
    public String description;
    @ProtoField(number = 6)
    public String providerId;
    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticationFlowEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticationFlowEntityDelegate.entityHashCode(this);
    }
}
