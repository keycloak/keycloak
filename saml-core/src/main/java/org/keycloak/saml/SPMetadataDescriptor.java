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

package org.keycloak.saml;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SPMetadataDescriptor {
    public static String getSPDescriptor(String binding, String assertionEndpoint, String logoutEndpoint, boolean wantAuthnRequestsSigned, String entityId, String nameIDPolicyFormat, String certificatePem) {
        String descriptor =
                "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + entityId + "\">\n" +
                "    <SPSSODescriptor AuthnRequestsSigned=\"" + wantAuthnRequestsSigned + "\"\n" +
                "            protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">\n";
        if (wantAuthnRequestsSigned) {
            descriptor +=
                    "        <KeyDescriptor use=\"signing\">\n" +
                            "            <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
                            "                <dsig:X509Data>\n" +
                            "                    <dsig:X509Certificate>\n" + certificatePem + "\n" +
                            "                    </dsig:X509Certificate>\n" +
                            "                </dsig:X509Data>\n" +
                            "            </dsig:KeyInfo>\n" +
                            "        </KeyDescriptor>\n";
        }
        descriptor +=
                "        <SingleLogoutService Binding=\"" + binding + "\" Location=\"" + logoutEndpoint + "\"/>\n" +
                "        <NameIDFormat>" + nameIDPolicyFormat + "\n" +
                "        </NameIDFormat>\n" +
                "        <AssertionConsumerService\n" +
                "                Binding=\"" + binding + "\" Location=\"" + assertionEndpoint + "\"\n" +
                "                index=\"1\" isDefault=\"true\" />\n";
        descriptor +=
                "    </SPSSODescriptor>\n" +
                "</EntityDescriptor>\n";
        return descriptor;
    }
}
