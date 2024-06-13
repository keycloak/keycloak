import GroupPage from "./GroupPage";

export class SearchGroupPage extends GroupPage {
  #groupSearchField = "group-search";
  #searchButton = "[data-testid='group-search'] button[type='submit']";

  public searchGroup(groupName: string) {
    this.typeSearchInput(groupName);
    this.clickSearchButton();
    return this;
  }

  public searchGlobal(searchValue: string) {
    this.search("[data-testid='searchForGroups']", searchValue, true);

    return this;
  }

  public typeSearchInput(value: string) {
    cy.findByTestId(this.#groupSearchField).type(value);
    return this;
  }

  public clickSearchButton() {
    cy.get(this.#searchButton).click();
    return this;
  }

  public checkTerm(searchTerm: string) {
    cy.get(".pf-v5-c-chip-group")
      .children()
      .contains(searchTerm)
      .should("exist");
    return this;
  }
}
