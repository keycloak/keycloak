package org.keycloak.tests.oid4vc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialScopeRepresentation;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;
import org.keycloak.protocol.oid4vc.model.OID4VCAuthorizationDetail;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;


/**
 * A context that can maintain state across OID4VCI message flows.
 *
 * It uses a typed in-memory value store based on attachment keys.
 * Values can be accessed by type and when there are multiple values - by name+type.
 *
 * @author <a href="mailto:tdiesler@ibm.com">Thomas Diesler</a>
 */
public class OID4VCTestContext {

    static final AttachmentKey<CredentialIssuer> ISSUER_METADATA_ATTACHMENT_KEY = new AttachmentKey<>(CredentialIssuer.class);
    static final AttachmentKey<CredentialOfferURI> CREDENTIAL_OFFER_URI_ATTACHMENT_KEY = new AttachmentKey<>(CredentialOfferURI.class);
    static final AttachmentKey<CredentialsOffer> CREDENTIALS_OFFER_ATTACHMENT_KEY = new AttachmentKey<>(CredentialsOffer.class);
    static final AttachmentKey<AccessTokenResponse> ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(AccessTokenResponse.class);
    static final AttachmentKey<CredentialResponse> CREDENTIAL_RESPONSE_ATTACHMENT_KEY = new AttachmentKey<>(CredentialResponse.class);

    ClientRepresentation client;
    String clientId;
    String issuer;      // Issuing username (i.e. agent who creates credential offers)
    String holder;      // Holder who requests the credential
    String credConfigId;
    String credScopeName;
    CredentialScopeRepresentation credentialScope;

    Map<AttachmentKey<?>, Object> attachments = new HashMap<>();

    public OID4VCTestContext(ClientRepresentation client, CredentialScopeRepresentation credentialScope) {
        this.client = client;
        this.clientId = client.getClientId();
        this.issuer = "john";
        this.holder = "alice";
        this.credentialScope = credentialScope;
        this.credScopeName = credentialScope.getName();
        this.credConfigId = credentialScope.getCredentialConfigurationId();
    }

    public List<String> getAuthorizedCredentialIdentifiers() {
        OID4VCAuthorizationDetail tokenAuthDetails = getOID4VCAuthorizationDetail();
        return Optional.ofNullable(tokenAuthDetails)
                .map(OID4VCAuthorizationDetail::getCredentialIdentifiers)
                .orElse(Collections.emptyList());
    }

    public String getAuthorizedCredentialIdentifier() {
        List<String> authorizedIdentifiers = getAuthorizedCredentialIdentifiers();
        return authorizedIdentifiers.size() == 1 ? authorizedIdentifiers.get(0) : null;
    }

    public String getAuthorizedCredentialConfigurationId() {
        OID4VCAuthorizationDetail tokenAuthDetails = getOID4VCAuthorizationDetail();
        return Optional.ofNullable(tokenAuthDetails)
                .map(OID4VCAuthorizationDetail::getCredentialConfigurationId)
                .orElse(null);
    }

    public List<OID4VCAuthorizationDetail> getOID4VCAuthorizationDetails() {
        AccessTokenResponse response = assertAttachment(ACCESS_TOKEN_RESPONSE_ATTACHMENT_KEY);
        return Optional.ofNullable(response)
                .map(AccessTokenResponse::getOID4VCAuthorizationDetails)
                .orElse(Collections.emptyList());
    }

    public OID4VCAuthorizationDetail getOID4VCAuthorizationDetail() {
        List<OID4VCAuthorizationDetail> tokenAuthDetails = getOID4VCAuthorizationDetails();
        return tokenAuthDetails.size() == 1 ? tokenAuthDetails.get(0) : null;
    }

    // Attachment Support ----------------------------------------------------------------------------------------------

    <T> void putAttachment(AttachmentKey<T> key, T value) {
        if (value != null) {
            attachments.put(key, value);
        } else {
            attachments.remove(key, value);
        }
    }

    <T> T assertAttachment(AttachmentKey<T> key) {
        return Optional.of(getAttachment(key)).get();
    }

    @SuppressWarnings("unchecked")
    <T> T getAttachment(AttachmentKey<T> key) {
        return (T) attachments.get(key);
    }

    @SuppressWarnings("unchecked")
    <T> T removeAttachment(AttachmentKey<T> key) {
        return (T) attachments.remove(key);
    }

    static class AttachmentKey<T> {
        private final String name;
        private final Class<T> type;

        AttachmentKey(Class<T> type) {
            this(null, type);
        }

        AttachmentKey(String name, Class<T> type) {
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
