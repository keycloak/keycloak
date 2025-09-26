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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.ConfigurableAuthenticatorFactory;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.util.Time;
import org.keycloak.dom.saml.v2.assertion.AuthnContextClassRefType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.StatementAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.RecoveryAuthnCodesCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static org.keycloak.provider.ProviderConfigProperty.INTEGER_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.MULTIVALUED_STRING_TYPE;
import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;
import static org.keycloak.services.messages.Messages.INVALID_AUTHENTICATOR_REFERENCE_NONE_VALID;
import static org.keycloak.services.messages.Messages.INVALID_AUTHENTICATOR_REFERENCE_SOME_VALID;

/**
 * Update the authn context in the SAML response based on the authenticators used in the user session.
 * <p>
 * Use it for example to pass on the information to Entra ID if a multifactor authentication was used.
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
public class AuthenticatorReferenceTypeToAuthnContextMapper extends AbstractSAMLProtocolMapper implements SAMLLoginResponseMapper {

    private static final Logger LOG = Logger.getLogger(AuthenticatorReferenceTypeToAuthnContextMapper.class);
    public static final String PROVIDER_ID = "authenticator-to-authn-context-mapper";
    public static final String SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN = "http://schemas.microsoft.com/claims/multipleauthn";
    public static final String CONFIG_AUTHN_CONTEXT = "authnContext";
    public static final String CONFIG_MAX_AGE = "maxAge";
    public static final String CONFIG_AUTHENTICATOR_REFERENCE_TYPES = "authenticatorReferenceTypes";
    private static final List<String> POTENTIAL_DEFAULT_AUTHENTICATOR_REFERENCES =
            List.of(OTPCredentialModel.TYPE, WebAuthnCredentialModel.TYPE_TWOFACTOR, WebAuthnCredentialModel.TYPE_PASSWORDLESS, RecoveryAuthnCodesCredentialModel.TYPE);
    private List<String> allAuthenticatorReferenceTypes;

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Authenticator reference type to AuthnContext";
    }

    @Override
    public String getDisplayCategory() {
        return AttributeStatementHelper.ATTRIBUTE_STATEMENT_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Update the SAML Authn context based on the used Authenticator reference category. For example, when '" + OTPCredentialModel.TYPE + "' is found, " +
                "set the Authn Context to the value expected by Entra ID for multi-factor authentication as '" + SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN + "'.";
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        allAuthenticatorReferenceTypes = factory.getProviderFactoriesStream(Authenticator.class)
                .filter(providerFactory -> (providerFactory instanceof ConfigurableAuthenticatorFactory))
                .map(providerFactory -> ((ConfigurableAuthenticatorFactory) providerFactory).getReferenceCategory())
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public ResponseType transformLoginResponse(ResponseType response, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        Set<String> authenticatorReferenceTypes = new HashSet<>(Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(mappingModel.getConfig().getOrDefault(CONFIG_AUTHENTICATOR_REFERENCE_TYPES, ""))));
        String configMaxAge = mappingModel.getConfig().get(CONFIG_MAX_AGE);
        Integer maxAge = StringUtil.isBlank(configMaxAge) ? null : Integer.parseInt(configMaxAge);
        String authnContext = mappingModel.getConfig().getOrDefault(CONFIG_AUTHN_CONTEXT, SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN);

        Map<String, Integer> executions = AuthenticatorUtils.parseCompletedExecutions(userSession.getNote(Constants.AUTHENTICATORS_COMPLETED));
        // From the authenticator executions...
        List<String> executedReferenceCategories = executions.entrySet().stream()
                // ... filter those recently executed
                .filter(e -> maxAge == null || e.getValue() + maxAge > Time.currentTime())
                // ... map executions to authenticators
                .map(s -> userSession.getRealm().getAuthenticationExecutionById(s.getKey()).getAuthenticator())
                .filter(Objects::nonNull)
                // ... map authenticators to reference category
                .map(a -> {
                    ProviderFactory<Authenticator> providerFactory = session.getKeycloakSessionFactory().getProviderFactory(Authenticator.class, a);
                    if (providerFactory instanceof ConfigurableAuthenticatorFactory caf) {
                        return caf.getReferenceCategory();
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        List<String> matchingCredentials = executedReferenceCategories.stream().filter(authenticatorReferenceTypes::contains).toList();
        if (!matchingCredentials.isEmpty()) {
            LOG.tracef("Found authenticators: %s", matchingCredentials);
            for (ResponseType.RTChoiceType assertion : response.getAssertions()) {
                for (StatementAbstractType statement : assertion.getAssertion().getStatements()) {
                    if (statement instanceof AuthnStatementType authnStatement) {
                        LOG.tracef("Authn Context set to: %s", authnContext);
                        authnStatement.getAuthnContext().getSequence().setClassRef(new AuthnContextClassRefType(URI.create(authnContext)));
                    }
                }
            }
        } else {
            LOG.debugf("No authenticator %s found in list: %s", authenticatorReferenceTypes, executedReferenceCategories);
        }
        return response;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        super.validateConfig(session, realm, client, mapperModel);
        String configReferenceTypes = mapperModel.getConfig().get(CONFIG_AUTHENTICATOR_REFERENCE_TYPES);
        if (configReferenceTypes == null) {
            // The frontend doesn't save the value if it is unchanged
            // https://github.com/keycloak/keycloak/issues/43949
            configReferenceTypes = getDefaultAuthenticators();
            mapperModel.getConfig().put(CONFIG_AUTHENTICATOR_REFERENCE_TYPES, configReferenceTypes);
        }
        if (configReferenceTypes != null) {
            String[] authenticators = Constants.CFG_DELIMITER_PATTERN.split(configReferenceTypes);
            List<String> unknownAuthenticators = Arrays.stream(authenticators)
                    .filter(a -> !allAuthenticatorReferenceTypes.contains(a))
                    .toList();
            if (!unknownAuthenticators.isEmpty()) {
                List<String> knownAuthenticators = Arrays.stream(authenticators)
                        .filter(a -> allAuthenticatorReferenceTypes.contains(a))
                        .toList();
                if (!knownAuthenticators.isEmpty()) {
                    String knownAuthenticatorsString = String.join(", ", knownAuthenticators.stream().map(v -> "'" + v + "'").toList());
                    // Avoid to reflect invalid values to the caller as it is a bad security practice to do so.
                    // Therefore, we're only returning the valid one.
                    throw new ProtocolMapperConfigException(INVALID_AUTHENTICATOR_REFERENCE_SOME_VALID, "Unknown authenticator reference types found.", knownAuthenticatorsString);
                } else {
                    throw new ProtocolMapperConfigException("Unknown authenticator reference types found.", INVALID_AUTHENTICATOR_REFERENCE_NONE_VALID);
                }
            }
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()

                .property()
                .name(CONFIG_AUTHENTICATOR_REFERENCE_TYPES)
                .label("Authenticator reference types")
                .helpText("Authenticator reference types where one of them must have been completed by the user. Available authenticator reference types: " + String.join(", ", allAuthenticatorReferenceTypes))
                .type(MULTIVALUED_STRING_TYPE)
                .defaultValue(getDefaultAuthenticators())
                .options(new ArrayList<>(allAuthenticatorReferenceTypes))
                .required(true)
                .add()

                .property()
                .name(CONFIG_MAX_AGE)
                .label("Maximum authentication age")
                .helpText("Maximum age of the successful authentication for the authenticator reference class. To use this option, update your authentication flow to ask again for the second factor credentials after a specified period,")
                .type(INTEGER_TYPE)
                .add()

                .property()
                .name(CONFIG_AUTHN_CONTEXT)
                .label("Authn Context")
                .helpText("Value set in the SAML response for the Authn Context. The expected value for Entra ID for multi-factor authentication is '\" + SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN + \"'")
                .type(STRING_TYPE)
                .defaultValue(SCHEMAS_MICROSOFT_COM_CLAIMS_MULTIPLEAUTHN)
                .required(true)
                .add()

                .build();
    }

    private String getDefaultAuthenticators() {
        return String.join(Constants.CFG_DELIMITER, POTENTIAL_DEFAULT_AUTHENTICATOR_REFERENCES.stream().sorted().filter(e -> allAuthenticatorReferenceTypes.stream().anyMatch(e::equals)).toList());
    }
}
