package org.keycloak.dom.saml.v2.assertion;

import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedDataType;
import org.keycloak.dom.xmlsec.w3.xmlenc.EncryptedKeyType;

import java.util.ArrayList;
import java.util.List;

public class SAMLEncryptedType {
    private EncryptedDataType encryptedData;
    private List<EncryptedKeyType> encryptedKeys = new ArrayList<>();

    public EncryptedDataType getEncryptedData() {
        return this.encryptedData;
    }

    public void setEncryptedData(EncryptedDataType encryptedData) {
        this.encryptedData = encryptedData;
    }

    public List<EncryptedKeyType> getEncryptedKeys() {
        return this.encryptedKeys;
    }

    public void addEncryptedKey(EncryptedKeyType encryptedKey) {
        this.encryptedKeys.add(encryptedKey);
    }

    public void addEncryptedKeys(List<EncryptedKeyType> encryptedKeys) {
        this.encryptedKeys.addAll(encryptedKeys);
    }
}
