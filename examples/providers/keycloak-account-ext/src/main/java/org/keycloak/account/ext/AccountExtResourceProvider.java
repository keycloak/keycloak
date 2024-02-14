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
package org.keycloak.account.ext;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.http.HttpRequest;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.ClientRepresentation;
import org.keycloak.representations.account.ConsentRepresentation;
import org.keycloak.representations.account.ConsentScopeRepresentation;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.theme.Theme;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountExtResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private Locale locale;
    //protected final Cache<String, SessionEntity> sessionCache;

    public AccountExtResourceProvider(KeycloakSession session) {//) Cache<String, SessionEntity> sessionCache) {
        this.session = session;
        //this.sessionCache = sessionCache;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/applications")
    public Stream<ClientRepresentation> get(@QueryParam("expired") int expired) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = getAccountManagementClient(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setAudience(client.getClientId())
                .authenticate();

        if (authResult == null) {
            throw new NotAuthorizedException("Bearer token required");
        }
        Auth auth = new Auth(session.getContext().getRealm(), authResult.getToken(), authResult.getUser(), client, authResult.getSession(), false);

        if (authResult.getUser().getServiceAccountClientLink() != null) {
            throw new NotAuthorizedException("Service accounts are not allowed to access this service");
        }

        HttpRequest request = session.getContext().getHttpRequest();
        HttpResponse response = session.getContext().getHttpResponse();
        Cors.add(request).allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").auth().build(response);

        UserModel user = auth.getUser();
        this.locale = session.getContext().resolveLocale(user);

        Set<String> inUseClients = session.sessions().getUserSessionsStream(realm, user)
                .flatMap(s -> s.getAuthenticatedClientSessions().values().stream())
                .map(AuthenticatedClientSessionModel::getClient)
                .map(ClientModel::getClientId)
                .collect(Collectors.toSet());

        Set<String> offlineClients = session.sessions().getOfflineUserSessionsStream(realm, user)
                .flatMap(s -> s.getAuthenticatedClientSessions().values().stream())
                .map(AuthenticatedClientSessionModel::getClient)
                .map(ClientModel::getClientId)
                .collect(Collectors.toSet());

        Map<String, UserConsentModel> consentModels = session.users().getConsentsStream(realm, user.getId())
                .collect(Collectors.toMap(c -> c.getClient().getClientId(), Function.identity()));

        return getApplications(session, realm, user)
                .filter(c -> !isAdminClient(c) || AdminPermissions.realms(session, realm, user).isAdmin())
                .map(c -> modelToRepresentation(c, inUseClients, offlineClients, consentModels))
                .sorted((c1, c2) -> c1.getClientId().compareTo(c2.getClientId()));
    }

    @Override
    public void close() {
    }

    private ClientModel getAccountManagementClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            throw new NotFoundException("account management not enabled");
        }
        return client;
    }

    private static boolean isAdminClient(ClientModel client) {
        return client.getClientId().equals(Constants.ADMIN_CLI_CLIENT_ID)
                || client.getClientId().equals(Constants.ADMIN_CONSOLE_CLIENT_ID);
    }

    private Stream<RoleModel> addSubClientRoles(Stream<RoleModel> roles) {
        return addSubRoles(roles).filter(RoleModel::isClientRole);
    }

    private Stream<RoleModel> addSubRoles(Stream<RoleModel> roles) {
        return addSubRoles(roles, new HashSet<>());
    }

    private Stream<RoleModel> addSubRoles(Stream<RoleModel> roles, HashSet<RoleModel> visited) {
        List<RoleModel> roleList = roles.collect(Collectors.toList());
        visited.addAll(roleList);
        return Stream.concat(roleList.stream(), roleList.stream().flatMap(r -> addSubRoles(r.getCompositesStream().filter(s -> !visited.contains(s)), visited)));
    }

    private Stream<GroupModel> addParents(GroupModel group) {
        //no cycle check here, I hope that's fine
        if (group.getParent() == null) {
            return Stream.of(group);
        }
        return Stream.concat(Stream.of(group), addParents(group.getParent()));
    }


    private Set<String> listCompositeUsersRoleMappings(UserModel user) {
        return addSubClientRoles(Stream.concat(
                        user.getRoleMappingsStream(),
                        user.getGroupsStream().flatMap(g -> addParents(g)).flatMap(GroupModel::getRoleMappingsStream)))
                .filter(RoleModel::isClientRole)
                .map(RoleModel::getContainerId)
                .collect(Collectors.toSet());
    }


    private Stream<ClientModel> getApplications(KeycloakSession session, RealmModel realm, UserModel user) {
        Predicate<ClientModel> bearerOnly = ClientModel::isBearerOnly;

        // get clients the user has a role in it
        Set<String> clientIds = listCompositeUsersRoleMappings(user);

        return clientIds.stream().map(id -> session.clients().getClientById(realm, id)).filter(bearerOnly.negate());
    }

    private Properties getProperties() {
        try {
            return session.theme().getTheme(Theme.Type.ACCOUNT).getMessages(locale);
        } catch (IOException e) {
            return null;
        }
    }

    private ConsentRepresentation modelToRepresentation(UserConsentModel model) {
        List<ConsentScopeRepresentation> grantedScopes = model.getGrantedClientScopes().stream()
                .map(m -> new ConsentScopeRepresentation(m.getId(), m.getConsentScreenText() != null
                ? m.getConsentScreenText()
                : m.getName(), StringPropertyReplacer.replaceProperties(m.getConsentScreenText(), getProperties())))
                .collect(Collectors.toList());
        return new ConsentRepresentation(grantedScopes, model.getCreatedDate(), model.getLastUpdatedDate());
    }

    private ClientRepresentation modelToRepresentation(ClientModel model, Set<String> inUseClients,
            Set<String> offlineClients, Map<String, UserConsentModel> consents) {
        ClientRepresentation representation = new ClientRepresentation();
        representation.setClientId(model.getClientId());
        representation.setClientName(StringPropertyReplacer.replaceProperties(model.getName(), getProperties()));
        representation.setDescription(model.getDescription());
        representation.setUserConsentRequired(model.isConsentRequired());
        representation.setInUse(inUseClients.contains(model.getClientId()));
        representation.setOfflineAccess(offlineClients.contains(model.getClientId()));
        representation.setRootUrl(model.getRootUrl());
        representation.setBaseUrl(model.getBaseUrl());
        representation.setEffectiveUrl(ResolveRelative.resolveRelativeUri(session, model.getRootUrl(), model.getBaseUrl()));
        UserConsentModel consentModel = consents.get(model.getClientId());
        if (consentModel != null) {
            representation.setConsent(modelToRepresentation(consentModel));
            representation.setLogoUri(model.getAttribute(ClientModel.LOGO_URI));
            representation.setPolicyUri(model.getAttribute(ClientModel.POLICY_URI));
            representation.setTosUri(model.getAttribute(ClientModel.TOS_URI));
        }
        return representation;
    }

}
