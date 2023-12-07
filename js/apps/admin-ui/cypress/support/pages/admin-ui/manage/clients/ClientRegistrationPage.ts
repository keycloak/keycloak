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

  selectRow(name: string) {
    cy.findAllByTestId(name).click();
    return this;
  }

  fillPolicyForm(props: { name: string }) {
    cy.findAllByTestId("name").type(props.name);
    return this;
  }
}
