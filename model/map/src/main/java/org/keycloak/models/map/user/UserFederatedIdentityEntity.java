package org.keycloak.models.map.user;

import org.keycloak.models.FederatedIdentityModel;

import java.util.Objects;

public class UserFederatedIdentityEntity {
    private String token;
    private String userId;
    private String identityProvider;
    private String userName;
    private boolean updated;
    
    private UserFederatedIdentityEntity() {}

    public static UserFederatedIdentityEntity fromModel(FederatedIdentityModel model) {
        if (model == null) return null;
        UserFederatedIdentityEntity entity = new UserFederatedIdentityEntity();
        entity.setIdentityProvider(model.getIdentityProvider());
        entity.setUserId(model.getUserId());
        entity.setUserName(model.getUserName().toLowerCase());
        entity.setToken(model.getToken());

        return entity;
    }

    public static FederatedIdentityModel toModel(UserFederatedIdentityEntity entity) {
        if (entity == null) return null;
        return new FederatedIdentityModel(entity.getIdentityProvider(), entity.getUserId(), entity.getUserName(), entity.getToken());
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.updated |= !Objects.equals(this.token, token);
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.updated |= !Objects.equals(this.userId, userId);
        this.userId = userId;
    }

    public String getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(String identityProvider) {
        this.updated |= !Objects.equals(this.identityProvider, identityProvider);
        this.identityProvider = identityProvider;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.updated |= !Objects.equals(this.userName, userName);
        this.userName = userName;
    }

    public boolean isUpdated() {
        return updated;
    }
}
