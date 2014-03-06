package org.keycloak.models.mongo.keycloak.entities;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientEntity extends AbstractMongoIdentifiableEntity implements MongoEntity {

    private String name;
    private boolean enabled;
    private String secret;
    private long allowedClaimsMask;
    private int notBefore;
    private boolean publicClient;

    private String realmId;

    private List<String> webOrigins = new ArrayList<String>();
    private List<String> redirectUris = new ArrayList<String>();
    private List<String> scopeIds = new ArrayList<String>();

    @MongoField
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @MongoField
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @MongoField
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @MongoField
    public long getAllowedClaimsMask() {
        return allowedClaimsMask;
    }

    public void setAllowedClaimsMask(long allowedClaimsMask) {
        this.allowedClaimsMask = allowedClaimsMask;
    }

    @MongoField
    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.notBefore = notBefore;
    }

    @MongoField
    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    @MongoField
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    @MongoField
    public List<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(List<String> webOrigins) {
        this.webOrigins = webOrigins;
    }

    @MongoField
    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    @MongoField
    public List<String> getScopeIds() {
        return scopeIds;
    }

    public void setScopeIds(List<String> scopeIds) {
        this.scopeIds = scopeIds;
    }
}
