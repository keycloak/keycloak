import ModalUtils from "../../../../../util/ModalUtils";
import EmptyStatePage from "../../../components/EmptyStatePage";
import PageObject from "../../../components/PageObject";

enum AdminEventsTabSearchFormFieldsLabel {
  ResourceTypes = "Resource types",
  OperationTypes = "Operation types",
  ResourcePath = "Resource path",
  Realm = "Realm",
  Client = "Client",
  User = "User",
  IpAddress = "IP address",
  DateFrom = "Date(from)",
  DateTo = "Date(to)",
}

export class AdminEventSearchData {
  resourceTypes?: string[];
  operationTypes?: string[];
  resourcePath?: string;
  realm?: string;
  client?: string;
  user?: string;
  ipAddress?: string;
  dateFrom?: string;
  dateTo?: string;
}

const emptyStatePage = new EmptyStatePage();
const modalUtils = new ModalUtils();

export default class AdminEventsTab extends PageObject {
  #searchAdminEventDrpDwnBtn = "dropdown-panel-btn";
  #searchEventsBtn = "search-events-btn";
  #operationTypesInputFld =
    ".pf-v5-c-form-control.pf-v5-c-select__toggle-typeahead";
  #authAttrDataRow = 'tbody > tr > [data-label="Attribute"]';
  #authValDataRow = 'tbody > tr > [data-label="Value"]';
  #refreshBtn = "refresh-btn";
  #resourcePathInput = "#kc-resourcePath";
  #realmInput = "#kc-realm";
  #clientInput = "#kc-client";
  #userInput = "#kc-user";
  #ipAddressInput = "#kc-ipAddress";
  #dateFromInput = "#kc-dateFrom";
  #dateToInput = "#kc-dateTo";

