package org.keycloak.protocol.oidc.rar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

public class AuthorizationDetailsProcessorManager {

    private static final Logger logger = Logger.getLogger(AuthorizationDetailsProcessorManager.class);

    private final KeycloakSession session;

    public AuthorizationDetailsProcessorManager(KeycloakSession session) {
        this.session = session;
    }

    public List<AuthorizationDetailsJSONRepresentation> processAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                                                                    String authorizationDetailsParam) throws InvalidAuthorizationDetailsException {
        return processAuthorizationDetailsInternal(authorizationDetailsParam,
                (processor, authzDetail) -> processor.process(userSession, clientSessionCtx, authzDetail));
    }


    public List<AuthorizationDetailsJSONRepresentation> processStoredAuthorizationDetails(UserSessionModel userSession,
                                                                                          ClientSessionContext clientSessionCtx,
                                                                                          String authorizationDetailsParam) throws InvalidAuthorizationDetailsException {
        return processAuthorizationDetailsInternal(authorizationDetailsParam,
                (processor, authzDetail) ->
                        processor.processStoredAuthorizationDetails(userSession, clientSessionCtx, authzDetail));
    }


    public List<AuthorizationDetailsJSONRepresentation> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        List<AuthorizationDetailsJSONRepresentation> allAuthzDetails = new ArrayList<>();
        session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                .sorted((f1, f2) -> f2.order() - f1.order())
                .map(f -> session.getProvider(AuthorizationDetailsProcessor.class, f.getId()))
                .map(processor -> processor.handleMissingAuthorizationDetails(userSession, clientSessionCtx))
                .filter(Objects::nonNull)
                .forEach(allAuthzDetails::addAll);
        return allAuthzDetails;
    }

    public void validateAuthorizationDetail(String authorizationDetailsParam) {
        processAuthorizationDetailsInternal(authorizationDetailsParam, AuthorizationDetailsProcessor::validateAuthorizationDetail);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void afterAuthorizationDetailsProcessed(UserSessionModel userSession,
                                                   ClientSessionContext clientSessionCtx,
                                                   List<AuthorizationDetailsJSONRepresentation> authorizationDetailsResponse) throws InvalidAuthorizationDetailsException {
        Map<String, AuthorizationDetailsProcessor<?>> processors = getAuthorizationDetailsProcessorMap();
        for (AuthorizationDetailsJSONRepresentation authzDetailResponse : authorizationDetailsResponse) {
            AuthorizationDetailsProcessor processor = findProcessorForAuthorizationDetails(processors, authzDetailResponse);
            processor.afterAuthorizationDetailsProcessed(userSession, clientSessionCtx, processor.narrowRepresentation(authzDetailResponse));
        }
    }

    /**
     * Sanitize authorization details before they are sent as part of the Token Response
     * https://github.com/keycloak/keycloak/issues/50079
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void sanitizeBeforeSendingTokenResponse(AccessTokenResponse tokenResponse) {
        if (tokenResponse.getAuthorizationDetails() != null) {
            List<AuthorizationDetailsJSONRepresentation> outAuthzDetails = new ArrayList<>();
            Map<String, AuthorizationDetailsProcessor<?>> processors = getAuthorizationDetailsProcessorMap();
            for (AuthorizationDetailsJSONRepresentation authzDetail : tokenResponse.getAuthorizationDetails()) {
                AuthorizationDetailsProcessor processor = findProcessorForAuthorizationDetails(processors, authzDetail);
                AuthorizationDetailsJSONRepresentation subAuthzDetail = processor.narrowRepresentation(authzDetail);
                outAuthzDetails.add(processor.sanitizeBeforeSendingTokenResponse(subAuthzDetail));
            }
            tokenResponse.setAuthorizationDetails(outAuthzDetails);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    /**
     * Builds a lookup map from authorization_details "type" to the processor handling it. A single processor may support
     * more than one type via {@link AuthorizationDetailsProcessor#getSupportedTypes()}, so the mapping is driven by the
     * supported types rather than by the {@link ProviderFactory#getId() provider id}. If multiple processors claim the
     * same type, the one whose factory has the higher {@link ProviderFactory#order() order} wins.
     */
    private Map<String, AuthorizationDetailsProcessor<?>> getAuthorizationDetailsProcessorMap() {
        Map<String, AuthorizationDetailsProcessor<?>> processorsByType = new HashMap<>();
        session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                .sorted((f1, f2) -> f2.order() - f1.order())
                .forEach(factory -> {
                    AuthorizationDetailsProcessor<?> processor = (AuthorizationDetailsProcessor<?>) session.getProvider(AuthorizationDetailsProcessor.class, factory.getId());
                    for (String supportedType : processor.getSupportedTypes()) {
                        AuthorizationDetailsProcessor<?> existing = processorsByType.putIfAbsent(supportedType, processor);
                        if (existing != null) {
                            logger.warnf("Multiple AuthorizationDetailsProcessor providers support authorization_details type '%s'. Using '%s' and ignoring '%s'.",
                                    supportedType, existing, processor);
                        }
                    }
                });
        return processorsByType;
    }

    private List<AuthorizationDetailsJSONRepresentation> processAuthorizationDetailsInternal(String authorizationDetailsParam,
                                                                                             BiFunction<AuthorizationDetailsProcessor<?>, AuthorizationDetailsJSONRepresentation, AuthorizationDetailsJSONRepresentation> function) throws InvalidAuthorizationDetailsException {

        List<AuthorizationDetailsJSONRepresentation> authzDetails = parseAuthorizationDetails(authorizationDetailsParam);
        if (authzDetails.isEmpty()) {
            throw new InvalidAuthorizationDetailsException("Authorization_Details parameter cannot be empty");
        }

        Map<String, AuthorizationDetailsProcessor<?>> processors = getAuthorizationDetailsProcessorMap();

        List<AuthorizationDetailsJSONRepresentation> authzResponses = new ArrayList<>();
        for (AuthorizationDetailsJSONRepresentation authzDetail : authzDetails) {
            AuthorizationDetailsProcessor<?> processor = findProcessorForAuthorizationDetails(processors, authzDetail);
            AuthorizationDetailsJSONRepresentation response = function.apply(processor, authzDetail);
            if (response != null) {
                authzResponses.add(response);
            } else {
                logger.debugf("Null response returned by authorization processor " + processor + " for given authorization details");
            }
        }

        return authzResponses;
    }

    private AuthorizationDetailsProcessor<?> findProcessorForAuthorizationDetails(Map<String, AuthorizationDetailsProcessor<?>> processors, AuthorizationDetailsJSONRepresentation authzDetail) {
        if (authzDetail.getType() == null) {
            throw new InvalidAuthorizationDetailsException("Authorization_Details parameter provided without type: " + authzDetail);
        }
        AuthorizationDetailsProcessor<?> processor = processors.get(authzDetail.getType());
        if (processor == null) {
            String errorDetails = String.format("Unsupported type '%s' of authorization_details parameter supplied in the request. Supported values: %s",
                    authzDetail.getType(), processors.keySet());
            logger.warn(errorDetails);
            throw new InvalidAuthorizationDetailsException(errorDetails);
        }
        return processor;
    }

    private List<AuthorizationDetailsJSONRepresentation> parseAuthorizationDetails(String authorizationDetailsParam) {
        try {
            return JsonSerialization.readValue(authorizationDetailsParam, new TypeReference<>() {});
        } catch (Exception e) {
            logger.warnf(e, "Cannot parse authorization_details: %s", authorizationDetailsParam);
            throw new InvalidAuthorizationDetailsException("Invalid authorization_details: " + authorizationDetailsParam);
        }
    }
}
