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

package org.keycloak.models.jpa.entities;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Entity
@Table(name="CLIENT_TEMPLATE", uniqueConstraints = {@UniqueConstraint(columnNames = {"REALM_ID", "NAME"})})
public class ClientTemplateEntity {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity.  This avoids an extra SQL
    private String id;
    @Column(name = "NAME")
    private String name;
    @Column(name = "DESCRIPTION")
    private String description;
    @OneToMany(cascade ={CascadeType.REMOVE}, orphanRemoval = true, mappedBy = "clientTemplate")
    Collection<ProtocolMapperEntity> protocolMappers = new ArrayList<ProtocolMapperEntity>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

    @Column(name="PROTOCOL")
    private String protocol;

    @Column(name="FULL_SCOPE_ALLOWED")
    private boolean fullScopeAllowed;

    @Column(name="CONSENT_REQUIRED")
    private boolean consentRequired;

    @Column(name="STANDARD_FLOW_ENABLED")
    private boolean standardFlowEnabled;

    @Column(name="IMPLICIT_FLOW_ENABLED")
    private boolean implicitFlowEnabled;

    @Column(name="DIRECT_ACCESS_GRANTS_ENABLED")
    private boolean directAccessGrantsEnabled;

    @Column(name="SERVICE_ACCOUNTS_ENABLED")
    private boolean serviceAccountsEnabled;

    @Column(name="FRONTCHANNEL_LOGOUT")
    private boolean frontchannelLogout;
    @Column(name="PUBLIC_CLIENT")
    private boolean publicClient;
    @Column(name="BEARER_ONLY")
    private boolean bearerOnly;


    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", length = 2048)
    @CollectionTable(name="CLIENT_TEMPLATE_ATTRIBUTES", joinColumns={ @JoinColumn(name="TEMPLATE_ID") })
    protected Map<String, String> attributes = new HashMap<String, String>();

    public RealmEntity getRealm() {
        return realm;
    }

    public void setRealm(RealmEntity realm) {
        this.realm = realm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<ProtocolMapperEntity> getProtocolMappers() {
        return protocolMappers;
    }

    public void setProtocolMappers(Collection<ProtocolMapperEntity> protocolMappers) {
        this.protocolMappers = protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.frontchannelLogout = frontchannelLogout;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.publicClient = publicClient;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.bearerOnly = bearerOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof ClientTemplateEntity)) return false;

        ClientTemplateEntity that = (ClientTemplateEntity) o;

        if (!id.equals(that.getId())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
