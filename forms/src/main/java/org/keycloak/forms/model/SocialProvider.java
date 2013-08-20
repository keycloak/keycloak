package org.keycloak.forms.model;

public class SocialProvider {

    private String id;
    private String name;
    private String loginUrl;

    public SocialProvider(String id, String name, String loginUrl) {
        this.id = id;
        this.name = name;
        this.loginUrl = loginUrl;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

}