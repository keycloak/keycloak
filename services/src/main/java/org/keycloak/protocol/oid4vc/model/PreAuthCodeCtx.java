package org.keycloak.protocol.oid4vc.model;

import java.util.List;
import java.util.Objects;

import org.keycloak.protocol.oid4vc.issuance.credentialoffer.CredentialOfferState;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Non-sensitive fields a pre-authorized code representation may embed.
 * <p></p>
 * Mainly intended to be used as a partial, public view of {@link CredentialOfferState}.
 */
public class PreAuthCodeCtx implements Cloneable {

    @JsonProperty("credentials_offer_id")
    private String credentialsOfferId;

    @JsonProperty("target_client_id")
    private String targetClientId;

    @JsonProperty("target_user_id")
    private String targetUserId;

    @JsonProperty("nonce")
    private String nonce;

    @JsonProperty("exp")
    private Long expiresAt;

    @JsonProperty("authorization_details")
    private List<OID4VCAuthorizationDetail> authorizationDetails;

    public PreAuthCodeCtx() {
    }

    /**
     * This construction makes it explicit what data can be made public.
     * For example, transactions codes must never leak into pre-auth codes.
     */
    public PreAuthCodeCtx(CredentialOfferState offerState) {
        Objects.requireNonNull(offerState);

        this.credentialsOfferId = offerState.getCredentialsOfferId();
        this.targetClientId = offerState.getTargetClientId();
        this.targetUserId = offerState.getTargetUserId();
        this.nonce = offerState.getNonce();
        this.expiresAt = offerState.getExpiresAt();

        List<OID4VCAuthorizationDetail> details = offerState.getAuthorizationDetails();
        this.authorizationDetails = details == null ? null : details.stream()
                .map(OID4VCAuthorizationDetail::clone)
                .peek(d -> d.setCredentialsOfferId(null))
                .toList();
    }

    @JsonIgnore
    public List<String> getCredentialConfigurationIds() {
        if (authorizationDetails == null) {
            return List.of();
        }

        return authorizationDetails.stream()
                .map(OID4VCAuthorizationDetail::getCredentialConfigurationId)
                .toList();
    }

    public String getCredentialsOfferId() {
        return credentialsOfferId;
    }

    public void setCredentialsOfferId(String credentialsOfferId) {
        this.credentialsOfferId = credentialsOfferId;
    }

    public List<OID4VCAuthorizationDetail> getAuthorizationDetails() {
        return authorizationDetails;
    }

    public void setAuthorizationDetails(List<OID4VCAuthorizationDetail> authorizationDetails) {
        this.authorizationDetails = authorizationDetails;
    }

    public String getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(String targetClientId) {
        this.targetClientId = targetClientId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PreAuthCodeCtx that = (PreAuthCodeCtx) o;
        return Objects.equals(getCredentialsOfferId(), that.getCredentialsOfferId()) && Objects.equals(getCredentialConfigurationIds(), that.getCredentialConfigurationIds()) && Objects.equals(getAuthorizationDetails(), that.getAuthorizationDetails()) && Objects.equals(getTargetClientId(), that.getTargetClientId()) && Objects.equals(getTargetUserId(), that.getTargetUserId()) && Objects.equals(getNonce(), that.getNonce()) && Objects.equals(getExpiresAt(), that.getExpiresAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCredentialsOfferId(), getCredentialConfigurationIds(), getAuthorizationDetails(), getTargetClientId(), getTargetUserId(), getNonce(), getExpiresAt());
    }

    @Override
    public PreAuthCodeCtx clone() {
        try {
            return (PreAuthCodeCtx) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
