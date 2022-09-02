package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodAttributeEntityNonIndexed;

import java.util.Set;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapComponentEntity"
)
@ProtoDoc("@Indexed")
public class HotRodComponentEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public String name;
    @ProtoField(number = 3)
    public String parentId;
    @ProtoField(number = 4)
    public String providerId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 5)
    public String providerType;

    @ProtoField(number = 6)
    public String subType;
    @ProtoField(number = 7)
    public Set<HotRodAttributeEntityNonIndexed> config;
    @Override
    public boolean equals(Object o) {
        return HotRodComponentEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodComponentEntityDelegate.entityHashCode(this);
    }
}
