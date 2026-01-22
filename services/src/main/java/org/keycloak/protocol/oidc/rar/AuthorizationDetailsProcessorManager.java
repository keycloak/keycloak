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
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;
import org.keycloak.representations.AuthorizationDetailsResponse;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;

public class AuthorizationDetailsProcessorManager {

    private static final Logger logger = Logger.getLogger(AuthorizationDetailsProcessorManager.class);

    public List<AuthorizationDetailsResponse> processAuthorizationDetails(KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                                                          String authorizationDetailsParam) throws InvalidAuthorizationDetailsException {
        return processAuthzDetailsImpl(session, authorizationDetailsParam,
                (processor, authzDetail) -> processor.process(userSession, clientSessionCtx, authzDetail));
    }


    public List<AuthorizationDetailsResponse> processStoredAuthorizationDetails(KeycloakSession session, UserSessionModel userSession,
                                                                                ClientSessionContext clientSessionCtx,
                                                                                String authorizationDetailsParam) throws InvalidAuthorizationDetailsException {
        return processAuthzDetailsImpl(session, authorizationDetailsParam,
                (processor, authzDetail) ->
                        processor.processStoredAuthorizationDetails(userSession, clientSessionCtx, authzDetail)
                );
    }


    public List<AuthorizationDetailsResponse> handleMissingAuthorizationDetails(KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        List<AuthorizationDetailsResponse> allAuthzDetails = new ArrayList<>();
        session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                .sorted((f1, f2) -> f2.order() - f1.order())
                .map(f -> session.getProvider(AuthorizationDetailsProcessor.class, f.getId()))
                .map(processor -> processor.handleMissingAuthorizationDetails(userSession, clientSessionCtx))
                .filter(Objects::nonNull)
                .forEach(allAuthzDetails::addAll);
        return allAuthzDetails;
    }


    private List<AuthorizationDetailsResponse> processAuthzDetailsImpl(KeycloakSession session, String authorizationDetailsParam,
                                    BiFunction<AuthorizationDetailsProcessor<?>, AuthorizationDetailsJSONRepresentation, AuthorizationDetailsResponse> function) throws InvalidAuthorizationDetailsException {
        if (authorizationDetailsParam == null) {
            return null;
        }

        List<AuthorizationDetailsResponse> authzResponses = new ArrayList<>();

        List<AuthorizationDetailsJSONRepresentation> authzDetails = parseAuthorizationDetails(authorizationDetailsParam);

        Map<String, AuthorizationDetailsProcessor<?>> processors = getProcessors(session);

        for (AuthorizationDetailsJSONRepresentation authzDetail : authzDetails) {
            if (authzDetail.getType() == null) {
                throw new InvalidAuthorizationDetailsException("Authorization_Details parameter provided without type: " + authorizationDetailsParam);
            }
            AuthorizationDetailsProcessor<?> processor = processors.get(authzDetail.getType());
            if (processor == null) {
                String errorDetails = String.format("Unsupported type '%s' of authorization_details parameter supplied in the request. Supported values: %s",
                        authzDetail.getType(), processors.keySet());
                logger.warn(errorDetails);
                throw new InvalidAuthorizationDetailsException(errorDetails);
            }
            function.apply(processor, authzDetail);
            AuthorizationDetailsResponse response = function.apply(processor, authzDetail);
            if (response != null) {
                authzResponses.add(response);
            } else {
                logger.debugf("Null response returned by authorization processor " + processor + " for given authorization details");
            }
        }

        return authzResponses;
    }

    private List<AuthorizationDetailsJSONRepresentation> parseAuthorizationDetails(String authorizationDetailsParam) {
        try {
            return JsonSerialization.readValue(authorizationDetailsParam, new TypeReference<>() {
            });
        } catch (Exception e) {
            logger.warnf(e, "Invalid authorization_details format: %s", authorizationDetailsParam);
            throw new InvalidAuthorizationDetailsException("Invalid authorization_details: " + authorizationDetailsParam);
        }
    }

    private Map<String, AuthorizationDetailsProcessor<?>> getProcessors(KeycloakSession session) {
        return session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(AuthorizationDetailsProcessor.class)
                .collect(Collectors.toMap(ProviderFactory::getId, factory -> (AuthorizationDetailsProcessor<?>) session.getProvider(AuthorizationDetailsProcessor.class, factory.getId())));
    }
}
