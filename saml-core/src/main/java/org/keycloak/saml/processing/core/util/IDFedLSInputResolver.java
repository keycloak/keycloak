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
package org.keycloak.saml.processing.core.util;

import org.keycloak.saml.common.util.SecurityActions;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * An LSResource Resolver for schema validation
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 9, 2009
 */
public class IDFedLSInputResolver implements LSResourceResolver {

    protected static final Logger logger = Logger.getLogger(IDFedLSInputResolver.class);

    private static final Map<String, String> schemaLocationMap;

    static {
        Map<String, String> schemaLocations = new LinkedHashMap<>();
        // XML Schema/DTD
        schemaLocations.put("datatypes.dtd", "schema/w3c/xmlschema/datatypes.dtd");
        schemaLocations.put("XMLSchema.dtd", "schema/w3c/xmlschema/XMLSchema.dtd");
        schemaLocations.put("http://www.w3.org/2001/XMLSchema.dtd", "schema/w3c/xmlschema/XMLSchema.dtd");
        schemaLocations.put("http://www.w3.org/2001/xml.xsd", "schema/w3c/xmlschema/xml.xsd");

        // XML DSIG
        schemaLocations.put("http://www.w3.org/2000/09/xmldsig#", "schema/w3c/xmldsig/xmldsig-core-schema.xsd");
        schemaLocations.put("http://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd",
                "schema/w3c/xmldsig/xmldsig-core-schema.xsd");
        schemaLocations.put("http://www.w3.org/TR/xmldsig-core/xmldsig-core-schema.xsd", "schema/w3c/xmldsig/xmldsig-core-schema.xsd");

        // XML Enc
        schemaLocations.put("http://www.w3.org/2001/04/xmlenc#", "schema/w3c/xmlenc/xenc-schema.xsd");
        schemaLocations.put("http://www.w3.org/TR/2002/REC-xmlenc-core-20021210/xenc-schema.xsd",
                "schema/w3c/xmlenc/xenc-schema.xsd");

        // XACML
        schemaLocations.put("access_control-xacml-2.0-context-schema-os.xsd",
                "schema/xacml/access_control-xacml-2.0-context-schema-os.xsd");
        schemaLocations.put("http://docs.oasis-open.org/xacml/2.0/access_control-xacml-2.0-context-schema-os.xsd",
                "schema/xacml/access_control-xacml-2.0-context-schema-os.xsd");
        schemaLocations.put("access_control-xacml-2.0-policy-schema-os.xsd",
                "schema/xacml/access_control-xacml-2.0-policy-schema-os.xsd");

        // SAML
        schemaLocations.put("saml-schema-assertion-2.0.xsd", "schema/saml/v2/saml-schema-assertion-2.0.xsd");
        schemaLocations.put("http://www.oasis-open.org/committees/download.php/11027/sstc-saml-schema-assertion-2.0.xsd", "schema/saml/v2/saml-schema-assertion-2.0.xsd");
        schemaLocations.put("saml-schema-protocol-2.0.xsd", "schema/saml/v2/saml-schema-protocol-2.0.xsd");
        schemaLocations.put("http://www.oasis-open.org/committees/download.php/11026/sstc-saml-schema-protocol-2.0.xsd", "schema/saml/v2/saml-schema-protocol-2.0.xsd");
        schemaLocations.put("saml-schema-metadata-2.0.xsd", "schema/saml/v2/saml-schema-metadata-2.0.xsd");
        schemaLocations.put("saml-schema-x500-2.0.xsd", "schema/saml/v2/saml-schema-x500-2.0.xsd");
        schemaLocations.put("saml-schema-xacml-2.0.xsd", "schema/saml/v2/saml-schema-xacml-2.0.xsd");
        schemaLocations.put("saml-schema-xacml-2.0.xsd", "schema/saml/v2/saml-schema-xacml-2.0.xsd");
        schemaLocations.put("saml-schema-authn-context-2.0.xsd", "schema/saml/v2/saml-schema-authn-context-2.0.xsd");
        schemaLocations.put("saml-schema-authn-context-types-2.0.xsd",
                "schema/saml/v2/saml-schema-authn-context-types-2.0.xsd");

        schemaLocations.put("saml-schema-assertion-1.0.xsd", "schema/saml/v1/saml-schema-assertion-1.0.xsd");
        schemaLocations.put("oasis-sstc-saml-schema-assertion-1.1.xsd",
                "schema/saml/v1/oasis-sstc-saml-schema-assertion-1.1.xsd");
        schemaLocations.put("saml-schema-protocol-1.1.xsd", "schema/saml/v1/saml-schema-protocol-1.1.xsd");

        schemaLocations.put("access_control-xacml-2.0-saml-assertion-schema-os.xsd",
                "schema/saml/v2/access_control-xacml-2.0-saml-assertion-schema-os.xsd");

        schemaLocations.put("access_control-xacml-2.0-saml-protocol-schema-os.xsd",
                "schema/saml/v2/access_control-xacml-2.0-saml-protocol-schema-os.xsd");


        // WS-T
        schemaLocations.put("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "schema/wstrust/v1_3/ws-trust-1.3.xsd");
        schemaLocations.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "schema/wstrust/v1_3/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        schemaLocations.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                "schema/wstrust/v1_3/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        schemaLocations.put("http://schemas.xmlsoap.org/ws/2004/09/policy", "schema/wstrust/v1_3/ws-policy.xsd");
        schemaLocations.put("http://schemas.xmlsoap.org/ws/2004/09/policy/ws-policy.xsd", "schema/wstrust/v1_3/ws-policy.xsd");
        schemaLocations.put("http://www.w3.org/2005/08/addressing", "schema/wstrust/v1_3/ws-addr.xsd");
        schemaLocations.put("http://www.w3.org/2006/03/addressing/ws-addr.xsd", "schema/wstrust/v1_3/ws-addr.xsd");
        schemaLocationMap = Collections.unmodifiableMap(schemaLocations);
    }

    public static Collection<String> schemas() {
        Collection<String> schemaValues = new HashSet<>(schemaLocationMap.values());
        schemaValues.remove("schema/w3c/xmlschema/datatypes.dtd");
        schemaValues.remove("schema/w3c/xmlschema/XMLSchema.dtd");
        logger.info("Considered the schemas:" + schemaValues);
        return schemaValues;
    }

    public IDFedLSInput resolveResource(String type, String namespaceURI, final String publicId, final String systemId, final String baseURI) {
        if (systemId == null) {
            throw new IllegalArgumentException("Expected systemId");
        }

        final String loc = schemaLocationMap.get(systemId);
        if (loc == null) {
            return null;
        }

        return new IDFedLSInput(baseURI, loc, publicId, systemId);
    }

    public static class IDFedLSInput implements LSInput {

        private final String baseURI;

        private final String loc;

        private final String publicId;

        private final String systemId;

        public IDFedLSInput(String baseURI, String loc, String publicID, String systemID) {
            this.baseURI = baseURI;
            this.loc = loc;
            this.publicId = publicID;
            this.systemId = systemID;
        }

        public String getBaseURI() {
            return baseURI;
        }

        public InputStream getByteStream() {
            URL url = SecurityActions.loadResource(getClass(), loc);
            InputStream is;
            try {
                is = url.openStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (is == null) {
                throw new RuntimeException("inputstream is null for " + loc);
            }
            return is;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getPublicId() {
            return publicId;
        }

        public String getStringData() {
            return null;
        }

        public String getSystemId() {
            return systemId;
        }

        public void setBaseURI(String baseURI) {
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setPublicId(String publicId) {
        }

        public void setStringData(String stringData) {
        }

        public void setSystemId(String systemId) {
        }

        @Override
        public String toString() {
            return "IDFedLSInput [baseURI=" + baseURI + ", loc=" + loc + ", publicId=" + publicId + ", systemId="
                    + systemId + "]";
        }
    }
}