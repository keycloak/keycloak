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
  #tableToolbar = "section .pf-v5-c-toolbar";
  #itemsRows = "table:visible";
  #deleteUserButton = "delete-user-btn";
  #emptyListImg = '[role="tabpanel"]:not([hidden]) [data-testid="empty-state"]';
  #emptyState = "empty-state";
  #itemRowDrpDwn = ".pf-v5-c-table__action button";
  #itemRowSelect = "[data-testid='cell-dropdown']";
  #itemRowSelectItem = ".pf-v5-c-menu__item";
  #itemCheckbox = ".pf-v5-c-table__check";
  public exportBtn = '[role="menuitem"]:nth-child(1)';
  public deleteBtn = '[role="menuitem"]:nth-child(2)';
  #searchBtn =
    ".pf-v5-c-page__main .pf-v5-c-toolbar__content-section button.pf-m-control:visible";
  #listHeaderPrimaryBtn =
    ".pf-v5-c-page__main .pf-v5-c-toolbar__content-section .pf-m-primary:visible";
  #listHeaderSecondaryBtn =
    ".pf-v5-c-page__main .pf-v5-c-toolbar__content-section .pf-m-link";
  #previousPageBtn =
    ".pf-v5-c-pagination:not([class*=pf-m-bottom]) button[data-action=previous]";
  #nextPageBtn =
    ".pf-v5-c-pagination:not([class*=pf-m-bottom]) button[data-action=next]";
  public tableRowItem = "tbody tr[data-ouia-component-type]:visible";
  #table = "table[aria-label]";
  #filterSessionDropdownButton = ".pf-v5-c-select button:nth-child(1)";
  #searchTypeButton = "[data-testid='clientScopeSearch']";
  #filterDropdownButton = "[data-testid='clientScopeSearchType']";
  #protocolFilterDropdownButton = "[data-testid='clientScopeSearchProtocol']";
  #kebabMenu = "[data-testid='kebab']";
  #dropdownItem = ".pf-v5-c-menu__list-item";
  #toolbarChangeType = "#change-type-dropdown";
  #tableNameColumnPrefix = "name-column-";
  #rowGroup = "table:visible tbody[role='rowgroup']";
  #tableHeaderCheckboxItemAllRows = "input[aria-label='Select all rows']";
  #searchBtnInModal =
    ".pf-v5-c-modal-box .pf-v5-c-toolbar__content-section button.pf-m-control:visible";
  #menuContent = ".pf-v5-c-menu__content";
  #menuItemText = ".pf-v5-c-menu__item-text";

  #getSearchInput() {
    return cy.findAllByTestId("table-search-input").last().find("input");
  }

  showPreviousPageTableItems() {
    cy.get(this.#previousPageBtn).first().click();

    return this;
  }

  showNextPageTableItems() {
    cy.get("body").then(($body) => {
      if (!$body.find('[data-testid="' + this.#nextPageBtn + '"]').length) {
        cy.get(this.#nextPageBtn).scrollIntoView();
        cy.get(this.#nextPageBtn).click();
      }
    });

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

  searchItem(searchValue: string, wait = true, realm = "master") {
    if (wait) {
      const searchUrl = `/admin/realms/${realm}/**/*${searchValue}*`;
      cy.intercept(searchUrl).as("search");
    }

    this.#getSearchInput().click({ force: true });
    this.#getSearchInput().clear();
    if (searchValue) {
      this.#getSearchInput().type(searchValue);
      cy.get(this.#searchBtn).click({ force: true });
    } else {
      // TODO: Remove else and move clickSearchButton outside of the if
      this.#getSearchInput().type("{enter}");
    }

    if (wait) {
      cy.wait(["@search"]);
    }

    return this;
  }

  searchItemInModal(searchValue: string) {
    this.#getSearchInput().click({ force: true });
    this.#getSearchInput().clear();
    if (searchValue) {
      this.#getSearchInput().type(searchValue);
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
    cy.get(this.#tableToolbar).find(this.#kebabMenu).click();

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

  clickMenuDelete() {
    cy.get(this.#menuContent)
      .find(this.#menuItemText)
      .contains("Delete")
      .click({ force: true });
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
    cy.get(this.#itemsRows).contains(itemName).click({ force: true });

    return this;
  }

  checkEmptyList() {
    cy.get(this.#emptyListImg).should("be.visible");

    return this;
  }

  deleteItem(itemName: string) {
    this.clickRowDetails(itemName);
    this.clickMenuDelete();

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

  checkEmptySearch() {
    cy.get(this.tableRowItem).its("length").as("initialCount");
    this.searchItem("", false);
    cy.get(this.tableRowItem).its("length").as("finalCount");

    cy.get("@initialCount").then((initial) => {
      cy.get("@finalCount").then((final) => {
        expect(initial).to.eq(final);
      });
    });

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
    cy.get(this.#searchTypeButton).click();
    cy.get(this.#dropdownItem).contains(filter).click();

    return this;
  }

  selectSecondaryFilter(itemName: string) {
    cy.get(this.#filterDropdownButton).click();
    cy.get(this.#itemRowSelectItem).contains(itemName).click();

    return this;
  }

  selectSecondaryFilterAssignedType(assignedType: FilterAssignedType) {
    this.selectSecondaryFilter(assignedType);

    return this;
  }

  selectSecondaryFilterProtocol(protocol: FilterProtocol) {
    cy.get(this.#protocolFilterDropdownButton).click();
    cy.get(this.#itemRowSelectItem).contains(protocol).click();

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
      .find(this.#toolbarChangeType)
      .first()
      .click();

    cy.get(this.#itemsRows)
      .contains(itemName)
      .parentsUntil("tbody")
      .find(this.#toolbarChangeType)
      .contains(assignedType)
      .click();

    return this;
  }

  checkInSearchBarChangeTypeToButtonIsDisabled(disabled: boolean = true) {
    let condition = "be.disabled";
    if (!disabled) {
      condition = "be.enabled";
    }
    cy.get(this.#toolbarChangeType).first().should(condition);

    return this;
  }

  checkDropdownItemIsDisabled(itemName: string, disabled: boolean = true) {
    cy.get(this.#dropdownItem)
      .contains(itemName)
      .should((disabled ? "" : "not.") + "be.disabled");

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

  assertNoResults() {
    cy.findByTestId(this.#emptyState).should("exist");
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
      .find("[class='pf-v5-c-button pf-m-plain'][id*='expandable']")
      .click();
    return this;
  }

  collapseRow(index = 0) {
    this.#getRowGroup(index)
      .find(
        "[class='pf-v5-c-button pf-m-plain pf-m-expanded'][id*='expandable']",
      )
      .click();
    return this;
  }

  assertExpandedRowContainText(index = 0, text: string) {
    this.#getRowGroup(index)
      .find("tr[class='pf-v5-c-table__expandable-row pf-m-expanded']")
      .should("contain.text", text);
    return this;
  }

  assertRowIsExpanded(index = 0, isExpanded: boolean) {
    this.#getRowGroup(index)
      .find(
        "[class='pf-v5-c-button pf-m-plain pf-m-expanded'][id*='expandable']",
      )
      .should((!isExpanded ? "not." : "") + "exist");
    return this;
  }
}
