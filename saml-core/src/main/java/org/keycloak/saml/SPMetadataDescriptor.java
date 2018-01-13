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

    public static String getSPDescriptor(String binding, String assertionEndpoint, String logoutEndpoint,
      boolean wantAuthnRequestsSigned, boolean wantAssertionsSigned, boolean wantAssertionsEncrypted,
      String entityId, String nameIDPolicyFormat, String signingCerts, String encryptionCerts) {
        String descriptor =
                "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"" + entityId + "\">\n" +
                "    <SPSSODescriptor AuthnRequestsSigned=\"" + wantAuthnRequestsSigned + "\" WantAssertionsSigned=\"" + wantAssertionsSigned + "\"\n" +
                "            protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">\n";
        if (wantAuthnRequestsSigned && signingCerts != null) {
            descriptor += signingCerts;
        }
        if (wantAssertionsEncrypted && encryptionCerts != null) {
            descriptor += encryptionCerts;
        }
        descriptor +=
                "        <SingleLogoutService Binding=\"" + binding + "\" Location=\"" + logoutEndpoint + "\"/>\n" +
                "        <NameIDFormat>" + nameIDPolicyFormat + "\n" +
                "        </NameIDFormat>\n" +
                "        <AssertionConsumerService\n" +
                "                Binding=\"" + binding + "\" Location=\"" + assertionEndpoint + "\"\n" +
                "                index=\"1\" isDefault=\"true\" />\n" +
                "    </SPSSODescriptor>\n" +
                "</EntityDescriptor>\n";
        return descriptor;
    }

    public static String xmlKeyInfo(String indentation, String keyId, String pemEncodedCertificate, String purpose, boolean declareDSigNamespace) {
        if (pemEncodedCertificate == null) {
            return "";
        }

        StringBuilder target = new StringBuilder()
          .append(indentation).append("<KeyDescriptor use=\"").append(purpose).append("\">\n")
          .append(indentation).append("  <dsig:KeyInfo").append(declareDSigNamespace ? " xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" : ">\n");

        if (keyId != null) {
            target.append(indentation).append("    <dsig:KeyName>").append(keyId).append("</dsig:KeyName>\n");
        }

        target
          .append(indentation).append("    <dsig:X509Data>\n")
          .append(indentation).append("      <dsig:X509Certificate>").append(pemEncodedCertificate).append("</dsig:X509Certificate>\n")
          .append(indentation).append("    </dsig:X509Data>\n")
          .append(indentation).append("  </dsig:KeyInfo>\n")
          .append(indentation).append("</KeyDescriptor>\n")
        ;

        return target.toString();
    }

}
