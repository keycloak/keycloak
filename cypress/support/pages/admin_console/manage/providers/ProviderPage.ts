export default class ProviderPage {
  kerberosNameInput: string;
  kerberosRealmInput: string;
  kerberosPrincipalInput: string;
  kerberosKeytabInput: string;

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

  cacheDayInput: string;
  cacheDayList: string;
  cacheHourInput: string;
  cacheHourList: string;
  cacheMinuteInput: string;
  cacheMinuteList: string;
  cachePolicyInput: string;
  cachePolicyList: string;

  saveBtn: string;
  cancelBtn: string;

  constructor() {
    // KerberosSettingsRequired required input values
    this.kerberosNameInput = "data-testid=kerberos-name";
    this.kerberosRealmInput = "data-testid=kerberos-realm";
    this.kerberosPrincipalInput = "data-testid=kerberos-principal";
    this.kerberosKeytabInput = "data-testid=kerberos-keytab";

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
    this.cacheDayInput = "#kc-eviction-day";
    this.cacheDayList = "#kc-eviction-day + ul";
    this.cacheHourInput = "#kc-eviction-hour";
    this.cacheHourList = "#kc-eviction-hour + ul";
    this.cacheMinuteInput = "#kc-eviction-minute";
    this.cacheMinuteList = "#kc-eviction-minute + ul";
    this.cachePolicyInput = "#kc-cache-policy";
    this.cachePolicyList = "#kc-cache-policy + ul";

    // LDAP settings enabled switch
    this.ldapEnabledInput = "#LDAP-switch";

    // LDAP action buttons
    this.saveBtn = "data-testid=ldap-save";
    this.cancelBtn = "data-testid=ldap-cancel";
  }

  changeCacheTime(unit: string, time: string) {
    switch (unit) {
      case "day":
        cy.get(this.cacheDayInput).click();
        cy.get(this.cacheDayList).contains(time).click();
        break;
      case "hour":
        cy.get(this.cacheHourInput).click();
        cy.get(this.cacheHourList).contains(time).click();
        break;
      case "minute":
        cy.get(this.cacheMinuteInput).click();
        cy.get(this.cacheMinuteList).contains(time).click();
        break;
      default:
        console.log("Invalid cache time, must be 'day', 'hour', or 'minute'.");
        break;
    }
    return this;
  }

  deleteCardFromCard(card: string) {
    cy.get(`[data-testid=${card}-dropdown]`).click();
    cy.get('[data-testid="card-delete"]').click();
    return this;
  }

  deleteCardFromMenu(providerType: string, card: string) {
    this.clickExistingCard(card);
    cy.get('[data-testid="action-dropdown"]').click();
    cy.get(`[data-testid="delete-${providerType}-cmd"]`).click();
    return this;
  }
  
  fillKerberosRequiredData(
    name: string,
    realm: string,
    principal: string,
    keytab: string
  ) {
    if (name) {
      cy.get(`[${this.kerberosNameInput}]`).type(name);
    }
    if (realm) {
      cy.get(`[${this.kerberosRealmInput}]`).type(realm);
    }
    if (principal) {
      cy.get(`[${this.kerberosPrincipalInput}]`).type(principal);
    }
    if (keytab) {
      cy.get(`[${this.kerberosKeytabInput}]`).type(keytab);
    }
    return this;
  }

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
    cy.get(this.cachePolicyInput).click();
    cy.get(this.cachePolicyList).contains(cacheType).click();
    return this;
  }

  clickExistingCard(cardName: string) {
    cy.get('[data-testid="keycloak-card-title"]').contains(cardName).click();
    cy.wait(1000);
    return this;
  }

  clickMenuCommand(menu: string, command: string) {
    cy.contains("button", menu).click();
    cy.contains("li", command).click();
    return this;
  }

  clickNewCard(providerType: string) {
    cy.get(`[data-testid=${providerType}-card]`).click();
    cy.wait(1000);
    return this;
  }

  disableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).uncheck({ force: true });
    return this;
  }

  enableEnabledSwitch(providerType: string) {
    cy.get(`#${providerType}-switch`).check({ force: true });
    return this;
  }

  save(providerType: string) {
    cy.get(`[data-testid=${providerType}-save]`).click();
    return this;
  }

  cancel(providerType: string) {
    cy.get(`[data-testid=${providerType}-cancel]`).click();
    return this;
  }
}
