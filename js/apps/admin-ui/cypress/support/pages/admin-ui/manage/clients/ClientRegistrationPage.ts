import CommonPage from "../../../CommonPage";

export class ClientRegistrationPage extends CommonPage {
  goToClientRegistrationTab() {
    this.tabUtils().clickTab("registration");
    return this;
  }

  createPolicy() {
    cy.findAllByTestId("createPolicy").click();
    return this;
  }

  selectRow(name: string) {
    cy.findAllByTestId(name).click();
    return this;
  }

  fillPolicyForm(props: { name: string }) {
    cy.findAllByTestId("name").clear().type(props.name);
    return this;
  }
}
