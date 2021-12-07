export default class AdminEventsTab {
  searchAdminEventDrpDwn = ".pf-c-dropdown__toggle";
  searchAdminEventDrpDwnBtn = "adminEventsSearchSelectorToggle";
  searchForm = ".pf-c-dropdown__menu";
  resourceTypesDrpDwnFld = "resource-types-searchField";
  operationTypesDrpDwnFld = "operation-types-searchField";
  resourcePathInputFld = "resourcePath-searchField";
  userInputFld = "user-searchField";
  realmInputFld = "realm-searchField";
  ipAddressInputFld = "ipAddress-searchField";
  dateFromInputFld = "dateFrom-searchField";
  dateToInputFld = "dateTo-searchField";
  searchEventsBtn = "search-events-btn";
  operationTypesList = ".pf-c-form-control";
  operationTypesOption = ".pf-c-select__menu-item";
  operationTypesInputFld = ".pf-c-form-control.pf-c-select__toggle-typeahead";
  operationTypesBtn = ".pf-c-button.pf-c-select__toggle-button.pf-m-plain";
  adminEventsTabTitle = ".pf-c-title";
  moreBtn = ".pf-c-dropdown__toggle.pf-m-plain";
  moreDrpDwnItems = ".pf-c-dropdown__menu";
  dialogTitle = ".pf-c-modal-box__title-text";
  dialogClose = `[aria-label="Close"]`;
  authAttrDataRow = 'tbody > tr > [data-label="Attribute"]';
  authValDataRow = 'tbody > tr > [data-label="Value"]';

  shouldHaveFormFields() {
    cy.findByTestId(this.searchAdminEventDrpDwnBtn).click();
    cy.get(this.searchForm).contains("Resource types");
    cy.get(this.searchForm).contains("Operation types");
    cy.get(this.searchForm).contains("Resource path");
    cy.get(this.searchForm).contains("User");
    cy.get(this.searchForm).contains("Realm");
    cy.get(this.searchForm).contains("IP address");
    cy.get(this.searchForm).contains("Date(from)");
    cy.get(this.searchForm).contains("Date(to)");
    cy.get(this.searchForm).contains("Search admin events");
  }

  shouldHaveSearchBtnDisabled() {
    cy.findByTestId(this.searchAdminEventDrpDwnBtn).click();
    cy.findByTestId(this.searchEventsBtn).should("have.attr", "disabled");
  }

  shouldDoAdminEventsSearchAndRemoveChips() {
    cy.findByTestId(this.searchAdminEventDrpDwnBtn).click();
    cy.findByTestId(this.resourcePathInputFld).type("events/config");

    cy.intercept("/auth/admin/realms/master/admin-events*").as("eventsFetch");
    cy.findByTestId(this.searchEventsBtn).click();
    cy.wait("@eventsFetch");

    cy.get("table").contains("td", "events/config").should("be.visible");

    cy.get("[id^=remove_group]").click();
    cy.wait("@eventsFetch");
    cy.get("table").should("be.visible").contains("td", "UPDATE");
  }

  shouldHaveSearchBtnEnabled() {
    cy.findByTestId(this.searchAdminEventDrpDwnBtn).click();
    cy.findByTestId(this.ipAddressInputFld).type("11111");
    cy.findByTestId(this.searchEventsBtn).should("not.have.attr", "disabled");
  }

  shouldDoNoResultsSearch() {
    cy.findByTestId(this.searchAdminEventDrpDwnBtn).click();
    cy.findByTestId(this.resourcePathInputFld).type("events/test");
    cy.findByTestId(this.searchEventsBtn).click();
    cy.get(this.adminEventsTabTitle).contains("No search results");
  }

  shouldCheckAuthDialogOpensAndIsNotEmpty() {
    cy.get(this.moreBtn).last().click();
    cy.get(this.moreDrpDwnItems).contains("Auth").click();
    cy.get(this.dialogTitle).contains("Auth");
    cy.get(this.authAttrDataRow).contains("Realm");
    cy.get(this.authAttrDataRow).contains("Client");
    cy.get(this.authAttrDataRow).contains("User");
    cy.get(this.authAttrDataRow).contains("IP address");
    cy.get(this.authValDataRow).should("exist");
    cy.get(this.dialogClose).click();
  }

  shouldCheckRepDialogOpensAndIsNotEmpty() {
    cy.get(this.moreBtn).last().click();
    cy.get(this.moreDrpDwnItems).contains("Representation").click();
    cy.get(this.dialogTitle).contains("Representation");
    cy.get(this.dialogClose).click();
  }
}
