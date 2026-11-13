package org.keycloak.broker.oidc.mappers;

import java.io.IOException;
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
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;

import static org.keycloak.utils.RegexUtils.valueMatchesRegex;

/**
 * Stores matching claims of the brokered tokens as user session notes.
 * <p>
 * Without regular expressions, a claim matches when its value equals the configured value. For a
 * multivalued claim it is enough that one of its values is equal. Session notes hold one string
 * value, so multivalued claims are stored joined with a comma and object values as their JSON
 * representation.
 * <p>
 * With regular expressions, only string claims are matched. Multivalued, object, number and boolean
 * claims are skipped.
 * <p>
 * A claim configured with an empty value is always stored, whatever its value and type, as the OID4VP
 * SD-JWT user session attribute mapper does.
 */
public class ClaimToUserSessionNoteMapper extends AbstractClaimMapper {

    private static final Logger LOG = Logger.getLogger(ClaimToUserSessionNoteMapper.class);

    // The shared mapper drops null values, which would change the JSON of object claims.
    private static final ObjectMapper CLAIM_MAPPER =
            JsonSerialization.mapper.copy().setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);

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
                        "Leave the value empty to store the claim whatever its value. " +
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
                "Without regular expressions, a multivalued claim matches when one of its values equals the configured value, " +
                "and is stored with its values joined with a comma. Object claims are stored as JSON. " +
                "With regular expressions, only string claims are matched. " +
                "Leave the value empty to store the claim whatever its value. " +
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
            Object claimValueObj = getClaimValue(context, claim.getKey(), true);
            if (claimValueObj == null) {
                continue;
            }
            // An empty value matches any claim value, so the claim is stored as is.
            if (claim.getValue().stream().anyMatch(StringUtil::isBlank)) {
                List<String> claimValues = claimValues(claimValueObj);
                if (!claimValues.isEmpty()) {
                    context.setSessionNote(claim.getKey(), String.join(",", claimValues));
                }
                continue;
            }
            if (areClaimValuesRegex) {
                addStringClaimMatchingRegex(context, claim.getKey(), claim.getValue(), claimValueObj);
                continue;
            }
            List<String> claimValues = claimValues(claimValueObj);
            if (claimValues.isEmpty()) {
                continue;
            }
            for (String value : claim.getValue()) {
                if (claimValues.stream().anyMatch(claimValue -> valueEquals(value, claimValue))) {
                    context.setSessionNote(claim.getKey(), String.join(",", claimValues));
                }
            }
        }
    }

    // Regular expressions are only applied to string claims, so a pattern never matches the
    // serialized form of an array or object.
    private static void addStringClaimMatchingRegex(BrokeredIdentityContext context, String claimName,
            List<String> regexes, Object claimValueObj) {
        if (!(claimValueObj instanceof String claimValue)) {
            LOG.warnf(
                    "Claim '%s' does not contain a string value for user with brokerUserId '%s', which is required "
                    + "when claim values are regular expressions. Actual value is of type '%s': %s",
                    claimName, context.getBrokerUserId(), claimValueObj.getClass(), claimValueObj);
            return;
        }
        for (String regex : regexes) {
            if (valueMatchesRegex(regex, claimValue)) {
                context.setSessionNote(claimName, claimValue);
            }
        }
    }

    /**
     * Flattens a claim value into the string values it holds: a top level array yields one value per
     * non null element, scalars yield their text and objects or nested arrays their JSON representation.
     */
    private static List<String> claimValues(Object claimValueObj) {
        List<String> values = new ArrayList<>();
        if (claimValueObj == null) {
            return values;
        }
        JsonNode node = CLAIM_MAPPER.valueToTree(claimValueObj);
        Iterable<JsonNode> elements = node.isArray() ? node : List.of(node);
        for (JsonNode element : elements) {
            if (!element.isNull()) {
                values.add(value(element));
            }
        }
        return values;
    }

    private static String value(JsonNode node) {
        if (node.isValueNode()) {
            return node.asText();
        }
        try {
            return CLAIM_MAPPER.writeValueAsString(node);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize the claim value", e);
        }
    }

}
