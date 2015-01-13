package org.keycloak.models.entities;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FederatedIdentityEntity {

    private String userId;
    private String userName;
    private String identityProvider;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.identityProvider = identityProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FederatedIdentityEntity that = (FederatedIdentityEntity) o;

        if (identityProvider != null && (that.identityProvider == null || !identityProvider.equals(that.identityProvider))) return false;
        if (userId != null && (that.userId == null || !userId.equals(that.userId))) return false;
        if (identityProvider == null && that.identityProvider != null)return false;
        if (userId == null && that.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int code = 1;
        if (userId != null) {
            code = code * userId.hashCode() * 13;
        }
        if (identityProvider != null) {
            code = code * identityProvider.hashCode() * 17;
        }
        return code;
    }
}
