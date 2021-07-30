/*
 * Copyright 2016 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.authentication.authenticators.x509;

import java.security.cert.X509Certificate;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;

import static org.keycloak.authentication.authenticators.util.AuthenticatorUtils.getDisabledByBruteForceEventError;

/**
 * @author <a href="mailto:pnalyvayko@agi.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @date 7/31/2016
 */

public class ValidateX509CertificateUsername extends AbstractX509ClientCertificateDirectGrantAuthenticator {

    protected static ServicesLogger logger = ServicesLogger.LOGGER;

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        X509Certificate[] certs = getCertificateChain(context);
        if (certs == null || certs.length == 0) {
            logger.debug("[ValidateX509CertificateUsername:authenticate] x509 client certificate is not available for mutual SSL.");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", "X509 client certificate is missing.");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }

        saveX509CertificateAuditDataToAuthSession(context, certs[0]);
        recordX509CertificateAuditDataViaContextEvent(context);

        X509AuthenticatorConfigModel config = null;
        if (context.getAuthenticatorConfig() != null && context.getAuthenticatorConfig().getConfig() != null) {
            config = new X509AuthenticatorConfigModel(context.getAuthenticatorConfig());
        }
        if (config == null) {
            logger.warn("[ValidateX509CertificateUsername:authenticate] x509 Client Certificate Authentication configuration is not available.");
            context.getEvent().error(Errors.USER_NOT_FOUND);
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", "Configuration is missing.");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        // Validate X509 client certificate
        try {
            CertificateValidator.CertificateValidatorBuilder builder = certificateValidationParameters(context.getSession(), config);
            CertificateValidator validator = builder.build(certs);
            validator.checkRevocationStatus()
                    .validateKeyUsage()
                    .validateExtendedKeyUsage()
                    .validateTimestamps();
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            // TODO use specific locale to load error messages
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }

        Object userIdentity = getUserIdentityExtractor(config).extractUserIdentity(certs);
        if (userIdentity == null) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            logger.errorf("[ValidateX509CertificateUsername:authenticate] Unable to extract user identity from certificate.");
            // TODO use specific locale to load error messages
            String errorMessage = "Unable to extract user identity from specified certificate";
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", errorMessage);
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        UserModel user;
        try {
            context.getEvent().detail(Details.USERNAME, userIdentity.toString());
            context.getAuthenticationSession().setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, userIdentity.toString());
            user = getUserIdentityToModelMapper(config).find(context, userIdentity);
        }
        catch(ModelDuplicateException e) {
            logger.modelDuplicateException(e);
            String errorMessage = String.format("X509 certificate authentication's failed. Reason: \"%s\"", e.getMessage());
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", errorMessage);
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            String errorMessage = String.format("X509 certificate authentication's failed. Reason: \"%s\"", e.getMessage());
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_request", errorMessage);
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        if (user == null) {
            context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
            Response challengeResponse = errorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "invalid_grant", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }

        String bruteForceError = getDisabledByBruteForceEventError(context.getProtector(), context.getSession(), context.getRealm(), user);
        if (bruteForceError != null) {
            context.getEvent().user(user);
            context.getEvent().error(bruteForceError);
            Response challengeResponse = errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_grant", "Invalid user credentials");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }

        if (!user.isEnabled()) {
            context.getEvent().user(user);
            context.getEvent().error(Errors.USER_DISABLED);
            Response challengeResponse = errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_grant", "Account disabled");
            context.failure(AuthenticationFlowError.INVALID_USER, challengeResponse);
            return;
        }
        context.setUser(user);
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Intentionally does nothing
    }
}
