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

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the schemas for PicketLink
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jun 30, 2011
 */
public class SchemaManagerUtil {

    public static List<String> getXMLSchemas() {
        List<String> list = new ArrayList<>();

        list.add("schema/w3c/xmlschema/xml.xsd");
        return list;
    }

    public static List<String> getXMLDSig() {
        List<String> list = new ArrayList<>();

        list.add("schema/w3c/xmldsig/xmldsig-core-schema.xsd");
        return list;
    }

    public static List<String> getXMLEnc() {
        List<String> list = new ArrayList<>();

        list.add("schema/w3c/xmlenc/xenc-schema.xsd");
        return list;
    }

    public static List<String> getXACMLSchemas() {
        List<String> list = new ArrayList<>();

        list.add("schema/xacml/access_control-xacml-2.0-policy-schema-os.xsd");
        list.add("schema/xacml/access_control-xacml-2.0-context-schema-os.xsd");
        return list;
    }

    public static List<String> getSAML2Schemas() {
        List<String> list = new ArrayList<>();

        list.add("schema/saml/v2/saml-schema-assertion-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-protocol-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-metadata-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-x500-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-authn-context-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-authn-context-types-2.0.xsd");
        list.add("schema/saml/v2/saml-schema-xacml-2.0.xsd");
        list.add("schema/saml/v2/access_control-xacml-2.0-saml-assertion-schema-os.xsd");
        list.add("schema/saml/v2/access_control-xacml-2.0-saml-protocol-schema-os.xsd");
        return list;
    }

    public static List<String> getSAML11Schemas() {
        List<String> list = new ArrayList<>();

        list.add("schema/saml/v1/saml-schema-assertion-1.0.xsd");
        list.add("schema/saml/v1/oasis-sstc-saml-schema-assertion-1.1.xsd");
        list.add("schema/saml/v1/saml-schema-protocol-1.1.xsd");
        return list;
    }

    public static List<String> getWSTrustSchemas() {
        List<String> list = new ArrayList<>();

        list.add("schema/wstrust/v1_3/ws-trust-1.3.xsd");
        list.add("schema/wstrust/v1_3/oasis-200401-wss-wssecurity-secext-1.0.xsd");
        list.add("schema/wstrust/v1_3/oasis-200401-wss-wssecurity-utility-1.0.xsd");
        list.add("schema/wstrust/v1_3/ws-policy.xsd");
        list.add("schema/wstrust/v1_3/ws-addr.xsd");
        return list;
    }

    public static List<String> getSchemas() {
        List<String> list = new ArrayList<>();
        list.addAll(getXMLSchemas());
        list.addAll(getXMLDSig());
        list.addAll(getXMLEnc());
        list.addAll(getSAML2Schemas());
        list.addAll(getSAML11Schemas());
        list.addAll(getXACMLSchemas());
        list.addAll(getWSTrustSchemas());
        return list;
    }
}