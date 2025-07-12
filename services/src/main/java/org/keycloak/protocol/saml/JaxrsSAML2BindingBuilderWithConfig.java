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

package org.keycloak.protocol.saml;

import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPEnvelope;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.jboss.logging.Logger;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.connections.httpclient.ProxyMappings;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.profile.util.Soap;
import org.keycloak.saml.BaseSAML2BindingBuilder;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.web.util.RedirectBindingUtil;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Document;

import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JaxrsSAML2BindingBuilderWithConfig extends BaseSAML2BindingBuilder<JaxrsSAML2BindingBuilderWithConfig> {

    protected static final Logger logger = Logger.getLogger(JaxrsSAML2BindingBuilderWithConfig.class);

    public static final String KEYSTORE_ALIAS = "key";

    private final KeycloakSession session;
    private final SAMLIdentityProviderConfig config;

    public JaxrsSAML2BindingBuilderWithConfig(KeycloakSession session, SAMLIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }

    public SAMLIdentityProviderConfig getConfig() {
        return this.config;
    }

    public class PostBindingBuilder extends BasePostBindingBuilder {

        private static final String HTTP_PROXY = "http_proxy";
        private static final String HTTPS_PROXY = "https_proxy";

        public PostBindingBuilder(JaxrsSAML2BindingBuilderWithConfig builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response request(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_REQUEST_KEY);
        }

        public Response response(String actionUrl) throws ConfigurationException, ProcessingException, IOException {
            return createResponse(actionUrl, GeneralConstants.SAML_RESPONSE_KEY);
        }

        private Response createResponse(String actionUrl, String key) throws ProcessingException, ConfigurationException, IOException {
            MultivaluedMap<String,String> formData = new MultivaluedHashMap<>();
            formData.add(GeneralConstants.URL, actionUrl);
            formData.add(key, BaseSAML2BindingBuilder.getSAMLResponse(document));

            if (this.getRelayState() != null) {
                formData.add(GeneralConstants.RELAY_STATE, this.getRelayState());
            }

            return session.getProvider(LoginFormsProvider.class).setFormData(formData).createSamlPostForm();
        }

        public String artifactResolutionRequest(URI artifactResolutionEndpoint, RealmModel realm) throws IOException {
            try {
                HttpClient httpClient = getHttpClient(realm);

                HttpPost post = createHttpPost(artifactResolutionEndpoint);
                HttpResponse httpResponse = httpClient.execute(post);

                return readHttpResponse(httpResponse, artifactResolutionEndpoint);
            } catch (GeneralSecurityException | SOAPException e) {
                logger.error("Error while getting ArtifactResponse. Returning empty response.", e);
            }
            return "";
        }

        private HttpClient getHttpClient(RealmModel realm) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
            if(getConfig().isMutualTLS()) {
                KeyWrapper rsaKey = session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.RS256);
                char[] keyStorePassword = new char[0];

                KeyStore keyStore = KeyStore.getInstance("JKS");
                keyStore.load(null, null);
                keyStore.setCertificateEntry(rsaKey.getKid(), rsaKey.getCertificate());
                Certificate[] chain = {rsaKey.getCertificate()};
                keyStore.setKeyEntry(KEYSTORE_ALIAS, rsaKey.getPrivateKey(), keyStorePassword, chain);

                SSLContext sslContext = SSLContexts.custom()
                        .loadKeyMaterial(keyStore, keyStorePassword, (map, socket) -> "key")
                        .build();

                SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                        new String[]{"TLSv1.3", "TLSv1.2", "TLSv1.1"},
                        null,
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());

                HttpClient httpClient = HttpClients.custom()
                        .setDefaultRequestConfig(buildRequestConfig())
                        .setSSLSocketFactory(sslConnectionSocketFactory)
                        .build();

                return httpClient;
            }
            return session.getProvider(HttpClientProvider.class).getHttpClient();
        }

        private RequestConfig buildRequestConfig() {
            RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD);
            HttpHost proxy = getProxy();
            if (proxy != null) {
                requestConfigBuilder.setProxy(proxy);
            }
            return requestConfigBuilder.build();
        }

        private HttpHost getProxy() {
            final String httpProxy = getHttpProxy();
            if (StringUtil.isBlank(httpProxy)) {
                return null;
            }
            return ProxyMappings.ProxyMapping.valueOf(".*" + ";" + httpProxy).getProxyHost();
        }

        private String getHttpProxy() {
            final String httpsProxy = getEnv(HTTPS_PROXY);
            if (StringUtil.isNotBlank(httpsProxy)) {
                return httpsProxy;
            }
            return getEnv(HTTP_PROXY);
        }

        private String getEnv(String name) {
            final String value = System.getenv(name.toLowerCase());
            if (StringUtil.isNotBlank(value)) {
                return value;
            }
            return System.getenv(name.toUpperCase());
        }

        private HttpPost createHttpPost(URI artifactResolutionEndpoint) throws ProcessingException, ConfigurationException, SOAPException, IOException {
            HttpPost post = new HttpPost(artifactResolutionEndpoint);
            String entity = DocumentUtil.getDocumentAsString(document);

            if (getConfig().isArtifactResolutionSOAP()) {
                entity = getSoapMessage(document);
            }
            if (getConfig().isArtifactResolutionWithXmlHeader()) {
                entity = "<?xml version=\"1.0\" encoding=\"" + getConfig().getCharSet().name() + "\"?>" + entity;
            }
            post.setEntity(new StringEntity(entity));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML_TYPE.withCharset(getConfig().getCharSet().name()).toString());
            post.setHeader(HttpHeaders.ACCEPT, MediaType.TEXT_XML_TYPE.withCharset(getConfig().getCharSet().name()).toString());
            return post;
        }

        private String getSoapMessage(Document document) throws SOAPException, IOException {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage message = factory.createMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
            envelope.addNamespaceDeclaration("xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);
            envelope.addNamespaceDeclaration("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            message.getSOAPHeader().detachNode();
            message.getSOAPBody().addDocument(document);
            message.writeTo(bos);
            return bos.toString();
        }
    }

    private String readHttpResponse(HttpResponse httpResponse, URI artifactResolutionEndpoint) throws IOException, ProcessingException {
        boolean wasBase64 = false;

        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (HttpStatus.SC_OK == statusCode) {
            InputStream content = httpResponse.getEntity().getContent();
            String response = new String(content.readAllBytes(), getConfig().getCharSet());

            if (isBase64String(response)) {
                wasBase64 = true;
                try (InputStream decodedStream = RedirectBindingUtil.base64DeflateDecode(response)) {
                    response = new String(decodedStream.readAllBytes());
                } catch (IOException e) {
                    throw new ProcessingException("Error decoding Base64 response", e);
                }
            }


            if (getConfig().isArtifactResolutionWithXmlHeader()) {
                response = response.replaceAll("<\\?xml(.+)\\?>", "");
            }
            if (getConfig().isArtifactResolutionSOAP()) {
                try {
                    SOAPMessage soapResponse = MessageFactory
                            .newInstance()
                            .createMessage(null, new ByteArrayInputStream(response.getBytes(getConfig().getCharSet())));
                    Document document = soapResponse.getSOAPBody().extractContentAsDocument();
                    response = DocumentUtil.getDocumentAsString(document);
//                  response = response.replaceAll("ec:", "");
                } catch (ConfigurationException | SOAPException e) {
                    logger.warn("ArtifactResponse not a valid SOAP message; was expecting one, ignoring for now.");
                }
            }


            if (wasBase64) {
                response = RedirectBindingUtil.base64Encode(response.getBytes(getConfig().getCharSet()));
            }
            return response;
        }else {
            logger.warnf("Response from endpoint (%s) was not 200 but %s. Returning empty response.", artifactResolutionEndpoint.toString(), statusCode);
        }
        return "";


    }

    private boolean isBase64String(String response) {
        return response.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
    }

    public static class RedirectBindingBuilder extends BaseRedirectBindingBuilder {
        public RedirectBindingBuilder(JaxrsSAML2BindingBuilderWithConfig builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response response(String redirectUri) throws ProcessingException, ConfigurationException, IOException {
            return response(redirectUri, false);
        }

        public Response request(String redirect) throws ProcessingException, ConfigurationException, IOException {
            return response(redirect, true);
        }

        private Response response(String redirectUri, boolean asRequest) throws ProcessingException, ConfigurationException, IOException {
            URI uri = generateURI(redirectUri, asRequest);
            logger.tracef("redirect-binding uri: %s", uri);
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            return Response.status(302).location(uri)
                    .header("Pragma", "no-cache")
                    .header("Cache-Control", "no-cache, no-store").build();
        }

    }

    public static class SoapBindingBuilder extends BaseSoapBindingBuilder {
        public SoapBindingBuilder(JaxrsSAML2BindingBuilderWithConfig builder, Document document) throws ProcessingException {
            super(builder, document);
        }

        public Response response() throws ConfigurationException, ProcessingException, IOException {
            try {
                Soap.SoapMessageBuilder messageBuilder = Soap.createMessage();
                messageBuilder.addToBody(document);
                return messageBuilder.build();
            } catch (Exception e) {
                throw new RuntimeException("Error while creating SAML response.", e);
            }
        }
    }

    @Override
    public RedirectBindingBuilder redirectBinding(Document document) throws ProcessingException  {
        return new RedirectBindingBuilder(this, document);
    }

    @Override
    public PostBindingBuilder postBinding(Document document) throws ProcessingException  {
        return new PostBindingBuilder(this, document);
    }

    @Override
    public SoapBindingBuilder soapBinding(Document document) throws ProcessingException {
        return new SoapBindingBuilder(this, document);
    }
}
