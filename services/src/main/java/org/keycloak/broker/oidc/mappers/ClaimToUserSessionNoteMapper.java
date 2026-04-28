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
        final var claims = mapperModel.getConfigMap(CLAIMS_PROPERTY_NAME);
        final var areClaimValuesRegex =
                Boolean.parseBoolean(mapperModel.getConfig().get(ARE_CLAIM_VALUES_REGEX_PROPERTY_NAME));

        for (Map.Entry<String, List<String>> claim : claims.entrySet()) {
            final var claimValueObj = getClaimValue(context, claim.getKey());
            if (claimValueObj == null) {
                continue;
            }

            final var brokeredUserId = context.getBrokerUserId();
            final var claimKey = claim.getKey();

            for (String value : claim.getValue()) {
                final var claimValue = getClaimValueString(brokeredUserId, claimKey, claimValueObj);
                if (claimValue == null) {
                    continue;
                }

                final var claimValuesMatch = areClaimValuesRegex ? valueMatchesRegex(value, claimValue)
                        : valueEquals(value, claimValue);

                if (claimValuesMatch) {
                    context.setSessionNote(claim.getKey(), claimValue);
                }
            }
        }
    }

    private String getClaimValueString(final String brokeredUserId, final String claimKey, final Object claimValueObj) {
        var result = "";

        if (claimValueObj instanceof List) {
            result = convertArrayClaimToString(brokeredUserId, claimKey, claimValueObj);
        } else if (claimValueObj instanceof String claimValueString) {
            result = claimValueString;
        } else {
            LOG.warnf("Claim '%s':%s contains a value with an unsupported type (%s) for user with brokerUserId '%s'. "
                            + "The supported types are either String or List.", claimKey, claimValueObj,
                    claimValueObj.getClass(), brokeredUserId);

            return null;
        }

        return result;
    }

    private String convertArrayClaimToString(final String brokeredUserId, final String claimKey,
                                             final Object claimValueObj) {
        var result = String.valueOf(claimValueObj);
        result = result.replace(" ", "");
        result = result.replace("[", "");
        result = result.replace("]", "");

        final var resultIsInvalid = containsMalformedEntries(brokeredUserId, claimKey, claimValueObj, result);
        if (Boolean.TRUE.equals(resultIsInvalid)) {
            return null;
        }

        return  result;
    }

    private Boolean containsMalformedEntries(final String brokeredUserId, final String claimKey,
                                             final Object claimValueObj, final String stringArray) {
        var foundEntryWithIllegalCharacter = false;
        final var entries = stringArray.split(",");

        for(String entry: entries) {
            final var entryIsValid = entry.matches("^[a-zA-Z0-9._-]+$");
            if (!entryIsValid) {
                LOG.warnf("Claim \"%s\": %s contains a String \"%s\" with unsupported characters for user with brokeredUserId '%s'",
                        claimKey, claimValueObj, entry, brokeredUserId);

                foundEntryWithIllegalCharacter = true;
                break;
            }
        }

        return foundEntryWithIllegalCharacter;
    }
}
