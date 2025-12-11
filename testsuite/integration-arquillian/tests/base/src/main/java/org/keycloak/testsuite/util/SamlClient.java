/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPBody;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPHeader;
import jakarta.xml.soap.SOAPHeaderElement;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.soap.SOAPFaultException;

import org.keycloak.adapters.saml.SamlDeployment;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.RequestAbstractType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.protocol.saml.SamlProtocolUtils;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.SAMLRequestParser;
import org.keycloak.saml.SignatureAlgorithm;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.util.JAXPValidationUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.testsuite.util.saml.StepWithCheckers;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.keycloak.saml.common.constants.GeneralConstants.RELAY_STATE;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlUtils.getSamlDeploymentForClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

/**
 * @author hmlnarik
 */
public class SamlClient {

    @FunctionalInterface
    public interface Step {
        HttpUriRequest perform(CloseableHttpClient client, URI currentURI, CloseableHttpResponse currentResponse, HttpClientContext context) throws Exception;
    }

    @FunctionalInterface
    public interface ResultExtractor<T> {
        T extract(CloseableHttpResponse response) throws Exception;
    }

    public static final class DoNotFollowRedirectStep implements Step {

        @Override
        public HttpUriRequest perform(CloseableHttpClient client, URI uri, CloseableHttpResponse response, HttpClientContext context) throws Exception {
            return null;
        }
    }

    public static class RedirectStrategyWithSwitchableFollowRedirect extends LaxRedirectStrategy {

        public boolean redirectable = true;

        @Override
        protected boolean isRedirectable(String method) {
            return redirectable && super.isRedirectable(method);
        }

        public void setRedirectable(boolean redirectable) {
            this.redirectable = redirectable;
        }
    }

