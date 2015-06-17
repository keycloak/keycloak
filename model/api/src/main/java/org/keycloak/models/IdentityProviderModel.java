/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.representations.idm.IdentityProviderRepresentation;

/**
 * <p>A model type representing the configuration for identity providers. It provides some common properties and also a {@link org.keycloak.models.IdentityProviderModel#config}
 * for configuration options and properties specifics to a identity provider.</p>
 *
 * @author Pedro Igor
 */
public class IdentityProviderModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String internalId;

    /**
     * <p>An user-defined identifier to unique identify an identity provider instance.</p>
     */
    private String alias;

    /**
     * <p>An identifier used to reference a specific identity provider implementation. The value of this field is the same
     * across instances of the same provider implementation.</p>
     */
    private String providerId;

    private boolean enabled;

    /**
     * For possible values see {@link IdentityProviderRepresentation#getUpdateProfileFirstLoginMode()}
     * @see IdentityProviderRepresentation#UPFLM_ON
     * @see IdentityProviderRepresentation#UPFLM_MISSING
     * @see IdentityProviderRepresentation#UPFLM_OFF
     */
    protected String updateProfileFirstLoginMode = IdentityProviderRepresentation.UPFLM_ON;
    
    private boolean trustEmail;

    private boolean storeToken;

    protected boolean addReadTokenRoleOnCreate;
    /**
     * Specifies if particular provider should be used by default for authentication even before displaying login screen
     */
    private boolean authenticateByDefault;

    /**
     * <p>A map containing the configuration and properties for a specific identity provider instance and implementation. The items
     * in the map are understood by the identity provider implementation.</p>
     */
    private Map<String, String> config = new HashMap<String, String>();

    public IdentityProviderModel() {
    }

    public IdentityProviderModel(IdentityProviderModel model) {
        this.internalId = model.getInternalId();
        this.providerId = model.getProviderId();
        this.alias = model.getAlias();
        this.config = new HashMap<String, String>(model.getConfig());
        this.enabled = model.isEnabled();
        this.updateProfileFirstLoginMode = model.getUpdateProfileFirstLoginMode();
        this.trustEmail = model.isTrustEmail();
        this.storeToken = model.isStoreToken();
        this.authenticateByDefault = model.isAuthenticateByDefault();
        this.addReadTokenRoleOnCreate = model.addReadTokenRoleOnCreate;
    }

    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String id) {
        this.alias = id;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @see IdentityProviderRepresentation#getUpdateProfileFirstLoginMode() 
     */
    public String getUpdateProfileFirstLoginMode() {
        return updateProfileFirstLoginMode;
    }

    /**
     * @see IdentityProviderRepresentation#setUpdateProfileFirstLoginMode(String) 
     */
    public void setUpdateProfileFirstLoginMode(String updateProfileFirstLoginMode) {
        this.updateProfileFirstLoginMode = updateProfileFirstLoginMode;
    }

    public boolean isStoreToken() {
        return this.storeToken;
    }

    public void setStoreToken(boolean storeToken) {
        this.storeToken = storeToken;
    }

    public boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    public void setAuthenticateByDefault(boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public boolean isAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public boolean isTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(boolean trustEmail) {
        this.trustEmail = trustEmail;
    }
    
}
