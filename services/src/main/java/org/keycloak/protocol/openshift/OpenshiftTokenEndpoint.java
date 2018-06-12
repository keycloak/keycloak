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

import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.openshift.clientstorage.OpenshiftClientModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftTokenEndpoint extends TokenEndpoint {
    public OpenshiftTokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        super(tokenManager, realm, event);
    }

    @Path("token-review")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response tokenReview(TokenReviewRequestRepresentation reviewRequest) throws Exception {
        event.event(EventType.INTROSPECT_TOKEN);

        checkSsl();
        checkRealm();

        AccessToken toIntrospect = null;
        String token = reviewRequest.getSpec().getToken();
        try {
            RSATokenVerifier verifier = RSATokenVerifier.create(token)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            PublicKey publicKey = session.keys().getRsaPublicKey(realm, verifier.getHeader().getKeyId());
            if (publicKey == null) {
                event.detail(Details.REASON, "invalid public key");
                event.error(Errors.INVALID_TOKEN);
                return Response.status(401).entity(TokenReviewResponseRepresentation.error("invalid token")).type(MediaType.APPLICATION_JSON_TYPE).build();
            } else {
                verifier.publicKey(publicKey);
                verifier.verify();
                toIntrospect = verifier.getToken();
            }
        } catch (VerificationException e) {
            event.detail(Details.REASON, "token verification failure");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(401).entity(TokenReviewResponseRepresentation.error("invalid token")).type(MediaType.APPLICATION_JSON_TYPE).build();
        }

        if (!tokenManager.isTokenValid(session, realm, toIntrospect)) {
            event.detail(Details.REASON, "stale token");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(401).entity(TokenReviewResponseRepresentation.error("invalid token")).type(MediaType.APPLICATION_JSON_TYPE).build();
        }


        UserModel user = tokenManager.extractUser(session, realm, toIntrospect);

        if (toIntrospect.getAudience() == null || toIntrospect.getAudience().length == 0) {
            event.detail(Details.REASON, "token audience is missing");
            event.error(Errors.INVALID_CLIENT);
            return Response.status(401).entity(TokenReviewResponseRepresentation.error("invalid client")).type(MediaType.APPLICATION_JSON_TYPE).build();

        }

        ClientModel client = session.realms().getClientByClientId(toIntrospect.getAudience()[0], realm);

        if (client == null) {
            event.detail(Details.REASON,    "invalid audience");
            event.error(Errors.INVALID_CLIENT);
            return Response.status(401).entity(TokenReviewResponseRepresentation.error("invalid client")).type(MediaType.APPLICATION_JSON_TYPE).build();

        }

        TokenReviewResponseRepresentation success = TokenReviewResponseRepresentation.success();
        TokenReviewResponseRepresentation.Status.User userRep = success.getStatus().getUser();
        userRep.setUid(user.getId());
        userRep.setUsername(user.getUsername());

        String scopeParam = tokenManager.getRequiredOAuthScope(session, realm, toIntrospect);
        List<String> scopes = new LinkedList<>();
        if (scopeParam != null) {
            for (String scope : scopeParam.split("\\s+")) {
                scopes.add(scope);
            }
        }
        if (client instanceof OpenshiftClientModel) {
            Set<String> failedScopes = ((OpenshiftClientModel)client).validateRequestedScope(scopes);
            if (failedScopes != null && !failedScopes.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (String failed : failedScopes) builder.append(" ").append(failed);
                event.detail(Details.REASON, builder.toString());
                event.error(Errors.INVALID_SCOPE);
                return Response.status(401).entity(TokenReviewResponseRepresentation.error(Errors.INVALID_SCOPE)).type(MediaType.APPLICATION_JSON_TYPE).build();

            }
            if (!scopes.isEmpty()) userRep.putExtra("scopes.authorization.openshift.io", scopes);
        }

        // todo should scope this information to avoid leaking info
        // should only display what is allowed for client
        for (GroupModel group : user.getGroups()) {
            addTree(userRep, group);
        }

        event.success();
        return Response.ok(success, MediaType.APPLICATION_JSON).build();
    }

    public static void addTree(TokenReviewResponseRepresentation.Status.User user, GroupModel group) {
        if (group.getParentId() != null) {
            addTree(user, group.getParent());
        }

        String groupPath = buildOsinGroupPath(group);
        if (!user.getGroups().contains(groupPath)) user.getGroups().add(groupPath);
    }

    public static void buildOsinGroupPath(StringBuilder sb, GroupModel group) {
        if (group.getParent() != null) {
            buildOsinGroupPath(sb, group.getParent());
            sb.append(':');
        }
        sb.append(group.getName());
    }

    public static String buildOsinGroupPath(GroupModel group) {
        StringBuilder sb = new StringBuilder();
        buildOsinGroupPath(sb, group);
        return sb.toString();
    }

}
