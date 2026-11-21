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

import net.jqwik.api.*;
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

import static org.junit.Assert.assertEquals;

/**
 * Property-based test for end-to-end assertion decryption via the SPI path.
 */
public class XMLEncryptionUtilSpiPropertyTest {

    private static final KeyPair RSA_2048;

    static {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            RSA_2048 = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA key pair for testing", e);
        }
    }

    /**
     * Verifies end-to-end assertion decryption preserves content.
     *
     * For any valid XML assertion content, encrypting it with a random AES key
     * (itself wrapped with an RSA public key) and then decrypting via
     * XMLEncryptionUtil.decryptElementInDocument() using the SPI provider path
     * produces a DOM element whose text content is identical to the original.
     */
    @Property(tries = 100)
    void endToEndDecryptionPreservesContent(
            @ForAll("validXmlTextContent") String originalContent,
            @ForAll("aesKeyBits") int aesKeySize) throws Exception {

        // Build a simple XML document with an element containing the text content
        String xmlString = "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
                + "<saml:Subject>" + escapeXml(originalContent) + "</saml:Subject>"
                + "</saml:Assertion>";
        Document document = DocumentUtil.getDocument(xmlString);

        // Generate AES key for content encryption
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(aesKeySize);
        SecretKey aesKey = keyGen.generateKey();

        // Encrypt the Subject element using XMLEncryptionUtil
        QName elementQName = new QName("urn:oasis:names:tc:SAML:2.0:assertion", "Subject", "saml");
        QName wrappingQName = new QName("urn:oasis:names:tc:SAML:2.0:assertion", "EncryptedData", "saml");

        XMLEncryptionUtil.encryptElement(elementQName, document, RSA_2048.getPublic(), aesKey,
                aesKeySize, wrappingQName, true);

        // Now set up the document for decryption: extract the EncryptedData wrapper
        Element encryptedWrapper = DocumentUtil.getElement(document,
                new QName("urn:oasis:names:tc:SAML:2.0:assertion", "EncryptedData", "saml"));

        Document encryptedDoc = DocumentUtil.createDocument();
        encryptedDoc.appendChild(encryptedDoc.importNode(encryptedWrapper, true));

        // Create a mock KeycloakSession that returns a DefaultSamlDecryptionProvider
        KeycloakSession mockSession = createMockSession(Collections.singletonList(RSA_2048.getPrivate()));

        // Decrypt using the SPI path
        Element decryptedElement = XMLEncryptionUtil.decryptElementInDocument(
                encryptedDoc,
                data -> Collections.singletonList(RSA_2048.getPrivate()),
                mockSession);

        // Verify the decrypted content matches the original
        String decryptedContent = decryptedElement.getTextContent();
        assertEquals(originalContent, decryptedContent);
    }

    @Provide
    Arbitrary<String> validXmlTextContent() {
        // Generate strings that are valid XML text content (no < > & characters, non-empty)
        return Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(200)
                .alpha()
                .numeric()
                .withChars(' ', '.', ',', '!', '?', '-', '_', ':', ';', '(', ')', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
                .filter(s -> !s.trim().isEmpty());
    }

    @Provide
    Arbitrary<Integer> aesKeyBits() {
        return Arbitraries.of(128, 192, 256);
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Creates a minimal KeycloakSession stub that returns a DefaultSamlDecryptionProvider
     * when getProvider(SamlDecryptionProvider.class) is called.
     */
    @SuppressWarnings("unchecked")
    private static KeycloakSession createMockSession(List<PrivateKey> privateKeys) {
        return new KeycloakSession() {
            private final Map<String, Object> attributes = new HashMap<>();

            @Override
            public <T extends Provider> T getProvider(Class<T> clazz) {
                if (clazz == SamlDecryptionProvider.class) {
                    return (T) new DefaultSamlDecryptionProvider(privateKeys);
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
