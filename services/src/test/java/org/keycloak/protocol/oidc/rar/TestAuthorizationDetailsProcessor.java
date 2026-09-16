package org.keycloak.protocol.oidc.rar;

import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

/**
 * Minimal {@link AuthorizationDetailsProcessor} for unit tests. Records the processing processor in the response,
 * so that tests can assert which processor handled a given authorization detail.
 */
class TestAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor<AuthorizationDetailsJSONRepresentation> {

    static final String PROCESSED_BY = "processed_by";

    private final String name;
    private final Set<String> supportedTypes;

    TestAuthorizationDetailsProcessor(String name, Set<String> supportedTypes) {
        this.name = name;
        this.supportedTypes = supportedTypes;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public Set<String> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public Class<AuthorizationDetailsJSONRepresentation> getSupportedResponseJavaType() {
        return AuthorizationDetailsJSONRepresentation.class;
    }

    @Override
    public AuthorizationDetailsJSONRepresentation narrowRepresentation(AuthorizationDetailsJSONRepresentation authzDetail) {
        return authzDetail;
    }

    @Override
    public AuthorizationDetailsJSONRepresentation validateAuthorizationDetail(AuthorizationDetailsJSONRepresentation authzDetail) {
        return authzDetail;
    }

    @Override
    public AuthorizationDetailsJSONRepresentation process(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authzDetail) {
        AuthorizationDetailsJSONRepresentation response = new AuthorizationDetailsJSONRepresentation();
        response.setType(authzDetail.getType());
        response.setCustomData(PROCESSED_BY, name);
        return response;
    }

    @Override
    public List<AuthorizationDetailsJSONRepresentation> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        return null;
    }

    @Override
    public AuthorizationDetailsJSONRepresentation processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation storedAuthDetailsMember) {
        return process(userSession, clientSessionCtx, storedAuthDetailsMember);
    }

    @Override
    public void afterAuthorizationDetailsProcessed(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authorizationDetailsResponse) {
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return name;
    }
}
