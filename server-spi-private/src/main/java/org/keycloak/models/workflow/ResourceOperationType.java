package org.keycloak.models.workflow;

import java.util.List;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityCreatedEvent;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityRemovedEvent;
import org.keycloak.models.GroupModel.GroupMemberJoinEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleModel.RoleGrantedEvent;
import org.keycloak.provider.ProviderEvent;

public enum ResourceOperationType {

    USER_ADD(OperationType.CREATE, EventType.REGISTER),
    USER_LOGIN(EventType.LOGIN),
    USER_FEDERATED_IDENTITY_ADD(FederatedIdentityCreatedEvent.class),
    USER_FEDERATED_IDENTITY_REMOVE(FederatedIdentityRemovedEvent.class),
    USER_GROUP_MEMBERSHIP_ADD(GroupMemberJoinEvent.class),
    USER_ROLE_ADD(RoleGrantedEvent.class),
    AD_HOC(new Class[] {});

    private final List<Object> types;
    private final List<Object> deactivationTypes;

    ResourceOperationType(Enum<?>... types) {
        this.types = List.of(types);
        this.deactivationTypes = List.of();
    }

    @SafeVarargs
    ResourceOperationType(Class<? extends ProviderEvent>... types) {
        this.types = List.of(types);
        this.deactivationTypes = List.of();
    }

    ResourceOperationType(Class<? extends ProviderEvent>[] types, Class<? extends ProviderEvent>[] deactivationTypes) {
        this.types = List.of(types);
        this.deactivationTypes = List.of(deactivationTypes);
    }

    public static ResourceOperationType toOperationType(Enum<?> from) {
        return toOperationType((Object) from);
    }

    public static ResourceOperationType toOperationType(Class<?> from) {
        return toOperationType((Object) from);
    }

    private static ResourceOperationType toOperationType(Object from) {
        for (ResourceOperationType value : values()) {
            if (value.types.contains(from)) {
                return value;
            }
            for (Object type : value.types) {
                if (type instanceof Class<?> cls && cls.isAssignableFrom((Class<?>) from)) {
                    return value;
                }
            }
        }

        return null;
    }

    public String getResourceId(ProviderEvent event) {
        if (event instanceof  GroupMemberJoinEvent gme) {
            return gme.getUser().getId();
        }
        if (event instanceof FederatedIdentityModel.FederatedIdentityCreatedEvent fie) {
            return fie.getUser().getId();
        }
        if (event instanceof FederatedIdentityModel.FederatedIdentityRemovedEvent fie) {
            return fie.getUser().getId();
        }
        if (event instanceof RoleModel.RoleGrantedEvent rge) {
            return rge.getUser().getId();
        }
        if (event instanceof RoleModel.RoleRevokedEvent rre) {
            return rre.getUser().getId();
        }
        return null;
    }

    public boolean isDeactivationEvent(Class<?> eventType) {
        for (Object deactivationType : deactivationTypes) {
            if (deactivationType instanceof Class<?> cls && cls.isAssignableFrom(eventType)) {
                return true;
            }
        }
        return false;
    }
}
