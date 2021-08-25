export default class EventsPage {
  searchEventDrpDwn = ".pf-c-dropdown__toggle";
  searchEventDrpDwnBtn =
    ".keycloak__user_events_search_selector_dropdown__toggle";
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
  eventsPageTitle = ".pf-c-title";

  shouldDisplay() {
    cy.get(this.searchEventDrpDwn).should("exist");
  }

  shouldHaveFormFields() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.get(this.searchForm).contains("User ID");
    cy.get(this.searchForm).contains("Event type");
    cy.get(this.searchForm).contains("Client");
    cy.get(this.searchForm).contains("Date(from)");
    cy.get(this.searchForm).contains("Date(to)");
    cy.get(this.searchForm).contains("Search events");
  }

  shouldHaveEventTypeOptions() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.get(this.eventTypeList).should("exist");
  }

  shouldHaveSearchBtnDisabled() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.getId(this.searchEventsBtn).should("have.attr", "disabled");
  }

  shouldHaveSearchBtnEnabled() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.getId(this.userIdInputFld).type("11111");
    cy.getId(this.searchEventsBtn).should("not.have.attr", "disabled");
  }

  shouldDoSearchAndRemoveChips() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.get(this.eventTypeInputFld).type("LOGIN");
    cy.get(this.eventTypeOption).contains("LOGIN").click();

    cy.intercept("/auth/admin/realms/master/events*").as("eventsFetch");
    cy.getId(this.searchEventsBtn).click();
    cy.wait("@eventsFetch");

    cy.get("table").contains("td", "LOGIN");
    cy.get("table").should("not.have.text", "CODE_TO_TOKEN");
    cy.get("table").should("not.have.text", "CODE_TO_TOKEN_ERROR");
    cy.get("table").should("not.have.text", "LOGIN_ERROR");
    cy.get("table").should("not.have.text", "LOGOUT");

    cy.get("[id^=remove_pf]").click();

    cy.get(this.searchEventDrpDwnBtn).click();
    cy.getId(this.userIdInputFld).type("11111");
    cy.getId(this.searchEventsBtn).click();
    cy.get(this.eventsPageTitle).contains("No events logged");
    cy.get("[id^=remove_group]").click();
    cy.get("table").contains("LOGIN");
  }

  shouldDoNoResultsSearch() {
    cy.get(this.searchEventDrpDwnBtn).click();
    cy.getId(this.userIdInputFld).type("test");
    cy.getId(this.searchEventsBtn).click();
    cy.get(this.eventsPageTitle).contains("No events logged");
  }
}
