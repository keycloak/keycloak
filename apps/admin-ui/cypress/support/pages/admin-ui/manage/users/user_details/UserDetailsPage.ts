import { RequiredActionAlias } from "@keycloak/keycloak-admin-client/lib/defs/requiredActionProviderRepresentation";
import PageObject from "../../../components/PageObject";

export default class UserDetailsPage extends PageObject {
  saveBtn: string;
  cancelBtn: string;
  emailInput: string;
  emailValue: () => string;
  firstNameInput: string;
  firstNameValue: string;
  lastNameInput: string;
  lastNameValue: string;
  requiredUserActions: RequiredActionAlias[];
  identityProviderLinksTab: string;

  constructor() {
    super();
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
    this.requiredUserActions = [RequiredActionAlias.UPDATE_PASSWORD];
    this.identityProviderLinksTab = "identity-provider-links-tab";
  }

  public goToIdentityProviderLinksTab() {
    cy.findByTestId(this.identityProviderLinksTab).click();
    cy.intercept("/admin/realms/master").as("load");
    cy.wait(["@load"]);

    return this;
  }

  fillUserData() {
    cy.findByTestId(this.emailInput).type(this.emailValue());
    cy.findByTestId(this.firstNameInput).type(this.firstNameValue);
    cy.findByTestId(this.lastNameInput).type(this.lastNameValue);

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
