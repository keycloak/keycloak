import CommonElements from "../CommonElements";

export enum Filter {
  Name = "Name",
  AssignedType = "Assigned type",
  Protocol = "Protocol",
}

export enum FilterAssignedType {
  AllTypes = "All types",
  Default = "Default",
  Optional = "Optional",
  None = "None",
}

export enum FilterProtocol {
  All = "All",
  SAML = "SAML",
  OpenID = "OpenID Connect",
}

export enum FilterSession {
  AllSessionTypes = "All session types",
  RegularSSO = "Regular SSO",
  Offline = "Offline",
  DirectGrant = "Direct grant",
  ServiceAccount = "Service account",
}

export default class ListingPage extends CommonElements {
  #searchInput =
    ".pf-c-toolbar__item .pf-c-text-input-group__text-input:visible";
  #tableToolbar = ".pf-c-toolbar";
  #itemsRows = "table:visible";
  #deleteUserButton = "delete-user-btn";
  #emptyListImg = '[role="tabpanel"]:not([hidden]) [data-testid="empty-state"]';
  public emptyState = "empty-state";
  #itemRowDrpDwn = ".pf-c-dropdown__toggle";
  #itemRowSelect = ".pf-c-select__toggle:nth-child(1)";
  #itemRowSelectItem = ".pf-c-select__menu-item";
  #itemCheckbox = ".pf-c-table__check";
  public exportBtn = '[role="menuitem"]:nth-child(1)';
  public deleteBtn = '[role="menuitem"]:nth-child(2)';
  #searchBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section button.pf-m-control:visible";
  #listHeaderPrimaryBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section .pf-m-primary:visible";
  #listHeaderSecondaryBtn =
    ".pf-c-page__main .pf-c-toolbar__content-section .pf-m-link";
  #previousPageBtn =
    "div[class=pf-c-pagination__nav-control] button[data-action=previous]:visible";
  #nextPageBtn =
    "div[class=pf-c-pagination__nav-control] button[data-action=next]:visible";
  public tableRowItem = "tbody tr[data-ouia-component-type]:visible";
  #table = "table[aria-label]";
  #filterSessionDropdownButton = ".pf-c-select button:nth-child(1)";
  #filterDropdownButton = "[class*='searchtype'] button";
  #dropdownItem = ".pf-c-dropdown__menu-item";
  #changeTypeToButton = ".pf-c-select__toggle";
  #toolbarChangeType = "#change-type-dropdown";
  #tableNameColumnPrefix = "name-column-";
  #rowGroup = "table:visible tbody[role='rowgroup']";
  #tableHeaderCheckboxItemAllRows = "input[aria-label='Select all rows']";

  #searchBtnInModal =
    ".pf-c-modal-box .pf-c-toolbar__content-section button.pf-m-control:visible";

