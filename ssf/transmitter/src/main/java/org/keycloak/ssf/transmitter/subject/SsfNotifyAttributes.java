package org.keycloak.ssf.transmitter.subject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
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
    private static final String ATTRIBUTE_VALUE_TRUE = "true";
    private static final String ATTRIBUTE_VALUE_FALSE = "false";

    private SsfNotifyAttributes() {}

    public static String attributeKey(String clientId) {
        return ATTRIBUTE_PREFIX + clientId;
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
