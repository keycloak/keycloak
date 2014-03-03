package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.AbstractMongoIdentifiableEntity;
import org.keycloak.models.mongo.api.MongoCollection;
import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;
import org.keycloak.models.mongo.api.context.MongoStoreInvocationContext;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@MongoCollection(collectionName = "oauthClients")
public class OAuthClientEntity extends AbstractMongoIdentifiableEntity implements MongoEntity, ScopedEntity {

    private String name;
    private boolean enabled;
    private String realmId;
    private String secret;
    private long allowedClaimsMask;
    private List<String> scopeIds;
    private List<String> webOrigins;
    private List<String> redirectUris;

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
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
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




    @Override
    public void afterRemove(MongoStoreInvocationContext context) {
    }
}
