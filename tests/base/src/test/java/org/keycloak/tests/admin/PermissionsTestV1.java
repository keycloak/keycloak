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

package org.keycloak.tests.admin;

import java.util.Collections;

import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourcePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.admin.authz.fgap.KeycloakAdminPermissionsV1ServerConfig;

import org.junit.jupiter.api.Test;

import static org.keycloak.services.resources.admin.AdminAuth.Resource.AUTHORIZATION;
import static org.keycloak.services.resources.admin.AdminAuth.Resource.CLIENT;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = KeycloakAdminPermissionsV1ServerConfig.class)
public class PermissionsTestV1 extends AbstractPermissionsTest {

    @InjectRealm(config = PermissionsTestRealmConfig1.class, ref = "realm1")
    ManagedRealm managedRealm1;

    @InjectRealm(config = PermissionsTestRealmConfig2.class, ref = "realm2")
    ManagedRealm managedRealm2;

    @Test
    public void clientAuthorization() {
        String fooAuthzClientUuid = ApiUtil.getCreatedId(managedRealm1.admin().clients().create(ClientConfigBuilder.create().clientId("foo-authz").build()));
        ClientRepresentation foo = managedRealm1.admin().clients().get(fooAuthzClientUuid).toRepresentation();

        invoke((realm, response) -> {
            foo.setServiceAccountsEnabled(true);
            foo.setAuthorizationServicesEnabled(true);
            realm.clients().get(foo.getId()).update(foo);
        }, CLIENT, true);
        invoke(realm -> realm.clients().get(foo.getId()).authorization().getSettings(), AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            ResourceServerRepresentation settings = authorization.getSettings();
            authorization.update(settings);
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resources();
        }, AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scopes();
        }, AUTHORIZATION, false);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policies();
        }, AUTHORIZATION, false);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            response.set(authorization.resources().create(new ResourceRepresentation("Test", Collections.emptySet())));
        }, AUTHORIZATION, true);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            response.set(authorization.scopes().create(new ScopeRepresentation("Test")));
        }, AUTHORIZATION, true);
        invoke((realm, response) -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            ResourcePermissionRepresentation representation = new ResourcePermissionRepresentation();
            representation.setName("Test PermissionsTest");
            representation.addResource("Default Resource");
            response.set(authorization.permissions().resource().create(representation));
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resource("nosuch").update(new ResourceRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scope("nosuch").update(new ScopeRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policy("nosuch").update(new PolicyRepresentation());
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.resources().resource("nosuch").remove();
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.scopes().scope("nosuch").remove();
        }, AUTHORIZATION, true);
        invoke(realm -> {
            AuthorizationResource authorization = realm.clients().get(foo.getId()).authorization();
            authorization.policies().policy("nosuch").remove();
        }, AUTHORIZATION, true);
    }
}
