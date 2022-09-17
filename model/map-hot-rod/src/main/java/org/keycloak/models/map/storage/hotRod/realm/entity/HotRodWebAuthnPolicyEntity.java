package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

import java.util.List;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity"
)
public class HotRodWebAuthnPolicyEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public Boolean avoidSameAuthenticatorRegister;
    @ProtoField(number = 2)
    public Integer createTimeout;
    @ProtoField(number = 3)
    public String attestationConveyancePreference;
    @ProtoField(number = 4)
    public String authenticatorAttachment;
    @ProtoField(number = 5)
    public String requireResidentKey;
    @ProtoField(number = 6)
    public String rpEntityName;
    @ProtoField(number = 7)
    public String rpId;
    @ProtoField(number = 8)
    public String userVerificationRequirement;
    @ProtoField(number = 9)
    public List<String> acceptableAaguids;
    @ProtoField(number = 10)
    public List<String> signatureAlgorithms;
    @Override
    public boolean equals(Object o) {
        return HotRodWebAuthnPolicyEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodWebAuthnPolicyEntityDelegate.entityHashCode(this);
    }
}
