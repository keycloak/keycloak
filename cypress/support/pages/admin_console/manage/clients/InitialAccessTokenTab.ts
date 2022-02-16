export default class InitialAccessTokenTab {
  private initialAccessTokenTab = "initialAccessToken";

  private emptyAction = "no-initial-access-tokens-empty-action";

  private expirationInput = "expiration";
  private countInput = "count";
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
    cy.findByTestId(this.expirationInput).clear().type(`${expiration}`);
    cy.findByTestId(this.countInput).clear();

    for (let i = 0; i < count; i++) {
      cy.get(this.countPlusBtn).click();
    }

    return this;
  }

  save() {
    cy.findByTestId(this.saveBtn).click();
    return this;
  }
}
