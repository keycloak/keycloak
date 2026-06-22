package org.keycloak.protocol.oidc.rar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
            processor.afterAuthorizationDetailsProcessed(userSession, clientSessionCtx, authzDetailResponse.asSubtype(processor.getSupportedResponseJavaType()));
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
                AuthorizationDetailsJSONRepresentation subAuthzDetail = authzDetail.asSubtype(processor.getSupportedResponseJavaType());
                outAuthzDetails.add(processor.sanitizeBeforeSendingTokenResponse(subAuthzDetail));
            }
            tokenResponse.setAuthorizationDetails(outAuthzDetails);
        }
    }

    // Private ---------------------------------------------------------------------------------------------------------

    private Map<String, AuthorizationDetailsProcessor<?>> getAuthorizationDetailsProcessorMap() {
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                .collect(Collectors.toMap(ProviderFactory::getId, factory -> (AuthorizationDetailsProcessor<?>) session.getProvider(AuthorizationDetailsProcessor.class, factory.getId())));
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
