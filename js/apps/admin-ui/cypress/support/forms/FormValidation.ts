export default class FormValidation {
  static assertRequired(chain: Cypress.Chainable<JQuery<HTMLElement>>) {
    return this.assertMessage(chain, "Required field");
  }

  static assertMessage(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    expectedMessage: string,
  ) {
    return this.#getHelperText(chain).should("have.text", expectedMessage);
  }

  static assertMinValue(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    minValue: number,
  ) {
    this.assertMessage(chain, `Must be greater than ${minValue}`);
  }

  static assertMaxValue(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    maxValue: number,
  ) {
    this.assertMessage(chain, `Must be less than ${maxValue}`);
  }

  static #getHelperText(chain: Cypress.Chainable<JQuery<HTMLElement>>) {
    // A regular ID selector doesn't work here so we have to query by attribute.
    return chain
      .invoke("attr", "data-testid")
      .then((id) => cy.get(`[data-testid="${id}-helper"]`));
  }
}
