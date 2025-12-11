/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.federation.ldap;

import java.net.URI;
import java.util.UUID;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.authentication.authenticators.broker.IdpAutoLinkAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper;
import org.keycloak.broker.saml.mappers.UsernameTemplateMapper.Target;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.broker.KcSamlBrokerConfiguration;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.updaters.Creator;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_SAML_ALIAS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

/**
 *
 * @author hmlnarik
 */
public class LDAPSamlIdPInitiatedVaryingLetterCaseTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    private static final String USER_NAME_LDAP = "JdOe";
    private static final String USER_NAME_LOWERCASE = USER_NAME_LDAP.toLowerCase();
    private static final String USER_NAME_UPPERCASE = USER_NAME_LDAP.toUpperCase();
    private static final String USER_FIRST_NAME = "Joe";
    private static final String USER_LAST_NAME = "Doe";
    private static final String USER_PASSWORD = "P@ssw0rd!";
    private static final String USER_EMAIL = "jdoe@keycloak.org";
    private static final String USER_STREET = "Street";
    private static final String USER_POSTAL_CODE = "Post code";

    private static final String MY_APP = "myapp";
    private static final String EXT_SSO = "sso";
    private static final String EXT_SSO_URL = "http://localhost-" + EXT_SSO + ".localtest.me";
    private static final String DUMMY_URL = "http://localhost-" + EXT_SSO + "-dummy.localtest.me";
    private static final String FLOW_AUTO_LINK = "AutoLink";

    private String idpAlias;

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        getTestingClient().server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Delete all LDAP users
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);
            // Add some new LDAP users for testing
            LDAPObject user = LDAPTestUtils.addLDAPUser
            (
                ctx.getLdapProvider(),
                appRealm,
                USER_NAME_LDAP,
                USER_FIRST_NAME,
                USER_LAST_NAME,
                USER_EMAIL,
                USER_STREET,
                USER_POSTAL_CODE
            );
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), user, USER_PASSWORD);
        });

        ComponentRepresentation ldap = testRealm().components().query(null, "org.keycloak.storage.UserStorageProvider").get(0);
        ComponentRepresentation ldapMapper = new ComponentRepresentation();
        ldapMapper.setName("uid-to-user-attr-mapper");
        ldapMapper.setProviderId(UserAttributeLDAPStorageMapperFactory.PROVIDER_ID);
        ldapMapper.setProviderType("org.keycloak.storage.ldap.mappers.LDAPStorageMapper");
        ldapMapper.setParentId(ldap.getId());
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        config.add(UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE, "ldapUid");
        config.add(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE, "uid");
        config.add(UserAttributeLDAPStorageMapper.READ_ONLY, "true");
        config.add(UserAttributeLDAPStorageMapper.IS_MANDATORY_IN_LDAP, "true");
        ldapMapper.setConfig(config);
        testRealm().components().add(ldapMapper);
    }

    @Before
    public void setupIdentityProvider() {
        // Configure autolink flow
        AuthenticationFlowRepresentation newFlow = new AuthenticationFlowRepresentation();
        newFlow.setAlias(FLOW_AUTO_LINK);
        newFlow.setDescription("Auto-link flow");
        newFlow.setProviderId("basic-flow");
        newFlow.setBuiltIn(false);
        newFlow.setTopLevel(true);

        Creator.Flow amr = Creator.create(testRealm(), newFlow);

        AuthenticationExecutionInfoRepresentation exCreateUser = amr.addExecution(IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID);
        exCreateUser.setRequirement(Requirement.ALTERNATIVE.name());
        testRealm().flows().updateExecutions(FLOW_AUTO_LINK, exCreateUser);

        AuthenticationExecutionInfoRepresentation exAutoLink = amr.addExecution(IdpAutoLinkAuthenticatorFactory.PROVIDER_ID);
        exAutoLink.setRequirement(Requirement.ALTERNATIVE.name());
        testRealm().flows().updateExecutions(FLOW_AUTO_LINK, exAutoLink);
        getCleanup().addCleanup(amr);

        // Configure identity provider
        IdentityProviderRepresentation idp = KcSamlBrokerConfiguration.INSTANCE.setUpIdentityProvider();
        idp.getConfig().put(SAMLIdentityProviderConfig.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_UNSPECIFIED.get());
        idp.setFirstBrokerLoginFlowAlias(FLOW_AUTO_LINK);
        final Creator<IdentityProviderResource> idpCreator = Creator.create(testRealm(), idp);
        
        IdentityProviderMapperRepresentation samlNameIdMapper = new IdentityProviderMapperRepresentation();
        samlNameIdMapper.setName("username-nameid-mapper");
        idpAlias = idp.getAlias();
        samlNameIdMapper.setIdentityProviderAlias(idpAlias);
        samlNameIdMapper.setIdentityProviderMapper(UsernameTemplateMapper.PROVIDER_ID);
        samlNameIdMapper.setConfig(ImmutableMap.<String,String>builder()
            .put(IdentityProviderMapperModel.SYNC_MODE, "IMPORT")
            .put(UsernameTemplateMapper.TEMPLATE, "${NAMEID | lowercase}")
            .put(UsernameTemplateMapper.TARGET, Target.BROKER_ID.name())
            .build());
        idpCreator.resource().addMapper(samlNameIdMapper);

        getCleanup().addCleanup(idpCreator);
    }

    @Before
    public void setupClients() {
        getCleanup().addCleanup(Creator.create(testRealm(), ClientBuilder.create()
          .protocol(SamlProtocol.LOGIN_PROTOCOL)
          .clientId(EXT_SSO_URL)
          .baseUrl(EXT_SSO_URL)
          .attribute(SamlProtocol.SAML_IDP_INITIATED_SSO_URL_NAME, EXT_SSO)
          .attribute(SamlConfigAttributes.SAML_NAME_ID_FORMAT_ATTRIBUTE, JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get())
          .attribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, DUMMY_URL)
          .build())
        );

        getCleanup().addCleanup(Creator.create(testRealm(), ClientBuilder.create()
          .clientId(MY_APP)
          .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
          .baseUrl(oauth.APP_AUTH_ROOT)
          .build())
        );
    }

    @After
    public void cleanupUsers() {
        testRealm().userStorage().removeImportedUsers(ldapModelId);
    }

    @Test
    public void loginLDAPTest() {
        loginPage.open();
        loginPage.login(USER_NAME_LDAP, USER_PASSWORD);
        appPage.assertCurrent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
        String code = oauth.parseLoginResponse().getCode();
        String idTokenHint = oauth.doAccessTokenRequest(code).getIdToken();
        appPage.logout(idTokenHint);
    }

    protected URI getAuthServerBrokerSamlEndpoint(String realm, String identityProviderAlias, String samlClientId) throws IllegalArgumentException, UriBuilderException {
        return RealmsResource
                .realmBaseUrl(UriBuilder.fromUri(getAuthServerRoot()))
                .path("broker/{idp-name}/endpoint/clients/{client-id}")
                .build(realm, identityProviderAlias, samlClientId);
    }

    @Test
    public void idpInitiatedMatchCaseLDAPTest() throws Exception {
        testIdpInitiated(USER_NAME_LDAP, true);
    }

    @Test
    public void idpInitiatedUpperCaseLDAPTest() throws Exception {
        testIdpInitiated(USER_NAME_UPPERCASE, true);
    }

    @Test
    public void idpInitiatedLowerCaseLDAPTest() throws Exception {
        testIdpInitiated(USER_NAME_LOWERCASE, true);
    }

    @Test
    public void idpInitiatedVaryingLetterCasesLDAPTest() throws Exception {
        testIdpInitiated(USER_NAME_LDAP, true);
        testIdpInitiated(USER_NAME_UPPERCASE, false);
        testIdpInitiated(USER_NAME_LOWERCASE, false);
    }

    private void testIdpInitiated(String userName, boolean isFirstBrokerLogin) throws Exception {
        final URI destination = getAuthServerBrokerSamlEndpoint(TEST_REALM_NAME, IDP_SAML_ALIAS, EXT_SSO);
        ResponseType response = prepareResponseForIdPInitiatedFlow(destination, userName);

        final SamlClientBuilder builder = new SamlClientBuilder()
          // Create user session via IdP-initiated login
          .submitSamlDocument(destination, response, Binding.POST)
          .targetAttributeSamlResponse()
          .build();

        if (isFirstBrokerLogin) {
            builder
              // First-broker login
              .followOneRedirect()

              // After first-broker login
              .followOneRedirect();
        }

        builder
          // Do not truly process SAML POST response for a virtual IdP-initiated client, just check that no error was reported
          .processSamlResponse(Binding.POST)
            .transformObject(so -> {
                assertThat(so, Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));
                return null;
            })
            .build()

          // Now navigate to the application where the session should already be created
          .navigateTo(oauth.loginForm().build())

          .assertResponse(Matchers.bodyHC(containsString("AUTH_RESPONSE")))
          .execute();

        assertThat(testRealm().users().search(USER_NAME_LDAP, Boolean.TRUE), hasSize(1));
    }

    private ResponseType prepareResponseForIdPInitiatedFlow(final URI destination, String userName) throws ConfigurationException, ProcessingException {
        // Prepare Response for IdP-initiated flow
        return new SAML2LoginResponseBuilder()
          .requestID(UUID.randomUUID().toString())
          .destination(destination.toString())
          .issuer(EXT_SSO_URL)
          .requestIssuer(destination.toString())
          .assertionExpiration(1000000)
          .subjectExpiration(1000000)
          .sessionIndex("idp:" + UUID.randomUUID())
          .nameIdentifier(JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get(), userName)
          .buildModel();
    }

}
