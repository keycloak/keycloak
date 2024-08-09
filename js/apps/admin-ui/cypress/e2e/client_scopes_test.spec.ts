import { v4 as uuid } from "uuid";
import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import ListingPage, {
  Filter,
  FilterAssignedType,
  FilterProtocol,
} from "../support/pages/admin-ui/ListingPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import CreateClientScopePage from "../support/pages/admin-ui/manage/client_scopes/CreateClientScopePage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RoleMappingTab from "../support/pages/admin-ui/manage/RoleMappingTab";
import ModalUtils from "../support/util/ModalUtils";
import adminClient from "../support/util/AdminClient";
import ClientScopeDetailsPage from "../support/pages/admin-ui/manage/client_scopes/client_scope_details/ClientScopeDetailsPage";
import CommonPage from "../support/pages/CommonPage";
import MappersTab from "../support/pages/admin-ui/manage/client_scopes/client_scope_details/tabs/MappersTab";
import MapperDetailsPage, {
  ClaimJsonType,
} from "../support/pages/admin-ui/manage/client_scopes/client_scope_details/tabs/mappers/MapperDetailsPage";
import DedicatedScopesMappersTab from "../support/pages/admin-ui/manage/clients/client_details/DedicatedScopesMappersTab";
import ClientDetailsPage from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";

let itemId = "client_scope_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const sidebarPage = new SidebarPage();

const commonPage = new CommonPage();
const listingPage = new ListingPage();
const createClientScopePage = new CreateClientScopePage();
const modalUtils = new ModalUtils();
const dedicatedScopesMappersTab = new DedicatedScopesMappersTab();
const clientDetailsPage = new ClientDetailsPage();

