export default class CreateKerberosProviderPage {
  kerberosNameInput: string;
  kerberosRealmInput: string;
  kerberosPrincipalInput: string;
  kerberosKeytabInput: string;

  // clientScopeTypeDrpDwn: string;
  // this.clientScopeTypeDrpDwn = "#kc-protocol";
  // selectClientScopeType(clientScopeType: string) {
  //   cy.get(this.clientScopeTypeDrpDwn).click();
  //   cy.get(this.clientScopeTypeList).contains(clientScopeType).click();

  //   return this;
  // }

  // kerberosCachePolicyInput: string;
  kerberosCacheDayInput: string;
  kerberosCacheDayList: string;
  kerberosCacheHourInput: string;
  kerberosCacheHourList: string;
  kerberosCacheMinuteInput: string;
  kerberosCacheMinuteList: string;
  // kerberosCacheLifespanInput: string;

  kerberosCachePolicyInput: string;
  kerberosCachePolicyList: string;

  realmRoleNameError: string;
  realmRoleDescriptionInput: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    // cypress IDs
    this.kerberosNameInput = "data-cy=kerberos-name";
    this.kerberosRealmInput = "data-cy=kerberos-realm";
    this.kerberosPrincipalInput = "data-cy=kerberos-principal";
    this.kerberosKeytabInput = "data-cy=kerberos-keytab";

    this.kerberosCachePolicyInput = "#kc-cache-policy";
    this.kerberosCachePolicyList = "#kc-cache-policy + ul";

    this.kerberosCacheDayInput = "#kc-eviction-day";
    this.kerberosCacheDayList = "#kc-eviction-day + ul";
    this.kerberosCacheHourInput = "#kc-eviction-hour";
    this.kerberosCacheHourList = "#kc-eviction-hour + ul";
    this.kerberosCacheMinuteInput = "#kc-eviction-minute";
    this.kerberosCacheMinuteList = "#kc-eviction-minute + ul";
    // this.kerberosCacheLifespanInput = "data-cy=kerberos-cache-lifespan";

    this.realmRoleNameError = "#kc-name-helper";
    this.realmRoleDescriptionInput = "#kc-role-description";

    this.saveBtn = "data-cy=kerberos-save";
    this.cancelBtn = "data-cy=kerberos-cancel";

    // this.cardTitle = "keycloak-card-title";
  }

  //#region Required Settings
  fillKerberosRequiredData(
    name: string,
    realm: string,
    principal: string,
    keytab: string
  ) {
    // cy.get(this.realmRoleNameInput).clear();
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

  // fillCachedData(day: string, hour: string, minute: string) {
    fillCachedData(hour: string, minute: string) {
      // cy.get(this.realmRoleNameInput).clear();
    // if (policy) {
    //   cy.get(`[${this.kerberosNameInput}]`).type(policy);
    // }
    // if (day) {
    //   cy.get(this.kerberosCacheDayInput).click();
    //   // cy.get(this.kerberosCacheDayInput).contains(day).click();
    //   cy.get(this.kerberosCacheDayInput).contains().click();

    // }
    if (hour) {
      cy.get(this.kerberosCacheHourInput).click();
      cy.get(this.kerberosCacheHourInput).contains(hour).click();
    }
    if (minute) {
      cy.get(this.kerberosCacheMinuteInput).click();
      cy.get(this.kerberosCacheMinuteInput).contains(minute).click();
    }
    return this;
  }

  // checkRealmRoleNameRequiredMessage(exist = true) {
  //   cy.get(this.realmRoleNameError).should((!exist ? "not." : "") + "exist");

  //   return this;
  // }
  //#endregion

  save() {
    cy.get(`[${this.saveBtn}]`).click();

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
