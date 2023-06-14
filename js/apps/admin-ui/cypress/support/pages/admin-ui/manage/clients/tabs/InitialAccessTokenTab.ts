import CommonPage from "../../../../CommonPage";

export default class InitialAccessTokenTab extends CommonPage {
  private initialAccessTokenTab = "initialAccessToken";

  private emptyAction = "no-initial-access-tokens-empty-action";

  private expirationNumberInput = "expiration";
  private expirationInput = 'input[name="count"]';
  private expirationText = "#expiration-helper";
  private countInput = '[data-testid="count"] input';
  private countPlusBtn = '[data-testid="count"] [aria-label="Plus"]';
  private saveBtn = "save";

  goToInitialAccessTokenTab() {
    cy.findByTestId(this.initialAccessTokenTab).click();
    return this;
  }

  shouldBeEmpty() {
    cy.findByTestId(this.emptyAction).should("exist");
    return this;
  }

  shouldNotBeEmpty() {
    cy.findByTestId(this.emptyAction).should("not.exist");
    return this;
  }

  getFirstId(callback: (id: string) => void) {
    cy.get('tbody > tr:first-child > [data-label="ID"]')
      .invoke("text")
      .then((text) => {
        callback(text);
      });
    return this;
  }

  goToCreateFromEmptyList() {
    cy.findByTestId(this.emptyAction).click();
    return this;
  }

  fillNewTokenData(expiration: number, count: number) {
    cy.findByTestId(this.expirationNumberInput).clear().type(`${expiration}`);
    cy.get(this.countInput).clear();

    for (let i = 0; i < count; i++) {
      cy.get(this.countPlusBtn).click();
    }

    return this;
  }

  save() {
    cy.findByTestId(this.saveBtn).click();
    return this;
  }

  checkExpirationGreaterThanZeroError() {
    cy.get(this.expirationText).should(
      "have.text",
      "Value should should be greater or equal to 1"
    );
    return this;
  }

  checkCountValue(value: number) {
    cy.get(this.expirationInput).should("have.value", value);
    return this;
  }

  checkSaveButtonIsDisabled() {
    cy.findByTestId(this.saveBtn).should("be.disabled");
    return this;
  }
}
