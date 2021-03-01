export default class CreateLdapProviderPage {
  ldapNameInput: string;
  ldapVendorInput: string;
  ldapVendorList: string;

  ldapConnectionUrlInput: string;
  ldapBindTypeInput: string;
  ldapBindTypeList: string;
  ldapBindDnInput: string;
  ldapBindCredsInput: string;

  ldapUsersDnInput: string;
  ldapUserLdapAttInput: string;
  ldapRdnLdapAttInput: string;
  ldapUuidLdapAttInput: string;
  ldapUserObjClassesInput: string;

  ldapEnabledInput: string;

  ldapCacheDayInput: string;
  ldapCacheDayList: string;
  ldapCacheHourInput: string;
  ldapCacheHourList: string;
  ldapCacheMinuteInput: string;
  ldapCacheMinuteList: string;
  ldapCachePolicyInput: string;
  ldapCachePolicyList: string;

  saveBtn: string;
  cancelBtn: string;

  constructor() {
    // LdapSettingsGeneral required input values
    this.ldapNameInput = "data-testid=ldap-name";
    this.ldapVendorInput = "#kc-vendor";
    this.ldapVendorList = "#kc-vendor + ul";

    // LdapSettingsConnection required input values
    this.ldapConnectionUrlInput = "data-testid=ldap-connection-url";
    this.ldapBindTypeInput = "#kc-bind-type";
    this.ldapBindTypeList = "#kc-bind-type + ul";
    this.ldapBindDnInput = "data-testid=ldap-bind-dn";
    this.ldapBindCredsInput = "data-testid=ldap-bind-credentials";

    // LdapSettingsSearching required input values
    this.ldapUsersDnInput = "data-testid=ldap-users-dn";
    this.ldapUserLdapAttInput = "data-testid=ldap-username-attribute";
    this.ldapRdnLdapAttInput = "data-testid=ldap-rdn-attribute";
    this.ldapUuidLdapAttInput = "data-testid=ldap-uuid-attribute";
    this.ldapUserObjClassesInput = "data-testid=ldap-user-object-classes";

    // SettingsCache input values
    this.ldapCacheDayInput = "#kc-eviction-day";
    this.ldapCacheDayList = "#kc-eviction-day + ul";
    this.ldapCacheHourInput = "#kc-eviction-hour";
    this.ldapCacheHourList = "#kc-eviction-hour + ul";
    this.ldapCacheMinuteInput = "#kc-eviction-minute";
    this.ldapCacheMinuteList = "#kc-eviction-minute + ul";
    this.ldapCachePolicyInput = "#kc-cache-policy";
    this.ldapCachePolicyList = "#kc-cache-policy + ul";

    // LDAP settings enabled switch
    this.ldapEnabledInput = "#LDAP-switch";

    // LDAP action buttons
    this.saveBtn = "data-testid=ldap-save";
    this.cancelBtn = "data-testid=ldap-cancel";
  }

  // Required fields - these always must be filled out when testing a save, everything else can feasibly be left blank (TEST THIS)
  fillLdapRequiredGeneralData(name: string, vendor: string) {
    if (name) {
      cy.get(`[${this.ldapNameInput}]`).type(name);
    }
    if (vendor) {
      cy.get(this.ldapVendorInput).click();
      cy.get(this.ldapVendorList).contains(vendor).click();
    }
    return this;
  }

  fillLdapRequiredConnectionData(
    connectionUrl: string,
    bindType: string,
    bindDn: string,
    bindCreds: string
  ) {
    if (connectionUrl) {
      cy.get(`[${this.ldapConnectionUrlInput}]`).type(connectionUrl);
    }
    if (bindType) {
      cy.get(this.ldapBindTypeInput).click();
      cy.get(this.ldapBindTypeList).contains(bindType).click();
    }
    if (bindDn) {
      cy.get(`[${this.ldapBindDnInput}]`).type(bindDn);
    }
    if (bindCreds) {
      cy.get(`[${this.ldapBindCredsInput}]`).type(bindCreds);
    }
    return this;
  }

  fillLdapRequiredSearchingData(
    usersDn: string,
    userLdapAtt: string,
    rdnLdapAtt: string,
    uuidLdapAtt: string,
    userObjClasses: string
  ) {
    if (usersDn) {
      cy.get(`[${this.ldapUsersDnInput}]`).type(usersDn);
    }
    if (userLdapAtt) {
      cy.get(`[${this.ldapUserLdapAttInput}]`).type(userLdapAtt);
    }
    if (rdnLdapAtt) {
      cy.get(`[${this.ldapRdnLdapAttInput}]`).type(rdnLdapAtt);
    }
    if (uuidLdapAtt) {
      cy.get(`[${this.ldapUuidLdapAttInput}]`).type(uuidLdapAtt);
    }
    if (userObjClasses) {
      cy.get(`[${this.ldapUserObjClassesInput}]`).type(userObjClasses);
    }
    return this;
  }

  selectCacheType(cacheType: string) {
    cy.get(this.ldapCachePolicyInput).click();
    cy.get(this.ldapCachePolicyList).contains(cacheType).click();
    return this;
  }

  clickProviderCard(cardName: string) {
    cy.get('[data-testid="keycloak-card-title"]').contains(cardName).click();
    cy.wait(1000);
    return this;
  }

  disableEnabledSwitch() {
    cy.get(this.ldapEnabledInput).uncheck({ force: true });
    return this;
  }

  enableEnabledSwitch() {
    cy.get(this.ldapEnabledInput).check({ force: true });
    return this;
  }

  save() {
    cy.get(`[${this.saveBtn}]`).click();
    return this;
  }

  cancel() {
    cy.get(`[${this.cancelBtn}]`).click();
    return this;
  }
}
