import CommonPage from "../../../CommonPage";

export class ClientRegistrationPage extends CommonPage {
  goToClientRegistrationTab() {
    this.tabUtils().clickTab("registration");
    return this;
  }

  goToAuthenticatedSubTab() {
    cy.findAllByTestId("authenticated").click();
    return this;
  }

  createPolicy() {
    cy.findAllByTestId("createPolicy").click({ force: true });
    return this;
  }

  createAnonymousPolicy() {
    cy.findByTestId("createPolicy-anonymous").click();
    return this;
  }

  createAuthenticatedPolicy() {
    cy.findByTestId("createPolicy-authenticated").click();
    return this;
  }

  findAndSelectInAnonymousPoliciesTable(policy: string) {
    cy.findByTestId("clientRegistration-anonymous")
      .find("tr")
      .contains(policy)
      .click();
  }

  findAndSelectInAuthenticatedPoliciesTable(policy: string) {
    cy.findByTestId("clientRegistration-authenticated")
      .find("tr")
      .contains(policy)
      .click();
  }

  selectRow(name: string) {
    cy.findAllByTestId(name).click();
    return this;
  }

  fillPolicyForm(props: { name: string }) {
    cy.findAllByTestId("name").type(props.name);
    return this;
  }
}
