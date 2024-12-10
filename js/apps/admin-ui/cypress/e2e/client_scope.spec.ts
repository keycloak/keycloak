import ListingPage, {
  Filter,
  FilterAssignedType,
} from "../support/pages/admin-ui/ListingPage";
import ClientDetailsPage from "../support/pages/admin-ui/manage/clients/client_details/ClientDetailsPage";
import CommonPage from "../support/pages/CommonPage";
import LoginPage from "../support/pages/LoginPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const clientDetailsPage = new ClientDetailsPage();
const commonPage = new CommonPage();
const listingPage = new ListingPage();

describe("Client details - Client scopes subtab", () => {
  const clientId = "client-scopes-subtab-test";
  const clientScopeName = "client-scope-test";
  const clientScopeNameDefaultType = "client-scope-test-default-type";
  const clientScopeNameOptionalType = "client-scope-test-optional-type";
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
  const msgScopeMappingRemoved = "Scope mapping successfully removed";

  const realmName = `clients-realm-${crypto.randomUUID()}`;

  before(async () => {
    await adminClient.createRealm(realmName);
    await adminClient.inRealm(realmName, () =>
      adminClient.createClient({
        clientId,
        protocol: "openid-connect",
        publicClient: false,
      }),
    );
    for (let i = 0; i < 5; i++) {
      clientScope.name = clientScopeName + i;
      await adminClient.inRealm(realmName, () =>
        adminClient.createClientScope(clientScope),
      );
      await adminClient.inRealm(realmName, () =>
        adminClient.addDefaultClientScopeInClient(
          clientScopeName + i,
          clientId,
        ),
      );
    }
    clientScope.name = clientScopeNameDefaultType;
    await adminClient.inRealm(realmName, () =>
      adminClient.createClientScope(clientScope),
    );
    clientScope.name = clientScopeNameOptionalType;
    await adminClient.inRealm(realmName, () =>
      adminClient.createClientScope(clientScope),
    );
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    commonPage.sidebar().goToRealm(realmName);
    commonPage.sidebar().goToClients();
    commonPage.tableToolbarUtils().searchItem(clientId);
    cy.intercept(`/admin/realms/${realmName}/clients/*`).as("fetchClient");
    commonPage.tableUtils().clickRowItemLink(clientId);
    cy.wait("@fetchClient");
    clientDetailsPage.goToClientScopesTab();
  });

  after(() => adminClient.deleteRealm(realmName));

  it("Should list client scopes", () => {
    commonPage
      .tableUtils()
      .checkRowItemsGreaterThan(1)
      .checkRowItemExists(clientScopeName + 0);
  });

  it("Should search existing client scope by name", () => {
    commonPage.tableToolbarUtils().searchItem(clientScopeName + 0, false);
    commonPage
      .tableUtils()
      .checkRowItemExists(clientScopeName + 0)
      .checkRowItemsEqualTo(1);
  });

  it("Should search non-existent client scope by name", () => {
    commonPage.tableToolbarUtils().searchItem("non-existent-item", false);
    commonPage.tableUtils().checkIfExists(false);
    commonPage.emptyState().checkIfExists(true);
  });

  it("Should search existing client scope by assigned type", () => {
    commonPage
      .tableToolbarUtils()
      .selectSearchType(Filter.Name, Filter.AssignedType)
      .selectSecondarySearchType(FilterAssignedType.Default);
    commonPage
      .tableUtils()
      .checkRowItemExists(FilterAssignedType.Default)
      .checkRowItemExists(FilterAssignedType.Optional, false);
    commonPage
      .tableToolbarUtils()
      .selectSecondarySearchType(FilterAssignedType.Optional);
    commonPage
      .tableUtils()
      .checkRowItemExists(FilterAssignedType.Default, false)
      .checkRowItemExists(FilterAssignedType.Optional);
    commonPage
      .tableToolbarUtils()
      .selectSecondarySearchType(FilterAssignedType.AllTypes);
    commonPage
      .tableUtils()
      .checkRowItemExists(FilterAssignedType.Default)
      .checkRowItemExists(FilterAssignedType.Optional);
  });

  const newItemsWithExpectedAssignedTypes = [
    [clientScopeNameOptionalType, FilterAssignedType.Optional],
    [clientScopeNameDefaultType, FilterAssignedType.Default],
  ];
  newItemsWithExpectedAssignedTypes.forEach(($type) => {
    const [itemName, assignedType] = $type;
    it(`Should add client scope ${itemName} with ${assignedType} assigned type`, () => {
      commonPage.tableToolbarUtils().addClientScope();
      commonPage
        .modalUtils()
        .checkModalTitle("Add client scopes to " + clientId);
      commonPage.tableUtils().selectRowItemCheckbox(itemName);
      commonPage.modalUtils().confirmModalWithItem(assignedType);
      commonPage.masthead().checkNotificationMessage("Scope mapping updated");
      commonPage.tableToolbarUtils().searchItem(itemName, false);
      commonPage
        .tableUtils()
        .checkRowItemExists(itemName)
        .checkRowItemExists(assignedType);
    });
  });

  const expectedItemAssignedTypes = [
    FilterAssignedType.Optional,
    FilterAssignedType.Default,
  ];
  expectedItemAssignedTypes.forEach(($assignedType) => {
    const itemName = clientScopeName + 0;
    it(`Should change item ${itemName} AssignedType to ${$assignedType} from search bar`, () => {
      commonPage.tableToolbarUtils().searchItem(itemName, false);
      commonPage.tableUtils().selectRowItemCheckbox(itemName);
      commonPage.tableToolbarUtils().changeTypeTo($assignedType);
      commonPage.masthead().checkNotificationMessage("Scope mapping updated");
      commonPage.tableToolbarUtils().searchItem(itemName, false);
      commonPage.tableUtils().checkRowItemExists($assignedType);
    });
  });

  it("Should show items on next page are more than 11", () => {
    commonPage.sidebar().waitForPageLoad();
    commonPage.tableToolbarUtils().clickNextPageButton();
    commonPage.tableUtils().checkRowItemsGreaterThan(1);
  });

  it("Should remove client scope from item bar", () => {
    const itemName = clientScopeName + 0;
    commonPage.tableToolbarUtils().searchItem(itemName, false);
    commonPage.tableUtils().selectRowItemAction(itemName, "Remove");
    commonPage.modalUtils().confirmModal();
    commonPage.masthead().checkNotificationMessage(msgScopeMappingRemoved);
    commonPage.tableToolbarUtils().searchItem(itemName, false);
    listingPage.assertNoResults();
  });

  it("Should remove multiple client scopes from search bar", () => {
    const itemName1 = clientScopeName + 1;
    const itemName2 = clientScopeName + 2;
    cy.intercept(`/admin/realms/${realmName}/client-scopes`).as("load");
    commonPage.tableToolbarUtils().clickSearchButton();
    cy.wait("@load");
    cy.wait(1000);
    commonPage.tableToolbarUtils().checkActionItemIsEnabled("Remove", false);
    commonPage.tableToolbarUtils().searchItem(clientScopeName, false);
    commonPage
      .tableUtils()
      .selectRowItemCheckbox(itemName1)
      .selectRowItemCheckbox(itemName2);
    cy.intercept(`/admin/realms/${realmName}/client-scopes`).as("load");
    commonPage.tableToolbarUtils().clickSearchButton();
    cy.wait("@load");
    cy.wait(1000);
    commonPage.tableToolbarUtils().clickActionItem("Remove");
    commonPage.masthead().checkNotificationMessage(msgScopeMappingRemoved);
    commonPage.tableToolbarUtils().searchItem(clientScopeName, false);
    commonPage
      .tableUtils()
      .checkRowItemExists(itemName1, false)
      .checkRowItemExists(itemName2, false);
    commonPage.tableToolbarUtils().clickSearchButton();
  });

  it("Should show initial items after filtering", () => {
    commonPage
      .tableToolbarUtils()
      .selectSearchType(Filter.Name, Filter.AssignedType)
      .selectSecondarySearchType(FilterAssignedType.Optional)
      .selectSearchType(Filter.AssignedType, Filter.Name);
    commonPage
      .tableUtils()
      .checkRowItemExists(FilterAssignedType.Default, false)
      .checkRowItemExists(FilterAssignedType.Optional);
  });

  describe("Client scopes evaluate subtab", () => {
    const clientName = "testClient";

    beforeEach(() => {
      loginPage.logIn();
      keycloakBefore();
      commonPage.sidebar().goToRealm(realmName);
      commonPage.sidebar().goToClients();
    });

    before(async () => {
      await adminClient.inRealm(realmName, () =>
        adminClient.createClient({
          protocol: "openid-connect",
          clientId: clientName,
          publicClient: false,
        }),
      );
      await adminClient.inRealm(realmName, () =>
        adminClient.createUser({
          username: "admin-a",
          enabled: true,
        }),
      );
    });

    after(async () => {
      await adminClient.inRealm(realmName, () =>
        adminClient.deleteClient(clientName),
      );
    });

    it("check effective protocol mappers list is not empty and find effective protocol mapper locale", () => {
      commonPage.tableToolbarUtils().searchItem(clientName);
      commonPage.tableUtils().clickRowItemLink(clientName);

      clientDetailsPage.goToClientScopesEvaluateTab();

      cy.findByTestId("effective-protocol-mappers")
        .find("tr")
        .should("have.length.gt", 0);
    });

    it("check role scope mappings list list is not empty and find role scope mapping admin", () => {
      commonPage.tableToolbarUtils().searchItem(clientName);
      commonPage.tableUtils().clickRowItemLink(clientName);

      clientDetailsPage.goToClientScopesEvaluateTab();
      clientDetailsPage.goToClientScopesEvaluateEffectiveRoleScopeMappingsTab();

      cy.findByTestId("effective-role-scope-mappings")
        .find("tr")
        .should("have.length.gt", 0);
    });

    it("check generated id token and user info", () => {
      commonPage.tableToolbarUtils().searchItem(clientName);
      commonPage.tableUtils().clickRowItemLink(clientName);

      clientDetailsPage.goToClientScopesEvaluateTab();
      cy.get("div#generatedAccessToken").contains("No generated access token");

      clientDetailsPage.goToClientScopesEvaluateGeneratedIdTokenTab();
      cy.get("div#generatedIdToken").contains("No generated id token");

      clientDetailsPage.goToClientScopesEvaluateGeneratedUserInfoTab();
      cy.get("div#generatedUserInfo").contains("No generated user info");

      cy.get("[data-testid='user'] input").type("admin-a");
      cy.get(".pf-v5-c-menu__item-text").click();

      clientDetailsPage.goToClientScopesEvaluateGeneratedAccessTokenTab();
      cy.get("div#generatedAccessToken").contains(
        '"preferred_username": "admin-a"',
      );
      cy.get("div#generatedAccessToken").contains('"scope": "');

      clientDetailsPage.goToClientScopesEvaluateGeneratedIdTokenTab();
      cy.get("div#generatedIdToken").contains(
        '"preferred_username": "admin-a"',
      );

      clientDetailsPage.goToClientScopesEvaluateGeneratedUserInfoTab();
      cy.get("div#generatedIdToken").contains(
        '"preferred_username": "admin-a"',
      );
      cy.get("div#generatedIdToken").contains('"sid"');
    });
  });
});
