package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity"
)
public class HotRodClientInitialAccessEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public Integer count;
    @ProtoField(number = 3)
    public Long expiration;
    @ProtoField(number = 4)
    public Integer remainingCount;
    @ProtoField(number = 5)
    public Long timestamp;
    @Override
    public boolean equals(Object o) {
        return HotRodClientInitialAccessEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodClientInitialAccessEntityDelegate.entityHashCode(this);
    }
}
