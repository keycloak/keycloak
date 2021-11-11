export default class UserEventsTab {
  searchEventDrpDwn = ".pf-c-dropdown__toggle";
  searchEventDrpDwnBtn = "userEventsSearchSelectorToggle";
  searchForm = ".pf-c-dropdown__menu";
  userIdInputFld = "userId-searchField";
  eventTypeDrpDwnFld = "event-type-searchField";
  clientInputFld = "client-searchField";
  dateFromInputFld = "dateFrom-searchField";
  dateToInputFld = "dateTo-searchField";
  searchEventsBtn = "search-events-btn";
  eventTypeList = ".pf-c-form-control";
  eventTypeOption = ".pf-c-select__menu-item";
  eventTypeInputFld = ".pf-c-form-control.pf-c-select__toggle-typeahead";
  eventTypeBtn = ".pf-c-button.pf-c-select__toggle-button.pf-m-plain";
  userEventsTabTitle = ".pf-c-title";

  shouldDisplay() {
    cy.get(this.searchEventDrpDwn).should("exist");
  }

  shouldHaveFormFields() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.get(this.searchForm).contains("User ID");
    cy.get(this.searchForm).contains("Event type");
    cy.get(this.searchForm).contains("Client");
    cy.get(this.searchForm).contains("Date(from)");
    cy.get(this.searchForm).contains("Date(to)");
    cy.get(this.searchForm).contains("Search events");
  }

  shouldHaveEventTypeOptions() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.get(this.eventTypeList).should("exist");
  }

  shouldHaveSearchBtnDisabled() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.findByTestId(this.searchEventsBtn).should("have.attr", "disabled");
  }

  shouldDoSearchAndRemoveChips() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.get(this.eventTypeInputFld).type("LOGOUT");
    cy.get(this.eventTypeOption).contains("LOGOUT").click();

    cy.intercept("/auth/admin/realms/master/events*").as("eventsFetch");
    cy.findByTestId(this.searchEventsBtn).click();
    cy.wait("@eventsFetch");

    cy.get("table").contains("td", "LOGOUT");
    cy.get("table").should("not.have.text", "CODE_TO_TOKEN");
    cy.get("table").should("not.have.text", "CODE_TO_TOKEN_ERROR");
    cy.get("table").should("not.have.text", "LOGIN_ERROR");
    cy.get("table").should("not.have.text", "LOGOUT");

    cy.get("[id^=remove_group]").click();
    cy.wait("@eventsFetch");
    cy.get("table").should("be.visible").contains("td", "LOGOUT");
  }

  shouldHaveSearchBtnEnabled() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.findByTestId(this.userIdInputFld).type("11111");
    cy.findByTestId(this.searchEventsBtn).should("not.have.attr", "disabled");
  }

  shouldDoNoResultsSearch() {
    cy.findByTestId(this.searchEventDrpDwnBtn).click();
    cy.findByTestId(this.userIdInputFld).type("test");
    cy.findByTestId(this.searchEventsBtn).click();
    cy.get(this.userEventsTabTitle).contains("No events logged");
  }
}
