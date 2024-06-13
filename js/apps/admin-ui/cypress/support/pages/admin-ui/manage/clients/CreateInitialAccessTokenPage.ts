import CommonPage from "../../../CommonPage";

export default class CreateInitialAccessTokenPage extends CommonPage {
  #expirationInput = "expiration";
  #countInput = "count";
  #countPlusBtn = '[data-testid="count"] [aria-label="Plus"]';

  fillNewTokenData(expiration: number, count: number) {
    cy.findByTestId(this.#expirationInput).clear().type(expiration.toString());
    cy.findByTestId(this.#countInput).clear();

    for (let i = 0; i < count; i++) {
      cy.get(this.#countPlusBtn).click();
    }

    return this;
  }
}
