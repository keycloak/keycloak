package org.keycloak.operator.crds.v2alpha1.deployment.spec;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class BootstrapAdminSpec {

    public static class User {
        @JsonPropertyDescription("Name of the secret with the username and password")
    	private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class Service {
        @JsonPropertyDescription("Name of the Secret with the client-id and client-secret")
    	private String secret;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

    }

    //private Integer expiration;
    @JsonPropertyDescription("Configures the bootstrap admin user")
    private User user;
    @JsonPropertyDescription("Configures the bootstrap admin service account")
    private Service service;

    /*public Integer getExpiration() {
        return expiration;
    }

    public void setExpiration(Integer expiration) {
        this.expiration = expiration;
    }*/

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

}