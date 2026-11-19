package org.keycloak.protocol.oauth2.cimd.resourceindicators;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.context.CimdResourceIndicatorContext;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AudienceResolveProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.token.TokenInterceptorException;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorContext;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.clientpolicy.ClientPolicyException;

/**
 * Adds the resource indicator (RFC 8707) of an access token to the audience of the token, if the Client ID Metadata
 * Document executors that apply to the client currently allow it.
 *
 * <p>The decision is taken for every token and nothing is recorded on the client, so removing a resource from the
 * allow list of an executor takes effect on the next token, whichever grant type it is requested with.
 * {@code ResourceIndicatorsPostProcessor} then resolves the resource against the audience as for any other client.
 */
public class CimdResourceIndicatorPostProcessor implements TokenPostProcessor {

    /**
     * Name prefix of the {@code Audience} mappers that the executor of 26.8 added to a client for each resource it
     * accepted. They add the resource to every token of the client, regardless of the allow list.
     */
    public static final String LEGACY_AUDIENCE_MAPPER_NAME_PREFIX = "cimd-resource-indicator-";

    private final KeycloakSession session;

    public CimdResourceIndicatorPostProcessor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void process(TokenPostProcessorContext context) {
        AccessToken accessToken = context.accessToken();
        if (accessToken == null) {
            return;
        }

        ClientModel client = context.clientSessionCtx().getClientSession().getClient();
        removeLegacyAudienceMappers(context.clientSessionCtx(), accessToken);

        if (!Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS)) {
            return;
        }

        String resource = getResource(context);
        if (resource == null) {
            return;
        }

        CimdResourceIndicatorContext resourceIndicatorContext = new CimdResourceIndicatorContext(client, resource);
        try {
            session.clientPolicy().triggerOnEvent(resourceIndicatorContext);
        } catch (ClientPolicyException e) {
            throw new TokenInterceptorException(e.getError(), e.getErrorDetail());
        }

        if (resourceIndicatorContext.isApproved()) {
            accessToken.addAudience(resource);
        }
    }

    // the resource that ResourceIndicatorsPostProcessor binds the token to: the one of the token request, otherwise
    // the one of the authorization code or of the refresh token that the token request presents.
    private String getResource(TokenPostProcessorContext context) {
        String resource = context.clientSessionCtx().getAttribute(OAuth2Constants.RESOURCE, String.class);
        if (resource != null) {
            return resource;
        }
        if (context.code() != null) {
            return context.code().getResource();
        }
        if (context.requestRefreshToken() != null) {
            return context.requestRefreshToken().getOtherClaims().get(OAuth2Constants.RESOURCE) instanceof String s ? s : null;
        }
        return null;
    }

    // The mappers have already added their audiences to this token, so these are taken out of it here, unless a
    // remaining audience mapper adds them as well; a currently allowed resource that the token is bound to is added
    // again afterwards. The mappers are removed in the same transaction, which keeps them out of the tokens and
    // introspection responses that follow.
    private void removeLegacyAudienceMappers(ClientSessionContext clientSessionCtx, AccessToken accessToken) {
        ClientModel client = clientSessionCtx.getClientSession().getClient();
        List<ProtocolMapperModel> legacyMappers = client.getProtocolMappersStream()
                .filter(CimdResourceIndicatorPostProcessor::isLegacyAudienceMapper)
                .toList();
        if (legacyMappers.isEmpty()) {
            return;
        }

        Set<String> legacyAudiences = legacyMappers.stream()
                .map(mapper -> mapper.getConfig().get(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE))
                .collect(Collectors.toSet());
        legacyMappers.forEach(client::removeProtocolMapper);

        String[] audience = accessToken.getAudience();
        if (audience != null) {
            AccessToken remainingMappers = applyRemainingAudienceMappers(clientSessionCtx);
            String[] remaining = Arrays.stream(audience)
                    .filter(a -> !legacyAudiences.contains(a) || remainingMappers.hasAudience(a))
                    .toArray(String[]::new);
            accessToken.audience(remaining.length == 0 ? null : remaining);
        }
    }

    // the audience that the Audience and Audience Resolve mappers other than the legacy ones add to an access token
    private AccessToken applyRemainingAudienceMappers(ClientSessionContext clientSessionCtx) {
        AccessToken token = new AccessToken();
        UserSessionModel userSession = clientSessionCtx.getClientSession().getUserSession();
        ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx,
                        entry -> (entry.getValue() instanceof AudienceProtocolMapper || entry.getValue() instanceof AudienceResolveProtocolMapper)
                                && !isLegacyAudienceMapper(entry.getKey()))
                .forEach(entry -> ((OIDCAccessTokenMapper) entry.getValue()).transformAccessToken(token, entry.getKey(), session, userSession, clientSessionCtx));
        return token;
    }

    private static boolean isLegacyAudienceMapper(ProtocolMapperModel mapper) {
        return mapper.getName() != null && mapper.getName().startsWith(LEGACY_AUDIENCE_MAPPER_NAME_PREFIX)
                && AudienceProtocolMapper.PROVIDER_ID.equals(mapper.getProtocolMapper())
                && mapper.getConfig() != null && mapper.getConfig().get(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE) != null;
    }
}
