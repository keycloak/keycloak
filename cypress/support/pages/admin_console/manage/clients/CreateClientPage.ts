export default class CreateClientPage {
  clientTypeDrpDwn: string;
  clientTypeError: string;
  clientTypeList: string;
  clientIdInput: string;
  clientIdError: string;
  clientDescriptionInput: string;
  clientAuthenticationSwitch: string;
  standardFlowChkBx: string;
  directAccessChkBx: string;
  implicitFlowChkBx: string;
  serviceAccountRolesChkBx: string;
  continueBtn: string;
  backBtn: string;
  cancelBtn: string;
  clientNameInput: string;

  constructor() {
    this.clientTypeDrpDwn = ".pf-c-select__toggle";
    this.clientTypeError = ".pf-c-select + div";
    this.clientTypeList = ".pf-c-select__toggle + ul";
    this.clientIdInput = "#kc-client-id";
    this.clientIdError = "#kc-client-id + div";
    this.clientNameInput = "#kc-name";
    this.clientDescriptionInput = "#kc-description";

    this.clientAuthenticationSwitch =
      '[for="kc-authentication"] .pf-c-switch__toggle';
    this.clientAuthenticationSwitch =
      '[for="kc-authorization"] .pf-c-switch__toggle';
    this.standardFlowChkBx = "#kc-flow-standard";
    this.directAccessChkBx = "#kc-flow-direct";
    this.implicitFlowChkBx = "#kc-flow-implicit";
    this.serviceAccountRolesChkBx = "#kc-flow-service-account";

    this.continueBtn = ".pf-c-wizard__footer .pf-m-primary";
    this.backBtn = ".pf-c-wizard__footer .pf-m-secondary";
    this.cancelBtn = ".pf-c-wizard__footer .pf-m-link";
  }

  //#region General Settings
  selectClientType(clientType: string) {
    cy.get(this.clientTypeDrpDwn).click();
    cy.get(this.clientTypeList).findByTestId(`option-${clientType}`).click();

    return this;
  }

  fillClientData(id: string, name = "", description = "") {
    cy.get(this.clientIdInput).clear();

    if (id) {
      cy.get(this.clientIdInput).type(id);
    }

    if (name) {
      cy.get(this.clientNameInput).type(name);
    }

    if (description) {
      cy.get(this.clientDescriptionInput).type(description);
    }

    return this;
  }

  changeSwitches(switches: string[]) {
    for (const uiSwitch of switches) {
      cy.findByTestId(uiSwitch).check({ force: true });
    }
    return this;
  }

  checkClientIdRequiredMessage(exist = true) {
    cy.get(this.clientIdError).should((!exist ? "not." : "") + "exist");

    return this;
  }
  //#endregion

  //#region Capability config
  switchClientAuthentication() {
    cy.get(this.clientAuthenticationSwitch).click();

    return this;
  }

  clickStandardFlow() {
    cy.get(this.standardFlowChkBx).click();

    return this;
  }

  clickDirectAccess() {
    cy.get(this.directAccessChkBx).click();

    return this;
  }

  clickImplicitFlow() {
    cy.get(this.implicitFlowChkBx).click();

    return this;
  }

  clickServiceAccountRoles() {
    cy.get(this.serviceAccountRolesChkBx).click();

    return this;
  }
  //#endregion

  continue() {
    cy.get(this.continueBtn).click();

    return this;
  }

  back() {
    cy.get(this.backBtn).click();

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
