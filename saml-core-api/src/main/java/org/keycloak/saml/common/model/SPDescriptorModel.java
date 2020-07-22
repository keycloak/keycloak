package org.keycloak.saml.common.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.w3c.dom.Element;

public class SPDescriptorModel {

    private URI binding;
    private URI assertionEndpoint;
    private URI logoutEndpoint;
    private boolean wantAuthnRequestsSigned;
    private boolean wantAssertionsSigned;
    private boolean wantAssertionsEncrypted;
    private String entityId;
    private String nameIDPolicyFormat;
    private List<Element> signingCerts = new ArrayList<>();
    private List<Element> encryptionCerts = new ArrayList<>();
    private String displayName;
    private String description;
    private List<List<String>> mappers = new ArrayList<>();
    private String registrationAuthority;
    private String registrationPolicy;
    private String informationURL;
    private String privacyStatementURL;
    private String logo;
    private Integer logoHeight;
    private Integer logoWidth;
    private String local;
    private String organizationName;
    private String organizationDisplayName;
    private String organizationURL;
    private ContactTypeType contactType;
    private String contactCompany;
    private String contactGivenName;
    private String contactSurname;
    private List<String> contactEmailAddresses;
    private List<String> contactTelephoneNumbers;
    private Map<String,String> samlAttributes;
   

    public URI getBinding() {
        return binding;
    }

    public void setBinding(URI binding) {
        this.binding = binding;
    }

    public URI getAssertionEndpoint() {
        return assertionEndpoint;
    }

    public void setAssertionEndpoint(URI assertionEndpoint) {
        this.assertionEndpoint = assertionEndpoint;
    }

    public URI getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(URI logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public boolean isWantAuthnRequestsSigned() {
        return wantAuthnRequestsSigned;
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        this.wantAuthnRequestsSigned = wantAuthnRequestsSigned;
    }

    public boolean isWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    public void setWantAssertionsSigned(boolean wantAssertionsSigned) {
        this.wantAssertionsSigned = wantAssertionsSigned;
    }

    public boolean isWantAssertionsEncrypted() {
        return wantAssertionsEncrypted;
    }

    public void setWantAssertionsEncrypted(boolean wantAssertionsEncrypted) {
        this.wantAssertionsEncrypted = wantAssertionsEncrypted;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getNameIDPolicyFormat() {
        return nameIDPolicyFormat;
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        this.nameIDPolicyFormat = nameIDPolicyFormat;
    }

    public List<Element> getSigningCerts() {
        return signingCerts;
    }

    public void setSigningCerts(List<Element> signingCerts) {
        this.signingCerts = signingCerts;
    }

    public List<Element> getEncryptionCerts() {
        return encryptionCerts;
    }

    public void setEncryptionCerts(List<Element> encryptionCerts) {
        this.encryptionCerts = encryptionCerts;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<List<String>> getMappers() {
        return mappers;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local != null ? local : "en";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegistrationAuthority() {
        return registrationAuthority;
    }

    public void setRegistrationAuthority(String registrationAuthority) {
        this.registrationAuthority = registrationAuthority;
    }

    public String getRegistrationPolicy() {
        return registrationPolicy;
    }

    public void setRegistrationPolicy(String registrationPolicy) {
        this.registrationPolicy = registrationPolicy;
    }

    public String getInformationURL() {
        return informationURL;
    }

    public void setInformationURL(String informationURL) {
        this.informationURL = informationURL;
    }

    public String getPrivacyStatementURL() {
        return privacyStatementURL;
    }

    public void setPrivacyStatementURL(String privacyStatementURL) {
        this.privacyStatementURL = privacyStatementURL;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public Integer getLogoHeight() {
        return logoHeight;
    }

    public void setLogoHeight(Integer logoHeight) {
        this.logoHeight = logoHeight;
    }

    public Integer getLogoWidth() {
        return logoWidth;
    }

    public void setLogoWidth(Integer logoWidth) {
        this.logoWidth = logoWidth;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationURL() {
        return organizationURL;
    }

    public void setOrganizationURL(String organizationURL) {
        this.organizationURL = organizationURL;
    }

    public ContactTypeType getContactType() {
        return contactType;
    }

    public void setContactType(ContactTypeType contactType) {
        this.contactType = contactType;
    }

    public String getContactCompany() {
        return contactCompany;
    }

    public void setContactCompany(String contactCompany) {
        this.contactCompany = contactCompany;
    }

    public String getContactGivenName() {
        return contactGivenName;
    }

    public void setContactGivenName(String contactGivenName) {
        this.contactGivenName = contactGivenName;
    }

    public String getContactSurname() {
        return contactSurname;
    }

    public void setContactSurname(String contactSurname) {
        this.contactSurname = contactSurname;
    }

    public List<String> getContactEmailAddresses() {
        return contactEmailAddresses;
    }

    public void setContactEmailAddresses(List<String> contactEmailAddresses) {
        this.contactEmailAddresses = contactEmailAddresses;
    }

    public List<String> getContactTelephoneNumbers() {
        return contactTelephoneNumbers;
    }

    public void setContactTelephoneNumbers(List<String> contactTelephoneNumbers) {
        this.contactTelephoneNumbers = contactTelephoneNumbers;
    }

    public Map<String, String> getSamlAttributes() {
        return samlAttributes;
    }

    public void setSamlAttributes(Map<String, String> samlAttributes) {
        this.samlAttributes = samlAttributes;
    }

}
