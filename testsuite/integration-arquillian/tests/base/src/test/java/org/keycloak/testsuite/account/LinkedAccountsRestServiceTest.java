/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.account;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.IdentityProviderBuilder;

import static org.junit.Assert.*;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.account.AccountLinkUriRepresentation;
import org.keycloak.representations.account.LinkedAccountRepresentation;
import org.keycloak.representations.account.LinkedAccountsRepresentation;

/**
 * @author <a href="mailto:ssilvert@redhat.com">Stan Silvert</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LinkedAccountsRestServiceTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.getUsers().add(UserBuilder.create().username("no-account-access").password("password").build());
        testRealm.getUsers().add(UserBuilder.create().username("view-account-access").role("account", "view-profile").password("password").build());
        
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                                              .providerId("github")
                                              .alias("github")
                                              .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                                              .providerId("saml")
                                              .alias("mysaml")
                                              .build());
        testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                                              .providerId("oidc")
                                              .alias("myoidc")
                                              .displayName("MyOIDC")
                                              .build());
        
        addGitHubIdentity(testRealm);
    }
    
    private void addGitHubIdentity(RealmRepresentation testRealm) {
        UserRepresentation acctMgtUser = findUser(testRealm, "test-user@localhost");
        
        FederatedIdentityRepresentation fedIdp = new FederatedIdentityRepresentation();
        fedIdp.setIdentityProvider("github");
        fedIdp.setUserId("foo");
        fedIdp.setUserName("foo");

        ArrayList<FederatedIdentityRepresentation> fedIdps = new ArrayList<>();
        fedIdps.add(fedIdp);
        
        acctMgtUser.setFederatedIdentities(fedIdps);
    }
    
    private UserRepresentation findUser(RealmRepresentation testRealm, String userName) {
        for (UserRepresentation user : testRealm.getUsers()) {
            if (user.getUsername().equals(userName)) return user;
        }
        
        return null;
    }

    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }
    
    private LinkedAccountsRepresentation linkedAccountsRep() throws IOException {
        return SimpleHttp.doGet(getAccountUrl("linked-accounts"), client).auth(tokenUtil.getToken()).asJson(new TypeReference<LinkedAccountsRepresentation>() {});
    }
    
    private LinkedAccountRepresentation findLinkedAccount(String providerId) throws IOException {
        for (LinkedAccountRepresentation account : linkedAccountsRep().getLinkedAccounts()) {
            if (account.getProviderId().equals(providerId)) return account;
        }
        
        return null;
    }
    
    @Test
    public void testBuildLinkedAccountUri() throws IOException {
        AccountLinkUriRepresentation rep = SimpleHttp.doGet(getAccountUrl("linked-accounts/github?redirectUri=phonyUri"), client)
                                       .auth(tokenUtil.getToken())
                                       .asJson(new TypeReference<AccountLinkUriRepresentation>() {});
        URI brokerUri = rep.getAccountLinkUri();
        
        assertTrue(brokerUri.getPath().endsWith("/auth/realms/test/broker/github/link"));
        
        List<NameValuePair> queryParams = URLEncodedUtils.parse(brokerUri, Charset.defaultCharset());
        assertEquals(4, queryParams.size());
        for (NameValuePair nvp : queryParams) {
            switch (nvp.getName()) {
                case "nonce" : { 
                    assertNotNull(nvp.getValue()); 
                    assertEquals(rep.getNonce(), nvp.getValue());
                    break;
                }
                case "hash" : {
                    assertNotNull(nvp.getValue());
                    assertEquals(rep.getHash(), nvp.getValue());
                    break;
                }
                case "client_id" : assertEquals("account", nvp.getValue()); break;
                case "redirect_uri" : assertEquals("phonyUri", nvp.getValue());
            }
        }
    }
    
    @Test
    public void testGetLinkedAccounts() throws IOException {
        LinkedAccountsRepresentation details = linkedAccountsRep();
        assertTrue(details.isHasAlternativeAuthentication());
        assertEquals(3, details.getLinkedAccounts().size());
        
        for (LinkedAccountRepresentation account : details.getLinkedAccounts()) {
            if (account.getProviderId().equals("github")) {
                assertTrue(account.isConnected());
            } else {
                assertFalse(account.isConnected());
            }
        }
    }
    
    @Test
    public void testRemoveLinkedAccount() throws IOException {
        assertTrue(findLinkedAccount("github").isConnected());
        SimpleHttp.doDelete(getAccountUrl("linked-accounts/github"), client).auth(tokenUtil.getToken()).acceptJson().asResponse();
        assertFalse(findLinkedAccount("github").isConnected());
    }
    
}
