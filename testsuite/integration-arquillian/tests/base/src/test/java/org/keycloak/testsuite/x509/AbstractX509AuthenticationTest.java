/*
 * Copyright 2017 Analytical Graphics, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.x509;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.x509.ValidateX509CertificateUsernameFactory;
import org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel;
import org.keycloak.authentication.authenticators.x509.X509ClientCertificateAuthenticatorFactory;
import org.keycloak.common.util.Encode;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USERNAME_EMAIL;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.IdentityMapperType.USER_ATTRIBUTE;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.ISSUERDN_CN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_CN;
import static org.keycloak.authentication.authenticators.x509.X509AuthenticatorConfigModel.MappingSourceType.SUBJECTDN_EMAIL;

/**
 * @author <a href="mailto:brat000012001@gmail.com">Peter Nalyvayko</a>
 * @version $Revision: 1 $
 * @since 10/28/2016
 */

public abstract class AbstractX509AuthenticationTest extends AbstractTestRealmKeycloakTest {

    public static final String EMPTY_CRL_PATH = "empty.crl";
    public static final String CLIENT_CRL_PATH = "intermediate-ca.crl";
    protected final Logger log = Logger.getLogger(this.getClass());

    static final String REQUIRED = "REQUIRED";
    static final String OPTIONAL = "OPTIONAL";
    static final String DISABLED = "DISABLED";
    static final String ALTERNATIVE = "ALTERNATIVE";

    // TODO move to a base class
    public static final String REALM_NAME = "test";

    protected String userId;

    protected String userId2;

    protected AuthenticationManagementResource authMgmtResource;

    protected AuthenticationExecutionInfoRepresentation browserExecution;

    protected AuthenticationExecutionInfoRepresentation directGrantExecution;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Before
    public void configureFlows() {
        authMgmtResource = adminClient.realms().realm(REALM_NAME).flows();

        AuthenticationFlowRepresentation browserFlow = copyBrowserFlow();
        Assert.assertNotNull(browserFlow);

        AuthenticationFlowRepresentation directGrantFlow = createDirectGrantFlow();
        Assert.assertNotNull(directGrantFlow);

        setBrowserFlow(browserFlow);
        Assert.assertEquals(testRealm().toRepresentation().getBrowserFlow(), browserFlow.getAlias());

        setDirectGrantFlow(directGrantFlow);
        Assert.assertEquals(testRealm().toRepresentation().getDirectGrantFlow(), directGrantFlow.getAlias());
        Assert.assertEquals(0, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 cert authenticator to the direct grant flow
        directGrantExecution = addAssertExecution(directGrantFlow, ValidateX509CertificateUsernameFactory.PROVIDER_ID, REQUIRED);
        Assert.assertNotNull(directGrantExecution);

        directGrantFlow = authMgmtResource.getFlow(directGrantFlow.getId());
        Assert.assertNotNull(directGrantFlow.getAuthenticationExecutions());
        Assert.assertEquals(1, directGrantFlow.getAuthenticationExecutions().size());

        // Add X509 authenticator to the browser flow
        browserExecution = addAssertExecution(browserFlow, X509ClientCertificateAuthenticatorFactory.PROVIDER_ID, ALTERNATIVE);
        Assert.assertNotNull(browserExecution);

        // Raise the priority of the authenticator to position it right before
        // the Username/password authentication
        // TODO find a better, more explicit way to specify the position
        // of authenticator within the flow relative to other authenticators
        authMgmtResource.raisePriority(browserExecution.getId());
        // TODO raising the priority didn't generate the event?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.UPDATE, AdminEventPaths.authRaiseExecutionPath(exec.getId()));

        UserRepresentation user = findUser("test-user@localhost");
        userId = user.getId();

        user.singleAttribute("x509_certificate_identity","-");
        updateUser(user);
    }

    private AuthenticationExecutionInfoRepresentation addAssertExecution(AuthenticationFlowRepresentation flow, String providerId, String requirement) {
        AuthenticationExecutionRepresentation rep = new AuthenticationExecutionRepresentation();
        rep.setPriority(10);
        rep.setAuthenticator(providerId);
        rep.setRequirement(requirement);
        rep.setParentFlow(flow.getId());

        Response response = authMgmtResource.addExecution(rep);
        // TODO the following statement asserts, the actual value is null?
        //assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authMgmtBasePath() + "/executions"), rep);
        try {
            Assert.assertEquals("added execution", 201, response.getStatus());
        } finally {
            response.close();
        }
        List<AuthenticationExecutionInfoRepresentation> executionReps = authMgmtResource.getExecutions(flow.getAlias());
        return findExecution(providerId, executionReps);
    }

    AuthenticationExecutionInfoRepresentation findExecution(String providerId, List<AuthenticationExecutionInfoRepresentation> reps) {
        for (AuthenticationExecutionInfoRepresentation exec : reps) {
            if (providerId.equals(exec.getProviderId())) {
                return exec;
            }
        }
        return null;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("resource-owner")
                .directAccessGrants()
                .secret("secret")
                .build();

        UserRepresentation user = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("Keycloak")
                .email("localhost@localhost")
                .enabled(true)
                .password("password")
                .build();

        userId2 = user.getId();

        ClientRepresentation client = findTestApp(testRealm);
        URI baseUri = URI.create(client.getRedirectUris().get(0));
        URI redir = URI.create("https://localhost:" + System.getProperty("app.server.https.port", "8543") + baseUri.getRawPath());
        client.getRedirectUris().add(redir.toString());

        testRealm.setBruteForceProtected(true);
        testRealm.setFailureFactor(2);

        RealmBuilder.edit(testRealm)
                .user(user)
                .client(app);
    }

    AuthenticationFlowRepresentation createFlow(AuthenticationFlowRepresentation flowRep) {
        Response response = authMgmtResource.createFlow(flowRep);
        try {
            org.keycloak.testsuite.Assert.assertEquals(201, response.getStatus());
        }
        finally {
            response.close();
        }
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, AssertAdminEvents.isExpectedPrefixFollowedByUuid(AdminEventPaths.authFlowsPath()), flowRep, ResourceType.AUTH_FLOW);

        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(flowRep.getAlias())) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation copyFlow(String existingFlow, String newFlow) {
        // copy that should succeed
        HashMap<String, String> params = new HashMap<>();
        params.put("newName", newFlow);
        Response response = authMgmtResource.copy(existingFlow, params);
        assertAdminEvents.assertEvent(REALM_NAME, OperationType.CREATE, Encode.decode(AdminEventPaths.authCopyFlowPath(existingFlow)), params, ResourceType.AUTH_FLOW);
        try {
            Assert.assertEquals("Copy flow", 201, response.getStatus());
        } finally {
            response.close();
        }
        for (AuthenticationFlowRepresentation flow : authMgmtResource.getFlows()) {
            if (flow.getAlias().equalsIgnoreCase(newFlow)) {
                return flow;
            }
        }
        return null;
    }

