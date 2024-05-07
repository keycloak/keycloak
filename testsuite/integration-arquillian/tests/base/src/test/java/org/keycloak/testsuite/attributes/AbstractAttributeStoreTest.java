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
package org.keycloak.testsuite.attributes;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.keycloak.common.Profile;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.attributes.AttributeFederationProviderFactory;
import org.keycloak.storage.attributes.AttributeMapper;
import org.keycloak.storage.attributes.AttributeStoreProvider;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.userprofile.config.UPConfigUtils;

import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.*;
import java.util.stream.Collectors;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Abstract class for attribute store tests. Creates default attribute lookup realm, attribute profile, and test user
 * and group. Contains helpers for updating user attributes, and fetching/updating attribute store and attribute federation
 * provider instances.
 */
@EnableFeature(value = Profile.Feature.ATTRIBUTE_STORE, skipRestart = true)
@EnableFeature(value = Profile.Feature.SCRIPTS, skipRestart = true)
public abstract class AbstractAttributeStoreTest extends AbstractKeycloakTest {

    // configuration
    protected static final int MAX_RETRIES = 4;
    protected static final String ATTR_LOOKUP_REALM = "attr-lookup";
    protected static final String ATTR_FED_ID = KeycloakModelUtils.generateId();
    protected static final String ATTR_SYNC_GROUP = "test-group";
    protected static final String XASP_ATTRIBUTE = "x509-dn";

    protected static final String TEST_USER = "test-user";
    protected static final String TEST_USER_PASSWORD = "password";
    protected static final String TEST_USER_NOT_SYNCED = "test-user-not-synced";
    protected static final String TEST_USER_NOT_SYNCED_PASSWORD = "password";

    protected static final List<String> IGNORED_ATTRIBUTES = Arrays.asList("firstName", "lastName", "email", "username");

    @Page
    protected LoginPage loginPage;

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {}

    @Before
    public void initialize() {
        initAttrLookupRealm();
        initOauthClient();
    }

    @After
    public void cleanup(){
        realmsResouce().realm(ATTR_LOOKUP_REALM).remove();
    }

    /**
     * Helper function to initialize the oauth client for the attribute lookup realm
     */
    private void initOauthClient() {
        createAppClientInRealm(ATTR_LOOKUP_REALM);
        oauth.realm(ATTR_LOOKUP_REALM);
    }

    /**
     * Helper function to create the attribute lookup realm
     */
    private void initAttrLookupRealm() {
        // create the realm
        realmsResouce().create(new RealmRepresentation(){{
            setEnabled(true);
            setRealm(ATTR_LOOKUP_REALM);
        }});

        // create the attribute profile
        realmsResouce().realm(ATTR_LOOKUP_REALM).users().userProfile().update(createAttributeProfile("test-attr-1", "test-attr-2", XASP_ATTRIBUTE));

        // create the test group
        realmsResouce().realm(ATTR_LOOKUP_REALM).groups().add(GroupBuilder.create()
                .name(ATTR_SYNC_GROUP)
                .build());

        // create the test users
        realmsResouce().realm(ATTR_LOOKUP_REALM).users().create(UserBuilder.create()
                .username(TEST_USER)
                .firstName("test")
                .lastName("user")
                .email(TEST_USER + "@email.com")
                .password(TEST_USER_PASSWORD)
                .addGroups(ATTR_SYNC_GROUP)
                .enabled(true)
                .build());
        realmsResouce().realm(ATTR_LOOKUP_REALM).users().create(UserBuilder.create()
                .username(TEST_USER_NOT_SYNCED)
                .firstName("testuser")
                .lastName("notsynced")
                .email(TEST_USER_NOT_SYNCED + "@email.com")
                .password(TEST_USER_NOT_SYNCED_PASSWORD)
                .enabled(true)
                .build());
    }

    /**
     * Helper function to set an attribute on the specified user in the given realm
     * @param realm the realm
     * @param username the user to update
     * @param key the key of the attribute to set
     * @param value the value of the attribute to set
     */
    protected void setUserAttribute(String realm, String username, String key, String value){
        UserRepresentation user = adminClient.realm(realm).users().searchByUsername(username, true).stream().findFirst().orElseThrow().singleAttribute(key,value);
        adminClient.realm(realm).users().get(user.getId()).update(user);
    }

