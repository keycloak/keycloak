import CommonPage from "../../../CommonPage";

export default class OrganizationPage extends CommonPage {
  #nameField = "[data-testid='name']";

  goToTab() {
    cy.get("#nav-item-organizations").click();
  }

  goToCreate(empty: boolean = true) {
    cy.findByTestId(
      empty ? "no-organizations-empty-action" : "addOrganization",
    ).click();
  }

  fillCreatePage(values: {
    name: string;
    domain?: string[];
    description?: string;
  }) {
    this.fillNameField(values.name);
    values.domain?.forEach((d, index) => {
      cy.findByTestId(`domains${index}`).type(d);
      if (index !== (values.domain?.length || 0) - 1)
        cy.findByTestId("addValue").click();
    });
    if (values.description)
      cy.findByTestId("description").type(values.description);
  }

  getNameField() {
    return cy.get(this.#nameField);
  }

  fillNameField(name: string) {
    cy.get(this.#nameField).clear().type(name);
    return this.getNameField();
  }

  assertSaveSuccess() {
    this.masthead().checkNotificationMessage(
      "Organization successfully saved.",
    );
  }

  assertDeleteSuccess() {
    this.masthead().checkNotificationMessage(
      "The organization has been deleted",
    );
  }
}
