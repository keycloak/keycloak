package org.keycloak.tests.oid4vc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialResponse.Credential;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.tests.oid4vc.abca.OIDCClientAttester;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.CredentialOfferUriResponse;
import org.keycloak.testsuite.util.oauth.oid4vc.Oid4vcCredentialResponse;


/**
 * A context that can maintain state across OID4VCI message flows.
 * <p>
 * It uses a typed in-memory value store based on attachment keys.
 * Values can be accessed by type and when there are multiple values - by name+type.
 *
 * @author <a href="mailto:tdiesler@proton.me">Thomas Diesler</a>
 */
public class OID4VCTestContext {

    public static final AttachmentKey<OIDCConfigurationRepresentation> AUTHORIZATION_SERVER_METADATA_ATTACHMENT_KEY = new AttachmentKey<>(OIDCConfigurationRepresentation.class);
    public static final AttachmentKey<CredentialIssuer> ISSUER_METADATA_ATTACHMENT_KEY = new AttachmentKey<>(CredentialIssuer.class);
    public static final AttachmentKey<OIDCClientAttester> CLIENT_ATTESTER_ATTACHMENT_KEY = new AttachmentKey<>(OIDCClientAttester.class);
    public static final AttachmentKey<CredentialOfferUriResponse> CREDENTIALS_OFFER_URI_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(CredentialOfferUriResponse.class);
    public static final AttachmentKey<CredentialOfferResponse> CREDENTIALS_OFFER_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(CredentialOfferResponse.class);
    public static final AttachmentKey<AccessTokenResponse> ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(AccessTokenResponse.class);
    public static final AttachmentKey<Oid4vcCredentialResponse> CREDENTIALS_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(Oid4vcCredentialResponse.class);
    public static final AttachmentKey<Credential[]> CREDENTIALS_ATTACHMENT_KEY = new AttachmentKey<>(Credential[].class);

    private String issuer;      // Issuing username (i.e. agent who creates credential offers)
    private String holder;      // Holder who requests the credential

    private ClientRepresentation client;
    private CredentialScopeRepresentation credentialScope;

    private final Map<AttachmentKey<?>, Object> attachments = new HashMap<>();

    public OID4VCTestContext(ClientRepresentation client, CredentialScopeRepresentation credentialScope) {
        this(client);
        this.withCredentialScope(credentialScope);
    }

    public OID4VCTestContext(ClientRepresentation client) {
        this.client = client;
        this.issuer = "john";
        this.holder = "alice";
    }

    public ClientRepresentation getClient() {
        return client;
    }

    public OID4VCTestContext withClient(ClientRepresentation client) {
        this.client = client;
        return this;
    }

    public String getIssuer() {
        return issuer;
    }

    public OID4VCTestContext withIssuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public String getHolder() {
        return holder;
    }

    public OID4VCTestContext withHolder(String holder) {
        this.holder = holder;
        return this;
    }

    public CredentialScopeRepresentation getCredentialScope() {
        return credentialScope;
    }

    /**
     * Set the current credential scope
     */
    public OID4VCTestContext withCredentialScope(CredentialScopeRepresentation credentialScope) {
        this.credentialScope = credentialScope;
        return this;
    }

    public String getCredentialConfigurationId() {
        return credentialScope.getCredentialConfigurationId();
    }

    public String getCredentialIdentifier() {
        return credentialScope.getCredentialIdentifier();
    }

    public List<String> getCredentialTypes() {
        return Optional.ofNullable(credentialScope.getSupportedCredentialTypes()).orElse(List.of());
    }

    public String getCredentialType() {
        List<String> credTypes = getCredentialTypes();
        return !credTypes.isEmpty() ? credTypes.get(0) : null;
    }

    public String getScope() {
        return credentialScope.getName();
    }

