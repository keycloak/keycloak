package org.keycloak.verifiableclaims.model;

import java.util.Date;

public class ClaimProjection {
    private final ClaimStatus status;
    private final String evidenceRef;
    private final String issuer;
    private final Date verifiedAt;

    public ClaimProjection(ClaimStatus status, String evidenceRef, String issuer, Date verifiedAt) {
        this.status = status;
        this.evidenceRef = evidenceRef;
        this.issuer = issuer;
        this.verifiedAt = verifiedAt;
    }
    public ClaimStatus getStatus() { return status; }
    public String getEvidenceRef() { return evidenceRef; }
    public String getIssuer() { return issuer; }
    public Date getVerifiedAt() { return verifiedAt; }
}
