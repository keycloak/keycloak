package org.keycloak.protocol.oauth2.cimd.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.utils.SubjectType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientregistration.ErrorCodes;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.validation.ClientValidationContext;
import org.keycloak.validation.ClientValidationProvider;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationResult;

import org.jboss.logging.Logger;

/**
 * The abstract class persists client metadata.
 *
 * <p>Creating and updating a client metadata:
 * The class does almost the same process in Dynamic Client Registration (DCR)in {@code OIDCClientRegistrationProvider}.
 *
 * <p>The reason is that a client sends its metadata to an authorization server in DCR by RFC 7591
 * while an authorization server fetches a client metadata in CIMD,
 * which means that only the method of getting a client metadata is different.
 *
 * <p>The reason why not directly calling methods of {@code OIDCClientRegistrationProvider} is as follows:
 * <ul>
 *     <li>{@code client_id} property is not allowed in DCR while it is mandatory in CIMD.
 *     {@code OIDCClientRegistrationProvider} does not allow client metadata including {@code client_id}. </li>
 *     <li>A registration access token is issued in DCR
 *     (to say more precisely, RFC 7592 OAuth 2.0 Dynamic Client Registration Management Protocol) while it is not needed in CIMD.
 *     {@code OIDCClientRegistrationProvider} issues the registration access token.</li>
 * </ul>
 *
 * <p>Cache expiry time:
 * The provider stores the cache expiry time in an attribute of {@link ClientRepresentation}/{@link ClientModel}.
 *
 * <p>Process when a cache expires</p>
 * Do nothing. After keycloak supports workflow for clients, it would be used to delete a client metadata.
 *
 * <p>Roles of the abstract class and its concrete class:
 * The abstract class itself covers all about persisting a client metadata while the concrete class can see
 * a configuration of the concrete class of ({@link AbstractClientIdMetadataDocumentExecutor}) and augment a client metadata based on it.
 * Moreover, the concrete class can add or modify the abstract class, which makes it easy to implement custom persistent CIMD provider.
 *
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractPersistentClientIdMetadataDocumentProvider<CONFIG extends AbstractClientIdMetadataDocumentExecutor.Configuration> implements ClientIdMetadataDocumentProvider<CONFIG> {

    protected KeycloakSession session;
    protected CONFIG configuration;

    public static final String CIMD_CACHE_EXPIRY_TIME_IN_SEC = "cimd.cache.expiry.time.in.sec";

    protected abstract Logger getLogger();

    protected AbstractPersistentClientIdMetadataDocumentProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setCacheExpiryTimeToClientMetadata(ClientRepresentation clientRep, int cacheExpiryTimeInSec) {
        clientRep.getAttributes().put(CIMD_CACHE_EXPIRY_TIME_IN_SEC, Integer.toString(cacheExpiryTimeInSec));
    }

    @Override
    public void setCacheExpiryTimeToClientMetadata(ClientModel clientModel, int cacheExpiryTimeInSec) {
        clientModel.setAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC, Integer.toString(cacheExpiryTimeInSec));
    }

    @Override
    public AbstractClientIdMetadataDocumentExecutor.FetchOperation determineFetchOperation(String clientId) {
        RealmModel realm = session.getContext().getRealm();
        ClientModel existingClientModel = realm.getClientByClientId(clientId);
        if (existingClientModel != null) {
            getLogger().debugv("client already exist: clientId = {0}", clientId);
            // Client Metadata Caching
            // if the client metadata remains effective, return
            // otherwise
            //   fetch Client ID Metadata
            //   Client Metadata verification
            //   Client Metadata validation
            //   Persist Client Metadata (overwrite)
            //  TODO: if an error occurs, the client metadata should be removed or remain persisted?
            //        -> it remains persisted. If client metadata removal by workflow is implemented, the client metadata is automatically removed.
            //        -> therefore, only returns an error response.
            if (existingClientModel.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC) != null) {
                int i = Integer.parseInt(existingClientModel.getAttribute(CIMD_CACHE_EXPIRY_TIME_IN_SEC));
                if (Time.currentTime() > i) {
                    getLogger().debugv("client need to update: clientId = {0}", clientId);
                    return AbstractClientIdMetadataDocumentExecutor.FetchOperation.UPDATE;
                } else {
                    // persisted client metadata is still effective
                    getLogger().debugv("client no need to update: clientId = {0}", clientId);
                    return AbstractClientIdMetadataDocumentExecutor.FetchOperation.NO_UPDATE;
                }
            }
        }
        getLogger().debugv("client need to create: clientId = {0}", clientId);
        return AbstractClientIdMetadataDocumentExecutor.FetchOperation.CREATE;
    }

    @Override
    public ClientModel createClientMetadata(AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration except for:
        //   - not set client registration token
        RealmModel realm = session.getContext().getRealm();
        try {
            OIDCClientRepresentation clientOIDC = clientOIDCWithCacheControl.getOidcClientRepresentation();
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDC);

            // set cache expiry time
            setCacheExpiryTimeToClientMetadata(clientRep, clientOIDCWithCacheControl.getClientMetadataCacheControl().getCacheExpiryTimeInSec());

            // augment client depending on the configuration of the CIMD executor
            augmentClientMetadata(clientRep);

            if (clientRep.getOptionalClientScopes() != null && clientRep.getDefaultClientScopes() == null) {
                clientRep.setDefaultClientScopes(List.of(OIDCLoginProtocolFactory.BASIC_SCOPE));
            }

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_REGISTER);

            ClientModel clientModel = ClientManager.createClient(session, realm, clientRep);

            if (clientRep.getDefaultRoles() != null) {
                for (String name : clientRep.getDefaultRoles()) {
                    addDefaultRole(clientModel, name);
                }
            }

            if (clientModel.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(clientModel);
            }

            if (Boolean.TRUE.equals(clientRep.getAuthorizationServicesEnabled())) {
                RepresentationToModel.createResourceServer(clientModel, session, true);
            }

            session.getContext().setClient(clientModel);

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            clientRep.setDirectAccessGrantsEnabled(clientModel.isDirectAccessGrantsEnabled());

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            clientModel = realm.getClientByClientId(clientRep.getClientId());
            updatePairwiseSubMappers(clientModel, SubjectType.parse(clientOIDC.getSubjectType()), clientOIDC.getSectorIdentifierUri());
            updateClientRepWithProtocolMappers(clientModel, clientRep);

            validateClient(clientModel, clientOIDC, true);

            return clientModel;
        } catch (ModelDuplicateException e) {
            getLogger().warnv("ModelDuplicateException: {0}", e);
            throw new ClientPolicyException(ErrorCodes.INVALID_CLIENT_METADATA, "Client Identifier in use");
        } catch (ClientPolicyException e) {
            throw e; // intentionally
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            throw invalidClientMetadata("invalid request");
        }
    }

    @Override
    public ClientModel updateClientMetadata(AbstractClientIdMetadataDocumentExecutor.OIDCClientRepresentationWithCacheControl clientOIDCWithCacheControl) throws ClientPolicyException {
        // do the same thing as in dynamic client registration except for:
        //   - not set client registration token
        RealmModel realm = session.getContext().getRealm();

        try {
            OIDCClientRepresentation clientOIDC = clientOIDCWithCacheControl.getOidcClientRepresentation();
            ClientRepresentation clientRep = DescriptionConverter.toInternal(session, clientOIDC);
            String clientId = clientOIDC.getClientId();

            // set cache expiry time
            setCacheExpiryTimeToClientMetadata(clientRep, clientOIDCWithCacheControl.getClientMetadataCacheControl().getCacheExpiryTimeInSec());

            // augment client depending on configuration
            augmentClientMetadata(clientRep);

            if (clientOIDC.getScope() != null) {
                ClientModel oldClient = realm.getClientByClientId(clientId);
                Collection<String> defaultClientScopes = oldClient.getClientScopes(true).keySet();
                clientRep.setDefaultClientScopes(new ArrayList<>(defaultClientScopes));
            }

            EventBuilder event = new EventBuilder(realm, session, session.getContext().getConnection());
            event.event(EventType.CLIENT_UPDATE).client(clientId);

            ClientModel clientModel = realm.getClientByClientId(clientId);

            if (!clientModel.getClientId().equals(clientRep.getClientId())) {
                throw invalidClientMetadata("Client Identifier modified");
            }

            ClientResource.updateClientServiceAccount(session, clientModel, clientRep.isServiceAccountsEnabled());
            RepresentationToModel.updateClient(clientRep, clientModel, session);
            RepresentationToModel.updateClientProtocolMappers(clientRep, clientModel);
            RepresentationToModel.updateClientScopes(clientRep, clientModel);

            clientRep = ModelToRepresentation.toRepresentation(clientModel, session);

            Stream<String> defaultRolesNames = getDefaultRolesStream(clientModel);
            if (defaultRolesNames != null) {
                clientRep.setDefaultRoles(defaultRolesNames.toArray(String[]::new));
            }

            event.client(clientRep.getClientId()).success();

            session.getContext().setClient(clientModel);

            clientModel = realm.getClientByClientId(clientRep.getClientId());
            updatePairwiseSubMappers(clientModel, SubjectType.parse(clientOIDC.getSubjectType()), clientOIDC.getSectorIdentifierUri());
            updateClientRepWithProtocolMappers(clientModel, clientRep);

            validateClient(clientModel, clientOIDC, false);

            return clientModel;
        } catch (ClientPolicyException e) {
            throw e; // intentionally
        } catch (Exception e) {
            getLogger().warnv("Exception: {0}", e);
            throw invalidClientMetadata("invalid request");
        }
    }

    private static ClientPolicyException invalidClientMetadata(String errorDetail) {
        return new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, errorDetail);
    }

    // the same as AbstractClientRegistrationProvider.addDefaultRole
    private void addDefaultRole(ClientModel client, String name) {
        client.getRealm().getDefaultRole().addCompositeRole(getOrAddRoleId(client, name));
    }

    // the same as AbstractClientRegistrationProvider.getOrAddRoleId
    private RoleModel getOrAddRoleId(ClientModel client, String name) {
        RoleModel role = client.getRole(name);
        if (role == null) {
            role = client.addRole(name);
        }
        return role;
    }

    // the same as AbstractClientRegistrationProvider.getDefaultRolesStream
    private Stream<String> getDefaultRolesStream(ClientModel client) {
        return client.getRealm().getDefaultRole().getCompositesStream()
                .filter(role -> role.isClientRole() && Objects.equals(role.getContainerId(), client.getId()))
                .map(RoleModel::getName);
    }

    // the same as OIDCClientRegistrationProvider.updatePairwiseSubMappers
    private void updatePairwiseSubMappers(ClientModel clientModel, SubjectType subjectType, String sectorIdentifierUri) {
        if (subjectType == SubjectType.PAIRWISE) {

            // See if we have existing pairwise mapper and update it. Otherwise create new
            AtomicBoolean foundPairwise = new AtomicBoolean(false);

            clientModel.getProtocolMappersStream().filter((ProtocolMapperModel mapping) -> {
                if (mapping.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX)) {
                    foundPairwise.set(true);
                    return true;
                } else {
                    return false;
                }
            }).toList().forEach((ProtocolMapperModel mapping) -> {
                PairwiseSubMapperHelper.setSectorIdentifierUri(mapping, sectorIdentifierUri);
                clientModel.updateProtocolMapper(mapping);
            });

            // We don't have existing pairwise mapper. So create new
            if (!foundPairwise.get()) {
                ProtocolMapperRepresentation newPairwise = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
                clientModel.addProtocolMapper(RepresentationToModel.toModel(newPairwise));
            }

        } else {
            // Rather find and remove all pairwise mappers
            clientModel.getProtocolMappersStream()
                    .filter(mapperRep -> mapperRep.getProtocolMapper().endsWith(AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX))
                    .toList()
                    .forEach(clientModel::removeProtocolMapper);
        }
    }

    // the same as OIDCClientRegistrationProvider.updateClientRepWithProtocolMappers
    private void updateClientRepWithProtocolMappers(ClientModel clientModel, ClientRepresentation rep) {
        List<ProtocolMapperRepresentation> mappings =
                clientModel.getProtocolMappersStream().map(ModelToRepresentation::toRepresentation).collect(Collectors.toList());
        rep.setProtocolMappers(mappings);
    }

    // the same as AbstractClientRegistrationProvider.updateClientRepWithProtocolMappers except for error handling
    private void validateClient(ClientModel client, OIDCClientRepresentation oidcClient, boolean create) throws ClientPolicyException{
        ClientValidationProvider provider = session.getProvider(ClientValidationProvider.class);
        if (provider != null) {
            ValidationContext.Event event = create ? ValidationContext.Event.CREATE : ValidationContext.Event.UPDATE;
            ValidationResult result;

            if (oidcClient != null) {
                result = provider.validate(new ClientValidationContext.OIDCContext(event, session, client, oidcClient));
            }
            else {
                result = provider.validate(new ClientValidationContext(event, session, client));
            }

            if (!result.isValid()) {
                getLogger().warnv("validateClient failed: {0}", result.getAllErrorsAsString());
                session.getTransactionManager().setRollbackOnly();
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid request");
            }
        }
    }
}