  public refresh() {
    cy.findByTestId(this.#refreshBtn).click();
    return this;
  }

  public openSearchAdminEventDropdownMenu() {
    super.openDropdownMenu(
      "",
      cy.findByTestId(this.#searchAdminEventDrpDwnBtn),
    );
    return this;
  }

  public assertAdminSearchDropdownMenuHasLabels() {
    super.assertDropdownMenuHasLabels(
      Object.values(AdminEventsTabSearchFormFieldsLabel),
    );
    return this;
  }

  public openResourceTypesSelectMenu() {
    cy.get(this.#operationTypesInputFld).first().click();
    return this;
  }

  public closeResourceTypesSelectMenu() {
    cy.get(this.#operationTypesInputFld).first().click();
    return this;
  }

  public openOperationTypesSelectMenu() {
    cy.get(this.#operationTypesInputFld).last().click();
    return this;
  }

  public closeOperationTypesSelectMenu() {
    cy.get(this.#operationTypesInputFld).last().click();
    return this;
  }

  public typeIpAddress(ipAddress: string) {
    cy.get(this.#ipAddressInput).type(ipAddress);
    return this;
  }

  searchAdminEvent(searchData: AdminEventSearchData) {
    this.openSearchAdminEventDropdownMenu();
    if (searchData.resourceTypes) {
      this.openResourceTypesSelectMenu();
      searchData.resourceTypes.forEach((value) => {
        super.clickSelectMenuItem(value);
      });
      this.closeResourceTypesSelectMenu();
    }
    if (searchData.operationTypes) {
      this.openOperationTypesSelectMenu();
      searchData.operationTypes.forEach((value) => {
        super.clickSelectMenuItem(value);
      });
      this.closeOperationTypesSelectMenu();
    }
    if (searchData.resourcePath) {
      cy.get(this.#resourcePathInput).type(searchData.resourcePath);
    }
    if (searchData.realm) {
      cy.get(this.#realmInput).type(searchData.realm);
    }
    if (searchData.client) {
      cy.get(this.#clientInput).type(searchData.client);
    }
    if (searchData.user) {
      cy.get(this.#userInput).type(searchData.user);
    }
    if (searchData.ipAddress) {
      cy.get(this.#ipAddressInput).type(searchData.ipAddress);
    }
    if (searchData.dateFrom) {
      cy.get(this.#dateFromInput).type(searchData.dateFrom);
    }
    if (searchData.dateTo) {
      cy.get(this.#dateToInput).type(searchData.dateTo);
    }
    cy.findByTestId(this.#searchEventsBtn).click();
    return this;
  }

  public assertSearchAdminBtnEnabled(disabled: boolean) {
    super.assertIsEnabled(cy.findByTestId(this.#searchEventsBtn), disabled);
    return this;
  }

  public searchAdminEventByResourceTypes(resourceTypes: string[]) {
    const searchData = new AdminEventSearchData();
    searchData.resourceTypes = resourceTypes;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByOperationTypes(operationTypes: string[]) {
    const searchData = new AdminEventSearchData();
    searchData.operationTypes = operationTypes;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByResourcePath(resourcePath: string) {
    const searchData = new AdminEventSearchData();
    searchData.resourcePath = resourcePath;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByRealm(realm: string) {
    const searchData = new AdminEventSearchData();
    searchData.realm = realm;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByClient(client: string) {
    const searchData = new AdminEventSearchData();
    searchData.client = client;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByUser(user: string) {
    const searchData = new AdminEventSearchData();
    searchData.user = user;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByIpAddress(ipAddress: string) {
    const searchData = new AdminEventSearchData();
    searchData.ipAddress = ipAddress;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByDateFrom(dateFrom: string) {
    const searchData = new AdminEventSearchData();
    searchData.dateFrom = dateFrom;
    this.searchAdminEvent(searchData);
    return this;
  }

  public searchAdminEventByDateTo(dateTo: string) {
    const searchData = new AdminEventSearchData();
    searchData.dateTo = dateTo;
    this.searchAdminEvent(searchData);
    return this;
  }

  public removeResourcePathChipGroup() {
    super.removeChipGroup(AdminEventsTabSearchFormFieldsLabel.ResourcePath);
    return this;
  }

  public assertResourceTypesChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.ResourceTypes,
      exist,
    );
    return this;
  }

  public assertOperationTypesChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.OperationTypes,
      exist,
    );
    return this;
  }

  public assertResourcePathChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.ResourcePath,
      exist,
    );
    return this;
  }

  public assertResourcePathChipGroupItemExist(
    itemName: string,
    exist: boolean,
  ) {
    super.assertChipGroupItemExist(
      AdminEventsTabSearchFormFieldsLabel.ResourcePath,
      itemName,
      exist,
    );
    return this;
  }

  public assertRealmChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.Realm,
      exist,
    );
    return this;
  }

  public assertClientChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.Client,
      exist,
    );
    return this;
  }

  public assertUserChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(AdminEventsTabSearchFormFieldsLabel.User, exist);
    return this;
  }

  public assertIpAddressChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.IpAddress,
      exist,
    );
    return this;
  }

  public assertDateFromChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.DateFrom,
      exist,
    );
    return this;
  }

  public assertDateToChipGroupExist(exist: boolean) {
    super.assertChipGroupExist(
      AdminEventsTabSearchFormFieldsLabel.DateTo,
      exist,
    );
    return this;
  }

  public assertAuthDialogIsNotEmpty() {
    modalUtils
      .checkModalTitle("Auth")
      .assertModalMessageContainText("Realm")
      .assertModalMessageContainText("Client")
      .assertModalMessageContainText("User")
      .assertModalMessageContainText("IP address")
      .assertModalHasElement(this.#authAttrDataRow, true)
      .assertModalHasElement(this.#authValDataRow, true)
      .closeModal();
    return this;
  }

  public assertRepresentationDialogIsNotEmpty() {
    modalUtils.checkModalTitle("Representation").closeModal();
    return this;
  }

  public assertNoSearchResultsExist(exist: boolean) {
    emptyStatePage.checkIfExists(exist);
    return this;
  }
}
