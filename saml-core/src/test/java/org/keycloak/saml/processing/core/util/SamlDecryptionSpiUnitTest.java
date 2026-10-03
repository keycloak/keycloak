/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.provider.InvalidationHandler.InvalidableObjectType;
import org.keycloak.provider.Provider;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.vault.VaultTranscriber;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.xml.namespace.QName;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Unit tests for SPI registration, factory, and fallback behavior.
 */
public class SamlDecryptionSpiUnitTest {

    private static KeyPair rsaKeyPair;

    @BeforeClass
    public static void setUp() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        rsaKeyPair = kpg.generateKeyPair();
    }

    // =========================================================================
    // SamlDecryptionSpi unit tests
    // =========================================================================

    @Test
    public void spiGetNameReturnsSamlDecryption() {
        SamlDecryptionSpi spi = new SamlDecryptionSpi();
        assertEquals("saml-decryption", spi.getName());
    }

    @Test
    public void spiGetProviderClassReturnsSamlDecryptionProvider() {
        SamlDecryptionSpi spi = new SamlDecryptionSpi();
        assertEquals(SamlDecryptionProvider.class, spi.getProviderClass());
    }

    @Test
    public void spiGetProviderFactoryClassReturnsSamlDecryptionProviderFactory() {
        SamlDecryptionSpi spi = new SamlDecryptionSpi();
        assertEquals(SamlDecryptionProviderFactory.class, spi.getProviderFactoryClass());
    }

    @Test
    public void spiIsInternalReturnsFalse() {
        SamlDecryptionSpi spi = new SamlDecryptionSpi();
        assertFalse(spi.isInternal());
    }

    // =========================================================================
    // DefaultSamlDecryptionProviderFactory unit tests
    // =========================================================================

    @Test
    public void factoryGetIdReturnsDefault() {
        DefaultSamlDecryptionProviderFactory factory = new DefaultSamlDecryptionProviderFactory();
        assertEquals("default", factory.getId());
    }

    @Test(expected = RuntimeException.class)
    public void factoryCreateThrowsWhenNoDecryptionKeyInSession() {
        DefaultSamlDecryptionProviderFactory factory = new DefaultSamlDecryptionProviderFactory();
        // Session with no attributes set - no decryption key available
        KeycloakSession emptySession = createMockSession(null);
        factory.create(emptySession);
    }

    @Test
    public void factoryCreateReturnsValidProviderWhenSingleKeyPresent() {
        DefaultSamlDecryptionProviderFactory factory = new DefaultSamlDecryptionProviderFactory();
        KeycloakSession session = createMockSession(null);
        session.setAttribute("saml.decryption.key", rsaKeyPair.getPrivate());

        SamlDecryptionProvider provider = factory.create(session);
        assertNotNull(provider);
    }

    @Test
    public void factoryCreateReturnsValidProviderWhenMultipleKeysPresent() {
        DefaultSamlDecryptionProviderFactory factory = new DefaultSamlDecryptionProviderFactory();
        KeycloakSession session = createMockSession(null);
        List<PrivateKey> keys = Collections.singletonList(rsaKeyPair.getPrivate());
        session.setAttribute("saml.decryption.keys", keys);

        SamlDecryptionProvider provider = factory.create(session);
        assertNotNull(provider);
    }

    @Test
    public void factoryCreatePrefersMultiKeysOverSingleKey() throws Exception {
        // Generate a second key pair — only rsaKeyPair can unwrap the wrapped key
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair secondKeyPair = kpg.generateKeyPair();

        // Wrap an AES key with rsaKeyPair's public key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();
        byte[] originalKeyBytes = aesKey.getEncoded();

        javax.crypto.Cipher wrapCipher = javax.crypto.Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
        wrapCipher.init(javax.crypto.Cipher.WRAP_MODE, rsaKeyPair.getPublic());
        byte[] wrappedBytes = wrapCipher.wrap(aesKey);

        DefaultSamlDecryptionProviderFactory factory = new DefaultSamlDecryptionProviderFactory();
        KeycloakSession session = createMockSession(null);

        // Set multi-key to rsaKeyPair (correct key), single-key to secondKeyPair (wrong key)
        List<PrivateKey> keys = Collections.singletonList(rsaKeyPair.getPrivate());
        session.setAttribute("saml.decryption.keys", keys);
        session.setAttribute("saml.decryption.key", secondKeyPair.getPrivate());

        SamlDecryptionProvider provider = factory.create(session);

        // If multi-key took precedence, unwrapping succeeds with the correct key
        byte[] unwrapped = provider.unwrapKey(
                org.apache.xml.security.encryption.XMLCipher.RSA_OAEP, wrappedBytes, null, null);
        assertArrayEquals(originalKeyBytes, unwrapped);
    }

    // =========================================================================
    // Fallback behavior unit tests
    // =========================================================================

    @Test
    public void decryptElementInDocumentWorksWithoutSession() throws Exception {
        // Test the legacy path: decryptElementInDocument(Document, DecryptionKeyLocator)
        // without any session (null session)
        Document encryptedDoc = createEncryptedDocument(rsaKeyPair);

        Element decrypted = XMLEncryptionUtil.decryptElementInDocument(
                encryptedDoc,
                data -> Collections.singletonList(rsaKeyPair.getPrivate()));

        assertNotNull(decrypted);
        assertEquals("TestContent", decrypted.getTextContent());
    }

    @Test
    public void decryptionSucceedsWhenNoSpiProviderRegistered() throws Exception {
        // Test graceful degradation: session is provided but getProvider returns null
        // Should fall back to DecryptionKeyLocator
        Document encryptedDoc = createEncryptedDocument(rsaKeyPair);

        // Create a session where getProvider(SamlDecryptionProvider.class) returns null
        KeycloakSession sessionWithNoProvider = createMockSession(null);

        Element decrypted = XMLEncryptionUtil.decryptElementInDocument(
                encryptedDoc,
                data -> Collections.singletonList(rsaKeyPair.getPrivate()),
                sessionWithNoProvider);

        assertNotNull(decrypted);
        assertEquals("TestContent", decrypted.getTextContent());
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Creates an encrypted XML document suitable for decryption testing.
     */
    private Document createEncryptedDocument(KeyPair keyPair) throws Exception {
        String xmlString = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
                + "<saml:Subject>TestContent</saml:Subject>"
                + "</saml:Assertion>";
        Document document = DocumentUtil.getDocument(xmlString);

        // Generate AES key for content encryption
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey aesKey = keyGen.generateKey();

        // Encrypt the Subject element
        QName elementQName = new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Subject", "saml");
        QName wrappingQName = new QName("urn:oasis:names:tc:SAML:2.0:assertion", "EncryptedData", "saml");

        XMLEncryptionUtil.encryptElement(elementQName, document, keyPair.getPublic(), aesKey,
                128, wrappingQName, true);

        // Extract the EncryptedData wrapper into its own document for decryption
        Element encryptedWrapper = DocumentUtil.getElement(document,
                new QName("urn:oasis:names:tc:SAML:2.0:assertion", "EncryptedData", "saml"));

        Document encryptedDoc = DocumentUtil.createDocument();
        encryptedDoc.appendChild(encryptedDoc.importNode(encryptedWrapper, true));

        return encryptedDoc;
    }

    /**
     * Creates a minimal KeycloakSession stub. When providerKeys is non-null,
     * getProvider(SamlDecryptionProvider.class) returns a DefaultSamlDecryptionProvider.
     * When providerKeys is null, getProvider returns null (simulating no SPI provider registered).
     */
    @SuppressWarnings("unchecked")
    private static KeycloakSession createMockSession(List<PrivateKey> providerKeys) {
        return new KeycloakSession() {
            private final Map<String, Object> attributes = new HashMap<>();

            @Override
            public <T extends Provider> T getProvider(Class<T> clazz) {
                if (clazz == SamlDecryptionProvider.class && providerKeys != null) {
                    return (T) new DefaultSamlDecryptionProvider(providerKeys);
                }
                return null;
            }

            @Override
            public <T extends Provider> T getProvider(Class<T> clazz, String id) { return null; }
            @Override
            public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId) { return null; }
            @Override
            public <T extends Provider> T getComponentProvider(Class<T> clazz, String componentId, Function<KeycloakSessionFactory, ComponentModel> modelGetter) { return null; }
            @Override
            public <T extends Provider> T getProvider(Class<T> clazz, ComponentModel componentModel) { return null; }
            @Override
            public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) { return Collections.emptySet(); }
            @Override
            public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) { return Collections.emptySet(); }
            @Override
            public Class<? extends Provider> getProviderClass(String providerClassName) { return null; }
            @Override
            public Object getAttribute(String attribute) { return attributes.get(attribute); }
            @Override
            public <T> T getAttribute(String attribute, Class<T> clazz) { return (T) attributes.get(attribute); }
            @Override
            public Object removeAttribute(String attribute) { return attributes.remove(attribute); }
            @Override
            public void setAttribute(String name, Object value) { attributes.put(name, value); }
            @Override
            public Map<String, Object> getAttributes() { return attributes; }
            @Override
            public void invalidate(InvalidableObjectType type, Object... params) {}
            @Override
            public void enlistForClose(Provider provider) {}
            @Override
            public KeycloakSessionFactory getKeycloakSessionFactory() { return null; }
            @Override
            public RealmProvider realms() { return null; }
            @Override
            public ClientProvider clients() { return null; }
            @Override
            public ClientScopeProvider clientScopes() { return null; }
            @Override
            public GroupProvider groups() { return null; }
            @Override
            public RoleProvider roles() { return null; }
            @Override
            public UserSessionProvider sessions() { return null; }
            @Override
            public UserLoginFailureProvider loginFailures() { return null; }
            @Override
            public AuthenticationSessionProvider authenticationSessions() { return null; }
            @Override
            public SingleUseObjectProvider singleUseObjects() { return null; }
            @Override
            public IdentityProviderStorageProvider identityProviders() { return null; }
            @Override
            public void close() {}
            @Override
            public UserProvider users() { return null; }
            @Override
            public KeyManager keys() { return null; }
            @Override
            public ThemeManager theme() { return null; }
            @Override
            public TokenManager tokens() { return null; }
            @Override
            public VaultTranscriber vault() { return null; }
            @Override
            public ClientPolicyManager clientPolicy() { return null; }
            @Override
            public boolean isClosed() { return false; }
            @Override
            public KeycloakContext getContext() { return null; }
            @Override
            public KeycloakTransactionManager getTransactionManager() { return null; }
        };
    }
}
