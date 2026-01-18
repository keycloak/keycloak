package org.keycloak.models.workflow;

import java.util.List;
import java.util.function.BiPredicate;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel.ClientCreationEvent;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityCreatedEvent;
import org.keycloak.models.FederatedIdentityModel.FederatedIdentityRemovedEvent;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupMemberJoinEvent;
import org.keycloak.models.GroupModel.GroupMemberLeaveEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleModel.RoleGrantedEvent;
import org.keycloak.models.RoleModel.RoleRevokedEvent;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderEvent;

import static org.keycloak.models.utils.KeycloakModelUtils.GROUP_PATH_SEPARATOR;

public enum ResourceOperationType {

    USER_CREATED(ResourceType.USERS, List.of(OperationType.CREATE, EventType.REGISTER)),
    USER_AUTHENTICATED(ResourceType.USERS,List.of(EventType.LOGIN), userLoginPredicate()),
    USER_FEDERATED_IDENTITY_ADDED(ResourceType.USERS,List.of(FederatedIdentityCreatedEvent.class), fedIdentityPredicate()),
    USER_FEDERATED_IDENTITY_REMOVED(ResourceType.USERS,List.of(FederatedIdentityRemovedEvent.class), fedIdentityPredicate()),
    USER_GROUP_MEMBERSHIP_ADDED(ResourceType.USERS,List.of(GroupMemberJoinEvent.class), groupMembershipPredicate()),
    USER_GROUP_MEMBERSHIP_REMOVED(ResourceType.USERS,List.of(GroupModel.GroupMemberLeaveEvent.class), groupMembershipPredicate()),
    USER_ROLE_GRANTED(ResourceType.USERS,List.of(RoleGrantedEvent.class), roleMembershipPredicate()),
    USER_ROLE_REVOKED(ResourceType.USERS,List.of(RoleModel.RoleRevokedEvent.class), roleMembershipPredicate()),

    CLIENT_ADDED(ResourceType.CLIENTS, List.of(OperationType.CREATE, ClientCreationEvent.class)),
    CLIENT_LOGGED_IN(ResourceType.CLIENTS, List.of(EventType.CLIENT_LOGIN)),

    AD_HOC(ResourceType.USERS, List.of(new Class[] {}));

    private final ResourceType resourceType;
    private final List<Object> eventTypes;
    private final List<Object> deactivationTypes;
    private final BiPredicate<WorkflowEvent, String> conditionPredicate;

    ResourceOperationType(ResourceType resourceType, List<Object> eventTypes) {
        this.resourceType = resourceType;
        this.eventTypes = eventTypes;
        this.deactivationTypes = List.of();
        this.conditionPredicate = defaultPredicate();
    }

    ResourceOperationType(ResourceType resourceType, List<Object> eventTypes, BiPredicate<WorkflowEvent, String> conditionPredicate) {
        this.resourceType = resourceType;
        this.eventTypes = eventTypes;
        this.deactivationTypes = List.of();
        this.conditionPredicate = defaultPredicate().and(conditionPredicate);
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public static ResourceOperationType toOperationType(Enum<?> from) {
        return toOperationType(null, from);
    }

    public static ResourceOperationType toOperationType(Class<?> from) {
        return toOperationType(null, from);
    }

    public static ResourceOperationType toOperationType(ResourceType resourceType, Object from) {
        for (ResourceOperationType value : values()) {
            if (resourceType != null && !resourceType.equals(value.resourceType)) {
                continue;
            }

            if (value.eventTypes.contains(from)) {
                return value;
            }
            for (Object type : value.eventTypes) {
                Class<?> fromClass = from instanceof Class ? (Class<?>) from : from.getClass();
                if (type instanceof Class<?> cls && cls.isAssignableFrom(fromClass)) {
                    return value;
                }
            }
        }

        return null;
    }

    public String getResourceId(ProviderEvent event) {
        if (event instanceof GroupMemberJoinEvent gme) {
            return gme.getUser().getId();
        }
        if (event instanceof GroupMemberLeaveEvent gme) {
            return gme.getUser().getId();
        }
        if (event instanceof FederatedIdentityModel.FederatedIdentityCreatedEvent fie) {
            return fie.getUser().getId();
        }
        if (event instanceof FederatedIdentityModel.FederatedIdentityRemovedEvent fie) {
            return fie.getUser().getId();
        }
        if (event instanceof RoleGrantedEvent rge) {
            return rge.getUser().getId();
        }
        if (event instanceof RoleRevokedEvent rre) {
            return rre.getUser().getId();
        }
        return null;
    }

    public boolean test(WorkflowEvent event, String detail) {
        return conditionPredicate.test(event, detail);
    }

    private BiPredicate<WorkflowEvent, String> defaultPredicate() {
        return (event, detail) -> event.getOperation().equals(this);
    }

    private static BiPredicate<WorkflowEvent, String> userLoginPredicate() {
            return (event, detail) -> {
                if (detail != null) {
                    Event loginEvent = (Event) event.getEvent();
                    return detail.equals(loginEvent.getClientId());
                } else {
                    return true;
                }
            };
    }

    private static BiPredicate<WorkflowEvent, String> groupMembershipPredicate() {
        return (event, groupName) -> {
            if (groupName != null) {
                if (!groupName.startsWith(GROUP_PATH_SEPARATOR))
                    groupName = GROUP_PATH_SEPARATOR + groupName;
                ProviderEvent groupEvent = (ProviderEvent) event.getEvent();
                if (groupEvent instanceof GroupMemberJoinEvent joinEvent) {
                    return groupName.equals(KeycloakModelUtils.buildGroupPath(joinEvent.getGroup()));
                } else if (groupEvent instanceof GroupModel.GroupMemberLeaveEvent leaveEvent) {
                    return groupName.equals(KeycloakModelUtils.buildGroupPath(leaveEvent.getGroup()));
                } else {
                    return false;
                }
            } else {
                return true;
            }
        };
    }

    private static BiPredicate<WorkflowEvent, String> roleMembershipPredicate() {
        return (event, roleName) -> {
            if (roleName != null) {
                ProviderEvent roleEvent = (ProviderEvent) event.getEvent();
                if (roleEvent instanceof RoleGrantedEvent roleGrantedEvent) {
                    return roleName.equals(roleGrantedEvent.getRole().getName());
                } else if (roleEvent instanceof RoleModel.RoleRevokedEvent roleRevokedEvent) {
                    return roleName.equals(roleRevokedEvent.getRole().getName());
                } else {
                    return false;
                }
            } else {
                return true;
            }
        };
    }

    private static BiPredicate<WorkflowEvent, String> fedIdentityPredicate() {
        return (event, idpAlias) -> {
            if (idpAlias != null) {
                ProviderEvent fedIdentityEvent = (ProviderEvent) event.getEvent();
                if (fedIdentityEvent instanceof FederatedIdentityModel.FederatedIdentityCreatedEvent fedIdentityCreatedEvent) {
                    return idpAlias.equals(fedIdentityCreatedEvent.getFederatedIdentity().getIdentityProvider());
                } else if (fedIdentityEvent instanceof FederatedIdentityRemovedEvent fedIdentityRemovedEvent) {
                    return idpAlias.equals(fedIdentityRemovedEvent.getFederatedIdentity().getIdentityProvider());
                } else {
                    return false;
                }
            } else {
                return true;
            }
        };
    }

}
