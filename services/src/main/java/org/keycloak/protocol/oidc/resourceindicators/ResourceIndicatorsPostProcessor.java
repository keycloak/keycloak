package org.keycloak.protocol.oidc.resourceindicators;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;

public class ResourceIndicatorsPostProcessor implements TokenPostProcessor {

    private final KeycloakSession session;

    public ResourceIndicatorsPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        Set<String> requestedResources = ResourceIndicators.parse(context.clientSessionCtx().getAttribute(OAuth2Constants.RESOURCE, String.class));
        if (requestedResources != null && !requestedResources.stream().allMatch(ResourceIndicatorValidation::isValidResourceIndicator)) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_INVALID_RESOURCE);
        }

        String grantType = context.clientSessionCtx().getAttribute(Constants.GRANT_TYPE, String.class);

        boolean originalResourceParamRequired = false;
        boolean refresh = false;
        Set<String> originalResources = null;
        if (OAuth2Constants.AUTHORIZATION_CODE.equals(grantType)) {
            originalResources = ResourceIndicators.parse(context.code().getResource());
            originalResourceParamRequired = true;
        } else if (context.requestRefreshToken() != null) {
            // the grant type is not available when refreshing an offline token, so the refresh token of the request is checked
            originalResources = ResourceIndicators.parse((String) context.requestRefreshToken().getOtherClaims().get(OAuth2Constants.RESOURCE));
            originalResourceParamRequired = true;
            refresh = true;
        }

        if (originalResources == null && requestedResources == null) {
            return;
        }

        if (originalResourceParamRequired) {
            if (originalResources == null) {
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_NOT_MATCHING);
            }

            if (requestedResources == null) {
                requestedResources = originalResources;
            } else if (!originalResources.containsAll(requestedResources)) {
                // only the resources that were originally granted, or a subset of them, can be requested
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_NOT_MATCHING);
            }
        }

        // the refresh token keeps all the originally granted resources, so that any subset of them can be requested
        // when refreshing. Therefore, all of them need to be resolved when the grant is used for the first time, not
        // only the requested ones. When refreshing, the scopes can be narrowed as well, so that only the requested
        // resources can be expected in the audience of the new access token.
        Set<String> grantedResources = originalResourceParamRequired ? originalResources : requestedResources;
        Set<String> audienceToSet = new LinkedHashSet<>();
        String[] audience = context.accessToken().getAudience();
        // the resource URLs of the clients of the audience, so that each client is looked up at most once per request
        Map<String, String> clientResourceUrls = new HashMap<>();
        for (String resource : refresh ? requestedResources : grantedResources) {
            String resolvedAudience = resolveAudience(resource, audience, clientResourceUrls);
            if (resolvedAudience == null) {
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ResourceIndicatorConstants.ERROR_INVALID_RESOURCE);
            }
            if (requestedResources.contains(resource)) {
                audienceToSet.add(resolvedAudience);
            }
        }

        if (context.refreshToken() != null) {
            context.refreshToken().getOtherClaims().put(OAuth2Constants.RESOURCE, ResourceIndicators.join(grantedResources));
        }
        context.accessToken().audience(audienceToSet.toArray(new String[0]));
    }

    private String resolveAudience(String resource, String[] audience, Map<String, String> clientResourceUrls) {
        if (audience == null) {
            return null;
        }
        if (isClientUrn(resource)) {
            return findAudienceByClientUrn(resource, audience);
        }
        String resolvedAudience = findCustomAudience(resource, audience);
        return resolvedAudience != null ? resolvedAudience : findAudienceByClientAttribute(resource, audience, clientResourceUrls);
    }

    private boolean isClientUrn(String resource) {
        return resource.startsWith(ResourceIndicatorConstants.URN_CLIENT_PREFIX);
    }

    private String findAudienceByClientUrn(String resource, String[] audience) {
        String requestedClientId = resource.substring(ResourceIndicatorConstants.URN_CLIENT_PREFIX.length());
        return find(requestedClientId, audience);
    }

    private String findAudienceByClientAttribute(String resource, String[] audience, Map<String, String> clientResourceUrls) {
        for (String a : audience) {
            // an audience that is not a client is kept as well, because a lookup of a missing client is not cached
            if (!clientResourceUrls.containsKey(a)) {
                ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), a);
                clientResourceUrls.put(a, client != null ? client.getAttribute(ResourceIndicatorConstants.CLIENT_RESOURCE_URL_ATTRIBUTE) : null);
            }
            if (resource.equals(clientResourceUrls.get(a))) {
                return resource;
            }
        }
        return null;
    }

    private String findCustomAudience(String resource, String[] audience) {
        // Direct match against existing token audiences (fast path to support custom audiences without DB lookups)
        return find(resource, audience);
    }

    private String find(String search, String[] array) {
        for (String a : array) {
            if (a.equals(search)) {
                return a;
            }
        }
        return null;
    }

}
