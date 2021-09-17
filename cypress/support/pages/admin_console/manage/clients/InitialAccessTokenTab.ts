export default class InitialAccessTokenTab {
  private initialAccessTokenTab = "initialAccessToken";

  private emptyAction = "no-initial-access-tokens-empty-action";

  private expirationInput = "expiration";
  private countInput = "count";
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

  getFistId(callback: (id: string) => void) {
    cy.get('tbody > tr > [data-label="ID"]')
      .invoke("text")
      .then((text) => {
        callback(text);
      });
    return this;
  }

  createNewToken(expiration: number, count: number) {
    cy.findByTestId(this.emptyAction).click();
    cy.findByTestId(this.expirationInput).type(`${expiration}`);
    cy.findByTestId(this.countInput).type(`${count}`);
    return this;
  }

  save() {
    cy.findByTestId(this.saveBtn).click();
    return this;
  }
}
