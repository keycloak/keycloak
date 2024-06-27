import CommonPage from "../../../../../../CommonPage";

export default class PoliciesTab extends CommonPage {
  #emptyPolicyCreateBtn = "no-policies-empty-action";
  #createPolicyBtn = "createPolicy";

  createPolicy(type: string, first = false) {
    if (first) {
      cy.findByTestId(this.#emptyPolicyCreateBtn).click();
    } else {
      cy.findByTestId(this.#createPolicyBtn).click();
    }
    cy.findByTestId(type).click();
    return this;
  }

  fillBasePolicyForm(policy: { [key: string]: string }) {
    Object.entries(policy).map(([key, value]) =>
      cy.findByTestId(key).type(value),
    );
    return this;
  }

  setPolicy(policyName: string) {
    cy.findByTestId(policyName).click();
    return this;
  }

  inputClient(clientName: string) {
    cy.get("#clients").click();
    cy.get("ul li").contains(clientName).click();
    cy.get("#clients").click();
    return this;
  }
}
