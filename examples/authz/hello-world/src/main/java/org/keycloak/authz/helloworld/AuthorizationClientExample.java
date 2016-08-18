/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.authz.helloworld;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.EntitlementRequest;
import org.keycloak.authorization.client.representation.EntitlementResponse;
import org.keycloak.authorization.client.representation.PermissionRequest;
import org.keycloak.authorization.client.representation.RegistrationResponse;
import org.keycloak.authorization.client.representation.ResourceRepresentation;
import org.keycloak.authorization.client.representation.ScopeRepresentation;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.idm.authorization.Permission;

import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AuthorizationClientExample {

    public static void main(String[] args) {
        obtainEntitlementsForResource();
        obtainAllEntitlements();
        createResource();
        introspectRequestingPartyToken();
    }

    private static void introspectRequestingPartyToken() {
        // create a new instance based on the configuration define at keycloak-authz.json
        AuthzClient authzClient = AuthzClient.create();

        // query the server for a resource with a given name
        Set<String> resourceId = authzClient.protection()
                .resource()
                .findByFilter("name=Default Resource");

        // obtian a Entitlement API Token in order to get access to the Entitlement API.
        // this token is just an access token issued to a client on behalf of an user with a scope kc_entitlement
        String eat = getEntitlementAPIToken(authzClient);

        // create an entitlement request
        EntitlementRequest request = new EntitlementRequest();
        PermissionRequest permission = new PermissionRequest();

        permission.setResourceSetId(resourceId.iterator().next());

        request.addPermission(permission);

        // send the entitlement request to the server in order to obtain a RPT with all permissions granted to the user
        EntitlementResponse response = authzClient.entitlement(eat).get("hello-world-authz-service", request);
        String rpt = response.getRpt();

        TokenIntrospectionResponse requestingPartyToken = authzClient.protection().introspectRequestingPartyToken(rpt);

        System.out.println("Token status is: " + requestingPartyToken.getActive());
        System.out.println("Permissions granted by the server: ");

        for (Permission granted : requestingPartyToken.getPermissions()) {
            System.out.println(granted);
        }

    }

    private static void createResource() {
        // create a new instance based on the configuration define at keycloak-authz.json
        AuthzClient authzClient = AuthzClient.create();

        // create a new resource representation with the information we want
        ResourceRepresentation newResource = new ResourceRepresentation();

        newResource.setName("New Resource");
        newResource.setType("urn:hello-world-authz:resources:example");

        newResource.addScope(new ScopeRepresentation("urn:hello-world-authz:scopes:view"));

        ProtectedResource resourceClient = authzClient.protection().resource();
        Set<String> existingResource = resourceClient.findByFilter("name=" + newResource.getName());

        if (!existingResource.isEmpty()) {
            resourceClient.delete(existingResource.iterator().next());
        }

        // create the resource on the server
        RegistrationResponse response = resourceClient.create(newResource);
        String resourceId = response.getId();

        // query the resource using its newly generated id
        ResourceRepresentation resource = resourceClient.findById(resourceId).getResourceDescription();

        System.out.println(resource);
    }

    private static void obtainEntitlementsForResource() {
        // create a new instance based on the configuration define at keycloak-authz.json
        AuthzClient authzClient = AuthzClient.create();

        // obtian a Entitlement API Token in order to get access to the Entitlement API.
        // this token is just an access token issued to a client on behalf of an user with a scope kc_entitlement
        String eat = getEntitlementAPIToken(authzClient);

        // create an entitlement request
        EntitlementRequest request = new EntitlementRequest();
        PermissionRequest permission = new PermissionRequest();

        permission.setResourceSetName("Default Resource");

        request.addPermission(permission);

        // send the entitlement request to the server in order to obtain a RPT with all permissions granted to the user
        EntitlementResponse response = authzClient.entitlement(eat).get("hello-world-authz-service", request);
        String rpt = response.getRpt();

        System.out.println("You got a RPT: " + rpt);

        // now you can use the RPT to access protected resources on the resource server
    }

    private static void obtainAllEntitlements() {
        // create a new instance based on the configuration define at keycloak-authz.json
        AuthzClient authzClient = AuthzClient.create();

        // obtian a Entitlement API Token in order to get access to the Entitlement API.
        // this token is just an access token issued to a client on behalf of an user with a scope kc_entitlement
        String eat = getEntitlementAPIToken(authzClient);

        // send the entitlement request to the server in order to obtain a RPT with all permissions granted to the user
        EntitlementResponse response = authzClient.entitlement(eat).getAll("hello-world-authz-service");
        String rpt = response.getRpt();

        System.out.println("You got a RPT: " + rpt);

        // now you can use the RPT to access protected resources on the resource server
    }

    /**
     * Obtain an Entitlement API Token or EAT from the server. Usually, EATs are going to be obtained by clients using a
     * authorization_code grant type. Here we are using resource owner credentials for demonstration purposes.
     *
     * @param authzClient the authorization client instance
     * @return a string representing a EAT
     */
    private static String getEntitlementAPIToken(AuthzClient authzClient) {
        return authzClient.obtainAccessToken("alice", "alice").getToken();
    }
}
