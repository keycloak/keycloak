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

package org.keycloak.testsuite.model;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.federation.ldap.AbstractLDAPTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;
import org.keycloak.testsuite.util.LDAPRule;

import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

import java.util.List;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ImportTest extends AbstractTestRealmKeycloakTest {


    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, ImportTest.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.model");
    }

    @Test
    public void demoDelete() throws Exception {
        // was having trouble deleting this realm from admin console
            removeRealm("demo-delete");
    }
    
	@Test
    public void install2() throws Exception {
            testingClient.server().run(session -> {
                RealmModel realm = session.realms().getRealmByName("demo");

                Assert.assertEquals(600, realm.getAccessCodeLifespanUserAction());
                Assert.assertEquals(Constants.DEFAULT_ACCESS_TOKEN_LIFESPAN_FOR_IMPLICIT_FLOW_TIMEOUT, realm.getAccessTokenLifespanForImplicitFlow());
                Assert.assertEquals(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT, realm.getOfflineSessionIdleTimeout());
                Assert.assertEquals(1, realm.getRequiredCredentials().size());
                Assert.assertEquals("password", realm.getRequiredCredentials().get(0).getType());
            });
    }

    private static void verifyRequiredCredentials(List<RequiredCredentialModel> requiredCreds, String expectedType) {

    	Assert.assertEquals(1, requiredCreds.size());
        Assert.assertEquals(expectedType, requiredCreds.get(0).getType());
    }

    private static void assertGssProtocolMapper(ProtocolMapperModel gssCredentialMapper) {
    	
    	Assert.assertEquals(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME, gssCredentialMapper.getName());
        Assert.assertEquals( OIDCLoginProtocol.LOGIN_PROTOCOL, gssCredentialMapper.getProtocol());
        Assert.assertEquals(UserSessionNoteMapper.PROVIDER_ID, gssCredentialMapper.getProtocolMapper());
        String includeInAccessToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        String includeInIdToken = gssCredentialMapper.getConfig().get(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        Assert.assertTrue(includeInAccessToken.equalsIgnoreCase("true"));
        Assert.assertTrue(includeInIdToken == null || Boolean.parseBoolean(includeInIdToken) == false);
    	
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealmParm) {

        log.infof("testrealm2 imported");
        RealmRepresentation testRealm = loadJson(getClass().getResourceAsStream("/model/testrealm2.json"), RealmRepresentation.class);
        adminClient.realms().create(testRealm);

        log.infof("testrealm-demo imported");
        testRealm = loadJson(getClass().getResourceAsStream("/model/testrealm-demo.json"), RealmRepresentation.class);
        testRealm.setRealm("demo");
        testRealm.setId("demo");
        adminClient.realms().create(testRealm);
    }
}
