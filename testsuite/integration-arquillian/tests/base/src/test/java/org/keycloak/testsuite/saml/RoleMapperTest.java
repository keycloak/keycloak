/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.saml;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.RoleListMapper;
import org.keycloak.protocol.saml.mappers.RoleNameMapper;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.ProtocolMappersUpdater;
import org.keycloak.testsuite.updaters.RoleScopeUpdater;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.SamlClient.Binding;
import org.keycloak.testsuite.util.SamlClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.keycloak.testsuite.util.SamlStreams.assertionsUnencrypted;
import static org.keycloak.testsuite.util.SamlStreams.attributeStatements;
import static org.keycloak.testsuite.util.SamlStreams.attributesUnecrypted;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_PORT;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SCHEME;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_SSL_REQUIRED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 *
 * @author hmlnarik
 */
public class RoleMapperTest extends AbstractSamlTest {

    public static final String ROLE_ATTRIBUTE_NAME = "Role";

    public static final String SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2 = AUTH_SERVER_SCHEME + "://localhost:" + (AUTH_SERVER_SSL_REQUIRED ? AUTH_SERVER_PORT : 8080) + "/employee2/";

    private ClientAttributeUpdater cau;
    private ProtocolMappersUpdater pmu;
    private static int COUNTER = 1;

    @Before
    public void cleanMappersAndScopes() {
        this.cau = ClientAttributeUpdater.forClient(adminClient, REALM_NAME, SAML_CLIENT_ID_EMPLOYEE_2)
          .setDefaultClientScopes(Collections.EMPTY_LIST)
          .update();
        this.pmu = cau.protocolMappers()
          .clear()
          .update();
    }

    @After
    public void revertCleanMappersAndScopes() throws IOException {
        this.pmu.close();
        this.cau.close();
    }

    public static ProtocolMapperRepresentation createSamlProtocolMapper(String protocolMapperProviderId, String... configKeyValue) {
        ProtocolMapperRepresentation res = new ProtocolMapperRepresentation();
        res.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        res.setName(protocolMapperProviderId + "-" + RoleMapperTest.COUNTER++);
        res.setProtocolMapper(protocolMapperProviderId);

        Map<String, String> config = new HashMap<>();
        for (int i = 0; i < configKeyValue.length - 1; i += 2) {
            String key = configKeyValue[i];
            String value = configKeyValue[i + 1];
            config.put(key, value);
        }
        res.setConfig(config);

        return res;
    }

    @Test
    public void singleRoleMapper() throws Exception {
        final String newClientId = SAML_CLIENT_ID_EMPLOYEE_2 + ".empl.oyee";
        cau.setClientId(newClientId).update();
        pmu.add(
          createSamlProtocolMapper(RoleListMapper.PROVIDER_ID,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAME, ROLE_ATTRIBUTE_NAME,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
            RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true"
          ),
          createSamlProtocolMapper(RoleNameMapper.PROVIDER_ID,
            RoleNameMapper.ROLE_CONFIG, newClientId + "." + "empl.oyee",
            RoleNameMapper.NEW_ROLE_NAME, "blah"
          )
        ).update();
        testExpectedRoles(newClientId, "user", "manager", "blah", "employee");
    }

    @Test
    public void singleRealmRoleWithDots() throws Exception {
        pmu.add(
          createSamlProtocolMapper(RoleListMapper.PROVIDER_ID,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAME, ROLE_ATTRIBUTE_NAME,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
            RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true"
          )
        ).update();
        RoleRepresentation roleWithDots = realmsResouce().realm(REALM_NAME).roles().get("role.with.dots").toRepresentation();
        try (UserAttributeUpdater uau = UserAttributeUpdater.forUserByUsername(adminClient, REALM_NAME, bburkeUser.getUsername()).update();
          RoleScopeUpdater rsu = uau.realmRoleScope().removeByName("user").add(roleWithDots).update()) {
            testExpectedRoles(SAML_CLIENT_ID_EMPLOYEE_2, "manager", "role.with.dots", "empl.oyee", "employee");
        }
    }

    @Test
    public void singleRealmRoleWithDotsRemapped() throws Exception {
        pmu.add(
          createSamlProtocolMapper(RoleListMapper.PROVIDER_ID,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAME, ROLE_ATTRIBUTE_NAME,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
            RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true"
          ),
          createSamlProtocolMapper(RoleNameMapper.PROVIDER_ID,
            RoleNameMapper.ROLE_CONFIG, "role.with.dots",
            RoleNameMapper.NEW_ROLE_NAME, "blahWithDots"
          )
        ).update();
        RoleRepresentation roleWithDots = realmsResouce().realm(REALM_NAME).roles().get("role.with.dots").toRepresentation();
        try (UserAttributeUpdater uau = UserAttributeUpdater.forUserByUsername(adminClient, REALM_NAME, bburkeUser.getUsername()).update();
          RoleScopeUpdater rsu = uau.realmRoleScope().removeByName("user").add(roleWithDots).update()) {
            testExpectedRoles(SAML_CLIENT_ID_EMPLOYEE_2, "manager", "blahWithDots", "empl.oyee", "employee");
        }
    }

    @Test
    public void defaultRoleMapperSingleAttribute() throws Exception {
        pmu.add(
          createSamlProtocolMapper(RoleListMapper.PROVIDER_ID,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAME, ROLE_ATTRIBUTE_NAME,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
            RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "true"
          )
        ).update();
        testExpectedRoles(SAML_CLIENT_ID_EMPLOYEE_2, "user", "manager", "empl.oyee", "employee");
    }

    @Test
    public void defaultRoleMapperMultipleAttributes() throws Exception {
        pmu.add(createSamlProtocolMapper(RoleListMapper.PROVIDER_ID,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAME, ROLE_ATTRIBUTE_NAME,
            AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, AttributeStatementHelper.BASIC,
            RoleListMapper.SINGLE_ROLE_ATTRIBUTE, "false"
          )
        ).update();
        testExpectedRoles(SAML_CLIENT_ID_EMPLOYEE_2, "user", "manager", "empl.oyee", "employee");
    }

    @Test
    public void noRoleMappers() throws Exception {
        testExpectedRoles(SAML_CLIENT_ID_EMPLOYEE_2);
    }

    public void testExpectedRoles(String clientId, String... expectedRoles) {
        SAMLDocumentHolder document = new SamlClientBuilder()
          .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), clientId, SAML_ASSERTION_CONSUMER_URL_EMPLOYEE_2, Binding.POST).build()
          .login().user(bburkeUser).build()
          .getSamlResponse(Binding.POST);

        assertThat(document.getSamlObject(), Matchers.isSamlResponse(JBossSAMLURIConstants.STATUS_SUCCESS));

        Stream<AssertionType> assertions = assertionsUnencrypted(document.getSamlObject());
        Stream<AttributeType> attributes = attributesUnecrypted(attributeStatements(assertions));
        Set<String> roles = attributes
          .filter(a -> a.getName().equals(ROLE_ATTRIBUTE_NAME))
          .flatMap(a -> a.getAttributeValue().stream())
          .map(Object::toString)
          .collect(Collectors.toSet());

        assertThat(roles, containsInAnyOrder(expectedRoles));
    }
}
