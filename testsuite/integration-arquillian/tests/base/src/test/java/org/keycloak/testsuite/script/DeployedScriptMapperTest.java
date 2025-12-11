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

import java.io.IOException;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ScriptBasedOIDCProtocolMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.util.JsonSerialization;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.common.Profile.Feature.SCRIPTS;
import static org.keycloak.testsuite.admin.ApiUtil.findClientResourceByClientId;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;
import static org.keycloak.testsuite.util.ProtocolMapperUtil.createScriptMapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@EnableFeature(value = SCRIPTS, skipRestart = true)
public class DeployedScriptMapperTest extends AbstractTestRealmKeycloakTest {

    private static final String SCRIPT_DEPLOYMENT_NAME = "scripts.jar";

    // Managed to make sure that archive is deployed once in @BeforeClass stage and undeployed once in @AfterClass stage
    @Deployment(name = SCRIPT_DEPLOYMENT_NAME, managed = true, testable = false)
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static JavaArchive deploy() throws IOException {
        ScriptProviderDescriptor representation = new ScriptProviderDescriptor();

        representation.addMapper("My Mapper", "mapper-a.js");

        return ShrinkWrap.create(JavaArchive.class, SCRIPT_DEPLOYMENT_NAME)
                .addAsManifestResource(new StringAsset(JsonSerialization.writeValueAsPrettyString(representation)),
                        "keycloak-scripts.json")
                .addAsResource("scripts/mapper-example.js", "mapper-a.js");
    }

    @BeforeClass
    public static void verifyEnvironment() {
        ContainerAssume.assumeNotAuthServerUndertow();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Test
    @DisableFeature(value = SCRIPTS, executeAsLast = false, skipRestart = true)
    public void testScriptMapperNotAvailable() {
        assertFalse(adminClient.serverInfo().getInfo().getProtocolMapperTypes().get(OIDCLoginProtocol.LOGIN_PROTOCOL).stream()
                .anyMatch(
                        mapper -> ScriptBasedOIDCProtocolMapper.PROVIDER_ID.equals(mapper.getId())));
    }

    @Test
    public void testTokenScriptMapping() {
        {
            ClientResource app = findClientResourceByClientId(adminClient.realm("test"), "test-app");

            ProtocolMapperRepresentation mapper = createScriptMapper("test-script-mapper1", "computed-via-script",
                    "computed-via-script", "String", true, true, true, "'hello_' + user.username", false);

            mapper.setProtocolMapper("script-mapper-a.js");

            app.getProtocolMappers().createMapper(mapper).close();
        }
        {
            AccessTokenResponse response = browserLogin("test-user@localhost", "password");
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

            assertEquals("hello_test-user@localhost", accessToken.getOtherClaims().get("computed-via-script"));
        }
    }

    private AccessTokenResponse browserLogin(String username, String password) {
        AuthorizationEndpointResponse authzEndpointResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(authzEndpointResponse.getCode());
    }
}
