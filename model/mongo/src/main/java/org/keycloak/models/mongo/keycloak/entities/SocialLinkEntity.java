package org.keycloak.models.mongo.keycloak.entities;

import org.keycloak.models.mongo.api.MongoEntity;
import org.keycloak.models.mongo.api.MongoField;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SocialLinkEntity implements MongoEntity {

    private String socialUsername;
    private String socialProvider;

    @MongoField
    public String getSocialUsername() {
        return socialUsername;
    }

    public void setSocialUsername(String socialUsername) {
        this.socialUsername = socialUsername;
    }

    @MongoField
    public String getSocialProvider() {
        return socialProvider;
    }

    public void setSocialProvider(String socialProvider) {
        this.socialProvider = socialProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SocialLinkEntity that = (SocialLinkEntity) o;

        if (socialProvider != null && (that.socialProvider == null || !socialProvider.equals(that.socialProvider))) return false;
        if (socialUsername != null && (that.socialUsername == null || !socialUsername.equals(that.socialUsername))) return false;
        if (socialProvider == null && that.socialProvider != null)return false;
        if (socialUsername == null && that.socialUsername != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 1;
        if (socialUsername != null) {
            code = code * 13;
        }
        if (socialProvider != null) {
            code = code * 17;
        }
        return code;
    }
}