    /**
     * Helper function to update the attribute store provider with the given settings
     * @param realm the realm to update the provider in
     * @param settings the settings to update on the provider
     */
    protected void updateAttributeStoreProvider(String realm, Map<String, String> settings){
        // get the current configuration
        ComponentRepresentation component = getAttributeStoreProvider(realm);
        MultivaluedHashMap<String, String> config = component.getConfig();

        // update the config with the specified settings
        for (Map.Entry<String, String> e: settings.entrySet()){
            config.putSingle(e.getKey(), e.getValue());
        }

        // apply the updated component
        adminClient.realm(realm).components().component(component.getId()).update(component);
    }

    /**
     * Assert the attributes on the specified user match the expected attributes provided. Attributes configured as
     * ignored for this test are not checked.
     * @param realm the realm
     * @param username the user to check
     * @param expected the expected attributes on the user
     */
    protected void assertAttributes(String realm, String username, String... expected){
        UserRepresentation user = adminClient.realm(realm).users().searchByUsername(username, true).stream().findFirst().orElseThrow();

        // default user attributes to empty map if not set
        Map<String, List<String>> userAttrs;
        if ((userAttrs = user.getAttributes()) == null) {
            userAttrs = Collections.emptyMap();
        }

        // convert Map to MultiValuedHashMap
        MultivaluedHashMap<String, String> actual = new MultivaluedHashMap<>();
        actual.putAll(userAttrs);

        // remove default attributes
        actual.forEach((k,v) -> {
            if (IGNORED_ATTRIBUTES.contains(k)) {
                log.debugf("removing default attributes %s -> %s", k, v);
                actual.remove(k);
            }
        });
        log.infof("expected attributes (%s). actual attributes (%s)", Arrays.stream(expected).collect(Collectors.toList()), actual);

        Assert.assertMultivaluedMap(actual, expected);
        Assert.assertEquals(Arrays.stream(expected).count() / 2, actual.size());
    }

    /**
     * Helper function to generate a certificate for the given subject
     * @param subject the subject to set on the certificate
     * @return the generated certificate
     */
    protected static CertHolder generateCertificate(String subject){
        KeyPair key = KeyUtils.generateRsaKeyPair(4096);
        Certificate cert = CertificateUtils.generateV1SelfSignedCertificate(key, subject);
        return new CertHolder(key, cert);
    }

    /**
     * Helper function to create an attribute profile configuration with the specified attributes. All attributes default
     * to admin-only access
     * @param attributes the attributes to add to the profile
     * @return an attribute profile containing the specified attributes
     */
    protected UPConfig createAttributeProfile(String... attributes){
        UPConfig profile = UPConfigUtils.parseSystemDefaultConfig();
        Arrays.stream(attributes).forEach(attr -> profile.addOrReplaceAttribute(new UPAttribute(attr, new UPAttributePermissions(Set.of(ROLE_ADMIN), Set.of(ROLE_ADMIN)))));

        return profile;
    }

    /**
     * Helper function to get the first {@link AttributeStoreProvider} configured on the specified realm
     * @param realm the realm
     * @return the component representation of the {@link AttributeStoreProvider} instance
     */
    protected ComponentRepresentation getAttributeStoreProvider(String realm){
        return adminClient.realm(realm).components().query(adminClient.realm(realm).toRepresentation().getId(), AttributeStoreProvider.class.getName()).stream().findFirst().orElseThrow();
    }

    /**
     * Helper function to add an attribute mapper with the provided configuration. Automatically sets the parent as the
     * first attribute federation provider configured in the specified realm
     * @param realm the realm to add the mapper on
     * @param providerId the type of mapper to create
     * @param config the configuration for the mapper
     */
    protected void addAttributeFedMapper(String realm, String providerId, MultivaluedHashMap<String, String> config){
        ComponentRepresentation mapper = new ComponentRepresentation();
        mapper.setName("test");
        mapper.setParentId(getAttributeFedProvider(realm).getId());
        mapper.setProviderId(providerId);
        mapper.setProviderType(AttributeMapper.class.getName());
        mapper.setConfig(config);
        adminClient.realm(realm).components().add(mapper);
    }

    /**
     * Helper function to add an attribute store protocol mapper with the provided configuration to the given realm
     * @param realm the realm to add the mapper on
     * @param providerId the type of mapper to create
     * @param config the configuration for the mapper
     */
    protected void addAttributeStoreProtocolMapper(String realm, String providerId, Map<String, String> config){
        ClientScopeRepresentation scope = realmsResouce().realm(realm).clientScopes().findAll().stream().filter(s -> s.getName().equals("acr")).findFirst().orElseThrow();

        ProtocolMapperRepresentation protocolMapper = new ProtocolMapperRepresentation();
        protocolMapper.setName("test");
        protocolMapper.setProtocol("openid-connect");
        protocolMapper.setProtocolMapper(providerId);
        protocolMapper.setConfig(config);

        realmsResouce().realm(realm).clientScopes().get(scope.getId()).getProtocolMappers().createMapper(protocolMapper);
    }

