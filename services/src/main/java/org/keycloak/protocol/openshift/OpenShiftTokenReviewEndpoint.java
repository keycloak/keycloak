/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.protocol.openshift;

import org.keycloak.TokenVerifier;
import org.keycloak.common.Profile;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.ext.OIDCExtProvider;
import org.keycloak.protocol.oidc.utils.AuthorizeClientUtil;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenShiftTokenReviewEndpoint implements OIDCExtProvider, EnvironmentDependentProviderFactory {

    private KeycloakSession session;
    private TokenManager tokenManager;
    private EventBuilder event;

    public OpenShiftTokenReviewEndpoint(KeycloakSession session) {
        this.session = session;
        this.tokenManager = new TokenManager();
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Path("/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenReview(OpenShiftTokenReviewRequestRepresentation reviewRequest) throws Exception {
        return tokenReview(null, reviewRequest);
    }

    @Path("/{client_id}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenReview(@PathParam("client_id") String clientId, OpenShiftTokenReviewRequestRepresentation reviewRequest) throws Exception {
        event.event(EventType.INTROSPECT_TOKEN);

        if (clientId != null) {
            session.setAttribute("client_id", clientId);
        }

        checkSsl();
        checkRealm();
        authorizeClient();

        RealmModel realm = session.getContext().getRealm();

        AccessToken token = null;
        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(reviewRequest.getSpec().getToken(), AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            verifier.verify();
            token = verifier.getToken();
        } catch (VerificationException e) {
            error(401, Errors.INVALID_TOKEN, "Token verification failure");
        }

        if (!tokenManager.checkTokenValidForIntrospection(session, realm, token)) {
            error(401, Errors.INVALID_TOKEN, "Token verification failure");
        }

        OpenShiftTokenReviewResponseRepresentation response = new OpenShiftTokenReviewResponseRepresentation();
        response.getStatus().setAuthenticated(true);
        response.getStatus().setUser(new OpenShiftTokenReviewResponseRepresentation.User());

        OpenShiftTokenReviewResponseRepresentation.User userRep = response.getStatus().getUser();
        userRep.setUid(token.getSubject());
        userRep.setUsername(token.getPreferredUsername());

        if (token.getScope() != null && !token.getScope().isEmpty()) {
            OpenShiftTokenReviewResponseRepresentation.Extra extra = new OpenShiftTokenReviewResponseRepresentation.Extra();
            extra.setScopes(token.getScope().split(" "));
            userRep.setExtra(extra);
        }

        if (token.getOtherClaims() != null && token.getOtherClaims().get("groups") != null) {
            List<String> groups = (List<String>) token.getOtherClaims().get("groups");
            userRep.setGroups(groups);
        }

        event.success();
        return Response.ok(response, MediaType.APPLICATION_JSON).build();
    }

    private void checkSsl() {
        if (!session.getContext().getUri().getBaseUri().getScheme().equals("https") && session.getContext().getRealm().getSslRequired().isRequired(session.getContext().getConnection())) {
            error(401, Errors.SSL_REQUIRED, null);
        }
    }

    private void checkRealm() {
        if (!session.getContext().getRealm().isEnabled()) {
            error(401, Errors.REALM_DISABLED,null);
        }
    }

    private void authorizeClient() {
        try {
            ClientModel client = AuthorizeClientUtil.authorizeClient(session, event).getClient();
            event.client(client);

            if (client == null || client.isPublicClient()) {
                error(401, Errors.INVALID_CLIENT, "Public client is not permitted to invoke token review endpoint");
            }

        } catch (ErrorResponseException ere) {
            error(401, Errors.INVALID_CLIENT_CREDENTIALS, ere.getErrorDescription());
        } catch (Exception e) {
            error(401, Errors.INVALID_CLIENT_CREDENTIALS, null);
        }
    }

    private void error(int statusCode, String error, String description) {
        OpenShiftTokenReviewResponseRepresentation rep = new OpenShiftTokenReviewResponseRepresentation();
        rep.getStatus().setAuthenticated(false);

        Response response = Response.status(statusCode).entity(rep).type(MediaType.APPLICATION_JSON_TYPE).build();

        event.error(error);
        event.detail(Details.REASON, description);

        throw new ErrorResponseException(response);
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.OPENSHIFT_INTEGRATION);
    }
}
