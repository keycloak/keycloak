package org.keycloak.protocol.oidc.token;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;

public class ResourceIndicatorsPostProcessor implements TokenPostProcessor {

    public static final String ERROR_NOT_MATCHING = "The requested resource is not matching the original request.";
    public static final String ERROR_INVALID_RESOURCE = "The requested resource is invalid, missing, unknown, or malformed.";
    public static final String URN_CLIENT_PREFIX = "urn:client:";
    public static final String CLIENT_RESOURCE_URL_ATTRIBUTE = "resource_url";

    private final KeycloakSession session;

    public ResourceIndicatorsPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        String requestedResource = context.clientSessionCtx().getAttribute(OAuth2Constants.RESOURCE, String.class);
        if (requestedResource == null) {
            return;
        }

        String grantType = context.clientSessionCtx().getAttribute(Constants.GRANT_TYPE, String.class);

        if (grantType.equals(OAuth2Constants.REFRESH_TOKEN)) {
            if (isClientUrn(requestedResource)) {
                if (findAudienceByClientUrn(requestedResource, context.refreshToken().getOriginalAudience()) == null) {
                    throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ERROR_NOT_MATCHING);
                }
            } else {
                if (find(requestedResource, context.refreshToken().getOriginalAudience()) == null) {
                    throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ERROR_NOT_MATCHING);
                }
            }
        } else {
            String audienceToSet;

            String originalResourceParam = context.clientSessionCtx().getClientSession().getNote(OAuth2Constants.RESOURCE);
            if (originalResourceParam != null && !originalResourceParam.equals(requestedResource)) {
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ERROR_NOT_MATCHING);
            }

            if (isClientUrn(requestedResource)) {
                audienceToSet = findAudienceByClientUrn(requestedResource, context.accessToken().getAudience());
            } else {
                audienceToSet = findAudienceByClientAttribute(requestedResource, context.accessToken().getAudience());
            }

            if (audienceToSet == null) {
                throw new TokenInterceptorException(OAuthErrorException.INVALID_TARGET, ERROR_INVALID_RESOURCE);
            }

            context.accessToken().audience(audienceToSet);
        }
    }

    private boolean isClientUrn(String resource) {
        return resource.startsWith(URN_CLIENT_PREFIX);
    }

    private String findAudienceByClientUrn(String resource, String[] audience) {
        String requestedClientId = resource.substring(URN_CLIENT_PREFIX.length());
        return find(requestedClientId, audience);
    }

    private String findAudienceByClientAttribute(String resource, String[] audience) {
        for (String a : audience) {
            ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), a);
            if (client != null) {
                String clientResourceUrl = client.getAttribute(CLIENT_RESOURCE_URL_ATTRIBUTE);
                if (clientResourceUrl.equals(resource)) {
                    return resource;
                }
            }
        }
        return null;
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
