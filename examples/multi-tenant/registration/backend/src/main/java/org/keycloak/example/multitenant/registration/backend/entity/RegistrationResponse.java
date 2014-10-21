/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.example.multitenant.registration.backend.entity;

/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class RegistrationResponse {
    private String nodeUsername;
    private String nodePassword;
    private String oauthSecret;
    private String errorMessage;
    private String keycloakJson;

    public RegistrationResponse(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public RegistrationResponse(String nodeUsername, String nodePassword, String oauthSecret, String keycloakJson) {
        this.nodeUsername = nodeUsername;
        this.nodePassword = nodePassword;
        this.oauthSecret = oauthSecret;
        this.keycloakJson = keycloakJson;
    }

    public RegistrationResponse() {
    }

    public String getNodeUsername() {
        return nodeUsername;
    }

    public void setNodeUsername(String nodeUsername) {
        this.nodeUsername = nodeUsername;
    }

    public String getNodePassword() {
        return nodePassword;
    }

    public void setNodePassword(String nodePassword) {
        this.nodePassword = nodePassword;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }

    public String getKeycloakJson() {
        return keycloakJson;
    }

    public void setKeycloakJson(String keycloakJson) {
        this.keycloakJson = keycloakJson;
    }

}
