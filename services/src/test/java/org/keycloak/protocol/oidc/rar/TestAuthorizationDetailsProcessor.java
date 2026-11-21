package org.keycloak.protocol.oidc.rar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AuthorizationDetailsJSONRepresentation;

/**
 * Minimal {@link AuthorizationDetailsProcessor} for unit tests. Records the processing processor in the response,
 * so that tests can assert which processor handled a given authorization detail. Narrowing produces a distinct
 * {@link NarrowedAuthorizationDetail} instance, sanitizing marks it and {@link #afterAuthorizationDetailsProcessed}
 * records it, so that tests can assert that a hook received the narrowed representation.
 */
class TestAuthorizationDetailsProcessor implements AuthorizationDetailsProcessor<TestAuthorizationDetailsProcessor.NarrowedAuthorizationDetail> {

    static final String PROCESSED_BY = "processed_by";
    static final String NARROWED_BY = "narrowed_by";
    static final String SANITIZED_BY = "sanitized_by";

    /**
     * Processor specific subtype, which can only be obtained via {@link #narrowRepresentation(AuthorizationDetailsJSONRepresentation)}.
     */
    static class NarrowedAuthorizationDetail extends AuthorizationDetailsJSONRepresentation {
    }

    private final String name;
    private final boolean supported;
    private final Set<String> supportedTypes;
    private final List<NarrowedAuthorizationDetail> afterProcessed = new ArrayList<>();

    TestAuthorizationDetailsProcessor(String name, boolean supported, Set<String> supportedTypes) {
        this.name = name;
        this.supported = supported;
        this.supportedTypes = supportedTypes;
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public Set<String> getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public Class<NarrowedAuthorizationDetail> getSupportedResponseJavaType() {
        return NarrowedAuthorizationDetail.class;
    }

    @Override
    public NarrowedAuthorizationDetail narrowRepresentation(AuthorizationDetailsJSONRepresentation authzDetail) {
        NarrowedAuthorizationDetail narrowed = new NarrowedAuthorizationDetail();
        narrowed.setType(authzDetail.getType());
        authzDetail.getCustomData().forEach(narrowed::setCustomData);
        narrowed.setCustomData(NARROWED_BY, name);
        return narrowed;
    }

    @Override
    public NarrowedAuthorizationDetail validateAuthorizationDetail(AuthorizationDetailsJSONRepresentation authzDetail) {
        return narrowRepresentation(authzDetail);
    }

    @Override
    public NarrowedAuthorizationDetail process(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation authzDetail) {
        return response(authzDetail.getType());
    }

    @Override
    public List<NarrowedAuthorizationDetail> handleMissingAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // Emit one response per supported type, as if the processor had derived it from the request context
        return supportedTypes.stream().sorted().map(this::response).toList();
    }

    @Override
    public NarrowedAuthorizationDetail processStoredAuthorizationDetails(UserSessionModel userSession, ClientSessionContext clientSessionCtx, AuthorizationDetailsJSONRepresentation storedAuthDetailsMember) {
        return process(userSession, clientSessionCtx, storedAuthDetailsMember);
    }

    @Override
    public void afterAuthorizationDetailsProcessed(UserSessionModel userSession, ClientSessionContext clientSessionCtx, NarrowedAuthorizationDetail authorizationDetailsResponse) {
        afterProcessed.add(authorizationDetailsResponse);
    }

    /**
     * @return the (narrowed) authorization details passed to {@link #afterAuthorizationDetailsProcessed} so far
     */
    List<NarrowedAuthorizationDetail> getAfterProcessed() {
        return afterProcessed;
    }

    @Override
    public NarrowedAuthorizationDetail sanitizeBeforeSendingTokenResponse(NarrowedAuthorizationDetail authzDetail) {
        authzDetail.setCustomData(SANITIZED_BY, name);
        return authzDetail;
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return name;
    }

    private NarrowedAuthorizationDetail response(String type) {
        NarrowedAuthorizationDetail response = new NarrowedAuthorizationDetail();
        response.setType(type);
        response.setCustomData(PROCESSED_BY, name);
        return response;
    }
}
