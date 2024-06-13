import CommonElements from "../../CommonElements";
import type { Filter, FilterAssignedType } from "../ListingPage";

export default class TableToolbar extends CommonElements {
  #searchBtn: string;
  #searchInput: string;
  #changeTypeBtn: string;
  #nextPageBtn: string;
  #previousPageBtn: string;
  #searchTypeDropdownBtn: string;
  #actionToggleBtn: string;

  constructor() {
    super(".pf-v5-c-toolbar:visible");
    this.#searchBtn =
      this.parentSelector + "button[aria-label='Search']:visible";
    this.#searchInput =
      this.parentSelector + ".pf-v5-c-text-input-group__text-input:visible";
    this.#changeTypeBtn = this.parentSelector + "#change-type-dropdown";
    this.#nextPageBtn = this.parentSelector + "button[data-action=next]";
    this.#previousPageBtn =
      this.parentSelector + "button[data-action=previous]";
    this.#searchTypeDropdownBtn = "[data-testid='clientScopeSearchType']";
    this.#actionToggleBtn = "[data-testid='kebab']";
  }

  clickNextPageButton(isUpperButton = true) {
    if (isUpperButton) {
      cy.get(this.#nextPageBtn).first().click();
    } else {
      cy.get(this.#nextPageBtn).last().click();
    }
    return this;
  }

  clickPreviousPageButton(isUpperButton = true) {
    if (isUpperButton) {
      cy.get(this.#previousPageBtn).first().click();
    } else {
      cy.get(this.#previousPageBtn).last().click();
    }
    return this;
  }

  clickActionItem(actionItemName: string) {
    cy.get(this.#actionToggleBtn).click();
    cy.get(this.dropdownMenuItem).contains(actionItemName).click();
    return this;
  }

  clickSearchButton() {
    cy.get(this.#searchBtn).click({ force: true });
    return this;
  }

  clickPrimaryButton(itemName?: string) {
    if (itemName == undefined) {
      cy.get(this.primaryBtn).click();
    } else {
      cy.get(this.primaryBtn).contains(itemName).click();
    }
    return this;
  }

  clickDropdownMenuItem(itemName: string) {
    cy.get(this.dropdownMenuItem).contains(itemName).click();
    return this;
  }

  searchItem(searchValue: string, wait = true) {
    if (wait) {
      const searchUrl = `/admin/realms/*/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }
    cy.get(this.#searchInput).clear();
    if (searchValue) {
      cy.get(this.#searchInput).type(searchValue);
      this.clickSearchButton();
    } else {
      // TODO: Remove else and move clickSearchButton outside of the if
      cy.get(this.#searchInput).type("{enter}");
    }
    if (wait) {
      cy.wait(["@search"]);
    }
    return this;
  }

  selectSearchType(existingName: Filter, itemName: Filter) {
    cy.contains("button", existingName).click();
    cy.get(this.dropdownMenuItem).contains(itemName).click();
    return this;
  }

  selectSecondarySearchType(itemName: FilterAssignedType) {
    cy.get(this.#searchTypeDropdownBtn).click();
    cy.get(this.dropdownSelectToggleItem).contains(itemName).click();
    return this;
  }

  changeTypeTo(itemName: FilterAssignedType) {
    cy.get(this.#changeTypeBtn).click();
    cy.get(this.dropdownSelectToggleItem).contains(itemName).click();
    return this;
  }

  addClientScope() {
    cy.get(this.primaryBtn).contains("Add client scope").click();
    return this;
  }

  createClient() {
    cy.get(this.primaryBtn).contains("Create client").click();
    return this;
  }

  addMapper() {
    cy.get(this.primaryBtn).contains("Add mapper").click();
    return this;
  }

  checkActionItemIsEnabled(actionItemName: string, enabled: boolean) {
    cy.get(this.#actionToggleBtn).click();
    cy.get(this.dropdownMenuItem)
      .contains(actionItemName)
      .should((!enabled ? "not." : "") + "be.disabled");
    cy.get(this.#actionToggleBtn).click();
    return this;
  }
}
