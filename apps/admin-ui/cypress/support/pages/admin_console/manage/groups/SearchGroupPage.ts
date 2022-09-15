import GroupPage from "./GroupPage";

export class SearchGroupPage extends GroupPage {
  private searchField = "group-search";
  private searchButton = "[data-testid='group-search'] > button";

  public searchGroup(groupName: string) {
    this.typeSearchInput(groupName);
    this.clickSearchButton();
    return this;
  }

  public typeSearchInput(value: string) {
    cy.findByTestId(this.searchField).type(value);
    return this;
  }

  public clickSearchButton() {
    cy.get(this.searchButton).click();
    return this;
  }

  public checkTerm(searchTerm: string) {
    cy.get(".pf-c-chip-group").children().contains(searchTerm).should("exist");
    return this;
  }
}
