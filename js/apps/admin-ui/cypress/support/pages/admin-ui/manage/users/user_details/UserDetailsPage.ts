import { v4 as uuid } from "uuid";
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
  detailsTab: string;
  consentsTab: string;
  sessionsTab: string;

  constructor() {
    super();
    this.saveBtn = "save-user";
    this.cancelBtn = "cancel-create-user";
    this.emailInput = "email";
    this.emailValue = () => "example" + "_" + uuid() + "@example.com";
    this.firstNameInput = "firstName";
    this.firstNameValue = "firstname";
    this.lastNameInput = "lastName";
    this.lastNameValue = "lastname";
    this.requiredUserActions = [RequiredActionAlias.UPDATE_PASSWORD];
    this.identityProviderLinksTab = "identity-provider-links-tab";
    this.detailsTab = "user-details-tab";
    this.consentsTab = "user-consents-tab";
    this.sessionsTab = "user-sessions-tab";
  }

  public goToIdentityProviderLinksTab() {
    cy.intercept("/admin/realms/master/identity-provider/instances").as(
      "idpInstances",
    );
    cy.intercept("/admin/realms/master/users/*/federated-identity").as(
      "fedIdentity",
    );
    cy.findByTestId(this.identityProviderLinksTab).click();
    cy.wait(["@idpInstances", "@fedIdentity"]);

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

  goToDetailsTab() {
    cy.findByTestId(this.detailsTab).click();
    return this;
  }

  goToConsentsTab() {
    cy.findByTestId(this.consentsTab).click();
    return this;
  }

  goToSessionsTab() {
    cy.findByTestId(this.sessionsTab).click();
    return this;
  }

  toggleEnabled(userName: string) {
    this.#getEnabledSwitch(userName).click({ force: true });
  }

  assertEnabled(userName: string) {
    this.#getEnabledSwitchLabel(userName)
      .scrollIntoView()
      .contains("Enabled")
      .should("be.visible");
  }

  assertDisabled(userName: string) {
    this.#getEnabledSwitchLabel(userName)
      .scrollIntoView()
      .contains("Disabled")
      .should("be.visible");
  }

  #getEnabledSwitch(userName: string) {
    return cy.findByTestId(`${userName}-switch`);
  }

  #getEnabledSwitchLabel(userName: string) {
    return this.#getEnabledSwitch(userName).closest("label");
  }
}
