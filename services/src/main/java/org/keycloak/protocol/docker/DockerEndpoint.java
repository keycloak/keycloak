package org.keycloak.protocol.docker;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequest;
import org.keycloak.protocol.oidc.endpoints.request.AuthorizationEndpointRequestParserProcessor;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.util.CacheControlUtil;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.utils.ProfileHelper;

import javax.ws.rs.GET;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * Implements a docker-client understandable format.
 */
public class DockerEndpoint extends AuthorizationEndpointBase {
    protected static final Logger logger = Logger.getLogger(DockerEndpoint.class);

    private final EventType login;
    private String account;
    private String service;
    private String scope;
    private ClientModel client;
    private AuthenticationSessionModel authenticationSession;

    public DockerEndpoint(final RealmModel realm, final EventBuilder event, final EventType login) {
        super(realm, event);
        this.login = login;
    }

    @GET
    public Response build() {
        ProfileHelper.requireFeature(Profile.Feature.DOCKER);

        final MultivaluedMap<String, String> params = session.getContext().getUri().getQueryParameters();

        account = params.getFirst(DockerAuthV2Protocol.ACCOUNT_PARAM);
        if (account == null) {
            logger.debug("Account parameter not provided by docker auth.  This is techincally required, but not actually used since " +
                    "username is provided by Basic auth header.");
        }
        service = params.getFirst(DockerAuthV2Protocol.SERVICE_PARAM);
        if (service == null) {
            throw new ErrorResponseException("invalid_request", "service parameter must be provided", Response.Status.BAD_REQUEST);
        }
        client = realm.getClientByClientId(service);
        if (client == null) {
            logger.errorv("Failed to lookup client given by service={0} parameter for realm: {1}.", service, realm.getName());
            throw new ErrorResponseException("invalid_client", "Client specified by 'service' parameter does not exist", Response.Status.BAD_REQUEST);
        }
        scope = params.getFirst(DockerAuthV2Protocol.SCOPE_PARAM);

        checkSsl();
        checkRealm();

        final AuthorizationEndpointRequest authRequest = AuthorizationEndpointRequestParserProcessor.parseRequest(event, session, client, params);
        authenticationSession = createAuthenticationSession(client, authRequest.getState());

        updateAuthenticationSession();

        // So back button doesn't work
        CacheControlUtil.noBackButtonCacheControlHeader();

        return handleBrowserAuthenticationRequest(authenticationSession, new DockerAuthV2Protocol(session, realm, session.getContext().getUri(), headers, event.event(login)), false, false);
    }

    private void updateAuthenticationSession() {
        authenticationSession.setProtocol(DockerAuthV2Protocol.LOGIN_PROTOCOL);
        authenticationSession.setAction(CommonClientSessionModel.Action.AUTHENTICATE.name());

        // Docker specific stuff
        authenticationSession.setClientNote(DockerAuthV2Protocol.ACCOUNT_PARAM, account);
        authenticationSession.setClientNote(DockerAuthV2Protocol.SERVICE_PARAM, service);
        authenticationSession.setClientNote(DockerAuthV2Protocol.SCOPE_PARAM, scope);
        authenticationSession.setClientNote(DockerAuthV2Protocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

    }

    @Override
    protected AuthenticationFlowModel getAuthenticationFlow(AuthenticationSessionModel authSession) {
        return realm.getDockerAuthenticationFlow();
    }

}
