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
package org.keycloak.testsuite.authorization;

import org.apache.commons.collections.map.HashedMap;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.Decision;
import org.keycloak.authorization.common.KeycloakEvaluationContext;
import org.keycloak.authorization.common.KeycloakIdentity;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.permission.ResourcePermission;
import org.keycloak.authorization.policy.evaluation.DefaultEvaluation;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractPhotozAdminTest extends AbstractAuthorizationTest {

    protected ResourceServer resourceServer;
    protected Resource adminResource;
    protected Policy anyAdminPolicy;
    protected Policy onlyFromSpecificAddressPolicy;
    protected Policy administrationPolicy;

    protected Resource albumResource;
    protected Policy anyUserPolicy;

    @Before
    public void onBefore() {
        super.onBefore();
        this.resourceServer = createResourceServer();
        this.adminResource = createAdminAlbumResource();
        this.anyAdminPolicy = createAnyAdminPolicy();
        this.onlyFromSpecificAddressPolicy = createOnlyFromSpecificAddressPolicy();
        this.administrationPolicy = createAdministrationPolicy();

        this.albumResource = createAlbumResource();
        this.anyUserPolicy = createAnyUserPolicy();
    }

    protected ResourceServer createResourceServer() {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            ResourceServerStore resourceServerStore = storeFactory.getResourceServerStore();

            ResourceServer resourceServer = resourceServerStore.create(getClientByClientId("photoz-restful-api").getId());

            resourceServer.setAllowRemoteResourceManagement(true);

            return resourceServer;
        });
    }

    protected Map<String, DefaultEvaluation> performEvaluation(List<ResourcePermission> permissions, AccessToken accessToken, ClientConnection clientConnection) {
        Map<String, DefaultEvaluation> evaluations = new HashedMap();

        onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();

            // during tests we create resource instances, but we need to reload them to get their collections updated
            List<ResourcePermission> updatedPermissions = permissions.stream().map(permission -> {
                Resource resource = storeFactory.getResourceStore().findById(permission.getResource().getId(), resourceServer.getId());
                return new ResourcePermission(resource, permission.getScopes(), permission.getResourceServer());
            }).collect(Collectors.toList());

            authorizationProvider.evaluators().from(updatedPermissions, createEvaluationContext(accessToken, clientConnection, authorizationProvider)).evaluate(new Decision<DefaultEvaluation>() {
                @Override
                public void onDecision(DefaultEvaluation evaluation) {
                    evaluations.put(evaluation.getPolicy().getId(), evaluation);
                }

                @Override
                public void onError(Throwable cause) {
                    throw new RuntimeException("Permission evaluation failed.", cause);
                }
            });
        });

        return evaluations;
    }

    private KeycloakEvaluationContext createEvaluationContext(AccessToken accessToken, ClientConnection clientConnection, AuthorizationProvider authorizationProvider) {
        KeycloakSession keycloakSession = authorizationProvider.getKeycloakSession();

        keycloakSession.getContext().setConnection(clientConnection);

        keycloakSession.getContext().setClient(getClientByClientId("photoz-html5-client"));

        ResteasyProviderFactory.pushContext(HttpHeaders.class, createHttpHeaders());

        KeycloakIdentity identity = new KeycloakIdentity(accessToken, keycloakSession);

        return new KeycloakEvaluationContext(identity, keycloakSession);
    }

    protected AccessToken createAccessToken(Set<String> roles) {
        AccessToken accessToken = new AccessToken();

        accessToken.setRealmAccess(new AccessToken.Access());
        accessToken.getRealmAccess().roles(roles);

        return accessToken;
    }


    private HttpHeaders createHttpHeaders() {
        return new HttpHeaders() {
            @Override
            public List<String> getRequestHeader(String name) {
                return null;
            }

            @Override
            public String getHeaderString(String name) {
                return null;
            }

            @Override
            public MultivaluedMap<String, String> getRequestHeaders() {
                return null;
            }

            @Override
            public List<MediaType> getAcceptableMediaTypes() {
                return null;
            }

            @Override
            public List<Locale> getAcceptableLanguages() {
                return null;
            }

            @Override
            public MediaType getMediaType() {
                return null;
            }

            @Override
            public Locale getLanguage() {
                return null;
            }

            @Override
            public Map<String, Cookie> getCookies() {
                return null;
            }

            @Override
            public Date getDate() {
                return null;
            }

            @Override
            public int getLength() {
                return 0;
            }
        };
    }

    protected ClientConnection createClientConnection(String remoteAddr) {
        return new ClientConnection() {
            @Override
            public String getRemoteAddr() {
                return remoteAddr;
            }

            @Override
            public String getRemoteHost() {
                return "localhost";
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalAddr() {
                return null;
            }

            @Override
            public int getLocalPort() {
                return 0;
            }
        };
    }

    protected Invocation.Builder newPermissionRequest(String... id) {
        String idPathParam = "";

        if (id.length != 0) {
            idPathParam = "/" + id[0];
        }

        return newClient(getClientByClientId("photoz-restful-api"), "/resource-server/policy" + idPathParam);
    }

    private Policy createAdministrationPolicy() {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();
            PolicyRepresentation representation = new PolicyRepresentation();

            representation.setName("Administration Policy");
            representation.setType("aggregate");
            representation.addPolicy(anyAdminPolicy.getName());
            representation.addPolicy(onlyFromSpecificAddressPolicy.getName());

            Policy policy = policyStore.create(representation, resourceServer);

            return policy;
        });
    }

    private Policy createOnlyFromSpecificAddressPolicy() {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();
            PolicyRepresentation representation = new PolicyRepresentation();

            representation.setName("Only From a Specific Client Address");
            representation.setType("js");
            HashedMap config = new HashedMap();

            config.put("code",
                    "var contextAttributes = $evaluation.getContext().getAttributes();" +
                            "var networkAddress = contextAttributes.getValue('kc.client.network.ip_address');" +
                            "if ('127.0.0.1'.equals(networkAddress.asInetAddress(0).getHostAddress())) {" +
                            "$evaluation.grant();" +
                            "}");

            representation.setConfig(config);

            return policyStore.create(representation, resourceServer);
        });
    }

    private Policy createAnyAdminPolicy() {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();
            RolePolicyRepresentation representation = new RolePolicyRepresentation();

            representation.setName("Any Admin Policy");
            representation.setType("role");
            representation.addRole("admin", false);

            return policyStore.create(representation, resourceServer);
        });
    }

    private Resource createAdminAlbumResource() {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName("Admin Resources");
        representation.setType("http://photoz.com/admin");
        representation.setUri("/admin/*");

        HashSet<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(new ScopeRepresentation("urn:photoz.com:scopes:album:admin:manage"));

        representation.setScopes(scopes);

        return createResource(representation);
    }

    private Resource createAlbumResource() {
        ResourceRepresentation representation = new ResourceRepresentation();

        representation.setName("Album Resource");
        representation.setType("http://photoz.com/album");
        representation.setUri("/album/*");

        HashSet<ScopeRepresentation> scopes = new HashSet<>();

        scopes.add(new ScopeRepresentation("urn:photoz.com:scopes:album:view"));
        scopes.add(new ScopeRepresentation("urn:photoz.com:scopes:album:create"));
        scopes.add(new ScopeRepresentation("urn:photoz.com:scopes:album:delete"));

        representation.setScopes(scopes);

        return createResource(representation);
    }

    protected Resource createResource(ResourceRepresentation representation) {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            ScopeStore scopeStore = storeFactory.getScopeStore();

            representation.getScopes().forEach(scopeRepresentation -> {
                scopeStore.create(scopeRepresentation.getName(), resourceServer);
            });

            ResourceStore resourceStore = storeFactory.getResourceStore();
            Resource albumResource = resourceStore.create(representation.getName(), resourceServer, resourceServer.getId());

            albumResource.setType(representation.getType());
            albumResource.setUri(representation.getUri());
            albumResource.setIconUri(representation.getIconUri());

            return albumResource;
        });
    }

    private Policy createAnyUserPolicy() {
        return onAuthorizationSession(authorizationProvider -> {
            StoreFactory storeFactory = authorizationProvider.getStoreFactory();
            PolicyStore policyStore = storeFactory.getPolicyStore();
            PolicyRepresentation representation = new PolicyRepresentation();

            representation.setName("Any User Policy");
            representation.setType("role");

            HashedMap config = new HashedMap();
            RealmModel realm = authorizationProvider.getKeycloakSession().realms().getRealmByName(TEST_REALM_NAME);
            RoleModel userRole = realm.getRole("user");

            Map role = new HashMap();

            role.put("id", userRole.getId());

            try {
                config.put("roles", JsonSerialization.writeValueAsString(new Map[] {role}));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            representation.setConfig(config);

            Policy policy = policyStore.create(representation, resourceServer);

            return policy;
        });
    }
}
