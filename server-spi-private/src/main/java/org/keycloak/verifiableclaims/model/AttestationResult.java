package org.keycloak.verifiableclaims.model;

public class AttestationResult {
    private final ClaimStatus status;
    private final String evidenceRef;
    private final String reason;

    public AttestationResult(ClaimStatus status, String evidenceRef, String reason) {
        this.status = status; this.evidenceRef = evidenceRef; this.reason = reason;
    }
    public ClaimStatus getStatus() { return status; }
    public String getEvidenceRef() { return evidenceRef; }
    public String getReason() { return reason; }
}
