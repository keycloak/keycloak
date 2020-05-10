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
package org.keycloak.storage.openshift;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.openshift.restclient.IClient;
import com.openshift.restclient.model.IResource;
import com.openshift.restclient.model.route.IRoute;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.UserPropertyMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.storage.client.AbstractReadOnlyClientScopeAdapter;
import org.keycloak.storage.client.AbstractReadOnlyClientStorageAdapter;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class OpenshiftSAClientAdapter extends AbstractReadOnlyClientStorageAdapter {

    private static final String ANNOTATION_OAUTH_REDIRECT_URI = "serviceaccounts.openshift.io/oauth-redirecturi";
    private static final String ANNOTATION_OAUTH_REDIRECT_REFERENCE = "serviceaccounts.openshift.io/oauth-redirectreference";
    private static final Pattern ROLE_SCOPE_PATTERN = Pattern.compile("role:([^:]+):([^:!]+)(:[!])?");
    private static final Set<String> OPTIONAL_SCOPES = Stream.of("user:info", "user:check-access").collect(Collectors.toSet());
    private static final Set<ProtocolMapperModel> DEFAULT_PROTOCOL_MAPPERS = createDefaultProtocolMappers();

    private static Set<ProtocolMapperModel> createDefaultProtocolMappers() {
        Set<ProtocolMapperModel> mappers = new HashSet<>();

        ProtocolMapperModel mapper = OIDCAttributeMapperHelper.createClaimMapper("username", "username", "preferred_username", "string", true, true, UserPropertyMapper.PROVIDER_ID);

        mapper.setId(KeycloakModelUtils.generateId());

        mappers.add(mapper);

        return mappers;
    }

    private final IResource resource;
    private final String clientId;
    private final IClient client;
    private final ClientRepresentation defaultConfig = new ClientRepresentation();

    public OpenshiftSAClientAdapter(String clientId, IResource resource, IClient client, KeycloakSession session, RealmModel realm, ClientStorageProviderModel component) {
        super(session, realm, component);
        this.resource = resource;
        this.clientId = clientId;
        this.client = client;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getName() {
        return resource.getName();
    }

    @Override
    public String getDescription() {
        return getConfigOrDefault(() -> defaultConfig.getDescription(), defaultConfig::setDescription, new StringBuilder().append(resource.getKind()).append(" ").append(resource.getName()).append(" from namespace ").append(resource.getNamespace().getName()).toString());
    }

    @Override
    public boolean isEnabled() {
        return getConfigOrDefault(() -> defaultConfig.isEnabled(), defaultConfig::setEnabled, true);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return getConfigOrDefault(() -> defaultConfig.isAlwaysDisplayInConsole(), defaultConfig::setAlwaysDisplayInConsole, false);
    }

    @Override
    public Set<String> getWebOrigins() {
        return new HashSet<>(getConfigOrDefault(() -> defaultConfig.getWebOrigins(), defaultConfig::setWebOrigins, Collections.emptyList()));
    }

    @Override
    public Set<String> getRedirectUris() {
        return new HashSet<>(getConfigOrDefault((Supplier<List<String>>) () -> defaultConfig.getRedirectUris(),
                uris -> defaultConfig.setRedirectUris(uris),
                (Supplier<List<String>>) () -> resource.getAnnotations().entrySet().stream()
                        .filter((entry) -> entry.getKey().startsWith(ANNOTATION_OAUTH_REDIRECT_URI) || entry.getKey().startsWith(ANNOTATION_OAUTH_REDIRECT_REFERENCE))
                        .map(entry -> {
                            if (entry.getKey().startsWith(ANNOTATION_OAUTH_REDIRECT_URI)) {
                                return entry.getValue();
                            } else {
                                Map values;

                                try {
                                    values = JsonSerialization.readValue(entry.getValue(), Map.class);
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to parse annotation [" + ANNOTATION_OAUTH_REDIRECT_REFERENCE + "]", e);
                                }

                                Map<String, String> reference = (Map<String, String>) values.get("reference");
                                String kind = (String) reference.get("kind");

                                if (!"Route".equals(kind)) {
                                    throw new IllegalArgumentException("Only route references are supported for " + ANNOTATION_OAUTH_REDIRECT_REFERENCE);
                                }

                                String name = (String) reference.get("name");
                                IRoute route = client.get(kind, name, resource.getNamespace().getName());

                                StringBuilder url = new StringBuilder(route.getURL());

                                if (url.charAt(url.length() - 1) != '/') {
                                    url.append('/');
                                }

                                return url.append('*').toString();
                            }
                        }).collect(Collectors.toList())));
    }

    @Override
    public String getManagementUrl() {
        return null;
    }

    @Override
    public String getRootUrl() {
        return null;
    }

    @Override
    public String getBaseUrl() {
        return null;
    }

    @Override
    public boolean isBearerOnly() {
        return false;
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return 0;
    }

    @Override
    public String getClientAuthenticatorType() {
        return null;
    }

    @Override
    public boolean validateSecret(String secret) {
        //TODO: do we want SAs as confidential clients and enable client credentials grant and resource owner grant ?
        return false;
    }

    @Override
    public String getSecret() {
        //TODO: check if validate secret is enough, don't see a reason to return SAs secret
        return null;
    }

    @Override
    public String getRegistrationToken() {
        return null;
    }

    @Override
    public String getProtocol() {
        //TODO: set login protocol, always oidc
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getAttribute(String name) {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return null;
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isFrontchannelLogout() {
        return false;
    }

    @Override
    public boolean isFullScopeAllowed() {
        return false;
    }

    @Override
    public boolean isPublicClient() {
        return true;
    }

    @Override
    public boolean isConsentRequired() {
        return component.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_REQUIRE_USER_CONSENT, true);
    }

    @Override
    public boolean isDisplayOnConsentScreen() {
        return false;
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return true;
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return false;
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return false;
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return false;
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope, boolean filterByProtocol) {
        if (defaultScope) {
            return Collections.emptyMap();
        }

        Map<String, ClientScopeModel> scopes = new HashMap<>();

        for (String scope : OPTIONAL_SCOPES) {
            scopes.put(scope, createClientScope(scope));
        }

        return scopes;
    }

    @Override
    public ClientScopeModel getDynamicClientScope(String scope) {
        if (OPTIONAL_SCOPES.contains(scope)) {
            return createClientScope(scope);
        }

        Matcher matcher = ROLE_SCOPE_PATTERN.matcher(scope);

        if (matcher.matches()) {
            String namespace = matcher.group(2);

            if (resource.getNamespace().getName().equals(namespace)) {
                return createClientScope(scope);
            }
        }

        return null;
    }

    @Override
    public int getNotBefore() {
        return 0;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return getConfigOrDefault(() -> {
            List<ProtocolMapperRepresentation> mappers = defaultConfig.getProtocolMappers();

            if (mappers == null) {
                return null;
            }

            Set<ProtocolMapperModel> model = new HashSet<>();

            for (ProtocolMapperRepresentation mapper : mappers) {
                model.add(RepresentationToModel.toModel(mapper));
            }

            return model;
        }, (Consumer<Set<ProtocolMapperModel>>) mappers -> {
            defaultConfig.setProtocolMappers(mappers.stream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList()));
        }, (Supplier<Set<ProtocolMapperModel>>) () -> DEFAULT_PROTOCOL_MAPPERS);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return getProtocolMappers().stream().filter(protocolMapperModel -> id.equals(protocolMapperModel.getId())).findAny().get();
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return getProtocolMappers().stream().filter(protocolMapperModel -> name.equals(protocolMapperModel.getName())).findAny().get();
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return Collections.emptySet();
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    private <V> V getConfigOrDefault(Supplier<V> valueSupplier, Consumer<V> valueConsumer, Supplier<V> defaultValue) {
        V value = valueSupplier.get();

        if (value != null) {
            return value;
        }

        value = defaultValue.get();

        if (valueConsumer != null) {
            valueConsumer.accept(value);
        }

        return value;
    }

    private <V> V getConfigOrDefault(Supplier<V> valueSupplier, Consumer<V> valueConsumer, V defaultValue) {
        return getConfigOrDefault(valueSupplier, valueConsumer, (Supplier<V>) () -> defaultValue);
    }

    private ClientScopeModel createClientScope(String scope) {
        ClientScopeModel managedScope = realm.getClientScopes().stream().filter(scopeModel -> scopeModel.getName().equals(scope))
                .findAny().orElse(null);

        if (managedScope != null) {
            return managedScope;
        }

        Map<String, String> attributes = new HashMap<>();

        attributes.put(ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN, Boolean.valueOf(isConsentRequired()).toString());

        if (component.get(OpenshiftClientStorageProviderFactory.CONFIG_PROPERTY_DISPLAY_SCOPE_CONSENT_TEXT, Boolean.TRUE)) {
            StringBuilder consentText = new StringBuilder("${openshift.scope.");

            if (scope.indexOf(':') != -1) {
                consentText.append(scope.replaceFirst(":", "_"));
            }

            attributes.put(ClientScopeModel.CONSENT_SCREEN_TEXT, consentText.append("}").toString());
        } else {
            attributes.put(ClientScopeModel.CONSENT_SCREEN_TEXT, scope);
        }

        return new AbstractReadOnlyClientScopeAdapter() {
            @Override
            public String getId() {
                return scope;
            }

            @Override
            public String getName() {
                return scope;
            }

            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public String getDescription() {
                return scope;
            }

            @Override
            public String getProtocol() {
                return OIDCLoginProtocol.LOGIN_PROTOCOL;
            }

            @Override
            public String getAttribute(String name) {
                return attributes.get(name);
            }

            @Override
            public Map<String, String> getAttributes() {
                return attributes;
            }

            @Override
            public Set<ProtocolMapperModel> getProtocolMappers() {
                return DEFAULT_PROTOCOL_MAPPERS;
            }

            @Override
            public ProtocolMapperModel getProtocolMapperById(String id) {
                return null;
            }

            @Override
            public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
                return null;
            }

            @Override
            public Set<RoleModel> getScopeMappings() {
                return Collections.emptySet();
            }

            @Override
            public Set<RoleModel> getRealmScopeMappings() {
                return Collections.emptySet();
            }

            @Override
            public boolean hasScope(RoleModel role) {
                return false;
            }
        };
    }
}
