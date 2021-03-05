export default class CreateKerberosProviderPage {
  kerberosNameInput: string;
  kerberosRealmInput: string;
  kerberosPrincipalInput: string;
  kerberosKeytabInput: string;

  kerberosEnabledInput: string;

  kerberosCacheDayInput: string;
  kerberosCacheDayList: string;
  kerberosCacheHourInput: string;
  kerberosCacheHourList: string;
  kerberosCacheMinuteInput: string;
  kerberosCacheMinuteList: string;
  kerberosCachePolicyInput: string;
  kerberosCachePolicyList: string;

  saveBtn: string;
  cancelBtn: string;

  constructor() {
    // KerberosSettingsRequired required input values
    this.kerberosNameInput = "data-testid=kerberos-name";
    this.kerberosRealmInput = "data-testid=kerberos-realm";
    this.kerberosPrincipalInput = "data-testid=kerberos-principal";
    this.kerberosKeytabInput = "data-testid=kerberos-keytab";

    // SettingsCache input values
    this.kerberosCacheDayInput = "#kc-eviction-day";
    this.kerberosCacheDayList = "#kc-eviction-day + ul";
    this.kerberosCacheHourInput = "#kc-eviction-hour";
    this.kerberosCacheHourList = "#kc-eviction-hour + ul";
    this.kerberosCacheMinuteInput = "#kc-eviction-minute";
    this.kerberosCacheMinuteList = "#kc-eviction-minute + ul";
    this.kerberosCachePolicyInput = "#kc-cache-policy";
    this.kerberosCachePolicyList = "#kc-cache-policy + ul";

    // Kerberos settings enabled switch
    this.kerberosEnabledInput = "#Kerberos-switch";

    // Kerberos action buttons
    this.saveBtn = "data-testid=kerberos-save";
    this.cancelBtn = "data-testid=kerberos-cancel";
  }

  changeTime(oldTime: string, newTime: string) {
    cy.contains(oldTime).click();
    cy.contains(newTime).click();
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

  // Required fields - these always must be filled out when testing a save
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

  selectCacheType(cacheType: string) {
    cy.get(this.kerberosCachePolicyInput).click();
    cy.get(this.kerberosCachePolicyList).contains(cacheType).click();
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

  disableEnabledSwitch() {
    cy.get(this.kerberosEnabledInput).uncheck({ force: true });
  }

  enableEnabledSwitch() {
    cy.get(this.kerberosEnabledInput).check({ force: true });
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
