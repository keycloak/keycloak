export default class OTPPolicies {
  goToTab() {
    cy.findAllByTestId("policies").click().get("#pf-tab-2-otpPolicy").click();
    return this;
  }

  setPolicyType(type: string) {
    cy.findByTestId(type).click();
    return this;
  }

  increaseInitialCounter() {
    cy.get('#initialCounter > .pf-c-input-group > [aria-label="Plus"]').click();
    return this;
  }

  checkSupportedActions(...supportedActions: string[]) {
    cy.findByTestId("supportedActions").should(
      "have.text",
      supportedActions.join("")
    );
    return this;
  }

  save() {
    cy.findByTestId("save").click();
    return this;
  }
}
