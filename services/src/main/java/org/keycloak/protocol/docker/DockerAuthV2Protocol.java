package org.keycloak.protocol.docker;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.docker.mapper.DockerAuthV2AttributeMapper;
import org.keycloak.representations.docker.DockerResponse;
import org.keycloak.representations.docker.DockerResponseToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.TokenUtil;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class DockerAuthV2Protocol implements LoginProtocol {
    protected static final Logger logger = Logger.getLogger(DockerEndpoint.class);

    public static final String LOGIN_PROTOCOL = "docker-v2";
    public static final String ACCOUNT_PARAM = "account";
    public static final String SERVICE_PARAM = "service";
    public static final String SCOPE_PARAM = "scope";
    public static final String ISSUER = "docker.iss"; // don't want to overlap with OIDC notes
    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private KeycloakSession session;
    private RealmModel realm;
    private UriInfo uriInfo;
    private HttpHeaders headers;
    private EventBuilder event;

    public DockerAuthV2Protocol() {
    }

    public DockerAuthV2Protocol(final KeycloakSession session, final RealmModel realm, final UriInfo uriInfo, final HttpHeaders headers, final EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.headers = headers;
        this.event = event;
    }

    @Override
    public LoginProtocol setSession(final KeycloakSession session) {
        this.session = session;
        return this;
    }

    @Override
    public LoginProtocol setRealm(final RealmModel realm) {
        this.realm = realm;
        return this;
    }

    @Override
    public LoginProtocol setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
        return this;
    }

    @Override
    public LoginProtocol setHttpHeaders(final HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public LoginProtocol setEventBuilder(final EventBuilder event) {
        this.event = event;
        return this;
    }

    @Override
    public Response authenticated(final AuthenticationSessionModel authSession, final UserSessionModel userSession, final ClientSessionContext clientSessionCtx) {
        // First, create a base response token with realm + user values populated
        final AuthenticatedClientSessionModel clientSession = clientSessionCtx.getClientSession();
        final ClientModel client = clientSession.getClient();

        DockerResponseToken responseToken = new DockerResponseToken()
                .id(KeycloakModelUtils.generateId())
                .type(TokenUtil.TOKEN_TYPE_BEARER)
                .issuer(authSession.getClientNote(DockerAuthV2Protocol.ISSUER))
                .subject(userSession.getUser().getUsername())
                .issuedNow()
                .audience(client.getClientId())
                .issuedFor(client.getClientId());

        // since realm access token is given in seconds
        final int accessTokenLifespan = realm.getAccessTokenLifespan();
        responseToken.notBefore(responseToken.getIssuedAt())
                .expiration(responseToken.getIssuedAt() + accessTokenLifespan);

        // Next, allow mappers to decorate the token to add/remove scopes as appropriate

        for (Map.Entry<ProtocolMapperModel, ProtocolMapper> entry : ProtocolMapperUtils.getSortedProtocolMappers(session, clientSessionCtx)) {
            ProtocolMapperModel mapping = entry.getKey();
            ProtocolMapper mapper = entry.getValue();

            if (mapper instanceof DockerAuthV2AttributeMapper) {
                final DockerAuthV2AttributeMapper dockerAttributeMapper = (DockerAuthV2AttributeMapper) mapper;
                if (dockerAttributeMapper.appliesTo(responseToken)) {
                    responseToken = dockerAttributeMapper.transformDockerResponseToken(responseToken, mapping, session, userSession, clientSession);
                }
            }
        }

        try {
            // Finally, construct the response to the docker client with the token + metadata
            if (event.getEvent() != null && EventType.LOGIN.equals(event.getEvent().getType())) {
                final KeyManager.ActiveRsaKey activeKey = session.keys().getActiveRsaKey(realm);
                final String encodedToken = new JWSBuilder()
                        .kid(new DockerKeyIdentifier(activeKey.getPublicKey()).toString())
                        .type("JWT")
                        .jsonContent(responseToken)
                        .rsa256(activeKey.getPrivateKey());
                final String expiresInIso8601String = new SimpleDateFormat(ISO_8601_DATE_FORMAT).format(new Date(responseToken.getIssuedAt() * 1000L));

                final DockerResponse responseEntity = new DockerResponse()
                        .setToken(encodedToken)
                        .setExpires_in(accessTokenLifespan)
                        .setIssued_at(expiresInIso8601String);
                return new ResponseBuilderImpl().status(Response.Status.OK).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).entity(responseEntity).build();
            } else {
                logger.errorv("Unable to handle request for event type {0}.  Currently only LOGIN event types are supported by docker protocol.", event.getEvent() == null ? "null" : event.getEvent().getType());
                throw new ErrorResponseException("invalid_request", "Event type not supported", Response.Status.BAD_REQUEST);
            }
        } catch (final InstantiationException e) {
            logger.errorv("Error attempting to create Key ID for Docker JOSE header: ", e.getMessage());
            throw new ErrorResponseException("token_error", "Unable to construct JOSE header for JWT", Response.Status.INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    public Response sendError(final AuthenticationSessionModel clientSession, final LoginProtocol.Error error) {
        return new ResponseBuilderImpl().status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @Override
    public void backchannelLogout(final UserSessionModel userSession, final AuthenticatedClientSessionModel clientSession) {
        errorResponse(userSession, "backchannelLogout");

    }

    @Override
    public Response frontchannelLogout(final UserSessionModel userSession, final AuthenticatedClientSessionModel clientSession) {
        return errorResponse(userSession, "frontchannelLogout");
    }

    @Override
    public Response finishLogout(final UserSessionModel userSession) {
        return errorResponse(userSession, "finishLogout");
    }

    @Override
    public boolean requireReauthentication(final UserSessionModel userSession, final AuthenticationSessionModel clientSession) {
        return true;
    }

    private Response errorResponse(final UserSessionModel userSession, final String methodName) {
        logger.errorv("User {0} attempted to invoke unsupported method {1} on docker protocol.", userSession.getUser().getUsername(), methodName);
        throw new ErrorResponseException("invalid_request", String.format("Attempted to invoke unsupported docker method %s", methodName), Response.Status.BAD_REQUEST);
    }

    @Override
    public void close() {
        // no-op
    }
}