    /**
     * SAML bindings and related HttpClient methods.
     */
    public enum Binding {
        POST {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.OK));
                String responsePage = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                response.close();
                return extractSamlResponseFromForm(responsePage);
            }

            @Override
            public HttpPost createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY, null, null, null);
            }

            @Override
            public HttpPost createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_RESPONSE_KEY, null, null, null);
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return createSamlSignedResponse(samlEndpoint, relayState, samlRequest, realmPrivateKey, realmPublicKey, null);
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                return null;
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.OK));
                String responsePage = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                response.close();
                return extractSamlRelayStateFromForm(responsePage);
            }

            @Override
            public HttpPost createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return createSamlSignedRequest(samlEndpoint, relayState, samlRequest, realmPrivateKey, realmPublicKey, null);
            }

            @Override
            public HttpPost createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                return createSamlPostMessage(samlEndpoint, relayState, samlRequest, GeneralConstants.SAML_REQUEST_KEY, realmPrivateKey, realmPublicKey, certificateStr);
            }

            private HttpPost createSamlPostMessage(URI samlEndpoint, String relayState, Document samlRequest, String messageType, String privateKeyStr, String publicKeyStr, String certificateStr) {
                HttpPost post = new HttpPost(samlEndpoint);

                List<NameValuePair> parameters = new LinkedList<>();


                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

                    if (privateKeyStr != null && (publicKeyStr != null || certificateStr != null)) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKeyStr);
                        PublicKey publicKey = publicKeyStr != null? org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKeyStr) : null;
                        X509Certificate cert = org.keycloak.common.util.PemUtils.decodeCertificate(certificateStr);
                        if (publicKey == null) {
                            publicKey = cert.getPublicKey();
                        }
                        binding
                                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey, cert)
                                .signDocument();
                    }

                    parameters.add(
                            new BasicNameValuePair(messageType,
                                    binding
                                            .postBinding(samlRequest)
                                            .encoded())
                    );
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                if (relayState != null) {
                    parameters.add(new BasicNameValuePair(RELAY_STATE, relayState));
                }

                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
                post.setEntity(formEntity);

                return post;
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            }
        },

        REDIRECT {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.FOUND));
                String location = response.getFirstHeader("Location").getValue();
                response.close();
                return extractSamlResponseFromRedirect(location, realmPublicKey);
            }

            @Override
            public HttpGet createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                try {
                    URI requestURI = new BaseSAML2BindingBuilder()
                            .relayState(relayState)
                            .redirectBinding(samlRequest)
                            .requestURI(samlEndpoint.toString());
                    return new HttpGet(requestURI);
                } catch (ProcessingException | ConfigurationException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                try {
                    URI responseURI = new BaseSAML2BindingBuilder()
                            .relayState(relayState)
                            .redirectBinding(samlRequest)
                            .responseURI(samlEndpoint.toString());
                    return new HttpGet(responseURI);
                } catch (ProcessingException | ConfigurationException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return createSamlSignedResponse(samlEndpoint, relayState, samlRequest, realmPrivateKey, realmPublicKey, null);
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {

                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

                    if (realmPrivateKey != null && realmPublicKey != null) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(realmPrivateKey);
                        PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(realmPublicKey);
                        X509Certificate cert = org.keycloak.common.util.PemUtils.decodeCertificate(certificateStr);
                        binding
                                .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey, cert)
                                .signDocument();
                    }

                    binding.relayState(relayState);

                    return new HttpGet(binding.redirectBinding(samlRequest).responseURI(samlEndpoint.toString()));
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.FOUND));
                String location = response.getFirstHeader("Location").getValue();
                response.close();
                return extractRelayStateFromRedirect(location);
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String privateKeyStr, String publicKeyStr) {
                return createSamlSignedRequest(samlEndpoint, relayState, samlRequest, privateKeyStr, publicKeyStr, null);
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String privateKeyStr, String publicKeyStr, String certificateStr) {
                try {
                    BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder().relayState(relayState);
                    if (privateKeyStr != null && (publicKeyStr != null || certificateStr != null)) {
                        PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(privateKeyStr);
                        PublicKey publicKey = publicKeyStr != null? org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(publicKeyStr) : null;
                        X509Certificate cert = org.keycloak.common.util.PemUtils.decodeCertificate(certificateStr);
                        if (publicKey == null) {
                            publicKey = cert.getPublicKey();
                        }
                        binding.signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                                .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey, cert)
                                .signDocument();
                    }
                    return new HttpGet(binding.redirectBinding(samlRequest).requestURI(samlEndpoint.toString()));
                } catch (IOException | ConfigurationException | ProcessingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        },
        /**
         * SOAP binding is currently usable only with http://localhost:8280/ecp-sp/ client, see to-do comment within
         * {@link #createSamlSignedRequest} for more details. After resolving that to-do it should be usable with any
         * client.
         */
        SOAP {
            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {

                try {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        MessageFactory messageFactory = MessageFactory.newInstance();
                        SOAPMessage soapMessage = messageFactory.createMessage(null, response.getEntity().getContent());
                        SOAPBody soapBody = soapMessage.getSOAPBody();
                        Node authnRequestNode = soapBody.getFirstChild();
                        Document document = DocumentUtil.createDocument();
                        document.appendChild(document.importNode(authnRequestNode, true));

                        SAMLParser samlParser = SAMLParser.getInstance();
                        JAXPValidationUtil.checkSchemaValidation(document);

                        SAML2Object responseType = (SAML2Object) samlParser.parse(document);

                        return new SAMLDocumentHolder(responseType, document);

                    } else if (statusCode == 500) {
                        MessageFactory messageFactory = MessageFactory.newInstance();
                        SOAPMessage soapMessage = messageFactory.createMessage(null, response.getEntity().getContent());
                        SOAPBody soapBody = soapMessage.getSOAPBody();
                        throw new SOAPFaultException(soapBody.getFault());
                    } else {
                        throw new RuntimeException("Unexpected response status code (" + statusCode + ")");
                    }
                } catch (SOAPException | ConfigurationException | ProcessingException | ParsingException e) {
                    throw new RuntimeException(e);
                }
            }

            private static final String NS_PREFIX_PROFILE_ECP = "ecp";
            private static final String NS_PREFIX_SAML_PROTOCOL = "samlp";
            private static final String NS_PREFIX_SAML_ASSERTION = "saml";
            private static final String NS_PREFIX_PAOS_BINDING = "paos";

            private void createEcpRequestHeader(SOAPEnvelope envelope, SamlDeployment deployment) throws SOAPException {
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

            private void createPaosRequestHeader(SOAPEnvelope envelope, SamlDeployment deployment) throws SOAPException {
                SOAPHeader headers = envelope.getHeader();
                SOAPHeaderElement paosRequestHeader = headers.addHeaderElement(envelope.createQName(JBossSAMLConstants.REQUEST.get(), NS_PREFIX_PAOS_BINDING));

                paosRequestHeader.setMustUnderstand(true);
                paosRequestHeader.setActor("http://schemas.xmlsoap.org/soap/actor/next");
                paosRequestHeader.addAttribute(envelope.createName("service"), JBossSAMLURIConstants.ECP_PROFILE.get());
                paosRequestHeader.addAttribute(envelope.createName("responseConsumerURL"), getResponseConsumerUrl(deployment));
            }

            private String getResponseConsumerUrl(SamlDeployment deployment) {
                return (deployment.getIDP() == null
                        || deployment.getIDP().getSingleSignOnService() == null
                        || deployment.getIDP().getSingleSignOnService().getAssertionConsumerServiceUrl() == null
                ) ? null
                        : deployment.getIDP().getSingleSignOnService().getAssertionConsumerServiceUrl().toString();
            }

            @Override
            public HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return createSamlSignedRequest(samlEndpoint, relayState, samlRequest, null, null, null);
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return createSamlSignedRequest(samlEndpoint, relayState, samlRequest, realmPrivateKey, realmPublicKey, null);
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                BaseSAML2BindingBuilder binding = new BaseSAML2BindingBuilder();

                if (realmPrivateKey != null && realmPublicKey != null) {
                    PrivateKey privateKey = org.keycloak.testsuite.util.KeyUtils.privateKeyFromString(realmPrivateKey);
                    PublicKey publicKey = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(realmPublicKey);
                    X509Certificate cert = org.keycloak.common.util.PemUtils.decodeCertificate(certificateStr);
                    binding
                            .signatureAlgorithm(SignatureAlgorithm.RSA_SHA256)
                            .signWith(KeyUtils.createKeyId(privateKey), privateKey, publicKey, cert)
                            .signDocument();

                    try {
                        samlRequest = binding.postBinding(samlRequest).getDocument();
                    } catch (ProcessingException e) {
                        throw new RuntimeException(e);
                    }
                }

                MessageFactory messageFactory = null;
                try {

                    messageFactory = MessageFactory.newInstance();

                    SOAPMessage message = messageFactory.createMessage();

                    SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

                    envelope.addNamespaceDeclaration(NS_PREFIX_SAML_ASSERTION, JBossSAMLURIConstants.ASSERTION_NSURI.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_SAML_PROTOCOL, JBossSAMLURIConstants.PROTOCOL_NSURI.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_PAOS_BINDING, JBossSAMLURIConstants.PAOS_BINDING.get());
                    envelope.addNamespaceDeclaration(NS_PREFIX_PROFILE_ECP, JBossSAMLURIConstants.ECP_PROFILE.get());

                    SamlDeployment deployment = getSamlDeploymentForClient("ecp-sp"); // TODO: Make more general for any client, currently SOAP is usable only with http://localhost:8280/ecp-sp/ client

                    createPaosRequestHeader(envelope, deployment);
                    createEcpRequestHeader(envelope, deployment);

                    SOAPBody body = envelope.getBody();

                    body.addDocument(samlRequest);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    message.writeTo(outputStream);

                    HttpPost post = new HttpPost(samlEndpoint);
                    post.setEntity(new ByteArrayEntity(outputStream.toByteArray(), ContentType.TEXT_XML));
                    return post;
                } catch (SOAPException | IOException | ParsingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public URI getBindingUri() {
                return null;
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                return null;
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                return null;
            }
        },
        ARTIFACT_RESPONSE {
            private Document extractSoapMessage(CloseableHttpResponse response) throws IOException {
                ByteArrayInputStream bais = new ByteArrayInputStream(EntityUtils.toByteArray(response.getEntity()));
                Document soapBody = Soap.extractSoapMessage(bais);
                response.close();
                return soapBody;
            }

            @Override
            public SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException {
                assertThat(response, statusCodeIsHC(Response.Status.OK));
                Document soapBodyContents = extractSoapMessage(response);

                SAMLDocumentHolder samlDoc = null;
                try {
                    samlDoc = SAML2Request.getSAML2ObjectFromDocument(soapBodyContents);
                } catch (ProcessingException | ParsingException e) {
                    throw new RuntimeException("Unable to get documentHolder from soapBodyResponse: " + DocumentUtil.asString(soapBodyContents));
                }
                if (!(samlDoc.getSamlObject() instanceof ArtifactResponseType)) {
                    throw new RuntimeException("Message received from ArtifactResolveService is not an ArtifactResponseMessage");
                }

                ArtifactResponseType art = (ArtifactResponseType) samlDoc.getSamlObject();

                try {
                    Object artifactResponseContent = art.getAny();
                    if (artifactResponseContent instanceof ResponseType) {
                        Document doc = SAML2Request.convert((ResponseType) artifactResponseContent);
                        return new SAMLDocumentHolder((ResponseType) artifactResponseContent, doc);
                    } else if (artifactResponseContent instanceof RequestAbstractType) {
                        Document doc = SAML2Request.convert((RequestAbstractType) art.getAny());
                        return new SAMLDocumentHolder((RequestAbstractType) artifactResponseContent, doc);
                    } else {
                        throw new RuntimeException("Can not recognise message contained in ArtifactResponse");
                    }
                } catch (ParsingException | ConfigurationException | ProcessingException e) {
                    throw new RuntimeException("Can not obtain document from artifact response: " + DocumentUtil.asString(soapBodyContents));
                }
            }

            @Override
            public HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                return null;
            }

            @Override
            public URI getBindingUri() {
                return JBossSAMLURIConstants.SAML_HTTP_ARTIFACT_BINDING.getUri();
            }

            @Override
            public HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey) {
                return null;
            }

            @Override
            public HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr) {
                return null;
            }

            @Override
            public String extractRelayState(CloseableHttpResponse response) throws IOException {
                return null;
            }
        };

        public abstract SAMLDocumentHolder extractResponse(CloseableHttpResponse response, String realmPublicKey) throws IOException;

        public SAMLDocumentHolder extractResponse(CloseableHttpResponse response) throws IOException {
            return extractResponse(response, null);
        }

        public abstract HttpUriRequest createSamlUnsignedRequest(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey);

        public abstract HttpUriRequest createSamlSignedRequest(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr);

        public abstract URI getBindingUri();

        public abstract HttpUriRequest createSamlUnsignedResponse(URI samlEndpoint, String relayState, Document samlRequest);

        public abstract HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey);

        public abstract HttpUriRequest createSamlSignedResponse(URI samlEndpoint, String relayState, Document samlRequest, String realmPrivateKey, String realmPublicKey, String certificateStr);

        public abstract String extractRelayState(CloseableHttpResponse response) throws IOException;
    }

    private static final Logger LOG = Logger.getLogger(SamlClient.class);

    private final HttpClientContext context = HttpClientContext.create();

    private final RedirectStrategyWithSwitchableFollowRedirect strategy = new RedirectStrategyWithSwitchableFollowRedirect();

    /**
     * Extracts and parses value of SAMLResponse input field of a form present in the given page.
     *
     * @param responsePage HTML code of the page
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromForm(String responsePage) {
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements samlResponses = theResponsePage.select("input[name=SAMLResponse]");
        Elements samlRequests = theResponsePage.select("input[name=SAMLRequest]");
        int size = samlResponses.size() + samlRequests.size();
        assertThat("Checking uniqueness of SAMLResponse/SAMLRequest input field in the page", size, is(1));

        Element respElement = samlResponses.isEmpty() ? samlRequests.first() : samlResponses.first();

        return SAMLRequestParser.parseResponsePostBinding(respElement.val());
    }

    /**
     * Extracts the form element from a Post binding.
     *
     * @param responsePage HTML code in the page
     * @return The element that is the form
     */
    public static Element extractFormFromPostResponse(String responsePage) {
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements form = theResponsePage.select("form");
        assertThat("Checking uniqueness of SAMLResponse/SAMLRequest form in Post binding", form.size(), is(1));

        return form.first();
    }

    /**
     * Extracts and parses value of RelayState input field of a form present in the given page.
     *
     * @param responsePage HTML code of the page
     * @return
     */
    public static String extractSamlRelayStateFromForm(String responsePage) {
        assertThat(responsePage, containsString("form name=\"saml-post-binding\""));
        org.jsoup.nodes.Document theResponsePage = Jsoup.parse(responsePage);
        Elements samlRelayStates = theResponsePage.select("input[name=RelayState]");

        if (samlRelayStates.isEmpty()) return null;

        return samlRelayStates.first().val();
    }

    /**
     * Extracts and parses value of RelayState query parameter from the given URI.
     *
     * @param responseUri
     * @return
     */
    public static String extractRelayStateFromRedirect(String responseUri) {
        List<NameValuePair> params = URLEncodedUtils.parse(URI.create(responseUri), StandardCharsets.UTF_8);

        return params.stream().filter(nameValuePair -> nameValuePair.getName().equals(RELAY_STATE))
                .findFirst().map(NameValuePair::getValue).orElse(null);
    }

    public static MultivaluedMap<String, String> parseEncodedQueryParameters(String queryString) throws IOException {
        MultivaluedMap<String, String> encodedParams = new MultivaluedHashMap<>();
        if (queryString != null) {
            String[] params = queryString.split("&");
            for (String param : params) {
                if (param.indexOf('=') >= 0) {
                    String[] nv = param.split("=", 2);
                    encodedParams.add(RedirectBindingUtil.urlDecode(nv[0]), nv.length > 1 ? nv[1] : "");
                } else {
                    encodedParams.add(RedirectBindingUtil.urlDecode(param), "");
                }
            }
        }
        return encodedParams;
    }

    /**
     * Extracts and parses value of SAMLResponse query parameter from the given URI.
     * If the realmPublicKey parameter is passed the response signature is
     * validated.
     *
     * @param responseUri The redirect URI to use
     * @param realmPublicKey The public realm key for validating signature in REDIRECT query parameters
     * @return
     */
    public static SAMLDocumentHolder extractSamlResponseFromRedirect(String responseUri, String realmPublicKey) throws IOException {
        MultivaluedMap<String, String> encodedParams = parseEncodedQueryParameters(URI.create(responseUri).getRawQuery());

        String samlResponse = encodedParams.getFirst(GeneralConstants.SAML_RESPONSE_KEY);
        String samlRequest = encodedParams.getFirst(GeneralConstants.SAML_REQUEST_KEY);
        assertTrue("Only one SAMLRequest/SAMLResponse check", (samlResponse != null && samlRequest == null)
                || (samlResponse == null && samlRequest != null));

        String samlDoc = RedirectBindingUtil.urlDecode(samlResponse != null? samlResponse : samlRequest);
        SAMLDocumentHolder documentHolder = SAMLRequestParser.parseResponseRedirectBinding(samlDoc);

        if (realmPublicKey != null) {
            // if the public key is passed verify the signature of the redirect URI
            try {
                KeyLocator locator = new KeyLocator() {

                    private final Key key = org.keycloak.testsuite.util.KeyUtils.publicKeyFromString(realmPublicKey);

                    @Override
                    public Key getKey(String kid) throws KeyManagementException {
                        return this.key;
                    }

                    @Override
                    public Key getKey(Key key) throws KeyManagementException {
                        return this.key;
                    }

                    @Override
                    public void refreshKeyCache() {
                    }

                    @Override
                    public Iterator<Key> iterator() {
                        return Collections.singleton(this.key).iterator();
                    }
                };
                SamlProtocolUtils.verifyRedirectSignature(documentHolder, locator, encodedParams,
                        samlResponse != null? GeneralConstants.SAML_RESPONSE_KEY : GeneralConstants.SAML_REQUEST_KEY);
            } catch (VerificationException e) {
                throw new IOException(e);
            }
        }

        return documentHolder;
    }

    /**
     * Creates a SAML login request document with the given parameters. See SAML &lt;AuthnRequest&gt; description for more details.
     *
     * @param issuer
     * @param assertionConsumerURL
     * @param destination
     * @return
     */
    public static AuthnRequestType createLoginRequestDocument(String issuer, String assertionConsumerURL, URI destination) {
        try {
            SAML2Request samlReq = new SAML2Request();
            AuthnRequestType loginReq = samlReq.createAuthnRequestType(UUID.randomUUID().toString(), assertionConsumerURL,
              destination == null ? null : destination.toString(), issuer);

            return loginReq;
        } catch (ConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void execute(Step... steps) {
        executeAndTransform(resp -> null, Arrays.asList(steps));
    }

    public void execute(List<Step> steps) {
        executeAndTransform(resp -> null, steps);
    }

    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer, Step... steps) {
        return executeAndTransform(resultTransformer, Arrays.asList(steps));
    }

    public <T> T executeAndTransform(ResultExtractor<T> resultTransformer, List<Step> steps) {
        CloseableHttpResponse currentResponse = null;
        URI currentUri = URI.create("about:blank");
        strategy.setRedirectable(true);

        try (CloseableHttpClient client = createHttpClientBuilderInstance().setRedirectStrategy(strategy).build()) {
            for (int i = 0; i < steps.size(); i ++) {
                Step s = steps.get(i);
                LOG.infof("Running step %d: %s", i, s.getClass());

                CloseableHttpResponse origResponse = currentResponse;

                HttpUriRequest request = s.perform(client, currentUri, origResponse, context);
                if (request == null) {
                    LOG.info("Last step returned no request, continuing with next step.");
                    continue;
                }

                // Setting of follow redirects has to be set before executing the final request of the current step
                if (i < steps.size() - 1 && steps.get(i + 1) instanceof DoNotFollowRedirectStep) {
                    LOG.debugf("Disabling following redirects");
                    strategy.setRedirectable(false);
                    i++;
                } else {
                    strategy.setRedirectable(true);
                }

                LOG.infof("Executing HTTP request to %s", request.getURI());
                
                if (s instanceof StepWithCheckers) {
                    Runnable beforeChecker = ((StepWithCheckers) s).getBeforeStepChecker();
                    if (beforeChecker != null) beforeChecker.run();
                }

                currentResponse = client.execute(request, context);

                if (s instanceof StepWithCheckers) {
                    Runnable afterChecker = ((StepWithCheckers) s).getAfterStepChecker();
                    if (afterChecker != null) afterChecker.run();
                }

                currentUri = request.getURI();
                List<URI> locations = context.getRedirectLocations();
                if (locations != null && ! locations.isEmpty()) {
                    currentUri = locations.get(locations.size() - 1);
                }

                LOG.infof("Landed to %s", currentUri);

                if (currentResponse != origResponse && origResponse != null) {
                    origResponse.close();
                }
            }

            LOG.info("Going to extract response");

            return resultTransformer.extract(currentResponse);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public HttpClientContext getContext() {
        return context;
    }

    protected HttpClientBuilder createHttpClientBuilderInstance() {
        return HttpClientBuilder
                .create()
                .evictIdleConnections(100, TimeUnit.MILLISECONDS);
    }
}