    AuthenticationFlowRepresentation createDirectGrantFlow() {
        AuthenticationFlowRepresentation newFlow = newFlow("Copy-of-direct-grant", "desc", AuthenticationFlow.BASIC_FLOW, true, false);
        return createFlow(newFlow);
    }

    AuthenticationFlowRepresentation newFlow(String alias, String description,
                                             String providerId, boolean topLevel, boolean builtIn) {
        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(alias);
        flow.setDescription(description);
        flow.setProviderId(providerId);
        flow.setTopLevel(topLevel);
        flow.setBuiltIn(builtIn);
        return flow;
    }

    AuthenticationFlowRepresentation copyBrowserFlow() {

        RealmRepresentation realm = testRealm().toRepresentation();
        return copyFlow(realm.getBrowserFlow(), "Copy-of-browser");
    }

    void setBrowserFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(flow.getAlias());
        testRealm().update(realm);
    }

    void setDirectGrantFlow(AuthenticationFlowRepresentation flow) {
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setDirectGrantFlow(flow.getAlias());
        testRealm().update(realm);
    }

    static AuthenticatorConfigRepresentation newConfig(String alias, Map<String,String> params) {
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(alias);
        config.setConfig(params);
        return config;
    }

    protected String createConfig(String executionId, AuthenticatorConfigRepresentation cfg) {
        Response resp = authMgmtResource.newExecutionConfig(executionId, cfg);
        try {
            Assert.assertEquals(201, resp.getStatus());
        }
        finally {
            resp.close();
        }
        return ApiUtil.getCreatedId(resp);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectEmail2UsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTDN_EMAIL)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginSubjectCN2UsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(SUBJECTDN_CN)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginIssuerCNToUsernameOrEmailConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(ISSUERDN_CN)
                .setUserIdentityMapperType(USERNAME_EMAIL);
    }

    protected static X509AuthenticatorConfigModel createLoginIssuerDN_OU2CustomAttributeConfig() {
        return new X509AuthenticatorConfigModel()
                .setConfirmationPageAllowed(true)
                .setMappingSourceType(ISSUERDN)
                .setRegularExpression("O=(.*?)(?:,|$)")
                .setUserIdentityMapperType(USER_ATTRIBUTE)
                .setCustomAttributeName("x509_certificate_identity");
    }

    protected void setUserEnabled(String userName, boolean enabled) {
        UserRepresentation user = findUser(userName);
        Assert.assertNotNull(user);

        user.setEnabled(enabled);

        updateUser(user);
    }
}
