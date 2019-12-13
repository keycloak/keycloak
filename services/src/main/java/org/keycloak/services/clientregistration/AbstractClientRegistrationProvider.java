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

import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.clientregistration.policy.ClientRegistrationPolicyManager;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.validation.ValidationMessages;
import org.keycloak.validation.ClientValidationUtil;

import javax.ws.rs.core.Response;

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

        event.event(EventType.CLIENT_REGISTER);

        RegistrationAuth registrationAuth = auth.requireCreate(context);

        ValidationMessages validationMessages = new ValidationMessages();
        if (!context.validateClient(validationMessages)) {
            String errorCode = validationMessages.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(
                    errorCode,
                    validationMessages.getStringMessages(),
                    Response.Status.BAD_REQUEST
            );
        }

        try {
            RealmModel realm = session.getContext().getRealm();
            ClientModel clientModel = new ClientManager(new RealmManager(session)).createClient(session, realm, client, true);

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(client.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            ClientRegistrationPolicyManager.triggerAfterRegister(context, registrationAuth, clientModel);

            client = ModelToRepresentation.toRepresentation(clientModel, session);

            client.setSecret(clientModel.getSecret());

            ClientValidationUtil.validate(session, clientModel, true, c -> {
                session.getTransactionManager().setRollbackOnly();
                throw  new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, c.getError(), Response.Status.BAD_REQUEST);
            });

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel, registrationAuth);
            client.setRegistrationAccessToken(registrationAccessToken);

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                session.realms().decreaseRemainingCount(realm, initialAccessModel);
            }

            event.client(client.getClientId()).success();
            return client;
        } catch (ModelDuplicateException e) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use", Response.Status.BAD_REQUEST);
        }
    }

    public ClientRepresentation get(ClientModel client) {
        event.event(EventType.CLIENT_INFO);

        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
        if (client.getSecret() != null) {
            rep.setSecret(client.getSecret());
        }

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateTokenSignature(session, auth);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public ClientRepresentation update(String clientId, ClientRegistrationContext context) {
        ClientRepresentation rep = context.getClient();

        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        RegistrationAuth registrationAuth = auth.requireUpdate(context, client);

        if (!client.getClientId().equals(rep.getClientId())) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier modified", Response.Status.BAD_REQUEST);
        }

        ValidationMessages validationMessages = new ValidationMessages();
        if (!context.validateClient(validationMessages)) {
            String errorCode = validationMessages.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(
                    errorCode,
                    validationMessages.getStringMessages(),
                    Response.Status.BAD_REQUEST
            );
        }

        RepresentationToModel.updateClient(rep, client);
        RepresentationToModel.updateClientProtocolMappers(rep, client);

        ClientValidationUtil.validate(session, client, false, c -> {
            session.getTransactionManager().setRollbackOnly();
            throw  new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, c.getError(), Response.Status.BAD_REQUEST);
        });

        rep = ModelToRepresentation.toRepresentation(client, session);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client, auth.getRegistrationAuth());
            rep.setRegistrationAccessToken(registrationAccessToken);
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

}
