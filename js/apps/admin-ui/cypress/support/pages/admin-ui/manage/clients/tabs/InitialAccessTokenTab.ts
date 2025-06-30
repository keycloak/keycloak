import CommonPage from "../../../../CommonPage";

export default class InitialAccessTokenTab extends CommonPage {
  #initialAccessTokenTab = "initialAccessToken";

  #emptyAction = "no-initial-access-tokens-empty-action";

  #expirationNumberInput = "expiration";
  #expirationText = ".pf-v5-c-helper-text__item-text";
  #countInput = "#count input";
  #countPlusBtn = '#count [aria-label="Plus"]';
  #saveBtn = "save";

  goToInitialAccessTokenTab() {
    cy.findByTestId(this.#initialAccessTokenTab).click();
    return this;
  }

  shouldBeEmpty() {
    cy.findByTestId(this.#emptyAction).should("exist");
    return this;
  }

  shouldNotBeEmpty() {
    cy.findByTestId(this.#emptyAction).should("not.exist");
    return this;
  }

  getFirstId(callback: (id: string) => void) {
    cy.get("tbody > tr:first-child > td:first-child")
      .invoke("text")
      .then((text) => {
        callback(text);
      });
    return this;
  }

  goToCreateFromEmptyList() {
    cy.findByTestId(this.#emptyAction).click();
    return this;
  }

  fillNewTokenData(expiration: number, count: number) {
    cy.findByTestId(this.#expirationNumberInput).clear();
    cy.findByTestId(this.#expirationNumberInput).type(`${expiration}`);
    cy.get(this.#countInput).clear();

    for (let i = 0; i < count; i++) {
      cy.get(this.#countPlusBtn).click();
    }

    return this;
  }

  save() {
    cy.findByTestId(this.#saveBtn).click();
    return this;
  }

  checkExpirationGreaterThanZeroError() {
    cy.get(this.#expirationText).should(
      "have.text",
      "Value should should be greater or equal to 1",
    );
    return this;
  }

  checkCountValue(value: number) {
    cy.get(this.#countInput).should("have.value", value);
    return this;
  }

  checkSaveButtonIsDisabled() {
    cy.findByTestId(this.#saveBtn).should("be.disabled");
    return this;
  }
}
