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

package org.keycloak.adapters.saml.profile.ecp;

import org.keycloak.adapters.saml.AbstractInitiateLogin;
import org.keycloak.adapters.saml.OnSessionCreated;
import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.adapters.saml.SamlSessionStore;
import org.keycloak.adapters.saml.profile.AbstractSamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.SamlAuthenticationHandler;
import org.keycloak.adapters.saml.profile.SamlInvocationContext;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.PostBindingUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class EcpAuthenticationHandler extends AbstractSamlAuthenticationHandler {

    public static final String PAOS_HEADER = "PAOS";
    public static final String PAOS_CONTENT_TYPE = "application/vnd.paos+xml";
    private static final String NS_PREFIX_PROFILE_ECP = "ecp";
    private static final String NS_PREFIX_SAML_PROTOCOL = "samlp";
    private static final String NS_PREFIX_SAML_ASSERTION = "saml";
    private static final String NS_PREFIX_PAOS_BINDING = "paos";

    public static boolean canHandle(HttpFacade httpFacade) {
        HttpFacade.Request request = httpFacade.getRequest();
        String acceptHeader = request.getHeader("Accept");
        String contentTypeHeader = request.getHeader("Content-Type");

        return (acceptHeader != null && acceptHeader.contains(PAOS_CONTENT_TYPE) && request.getHeader(PAOS_HEADER) != null)
                || (contentTypeHeader != null && contentTypeHeader.contains(PAOS_CONTENT_TYPE));
    }

    public static SamlAuthenticationHandler create(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        return new EcpAuthenticationHandler(facade, deployment, sessionStore);
    }

    private  EcpAuthenticationHandler(HttpFacade facade, SamlDeployment deployment, SamlSessionStore sessionStore) {
        super(facade, deployment, sessionStore);
    }

    @Override
    protected AuthOutcome logoutRequest(LogoutRequestType request, String relayState) {
        throw new RuntimeException("Not supported.");
    }


    @Override
    public AuthOutcome handle(OnSessionCreated onCreateSession) {
        String header = facade.getRequest().getHeader(PAOS_HEADER);

        if (header != null) {
            return doHandle(new SamlInvocationContext(), onCreateSession);
        } else {
            try {
                MessageFactory messageFactory = MessageFactory.newInstance();
                SOAPMessage soapMessage = messageFactory.createMessage(null, facade.getRequest().getInputStream());
                SOAPBody soapBody = soapMessage.getSOAPBody();
                Node authnRequestNode = soapBody.getFirstChild();
                Document document = DocumentUtil.createDocument();

                document.appendChild(document.importNode(authnRequestNode, true));

                String samlResponse = PostBindingUtil.base64Encode(DocumentUtil.asString(document));

                return doHandle(new SamlInvocationContext(null, samlResponse, null), onCreateSession);
            } catch (Exception e) {
                throw new RuntimeException("Error creating fault message.", e);
            }
        }
    }

    @Override
    protected AbstractInitiateLogin createChallenge() {
        return new AbstractInitiateLogin(deployment, sessionStore) {
            @Override
            protected void sendAuthnRequest(HttpFacade httpFacade, SAML2AuthnRequestBuilder authnRequestBuilder, BaseSAML2BindingBuilder binding) {
                try {
                    MessageFactory messageFactory = MessageFactory.newInstance();
                    SOAPMessage message = messageFactory.createMessage();

                    SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

                    envelope.addNamespaceDeclaration(NS_PREFIX_SAML_ASSERTION, JBossSAMLURIConstants.ASSERTION_NSURI.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_SAML_PROTOCOL, JBossSAMLURIConstants.PROTOCOL_NSURI.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_PAOS_BINDING, JBossSAMLURIConstants.PAOS_BINDING.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_PROFILE_ECP, JBossSAMLURIConstants.ECP_PROFILE.get());

                    createPaosRequestHeader(envelope);
                    createEcpRequestHeader(envelope);

                    SOAPBody body = envelope.getBody();

                    body.addDocument(binding.postBinding(authnRequestBuilder.toDocument()).getDocument());

                    message.writeTo(httpFacade.getResponse().getOutputStream());
                } catch (Exception e) {
                    throw new RuntimeException("Could not create AuthnRequest.", e);
                }
            }

            private void createEcpRequestHeader(SOAPEnvelope envelope) throws SOAPException {
                SOAPHeader headers = envelope.getHeader();
                SOAPHeaderElement ecpRequestHeader = headers.addHeaderElement(envelope.createQName(JBossSAMLConstants.REQUEST.get(), NS_PREFIX_PROFILE_ECP));

                ecpRequestHeader.setMustUnderstand(true);
                ecpRequestHeader.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                ecpRequestHeader.addAttribute(envelope.createName("ProviderName"), deployment.getEntityID());
                ecpRequestHeader.addAttribute(envelope.createName("IsPassive"), "0");
                ecpRequestHeader.addChildElement(envelope.createQName("Issuer", "saml")).setValue(deployment.getEntityID());
                ecpRequestHeader.addChildElement(envelope.createQName("IDPList", "samlp"))
                        .addChildElement(envelope.createQName("IDPEntry", "samlp"))
                        .addAttribute(envelope.createName("ProviderID"), deployment.getIDP().getEntityID())
                        .addAttribute(envelope.createName("Name"), deployment.getIDP().getEntityID())
                        .addAttribute(envelope.createName("Loc"), deployment.getIDP().getSingleSignOnService().getRequestBindingUrl());
            }

            private void createPaosRequestHeader(SOAPEnvelope envelope) throws SOAPException {
                SOAPHeader headers = envelope.getHeader();
                SOAPHeaderElement paosRequestHeader = headers.addHeaderElement(envelope.createQName(JBossSAMLConstants.REQUEST.get(), NS_PREFIX_PAOS_BINDING));

                paosRequestHeader.setMustUnderstand(true);
                paosRequestHeader.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                paosRequestHeader.addAttribute(envelope.createName("service"), JBossSAMLURIConstants.ECP_PROFILE.get());
                paosRequestHeader.addAttribute(envelope.createName("responseConsumerURL"), getResponseConsumerUrl());
            }

            private String getResponseConsumerUrl() {
                return (deployment.getIDP() == null
                  || deployment.getIDP().getSingleSignOnService() == null
                  || deployment.getIDP().getSingleSignOnService().getAssertionConsumerServiceUrl() == null
                ) ? null
                  : deployment.getIDP().getSingleSignOnService().getAssertionConsumerServiceUrl().toString();
            }
        };
    }
}