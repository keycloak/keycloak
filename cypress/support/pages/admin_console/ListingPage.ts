export default class ListingPage {
  searchInput: string;
  itemsRows: string;
  itemRowDrpDwn: string;
  exportBtn: string;
  deleteBtn: string;
  searchBtn: string;
  createBtn: string;
  importBtn: string;
  initialAccessTokenTab = "initialAccessToken";

  constructor() {
    this.searchInput = '.pf-c-toolbar__item [type="search"]';
    this.itemsRows = ".pf-c-page__main-section table";
    this.itemRowDrpDwn = ".pf-c-dropdown > button";
    this.exportBtn = '[role="menuitem"]:nth-child(1)';
    this.deleteBtn = '[role="menuitem"]:nth-child(2)';
    this.searchBtn =
      ".pf-c-page__main .pf-c-toolbar__content-section button.pf-m-control";
    this.createBtn =
      ".pf-c-page__main .pf-c-toolbar__content-section button.pf-m-primary";
    this.importBtn =
      ".pf-c-page__main .pf-c-toolbar__content-section button.pf-m-link";
  }

  goToCreateItem() {
    cy.get(this.createBtn).click();

    return this;
  }

  goToImportItem() {
    cy.get(this.importBtn).click();

    return this;
  }

  goToInitialAccessTokenTab() {
    cy.getId(this.initialAccessTokenTab).click();
    return this;
  }

  searchItem(searchValue: string, wait = true) {
    if (wait) {
      const searchUrl = `/auth/admin/realms/master/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }
    cy.get(this.searchInput).type(searchValue);
    cy.get(this.searchBtn).click();
    if (wait) {
      cy.wait(["@search"]);
    }
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
    cy.get(this.itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.itemRowDrpDwn)
      .click();
    cy.get(this.itemsRows).contains("Delete").click();

    return this;
  }

  exportItem(itemName: string) {
    cy.get(this.itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.itemRowDrpDwn)
      .click();
    cy.get(this.itemsRows).contains("Export").click();

    return this;
  }
}
