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
    this.kerberosNameInput = "data-cy=kerberos-name";
    this.kerberosRealmInput = "data-cy=kerberos-realm";
    this.kerberosPrincipalInput = "data-cy=kerberos-principal";
    this.kerberosKeytabInput = "data-cy=kerberos-keytab";

    this.kerberosEnabledInput = "#Kerberos-switch";

    this.kerberosCacheDayInput = "#kc-eviction-day";
    this.kerberosCacheDayList = "#kc-eviction-day + ul";
    this.kerberosCacheHourInput = "#kc-eviction-hour";
    this.kerberosCacheHourList = "#kc-eviction-hour + ul";
    this.kerberosCacheMinuteInput = "#kc-eviction-minute";
    this.kerberosCacheMinuteList = "#kc-eviction-minute + ul";
    this.kerberosCachePolicyInput = "#kc-cache-policy";
    this.kerberosCachePolicyList = "#kc-cache-policy + ul";

    this.saveBtn = "data-cy=kerberos-save";
    this.cancelBtn = "data-cy=kerberos-cancel";
  }

  //#region Required Settings
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
