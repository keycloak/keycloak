package org.keycloak.broker.oidc.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import org.jboss.logging.Logger;

import static org.keycloak.utils.RegexUtils.valueMatchesRegex;

public class ClaimToUserSessionNoteMapper extends AbstractClaimMapper {

    private static final Logger LOG = Logger.getLogger(ClaimToUserSessionNoteMapper.class);

    private static final String CLAIMS_PROPERTY_NAME = "claims";
    private static final String ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME = "are.claim.values.regex";
    private static final String[] COMPATIBLE_PROVIDERS = {KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = new ArrayList<>();

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES =
            new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    static {
        ProviderConfigProperty claimsProperty = new ProviderConfigProperty();
        claimsProperty.setName(CLAIMS_PROPERTY_NAME);
        claimsProperty.setLabel("Claims");
        claimsProperty.setHelpText(
                "Names and values of the claims to search for in the token. " +
                        "You can reference nested claims using a '.', i.e. 'address.locality'. " +
                        "To use dot (.) literally, escape it with backslash (\\.)");
        claimsProperty.setType(ProviderConfigProperty.MAP_TYPE);
        CONFIG_PROPERTIES.add(claimsProperty);
        ProviderConfigProperty isClaimValueRegexProperty = new ProviderConfigProperty();
        isClaimValueRegexProperty.setName(ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME);
        isClaimValueRegexProperty.setLabel("Regex Claim Values");
        isClaimValueRegexProperty.setHelpText("If enabled, claim values are interpreted as regular expressions.");
        isClaimValueRegexProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        CONFIG_PROPERTIES.add(isClaimValueRegexProperty);
    }

    public static final String PROVIDER_ID = "oidc-user-session-note-idp-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "User Session";
    }

    @Override
    public String getDisplayType() {
        return "User Session Note Mapper";
    }

    @Override
    public String getHelpText() {
        return "Add every matching claim to the user session note. " +
                "This can be used together for instance with the 'User Session Note' protocol mapper configured for your " +
                "client scope or client, so that claims for 3rd party IDPs would be available in the access token sent to your client application.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addClaimsToSessionNote(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
            IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        addClaimsToSessionNote(mapperModel, context);
    }

    private void addClaimsToSessionNote(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        Map<String, List<String>> claims = mapperModel.getConfigMap(CLAIMS_PROPERTY_NAME);
        boolean areClaimValuesRegex =
                Boolean.parseBoolean(mapperModel.getConfig().get(ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME));

        for (Map.Entry<String, List<String>> claim : claims.entrySet()) {
            Object claimValueObj = getClaimValue(context, claim.getKey());
            for (String value : claim.getValue()) {
                if (claimValueObj != null) {
                    if (!(claimValueObj instanceof String)) {
                        LOG.warnf(
                                "Claim '%s' does not contain a string value for user with brokerUserId '%s'. "
                                + "Actual value is of type '%s': %s",
                                claim.getKey(),
                                context.getBrokerUserId(), claimValueObj.getClass(), claimValueObj);
                        continue;
                    }

                    String claimValue = (String) claimValueObj;

                    boolean claimValuesMatch = areClaimValuesRegex ? valueMatchesRegex(value, claimValue)
                            : valueEquals(value, claimValue);

                    if (claimValuesMatch) {
                        context.setSessionNote(claim.getKey(), claimValue);
                    }
                }
            }
        }
    }

}
