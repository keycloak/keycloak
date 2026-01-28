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
package org.keycloak.testsuite.util.saml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;
import org.keycloak.dom.saml.v2.protocol.ArtifactResponseType;
import org.keycloak.dom.saml.v2.protocol.AttributeQueryType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.response.SAML2Response;
import org.keycloak.saml.processing.core.parsers.saml.protocol.SAMLProtocolQNames;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLRequestWriter;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLResponseWriter;
import org.keycloak.testsuite.util.SamlClient.Step;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.w3c.dom.Document;

/**
 *
 * @author hmlnarik
 */
public abstract class SamlDocumentStepBuilder<T extends SAML2Object, This extends SamlDocumentStepBuilder<T, This>> implements Step {

    private static final Logger LOG = Logger.getLogger(SamlDocumentStepBuilder.class);

    @FunctionalInterface
    public interface Saml2ObjectTransformer<T extends SAML2Object> {
        public T transform(T original) throws Exception;
    }

    @FunctionalInterface
    public interface Saml2DocumentTransformer {
        public Document transform(Document original) throws Exception;
    }

    @FunctionalInterface
    public interface StringTransformer {
        public String transform(String original) throws Exception;
    }

    private final SamlClientBuilder clientBuilder;

    private StringTransformer transformer = t -> t;

    public SamlDocumentStepBuilder(SamlClientBuilder clientBuilder) {
        this.clientBuilder = clientBuilder;
    }

    public This transformObject(Consumer<T> tr) {
        return transformObject(so -> {
            tr.accept(so);
            return so;
        });
    }

    @SuppressWarnings("unchecked")
    public This transformObject(Saml2ObjectTransformer<T> tr) {
        final StringTransformer original = this.transformer;
        this.transformer = s -> {
            final String originalTransformed = original.transform(s);

            if (originalTransformed == null) {
                return null;
            }

            final ByteArrayInputStream baos = new ByteArrayInputStream(originalTransformed.getBytes());
            final T saml2Object = (T) new SAML2Response().getSAML2ObjectFromStream(baos);
            final T transformed = tr.transform(saml2Object);

            if (transformed == null) {
                return null;
            }

            String res = saml2Object2String(transformed);
            LOG.debugf("  ---> %s", res);
            return res;
        };
        return (This) this;
    }

    public static String saml2Object2String(final SAML2Object transformed) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            XMLStreamWriter xmlStreamWriter = StaxUtil.getXMLStreamWriter(bos);
            
            if (transformed instanceof AuthnRequestType) {
                new SAMLRequestWriter(xmlStreamWriter).write((AuthnRequestType) transformed);
            } else if (transformed instanceof LogoutRequestType) {
                new SAMLRequestWriter(xmlStreamWriter).write((LogoutRequestType) transformed);
            } else if (transformed instanceof ArtifactResolveType) {
                new SAMLRequestWriter(xmlStreamWriter).write((ArtifactResolveType) transformed);
            } else if (transformed instanceof AttributeQueryType) {
                new SAMLRequestWriter(xmlStreamWriter).write((AttributeQueryType) transformed);
            } else if (transformed instanceof ResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((ResponseType) transformed);
            } else if (transformed instanceof ArtifactResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((ArtifactResponseType) transformed);
            } else if (transformed instanceof StatusResponseType) {
                new SAMLResponseWriter(xmlStreamWriter).write((StatusResponseType) transformed, SAMLProtocolQNames.LOGOUT_RESPONSE.getQName("samlp"));
            } else {
                Assert.assertNotNull("Unknown type: <null>", transformed);
                Assert.fail("Unknown type: " + transformed.getClass().getName());
            }
            return new String(bos.toByteArray(), GeneralConstants.SAML_CHARSET);
        } catch (ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public This transformDocument(Consumer<Document> tr) {
        return transformDocument(so -> {
            tr.accept(so);
            return so;
        });
    }

    @SuppressWarnings("unchecked")
    public This transformDocument(Saml2DocumentTransformer tr) {
        final StringTransformer original = this.transformer;
        this.transformer = s -> {
            final String originalTransformed = original.transform(s);

            if (originalTransformed == null) {
                return null;
            }

            final Document transformed = tr.transform(DocumentUtil.getDocument(originalTransformed));
            return transformed == null ? null : DocumentUtil.getDocumentAsString(transformed);
        };
        return (This) this;
    }

    public This transformString(Consumer<String> tr) {
        return transformString(s -> {
            tr.accept(s);
            return s;
        });
    }

    @SuppressWarnings("unchecked")
    public This transformString(StringTransformer tr) {
        final StringTransformer original = this.transformer;
        this.transformer = s -> {
            final String originalTransformed = original.transform(s);

            if (originalTransformed == null) {
                return null;
            }

            return tr.transform(originalTransformed);
        };
        return (This) this;
    }

    @SuppressWarnings("unchecked")
    public This apply(Consumer<This> updaterOfThis) {
        updaterOfThis.accept((This) this);
        return (This) this;
    }

    public SamlClientBuilder build() {
        return this.clientBuilder;
    }

    public StringTransformer getTransformer() {
        return transformer;
    }
}
