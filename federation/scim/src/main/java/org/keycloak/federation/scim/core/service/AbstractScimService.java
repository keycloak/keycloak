package org.keycloak.federation.scim.core.service;

import de.captaingoldfish.scim.sdk.common.resources.ResourceNode;
import de.captaingoldfish.scim.sdk.common.resources.complex.Meta;
import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;
import org.keycloak.federation.scim.core.exceptions.InconsistentScimMappingException;
import org.keycloak.federation.scim.core.exceptions.InvalidResponseFromScimEndpointException;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopStrategy;
import org.keycloak.federation.scim.core.exceptions.UnexpectedScimDataException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A service in charge of synchronisation (CRUD) between a Keykloak Role (UserModel, GroupModel) and a SCIM Resource
 * (User,Group).
 *
 * @param <K> The Keycloack Model (e.g. UserModel, GroupModel)
 * @param <S> The SCIM Resource (e.g. User, Group)
 */
public abstract class AbstractScimService<K extends RoleMapperModel, S extends ResourceNode> implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(AbstractScimService.class);
    protected final SkipOrStopStrategy skipOrStopStrategy;
    private final KeycloakSession keycloakSession;
    private final ScrimEndPointConfiguration scimProviderConfiguration;
    private final ScimResourceType type;
    private final ScimClient<S> scimClient;

    protected AbstractScimService(KeycloakSession keycloakSession, ScrimEndPointConfiguration scimProviderConfiguration,
            ScimResourceType type, SkipOrStopStrategy skipOrStopStrategy) {
        this.keycloakSession = keycloakSession;
        this.scimProviderConfiguration = scimProviderConfiguration;
        this.type = type;
        this.scimClient = ScimClient.open(scimProviderConfiguration, type);
        this.skipOrStopStrategy = skipOrStopStrategy;
    }

    public void create(K roleMapperModel) throws InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        if (isMarkedToIgnore(roleMapperModel)) {
            // Silently return: resource is explicitly marked as to ignore
            return;
        }
        // If mapping, then we are trying to recreate a user that was already created by import
        KeycloakId id = getId(roleMapperModel);
        if (findMappingById(id) != null) {
            throw new InconsistentScimMappingException(
                    "Trying to create user with id " + id + ": id already exists in Keycloak database");
        }
        S scimForCreation = scimRequestBodyForCreate(roleMapperModel);
        String externalId = scimClient.create(id, scimForCreation);
        switch (type) {
            case USER -> UserModel.class.cast(roleMapperModel).setSingleAttribute("SCIM_ID", externalId);
            case GROUP -> GroupModel.class.cast(roleMapperModel).setSingleAttribute("SCIM_ID", externalId);
        }
    }

    public void update(K roleMapperModel) throws InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        if (isMarkedToIgnore(roleMapperModel)) {
            // Silently return: resource is explicitly marked as to ignore
            return;
        }
        KeycloakId keycloakId = getId(roleMapperModel);
        String entityOnRemoteScimId = findMappingById(keycloakId);
        if (entityOnRemoteScimId == null) {
            throw new InconsistentScimMappingException("Failed to find SCIM mapping for " + keycloakId);
        }
        S scimForReplace = scimRequestBodyForUpdate(roleMapperModel, entityOnRemoteScimId);
        scimClient.update(entityOnRemoteScimId, scimForReplace);
    }

    protected abstract S scimRequestBodyForUpdate(K roleMapperModel, String externalId)
            throws InconsistentScimMappingException;

    public void delete(String externalId) throws InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        scimClient.delete(externalId);
    }

    public void pushAllResourcesToScim(SynchronizationResult syncRes)
            throws InvalidResponseFromScimEndpointException, InconsistentScimMappingException {
        LOGGER.info("[SCIM] Push resources to endpoint  " + this.getConfiguration().getEndPoint());
        try (Stream<K> resourcesStream = getResourceStream()) {
            Set<K> resources = resourcesStream.collect(Collectors.toUnmodifiableSet());
            for (K resource : resources) {
                KeycloakId id = getId(resource);
                pushSingleResourceToScim(syncRes, resource, id);
            }
        }
    }

    public void pullAllResourcesFromScim(SynchronizationResult syncRes)
            throws UnexpectedScimDataException, InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        LOGGER.info("[SCIM] Pull resources from endpoint " + this.getConfiguration().getEndPoint());
        for (S resource : scimClient.listResources()) {
            pullSingleResourceFromScim(syncRes, resource);
        }
    }

    private void pushSingleResourceToScim(SynchronizationResult syncRes, K resource, KeycloakId id)
            throws InvalidResponseFromScimEndpointException, InconsistentScimMappingException {
        try {
            LOGGER.infof("[SCIM] Reconciling local resource %s", id);
            if (shouldIgnoreForScimSynchronization(resource)) {
                LOGGER.infof("[SCIM] Skip local resource %s", id);
                return;
            }
            if (findMappingById(id) != null) {
                LOGGER.info("[SCIM] Replacing it");
                update(resource);
            } else {
                LOGGER.info("[SCIM] Creating it");
                create(resource);
            }
            syncRes.increaseUpdated();
        } catch (InvalidResponseFromScimEndpointException e) {
            if (skipOrStopStrategy.allowPartialSynchronizationWhenPushingToScim(this.getConfiguration())) {
                LOGGER.warn("Error while syncing " + id + " to endpoint " + getConfiguration().getEndPoint(), e);
            } else {
                throw e;
            }
        } catch (InconsistentScimMappingException e) {
            if (skipOrStopStrategy.allowPartialSynchronizationWhenPushingToScim(this.getConfiguration())) {
                LOGGER.warn("Inconsistent data for element " + id + " and endpoint " + getConfiguration().getEndPoint(), e);
            } else {
                throw e;
            }
        }
    }

    private void pullSingleResourceFromScim(SynchronizationResult syncRes, S resource)
            throws UnexpectedScimDataException, InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        try {
            LOGGER.infof("[SCIM] Reconciling remote resource %s", resource);
            String externalId = resource.getId().orElse(null);
            if (externalId == null) {
                throw new UnexpectedScimDataException("Remote SCIM resource doesn't have an id, cannot import it in Keycloak");
            }
            if (validMappingAlreadyExists(externalId))
                return;

            // Here no keycloak user/group matching the SCIM external id exists
            // Try to match existing keycloak resource by properties (username, email, name)
            Optional<KeycloakId> mapped = matchKeycloakMappingByScimProperties(resource);
            if (mapped.isPresent()) {
                // If found a mapped, update
                LOGGER.info(
                        "[SCIM] Matched SCIM resource " + externalId + " from properties with keycloak entity " + mapped.get());
                syncRes.increaseUpdated();
            } else {
                // If not, create it locally or deleting it remotely (according to the configured Import Action)
                createLocalOrDeleteRemote(syncRes, resource, externalId);
            }
        } catch (UnexpectedScimDataException e) {
            if (skipOrStopStrategy.skipInvalidDataFromScimEndpoint(getConfiguration())) {
                LOGGER.warn("[SCIM] Skipping element synchronisation because of invalid Scim Data for element "
                        + resource.getId() + " : " + e.getMessage(), e);
            } else {
                throw e;
            }
        } catch (InconsistentScimMappingException e) {
            if (skipOrStopStrategy.allowPartialSynchronizationWhenPullingFromScim(getConfiguration())) {
                LOGGER.warn("[SCIM] Skipping element synchronisation because of inconsistent mapping for element "
                        + resource.getId() + " : " + e.getMessage(), e);
            } else {
                throw e;
            }
        } catch (InvalidResponseFromScimEndpointException e) {
            // Can only occur in case of a DELETE_REMOTE conflict action
            if (skipOrStopStrategy.allowPartialSynchronizationWhenPullingFromScim(getConfiguration())) {
                LOGGER.warn("[SCIM] Could not  delete SCIM resource " + resource.getId() + " during synchronisation", e);
            } else {
                throw e;
            }
        }

    }

    private boolean validMappingAlreadyExists(String externalId) {
        String optionalMapping = findByExternalId(externalId, type);
        // If an existing mapping exists, delete potential dangling references
        if (optionalMapping != null) {
            if (entityExists(new KeycloakId(optionalMapping))) {
                LOGGER.info("[SCIM] Valid mapping found, skipping");
                return true;
            }
        }
        return false;
    }

    protected String findByExternalId(String externalId, ScimResourceType type) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        if (ScimResourceType.USER.equals(type)) {
            return keycloakSession.users().searchForUserByUserAttributeStream(realm, "SCIM_ID", externalId)
                    .map(UserModel::getId).findFirst().orElse(null);
        } else if (ScimResourceType.GROUP.equals(type)) {
            return keycloakSession.groups().searchGroupsByAttributes(realm, Map.of("SCIM_ID", externalId), -1, -1)
                    .map(GroupModel::getId).findFirst().orElse(null);
        }
        return null;
    }

    private void createLocalOrDeleteRemote(SynchronizationResult syncRes, S resource, String externalId)
            throws UnexpectedScimDataException, InconsistentScimMappingException, InvalidResponseFromScimEndpointException {
        switch (scimProviderConfiguration.getImportAction()) {
            case CREATE_LOCAL -> {
                LOGGER.info("[SCIM] Create local resource for SCIM resource " + externalId);
                KeycloakId id = createEntity(resource);
                RealmModel realm = keycloakSession.getContext().getRealm();
                switch (type) {
                    case USER -> {
                        UserModel.class.cast(keycloakSession.users().getUserById(realm, id.asString())).setSingleAttribute("SCIM_ID", externalId);
                    }
                    case GROUP -> GroupModel.class.cast(keycloakSession.groups().getGroupById(realm, id.asString())).setSingleAttribute("SCIM_ID", externalId);
                }
                syncRes.increaseAdded();
            }
            case DELETE_REMOTE -> {
                LOGGER.info("[SCIM] Delete remote resource " + externalId);
                scimClient.delete(externalId);
            }
            case NOTHING -> LOGGER.info("[SCIM] Import action set to NOTHING");
        }
    }

    protected abstract S scimRequestBodyForCreate(K roleMapperModel) throws InconsistentScimMappingException;

    protected abstract KeycloakId getId(K roleMapperModel);

    protected abstract boolean isMarkedToIgnore(K roleMapperModel);

    protected String findMappingById(KeycloakId keycloakId) {
        UserModel user = keycloakSession.users().getUserById(keycloakSession.getContext().getRealm(), keycloakId.asString());
        if (user == null) {
            return null;
        }
        return user.getFirstAttribute("SCIM_ID");
    }

    private KeycloakSession getKeycloakSession() {
        return keycloakSession;
    }

    protected abstract boolean shouldIgnoreForScimSynchronization(K resource);

    protected abstract Stream<K> getResourceStream();

    protected abstract KeycloakId createEntity(S resource) throws UnexpectedScimDataException, InconsistentScimMappingException;

    protected abstract Optional<KeycloakId> matchKeycloakMappingByScimProperties(S resource)
            throws InconsistentScimMappingException;

    protected abstract boolean entityExists(KeycloakId keycloakId);

    public void sync(SynchronizationResult syncRes)
            throws InconsistentScimMappingException, InvalidResponseFromScimEndpointException, UnexpectedScimDataException {
        if (this.scimProviderConfiguration.isPullFromScimSynchronisationActivated()) {
            this.pullAllResourcesFromScim(syncRes);
        }
        if (this.scimProviderConfiguration.isPushToScimSynchronisationActivated()) {
            this.pushAllResourcesToScim(syncRes);
        }
    }

    protected Meta newMetaLocation(String externalId) {
        Meta meta = new Meta();
        URI uri = getUri(type, externalId);
        meta.setLocation(uri.toString());
        return meta;
    }

    protected URI getUri(ScimResourceType type, String externalId) {
        try {
            return new URI("%s/%s".formatted(type.getEndpoint(), externalId));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(
                    "should never occur: can not format URI for type %s and id %s".formatted(type, externalId), e);
        }
    }

    protected KeycloakDao getKeycloakDao() {
        return new KeycloakDao(getKeycloakSession());
    }

    @Override
    public void close() {
        scimClient.close();
    }

    public ScrimEndPointConfiguration getConfiguration() {
        return scimProviderConfiguration;
    }
}
