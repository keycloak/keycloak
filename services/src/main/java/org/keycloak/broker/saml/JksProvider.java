package org.keycloak.broker.saml;

import org.keycloak.encryption.EncryptionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SAMLDecryptionKeysLocator;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.processing.core.saml.v2.util.DecryptionException;
import org.keycloak.saml.processing.core.util.XMLEncryptionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JksProvider implements EncryptionProvider {

    private final KeycloakSession session;
    private static final PicketLinkLogger logger = PicketLinkLoggerFactory.getLogger();

    public JksProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void encrypt(Document document) {

    }

    @Override
    public Element decrypt(Document documentWithEncryptedElement, RealmModel realm, String encryptionAlgorithm) throws DecryptionException {
        var samlDecryptionKeysLocator = new SAMLDecryptionKeysLocator(session, realm, encryptionAlgorithm);
        return XMLEncryptionUtil.decryptElementInDocument(documentWithEncryptedElement, samlDecryptionKeysLocator);
    }

    @Override
    public void close() {}
}
