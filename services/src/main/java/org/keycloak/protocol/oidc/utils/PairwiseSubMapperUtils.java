package org.keycloak.protocol.oidc.utils;

import org.jboss.logging.Logger;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PairwiseSubMapperUtils {
    private static final Logger logger = Logger.getLogger(PairwiseSubMapperUtils.class);

    /**
     * Returns a set of valid redirect URIs from the root url and redirect URIs registered on a client.
     *
     * @param clientRootUrl
     * @param clientRedirectUris
     * @return
     */
    public static Set<String> resolveValidRedirectUris(String clientRootUrl, Collection<String> clientRedirectUris) {
        if (clientRedirectUris == null) {
            return Collections.emptySet();
        }

        Set<String> validRedirects = new HashSet<String>();
        for (String redirectUri : clientRedirectUris) {
            if (redirectUri.startsWith("/")) {
                redirectUri = relativeToAbsoluteURI(clientRootUrl, redirectUri);
                logger.debugv("replacing relative valid redirect with: {0}", redirectUri);
            }
            if (redirectUri != null) {
                validRedirects.add(redirectUri);
            }
        }
        return validRedirects.stream()
                .filter(r -> r != null && !r.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Tries to resolve a valid sector identifier from a sector identifier URI.
     *
     * @param sectorIdentifierUri
     * @return a sector identifier iff. the sector identifier URI is a valid URI, contains a valid scheme and contains a valid host component.
     */
    public static String resolveValidSectorIdentifier(String sectorIdentifierUri) {
        URI uri;
        try {
            uri = new URI(sectorIdentifierUri);
        } catch (URISyntaxException e) {
            logger.debug("Invalid sector identifier URI", e);
            return null;
        }

        if (uri.getScheme() == null) {
            logger.debugv("Invalid sector identifier URI: {0}", sectorIdentifierUri);
            return null;
        }

        /*if (!uri.getScheme().equalsIgnoreCase("https")) {
            logger.debugv("The sector identifier URI scheme must be HTTPS. Was '{0}'", uri.getScheme());
            return null;
        }*/

        if (uri.getHost() == null) {
            logger.debug("The sector identifier URI must specify a host");
            return null;
        }

        return uri.getHost();
    }

    /**
     * Tries to resolve a valid sector identifier from the redirect URIs registered on a client.
     *
     * @param clientRootUrl      Root url registered on the client.
     * @param clientRedirectUris Redirect URIs registered on the client.
     * @return a sector identifier iff. all the registered redirect URIs are located at the same host, otherwise {@code null}.
     */
    public static String resolveValidSectorIdentifier(String clientRootUrl, Set<String> clientRedirectUris) {
        Set<String> hosts = new HashSet<>();
        for (String redirectUri : resolveValidRedirectUris(clientRootUrl, clientRedirectUris)) {
            try {
                URI uri = new URI(redirectUri);
                hosts.add(uri.getHost());
            } catch (URISyntaxException e) {
                logger.debugv("client redirect uris contained an invalid uri: {0}", redirectUri);
            }
        }
        if (hosts.isEmpty()) {
            logger.debug("could not infer any valid sector_identifiers from client redirect uris");
            return null;
        }
        if (hosts.size() > 1) {
            logger.debug("the client redirect uris contained multiple hosts");
            return null;
        }
        return hosts.iterator().next();
    }

    /**
     * Checks if the the registered client redirect URIs matches the set of redirect URIs from the sector identifier URI.
     *
     * @param clientRootUrl      root url registered on the client.
     * @param clientRedirectUris redirect URIs registered on the client.
     * @param sectorRedirects    value of the sector identifier URI.
     * @return {@code true} iff. the all the redirect URIs can be described by the {@code sectorRedirects}, i.e if the registered redirect URIs is a subset of the {@code sectorRedirects}, otherwise {@code false}.
     */
    public static boolean matchesRedirects(String clientRootUrl, Set<String> clientRedirectUris, Set<String> sectorRedirects) {
        Set<String> validRedirects = resolveValidRedirectUris(clientRootUrl, clientRedirectUris);
        for (String redirect : validRedirects) {
            if (!matchesRedirect(sectorRedirects, redirect)) return false;
        }
        return true;
    }

    private static boolean matchesRedirect(Set<String> validRedirects, String redirect) {
        for (String validRedirect : validRedirects) {
            if (validRedirect.endsWith("*") && !validRedirect.contains("?")) {
                // strip off the query component - we don't check them when wildcards are effective
                String r = redirect.contains("?") ? redirect.substring(0, redirect.indexOf("?")) : redirect;
                // strip off *
                int length = validRedirect.length() - 1;
                validRedirect = validRedirect.substring(0, length);
                if (r.startsWith(validRedirect)) return true;
                // strip off trailing '/'
                if (length - 1 > 0 && validRedirect.charAt(length - 1) == '/') length--;
                validRedirect = validRedirect.substring(0, length);
                if (validRedirect.equals(r)) return true;
            } else if (validRedirect.equals(redirect)) return true;
        }
        return false;
    }

    private static String relativeToAbsoluteURI(String rootUrl, String relative) {
        if (rootUrl == null || rootUrl.isEmpty()) {
            return null;
        }
        relative = rootUrl + relative;
        return relative;
    }

    public static List<ProtocolMapperRepresentation> getPairwiseSubMappers(ClientRepresentation client) {
        List<ProtocolMapperRepresentation> pairwiseMappers = new LinkedList<>();
        List<ProtocolMapperRepresentation> mappers = client.getProtocolMappers();

        if (mappers != null) {
            client.getProtocolMappers().stream().filter((ProtocolMapperRepresentation mapping) -> {
                return mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX);
            }).forEach((ProtocolMapperRepresentation mapping) -> {
                pairwiseMappers.add(mapping);
            });
        }

        return pairwiseMappers;
    }
}
