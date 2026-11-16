package org.keycloak.protocol.oauth2.cimd.resourceindicators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * The audience mapper that a CIMD executor adds to a client for a resource (RFC 8707) it has accepted on an
 * authorization request of the client, so that {@code ResourceIndicatorsPostProcessor} can resolve the resource against
 * the audience of the token.
 *
 * <p>Unlike {@link AudienceProtocolMapper}, it adds the resource to the audience only while the resource is allowed for
 * the client by the resource indicator allow lists of the CIMD executors (see {@link CimdResourceIndicatorAllowLists}).
 * This is evaluated whenever a token is issued, i.e. for any grant type and for token exchange, with the client whose
 * mappers are used. Therefore, a change that makes a resource no longer allowed for a client takes effect on the tokens
 * issued afterwards without the mapper being removed, whatever the change is: the allow list of an executor, the client
 * profiles or policies (including the global ones), the CIMD, Client Policies or Resource Indicators feature, or the
 * client itself (e.g. its client ID, client roles or attributes, on which the conditions of the client policies
 * depend). Likewise, the mapper adds the resource again once the resource is allowed again.
 *
 * <p>It adds nothing when a token is introspected: the introspection response keeps the audience of the token as
 * issued, and the resource might have been allowed again after the token has been issued without it. Therefore, it
 * adds the resource also to a lightweight access token, which would otherwise get it only when it is introspected.
 *
 * <p>The mappers added by the versions that used {@link AudienceProtocolMapper} for this purpose are converted to this
 * type when the database is upgraded, and when a CIMD executor handles an authorization request of the client (see
 * {@link #adoptLegacyMappers(ClientModel)}), e.g. for a client imported from a representation of such a version.
 */
public class CimdResourceIndicatorAudienceMapper extends AudienceProtocolMapper {

    public static final String PROVIDER_ID = "oidc-cimd-resource-indicator-audience-mapper";

    /**
     * The name prefix of the mappers added by the CIMD executors. The resource is the custom audience of the mapper,
     * not a part of its name, so that each resource has its own mapper.
     */
    public static final String NAME_PREFIX = "cimd-resource-indicator-";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(INCLUDED_CUSTOM_AUDIENCE);
        property.setLabel("included.custom.audience.label");
        property.setHelpText("included.custom.audience.tooltip");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, CimdResourceIndicatorAudienceMapper.class);
        // the mapper adds nothing to the introspection response, and therefore does not leave the resource out of a
        // lightweight access token (see transformIntrospectionToken and transformAccessToken).
        configProperties.removeIf(prop -> OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION.equals(prop.getName())
                || OIDCAttributeMapperHelper.INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN.equals(prop.getName()));

        // Don't include audience in ID Token by default
        for (ProviderConfigProperty prop : configProperties) {
            if (OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN.equals(prop.getName())) {
                prop.setDefaultValue("false");
            }
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Audience (CIMD Resource Indicator)";
    }

    @Override
    public String getHelpText() {
        return "Add the custom audience to the audience (aud) field of token, only while it is allowed for the client as a resource by the "
                + "resource indicator allow lists of the OAuth Client ID Metadata Document executors";
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx) {
        String resource = getResource(mappingModel);
        if (resource == null || clientSessionCtx == null || clientSessionCtx.getClientSession() == null) {
            return;
        }
        if (CimdResourceIndicatorAllowLists.isAllowed(keycloakSession, clientSessionCtx.getClientSession().getClient(), resource)) {
            token.addAudience(resource);
        }
    }

    /**
     * Adds the resource also to a lightweight access token: the mapper adds nothing when the token is introspected, so
     * the token itself has to carry the resource it has been issued for.
     */
    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        if (OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            setClaim(token, mappingModel, userSession, session, clientSessionCtx);
        }
        return token;
    }

    /**
     * Adds nothing: the introspection response already has the audience of the token as issued. Adding the resource
     * here would bind a token to a resource it has not been issued for, e.g. a token issued while the resource was not
     * allowed, introspected after the resource has been allowed again.
     */
    @Override
    public AccessToken transformIntrospectionToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                                   UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return token;
    }

    /**
     * @return {@code true} if the client has a mapper added by a CIMD executor for the resource
     */
    public static boolean hasMapper(ClientModel client, String resource) {
        return getMappers(client).anyMatch(m -> resource.equals(getResource(m)));
    }

    /**
     * Adds a mapper for the resource to the client, unless the client already has one.
     */
    public static void ensureMapper(ClientModel client, String resource) {
        if (getMappers(client).anyMatch(m -> PROVIDER_ID.equals(m.getProtocolMapper()) && resource.equals(getResource(m)))) {
            return;
        }
        ProtocolMapperModel mapper = createClaimMapper(NAME_PREFIX + KeycloakModelUtils.generateId(), null, resource, true, false, false);
        mapper.setProtocolMapper(PROVIDER_ID);
        client.addProtocolMapper(mapper);
    }

    /**
     * Removes the mappers added by a CIMD executor for a resource that a CIMD executor has rejected on an authorization
     * request of the client. They are kept if the rejection is obsolete, i.e. no CIMD executor that applies or might
     * apply to the client rejects the resource any longer according to the client profiles and policies as the session
     * reads them, e.g. the resource has been allowed again since the authorization request has read them.
     *
     * @param session the session, whose context has the realm of the client
     * @param client the client
     * @param resource the resource rejected
     */
    public static void removeMappersOfRejectedResource(KeycloakSession session, ClientModel client, String resource) {
        if (!CimdResourceIndicatorAllowLists.isRejected(session, client, resource)) {
            return;
        }
        getMappers(client)
                .filter(m -> resource.equals(getResource(m)))
                .toList()
                .forEach(client::removeProtocolMapper);
    }

    /**
     * Converts the mappers of the client that a CIMD executor of a version using {@link AudienceProtocolMapper} has
     * added to this type, so that they add their resource to the audience only while it is allowed.
     *
     * @return {@code true} if any mapper has been converted
     */
    public static boolean adoptLegacyMappers(ClientModel client) {
        List<ProtocolMapperModel> legacyMappers = getMappers(client)
                .filter(m -> AudienceProtocolMapper.PROVIDER_ID.equals(m.getProtocolMapper()))
                .toList();
        legacyMappers.forEach(m -> {
            m.setProtocolMapper(PROVIDER_ID);
            client.updateProtocolMapper(m);
        });
        return !legacyMappers.isEmpty();
    }

    // the mappers added by the CIMD executors, of this type or of the type used by the earlier versions.
    private static Stream<ProtocolMapperModel> getMappers(ClientModel client) {
        return client.getProtocolMappersStream()
                .filter(m -> m.getName() != null && m.getName().startsWith(NAME_PREFIX))
                .filter(m -> PROVIDER_ID.equals(m.getProtocolMapper()) || AudienceProtocolMapper.PROVIDER_ID.equals(m.getProtocolMapper()));
    }

    private static String getResource(ProtocolMapperModel mapper) {
        return mapper.getConfig() == null ? null : mapper.getConfig().get(INCLUDED_CUSTOM_AUDIENCE);
    }
}
