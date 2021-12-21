import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";

export default class UserDetailsPage {
  saveBtn: string;
  cancelBtn: string;
  emailInput: string;
  emailValue: () => string;
  firstNameInput: string;
  firstNameValue: string;
  lastNameInput: string;
  lastNameValue: string;
  enabledSwitch: string;
  enabledValue: boolean;
  requiredUserActions: RequiredActionAlias[];

  constructor() {
    this.saveBtn = "save-user";
    this.cancelBtn = "cancel-create-user";
    this.emailInput = "email-input";
    this.emailValue = () =>
      "example" +
      "_" +
      (Math.random() + 1).toString(36).substring(7) +
      "@example.com";
    this.firstNameInput = "firstName-input";
    this.firstNameValue = "firstname";
    this.lastNameInput = "lastName-input";
    this.lastNameValue = "lastname";
    this.enabledSwitch = "user-enabled-switch";
    this.enabledValue = true;
    this.requiredUserActions = [RequiredActionAlias.UPDATE_PASSWORD];
  }

  fillUserData() {
    cy.findByTestId(this.emailInput).type(this.emailValue());
    cy.findByTestId(this.firstNameInput).type(this.firstNameValue);
    cy.findByTestId(this.lastNameInput).type(this.lastNameValue);
    cy.findByTestId(this.enabledSwitch).check({ force: true });

    return this;
  }

  save() {
    cy.findByTestId(this.saveBtn).click();

    return this;
  }

  cancel() {
    cy.findByTestId(this.cancelBtn).click();

    return this;
  }
}
