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

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.representations.account.AccountLinkUriRepresentation;
import org.keycloak.representations.account.LinkedAccountRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.IdentityProviderBuilder;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.keycloak.models.Constants.ACCOUNT_CONSOLE_CLIENT_ID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

        String[] providers = new String[]{"saml:mysaml:saml-idp", "oidc:myoidc:oidc-idp", "github", "gitlab", "twitter", "facebook", "bitbucket", "microsoft"};
        for (int i = 0; i < providers.length; i++) {
            String[] idpInfo = providers[i].split(":");
            testRealm.addIdentityProvider(IdentityProviderBuilder.create()
                    .providerId(idpInfo[0])
                    .alias(idpInfo.length == 1 ? idpInfo[0] : idpInfo[1])
                    .displayName(idpInfo.length == 1 ? null : idpInfo[2])
                    .setAttribute("guiOrder", String.valueOf(i))
                    .build());
        }

        addFederatedIdentities(testRealm, "github", "gitlab", "mysaml");
    }

    private void addFederatedIdentities(RealmRepresentation testRealm, String... idpAliases) {
        UserRepresentation acctMgtUser = findUser(testRealm, "test-user@localhost");
        if (acctMgtUser != null) {
            ArrayList<FederatedIdentityRepresentation> fedIdps = new ArrayList<>();
            for (String alias : idpAliases) {
                FederatedIdentityRepresentation fedIdp = new FederatedIdentityRepresentation();
                fedIdp.setIdentityProvider(alias);
                fedIdp.setUserId("foo");
                fedIdp.setUserName("foo");
                fedIdps.add(fedIdp);
            }
            acctMgtUser.setFederatedIdentities(fedIdps);
        }
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

    private List<LinkedAccountRepresentation> linkedAccountsRep() throws IOException {
		return linkedAccountsRep(null);
    }

    private List<LinkedAccountRepresentation> linkedAccountsRep(String params) throws IOException {
		String resource = Stream.of("linked-accounts", params)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("?"));

        return SimpleHttpDefault.doGet(getAccountUrl(resource), client).auth(tokenUtil.getToken()).asJson(new TypeReference<>() {});
    }

    private LinkedAccountRepresentation findLinkedAccount(String providerAlias) throws IOException {
        return findLinkedAccount(providerAlias, null);
    }

	private LinkedAccountRepresentation findLinkedAccount(String providerAlias, String params) throws IOException {
		for (LinkedAccountRepresentation account : linkedAccountsRep(params)) {
			if (account.getProviderAlias().equals(providerAlias)) return account;
		}

		return null;
	}

    @Test

    public void testBuildLinkedAccountUri() throws IOException {
        AccountLinkUriRepresentation rep = SimpleHttpDefault.doGet(getAccountUrl("linked-accounts/github?redirectUri=phonyUri"), client)
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
                case "client_id" : assertEquals(ACCOUNT_CONSOLE_CLIENT_ID, nvp.getValue()); break;
                case "redirect_uri" : assertEquals("phonyUri", nvp.getValue());
            }
        }
    }

    @Test
    public void testGetLinkedAccounts() throws IOException {
        List<LinkedAccountRepresentation> details = linkedAccountsRep();
        assertEquals(8, details.size());

        // test order of linked accounts
        List<String> linkedAccountAliases = details.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("mysaml", "myoidc", "github", "gitlab", "twitter", "facebook", "bitbucket", "microsoft"));

        List<String> expectedConnectedAccounts = List.of("github", "gitlab", "mysaml");
        for (LinkedAccountRepresentation account : details) {
            if (expectedConnectedAccounts.contains(account.getProviderAlias())) {
                assertTrue(account.isConnected());
            } else {
                assertFalse(account.isConnected());
            }
        }
    }

    @Test
    public void testGetLinkedAccountsWithPagination() throws IOException {

        // search only connected accounts, with a max result size of 10 - should fetch all connected accounts.
        List<LinkedAccountRepresentation> accounts = linkedAccountsRep("linked=true&first=0&max=10");
        assertEquals(3, accounts.size());

        List<String> linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("mysaml", "github", "gitlab"));
        for (LinkedAccountRepresentation account : accounts) {
            assertTrue(account.isConnected());
        }

        // same search, but testing the pagination.
        accounts = linkedAccountsRep("linked=true&first=0&max=2");
        assertEquals(2, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("mysaml","github"));

        accounts = linkedAccountsRep("linked=true&first=2&max=4");
        assertEquals(1, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("gitlab"));

        // now use a search string to further filter the results.
        accounts = linkedAccountsRep("linked=true&search=git*");
        assertEquals(2, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("github", "gitlab"));

        accounts = linkedAccountsRep("linked=true&search=*l-id*");
        assertEquals(1, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("mysaml"));

        // search unlinked identity providers.
        accounts = linkedAccountsRep("linked=false&first=0&max=10");
        assertEquals(5, accounts.size());

        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        // the unlinked accounts are ordered by alias, not gui order (this test needs to be adjusted if the model is fixed to order by gui order)
        assertThat(linkedAccountAliases, contains("bitbucket", "facebook", "microsoft", "myoidc", "twitter"));
        for (LinkedAccountRepresentation account : accounts) {
            assertFalse(account.isConnected());
        }

        // same search, but testing the pagination.
        accounts = linkedAccountsRep("linked=false&first=0&max=3");
        assertEquals(3, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("bitbucket", "facebook", "microsoft"));

        accounts = linkedAccountsRep("linked=false&first=3&max=3");
        assertEquals(2, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("myoidc", "twitter"));

        // now use a search string to filter the results.
        accounts = linkedAccountsRep("linked=false&search=*o*");
        assertEquals(3, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("facebook", "microsoft", "myoidc"));

        // finally use the search string with pagination.
        accounts = linkedAccountsRep("linked=false&search=*o*&first=1&max=1");
        assertEquals(1, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("microsoft"));

        //search based on display name
        accounts = linkedAccountsRep("linked=false&search=*c-id*");
        assertEquals(1, accounts.size());
        linkedAccountAliases = accounts.stream().map(LinkedAccountRepresentation::getProviderAlias).toList();
        assertThat(linkedAccountAliases, contains("myoidc"));
    }

    @Test
    public void testRemoveLinkedAccount() throws IOException {
        assertTrue(findLinkedAccount("github").isConnected());
        SimpleHttpDefault.doDelete(getAccountUrl("linked-accounts/github"), client).auth(tokenUtil.getToken()).acceptJson().asResponse();
        assertFalse(findLinkedAccount("github").isConnected());
    }

	@Test
	public void testIdentityProviderShowInAccountConsoleNull() throws IOException {
		// Linked
		assertNotNull(findLinkedAccount("github"));
		assertNotNull(findLinkedAccount("github", "linked=true"));
		assertNull(findLinkedAccount("github", "linked=false"));
		// Not linked
		assertNotNull(findLinkedAccount("twitter"));
		assertNull(findLinkedAccount("twitter", "linked=true"));
		assertNotNull(findLinkedAccount("twitter", "linked=false"));
	}

	@Test
	public void testIdentityProviderShowInAccountConsoleAlways() throws IOException {
		// Linked
		runUsingShowInAccountConsoleValue("github", "ALWAYS", () -> {
			assertNotNull(findLinkedAccount("github"));
			assertNotNull(findLinkedAccount("github", "linked=true"));
			assertNull(findLinkedAccount("github", "linked=false"));
		});
		// Not linked
		runUsingShowInAccountConsoleValue("twitter", "ALWAYS", () -> {
			assertNotNull(findLinkedAccount("twitter"));
			assertNull(findLinkedAccount("twitter", "linked=true"));
			assertNotNull(findLinkedAccount("twitter", "linked=false"));
		});
	}

	@Test
	public void testIdentityProviderShowInAccountConsoleWhenLinked() throws IOException {
		// Linked
		runUsingShowInAccountConsoleValue("github", "WHEN_LINKED", () -> {
			assertNotNull(findLinkedAccount("github"));
			assertNotNull(findLinkedAccount("github", "linked=true"));
			assertNull(findLinkedAccount("github", "linked=false"));
		});
		// Not linked
		runUsingShowInAccountConsoleValue("twitter", "WHEN_LINKED", () -> {
			assertNull(findLinkedAccount("twitter"));
			assertNull(findLinkedAccount("twitter", "linked=true"));
			assertNull(findLinkedAccount("twitter", "linked=false"));
		});
	}

	@Test
	public void testIdentityProviderShowInAccountConsoleNever() throws IOException {
		// Linked
		runUsingShowInAccountConsoleValue("github", "NEVER", () -> {
			assertNull(findLinkedAccount("github"));
			assertNull(findLinkedAccount("github", "linked=true"));
			assertNull(findLinkedAccount("github", "linked=false"));
		});
		// Not linked
		runUsingShowInAccountConsoleValue("twitter", "NEVER", () -> {
			assertNull(findLinkedAccount("twitter"));
			assertNull(findLinkedAccount("twitter", "linked=true"));
			assertNull(findLinkedAccount("twitter", "linked=false"));
		});
	}

	private void runUsingShowInAccountConsoleValue(String identityProviderAlias, String showInAccountConsoleValue, ThrowingRunnable runnable) throws IOException {
		IdentityProviderResource identityProviderResource = testRealm().identityProviders().get(identityProviderAlias);
		IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();
		String attribute = "showInAccountConsole";
		String genuineValue = representation.getConfig().get(attribute);
		representation.getConfig().put(attribute, showInAccountConsoleValue);
		identityProviderResource.update(representation);
		try {
			runnable.run();
		} finally {
			representation.getConfig().put(attribute, genuineValue);
			identityProviderResource.update(representation);
		}
	}

	private interface ThrowingRunnable {
		void run() throws IOException;
	}
}
