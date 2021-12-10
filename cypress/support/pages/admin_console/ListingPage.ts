export default class ListingPage {
  private searchInput = '.pf-c-toolbar__item [type="search"]:visible';
  private itemsRows = "table:visible";
  private itemRowDrpDwn = ".pf-c-dropdown__toggle";
  public exportBtn = '[role="menuitem"]:nth-child(1)';
  public deleteBtn = '[role="menuitem"]:nth-child(2)';
  private searchBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section button.pf-m-control:visible";
  private createBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section .pf-m-primary:visible";
  private importBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section .pf-m-link";
  private previousPageBtn =
    "div[class=pf-c-pagination__nav-control] button[data-action=previous]:visible";
  private nextPageBtn =
    "div[class=pf-c-pagination__nav-control] button[data-action=next]:visible";
  public tableRowItem = "tbody tr[data-ouia-component-type]:visible";

  showPreviousPageTableItems() {
    cy.get(this.previousPageBtn).first().click();

    return this;
  }

  showNextPageTableItems() {
    cy.get(this.nextPageBtn).first().click();

    return this;
  }

  goToCreateItem() {
    cy.get(this.createBtn).click();

    return this;
  }

  goToImportItem() {
    cy.get(this.importBtn).click();

    return this;
  }

  searchItem(searchValue: string, wait = true) {
    if (wait) {
      const searchUrl = `/auth/admin/realms/master/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }
    cy.get(this.searchInput).clear().type(searchValue);
    cy.get(this.searchBtn).click();
    if (wait) {
      cy.wait(["@search"]);
    }
    return this;
  }

  clickRowDetails(itemName: string) {
    cy.get(this.itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.itemRowDrpDwn)
      .click();
    return this;
  }

  clickDetailMenu(name: string) {
    cy.get(this.itemsRows).contains(name).click();
    return this;
  }

  itemExist(itemName: string, exist = true) {
    cy.get(this.itemsRows)
      .contains(itemName)
      .should((!exist ? "not." : "") + "exist");

    return this;
  }

  goToItemDetails(itemName: string) {
    cy.get(this.itemsRows).contains(itemName).click();

    return this;
  }

  deleteItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickDetailMenu("Delete");

    return this;
  }

  exportItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickDetailMenu("Export");

    return this;
  }
}
