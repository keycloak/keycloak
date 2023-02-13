package org.keycloak.protocol.oidc.utils;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:martin.hardselius@gmail.com">Martin Hardselius</a>
 */
public class PairwiseSubMapperValidator {

    public static final String PAIRWISE_MALFORMED_CLIENT_REDIRECT_URI = "pairwiseMalformedClientRedirectURI";
    public static final String PAIRWISE_CLIENT_REDIRECT_URIS_MISSING_HOST = "pairwiseClientRedirectURIsMissingHost";
    public static final String PAIRWISE_CLIENT_REDIRECT_URIS_MULTIPLE_HOSTS = "pairwiseClientRedirectURIsMultipleHosts";
    public static final String PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI = "pairwiseMalformedSectorIdentifierURI";
    public static final String PAIRWISE_FAILED_TO_GET_REDIRECT_URIS = "pairwiseFailedToGetRedirectURIs";
    public static final String PAIRWISE_REDIRECT_URIS_MISMATCH = "pairwiseRedirectURIsMismatch";

    public static void validate(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mapperModel);
        String rootUrl = client.getRootUrl();
        Set<String> redirectUris = client.getRedirectUris();
        validate(session, rootUrl, redirectUris, sectorIdentifierUri);
    }

    public static void validate(KeycloakSession session, String rootUrl, Set<String> redirectUris, String sectorIdentifierUri) throws ProtocolMapperConfigException {
        if (sectorIdentifierUri == null || sectorIdentifierUri.isEmpty()) {
            validateClientRedirectUris(rootUrl, redirectUris);
            return;
        }
        validateSectorIdentifierUri(sectorIdentifierUri);
        validateSectorIdentifierUri(session, rootUrl, redirectUris, sectorIdentifierUri);
    }

    private static void validateClientRedirectUris(String rootUrl, Set<String> redirectUris) throws ProtocolMapperConfigException {
        Set<String> hosts = new HashSet<>();
        for (String redirectUri : PairwiseSubMapperUtils.resolveValidRedirectUris(rootUrl, redirectUris)) {
            try {
                URI uri = new URI(redirectUri);
                hosts.add(uri.getHost());
            } catch (URISyntaxException e) {
                throw new ProtocolMapperConfigException("Client contained an invalid redirect URI.",
                        PAIRWISE_MALFORMED_CLIENT_REDIRECT_URI, e);
            }
        }

        if (hosts.isEmpty()) {
            throw new ProtocolMapperConfigException("Client redirect URIs must contain a valid host component.",
                    PAIRWISE_CLIENT_REDIRECT_URIS_MISSING_HOST);
        }

        if (hosts.size() > 1) {
            throw new ProtocolMapperConfigException("Without a configured Sector Identifier URI, client redirect URIs must not contain multiple host components.", PAIRWISE_CLIENT_REDIRECT_URIS_MULTIPLE_HOSTS);
        }
    }

    private static void validateSectorIdentifierUri(String sectorIdentifierUri) throws ProtocolMapperConfigException {
        URI uri;
        try {
            uri = new URI(sectorIdentifierUri);
        } catch (URISyntaxException e) {
            throw new ProtocolMapperConfigException("Invalid Sector Identifier URI.",
                    PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI, e);
        }
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new ProtocolMapperConfigException("Invalid Sector Identifier URI.",
                    PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI);
        }
    }

    private static void validateSectorIdentifierUri(KeycloakSession session, String rootUrl, Set<String> redirectUris, String sectorIdentifierUri) throws ProtocolMapperConfigException {
        Set<String> sectorRedirects = getSectorRedirects(session, sectorIdentifierUri);
        if (!PairwiseSubMapperUtils.matchesRedirects(rootUrl, redirectUris, sectorRedirects)) {
            throw new ProtocolMapperConfigException("Client redirect URIs does not match redirect URIs fetched from the Sector Identifier URI.",
                    PAIRWISE_REDIRECT_URIS_MISMATCH);
        }
    }

    private static Set<String> getSectorRedirects(KeycloakSession session, String sectorIdentifierUri) throws ProtocolMapperConfigException {
        InputStream is = null;
        try {
            is = session.getProvider(HttpClientProvider.class).get(sectorIdentifierUri);
            List<String> sectorRedirects = JsonSerialization.readValue(is, TypedList.class);
            return new HashSet<>(sectorRedirects);
        } catch (IOException e) {
            throw new ProtocolMapperConfigException("Failed to get redirect URIs from the Sector Identifier URI.",
                    PAIRWISE_FAILED_TO_GET_REDIRECT_URIS, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static class TypedList extends ArrayList<String> {}

}
