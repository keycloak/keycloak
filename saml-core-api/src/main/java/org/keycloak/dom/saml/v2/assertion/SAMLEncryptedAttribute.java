package org.keycloak.dom.saml.v2.assertion;

import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedDataType;

public class SAMLEncryptedAttribute extends SAMLEncryptedType {

    @Override
    public String toString() {
        StringBuilder xml = new StringBuilder();
        xml.append("<saml2:EncryptedAttribute>");
        xml.append(printEncryptedType());
        xml.append("</saml2:EncryptedAttribute>");
        return xml.toString();
    }

    private String printEncryptedType() {
        EncryptedDataType encryptedData = getEncryptedData();
        StringBuilder encryptedType = new StringBuilder();
        encryptedType.append("<xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\" ")
                .append("id=\"")
                .append(encryptedData.getId())
                .append("\"")
                .append(" Type=\"http://www.w3.org/2001/04/xmlenc#Element\">");
        encryptedType.append("</xenc:EncryptedData>");
        return encryptedType.toString();
    }
}
