package org.keycloak.federation.scim.event;

import static org.keycloak.federation.scim.core.service.AbstractScimService.SCIM_ID;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.federation.scim.core.ScimDispatcher;
import org.keycloak.federation.scim.core.ScimUserStorageProviderFactory;
import org.keycloak.federation.scim.core.service.ScimResourceType;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;

/**
 * An Event listener reacting to Keycloak models modification (e.g. User creation, Group deletion, membership modifications,
 * endpoint configuration change...) by propagating it to all registered Scim endpoints.
 */
public class ScimEventListenerProvider implements EventListenerProvider {

    private static final Logger LOGGER = Logger.getLogger(ScimEventListenerProvider.class);

    private final ScimDispatcher dispatcher;

    private final KeycloakSession session;

    private final Map<ResourceType, Pattern> listenedEventPathPatterns = Map.of(ResourceType.USER,
            Pattern.compile("users/(.+)"), ResourceType.GROUP, Pattern.compile("groups/([\\w-]+)(/children)?"),
            ResourceType.GROUP_MEMBERSHIP, Pattern.compile("users/(.+)/groups/(.+)"), ResourceType.REALM_ROLE_MAPPING,
            Pattern.compile("^(.+)/(.+)/role-mappings"), ResourceType.COMPONENT, Pattern.compile("components/(.+)"));

    public ScimEventListenerProvider(KeycloakSession session) {
        this.session = session;
        this.dispatcher = new ScimDispatcher(session);
    }

    @Override
    public void onEvent(Event event) {
        /** @see org.keycloak.federation.scim.core.ScimUserStorageProvider **/
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
                case GROUP -> {
                    String groupId = matcher.group(1);
                    handleGroupEvent(event, groupId);
                }
                case GROUP_MEMBERSHIP -> {
                    String userId = matcher.group(1);
                    String groupId = matcher.group(2);
                    handleGroupMemberShipEvent(event, userId, groupId);
                }
                case REALM_ROLE_MAPPING -> {
                    String rawResourceType = matcher.group(1);
                    ScimResourceType type = switch (rawResourceType) {
                        case "users" -> ScimResourceType.USER;
                        case "groups" -> ScimResourceType.GROUP;
                        default -> throw new IllegalArgumentException("Unsupported resource type: " + rawResourceType);
                    };
                    String id = matcher.group(2);
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

    /**
     * Propagating the given group-related event to Scim endpoints.
     *
     * @param event the event to propagate
     * @param groupId event target's id
     */
    private void handleGroupEvent(AdminEvent event, String groupId) {
        LOGGER.infof("[SCIM] Propagate Group %s - %s", event.getOperationType(), groupId);
        switch (event.getOperationType()) {
            case CREATE -> {
                GroupModel group = session.groups().getGroupById(session.getContext().getRealm(), groupId);
                dispatcher.dispatchGroupModificationToAll(client -> client.create(group));
            }
            case UPDATE -> {
                GroupModel group = session.groups().getGroupById(session.getContext().getRealm(), groupId);
                dispatcher.dispatchGroupModificationToAll(client -> client.update(group));
            }
            case DELETE -> dispatcher.dispatchGroupModificationToAll(client -> client.delete(event.getDetails().get(SCIM_ID)));
            default -> {
                // ACTION event are not relevant, nothing to do
            }
        }
    }

    private void handleGroupMemberShipEvent(AdminEvent groupMemberShipEvent, String userId, String groupId) {
        LOGGER.infof("[SCIM] Propagate GroupMemberShip %s - User %s Group %s", groupMemberShipEvent.getOperationType(), userId,
                groupId);
        // Step 1: update USER immediately
        GroupModel group = session.groups().getGroupById(session.getContext().getRealm(), groupId);
        UserModel user = session.users().getUserById(session.getContext().getRealm(), userId);
        dispatcher.dispatchUserModificationToAll(client -> client.update(user));

        // Step 2: delayed GROUP update :
        // if several users are added to the group simultaneously in different Keycloack sessions
        // update the group in the context of the current session may not reflect those other changes
        // We trigger a delayed update by setting an attribute on the group (that will be handled by
        // ScimBackgroundGroupMembershipUpdaters)
        group.setSingleAttribute(ScimBackgroundGroupMembershipUpdater.GROUP_DIRTY_SINCE_ATTRIBUTE_NAME,
                "" + System.currentTimeMillis());
    }

    private void handleRoleMappingEvent(AdminEvent roleMappingEvent, ScimResourceType type, String id) {
        LOGGER.infof("[SCIM] Propagate RoleMapping %s - %s %s", roleMappingEvent.getOperationType(), type, id);
        switch (type) {
            case USER -> {
                UserModel user = session.users().getUserById(session.getContext().getRealm(), id);
                dispatcher.dispatchUserModificationToAll(client -> client.update(user));
            }
            case GROUP -> {
                GroupModel group = session.groups().getGroupById(session.getContext().getRealm(), id);
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
                    .filter(m -> ScimUserStorageProviderFactory.ID.equals(m.getProviderId())
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

    @Override
    public void close() {
        dispatcher.close();
    }

}
