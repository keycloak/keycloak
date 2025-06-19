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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserRepresentation extends AbstractUserRepresentation{

    protected String self; // link
    protected String origin;
    protected Long createdTimestamp;
    protected Boolean totp;
    protected String federationLink;
    protected String serviceAccountClientId; // For rep, it points to clientId (not DB ID)

    protected List<CredentialRepresentation> credentials;
    protected Set<String> disableableCredentialTypes;
    protected List<String> requiredActions;
    protected List<FederatedIdentityRepresentation> federatedIdentities;
    protected List<String> realmRoles;
    protected Map<String, List<String>> clientRoles;
    protected List<UserConsentRepresentation> clientConsents;
    protected Integer notBefore;

    @Deprecated
    protected Map<String, List<String>> applicationRoles;
    @Deprecated
    protected List<SocialLinkRepresentation> socialLinks;

    protected List<String> groups;
    private Map<String, Boolean> access;

    public UserRepresentation() {
    }

    public UserRepresentation(UserRepresentation rep) {
        // AbstractUserRepresentation
        this.id = rep.getId();
        this.username = rep.getUsername();
        this.firstName = rep.getFirstName();
        this.lastName = rep.getLastName();
        this.email = rep.getEmail();
        this.emailVerified = rep.isEmailVerified();
        this.attributes = rep.getAttributes();
        this.setUserProfileMetadata(rep.getUserProfileMetadata());

        this.self = rep.getSelf();
        this.createdTimestamp = rep.getCreatedTimestamp();
        this.enabled = rep.isEnabled();
        this.totp = rep.isTotp();
        this.federationLink = rep.getFederationLink();
        this.serviceAccountClientId = rep.getServiceAccountClientId();
        this.credentials = rep.getCredentials();
        this.disableableCredentialTypes = rep.getDisableableCredentialTypes();
        this.requiredActions = rep.getRequiredActions();
        this.federatedIdentities = rep.getFederatedIdentities();
        this.realmRoles = rep.getRealmRoles();
        this.clientRoles = rep.getClientRoles();
        this.clientConsents = rep.getClientConsents();
        this.notBefore = rep.getNotBefore();

        this.applicationRoles = rep.getApplicationRoles();
        this.socialLinks = rep.getSocialLinks();

        this.groups = rep.getGroups();
        this.access = rep.getAccess();
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public Long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Deprecated
    public Boolean isTotp() {
        return totp;
    }

    @Deprecated
    public void setTotp(Boolean totp) {
        this.totp = totp;
    }

    public List<CredentialRepresentation> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<CredentialRepresentation> credentials) {
        this.credentials = credentials;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions;
    }

    public List<FederatedIdentityRepresentation> getFederatedIdentities() {
        return federatedIdentities;
    }

    public void setFederatedIdentities(List<FederatedIdentityRepresentation> federatedIdentities) {
        this.federatedIdentities = federatedIdentities;
    }

    public List<SocialLinkRepresentation> getSocialLinks() {
        return socialLinks;
    }

    public void setSocialLinks(List<SocialLinkRepresentation> socialLinks) {
        this.socialLinks = socialLinks;
    }

    public List<String> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<String> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, List<String>> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, List<String>> clientRoles) {
        this.clientRoles = clientRoles;
    }

    public List<UserConsentRepresentation> getClientConsents() {
        return clientConsents;
    }

    public void setClientConsents(List<UserConsentRepresentation> clientConsents) {
        this.clientConsents = clientConsents;
    }

    public Integer getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Integer notBefore) {
        this.notBefore = notBefore;
    }

    @Deprecated
    public Map<String, List<String>> getApplicationRoles() {
        return applicationRoles;
    }

    public String getFederationLink() {
        return federationLink;
    }

    public void setFederationLink(String federationLink) {
        this.federationLink = federationLink;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    /**
     * Returns id of UserStorageProvider that loaded this user
     *
     * @return NULL if user stored locally
     * @deprecated Use {@link #getFederationLink()} instead
     */
    @Deprecated
    public String getOrigin() {
        return federationLink;
    }

    /**
     *
     * @param origin the origin
     * @deprecated Use {@link #setFederationLink(String)} instead
     */
    @Deprecated
    public void setOrigin(String origin) {
        // deprecated
    }

    public Set<String> getDisableableCredentialTypes() {
        return disableableCredentialTypes;
    }

    public void setDisableableCredentialTypes(Set<String> disableableCredentialTypes) {
        this.disableableCredentialTypes = disableableCredentialTypes;
    }

    public Map<String, Boolean> getAccess() {
        return access;
    }

    public void setAccess(Map<String, Boolean> access) {
        this.access = access;
    }
}
