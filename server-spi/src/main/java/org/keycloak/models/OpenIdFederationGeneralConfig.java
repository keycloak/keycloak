package org.keycloak.models;

import org.keycloak.models.enums.ClientRegistrationTypeEnum;
import org.keycloak.models.enums.EntityTypeEnum;

import java.io.Serializable;
import java.util.List;

public class OpenIdFederationGeneralConfig implements Serializable {

    private String organizationName;
    private List<String> contacts;
    private String logoUri;
    private String policyUri;
    private String organizationUri;
    private List<String> authorityHints;
    private Integer lifespan;
    // default 1 day - duration
    private String federationResolveEndpoint;
    private String federationHistoricalKeysEndpoint;
    private List<OpenIdFederationConfig> openIdFederationList = List.of();
    private List<EntityTypeEnum> entityTypes;
    private List<ClientRegistrationTypeEnum> opClientRegistrationTypesSupported;

    public OpenIdFederationGeneralConfig(){}

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public List<String> getContacts() {
        return contacts;
    }

    public void setContacts(List<String> contacts) {
        this.contacts = contacts;
    }

    public String getLogoUri() {
        return logoUri;
    }

    public void setLogoUri(String logoUri) {
        this.logoUri = logoUri;
    }

    public String getPolicyUri() {
        return policyUri;
    }

    public void setPolicyUri(String policyUri) {
        this.policyUri = policyUri;
    }

    public String getOrganizationUri() {
        return organizationUri;
    }

    public void setOrganizationUri(String organizationUri) {
        this.organizationUri = organizationUri;
    }

    public List<String> getAuthorityHints() {
        return authorityHints;
    }

    public void setAuthorityHints(List<String> authorityHints) {
        this.authorityHints = authorityHints;
    }

    public Integer getLifespan() {
        return lifespan;
    }

    public void setLifespan(Integer lifespan) {
        this.lifespan = lifespan;
    }

    public String getFederationResolveEndpoint() {
        return federationResolveEndpoint;
    }

    public void setFederationResolveEndpoint(String federationResolveEndpoint) {
        this.federationResolveEndpoint = federationResolveEndpoint;
    }

    public String getFederationHistoricalKeysEndpoint() {
        return federationHistoricalKeysEndpoint;
    }

    public void setFederationHistoricalKeysEndpoint(String federationHistoricalKeysEndpoint) {
        this.federationHistoricalKeysEndpoint = federationHistoricalKeysEndpoint;
    }

    public List<OpenIdFederationConfig> getOpenIdFederationList() {
        return openIdFederationList;
    }

    public void setOpenIdFederationList(List<OpenIdFederationConfig> openIdFederationList) {
        this.openIdFederationList = openIdFederationList == null ? List.of() : List.copyOf(openIdFederationList);
    }

    public List<EntityTypeEnum> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(List<EntityTypeEnum> entityTypes) {
        this.entityTypes = entityTypes;
    }

    public List<ClientRegistrationTypeEnum> getOpClientRegistrationTypesSupported() {
        return opClientRegistrationTypesSupported;
    }

    public void setOpClientRegistrationTypesSupported(List<ClientRegistrationTypeEnum> opClientRegistrationTypesSupported) {
        this.opClientRegistrationTypesSupported = opClientRegistrationTypesSupported;
    }
}


