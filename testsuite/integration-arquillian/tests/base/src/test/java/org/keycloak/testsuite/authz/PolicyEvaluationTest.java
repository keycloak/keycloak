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
package org.keycloak.testsuite.authz;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.attribute.Attributes;
import org.keycloak.authorization.common.DefaultEvaluationContext;
import org.keycloak.authorization.identity.Identity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.permission.evaluator.PermissionEvaluator;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.policy.provider.PolicyProvider;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.JSPolicyRepresentation;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.TimePolicyRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GroupBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.RolesBuilder;
import org.keycloak.testsuite.util.UserBuilder;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class PolicyEvaluationTest extends AbstractAuthzTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        ProtocolMapperRepresentation groupProtocolMapper = new ProtocolMapperRepresentation();

        groupProtocolMapper.setName("groups");
        groupProtocolMapper.setProtocolMapper(GroupMembershipMapper.PROVIDER_ID);
        groupProtocolMapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "groups");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        config.put("full.path", "true");
        groupProtocolMapper.setConfig(config);

        testRealms.add(RealmBuilder.create().name("authz-test")
                .roles(RolesBuilder.create()
                        .realmRole(RoleBuilder.create().name("uma_authorization").build())
                        .realmRole(RoleBuilder.create().name("role-a").build())
                        .realmRole(RoleBuilder.create().name("role-b").build())
                )
                .group(GroupBuilder.create().name("Group A")
                        .subGroups(Arrays.asList("Group B", "Group D").stream().map(name -> {
                            if ("Group B".equals(name)) {
                                return GroupBuilder.create().name(name).subGroups(Arrays.asList("Group C", "Group E").stream().map(new Function<String, GroupRepresentation>() {
                                    @Override
                                    public GroupRepresentation apply(String name) {
                                        return GroupBuilder.create().name(name).build();
                                    }
                                }).collect(Collectors.toList())).build();
                            }
                            return GroupBuilder.create().name(name).realmRoles(Arrays.asList("role-a")).build();
                        }).collect(Collectors.toList())).build())
                .group(GroupBuilder.create().name("Group E").build())
                .user(UserBuilder.create().username("marta").password("password").addRoles("uma_authorization", "role-a").addGroups("Group A"))
                .user(UserBuilder.create().username("alice").password("password").addRoles("uma_authorization").addGroups("/Group A/Group B/Group E"))
                .user(UserBuilder.create().username("kolo").password("password").addRoles("uma_authorization").addGroups("/Group A/Group D"))
                .user(UserBuilder.create().username("trinity").password("password").addRoles("uma_authorization").role("role-mapping-client", "client-role-a"))
                .user(UserBuilder.create().username("jdoe").password("password").addGroups("/Group A/Group B", "/Group A/Group D"))
                .client(ClientBuilder.create().clientId("resource-server-test")
                        .secret("secret")
                        .authorizationServicesEnabled(true)
                        .redirectUris("http://localhost/resource-server-test")
                        .defaultRoles("uma_protection")
                        .directAccessGrants()
                        .protocolMapper(groupProtocolMapper))
                .client(ClientBuilder.create().clientId("role-mapping-client")
                        .defaultRoles("client-role-a", "client-role-b"))
                .build());
    }

    @Test
    public void testCheckDateAndTime() {testingClient.server().run(PolicyEvaluationTest::testCheckDateAndTime);}

    public static void testCheckDateAndTime(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        TimePolicyRepresentation policyRepresentation = new TimePolicyRepresentation();
        policyRepresentation.setName("testCheckDateAndTime");

        // set the notOnOrAfter for 1 hour from now
        long notOnOrAfter = System.currentTimeMillis() + 3600000;
        Date notOnOrAfterDate = new Date(notOnOrAfter);
        policyRepresentation.setNotOnOrAfter(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(notOnOrAfterDate));

        // evaluation should succeed with the default context as it uses the current time as the date to be compared.
        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());
        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);
        provider.evaluate(evaluation);
        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        // lets now override the context to use a time that exceeds the time that was set in the policy.
        long contextTime = System.currentTimeMillis() + 5400000;
        Map<String,Collection<String>> attributes = new HashMap<>();
        attributes.put("kc.time.date_time", Arrays.asList(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(contextTime))));
        evaluation = createEvaluation(session, authorization, null, resourceServer, policy, attributes);
        provider.evaluate(evaluation);
        Assert.assertEquals(Effect.DENY, evaluation.getEffect());
    }

    @Test
    public void testCheckUserInGroup() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserInGroup);
    }

    public static void testCheckUserInGroup(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserInGroup");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('marta', 'Group C')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('marta', 'Group A')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('marta', '/Group A')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('marta', '/Group A/Group B')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('alice', '/Group A/Group B/Group E')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('alice', '/Group A')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (!realm.isUserInGroup('alice', '/Group A', false)) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('alice', '/Group E')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInGroup('alice', 'Group E')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());
    }

    @Test
    public void testCheckUserInRole() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserInRole);
    }

    public static void testCheckUserInRole(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserInRole");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInRealmRole('marta', 'role-a')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInRealmRole('marta', 'role-b')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());
    }

    @Test
    public void testCheckUserInClientRole() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserInClientRole);
    }

    public static void testCheckUserInClientRole(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserInClientRole");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInClientRole('trinity', 'role-mapping-client', 'client-role-a')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isUserInRealmRole('trinity', 'client-role-b')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());
    }

    @Test
    public void testCheckGroupInRole() {
        testingClient.server().run(PolicyEvaluationTest::testCheckGroupInRole);
    }

    public static void testCheckGroupInRole(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckGroupInRole");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isGroupInRole('/Group A/Group D', 'role-a')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());

        builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("if (realm.isGroupInRole('/Group A/Group D', 'role-b')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        policyRepresentation.setId(policy.getId());
        policy = RepresentationToModel.toModel(policyRepresentation, authorization, policy);

        evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertNull(evaluation.getEffect());
    }

    @Test
    public void testCheckUserRealmRoles() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserRealmRoles);
    }

    public static void testCheckUserRealmRoles(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserRealmRoles");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("var roles = realm.getUserRealmRoles('marta');");
        builder.append("if (roles.size() == 2 && roles.contains('uma_authorization') && roles.contains('role-a')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    @Test
    public void testCheckUserClientRoles() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserClientRoles);
    }

    public static void testCheckUserClientRoles(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserClientRoles");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("var roles = realm.getUserClientRoles('trinity', 'role-mapping-client');");
        builder.append("if (roles.size() == 1 && roles.contains('client-role-a')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    @Test
    public void testCheckUserGroups() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserGroups);
    }

    public static void testCheckUserGroups(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserGroups");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("var groups = realm.getUserGroups('jdoe');");
        builder.append("if (groups.size() == 2 && groups.contains('/Group A/Group B') && groups.contains('/Group A/Group D')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    @Test
    public void testCheckUserAttributes() {
        testingClient.server().run(PolicyEvaluationTest::testCheckUserAttributes);
    }

    public static void testCheckUserAttributes(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("authz-test");
        UserModel jdoe = session.users().getUserByUsername("jdoe", realm);

        jdoe.setAttribute("a1", Arrays.asList("1", "2"));
        jdoe.setSingleAttribute("a2", "3");

        session.getContext().setRealm(realm);
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckUserAttributes");
        StringBuilder builder = new StringBuilder();

        builder.append("var realm = $evaluation.getRealm();");
        builder.append("var attributes = realm.getUserAttributes('jdoe');");
        builder.append("if (attributes.size() == 2 && attributes.containsKey('a1') && attributes.containsKey('a2') && attributes.get('a1').size() == 2 && attributes.get('a2').get(0).equals('3')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    @Test
    public void testCheckResourceAttributes() {
        testingClient.server().run(PolicyEvaluationTest::testCheckResourceAttributes);
    }

    public static void testCheckResourceAttributes(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckResourceAttributes");
        StringBuilder builder = new StringBuilder();

        builder.append("var permission = $evaluation.getPermission();");
        builder.append("var resource = permission.getResource();");
        builder.append("var attributes = resource.getAttributes();");
        builder.append("if (attributes.size() == 2 && attributes.containsKey('a1') && attributes.containsKey('a2') && attributes.get('a1').size() == 2 && attributes.get('a2').get(0).equals('3') && resource.getAttribute('a1').size() == 2 && resource.getSingleAttribute('a2').equals('3')) { $evaluation.grant(); }");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);
        PolicyProvider provider = authorization.getProvider(policy.getType());
        Resource resource = storeFactory.getResourceStore().create("testCheckResourceAttributesResource", resourceServer, resourceServer.getId());

        resource.setAttribute("a1", Arrays.asList("1", "2"));
        resource.setAttribute("a2", Arrays.asList("3"));

        DefaultEvaluation evaluation = createEvaluation(session, authorization, resource, resourceServer, policy);

        provider.evaluate(evaluation);

        Assert.assertEquals(Effect.PERMIT, evaluation.getEffect());
    }

    @Test
    public void testCheckReadOnlyInstances() {
        testingClient.server().run(PolicyEvaluationTest::testCheckReadOnlyInstances);
    }

    public static void testCheckReadOnlyInstances(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());
        JSPolicyRepresentation policyRepresentation = new JSPolicyRepresentation();

        policyRepresentation.setName("testCheckReadOnlyInstances");
        StringBuilder builder = new StringBuilder();

        builder.append("$evaluation.getPermission().getResource().setName('test')");

        policyRepresentation.setCode(builder.toString());

        Policy policy = storeFactory.getPolicyStore().create(policyRepresentation, resourceServer);

        Resource resource = storeFactory.getResourceStore().create("Resource A", resourceServer, resourceServer.getId());
        Scope scope = storeFactory.getScopeStore().create("Scope A", resourceServer);

        resource.updateScopes(new HashSet<>(Arrays.asList(scope)));

        ResourcePermissionRepresentation permission = new ResourcePermissionRepresentation();

        permission.setName("testCheckReadOnlyInstances permission");
        permission.addPolicy(policy.getId());
        permission.addResource(resource.getId());

        storeFactory.getPolicyStore().create(permission, resourceServer);

        session.getTransactionManager().commit();

        PermissionEvaluator evaluator = authorization.evaluators().from(Arrays.asList(new ResourcePermission(resource, Arrays.asList(scope), resourceServer)), createEvaluationContext(session, Collections.emptyMap()));

        try {
            evaluator.evaluate(resourceServer, null);
            Assert.fail("Instances should be marked as read-only");
        } catch (Exception ignore) {
        }
    }

    @Test
    public void testCachedDecisionsWithNegativePolicies() {
        testingClient.server().run(PolicyEvaluationTest::testCachedDecisionsWithNegativePolicies);
    }

    public static void testCachedDecisionsWithNegativePolicies(KeycloakSession session) {
        session.getContext().setRealm(session.realms().getRealmByName("authz-test"));
        AuthorizationProvider authorization = session.getProvider(AuthorizationProvider.class);
        ClientModel clientModel = session.realms().getClientByClientId("resource-server-test", session.getContext().getRealm());
        StoreFactory storeFactory = authorization.getStoreFactory();
        ResourceServer resourceServer = storeFactory.getResourceServerStore().findById(clientModel.getId());

        Scope readScope = storeFactory.getScopeStore().create("read", resourceServer);
        Scope writeScope = storeFactory.getScopeStore().create("write", resourceServer);

        JSPolicyRepresentation policy = new JSPolicyRepresentation();

        policy.setName(KeycloakModelUtils.generateId());
        policy.setCode("$evaluation.grant()");
        policy.setLogic(Logic.NEGATIVE);

        storeFactory.getPolicyStore().create(policy, resourceServer);

        ScopePermissionRepresentation readPermission = new ScopePermissionRepresentation();

        readPermission.setName(KeycloakModelUtils.generateId());
        readPermission.addScope(readScope.getId());
        readPermission.addPolicy(policy.getName());

        storeFactory.getPolicyStore().create(readPermission, resourceServer);

        ScopePermissionRepresentation writePermission = new ScopePermissionRepresentation();

        writePermission.setName(KeycloakModelUtils.generateId());
        writePermission.addScope(writeScope.getId());
        writePermission.addPolicy(policy.getName());

        storeFactory.getPolicyStore().create(writePermission, resourceServer);

        Resource resource = storeFactory.getResourceStore().create(KeycloakModelUtils.generateId(), resourceServer, resourceServer.getId());

        PermissionEvaluator evaluator = authorization.evaluators().from(Arrays.asList(new ResourcePermission(resource, Arrays.asList(readScope, writeScope), resourceServer)), createEvaluationContext(session, Collections.emptyMap()));
        Collection<Permission> permissions = evaluator.evaluate(resourceServer, null);

        Assert.assertEquals(0, permissions.size());
    }

    private static DefaultEvaluation createEvaluation(KeycloakSession session, AuthorizationProvider authorization, ResourceServer resourceServer, Policy policy) {
        return createEvaluation(session, authorization, null, resourceServer, policy);
    }

    private static DefaultEvaluation createEvaluation(KeycloakSession session, AuthorizationProvider authorization, Resource resource, ResourceServer resourceServer, Policy policy) {
        return createEvaluation(session, authorization, resource, resourceServer, policy, null);
    }

    private static DefaultEvaluation createEvaluation(KeycloakSession session, AuthorizationProvider authorization,
                                                      Resource resource, ResourceServer resourceServer, Policy policy,
                                                      Map<String, Collection<String>> contextAttributes) {
        return new DefaultEvaluation(new ResourcePermission(resource, null, resourceServer), createEvaluationContext(session, contextAttributes), policy, evaluation -> {}, authorization, null);
    }

    private static DefaultEvaluationContext createEvaluationContext(KeycloakSession session, Map<String, Collection<String>> contextAttributes) {
        return new DefaultEvaluationContext(new Identity() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public Attributes getAttributes() {
                return null;
            }
        }, session) {

            /*
             * Allow specific tests to override/add attributes to the context.
             */
            @Override
            public Map<String, Collection<String>> getBaseAttributes() {
                Map<String, Collection<String>> baseAttributes = super.getBaseAttributes();
                if (contextAttributes != null) {
                    baseAttributes.putAll(contextAttributes);
                }
                return baseAttributes;
            }
        };
    }
}
