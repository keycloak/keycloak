/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.keycloak.common.Profile.Feature.UPLOAD_SCRIPTS;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.DecisionEffect;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyEvaluationRequest;
import org.keycloak.representations.idm.authorization.PolicyEvaluationResponse;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.UncaughtServerErrorExpected;
import org.keycloak.testsuite.authz.AbstractAuthzTest;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class DeployedScriptPolicyTest extends AbstractAuthzTest {

    private static final String SCRIPT_DEPLOYMENT_NAME = "scripts.jar";

    @Deployment(name = SCRIPT_DEPLOYMENT_NAME, managed = false, testable = false)
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static JavaArchive deploy() throws IOException {
        ScriptProviderDescriptor representation = new ScriptProviderDescriptor();

        representation.addPolicy("Grant Policy", "policy-grant.js");
        representation.addPolicy("Deny Policy", "policy-deny.js");

        return ShrinkWrap.create(JavaArchive.class, SCRIPT_DEPLOYMENT_NAME)
                .addAsManifestResource(new StringAsset(JsonSerialization.writeValueAsPrettyString(representation)),
                        "keycloak-scripts.json")
                .addAsResource(new StringAsset("$evaluation.grant();"), "policy-grant.js")
                .addAsResource(new StringAsset("$evaluation.deny();"), "policy-deny.js");
    }

    @BeforeClass
    public static void verifyEnvironment() {
        ContainerAssume.assumeNotAuthServerUndertow();
    }
    @ArquillianResource
    private Deployer deployer;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create().realmRole(RoleBuilder.create().name("uma_authorization").build()))
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization"))
                .user(UserBuilder.create().username("kolo").password("password"))
                .client(ClientBuilder.create().clientId("resource-server")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants())
                .build());
    }

    @Before
    public void onBefore() {
        deployer.deploy(SCRIPT_DEPLOYMENT_NAME);
        AuthorizationResource authorization = getAuthorizationResource();
        authorization.resources().create(new ResourceRepresentation("Default Resource"));
    }

    @After
    public void onAfter() {
        deployer.undeploy(SCRIPT_DEPLOYMENT_NAME);
    }

    @Test
    @DisableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    public void testJSPolicyProviderNotAvailable() {
        assertFalse(getAuthorizationResource().policies().policyProviders().stream().anyMatch(rep -> "js".equals(rep.getType())));
    }

    @Test
    @UncaughtServerErrorExpected
    @DisableFeature(value = UPLOAD_SCRIPTS, skipRestart = true)
    public void failCreateJSPolicy() {
        JSPolicyRepresentation grantPolicy = new JSPolicyRepresentation();

        grantPolicy.setName("JS Policy");
        grantPolicy.setType("js");
        grantPolicy.setCode("$evaluation.grant();");

        try (Response response = getAuthorizationResource().policies().js().create(grantPolicy)) {
            assertEquals(500, response.getStatus());
        }
    }

    @Test
    public void testCreatePermission() {
        AuthorizationResource authorization = getAuthorizationResource();
        PolicyRepresentation grantPolicy = new PolicyRepresentation();

        grantPolicy.setName("Grant Policy");
        grantPolicy.setType("script-policy-grant.js");

        authorization.policies().create(grantPolicy).close();

        PolicyRepresentation denyPolicy = new PolicyRepresentation();

        denyPolicy.setName("Deny Policy");
        denyPolicy.setType("script-policy-deny.js");

        authorization.policies().create(denyPolicy).close();

        PermissionsResource permissions = authorization.permissions();

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName("Test Deployed JS Permission");
        permission.addResource("Default Resource");
        permission.addPolicy(grantPolicy.getName());

        permissions.resource().create(permission).close();

        PolicyEvaluationRequest request = new PolicyEvaluationRequest();

        request.setUserId("marta");
        request.addResource("Default Resource");

        PolicyEvaluationResponse response = authorization.policies().evaluate(request);

        assertEquals(DecisionEffect.PERMIT, response.getStatus());

        permission = permissions.resource().findByName(permission.getName());

        permission.addPolicy(denyPolicy.getName());

        permissions.resource().findById(permission.getId()).update(permission);

        response = authorization.policies().evaluate(request);

        assertEquals(DecisionEffect.DENY, response.getStatus());

        permission.addPolicy(grantPolicy.getName());

        permissions.resource().findById(permission.getId()).update(permission);

        response = authorization.policies().evaluate(request);

        assertEquals(DecisionEffect.DENY, response.getStatus());

        permission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);

        permissions.resource().findById(permission.getId()).update(permission);

        response = authorization.policies().evaluate(request);

        assertEquals(DecisionEffect.PERMIT, response.getStatus());
    }

    private AuthorizationResource getAuthorizationResource() {
        return getClient(realmsResouce().realm("authz-test"), "resource-server").authorization();
    }

    private ClientResource getClient(RealmResource realm, String clientId) {
        ClientsResource clients = realm.clients();
        return clients.findByClientId(clientId).stream().map(representation -> clients.get(representation.getId())).findFirst()
                .orElseThrow(() -> new RuntimeException("Expected client [resource-server-test]"));
    }
}
