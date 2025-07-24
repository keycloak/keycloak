package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.TokenCategory;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.TokenUtil;

import java.util.List;
import java.util.Map;

public class EntityStatement extends JsonWebToken {

    protected JSONWebKeySet jwks;

    @JsonProperty("authority_hints")
    protected List<String> authorityHints;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected Metadata metadata;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("metadata_policy")
    protected MetadataPolicy metadataPolicy;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected Constraints constraints;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected List<String> crit;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("metadata_policy_crit")
    protected List<String> metadataPolicyCrit;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("trust_marks")
    protected List<TrustMark> trustMarks;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("trust_mark_issuers")
    protected List<TrustMark> trustMarksIssuers;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("trust_mark_owners")
    protected TrustMarkOwners trustMarkOwners;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("source_endpoint")
    protected String source_endpoint;

    public EntityStatement (){}

    public EntityStatement (String issuer, Long expiration, List<String> authorityHints, JSONWebKeySet jwks, Metadata metadata){
        this.issuer = issuer;
        this.subject = issuer;
        this.issuedNow();
        this.exp(this.iat + expiration);
        this.authorityHints = authorityHints;
        this.setJwks(jwks);
        this.type(TokenUtil.ENTITY_STATEMENT_JWT);
        this.metadata = metadata;
    }

    public JSONWebKeySet getJwks() {
        return jwks;
    }

    public void setJwks(JSONWebKeySet jwks) {
        this.jwks = jwks;
    }

    public List<String> getAuthorityHints() {
        return authorityHints;
    }

    public void setAuthorityHints(List<String> authorityHints) {
        this.authorityHints = authorityHints;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public MetadataPolicy getMetadataPolicy() {
        return metadataPolicy;
    }

    public void setMetadataPolicy(MetadataPolicy metadataPolicy) {
        this.metadataPolicy = metadataPolicy;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public void setConstraints(Constraints constraints) {
        this.constraints = constraints;
    }

    public List<String> getCrit() {
        return crit;
    }

    public void setCrit(List<String> crit) {
        this.crit = crit;
    }

    public List<String> getMetadataPolicyCrit() {
        return metadataPolicyCrit;
    }

    public void setMetadataPolicyCrit(List<String> metadataPolicyCrit) {
        this.metadataPolicyCrit = metadataPolicyCrit;
    }

    public List<TrustMark> getTrustMarks() {
        return trustMarks;
    }

    public void setTrustMarks(List<TrustMark> trustMarks) {
        this.trustMarks = trustMarks;
    }

    public List<TrustMark> getTrustMarksIssuers() {
        return trustMarksIssuers;
    }

    public void setTrustMarksIssuers(List<TrustMark> trustMarksIssuers) {
        this.trustMarksIssuers = trustMarksIssuers;
    }

    public TrustMarkOwners getTrustMarkOwners() {
        return trustMarkOwners;
    }

    public void setTrustMarkOwners(TrustMarkOwners trustMarkOwners) {
        this.trustMarkOwners = trustMarkOwners;
    }

    public String getSource_endpoint() {
        return source_endpoint;
    }

    public void setSource_endpoint(String source_endpoint) {
        this.source_endpoint = source_endpoint;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.ENTITY_STATEMENT; //treat it as an access token (use asymmetric crypto algorithms)
    }

    @Override
    public EntityStatement type(String type) {
        return (EntityStatement) super.type(type);
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

    class TrustMarkOwners {
        private String sub;
        private JSONWebKeySet jwks;

        public TrustMarkOwners() {}

        public String getSub() {
            return sub;
        }

        public void setSub(String sub) {
            this.sub = sub;
        }

        public JSONWebKeySet getJwks() {
            return jwks;
        }

        public void setJwks(JSONWebKeySet jwks) {
            this.jwks = jwks;
        }
    }

}



