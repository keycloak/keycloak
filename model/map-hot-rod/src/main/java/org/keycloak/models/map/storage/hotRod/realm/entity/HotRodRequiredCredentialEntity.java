package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity"
)
public class HotRodRequiredCredentialEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public Boolean input;
    @ProtoField(number = 2)
    public Boolean secret;
    @ProtoField(number = 3)
    public String formLabel;
    @ProtoField(number = 4)
    public String type;
    @Override
    public boolean equals(Object o) {
        return HotRodRequiredCredentialEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodRequiredCredentialEntityDelegate.entityHashCode(this);
    }
}
