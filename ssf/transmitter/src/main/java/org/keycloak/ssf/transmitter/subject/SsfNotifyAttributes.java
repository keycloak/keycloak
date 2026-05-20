package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.organization.OrganizationProvider;

/**
 * Helpers for reading and writing the {@code ssf.notify.<clientId>}
 * user / organization attribute that controls per-stream subject
 * subscription filtering.
 */
public final class SsfNotifyAttributes {

    public static final String ATTRIBUTE_PREFIX = "ssf.notify.";
    /**
     * Tombstone attribute prefix written when a subject is removed
     * from a stream. Stores the epoch-second at which the removal
     * happened so the dispatcher can keep delivering events for the
     * spec-recommended grace window (SSF 1.0 §9.3 — "Malicious Subject
     * Removal" defense). Cleared on re-add or explicit exclude.
     */
    public static final String REMOVED_AT_PREFIX = "ssf.notifyRemovedAt.";

    /**
     * Granularity (seconds) for the stamp-on-remove writes. Mirrors
     * {@link org.keycloak.ssf.transmitter.support.SsfActivityTracker#STAMP_GRANULARITY_SECONDS}
     * — re-removing the same subject inside the slot is treated as
     * the same removal event so we don't trash the user / organization
     * cache with redundant attribute writes. Grace-window accuracy
     * therefore degrades by up to {@value} seconds, negligible given
     * the configured grace is typically measured in hours.
     */
    public static final long STAMP_GRANULARITY_SECONDS = 300L;

    public static final String ATTRIBUTE_VALUE_TRUE = "true";

    public static final String ATTRIBUTE_VALUE_FALSE = "false";

    private SsfNotifyAttributes() {}

    public static String attributeKey(String clientId) {
        return ATTRIBUTE_PREFIX + clientId;
    }

    public static String removedAtKey(String clientId) {
        return REMOVED_AT_PREFIX + clientId;
    }

    // -- user --

    public static void setForUser(UserModel user, String clientId) {
        user.setSingleAttribute(attributeKey(clientId), ATTRIBUTE_VALUE_TRUE);
    }

    public static void excludeForUser(UserModel user, String clientId) {
        user.setSingleAttribute(attributeKey(clientId), ATTRIBUTE_VALUE_FALSE);
    }

    public static void clearForUser(UserModel user, String clientId) {
        user.removeAttribute(attributeKey(clientId));
    }

    public static boolean isUserNotified(UserModel user, String clientId) {
        return ATTRIBUTE_VALUE_TRUE.equals(user.getFirstAttribute(attributeKey(clientId)));
    }

    public static boolean isUserExcluded(UserModel user, String clientId) {
        return ATTRIBUTE_VALUE_FALSE.equals(user.getFirstAttribute(attributeKey(clientId)));
    }

    /**
     * Stamps the user with the moment a receiver-driven
     * {@code removeSubject} fired so the dispatcher can honor the
     * configurable grace window (SSF §9.3). Admin-driven removals
     * deliberately skip this — operator actions are trusted and take
     * effect immediately.
     */
    public static void setRemovedAtForUser(UserModel user, String clientId, long epochSeconds) {
        user.setSingleAttribute(removedAtKey(clientId), Long.toString(epochSeconds));
    }

    /**
     * Stamps the user's tombstone with {@code Time.currentTime()},
     * coalesced to {@link #STAMP_GRANULARITY_SECONDS} so a re-remove
     * inside the same slot doesn't trigger another attribute write
     * (and therefore doesn't invalidate the user cache cluster-wide).
     */
    public static void stampRemovedAtForUser(UserModel user, String clientId) {
        long now = Time.currentTime();
        Long existing = getRemovedAtForUser(user, clientId);
        if (existing != null && now - existing < STAMP_GRANULARITY_SECONDS) {
            return;
        }
        setRemovedAtForUser(user, clientId, now);
    }

