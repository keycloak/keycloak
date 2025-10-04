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

package org.keycloak.protocol.saml.mappers;

import org.jboss.logging.Logger;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pass on the information to EntraID if a multifactor authentication was used.
 * <p>
 * It caters for the special needs as described in
 * <a href="https://learn.microsoft.com/en-us/entra/identity/authentication/how-to-mfa-expected-inbound-assertions">Satisfy Microsoft Entra ID multifactor authentication (MFA) controls with MFA claims from a federated IdP</a>.
 * <p>
 * TL;DR: the following should be provided if multi-factor is used:
 * <p>
 * <pre>
 * {@code
 * <AuthnStatement AuthnInstant="2024-11-22T18:48:07.547Z">
 *     <AuthnContext>
 *         <AuthnContextClassRef>http://schemas.microsoft.com/claims/multipleauthn</AuthnContextClassRef>
 *     </AuthnContext>
 * </AuthnStatement>
 * }
 * </pre>
 *
 */
public class EntraIdMultiFactorAuthnContextMapper extends AbstractSAMLProtocolMapper implements SAMLLoginResponseMapper {

    private static final Logger LOG = Logger.getLogger(EntraIdMultiFactorAuthnContextMapper.class);
    public static final String PROVIDER_ID = "entraid-multifactor-authn-context-mapper";
    public static final String SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN = "http://schemas.microsoft.com/claims/multipleauthn";

    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "EntraID Multi-Factor Authn Context Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Pass on information about an active multi-factor authentication to EntraID.";
    }

    @Override
    public ResponseType transformLoginResponse(ResponseType response, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        Map<String, Integer> executions = AuthenticatorUtils.parseCompletedExecutions(userSession.getNote(Constants.AUTHENTICATORS_COMPLETED));
        // Check if multiple authenticators have been executed
        if (executions.size() >= 2) {
            // From the authenticator executions, filter those that are considered primary and second factor in authentication
            List<String> credentialAuthenticators = executions.keySet().stream().map(s -> userSession.getRealm().getAuthenticationExecutionById(s).getAuthenticator())
                    .filter(Objects::nonNull)
                    .filter(authenticator -> authenticator.equals(UsernamePasswordFormFactory.PROVIDER_ID) ||
                            session.getProvider(Authenticator.class, authenticator) instanceof CredentialValidator)
                    .toList();
            if (credentialAuthenticators.size() >= 2) {
                for (ResponseType.RTChoiceType assertion : response.getAssertions()) {
                    for (StatementAbstractType statement : assertion.getAssertion().getStatements()) {
                        if (statement instanceof AuthnStatementType authnStatement) {
                            authnStatement.getAuthnContext().getSequence().setClassRef(new AuthnContextClassRefType(URI.create(SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN)));
                        }
                    }
                }
            } else {
                LOG.debugf("Found the the following credential validators: %s", credentialAuthenticators);
            }
        }
        return response;
    }
}
