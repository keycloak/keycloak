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

package org.keycloak.services.clientregistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientInitialAccessModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientRegistrationAccessTokenConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdatedContext;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.validation.ValidationUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class AbstractClientRegistrationProvider implements ClientRegistrationProvider {

    protected KeycloakSession session;
    protected EventBuilder event;
    protected ClientRegistrationAuth auth;

    public AbstractClientRegistrationProvider(KeycloakSession session) {
        this.session = session;
    }

    public ClientRepresentation create(ClientRegistrationContext context) {
        ClientRepresentation client = context.getClient();
        if(client.getOptionalClientScopes() != null && client.getDefaultClientScopes() == null) {
            client.setDefaultClientScopes(List.of(OIDCLoginProtocolFactory.BASIC_SCOPE));
        }

        event.event(EventType.CLIENT_REGISTER);

        RegistrationAuth registrationAuth = auth.requireCreate(context);

        try {
            RealmModel realm = session.getContext().getRealm();
            ClientModel clientModel = ClientManager.createClient(session, realm, client);

            if (client.getDefaultRoles() != null) {
                for (String name : client.getDefaultRoles()) {
                    addDefaultRole(clientModel, name);
                }
            }

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(client.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            session.getContext().setClient(clientModel);
            session.clientPolicy().triggerOnEvent(new DynamicClientRegisteredContext(context, clientModel, auth.getJwt(), realm));
            ClientRegistrationPolicyManager.triggerAfterRegister(context, registrationAuth, clientModel);

            client = ModelToRepresentation.toRepresentation(clientModel, session);

            client.setSecret(clientModel.getSecret());

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel, registrationAuth, getAllowedOrigins());
            client.setRegistrationAccessToken(registrationAccessToken);

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                session.realms().decreaseRemainingCount(realm, initialAccessModel);
            }

            client.setDirectAccessGrantsEnabled(clientModel.isDirectAccessGrantsEnabled());

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                client.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(client.getClientId()).success();
            return client;
        } catch (ModelDuplicateException e) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use", Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
    }

    public ClientRepresentation get(ClientModel client) {
        event.event(EventType.CLIENT_INFO);

        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
        if (!(Boolean.TRUE.equals(rep.isBearerOnly()) || Boolean.TRUE.equals(rep.isPublicClient()))) {
            rep.setSecret(client.getSecret());
        }

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateTokenSignature(session, auth);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        Stream<String> defaultRolesNames = getDefaultRolesStream(client);
        if (defaultRolesNames != null) {
            rep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public ClientRepresentation update(String clientId, ClientRegistrationContext context) {
        ClientRepresentation rep = context.getClient();

        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        session.setAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED, true);
        RegistrationAuth registrationAuth = auth.requireUpdate(context, client);

        if (!client.getClientId().equals(rep.getClientId())) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier modified", Response.Status.BAD_REQUEST);
        }

        RepresentationToModel.updateClient(rep, client, session);
        RepresentationToModel.updateClientProtocolMappers(rep, client);
        RepresentationToModel.updateClientScopes(rep, client);

        if (rep.getDefaultRoles() != null) {
            updateDefaultRoles(client, rep.getDefaultRoles());
        }

        rep = ModelToRepresentation.toRepresentation(client, session);

        rep.setSecret(client.getSecret());

        Stream<String> defaultRolesNames = getDefaultRolesStream(client);
        if (defaultRolesNames != null) {
            rep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
        }

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken;
            if ((boolean) session.getAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED)) {
                registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client, auth.getRegistrationAuth(), getAllowedOrigins());
            } else {
                registrationAccessToken = ClientRegistrationTokenUtils.updateTokenSignature(session, auth);
            }
            rep.setRegistrationAccessToken(registrationAccessToken);
        }
        session.removeAttribute(ClientRegistrationAccessTokenConstants.ROTATION_ENABLED);

        try {
            session.getContext().setClient(client);
            session.clientPolicy().triggerOnEvent(new DynamicClientUpdatedContext(session, client, auth.getJwt(), client.getRealm()));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }
        ClientRegistrationPolicyManager.triggerAfterUpdate(context, registrationAuth, client);

        event.client(client.getClientId()).success();
        return rep;
    }


    public void delete(String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireDelete(client);

        if (new ClientManager(new RealmManager(session)).removeClient(session.getContext().getRealm(), client)) {
            event.client(client.getClientId()).success();
        } else {
            throw new ForbiddenException();
        }
    }

    public void validateClient(ClientModel clientModel, OIDCClientRepresentation oidcClient, boolean create) {
        ValidationUtil.validateClient(session, clientModel, oidcClient, create, r -> {
            session.getTransactionManager().setRollbackOnly();
            String errorCode = r.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(errorCode, r.getAllErrorsAsString(), Response.Status.BAD_REQUEST);
        });
    }

    public void validateClient(ClientRepresentation clientRep, boolean create) {
        validateClient(session.getContext().getRealm().getClientByClientId(clientRep.getClientId()), null, create);
    }

    @Override
    public void setAuth(ClientRegistrationAuth auth) {
        this.auth = auth;
    }

    @Override
    public ClientRegistrationAuth getAuth() {
        return this.auth;
    }

    @Override
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public EventBuilder getEvent() {
        return event;
    }

    @Override
    public void close() {
    }

    /* ===========  default roles =========== */

    private void addDefaultRole(ClientModel client, String name) {
        client.getRealm().getDefaultRole().addCompositeRole(getOrAddRoleId(client, name));
    }

    private RoleModel getOrAddRoleId(ClientModel client, String name) {
        RoleModel role = client.getRole(name);
        if (role == null) {
            role = client.addRole(name);
        }
        return role;
    }

    private Stream<String> getDefaultRolesStream(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName);
    }

    private void updateDefaultRoles(ClientModel client, String... defaultRoles) {
        List<String> defaultRolesArray = Arrays.asList(String.valueOf(defaultRoles));
        Collection<String> entities = getDefaultRolesStream(client).collect(Collectors.toList());
        Set<String> already = new HashSet<>();
        ArrayList<String> remove = new ArrayList<>();
        for (String rel : entities) {
            if (! defaultRolesArray.contains(rel)) {
                remove.add(rel);
            } else {
                already.add(rel);
            }
        }
        removeDefaultRoles(client, remove.toArray(new String[] {}));

        for (String roleName : defaultRoles) {
            if (!already.contains(roleName)) {
                addDefaultRole(client, roleName);
            }
        }
    }

    private void removeDefaultRoles(ClientModel client, String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            client.getRealm().getDefaultRole().removeCompositeRole(client.getRole(defaultRole));
        }
    }

    protected List<String> getAllowedOrigins() {
        List<String> allowedOrigins = new LinkedList<>();
        AccessToken jwt = auth.getJwt();
        if (jwt != null && jwt.getAllowedOrigins() != null) {
            allowedOrigins.addAll(jwt.getAllowedOrigins());
        }
        return allowedOrigins;
    }
}
