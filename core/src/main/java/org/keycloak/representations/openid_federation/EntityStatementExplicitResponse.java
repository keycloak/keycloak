package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityStatementExplicitResponse extends EntityStatement{

    public EntityStatementExplicitResponse(){}

    @JsonProperty("trust_anchor")
    protected String trustAnchor;

    public EntityStatementExplicitResponse (EntityStatement statement, String issuer, RPMetadata rpMetadata, String trustAnchor, String authorityHint){
        this.issuer = issuer;
        this.subject = statement.getSubject();
        this.iat = Long.valueOf(rpMetadata.getClientIdIssuedAt());
        this.exp = statement.getExp();
        this.jwks = statement.getJwks();
        this.audience = new String [] {statement.getSubject()};
        this.trustAnchor = trustAnchor;
        this.authorityHints = Stream.of(authorityHint).collect(Collectors.toList());
        Metadata md = new Metadata();
        md.setRelyingPartyMetadata(rpMetadata);
        this.metadata = md;
    }

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }
}
