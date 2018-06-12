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
package org.keycloak.protocol.openshift.clientstorage;

import org.keycloak.models.AuthenticationFlowBindings;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.openshift.TokenReviewRequestRepresentation;
import org.keycloak.protocol.openshift.TokenReviewResponseRepresentation;
import org.keycloak.protocol.openshift.connections.rest.api.v1.ServiceAccounts;
import org.keycloak.protocol.openshift.connections.rest.apis.oauth.OAuthClients;
import org.keycloak.storage.client.AbstractReadOnlyClientStorageAdapter;
import org.keycloak.storage.client.ClientStorageProviderModel;

import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OpenshiftSAClientAdapter extends AbstractReadOnlyClientStorageAdapter implements OpenshiftClientModel {
    public static final Pattern ROLE_SCOPE_PATTERN = Pattern.compile("role:([^:]+):([^:!]+)(:[!])?");
    public static final Pattern SERVICE_ACCOUNT_PATTERN = Pattern.compile("system:serviceaccount:([^:]+):([^:]+)");
    protected final ServiceAccounts.ServiceAccountRepresentation serviceAccount;
    protected final OpenshiftClientStorageProvider provider;
    protected final String clientId;

    public static final Set<String> ALLOWED_SCOPES = new HashSet<>();

    static {
        ALLOWED_SCOPES.add("user:info");
        ALLOWED_SCOPES.add("user:check-access");
    }

    public OpenshiftSAClientAdapter(KeycloakSession session, RealmModel realm, String clientId, ServiceAccounts.ServiceAccountRepresentation serviceAccount, OpenshiftClientStorageProvider provider) {
        super(session, realm, provider.getComponent());
        this.serviceAccount = serviceAccount;
        this.provider = provider;
        this.clientId = clientId;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getName() {
        return clientId;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Set<String> getWebOrigins() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<String> getRedirectUris() {
        return serviceAccount.getOauthRedirectUris();
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
        TokenReviewRequestRepresentation request = TokenReviewRequestRepresentation.create(secret);
        Response response = provider.getOpenshiftClient().apis().kubernetesAuthentication().tokenReview().review(request);
        if (response.getStatus() < 200 || response.getStatus() > 204) {
            return false;
        }
        TokenReviewResponseRepresentation review = response.readEntity(TokenReviewResponseRepresentation.class);
        response.close();
        if (!review.getStatus().isAuthenticated()) return false;
        return clientId.equals(review.getStatus().getUser().getUsername());
    }

    @Override
    public String getSecret() {
        return "";
    }

    @Override
    public String getRegistrationToken() {
        return null;
    }

    @Override
    public String getProtocol() {
        return OIDCLoginProtocol.LOGIN_PROTOCOL;
    }

    @Override
    public String getAttribute(String name) {
        return null;
    }

    @Override
    public Map<String, String> getAttributes() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        if (!AuthenticationFlowBindings.BROWSER_BINDING.equals(binding)) return null;
        if (serviceAccount.oauthWantChallenges()) {
            AuthenticationFlowModel model = realm.getFlowByAlias(DefaultAuthenticationFlows.OPENSHIFT_CHALLENGE_FLOW);
            if (model != null) return model.getId();
        }
        return null;
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        if (serviceAccount.oauthWantChallenges()) {
            AuthenticationFlowModel model = realm.getFlowByAlias(DefaultAuthenticationFlows.OPENSHIFT_CHALLENGE_FLOW);
            if (model != null) {
                Map<String, String> map = new HashMap<>();
                map.put(AuthenticationFlowBindings.BROWSER_BINDING, model.getId());
                return map;
            }
        }

        return Collections.EMPTY_MAP;
    }
    @Override
    public boolean isFrontchannelLogout() {
        return false;
    }

    @Override
    public boolean isPublicClient() {
        return false;
    }

    @Override
    public boolean isConsentRequired() {
        return false;
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return true;
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return true;
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return true;
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return false;
    }

    @Override
    public ClientTemplateModel getClientTemplate() {
        return null;
    }

    @Override
    public boolean useTemplateScope() {
        return false;
    }

    @Override
    public boolean useTemplateMappers() {
        return false;
    }

    @Override
    public boolean useTemplateConfig() {
        return false;
    }

    @Override
    public int getNotBefore() {
        return 0;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return Collections.EMPTY_SET;
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
    public boolean isFullScopeAllowed() {
        return false;
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return false;
    }

    public Set<String> validateRequestedScope(List<String> requestedScopes) {
        Set<String> failed = new HashSet<>();
        for (String requested : requestedScopes) {
            if (ALLOWED_SCOPES.contains(requested)) continue;

            Matcher m = ROLE_SCOPE_PATTERN.matcher(requested);
            if (!m.matches()) {
                failed.add(requested);
                continue;
            }
            // I think that it has to match namespace?  Not sure.
            String namespace = m.group(2);
            if (!serviceAccount.getNamespace().equals(namespace)) {
                failed.add(requested);
            }
        }
        return failed;
    }


}
