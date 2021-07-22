package org.keycloak.authentication.authenticators.limit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class UserSessionsLimiterAuthenticatorFactory implements AuthenticatorFactory, ConfigurableAuthenticatorFactory {

    public static final String PROVIDER_ID = "user-session-limit-authenticator";
    private static final UserSessionsLimiterAuthenticator SINGLETON = new UserSessionsLimiterAuthenticator();

    public static final String CONF_ATTR_MAX_SESSIONS = "attr_max_sessions";
    public static final String CONF_ATTR_SORT = "attr_sort";
    public static final String CONF_ATTR_OFFLINE_SESSIONS = "attr_offline_sessions";
    public static final String CONF_ATTR_MAX_OFFLINE_SESSIONS = "attr_max_offline_sessions";
    public static final String CONF_ATTR_OFFLINE_SORT = "attr_offline_sort";

    public enum ConfAttrSortOptions {
        STARTED("Started"),
        LAST_ACCESS("Last Access");

        private static final Map<String, ConfAttrSortOptions> BY_LABEL = new TreeMap<>();

        static {
            for (ConfAttrSortOptions e : values()) {
                BY_LABEL.put(e.label, e);
            }
        }

        private final String label;

        ConfAttrSortOptions(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static ConfAttrSortOptions valueOfLabel(String label) {
            return BY_LABEL.get(label);
        }

        public static List<String> getAllLabels() {
            return new ArrayList<>(BY_LABEL.keySet());
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return SINGLETON;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public String getHelpText() {
        return "Limits the sessions and/or offline sessions a user could have.";
    }

    @Override
    public String getDisplayType() {
        return "User session limit";
    }

    @Override
    public String getReferenceCategory() {
        return null;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        ProviderConfigProperty attributeMaxSessions = new ProviderConfigProperty();
        attributeMaxSessions.setType(ProviderConfigProperty.STRING_TYPE);
        attributeMaxSessions.setName(CONF_ATTR_MAX_SESSIONS);
        attributeMaxSessions.setLabel("Max amount of sessions");
        attributeMaxSessions.setHelpText("Maximum amount of sessions per user");

        ProviderConfigProperty attributeSort = new ProviderConfigProperty();
        attributeSort.setType(ProviderConfigProperty.LIST_TYPE);
        attributeSort.setName(CONF_ATTR_SORT);
        attributeSort.setOptions(ConfAttrSortOptions.getAllLabels());
        attributeSort.setDefaultValue(ConfAttrSortOptions.LAST_ACCESS.toString());
        attributeSort.setLabel("Session sorting");
        attributeSort.setHelpText("Session attribute, which is used to find the oldest session.");

        ProviderConfigProperty attributeOfflineSessions = new ProviderConfigProperty();
        attributeOfflineSessions.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        attributeOfflineSessions.setName(CONF_ATTR_OFFLINE_SESSIONS);
        attributeOfflineSessions.setLabel("Offline sessions");
        attributeOfflineSessions.setHelpText("Should offline sessions be included in the limitation?");
        attributeOfflineSessions.setDefaultValue(false);

        ProviderConfigProperty attributeMaxOfflineSessions = new ProviderConfigProperty();
        attributeMaxOfflineSessions.setType(ProviderConfigProperty.STRING_TYPE);
        attributeMaxOfflineSessions.setName(CONF_ATTR_MAX_OFFLINE_SESSIONS);
        attributeMaxOfflineSessions.setLabel("Max amount of offline sessions");
        attributeMaxOfflineSessions.setHelpText("Maximum amount of offline sessions per user");

        ProviderConfigProperty attributeOfflineSort = new ProviderConfigProperty();
        attributeOfflineSort.setType(ProviderConfigProperty.LIST_TYPE);
        attributeOfflineSort.setName(CONF_ATTR_OFFLINE_SORT);
        attributeOfflineSort.setOptions(ConfAttrSortOptions.getAllLabels());
        attributeOfflineSort.setDefaultValue(ConfAttrSortOptions.LAST_ACCESS.toString());
        attributeOfflineSort.setLabel("Offline session sorting");
        attributeOfflineSort.setHelpText("Session attribute, which is used to find the oldest offline session.");

        return Arrays.asList(attributeMaxSessions, attributeSort, attributeOfflineSessions, attributeMaxOfflineSessions,
                attributeOfflineSort);
    }
}
