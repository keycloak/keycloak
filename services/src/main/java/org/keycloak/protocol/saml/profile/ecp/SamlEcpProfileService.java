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

package org.keycloak.protocol.saml.profile.ecp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import jakarta.ws.rs.core.Response;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeaderElement;

import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlClient;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.SAML2LogoutResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.w3c.dom.Document;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class SamlEcpProfileService extends SamlService {

    private static final String NS_PREFIX_PROFILE_ECP = "ecp";
    private static final String NS_PREFIX_SAML_PROTOCOL = "samlp";
    private static final String NS_PREFIX_SAML_ASSERTION = "saml";

    public SamlEcpProfileService(KeycloakSession session, EventBuilder event, DestinationValidator destinationValidator) {
        super(session, event, destinationValidator);
    }

    public Response authenticate(InputStream inputStream) {
        return authenticate(Soap.extractSoapMessage(inputStream));
    }

    public Response authenticate(Document soapMessage) {
        try {
            return new PostBindingProtocol() {

                @Override
                protected Response error(KeycloakSession session, AuthenticationSessionModel authenticationSession, Response.Status status, String message, Object... parameters) {
                    return Soap.createFault().code("error").reason(message).build();
                }

                @Override
                protected String getBindingType(AuthnRequestType requestAbstractType) {
                    return SamlProtocol.SAML_SOAP_BINDING;
                }

                @Override
                protected String getBindingType() {
                    return SamlProtocol.SAML_SOAP_BINDING;
                }

                @Override
                protected boolean isDestinationRequired() {
                    return false;
                }

                @Override
                protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType, ClientModel client) {
                    // Do not allow ECP login when client does not support it
                    if (!new SamlClient(client).allowECPFlow()) {
                        logger.errorf("Client %s is not allowed to execute ECP flow", client.getClientId());
                        throw new RuntimeException("Client is not allowed to use ECP profile.");
                    }

                    // force passive authentication when executing this profile
                    requestAbstractType.setIsPassive(true);
                    requestAbstractType.setDestination(session.getContext().getUri().getAbsolutePath());
                    return super.loginRequest(relayState, requestAbstractType, client);
                }
            }.execute(Soap.toSamlHttpPostMessage(soapMessage), null, null, null);
        } catch (Exception e) {
            String reason = "Some error occurred while processing the AuthnRequest.";
            String detail = e.getMessage();

            if (detail == null) {
                detail = reason;
            }

            return Soap.createFault().reason(reason).detail(detail).build();
        }
    }

    @Override
    protected Response newBrowserAuthentication(AuthenticationSessionModel authSession, boolean isPassive, boolean redirectToAuthentication, SamlProtocol samlProtocol) {
        // Saml ECP flow creates only TRANSIENT user sessions
        authSession.setClientNote(AuthenticationManager.USER_SESSION_PERSISTENT_STATE, UserSessionModel.SessionPersistenceState.TRANSIENT.toString());
        return super.newBrowserAuthentication(authSession, isPassive, redirectToAuthentication, createEcpSamlProtocol());
    }

    private SamlProtocol createEcpSamlProtocol() {
        return new SamlProtocol() {
            // method created to send a SOAP Binding response instead of a HTTP POST response
            @Override
            protected Response buildAuthenticatedResponse(AuthenticatedClientSessionModel clientSession, String redirectUri, Document samlDocument, JaxrsSAML2BindingBuilder bindingBuilder) throws ConfigurationException, ProcessingException, IOException {
                Document document = bindingBuilder.postBinding(samlDocument).getDocument();

                try {
                    Soap.SoapMessageBuilder messageBuilder = Soap.createMessage()
                            .addNamespace(NS_PREFIX_SAML_ASSERTION, JBossSAMLURIConstants.ASSERTION_NSURI.get())
                            .addNamespace(NS_PREFIX_SAML_PROTOCOL, JBossSAMLURIConstants.PROTOCOL_NSURI.get())
                            .addNamespace(NS_PREFIX_PROFILE_ECP, JBossSAMLURIConstants.ECP_PROFILE.get());

                    createEcpResponseHeader(redirectUri, messageBuilder);
                    createRequestAuthenticatedHeader(clientSession, messageBuilder);

                    messageBuilder.addToBody(document);

                    return messageBuilder.build();
                } catch (Exception e) {
                    throw new RuntimeException("Error while creating SAML response.", e);
                }
            }

            private void createRequestAuthenticatedHeader(AuthenticatedClientSessionModel clientSession, Soap.SoapMessageBuilder messageBuilder) {
                ClientModel client = clientSession.getClient();

                if ("true".equals(client.getAttribute(SamlConfigAttributes.SAML_CLIENT_SIGNATURE_ATTRIBUTE))) {
                    SOAPHeaderElement ecpRequestAuthenticated = messageBuilder.addHeader(JBossSAMLConstants.REQUEST_AUTHENTICATED.get(), NS_PREFIX_PROFILE_ECP);

                    ecpRequestAuthenticated.setMustUnderstand(true);
                    ecpRequestAuthenticated.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                }
            }

            private void createEcpResponseHeader(String redirectUri, Soap.SoapMessageBuilder messageBuilder) throws SOAPException {
                SOAPHeaderElement ecpResponseHeader = messageBuilder.addHeader(JBossSAMLConstants.RESPONSE__ECP.get(), NS_PREFIX_PROFILE_ECP);

                ecpResponseHeader.setMustUnderstand(true);
                ecpResponseHeader.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                ecpResponseHeader.addAttribute(messageBuilder.createName(JBossSAMLConstants.ASSERTION_CONSUMER_SERVICE_URL.get()), redirectUri);
            }

            @Override
            protected Response buildErrorResponse(boolean isPostBinding, String uri, JaxrsSAML2BindingBuilder binding, Document document) throws ConfigurationException, ProcessingException, IOException {
                return Soap.createMessage().addToBody(document).build();
            }

            @Override
            protected Response buildLogoutResponse(UserSessionModel userSession, String logoutBindingUri, SAML2LogoutResponseBuilder builder, JaxrsSAML2BindingBuilder binding) throws ConfigurationException, ProcessingException, IOException {
                return Soap.createFault().reason("Logout not supported.").build();
            }
        }.setEventBuilder(event).setHttpHeaders(headers).setRealm(realm).setSession(session).setUriInfo(session.getContext().getUri());
    }

    @Override
    protected AuthenticationFlowModel getAuthenticationFlow(AuthenticationSessionModel authSession) {
        return realm.getAuthenticationFlowsStream()
                .filter(flow -> Objects.equals(flow.getAlias(), DefaultAuthenticationFlows.SAML_ECP_FLOW))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not resolve authentication flow for SAML ECP Profile."));
    }
}