  showPreviousPageTableItems() {
    cy.get(this.#previousPageBtn).first().click();

    return this;
  }

  showNextPageTableItems() {
    cy.get(this.#nextPageBtn).first().click();

    return this;
  }

  goToCreateItem() {
    cy.get(this.#listHeaderPrimaryBtn).click();

    return this;
  }

  goToImportItem() {
    cy.get(this.#listHeaderSecondaryBtn).click();

    return this;
  }

  searchItem(searchValue: string, wait = true) {
    if (wait) {
      const searchUrl = `/admin/realms/master/**/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }

    cy.get(this.#searchInput).clear();
    if (searchValue) {
      cy.get(this.#searchInput).type(searchValue);
      cy.get(this.#searchBtn).click({ force: true });
    } else {
      // TODO: Remove else and move clickSearchButton outside of the if
      cy.get(this.#searchInput).type("{enter}");
    }

    if (wait) {
      cy.wait(["@search"]);
    }

    return this;
  }

  searchItemInModal(searchValue: string) {
    cy.get(this.#searchInput).clear();
    if (searchValue) {
      cy.get(this.#searchInput).type(searchValue);
    }
    cy.get(this.#searchBtnInModal).click({ force: true });
  }

  checkTableLength(length: number, identifier: string) {
    cy.get("table")
      .should("have.class", identifier)
      .get("tbody")
      .children()
      .should("have.length", length);
  }

  clickSearchBarActionButton() {
    cy.get(this.#tableToolbar).find(this.#itemRowDrpDwn).last().click();

    return this;
  }

  clickSearchBarActionItem(itemName: string) {
    cy.get(this.#tableToolbar)
      .find(this.#dropdownItem)
      .contains(itemName)
      .click();

    return this;
  }

  clickRowDetails(itemName: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#itemRowDrpDwn)
      .click({ force: true });
    return this;
  }

  markItemRow(itemName: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find('input[name*="checkrow"]')
      .click();
    return this;
  }

  removeMarkedItems(name: string = "Remove") {
    cy.get(this.#listHeaderSecondaryBtn).contains(name).click();
    return this;
  }

  checkRowColumnValue(itemName: string, column: number, value: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find("td:nth-child(" + column + ")")
      .should("have.text", value);
    return this;
  }

  clickDetailMenu(name: string) {
    cy.get(this.#itemsRows).contains(name).click();
    return this;
  }

  clickItemCheckbox(itemName: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#itemCheckbox)
      .click();
    return this;
  }

  clickTableHeaderItemCheckboxAllRows() {
    cy.get(this.#tableHeaderCheckboxItemAllRows).click();
    return this;
  }

  clickRowSelectButton(itemName: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#itemRowSelect)
      .click();
    return this;
  }

  clickPrimaryButton() {
    cy.get(this.#listHeaderPrimaryBtn).click();

    return this;
  }

  clickRowSelectItem(rowItemName: string, selectItemName: string) {
    this.clickRowSelectButton(rowItemName);
    cy.get(this.#itemRowSelectItem).contains(selectItemName).click();

    return this;
  }

  itemExist(itemName: string, exist = true) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .should((!exist ? "not." : "") + "exist");

    return this;
  }

  goToItemDetails(itemName: string) {
    cy.get(this.#itemsRows).contains(itemName).click();

    return this;
  }

  checkEmptyList() {
    cy.get(this.#emptyListImg).should("be.visible");

    return this;
  }

  deleteItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickDetailMenu("Delete");

    return this;
  }

  removeItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickDetailMenu("Unassign");

    return this;
  }

  deleteItemFromSearchBar(itemName: string) {
    this.markItemRow(itemName);
    cy.findByTestId(this.#deleteUserButton).click();

    return this;
  }

  exportItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickDetailMenu("Export");

    return this;
  }

  itemsEqualTo(amount: number) {
    cy.get(this.tableRowItem).its("length").should("be.eq", amount);

    return this;
  }

  itemsGreaterThan(amount: number) {
    cy.get(this.tableRowItem).its("length").should("be.gt", amount);

    return this;
  }

  itemContainValue(itemName: string, colIndex: number, value: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find("td")
      .eq(colIndex)
      .should("contain", value);

    return this;
  }

  selectFilter(filter: Filter) {
    cy.get(this.#filterDropdownButton).first().click();
    cy.get(this.#dropdownItem).contains(filter).click();

    return this;
  }

  selectSecondaryFilter(itemName: string) {
    cy.get(this.#filterDropdownButton).last().click();
    cy.get(this.#itemRowSelectItem).contains(itemName).click();

    return this;
  }

  selectSecondaryFilterAssignedType(assignedType: FilterAssignedType) {
    this.selectSecondaryFilter(assignedType);

    return this;
  }

  selectSecondaryFilterProtocol(protocol: FilterProtocol) {
    this.selectSecondaryFilter(protocol);

    return this;
  }

  selectSecondaryFilterSession(sessionName: FilterSession) {
    cy.get(this.#filterSessionDropdownButton).click();
    cy.get(this.#itemRowSelectItem).contains(sessionName);

    return this;
  }

  changeTypeToOfSelectedItems(assignedType: FilterAssignedType) {
    cy.intercept("/admin/realms/master/client-scopes").as("load");
    cy.get(this.#toolbarChangeType).click();
    cy.get(this.#itemRowSelectItem).contains(assignedType).click();
    cy.wait("@load");
    return this;
  }

  changeTypeToOfItem(assignedType: FilterAssignedType, itemName: string) {
    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#changeTypeToButton)
      .first()
      .click();

    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#changeTypeToButton)
      .contains(assignedType)
      .click();

    return this;
  }

  checkInSearchBarChangeTypeToButtonIsDisabled(disabled: boolean = true) {
    let condition = "be.disabled";
    if (!disabled) {
      condition = "be.enabled";
    }
    cy.get(this.#changeTypeToButton).first().should(condition);

    return this;
  }

  checkDropdownItemIsDisabled(itemName: string, disabled: boolean = true) {
    cy.get(this.#dropdownItem)
      .contains(itemName)
      .should("have.attr", "aria-disabled", String(disabled));

    return this;
  }

  checkTableExists(exists: boolean = true) {
    let condition = "be.visible";
    if (!exists) {
      condition = "not.be.visible";
    }
    cy.get(this.#table).should(condition);

    return this;
  }

  #getResourceLink(name: string) {
    return cy.findByTestId(this.#tableNameColumnPrefix + name);
  }

  goToResourceDetails(name: string) {
    this.#getResourceLink(name).click();
    return this;
  }

  assertDefaultResource() {
    this.assertResource("Default Resource");
    return this;
  }

  assertResource(name: string) {
    this.#getResourceLink(name).should("exist");
    return this;
  }

  #getRowGroup(index = 0) {
    return cy.get(this.#rowGroup).eq(index);
  }

  expandRow(index = 0) {
    this.#getRowGroup(index)
      .find("[class='pf-c-button pf-m-plain'][id*='expandable']")
      .click();
    return this;
  }

  collapseRow(index = 0) {
    this.#getRowGroup(index)
      .find("[class='pf-c-button pf-m-plain pf-m-expanded'][id*='expandable']")
      .click();
    return this;
  }

  assertExpandedRowContainText(index = 0, text: string) {
    this.#getRowGroup(index)
      .find("tr[class='pf-c-table__expandable-row pf-m-expanded']")
      .should("contain.text", text);
    return this;
  }

  assertRowIsExpanded(index = 0, isExpanded: boolean) {
    this.#getRowGroup(index)
      .find("[class='pf-c-button pf-m-plain pf-m-expanded'][id*='expandable']")
      .should((!isExpanded ? "not." : "") + "exist");
    return this;
  }
}
