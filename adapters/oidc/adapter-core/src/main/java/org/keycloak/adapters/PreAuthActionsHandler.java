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

package org.keycloak.adapters;

import org.keycloak.adapters.authentication.ClientCredentialsProvider;
import org.keycloak.adapters.authentication.JWTClientCredentialsProvider;
import org.keycloak.adapters.rotation.AdapterRSATokenVerifier;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.UserSessionManagement;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.representations.VersionRepresentation;
import org.keycloak.representations.adapters.action.AdminAction;
import org.keycloak.representations.adapters.action.LogoutAction;
import org.keycloak.representations.adapters.action.PushNotBeforeAction;
import org.keycloak.representations.adapters.action.TestAvailabilityAction;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PreAuthActionsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PreAuthActionsHandler.class);

    protected UserSessionManagement userSessionManagement;
    protected AdapterDeploymentContext deploymentContext;
    protected KeycloakDeployment deployment;
    protected HttpFacade facade;

    public PreAuthActionsHandler(UserSessionManagement userSessionManagement, AdapterDeploymentContext deploymentContext, HttpFacade facade) {
        this.userSessionManagement = userSessionManagement;
        this.deploymentContext = deploymentContext;
        this.facade = facade;
    }

    protected boolean resolveDeployment() {
        deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isConfigured()) {
            LOG.warn("can't take request, adapter not configured");
            facade.getResponse().sendError(403, "adapter not configured");
            return false;
        }
        return true;
    }

    public boolean handleRequest() {
        String requestUri = facade.getRequest().getURI();
        LOG.debug("adminRequest {}", requestUri);
        if (preflightCors()) {
            return true;
        }
        if (requestUri.endsWith(AdapterConstants.K_LOGOUT)) {
            if (!resolveDeployment()) return true;
            handleLogout();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_PUSH_NOT_BEFORE)) {
            if (!resolveDeployment()) return true;
            handlePushNotBefore();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_VERSION)) {
            handleVersion();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_TEST_AVAILABLE)) {
            if (!resolveDeployment()) return true;
            handleTestAvailable();
            return true;
        } else if (requestUri.endsWith(AdapterConstants.K_JWKS)) {
            if (!resolveDeployment()) return true;
            handleJwksRequest();
            return true;
        }
        return false;
    }

    public boolean preflightCors() {
        // don't need to resolve deployment on cors requests.  Just need to know local cors config.
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (!deployment.isCors()) return false;
        LOG.debug("checkCorsPreflight {}", facade.getRequest().getURI());
        if (!facade.getRequest().getMethod().equalsIgnoreCase("OPTIONS")) {
            return false;
        }
        if (facade.getRequest().getHeader(CorsHeaders.ORIGIN) == null) {
            LOG.debug("checkCorsPreflight: no origin header");
            return false;
        }
        LOG.debug("Preflight request returning");
        facade.getResponse().setStatus(200);
        String origin = facade.getRequest().getHeader(CorsHeaders.ORIGIN);
        facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        String requestMethods = facade.getRequest().getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        if (requestMethods != null) {
            if (deployment.getCorsAllowedMethods() != null) {
                requestMethods = deployment.getCorsAllowedMethods();
            }
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_METHODS, requestMethods);
        }
        String allowHeaders = facade.getRequest().getHeader(CorsHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (allowHeaders != null) {
            if (deployment.getCorsAllowedHeaders() != null) {
                allowHeaders = deployment.getCorsAllowedHeaders();
            }
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowHeaders);
        }
        if (deployment.getCorsMaxAge() > -1) {
            facade.getResponse().setHeader(CorsHeaders.ACCESS_CONTROL_MAX_AGE, Integer.toString(deployment.getCorsMaxAge()));
        }
        return true;
    }

    protected void handleLogout()  {
        if (LOG.isTraceEnabled()) {
            LOG.trace("K_LOGOUT sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            LogoutAction action = JsonSerialization.readValue(token.getContent(), LogoutAction.class);
            if (!validateAction(action)) return;
            if (action.getAdapterSessionIds() != null) {
                userSessionManagement.logoutHttpSessions(action.getAdapterSessionIds());
            } else {
                LOG.debug("logout of all sessions for application '{}'", action.getResource());
                if (action.getNotBefore() > deployment.getNotBefore()) {
                    deployment.updateNotBefore(action.getNotBefore());
                }
                userSessionManagement.logoutAll();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    protected void handlePushNotBefore()  {
        if (LOG.isTraceEnabled()) {
            LOG.trace("K_PUSH_NOT_BEFORE sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            PushNotBeforeAction action = JsonSerialization.readValue(token.getContent(), PushNotBeforeAction.class);
            if (!validateAction(action)) return;
            deployment.updateNotBefore(action.getNotBefore());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleTestAvailable()  {
        if (LOG.isTraceEnabled()) {
            LOG.trace("K_TEST_AVAILABLE sent");
        }
        try {
            JWSInput token = verifyAdminRequest();
            if (token == null) {
                return;
            }
            TestAvailabilityAction action = JsonSerialization.readValue(token.getContent(), TestAvailabilityAction.class);
            validateAction(action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JWSInput verifyAdminRequest() throws Exception {
        if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            LOG.warn("SSL is required for adapter admin action");
            facade.getResponse().sendError(403, "ssl required");
            return null;
        }
        String token = StreamUtil.readString(facade.getRequest().getInputStream());
        if (token == null) {
            LOG.warn("admin request failed, no token");
            facade.getResponse().sendError(403, "no token");
            return null;
        }

        try {
            JWSInput input = new JWSInput(token);
            PublicKey publicKey = AdapterRSATokenVerifier.getPublicKey(input.getHeader().getKeyId(), deployment);
            if (RSAProvider.verify(input, publicKey)) {
                return input;
            }
        } catch (JWSInputException ignore) {
        }

        LOG.warn("admin request failed, unable to verify token");
        facade.getResponse().sendError(403, "no token");
        return null;
    }


    protected boolean validateAction(AdminAction action)  {
        if (!action.validate()) {
            LOG.warn("admin request failed, not validated" + action.getAction());
            facade.getResponse().sendError(400, "Not validated");
            return false;
        }
        if (action.isExpired()) {
            LOG.warn("admin request failed, expired token");
            facade.getResponse().sendError(400, "Expired token");
            return false;
        }
        if (!deployment.getResourceName().equals(action.getResource())) {
            LOG.warn("Resource name does not match");
            facade.getResponse().sendError(400, "Resource name does not match");
            return false;

        }
        return true;
    }

    protected void handleVersion()  {
        try {
            facade.getResponse().setStatus(200);
            facade.getResponse().setHeader("Content-Type", "application/json");
            JsonSerialization.writeValueToStream(facade.getResponse().getOutputStream(), VersionRepresentation.SINGLETON);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void handleJwksRequest() {
        try {
            JSONWebKeySet jwks = new JSONWebKeySet();
            ClientCredentialsProvider clientCredentialsProvider = deployment.getClientAuthenticator();

            // For now, just get signature key from JWT provider. We can add more if we support encryption etc.
            if (clientCredentialsProvider instanceof JWTClientCredentialsProvider) {
                PublicKey publicKey = ((JWTClientCredentialsProvider) clientCredentialsProvider).getPublicKey();
                JWK jwk = JWKBuilder.create().rs256(publicKey);
                jwks.setKeys(new JWK[] { jwk });
            } else {
                jwks.setKeys(new JWK[] {});
            }

            facade.getResponse().setStatus(200);
            facade.getResponse().setHeader("Content-Type", "application/json");
            JsonSerialization.writeValueToStream(facade.getResponse().getOutputStream(), jwks);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
