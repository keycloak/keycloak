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
    cy.get('#otpPolicyInitialCounter [aria-label="Plus"]').click();
    return this;
  }

  checkSupportedApplications(...supportedApplications: string[]) {
    cy.findByTestId("supportedApplications")
      .children()
      .should("have.text", supportedApplications.join(""));
    return this;
  }

  save() {
    cy.findByTestId("save").click();
    return this;
  }
}
