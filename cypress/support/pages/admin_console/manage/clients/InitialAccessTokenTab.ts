export default class InitialAccessTokenTab {
  private initialAccessTokenTab = "initialAccessToken";

  private emptyAction = "no-initial-access-tokens-empty-action";

  private expirationInput = "expiration";
  private countInput = "count";
  private saveBtn = "save";

  goToInitialAccessTokenTab() {
    cy.getId(this.initialAccessTokenTab).click();
    return this;
  }

  shouldBeEmpty() {
    cy.getId(this.emptyAction).should("exist");
    return this;
  }

  shouldNotBeEmpty() {
    cy.getId(this.emptyAction).should("not.exist");
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
    cy.getId(this.emptyAction).click();
    cy.getId(this.expirationInput).type(`${expiration}`);
    cy.getId(this.countInput).type(`${count}`);
    return this;
  }

  save() {
    cy.getId(this.saveBtn).click();
    return this;
  }
}
