package org.keycloak.models.policy;

import java.util.List;

import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.GroupModel.GroupMemberJoinEvent;
import org.keycloak.provider.ProviderEvent;

public enum ResourceOperationType {

    CREATE(OperationType.CREATE, EventType.REGISTER),
    LOGIN(EventType.LOGIN),
    ADD_FEDERATED_IDENTITY,
    REMOVE_FEDERATED_IDENTITY,
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
        return null;
    }
}
