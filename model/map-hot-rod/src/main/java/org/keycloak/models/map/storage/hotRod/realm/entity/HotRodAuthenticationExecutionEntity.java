package org.keycloak.models.map.storage.hotRod.realm.entity;

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity"
)
public class HotRodAuthenticationExecutionEntity extends AbstractHotRodEntity {
    @ProtoField(number = 1)
    public String id;
    @ProtoField(number = 2)
    public Boolean autheticatorFlow;
    @ProtoField(number = 3)
    public Integer priority;
    @ProtoField(number = 4)
    public Integer requirement;
    @ProtoField(number = 5)
    public String authenticator;
    @ProtoField(number = 6)
    public String authenticatorConfig;
    @ProtoField(number = 7)
    public String flowId;
    @ProtoField(number = 8)
    public String parentFlowId;
    @Override
    public boolean equals(Object o) {
        return HotRodAuthenticationExecutionEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodAuthenticationExecutionEntityDelegate.entityHashCode(this);
    }
}
