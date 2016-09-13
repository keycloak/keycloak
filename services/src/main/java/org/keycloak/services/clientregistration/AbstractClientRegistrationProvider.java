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
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA265PairwiseSubMapper;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.ForbiddenException;
import org.keycloak.services.validation.ClientValidator;
import org.keycloak.services.validation.PairwiseClientValidator;
import org.keycloak.services.validation.ValidationMessages;

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

    public ClientRepresentation create(ClientRepresentation client) {
        event.event(EventType.CLIENT_REGISTER);

        auth.requireCreate();

        ValidationMessages validationMessages = new ValidationMessages();
        if (!ClientValidator.validate(client, validationMessages) || !PairwiseClientValidator.validate(session, client, validationMessages)) {
            String errorCode = validationMessages.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(
                    errorCode,
                    validationMessages.getStringMessages(),
                    Response.Status.BAD_REQUEST
            );
        }

        try {
            ClientModel clientModel = RepresentationToModel.createClient(session, session.getContext().getRealm(), client, true);
            OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
            if (configWrapper.getSubjectType().equals(SubjectType.PAIRWISE)) {
                addPairwiseSubMapper(clientModel, configWrapper.getSectorIdentifierUri());
            }

            client = ModelToRepresentation.toRepresentation(clientModel);

            client.setSecret(clientModel.getSecret());

            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, clientModel);
            client.setRegistrationAccessToken(registrationAccessToken);

            if (auth.isInitialAccessToken()) {
                ClientInitialAccessModel initialAccessModel = auth.getInitialAccessModel();
                initialAccessModel.decreaseRemainingCount();
            }

            if (auth.isRegistrationHostTrusted()) {
                ClientRegistrationTrustedHostModel trustedHost = auth.getTrustedHostModel();
                trustedHost.decreaseRemainingCount();
            }

            event.client(client.getClientId()).success();
            return client;
        } catch (ModelDuplicateException e) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use", Response.Status.BAD_REQUEST);
        }
    }

    public ClientRepresentation get(String clientId) {
        event.event(EventType.CLIENT_INFO);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireView(client);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    public ClientRepresentation update(String clientId, ClientRepresentation rep) {
        event.event(EventType.CLIENT_UPDATE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        if (!client.getClientId().equals(rep.getClientId())) {
            throw new ErrorResponseException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier modified", Response.Status.BAD_REQUEST);
        }

        ValidationMessages validationMessages = new ValidationMessages();
        if (!ClientValidator.validate(rep, validationMessages) || !PairwiseClientValidator.validate(session, rep, validationMessages)) {
            String errorCode = validationMessages.fieldHasError("redirectUris") ? ErrorCodes.INVALID_REDIRECT_URI : ErrorCodes.INVALID_CLIENT_METADATA;
            throw new ErrorResponseException(
                    errorCode,
                    validationMessages.getStringMessages(),
                    Response.Status.BAD_REQUEST
            );
        }

        updateSubjectType(rep, client);
        RepresentationToModel.updateClient(rep, client);
        rep = ModelToRepresentation.toRepresentation(client);

        if (auth.isRegistrationAccessToken()) {
            String registrationAccessToken = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, client);
            rep.setRegistrationAccessToken(registrationAccessToken);
        }

        event.client(client.getClientId()).success();
        return rep;
    }

    private void updateSubjectType(ClientRepresentation rep, ClientModel client) {
        OIDCAdvancedConfigWrapper repConfigWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(rep);
        SubjectType repSubjectType = repConfigWrapper.getSubjectType();
        OIDCAdvancedConfigWrapper clientConfigWrapper = OIDCAdvancedConfigWrapper.fromClientModel(client);
        SubjectType clientSubjectType = clientConfigWrapper.getSubjectType();

        if (repSubjectType.equals(SubjectType.PAIRWISE) && clientSubjectType.equals(SubjectType.PAIRWISE)) {
            updateSectorIdentifier(client, repConfigWrapper.getSectorIdentifierUri());
        }

        if (repSubjectType.equals(SubjectType.PAIRWISE) && clientSubjectType.equals(SubjectType.PUBLIC)) {
            addPairwiseSubMapper(client, repConfigWrapper.getSectorIdentifierUri());
        }

        if (repSubjectType.equals(SubjectType.PUBLIC) && clientSubjectType.equals(SubjectType.PAIRWISE)) {
            removePairwiseSubMapper(client);
        }
    }

    private void updateSectorIdentifier(ClientModel client, String sectorIdentifierUri) {
        client.getProtocolMappers().stream().filter(mapping -> mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX)).forEach(mapping -> {
            mapping.getConfig().put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
        });
    }

    private void addPairwiseSubMapper(ClientModel client, String sectorIdentifierUri) {
        client.addProtocolMapper(SHA265PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri));
    }

    private void removePairwiseSubMapper(ClientModel client) {
        for (ProtocolMapperModel mapping : client.getProtocolMappers()) {
            if (mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX)) {
                client.removeProtocolMapper(mapping);
            }
        }
    }

    public void delete(String clientId) {
        event.event(EventType.CLIENT_DELETE).client(clientId);

        ClientModel client = session.getContext().getRealm().getClientByClientId(clientId);
        auth.requireUpdate(client);

        if (session.getContext().getRealm().removeClient(client.getId())) {
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
    public void setEvent(EventBuilder event) {
        this.event = event;
    }

    @Override
    public void close() {
    }

}
