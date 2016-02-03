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

import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.util.StringUtil;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LSResource Resolver for schema validation
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 9, 2009
 */
public class IDFedLSInputResolver implements LSResourceResolver {

    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    private static Map<String, LSInput> lsmap = new HashMap<String, LSInput>();

    private static Map<String, String> schemaLocationMap = new LinkedHashMap<String, String>();

    static {
        // XML Schema/DTD
        schemaLocationMap.put("datatypes.dtd", "schema/w3c/xmlschema/datatypes.dtd");
        schemaLocationMap.put("XMLSchema.dtd", "schema/w3c/xmlschema/XMLSchema.dtd");
        schemaLocationMap.put("http://www.w3.org/2001/xml.xsd", "schema/w3c/xmlschema/xml.xsd");

        // XML DSIG
        schemaLocationMap.put("http://www.w3.org/2000/09/xmldsig#", "schema/w3c/xmldsig/xmldsig-core-schema.xsd");
        schemaLocationMap.put("http://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd",
                "schema/w3c/xmldsig/xmldsig-core-schema.xsd");

        // XML Enc
        schemaLocationMap.put("http://www.w3.org/2001/04/xmlenc#", "schema/w3c/xmlenc/xenc-schema.xsd");
        schemaLocationMap.put("http://www.w3.org/TR/2002/REC-xmlenc-core-20021210/xenc-schema.xsd",
                "schema/w3c/xmlenc/xenc-schema.xsd");

        // XACML
        schemaLocationMap.put("access_control-xacml-2.0-context-schema-os.xsd",
                "schema/access_control-xacml-2.0-context-schema-os.xsd");
        schemaLocationMap.put("access_control-xacml-2.0-policy-schema-os.xsd",
                "schema/access_control-xacml-2.0-policy-schema-os.xsd");

        // SAML

        schemaLocationMap.put("saml-schema-assertion-2.0.xsd", "schema/saml/v2/saml-schema-assertion-2.0.xsd");
        schemaLocationMap.put("saml-schema-protocol-2.0.xsd", "schema/saml/v2/saml-schema-protocol-2.0.xsd");
        schemaLocationMap.put("saml-schema-metadata-2.0.xsd", "schema/saml/v2/saml-schema-metadata-2.0.xsd");
        schemaLocationMap.put("saml-schema-x500-2.0.xsd", "schema/saml/v2/saml-schema-x500-2.0.xsd");
        schemaLocationMap.put("saml-schema-xacml-2.0.xsd", "schema/saml/v2/saml-schema-xacml-2.0.xsd");
        schemaLocationMap.put("saml-schema-xacml-2.0.xsd", "schema/saml/v2/saml-schema-xacml-2.0.xsd");
        schemaLocationMap.put("saml-schema-authn-context-2.0.xsd", "schema/saml/v2/saml-schema-authn-context-2.0.xsd");
        schemaLocationMap.put("saml-schema-authn-context-types-2.0.xsd",
                "schema/saml/v2/saml-schema-authn-context-types-2.0.xsd");

        schemaLocationMap.put("saml-schema-assertion-1.0.xsd", "schema/saml/v1/saml-schema-assertion-1.0.xsd");
        schemaLocationMap.put("oasis-sstc-saml-schema-assertion-1.1.xsd",
                "schema/saml/v1/oasis-sstc-saml-schema-assertion-1.1.xsd");
        schemaLocationMap.put("saml-schema-protocol-1.1.xsd", "schema/saml/v1/saml-schema-protocol-1.1.xsd");

        schemaLocationMap.put("access_control-xacml-2.0-saml-assertion-schema-os.xsd",
                "schema/saml/v2/access_control-xacml-2.0-saml-assertion-schema-os.xsd");

        schemaLocationMap.put("access_control-xacml-2.0-saml-protocol-schema-os.xsd",
                "schema/saml/v2/access_control-xacml-2.0-saml-protocol-schema-os.xsd");

        // WS-T
        schemaLocationMap.put("http://docs.oasis-open.org/ws-sx/ws-trust/200512", "schema/wstrust/v1_3/ws-trust-1.3.xsd");
        schemaLocationMap.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "schema/wstrust/v1_3/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        schemaLocationMap.put("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd",
                "schema/wstrust/v1_3/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        schemaLocationMap.put("http://schemas.xmlsoap.org/ws/2004/09/policy", "schema/wstrust/v1_3/ws-policy.xsd");
        schemaLocationMap.put("http://www.w3.org/2005/08/addressing", "schema/wstrust/v1_3/ws-addr.xsd");
    }

    public static Collection<String> schemas() {
        Collection<String> schemaValues = schemaLocationMap.values();
        schemaValues.remove("schema/w3c/xmlschema/datatypes.dtd");
        schemaValues.remove("schema/w3c/xmlschema/XMLSchema.dtd");
        logger.info("Considered the schemas:" + schemaValues);
        return schemaValues;
    }

    public LSInput resolveResource(String type, String namespaceURI, final String publicId, final String systemId,
                                   final String baseURI) {
        LSInput lsi = null;
        if (systemId == null)
            throw logger.nullValueError("systemid");
        if (StringUtil.isNotNull(systemId) && systemId.endsWith("dtd") && StringUtil.isNotNull(baseURI)) {
            lsi = lsmap.get(baseURI);
        }
        if (lsi == null)
            lsi = lsmap.get(systemId);
        if (lsi == null) {
            final String loc = schemaLocationMap.get(systemId);
            if (loc == null)
                return null;

            lsi = new PicketLinkLSInput(baseURI, loc, publicId, systemId);

            logger.trace("Loaded:" + lsi);

            lsmap.put(systemId, lsi);
        }
        return lsi;
    }

    public static class PicketLinkLSInput implements LSInput {

        private final String baseURI;

        private final String loc;

        private final String publicId;

        private final String systemId;

        public PicketLinkLSInput(String baseURI, String loc, String publicID, String systemID) {
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
                throw new RuntimeException(logger.classNotLoadedError(loc));
            }
            if (is == null)
                throw logger.nullValueError("inputstream is null for " + loc);
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
            return "PicketLinkLSInput [baseURI=" + baseURI + ", loc=" + loc + ", publicId=" + publicId + ", systemId="
                    + systemId + "]";
        }
    }
}