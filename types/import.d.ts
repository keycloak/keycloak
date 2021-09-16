declare namespace Cypress {
  interface Chainable {
    /**
     * Get one or more DOM elements by `data-testid`.
     *
     * @example
     * cy.getId('searchButton')  // Gets the <button data-testid="searchButton">Search</button>
     */
    getId(selector: string, ...args): Chainable<any>;
  }
}
