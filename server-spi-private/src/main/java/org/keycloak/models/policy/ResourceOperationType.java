package org.keycloak.models.policy;

import java.util.List;
import java.util.Map;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityCreatedEvent;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityRemovedEvent;
import org.keycloak.models.GroupModel.GroupMemberJoinEvent;
import org.keycloak.provider.ProviderEvent;

public enum ResourceOperationType {

    CREATE(OperationType.CREATE, EventType.REGISTER),
    LOGIN(EventType.LOGIN),
    ADD_FEDERATED_IDENTITY(FederatedIdentityCreatedEvent.class),
    REMOVE_FEDERATED_IDENTITY(FederatedIdentityRemovedEvent.class),
    GROUP_MEMBERSHIP_JOIN(GroupMemberJoinEvent.class);

    private final List<Object> types;

    ResourceOperationType(Enum<?>... types) {
        this.types = List.of(types);
    }

    @SafeVarargs
    ResourceOperationType(Class<? extends ProviderEvent>... types) {
        this.types = List.of(types);
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
//                    factory.register(fired -> {
//                        ResourcePolicyEvent rpe = null;
//                        if (fired instanceof FederatedIdentityModel.FederatedIdentityCreatedEvent event) {
//                            rpe = new ResourcePolicyEvent(ResourceType.USERS, ResourceOperationType.ADD_FEDERATED_IDENTITY,
//                                    event.getUser().getId(), Map.of("provider", event.getFederatedIdentity().getIdentityProvider()));
//                            ResourcePolicyManager manager = new ResourcePolicyManager(event.getKeycloakSession());
//                            manager.processEvent(rpe);
//                        } else if (fired instanceof FederatedIdentityModel.FederatedIdentityRemovedEvent event) {
//                            rpe =  new ResourcePolicyEvent(ResourceType.USERS, ResourceOperationType.REMOVE_FEDERATED_IDENTITY,
//                                    event.getUser().getId(), Map.of("provider", event.getFederatedIdentity().getIdentityProvider()));
//                            ResourcePolicyManager manager = new ResourcePolicyManager(event.getKeycloakSession());
//                            manager.processEvent(rpe);
//                        }
//                    });
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
        return null;
    }
}
