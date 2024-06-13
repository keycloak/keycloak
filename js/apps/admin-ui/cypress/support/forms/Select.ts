export default class Select {
  static assertSelectedItem(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    itemName: string,
  ) {
    chain.parent().should("have.text", itemName);
  }

  static selectItem(
    chain: Cypress.Chainable<JQuery<HTMLElement>>,
    itemName: string,
  ) {
    chain.click();
    this.#getSelectMenu(chain).contains(itemName).click();
  }

  static #getSelectMenu(chain: Cypress.Chainable<JQuery<HTMLElement>>) {
    return chain.parent().get(".pf-v5-c-menu__list");
  }
}
