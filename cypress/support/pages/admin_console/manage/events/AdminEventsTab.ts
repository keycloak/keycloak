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

  shouldHaveFormFields() {
    cy.getId(this.searchAdminEventDrpDwnBtn).click();
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
    cy.getId(this.searchAdminEventDrpDwnBtn).click();
    cy.getId(this.searchEventsBtn).should("have.attr", "disabled");
  }

  shouldDoAdminEventsSearchAndRemoveChips() {
    cy.getId(this.searchAdminEventDrpDwnBtn).click();
    cy.getId(this.resourcePathInputFld).type("events/config");

    cy.intercept("/auth/admin/realms/master/admin-events*").as("eventsFetch");
    cy.getId(this.searchEventsBtn).click();
    cy.wait("@eventsFetch");

    cy.get("table").contains("td", "events/config").should("be.visible");

    cy.get("[id^=remove_group]").click();
    cy.wait("@eventsFetch");
    cy.get("table").should("be.visible").contains("td", "UPDATE");
  }

  shouldHaveSearchBtnEnabled() {
    cy.getId(this.searchAdminEventDrpDwnBtn).click();
    cy.getId(this.ipAddressInputFld).type("11111");
    cy.getId(this.searchEventsBtn).should("not.have.attr", "disabled");
  }

  shouldDoNoResultsSearch() {
    cy.getId(this.searchAdminEventDrpDwnBtn).click();
    cy.getId(this.resourcePathInputFld).type("events/test");
    cy.getId(this.searchEventsBtn).click();
    cy.get(this.adminEventsTabTitle).contains("No events logged");
  }
}