    public static void clearRemovedAtForUser(UserModel user, String clientId) {
        user.removeAttribute(removedAtKey(clientId));
    }

    /**
     * Returns the epoch-second at which the user was removed from
     * this receiver's subscription, or {@code null} if no tombstone is
     * set or the stored value is malformed.
     */
    public static Long getRemovedAtForUser(UserModel user, String clientId) {
        String raw = user.getFirstAttribute(removedAtKey(clientId));
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Stream<UserModel> findAllNotifiedUsers(KeycloakSession session,
                                                         RealmModel realm,
                                                         String clientId) {
        return session.users()
                .searchForUserByUserAttributeStream(realm, attributeKey(clientId), ATTRIBUTE_VALUE_TRUE);
    }

    // -- organization --

    public static void setForOrganization(OrganizationModel org, String clientId) {
        Map<String, List<String>> attrs = new HashMap<>(org.getAttributes());
        attrs.put(attributeKey(clientId), List.of(ATTRIBUTE_VALUE_TRUE));
        org.setAttributes(attrs);
    }

    public static void excludeForOrganization(OrganizationModel org, String clientId) {
        Map<String, List<String>> attrs = new HashMap<>(org.getAttributes());
        attrs.put(attributeKey(clientId), List.of(ATTRIBUTE_VALUE_FALSE));
        org.setAttributes(attrs);
    }

    public static void clearForOrganization(OrganizationModel org, String clientId) {
        Map<String, List<String>> attrs = new HashMap<>(org.getAttributes());
        attrs.remove(attributeKey(clientId));
        org.setAttributes(attrs);
    }

    public static boolean isOrganizationNotified(OrganizationModel org, String clientId) {
        List<String> values = org.getAttributes().get(attributeKey(clientId));
        return values != null && values.contains(ATTRIBUTE_VALUE_TRUE);
    }

    public static boolean isOrganizationExcluded(OrganizationModel org, String clientId) {
        List<String> values = org.getAttributes().get(attributeKey(clientId));
        return values != null && values.contains(ATTRIBUTE_VALUE_FALSE);
    }

    public static void setRemovedAtForOrganization(OrganizationModel org, String clientId, long epochSeconds) {
        Map<String, List<String>> attrs = new HashMap<>(org.getAttributes());
        attrs.put(removedAtKey(clientId), List.of(Long.toString(epochSeconds)));
        org.setAttributes(attrs);
    }

    /**
     * Org-level analog of {@link #stampRemovedAtForUser}, with the
     * same {@link #STAMP_GRANULARITY_SECONDS} write coalescing.
     */
    public static void stampRemovedAtForOrganization(OrganizationModel org, String clientId) {
        long now = Time.currentTime();
        Long existing = getRemovedAtForOrganization(org, clientId);
        if (existing != null && now - existing < STAMP_GRANULARITY_SECONDS) {
            return;
        }
        setRemovedAtForOrganization(org, clientId, now);
    }

    public static void clearRemovedAtForOrganization(OrganizationModel org, String clientId) {
        Map<String, List<String>> attrs = new HashMap<>(org.getAttributes());
        attrs.remove(removedAtKey(clientId));
        org.setAttributes(attrs);
    }

    public static Long getRemovedAtForOrganization(OrganizationModel org, String clientId) {
        List<String> values = org.getAttributes().get(removedAtKey(clientId));
        if (values == null || values.isEmpty()) {
            return null;
        }
        String raw = values.get(0);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Stream<OrganizationModel> findAllNotifiedOrganizations(KeycloakSession session,
                                                                         String clientId) {
        if (!Profile.isFeatureEnabled(Profile.Feature.ORGANIZATION)) {
            return Stream.empty();
        }
        OrganizationProvider orgProvider = session.getProvider(OrganizationProvider.class);
        if (orgProvider == null) {
            return Stream.empty();
        }
        return orgProvider.getAllStream(Map.of(attributeKey(clientId), ATTRIBUTE_VALUE_TRUE), null, null);
    }
}
