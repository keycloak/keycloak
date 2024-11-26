package org.keycloak.federation.scim.event;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.federation.scim.core.ScimDispatcher;
import org.keycloak.federation.scim.core.ScimEndpointConfigurationStorageProviderFactory;
import org.keycloak.federation.scim.core.service.KeycloakDao;
import org.keycloak.federation.scim.core.service.KeycloakId;
import org.keycloak.federation.scim.core.service.ScimResourceType;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * An Event listener reacting to Keycloak models modification (e.g. User creation, Group deletion, membership modifications,
 * endpoint configuration change...) by propagating it to all registered Scim endpoints.
 */
public class ScimEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = Logger.getLogger(ScimEventListenerProvider.class);

    private final ScimDispatcher dispatcher;

    private final KeycloakSession session;

    private final KeycloakDao keycloakDao;

    private final Map<ResourceType, Pattern> listenedEventPathPatterns = Map.of(ResourceType.USER,
            Pattern.compile("users/(.+)"), ResourceType.GROUP, Pattern.compile("groups/([\\w-]+)(/children)?"),
            ResourceType.GROUP_MEMBERSHIP, Pattern.compile("users/(.+)/groups/(.+)"), ResourceType.REALM_ROLE_MAPPING,
            Pattern.compile("^(.+)/(.+)/role-mappings"), ResourceType.COMPONENT, Pattern.compile("components/(.+)"));

    public ScimEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.keycloakDao = new KeycloakDao(session);
        this.dispatcher = new ScimDispatcher(session);
    }

    @Override
    public void onEvent(Event event) {
        if (Profile.isFeatureEnabled(Feature.SCIM_FEDERATION)) {
            // React to User-related event : creation, deletion, update
            EventType eventType = event.getType();
            KeycloakId eventUserId = new KeycloakId(event.getUserId());
            switch (eventType) {
                case REGISTER -> {
                    LOGGER.infof("[SCIM] Propagate User Registration - %s", eventUserId);
                    UserModel user = getUser(eventUserId);
                    dispatcher.dispatchUserModificationToAll(client -> client.create(user));
                }
                case UPDATE_EMAIL, UPDATE_PROFILE -> {
                    LOGGER.infof("[SCIM] Propagate User %s - %s", eventType, eventUserId);
                    UserModel user = getUser(eventUserId);
                    dispatcher.dispatchUserModificationToAll(client -> client.update(user));
                }
                case DELETE_ACCOUNT -> {
                    LOGGER.infof("[SCIM] Propagate User deletion - %s", eventUserId);
                    dispatcher.dispatchUserModificationToAll(client -> client.delete(event.getDetails().get("SCIM_ID")));
                }
                default -> {
                    // No other event has to be propagated to Scim endpoints
                }
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        if (Profile.isFeatureEnabled(Feature.SCIM_FEDERATION)) {
            // Step 1: check if event is relevant for propagation through SCIM
            Pattern pattern = listenedEventPathPatterns.get(event.getResourceType());
            if (pattern == null)
                return;
            Matcher matcher = pattern.matcher(event.getResourcePath());
            if (!matcher.find())
                return;

            // Step 2: propagate event (if needed) according to its resource type
            switch (event.getResourceType()) {
                case USER -> {
                    KeycloakId userId = new KeycloakId(matcher.group(1));
                    handleUserEvent(event, userId);
                }
                case GROUP -> {
                    KeycloakId groupId = new KeycloakId(matcher.group(1));
                    handleGroupEvent(event, groupId);
                }
                case GROUP_MEMBERSHIP -> {
                    KeycloakId userId = new KeycloakId(matcher.group(1));
                    KeycloakId groupId = new KeycloakId(matcher.group(2));
                    handleGroupMemberShipEvent(event, userId, groupId);
                }
                case REALM_ROLE_MAPPING -> {
                    String rawResourceType = matcher.group(1);
                    ScimResourceType type = switch (rawResourceType) {
                        case "users" -> ScimResourceType.USER;
                        case "groups" -> ScimResourceType.GROUP;
                        default -> throw new IllegalArgumentException("Unsupported resource type: " + rawResourceType);
                    };
                    KeycloakId id = new KeycloakId(matcher.group(2));
                    handleRoleMappingEvent(event, type, id);
                }
                case COMPONENT -> {
                    String id = matcher.group(1);
                    handleScimEndpointConfigurationEvent(event, id);

                }
                default -> {
                    // No other resource modification has to be propagated to Scim endpoints
                }
            }
        }
    }

    private void handleUserEvent(AdminEvent userEvent, KeycloakId userId) {
        LOGGER.infof("[SCIM] Propagate User %s - %s", userEvent.getOperationType(), userId);
        switch (userEvent.getOperationType()) {
            case CREATE -> {
                UserModel user = getUser(userId);
                dispatcher.dispatchUserModificationToAll(client -> client.create(user));
                user.getGroupsStream()
                        .forEach(group -> dispatcher.dispatchGroupModificationToAll(client -> client.update(group)));
            }
            case UPDATE -> {
                UserModel user = getUser(userId);
                dispatcher.dispatchUserModificationToAll(client -> client.update(user));
            }
            case DELETE -> dispatcher.dispatchUserModificationToAll(client -> client.delete(userEvent.getDetails().get("SCIM_ID")));
            default -> {
                // ACTION userEvent are not relevant, nothing to do
            }
        }
    }

    /**
     * Propagating the given group-related event to Scim endpoints.
     *
     * @param event the event to propagate
     * @param groupId event target's id
     */
    private void handleGroupEvent(AdminEvent event, KeycloakId groupId) {
        LOGGER.infof("[SCIM] Propagate Group %s - %s", event.getOperationType(), groupId);
        switch (event.getOperationType()) {
            case CREATE -> {
                GroupModel group = getGroup(groupId);
                dispatcher.dispatchGroupModificationToAll(client -> client.create(group));
            }
            case UPDATE -> {
                GroupModel group = getGroup(groupId);
                dispatcher.dispatchGroupModificationToAll(client -> client.update(group));
            }
            case DELETE -> dispatcher.dispatchGroupModificationToAll(client -> client.delete(event.getDetails().get("SCIM_ID")));
            default -> {
                // ACTION event are not relevant, nothing to do
            }
        }
    }

    private void handleGroupMemberShipEvent(AdminEvent groupMemberShipEvent, KeycloakId userId, KeycloakId groupId) {
        LOGGER.infof("[SCIM] Propagate GroupMemberShip %s - User %s Group %s", groupMemberShipEvent.getOperationType(), userId,
                groupId);
        // Step 1: update USER immediately
        GroupModel group = getGroup(groupId);
        UserModel user = getUser(userId);
        dispatcher.dispatchUserModificationToAll(client -> client.update(user));

        // Step 2: delayed GROUP update :
        // if several users are added to the group simultaneously in different Keycloack sessions
        // update the group in the context of the current session may not reflect those other changes
        // We trigger a delayed update by setting an attribute on the group (that will be handled by
        // ScimBackgroundGroupMembershipUpdaters)
        group.setSingleAttribute(ScimBackgroundGroupMembershipUpdater.GROUP_DIRTY_SINCE_ATTRIBUTE_NAME,
                "" + System.currentTimeMillis());
    }

    private void handleRoleMappingEvent(AdminEvent roleMappingEvent, ScimResourceType type, KeycloakId id) {
        LOGGER.infof("[SCIM] Propagate RoleMapping %s - %s %s", roleMappingEvent.getOperationType(), type, id);
        switch (type) {
            case USER -> {
                UserModel user = getUser(id);
                dispatcher.dispatchUserModificationToAll(client -> client.update(user));
            }
            case GROUP -> {
                GroupModel group = getGroup(id);
                session.users().getGroupMembersStream(session.getContext().getRealm(), group)
                        .forEach(user -> dispatcher.dispatchUserModificationToAll(client -> client.update(user)));
            }
            default -> {
                // No other type is relevant for propagation
            }
        }
    }

    private void handleScimEndpointConfigurationEvent(AdminEvent event, String id) {
        // In case of a component deletion
        if (event.getOperationType() == OperationType.DELETE) {
            // Check if it was a Scim endpoint configuration, and forward deletion if so
            Stream<ComponentModel> scimEndpointConfigurationsWithDeletedId = session.getContext().getRealm()
                    .getComponentsStream()
                    .filter(m -> ScimEndpointConfigurationStorageProviderFactory.ID.equals(m.getProviderId())
                            && id.equals(m.getId()));
            if (scimEndpointConfigurationsWithDeletedId.iterator().hasNext()) {
                LOGGER.infof("[SCIM] SCIM Endpoint configuration DELETE - %s ", id);
                dispatcher.refreshActiveScimEndpoints();
            }
        } else {
            // In case of CREATE or UPDATE, we can directly use the string representation
            // to check if it defines a SCIM endpoint (faster)
            if (event.getRepresentation() != null && event.getRepresentation().contains("\"providerId\":\"scim\"")) {
                LOGGER.infof("[SCIM] SCIM Endpoint configuration CREATE - %s ", id);
                dispatcher.refreshActiveScimEndpoints();
            }
        }

    }

    private UserModel getUser(KeycloakId id) {
        return keycloakDao.getUserById(id);
    }

    private GroupModel getGroup(KeycloakId id) {
        return keycloakDao.getGroupById(id);
    }

    @Override
    public void close() {
        dispatcher.close();
    }

}
