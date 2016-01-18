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
                "            protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol urn:oasis:names:tc:SAML:1.1:protocol http://schemas.xmlsoap.org/ws/2003/07/secext\">\n" +
                "        <NameIDFormat>" + nameIDPolicyFormat + "\n" +
                "        </NameIDFormat>\n" +
                "        <SingleLogoutService Binding=\"" + binding + "\" Location=\"" + logoutEndpoint + "\"/>\n" +
                "        <AssertionConsumerService\n" +
                "                Binding=\"" + binding + "\" Location=\"" + assertionEndpoint + "\"\n" +
                "                index=\"1\" isDefault=\"true\" />\n";
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
                "    </SPSSODescriptor>\n" +
                "</EntityDescriptor>\n";
        return descriptor;
    }
}