describe("Client Scopes test", () => {
  const modalMessageDeleteConfirmation =
    "Are you sure you want to delete this client scope";
  const notificationMessageDeletionConfirmation =
    "The client scope has been deleted";
  const clientScopeName = "client-scope-test";
  const openIDConnectItemText = "OpenID Connect";
  const clientScope = {
    name: clientScopeName,
    description: "",
    protocol: "openid-connect",
    attributes: {
      "include.in.token.scope": "true",
      "display.on.consent.screen": "true",
      "gui.order": "1",
      "consent.screen.text": "",
    },
  };

  before(async () => {
    for (let i = 0; i < 5; i++) {
      clientScope.name = clientScopeName + i;
      await adminClient.createClientScope(clientScope);
    }
  });

  after(async () => {
    for (let i = 0; i < 5; i++) {
      if (await adminClient.existsClientScope(clientScopeName + i)) {
        await adminClient.deleteClientScope(clientScopeName + i);
      }
    }
  });

  describe("Client Scope filter list items", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("should filter item by name", () => {
      const itemName = clientScopeName + 0;

      listingPage
        .checkEmptySearch()
        .searchItem(itemName, false)
        .itemsEqualTo(1)
        .itemExist(itemName, true);
    });

    it("should filter items by Assigned type All types", () => {
      listingPage
        .selectClientScopeFilter(Filter.AssignedType)
        .selectSecondaryFilterAssignedType(FilterAssignedType.AllTypes)
        .itemExist(FilterAssignedType.Default, true)
        .itemExist(FilterAssignedType.Optional, true)
        .itemExist(FilterAssignedType.None, true);
    });

    it("should filter items by Assigned type Default", () => {
      listingPage
        .selectClientScopeFilter(Filter.AssignedType)
        .selectSecondaryFilterAssignedType(FilterAssignedType.Default)
        .itemExist(FilterAssignedType.Default, true)
        .itemExist(FilterAssignedType.Optional, false)
        .itemExist(FilterAssignedType.None, false);
    });

    it("should filter items by Assigned type Optional", () => {
      listingPage
        .selectClientScopeFilter(Filter.AssignedType)
        .selectSecondaryFilterAssignedType(FilterAssignedType.Optional)
        .itemExist(FilterAssignedType.Default, false)
        .itemExist(FilterAssignedType.Optional, true)
        .itemExist(FilterAssignedType.None, false);
    });

    it("should filter items by Protocol All", () => {
      listingPage
        .selectClientScopeFilter(Filter.Protocol)
        .selectSecondaryFilterProtocol(FilterProtocol.All);
      sidebarPage.waitForPageLoad();
      listingPage
        .showNextPageTableItems()
        .itemExist(FilterProtocol.SAML, true)
        .itemExist(openIDConnectItemText, true); //using FilterProtocol.OpenID will fail, text does not match.
    });

    it("should filter items by Protocol SAML", () => {
      listingPage
        .selectClientScopeFilter(Filter.Protocol)
        .selectSecondaryFilterProtocol(FilterProtocol.SAML)
        .itemExist(FilterProtocol.SAML, true)
        .itemExist(openIDConnectItemText, false); //using FilterProtocol.OpenID will fail, text does not match.
    });

    it("should filter items by Protocol OpenID", () => {
      listingPage
        .selectClientScopeFilter(Filter.Protocol)
        .selectSecondaryFilterProtocol(FilterProtocol.OpenID)
        .itemExist(FilterProtocol.SAML, false)
        .itemExist(openIDConnectItemText, true); //using FilterProtocol.OpenID will fail, text does not match.
    });

    it("should show items on next page are more than 11", () => {
      listingPage.showNextPageTableItems();
      listingPage.itemsGreaterThan(1);
    });
  });

  describe("Client Scope modify list items", () => {
    const itemName = clientScopeName + 0;

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("should modify selected item type to Default from search bar", () => {
      listingPage
        .clickItemCheckbox(itemName)
        .changeTypeToOfSelectedItems(FilterAssignedType.Default);
      listingPage.itemContainValue(itemName, 2, FilterAssignedType.Default);
    });

    it("should modify selected item type to Optional from search bar", () => {
      listingPage
        .clickItemCheckbox(itemName)
        .changeTypeToOfSelectedItems(FilterAssignedType.Optional);
      listingPage.itemContainValue(itemName, 2, FilterAssignedType.Optional);
    });

    const expectedItemAssignedTypes = [
      FilterAssignedType.Default,
      FilterAssignedType.Optional,
      FilterAssignedType.None,
    ];
    expectedItemAssignedTypes.forEach(($assignedType) => {
      const itemName = clientScopeName + 0;
      it(`should modify item ${itemName} AssignedType to ${$assignedType} from item bar`, () => {
        listingPage
          .searchItem(clientScopeName, false)
          .clickRowSelectItem(itemName, $assignedType);
        cy.wait(2000);
        listingPage.searchItem(itemName, false).itemExist($assignedType);
      });
    });

    it("should not allow to modify item AssignedType from search bar when no item selected", () => {
      const itemName = clientScopeName + 0;
      listingPage
        .searchItem(itemName, false)
        .checkInSearchBarChangeTypeToButtonIsDisabled()
        .clickSearchBarActionButton()
        .checkDropdownItemIsDisabled("Delete")
        .clickItemCheckbox(itemName)
        .checkInSearchBarChangeTypeToButtonIsDisabled(false)
        .clickSearchBarActionButton()
        .checkDropdownItemIsDisabled("Delete", false)
        .clickItemCheckbox(itemName)
        .checkInSearchBarChangeTypeToButtonIsDisabled()
        .clickSearchBarActionButton()
        .checkDropdownItemIsDisabled("Delete");
    });

    //TODO: blocked by https://github.com/keycloak/keycloak-admin-ui/issues/1952
    //it("should export item from item bar", () => {

    //});
  });

  describe("Client Scope delete list items ", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("should delete item from item bar", () => {
      listingPage
        .checkInSearchBarChangeTypeToButtonIsDisabled()
        .clickItemCheckbox(clientScopeName + 0)
        .deleteItem(clientScopeName + 0);
      modalUtils
        .checkModalMessage(modalMessageDeleteConfirmation)
        .confirmModal();
      masthead.checkNotificationMessage(
        notificationMessageDeletionConfirmation,
      );
      listingPage.checkInSearchBarChangeTypeToButtonIsDisabled();
    });

    it("should delete selected item from search bar", () => {
      listingPage
        .checkInSearchBarChangeTypeToButtonIsDisabled()
        .clickItemCheckbox(clientScopeName + 1)
        .clickSearchBarActionButton()
        .clickSearchBarActionItem("Delete");
      modalUtils
        .checkModalMessage(modalMessageDeleteConfirmation)
        .confirmModal();
      masthead.checkNotificationMessage(
        notificationMessageDeletionConfirmation,
      );
      listingPage.checkInSearchBarChangeTypeToButtonIsDisabled();
    });

    it("should delete multiple selected items from search bar", () => {
      listingPage
        .checkInSearchBarChangeTypeToButtonIsDisabled()
        .clickItemCheckbox(clientScopeName + 2)
        .clickItemCheckbox(clientScopeName + 3)
        .clickItemCheckbox(clientScopeName + 4)
        .clickSearchBarActionButton()
        .clickSearchBarActionItem("Delete");
      modalUtils
        .checkModalMessage(modalMessageDeleteConfirmation)
        .confirmModal();
      masthead.checkNotificationMessage(
        notificationMessageDeletionConfirmation,
      );
      listingPage.checkInSearchBarChangeTypeToButtonIsDisabled();
    });
  });

  describe("Client Scope creation", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("should fail creating client scope", () => {
      sidebarPage.waitForPageLoad();
      listingPage.goToCreateItem();

      createClientScopePage.save_is_disabled(true);
      createClientScopePage.fillClientScopeData("address").save();

      masthead.checkNotificationMessage(
        "Could not create client scope: 'Client Scope address already exists'",
      );

      createClientScopePage.fillClientScopeData("");
      createClientScopePage.save_is_disabled(true);
    });

    it("hides 'consent text' field when 'display consent' switch is disabled", () => {
      sidebarPage.waitForPageLoad();
      listingPage.goToCreateItem();

      createClientScopePage
        .getSwitchDisplayOnConsentScreenInput()
        .should("be.checked");

      createClientScopePage.getConsentScreenTextInput().should("exist");

      createClientScopePage.switchDisplayOnConsentScreen();

      createClientScopePage
        .getSwitchDisplayOnConsentScreenInput()
        .should("not.be.checked");

      createClientScopePage.getConsentScreenTextInput().should("not.exist");
    });

    it("Client scope CRUD test", () => {
      itemId += "_" + uuid();

      // Create
      listingPage.itemExist(itemId, false).goToCreateItem();

      createClientScopePage.fillClientScopeData(itemId).save();

      masthead.checkNotificationMessage("Client scope created");

      sidebarPage.goToClientScopes();
      sidebarPage.waitForPageLoad();

      // Delete
      listingPage
        .searchItem(itemId, false)
        .itemExist(itemId)
        .deleteItem(itemId);

      modalUtils
        .checkModalMessage(modalMessageDeleteConfirmation)
        .confirmModal();

      masthead.checkNotificationMessage("The client scope has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });

  describe("Scope tab test", () => {
    const scopeTab = new RoleMappingTab("client-scope");
    const scopeName = "address";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("Assign and unassign role", () => {
      const role = "admin";
      const roleType = "roles";
      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);
      scopeTab
        .goToScopeTab()
        .assignRole()
        .changeRoleTypeFilter(roleType)
        .selectRow(role)
        .assign();
      masthead.checkNotificationMessage("Role mapping updated");
      scopeTab.checkRoles([role]);
      scopeTab.hideInheritedRoles().selectRow(role).unAssign();
      modalUtils.checkModalTitle("Remove role?").confirmModal();
      scopeTab.checkRoles([]);
    });
  });

  describe("Mappers tab test", () => {
    const clientScopeDetailsPage = new ClientScopeDetailsPage();
    const mappersTab = new MappersTab();
    const mapperDetailsTab = new MapperDetailsPage();
    const scopeName = "address";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
    });

    it("CRUD mappers", () => {
      const predefinedMapperName = "Predefined Mapper test";
      const predefinedMapper = "Allowed Web Origins";
      const mappers1 = ["birthdate"];
      const mappers2 = ["email verified", "email", "family name"];

      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);
      clientScopeDetailsPage
        .goToMappersTab()
        .addPredefinedMappers(mappers1)
        .addPredefinedMappers(mappers2);

      listingPage.searchItem(mappers1[0], false).goToItemDetails(mappers1[0]);

      mapperDetailsTab
        .fillUserAttribute(mappers1[0] + "1")
        .fillTokenClaimName(mappers1[0] + "2")
        .changeClaimJsonType(ClaimJsonType.Long);

      commonPage.formUtils().save();
      commonPage
        .masthead()
        .checkNotificationMessage("Mapping successfully updated");

      sidebarPage.goToClientScopes();
      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);

      clientScopeDetailsPage.goToMappersTab();

      listingPage.searchItem(mappers1[0], false).goToItemDetails(mappers1[0]);

      mapperDetailsTab
        .checkUserAttribute(mappers1[0] + "1")
        .checkTokenClaimName(mappers1[0] + "2")
        .checkClaimJsonType(ClaimJsonType.Long);

      commonPage.formUtils().cancel();

      mappersTab
        .removeMappers(mappers1.concat(mappers2))
        .addMappersByConfiguration(predefinedMapper, predefinedMapperName);

      sidebarPage.goToClientScopes();
      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);
      clientScopeDetailsPage.goToMappersTab();

      commonPage.tableUtils().checkRowItemExists(predefinedMapperName, true);

      mappersTab.removeMappers([predefinedMapperName]);
    });
  });

  describe("Accessibility tests for client scopes", () => {
    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      sidebarPage.goToClientScopes();
      cy.injectAxe();
    });

    const scopeName = "a11y";

    after(async () => {
      await adminClient.deleteClientScope(scopeName);
    });

    it("Check a11y violations on load/ client scopes", () => {
      cy.checkA11y();
    });

    it("Check a11y violations on empty client scope", () => {
      listingPage.goToCreateItem();
      cy.checkA11y();
    });

    it("Check a11y violations on client scope details", () => {
      const clientScopeDetailsPage = new ClientScopeDetailsPage();
      const mappersTab = new MappersTab();
      const predefinedMapperName = "Predefined Mapper test";
      const predefinedMapper = "Allowed Web Origins";
      const scopeTab = new RoleMappingTab("client-scope");
      const role = "admin";
      const roleType = "roles";

      listingPage.goToCreateItem();
      createClientScopePage.fillClientScopeData(scopeName).save();
      cy.checkA11y();

      clientScopeDetailsPage.goToMappersTab();
      cy.checkA11y();

      dedicatedScopesMappersTab.addPredefinedMapper();
      cy.checkA11y();
      clientDetailsPage.modalUtils().table().clickHeaderItem(1, "input");
      cy.findByTestId("confirm").click();
      cy.checkA11y();

      mappersTab.addMappersByConfiguration(
        predefinedMapper,
        predefinedMapperName,
      );
      cy.checkA11y();

      sidebarPage.goToClientScopes();
      listingPage.searchItem(scopeName, false).goToItemDetails(scopeName);
      clientScopeDetailsPage.goToScopesTab();
      cy.checkA11y();

      cy.findByTestId("no-roles-for-this-client-scope-empty-action").click();
      cy.checkA11y();
      cy.findByTestId("cancel").click();

      scopeTab
        .goToScopeTab()
        .assignRole()
        .changeRoleTypeFilter(roleType)
        .selectRow(role)
        .assign();
      cy.checkA11y();
    });
  });
});
