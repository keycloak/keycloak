const expect = chai.expect;

export default class RealmSelector {
  private realmSelector = "realmSelector";
  private realmContextSelector = ".keycloak__realm_selector__context_selector";

  shouldContainAll(realmsList: string[]) {
    cy.getId(this.realmSelector)
      .scrollIntoView()
      .get("ul")
      .should((realms) => {
        for (let index = 0; index < realmsList.length; index++) {
          const realmName = realmsList[index];
          expect(realms).to.contain(realmName);
        }
      });

    return this;
  }

  openRealmContextSelector() {
    cy.getId(this.realmSelector).scrollIntoView();
    cy.get(this.realmContextSelector).click();

    return this;
  }
}