    /**
     * Helper function to get the first {@link org.keycloak.storage.attributes.AttributeFederationProvider} configured on the specified realm
     * @param realm the realm
     * @return the component representation of the {@link org.keycloak.storage.attributes.AttributeFederationProvider} instance
     */
    protected ComponentRepresentation getAttributeFedProvider(String realm){
        return adminClient.realm(realm).components().query(adminClient.realm(realm).toRepresentation().getId(), UserStorageProvider.class.getName()).stream().findFirst().orElseThrow();
    }

    /**
     * Helper function to force an attribute sync for the given provider instance
     * @param realm the realm the attribute federation provider exists in
     * @param componentId the component ID of the attribute federation provider instance to trigger
     * @param full true if you want to perform a full since, otherwise perform a 'syncSince'
     */
    protected void triggerAttributeSync(String realm, String componentId, boolean full){
        String realmId = realmsResouce().realm(realm).toRepresentation().getId();
        String providerId = getAttributeFedProvider(realm).getProviderId();

        testingClient.server(realm).run(session -> {
            // fetch the component model and provider instance based on the provided componentId
            AttributeFederationProviderFactory factory = (AttributeFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, providerId);
            ComponentModel providerComp = session.getContext().getRealm().getComponent(componentId);
            UserStorageProviderModel provider = new UserStorageProviderModel(providerComp);

            // perform the sync
            if (full){
                retry(MAX_RETRIES, () -> factory.sync(session.getKeycloakSessionFactory(), realmId, provider));
            } else {
                retry(MAX_RETRIES, () -> factory.syncSince(new Date(), session.getKeycloakSessionFactory(), realmId, provider));
            }

        });
    }

    /**
     * Helper function to get the test sync group path
     * @return
     */
    protected String getAttrSyncGroupPath(){
        return realmsResouce().realm(ATTR_LOOKUP_REALM).groups().query(ATTR_SYNC_GROUP).stream().findFirst().orElseThrow().getPath();
    }

    /**
     * Helper function to retry the provided function a given number of times
     * @param maxRetries
     * @param func
     */
    protected static void retry(int maxRetries, Runnable func) {
        int retries = 0;
        while (retries < maxRetries-1){
            try {
                func.run();
                break;
            } catch (Exception | AssertionError e){
                retries += 1;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored){}
            }
        }
        func.run();
    }

    /**
     * Helper function to perform OIDC login and receive the access tokens
     * @param username the username to log in with
     * @param password the password of the user
     * @return the generated tokens resulting from the login
     */
    protected Tokens login(String username, String password){
        OAuthClient.AuthorizationEndpointResponse authResponse = oauth.doLogin(username, password);

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authResponse.getCode(), "password");
        Assert.assertEquals(200, tokenResponse.getStatusCode());

        IDToken idToken = oauth.verifyIDToken(tokenResponse.getIdToken());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());

        return new Tokens(idToken, tokenResponse.getIdToken(), accessToken, tokenResponse.getAccessToken());
    }

    /**
     * Helper function to assert that a claim is present in the provided token
     * @param token the token
     * @param claim the claim name
     * @param expected the expected value of the claim
     */
    protected void assertClaim(IDToken token, String claim, String expected) {
        String claimValue = (String) token.getOtherClaims().get(claim);
        Assert.assertNotNull(claimValue);
        Assert.assertEquals(expected, claimValue);
    }

    /**
     * Helper class to holder a key pair and associated certificate
     */
    protected static class CertHolder {
        public Certificate certificate;
        public KeyPair keyPair;

        public CertHolder(KeyPair keyPair, Certificate certificate){
            this.keyPair = keyPair;
            this.certificate = certificate;
        }
    }

    /**
     * Helper class to hold serialized and deserialized version of access and ID tokens
     */
    protected static class Tokens {
        IDToken idToken;
        AccessToken accessToken;
        String encodedIdToken;
        String encodedAccessToken;

        public Tokens(IDToken idToken, String encodedIdToken, AccessToken accessToken, String encodedAccessToken){
            this.idToken = idToken;
            this.encodedIdToken = encodedIdToken;
            this.accessToken = accessToken;
            this.encodedAccessToken = encodedAccessToken;
        }
    }
}