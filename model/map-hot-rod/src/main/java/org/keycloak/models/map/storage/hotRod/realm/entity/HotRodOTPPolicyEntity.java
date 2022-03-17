package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapOTPPolicyEntity"
)
public class HotRodOTPPolicyEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public Integer otpPolicyDigits;
    @ProtoField(number = 2)
    public Integer otpPolicyInitialCounter;
    @ProtoField(number = 3)
    public Integer otpPolicyLookAheadWindow;
    @ProtoField(number = 4)
    public Integer otpPolicyPeriod;
    @ProtoField(number = 5)
    public String otpPolicyAlgorithm;
    @ProtoField(number = 6)
    public String otpPolicyType;
    @Override
    public boolean equals(Object o) {
        return HotRodOTPPolicyEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodOTPPolicyEntityDelegate.entityHashCode(this);
    }
}
