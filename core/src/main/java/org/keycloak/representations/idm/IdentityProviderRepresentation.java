/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.representations.idm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pedro Igor
 */
public class IdentityProviderRepresentation {

    protected String alias;
    protected String displayName;
    protected String internalId;
    protected String providerId;
    protected boolean enabled = true;

    public static final String UPFLM_ON = "on";
    public static final String UPFLM_MISSING = "missing";
    public static final String UPFLM_OFF = "off";

    /**
     * Mode of profile update after first login when user is created over this identity provider. Possible values:
     * <ul>
     * <li><code>on</code> - update profile page is presented for all users
     * <li><code>missing</code> - update profile page is presented for users with missing some of mandatory user profile fields
     * <li><code>off</code> - update profile page is newer shown after first login
     * </ul>
     *
     * @see #UPFLM_ON
     * @see #UPFLM_MISSING
     * @see #UPFLM_OFF
     */
    @Deprecated
    protected String updateProfileFirstLoginMode;

    protected Boolean trustEmail;
    protected Boolean storeToken;
    protected Boolean addReadTokenRoleOnCreate;
    protected Boolean authenticateByDefault;
    protected Boolean linkOnly;
    protected Boolean hideOnLogin;
    protected String firstBrokerLoginFlowAlias;
    protected String postBrokerLoginFlowAlias;
    protected String organizationId;
    protected Map<String, String> config = new HashMap<>();
    protected List<String> types = new ArrayList<>();

    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, String> getConfig() {
        return this.config;
    }

    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isLinkOnly() {
        return linkOnly;
    }

    public void setLinkOnly(Boolean linkOnly) {
        this.linkOnly = linkOnly;
    }

    public Boolean isHideOnLogin() {
        return this.hideOnLogin;
    }

    public void setHideOnLogin(Boolean hideOnLogin) {
        this.hideOnLogin = hideOnLogin;
    }

    /**
     *
     * Deprecated because replaced by {@link #updateProfileFirstLoginMode}. Kept here to allow import of old realms.
     *
     * @deprecated {@link #setUpdateProfileFirstLoginMode(String)}
     */
    @Deprecated
    public void setUpdateProfileFirstLogin(Boolean updateProfileFirstLogin) {
        this.updateProfileFirstLoginMode = updateProfileFirstLogin == null ? null : (updateProfileFirstLogin ? UPFLM_ON : UPFLM_OFF);
    }

    /**
     * @deprecated deprecated and replaced by configuration on IdpReviewProfileAuthenticator
     */
    @Deprecated
    public String getUpdateProfileFirstLoginMode() {
        return updateProfileFirstLoginMode;
    }

    /**
     * @deprecated deprecated and replaced by configuration on IdpReviewProfileAuthenticator
     */
    @Deprecated
    public void setUpdateProfileFirstLoginMode(String updateProfileFirstLoginMode) {
        this.updateProfileFirstLoginMode = updateProfileFirstLoginMode;
    }

    /**
     * @deprecated Replaced by configuration option in identity provider authenticator
     */
    @Deprecated
    public Boolean isAuthenticateByDefault() {
        return authenticateByDefault;
    }

    @Deprecated
    public void setAuthenticateByDefault(Boolean authenticateByDefault) {
        this.authenticateByDefault = authenticateByDefault;
    }

    public String getFirstBrokerLoginFlowAlias() {
        return firstBrokerLoginFlowAlias;
    }

    public void setFirstBrokerLoginFlowAlias(String firstBrokerLoginFlowAlias) {
        this.firstBrokerLoginFlowAlias = firstBrokerLoginFlowAlias;
    }

    public String getPostBrokerLoginFlowAlias() {
        return postBrokerLoginFlowAlias;
    }

    public void setPostBrokerLoginFlowAlias(String postBrokerLoginFlowAlias) {
        this.postBrokerLoginFlowAlias = postBrokerLoginFlowAlias;
    }

    public Boolean isStoreToken() {
        return this.storeToken;
    }

    public void setStoreToken(Boolean storeToken) {
        this.storeToken = storeToken;
    }

    public Boolean isAddReadTokenRoleOnCreate() {
        return addReadTokenRoleOnCreate;
    }

    public void setAddReadTokenRoleOnCreate(Boolean addReadTokenRoleOnCreate) {
        this.addReadTokenRoleOnCreate = addReadTokenRoleOnCreate;
    }

    public Boolean isTrustEmail() {
        return trustEmail;
    }

    public void setTrustEmail(Boolean trustEmail) {
        this.trustEmail = trustEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOrganizationId() {
        return this.organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public List<String> getTypes() {
        return this.types;
    }
    public void setTypes(List<String> types) {
        this.types = types;
    }

}
