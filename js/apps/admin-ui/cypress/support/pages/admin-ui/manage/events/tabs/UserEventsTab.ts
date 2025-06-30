import EmptyStatePage from "../../../components/EmptyStatePage";
import PageObject from "../../../components/PageObject";

enum UserEventsTabSearchFormFieldsLabel {
  UserId = "User ID",
  EventType = "Event type",
  Client = "Client",
  DateFrom = "Date(from)",
  DateTo = "Date(to)",
  IpAddress = "IP Address",
}

export class UserEventSearchData {
  userId?: string;
  eventType?: string[];
  client?: string;
  dateFrom?: string;
  dateTo?: string;
  ipAddress?: string;
}

const emptyStatePage = new EmptyStatePage();

export default class UserEventsTab extends PageObject {
  #searchUserEventDrpDwnToggle = "dropdown-panel-btn";
  #searchUserIdInput = "#kc-userId";
  #searchEventTypeSelectToggle =
    ".pf-v5-c-select.keycloak__events_search__type_select";
  #searchClientInput = "#kc-client";
  #searchDateFromInput = "#kc-dateFrom";
  #searchDateToInput = "#kc-dateTo";
  #searchIpAddress = "#kc-ipAddress";
  #searchEventsBtn = "search-events-btn";
  #refreshBtn = "refresh-btn";

  public openSearchUserEventDropdownMenu() {
    super.openDropdownMenu(
      "",
      cy.findByTestId(this.#searchUserEventDrpDwnToggle),
    );
    return this;
  }

  public openEventTypeSelectMenu() {
    super.openSelectMenu("", cy.get(this.#searchEventTypeSelectToggle));
    return this;
  }

  public closeEventTypeSelectMenu() {
    super.closeSelectMenu("", cy.get(this.#searchEventTypeSelectToggle));
    return this;
  }

  public clickEventTypeSelectItem(itemName: string) {
    super.clickSelectMenuItem(itemName);
    return this;
  }

  public assertSearchEventBtnIsEnabled(enabled: boolean) {
    super.assertIsEnabled(cy.findByTestId(this.#searchEventsBtn), enabled);
    return this;
  }

  public assertUserSearchDropdownMenuHasLabels() {
    super.assertDropdownMenuHasLabels(
      Object.values(UserEventsTabSearchFormFieldsLabel),
    );
    return this;
  }

  public assertSearchUserEventDropdownMenuExist(exist: boolean) {
    super.assertExist(
      cy.findByTestId(this.#searchUserEventDrpDwnToggle),
      exist,
    );
    return this;
  }

  public refresh() {
    cy.findByTestId(this.#refreshBtn).click();
    return this;
  }

  public typeUserId(userId: string) {
    cy.get(this.#searchUserIdInput).type(userId);
    return this;
  }

  public typeIpAddress(ipAddress: string) {
    cy.get(this.#searchIpAddress).type(ipAddress);
    return this;
  }

  public searchUserEvent(searchData: UserEventSearchData) {
    this.openSearchUserEventDropdownMenu();
    if (searchData.userId) {
      this.typeUserId(searchData.userId);
    }
    if (searchData.eventType) {
      this.openEventTypeSelectMenu();
      searchData.eventType.forEach((value) => {
        super.clickSelectMenuItem(value);
      });
      this.closeEventTypeSelectMenu();
    }
    if (searchData.client) {
      cy.get(this.#searchClientInput).type(searchData.client);
    }
    if (searchData.dateFrom) {
      cy.get(this.#searchDateFromInput).type(searchData.dateFrom);
    }
    if (searchData.dateTo) {
      cy.get(this.#searchDateToInput).type(searchData.dateTo);
    }
    if (searchData.ipAddress) {
      cy.get(this.#searchIpAddress).type(searchData.ipAddress);
    }
    cy.findByTestId(this.#searchEventsBtn).click();
    return this;
  }

  public searchUserEventByUserId(userId: string) {
    const searchData = new UserEventSearchData();
    searchData.userId = userId;
    this.searchUserEvent(searchData);
    return this;
  }

  public searchUserEventByEventType(eventType: string[]) {
    const searchData = new UserEventSearchData();
    searchData.eventType = eventType;
    this.searchUserEvent(searchData);
    return this;
  }

  public searchUserEventByClient(client: string) {
    const searchData = new UserEventSearchData();
    searchData.client = client;
    this.searchUserEvent(searchData);
    return this;
  }

  public searchUserEventByDateFrom(dateFrom: string) {
    const searchData = new UserEventSearchData();
    searchData.dateFrom = dateFrom;
    this.searchUserEvent(searchData);
    return this;
  }

  public searchUserEventByDateTo(dateTo: string) {
    const searchData = new UserEventSearchData();
    searchData.dateTo = dateTo;
    this.searchUserEvent(searchData);
    return this;
  }

  public searchUserEventByIpAddress(ipAddress: string) {
    const searchData = new UserEventSearchData();
    searchData.ipAddress = ipAddress;
    this.searchUserEvent(searchData);
    return this;
  }

  public removeEventTypeChipGroupItem(itemName: string) {
    super.removeChipGroupItem(
      UserEventsTabSearchFormFieldsLabel.EventType,
      itemName,
    );
    return this;
  }

  public assertEventTypeChipGroupItemExist(itemName: string, exist: boolean) {
    super.assertChipGroupItemExist(
      UserEventsTabSearchFormFieldsLabel.EventType,
      itemName,
      exist,
    );
    return this;
  }

  public assertUserIdChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.UserId,
      exist,
    );
    return this;
  }

  public assertEventTypeChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.EventType,
      exist,
    );
    return this;
  }

  public assertClientChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.Client,
      exist,
    );
    return this;
  }

  public assertDateFromChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.DateFrom,
      exist,
    );
    return this;
  }

  public assertDateToChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.DateTo,
      exist,
    );
    return this;
  }

  public assertIpAddressChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      UserEventsTabSearchFormFieldsLabel.IpAddress,
      exist,
    );
    return this;
  }

  public assertNoSearchResultsExist(exist: boolean) {
    emptyStatePage.checkIfExists(exist);
    return this;
  }
}
