import CommonPage from "../../../CommonPage";

export default class CreateClientScopePage extends CommonPage {
  settingsTab: string;
  mappersTab: string;
  clientScopeNameInput: string;
  clientScopeNameError: string;
  clientScopeDescriptionInput: string;
  clientScopeTypeDrpDwn: string;
  clientScopeTypeList: string;
  displayOnConsentInput: string;
  displayOnConsentSwitch: string;
  consentScreenTextInput: string;
  includeInTokenSwitch: string;
  displayOrderInput: string;
  saveBtn: string;
  cancelBtn: string;

  constructor() {
    super();
    this.settingsTab = ".pf-v5-c-tabs__item:nth-child(1)";
    this.mappersTab = ".pf-v5-c-tabs__item:nth-child(2)";

    this.clientScopeNameInput = "name";
    this.clientScopeNameError = "#name-helper";
    this.clientScopeDescriptionInput = "description";
    this.clientScopeTypeDrpDwn = "#kc-protocol";
    this.clientScopeTypeList = "#kc-protocol + ul";
    this.displayOnConsentInput = "attributes.display🍺on🍺consent🍺screen";
    this.displayOnConsentSwitch =
      '[for="attributes.display🍺on🍺consent🍺screen"] .pf-v5-c-switch__toggle';
    this.consentScreenTextInput = "attributes.consent🍺screen🍺text";
    this.includeInTokenSwitch = "#attributes.include🍺in🍺token🍺scope-on";
    this.displayOrderInput = "attributes.gui🍺order";

    this.saveBtn = '[type="submit"]';
    this.cancelBtn = '[type="button"]';
  }

  //#region General Settings
  fillClientScopeData(
    name: string,
    description = "",
    consentScreenText = "",
    displayOrder = "",
  ) {
    cy.findByTestId(this.clientScopeNameInput).clear();

    if (name) {
      cy.findByTestId(this.clientScopeNameInput).type(name);
    }

    if (description) {
      cy.findByTestId(this.clientScopeDescriptionInput).type(description);
    }

    if (consentScreenText) {
      cy.findByTestId(this.consentScreenTextInput).type(consentScreenText);
    }

    if (displayOrder) {
      cy.findByTestId(this.displayOrderInput).type(displayOrder);
    }

    return this;
  }

  selectClientScopeType(clientScopeType: string) {
    cy.get(this.clientScopeTypeDrpDwn).click();
    cy.get(this.clientScopeTypeList).contains(clientScopeType).click();

    return this;
  }

  getSwitchDisplayOnConsentScreenInput() {
    return cy.findByTestId(this.displayOnConsentInput);
  }

  getConsentScreenTextInput() {
    return cy.findByTestId(this.consentScreenTextInput);
  }

  switchDisplayOnConsentScreen() {
    cy.get(this.displayOnConsentSwitch).click();

    return this;
  }

  switchIncludeInTokenScope() {
    cy.get(this.includeInTokenSwitch).click();

    return this;
  }
  //#endregion

  save() {
    cy.get(this.saveBtn).click();

    return this;
  }

  save_is_disabled(value: boolean) {
    cy.get(this.saveBtn)
      .invoke("attr", "aria-disabled")
      .should("eq", value ? "true" : "false");

    return this;
  }

  cancel() {
    cy.get(this.cancelBtn).click();

    return this;
  }
}
