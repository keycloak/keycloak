package org.keycloak.protocol.oauth2;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.ResourceIndicatorMapper;
import org.keycloak.validate.validators.UriValidator;

import java.net.URI;
import java.util.Set;

/**
 * Helpers for RFC 8707 OAuth2 Resource Indicators support.
 *
 * @link <a href="https://www.rfc-editor.org/rfc/rfc8707">RFC 8707 OAuth2 Resource Indicators</a>
 */
public class ResourceIndicators {

    public static final Set<String> ALLOWED_SCHEMES = Set.copyOf(UriValidator.DEFAULT_ALLOWED_SCHEMES);

    /**
     * Encodes the given resource indicators to a string that can be stored in the client session.
     *
     * @param indicators
     * @return
     */
    public static String encodeResourceIndicators(Set<String> indicators) {
        return String.join(Constants.CFG_DELIMITER, indicators);
    }

    /**
     * Decodes the given string from the client session into a Set of resource indicators.
     *
     * @param encodedIndicators
     * @return
     */
    public static Set<String> decodeResourceIndicators(String encodedIndicators) {
        if (encodedIndicators == null) {
            return null;
        }
        return Set.of(Constants.CFG_DELIMITER_PATTERN.split(encodedIndicators));
    }

    /**
     * Tests if the given resource indicator is allowed according to https://www.rfc-editor.org/rfc/rfc8707#section-2
     *
     * @param indicator
     * @return
     */
    public static boolean isValidResourceIndicatorFormat(String indicator) {
        return UriValidator.INSTANCE.validateUri(URI.create(indicator), ALLOWED_SCHEMES, false, false);
    }

    /**
     * Tests if the given resource indicator is supported by the current client in the context of the current client session.
     *
     * @param clientSession     may be {@literal null} for the initial request
     * @param client
     * @param resourceIndicator
     * @return
     */
    public static boolean isResourceIndicatorAllowed(AuthenticatedClientSessionModel clientSession, ClientModel client, String resourceIndicator) {

        if (!isValidResourceIndicatorFormat(resourceIndicator)) {
            return false;
        }

        if (clientSession == null) {
            // initial request, check if given client allows the given resource indicator
            return isResourceIndicatorAllowed(resourceIndicator, getSupportedResourceIndicators(client));
        }

        // fixed set of resource indicators were already added to auth session
        String encodedClientResourceIndicators = clientSession.getNote(OAuth2Constants.RESOURCE);
        if (encodedClientResourceIndicators == null) {
            return false;
        }

        // check if given resource indicator was provided during initial request
        Set<String> clientResourceIndicators = decodeResourceIndicators(encodedClientResourceIndicators);
        return isResourceIndicatorAllowed(resourceIndicator, clientResourceIndicators);
    }

    private static boolean isResourceIndicatorAllowed(String resourceIndicator, Set<String> resourceIndicators) {

        if (resourceIndicators == null) {
            return false;
        }

        return resourceIndicators.contains(resourceIndicator);
    }

    /**
     * Extracts the Set of fixed allowed resource indicators from the given client.
     *
     * @param client
     * @return
     */
    public static Set<String> getSupportedResourceIndicators(ClientModel client) {

        ProtocolMapperModel resourceIndicatorMapper = ProtocolMapperUtils.findFirstProtocolMapperByProviderId(client, ResourceIndicatorMapper.PROVIDER_ID);

        if (resourceIndicatorMapper == null || resourceIndicatorMapper.getConfig() == null) {
            return null;
        }

        String encodedResourceIndicators = resourceIndicatorMapper.getConfig().get(ResourceIndicatorMapper.RESOURCES_PROPERTY);
        return decodeResourceIndicators(encodedResourceIndicators);
    }

}
