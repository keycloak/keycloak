package org.keycloak.encryption;

import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface EncryptionProvider extends Provider {

    void encrypt(Document document);

    Element decrypt(Document document, RealmModel realm, String encryptionAlg) throws Exception;
}
