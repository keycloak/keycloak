export default class FormValidation {
  static assertRequired(chain: Cypress.Chainable<JQuery<HTMLElement>>) {
    return this.#getHelperText(chain).should("have.text", "Required field");
  }

  static assertMinValue(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    minValue: number
  ) {
    this.#getHelperText(chain).should(
      "have.text",
      `Must be greater than ${minValue}`
    );
  }

  static assertMaxValue(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    maxValue: number
  ) {
    this.#getHelperText(chain).should(
      "have.text",
      `Must be less than ${maxValue}`
    );
  }

  static #getHelperText(chain: Cypress.Chainable<JQuery<HTMLElement>>) {
    // A regular ID selector doesn't work here so we have to query by attribute.
    return chain
      .invoke("attr", "id")
      .then((id) => cy.get(`[id="${id}-helper"]`));
  }
}
