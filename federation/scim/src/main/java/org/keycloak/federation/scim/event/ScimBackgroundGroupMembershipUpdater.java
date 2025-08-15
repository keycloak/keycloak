package org.keycloak.federation.scim.event;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;
import org.keycloak.federation.scim.core.ScimDispatcher;

import java.time.Duration;

/**
 * In charge of making background checks and sent UPDATE requests from group for which membership information has changed.
 * <p>
 * This is required to avoid immediate group membership updates which could cause to incorrect group members list in case of
 * concurrent group membership changes.
 */
public class ScimBackgroundGroupMembershipUpdater {
    public static final String GROUP_DIRTY_SINCE_ATTRIBUTE_NAME = "scim-dirty-since";

    private static final Logger LOGGER = Logger.getLogger(ScimBackgroundGroupMembershipUpdater.class);
    // Update check loop will run every time this delay has passed
    private static final long UPDATE_CHECK_DELAY_MS = 2000;
    // If a group is marked dirty since less that this debounce delay, wait for the next update check loop
    private static final long DEBOUNCE_DELAY_MS = 1200;
    private final KeycloakSessionFactory sessionFactory;

    public ScimBackgroundGroupMembershipUpdater(KeycloakSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void startBackgroundUpdates() {
        // Every UPDATE_CHECK_DELAY_MS, check for dirty groups and send updates if required
        try (KeycloakSession keycloakSession = sessionFactory.create()) {
            TimerProvider timer = keycloakSession.getProvider(TimerProvider.class);
            timer.scheduleTask(taskSession -> {
                for (RealmModel realm : taskSession.realms().getRealmsStream().toList()) {
                    dispatchDirtyGroupsUpdates(realm);
                }
            }, Duration.ofMillis(UPDATE_CHECK_DELAY_MS).toMillis(), "scim-background");
        }
    }

    private void dispatchDirtyGroupsUpdates(RealmModel realm) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, session -> {
            session.getContext().setRealm(realm);
            ScimDispatcher dispatcher = new ScimDispatcher(session);
            // Identify groups marked as dirty by the ScimEventListenerProvider
            for (GroupModel group : session.groups().getGroupsStream(realm).filter(this::isDirtyGroup).toList()) {
                LOGGER.infof("[SCIM] Group %s is dirty, dispatch an update", group.getName());
                // If dirty : dispatch a group update to all clients and mark it clean
                dispatcher.dispatchGroupModificationToAll(client -> client.update(group));
                group.removeAttribute(GROUP_DIRTY_SINCE_ATTRIBUTE_NAME);
            }
            dispatcher.close();
        });
    }

    private boolean isDirtyGroup(GroupModel g) {
        String groupDirtySinceAttribute = g.getFirstAttribute(GROUP_DIRTY_SINCE_ATTRIBUTE_NAME);
        try {
            long groupDirtySince = Long.parseLong(groupDirtySinceAttribute);
            // Must be dirty for more than DEBOUNCE_DELAY_MS
            // (otherwise update will be dispatched in next scheduled loop)
            return System.currentTimeMillis() - groupDirtySince > DEBOUNCE_DELAY_MS;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
