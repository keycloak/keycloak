package org.keycloak.federation.scim.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.federation.scim.core.exceptions.ScimExceptionHandler;
import org.keycloak.federation.scim.core.exceptions.ScimPropagationException;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopApproach;
import org.keycloak.federation.scim.core.exceptions.SkipOrStopStrategy;
import org.keycloak.federation.scim.core.service.AbstractScimService;
import org.keycloak.federation.scim.core.service.GroupScimService;
import org.keycloak.federation.scim.core.service.UserScimService;
import org.keycloak.models.KeycloakSession;

/**
 * In charge of sending SCIM Request to all registered Scim endpoints.
 */
public class ScimDispatcher {

    private static final Logger LOGGER = Logger.getLogger(ScimDispatcher.class);

    private final KeycloakSession session;
    private final ScimExceptionHandler exceptionHandler;
    private final SkipOrStopStrategy skipOrStopStrategy;
    private final List<UserScimService> userScimServices = new ArrayList<>();
    private final List<GroupScimService> groupScimServices = new ArrayList<>();
    private boolean clientsInitialized = false;

    public ScimDispatcher(KeycloakSession session) {
        this.session = session;
        this.exceptionHandler = new ScimExceptionHandler(session);
        // By default, use a permissive Skip or Stop strategy
        this.skipOrStopStrategy = SkipOrStopApproach.ALWAYS_SKIP_AND_CONTINUE;
    }

    /**
     * Lists all active ScimStorageProviderFactory and create new ScimClients for each of them
     */
    public void refreshActiveScimEndpoints() {
        // Step 1: close existing clients (as configuration may have changed)
        groupScimServices.forEach(GroupScimService::close);
        groupScimServices.clear();
        userScimServices.forEach(UserScimService::close);
        userScimServices.clear();

        // Step 2: Get All SCIM endpoints defined in Admin Console (enabled ScimStorageProviderFactory)
        session.getContext().getRealm().getComponentsStream().filter(
                m -> ScimUserStorageProviderFactory.ID.equals(m.getProviderId()) && m.get("enabled", true))
                .forEach(scimEndpointConfigurationRaw -> {
                    try {
                        ScimEndPointConfiguration scrimEndPointConfiguration = new ScimEndPointConfiguration(
                                scimEndpointConfigurationRaw);

                        // Step 3 : create scim clients for each endpoint
                        if (scimEndpointConfigurationRaw.get(ScimEndPointConfiguration.CONF_KEY_PROPAGATION_GROUP, false)) {
                            GroupScimService groupScimService = new GroupScimService(session, scrimEndPointConfiguration,
                                    skipOrStopStrategy);
                            groupScimServices.add(groupScimService);
                        }
                        if (scimEndpointConfigurationRaw.get(ScimEndPointConfiguration.CONF_KEY_PROPAGATION_USER, false)) {
                            UserScimService userScimService = new UserScimService(session, scrimEndPointConfiguration,
                                    skipOrStopStrategy);
                            userScimServices.add(userScimService);
                        }
                    } catch (IllegalArgumentException e) {
                        if (skipOrStopStrategy.allowInvalidEndpointConfiguration()) {
                            LOGGER.warn("[SCIM] Invalid Endpoint configuration " + scimEndpointConfigurationRaw.getId(), e);
                        } else {
                            throw e;
                        }
                    }
                });
    }

    public void dispatchUserModificationToAll(SCIMPropagationConsumer<UserScimService> operationToDispatch) {
        initializeClientsIfNeeded();
        Set<UserScimService> servicesCorrectlyPropagated = new LinkedHashSet<>();
        userScimServices.forEach(userScimService -> {
            try {
                operationToDispatch.acceptThrows(userScimService);
                servicesCorrectlyPropagated.add(userScimService);
            } catch (ScimPropagationException e) {
                exceptionHandler.handleException(userScimService.getConfiguration(), e);
            }
        });
        // TODO we could iterate on servicesCorrectlyPropagated to undo modification on already handled SCIM endpoints
        LOGGER.infof("[SCIM] User operation dispatched to %d SCIM server", servicesCorrectlyPropagated.size());
    }

    public void dispatchGroupModificationToAll(SCIMPropagationConsumer<GroupScimService> operationToDispatch) {
        initializeClientsIfNeeded();
        Set<GroupScimService> servicesCorrectlyPropagated = new LinkedHashSet<>();
        groupScimServices.forEach(groupScimService -> {
            try {
                operationToDispatch.acceptThrows(groupScimService);
                servicesCorrectlyPropagated.add(groupScimService);
            } catch (ScimPropagationException e) {
                exceptionHandler.handleException(groupScimService.getConfiguration(), e);
            }
        });
        // TODO we could iterate on servicesCorrectlyPropagated to undo modification on already handled SCIM endpoints
        LOGGER.infof("[SCIM] Group operation dispatched to %d SCIM server", servicesCorrectlyPropagated.size());
    }

    public void dispatchUserModificationToOne(ComponentModel scimServerConfiguration,
            SCIMPropagationConsumer<UserScimService> operationToDispatch) {
        initializeClientsIfNeeded();
        // Scim client should already have been created
        Optional<UserScimService> matchingClient = userScimServices.stream()
                .filter(u -> u.getConfiguration().getId().equals(scimServerConfiguration.getId())).findFirst();
        if (matchingClient.isPresent()) {
            try {
                operationToDispatch.acceptThrows(matchingClient.get());
                LOGGER.infof("[SCIM] User operation dispatched to SCIM server %s",
                        matchingClient.get().getConfiguration().getName());
            } catch (ScimPropagationException e) {
                exceptionHandler.handleException(matchingClient.get().getConfiguration(), e);
            }
        } else {
            LOGGER.error("[SCIM] Could not find a Scim Client matching User endpoint configuration"
                    + scimServerConfiguration.getId());
        }
    }

    public void dispatchGroupModificationToOne(ComponentModel scimServerConfiguration,
            SCIMPropagationConsumer<GroupScimService> operationToDispatch) {
        initializeClientsIfNeeded();
        // Scim client should already have been created
        Optional<GroupScimService> matchingClient = groupScimServices.stream()
                .filter(u -> u.getConfiguration().getId().equals(scimServerConfiguration.getId())).findFirst();
        if (matchingClient.isPresent()) {
            try {
                operationToDispatch.acceptThrows(matchingClient.get());
                LOGGER.infof("[SCIM] Group operation dispatched to SCIM server %s",
                        matchingClient.get().getConfiguration().getName());
            } catch (ScimPropagationException e) {
                exceptionHandler.handleException(matchingClient.get().getConfiguration(), e);
            }
        } else {
            LOGGER.error("[SCIM] Could not find a Scim Client matching Group endpoint configuration"
                    + scimServerConfiguration.getId());
        }
    }

    public void close() {
        for (GroupScimService c : groupScimServices) {
            c.close();
        }
        for (UserScimService c : userScimServices) {
            c.close();
        }
        groupScimServices.clear();
        userScimServices.clear();
    }

    private void initializeClientsIfNeeded() {
        if (!clientsInitialized) {
            clientsInitialized = true;
            refreshActiveScimEndpoints();
        }
    }

    /**
     * A Consumer that throws ScimPropagationException.
     *
     * @param <T> An {@link AbstractScimService to call}
     */
    @FunctionalInterface
    public interface SCIMPropagationConsumer<T> {

        void acceptThrows(T elem) throws ScimPropagationException;

    }
}
