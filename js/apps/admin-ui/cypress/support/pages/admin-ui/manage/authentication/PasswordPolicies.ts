export default class PasswordPolicies {
  goToTab() {
    cy.findAllByTestId("policies").click();
    return this;
  }

  shouldShowEmptyState() {
    cy.findByTestId("empty-state").should("exist");
    return this;
  }

  addPolicy(name: string) {
    cy.findByTestId("add-policy").click().parent().contains(name).click();
    return this;
  }

  removePolicy(name: string) {
    cy.findByTestId(name).click();
    return this;
  }

  save() {
    cy.findByTestId("save").click();
    return this;
  }
}