    public List<OID4VCAuthorizationDetail> getAuthorizationDetails() {
        AccessTokenResponse response = assertAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY);
        return Optional.ofNullable(response)
                .map(AccessTokenResponse::getOID4VCAuthorizationDetails)
                .orElse(Collections.emptyList());
    }

    public OID4VCAuthorizationDetail getAuthorizationDetail() {
        List<OID4VCAuthorizationDetail> tokenAuthDetails = getAuthorizationDetails();
        return tokenAuthDetails.size() == 1 ? tokenAuthDetails.get(0) : null;
    }

    public List<String> getAuthorizedCredentialIdentifiers() {
        OID4VCAuthorizationDetail tokenAuthDetails = getAuthorizationDetail();
        return Optional.ofNullable(tokenAuthDetails)
                .map(OID4VCAuthorizationDetail::getCredentialIdentifiers)
                .orElse(Collections.emptyList());
    }

    public String getAuthorizedCredentialIdentifier() {
        List<String> authorizedIdentifiers = getAuthorizedCredentialIdentifiers();
        return authorizedIdentifiers.size() == 1 ? authorizedIdentifiers.get(0) : null;
    }

    public String getAuthorizedCredentialConfigurationId() {
        OID4VCAuthorizationDetail tokenAuthDetails = getAuthorizationDetail();
        return Optional.ofNullable(tokenAuthDetails)
                .map(OID4VCAuthorizationDetail::getCredentialConfigurationId)
                .orElse(null);
    }

    public AccessTokenResponse getAccessTokenResponse() {
        var tokenResponse = getAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY);
        return tokenResponse;
    }

    public CredentialOfferUriResponse getCredentialsOfferUriResponse() {
        var credOfferUriResponse = getAttachment(CREDENTIALS_OFFER_URI_RESPONSE_ATTACHMENT_KEY);
        return credOfferUriResponse;
    }

    public CredentialOfferURI getCredentialsOfferUri() {
        return Optional.ofNullable(getCredentialsOfferUriResponse())
                .map(CredentialOfferUriResponse::getCredentialOfferURI)
                .orElse(null);
    }

    public CredentialOfferResponse getCredentialsOfferResponse() {
        var credOfferResponse = getAttachment(CREDENTIALS_OFFER_RESPONSE_ATTACHMENT_KEY);
        return credOfferResponse;
    }

    public CredentialsOffer getCredentialsOffer() {
        return Optional.ofNullable(getCredentialsOfferResponse())
                .map(CredentialOfferResponse::getCredentialsOffer)
                .orElse(null);
    }

    public Oid4vcCredentialResponse getOid4vcCredentialResponse() {
        var credResponse = getAttachment(CREDENTIALS_RESPONSE_ATTACHMENT_KEY);
        return credResponse;
    }

    public CredentialResponse getCredentialResponse() {
        return Optional.ofNullable(getOid4vcCredentialResponse())
                .map(Oid4vcCredentialResponse::getCredentialResponse)
                .orElse(null);
    }

    public List<Credential> getCredentials() {
        return Optional.ofNullable(getAttachment(CREDENTIALS_ATTACHMENT_KEY))
                .map(it -> new ArrayList<>(Arrays.asList(it)))
                .orElse(new ArrayList<>());
    }

    public void addCredentials(List<Credential> credentials) {
        List<Credential> storedCreds = getCredentials();
        storedCreds.addAll(credentials);
        putAttachment(CREDENTIALS_ATTACHMENT_KEY, storedCreds.toArray(new Credential[0]));
    }

    public List<Credential> findCredentials(Predicate<Credential> predicate) {
        return getCredentials().stream().filter(predicate).toList();

    }

    // Attachment Support ----------------------------------------------------------------------------------------------

    public <T> void putAttachment(AttachmentKey<T> key, T value) {
        if (value != null) {
            attachments.put(key, value);
        } else {
            attachments.remove(key);
        }
    }

    public <T> T assertAttachment(AttachmentKey<T> key) {
        return Optional.of(getAttachment(key)).get();
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment(AttachmentKey<T> key) {
        return (T) attachments.get(key);
    }

    public <T> T getAttachment(AttachmentKey<T> key, T defaultValue) {
        return Optional.ofNullable(getAttachment(key)).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttachment(AttachmentKey<T> key) {
        return (T) attachments.remove(key);
    }

    public static class AttachmentKey<T> {
        private final String name;
        private final Class<T> type;

        public AttachmentKey(Class<T> type) {
            this(null, type);
        }

        public AttachmentKey(String name, Class<T> type) {
            this.name = Optional.ofNullable(name).orElse("");
            this.type = Optional.of(type).get();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AttachmentKey<?> that = (AttachmentKey<?>) o;
            return Objects.equals(name, that.name) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }
}
