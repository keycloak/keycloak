import Form from "../support/forms/Form";
import LoginPage from "../support/pages/LoginPage";
import ListingPage from "../support/pages/admin-ui/ListingPage";
import IdentityProviderTab from "../support/pages/admin-ui/manage/organization/IdentityProviderTab";
import MembersTab from "../support/pages/admin-ui/manage/organization/MemberTab";
import OrganizationPage from "../support/pages/admin-ui/manage/organization/OrganizationPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import RealmSettingsPage from "../support/pages/admin-ui/manage/realm_settings/RealmSettingsPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";

const loginPage = new LoginPage();
const listingPage = new ListingPage();
const page = new OrganizationPage();
const realmSettingsPage = new RealmSettingsPage();
const sidebarPage = new SidebarPage();

describe.skip("Organization CRUD", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToRealmSettings();
    realmSettingsPage.setSwitch("organizationsEnabled", true);
    realmSettingsPage.saveGeneral();
  });

  it("should create new organization", () => {
    page.goToTab();
    page.goToCreate();
    Form.assertSaveButtonDisabled();
    page.fillCreatePage({ name: "orgName" });
    Form.assertSaveButtonEnabled();
    page.fillCreatePage({
      name: "orgName",
      domain: ["ame.org", "test.nl"],
      description: "some description",
    });
    Form.clickSaveButton();
    page.assertSaveSuccess();
  });

  it("should modify existing organization", () => {
    cy.wrap(null).then(() =>
      adminClient.createOrganization({
        name: "editName",
        domains: [{ name: "go.org", verified: false }],
      }),
    );
    page.goToTab();

    listingPage.goToItemDetails("editName");
    const newValue = "newName";
    page.fillNameField(newValue).should("have.value", newValue);
    Form.clickSaveButton();
    page.assertSaveSuccess();
    page.goToTab();
    listingPage.itemExist(newValue);
  });

  it("should delete from list", () => {
    page.goToTab();
    listingPage.deleteItem("orgName");
    page.modalUtils().confirmModal();
    page.assertDeleteSuccess();
  });

  it.skip("should delete from details page", () => {
    page.goToTab();
    listingPage.goToItemDetails("newName");

    page
      .actionToolbarUtils()
      .clickActionToggleButton()
      .clickDropdownItem("Delete");
    page.modalUtils().confirmModal();

    page.assertDeleteSuccess();
  });
});

describe.skip("Members", () => {
  const membersTab = new MembersTab();

  before(() => {
    adminClient.createOrganization({
      name: "member",
      domains: [{ name: "o.com", verified: false }],
    });
    adminClient.createUser({ username: "realm-user", enabled: true });
  });

  after(() => {
    adminClient.deleteOrganization("member");
    adminClient.deleteUser("realm-user");
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    page.goToTab();
  });

  it("should add member", () => {
    listingPage.goToItemDetails("member");
    membersTab.goToTab();
    membersTab.clickAddRealmUser();
    membersTab.modalUtils().assertModalVisible(true);
    membersTab.modalUtils().table().selectRowItemCheckbox("realm-user");
    membersTab.modalUtils().add();
    membersTab.assertMemberAddedSuccess();
    membersTab.tableUtils().checkRowItemExists("realm-user");
  });
});

describe.skip("Identity providers", () => {
  const idpTab = new IdentityProviderTab();
  before(() => {
    adminClient.createOrganization({
      name: "idp",
      domains: [{ name: "o.com", verified: false }],
    });
    adminClient.createIdentityProvider("BitBucket", "bitbucket");
  });
  after(() => {
    adminClient.deleteOrganization("idp");
    adminClient.deleteIdentityProvider("bitbucket");
  });

  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    page.goToTab();
  });

  it("should add idp", () => {
    listingPage.goToItemDetails("idp");
    idpTab.goToTab();
    idpTab.emptyState().checkIfExists(true);
    idpTab.emptyState().clickPrimaryBtn();

    idpTab.fillForm({ name: "bitbucket", domain: "o.com", public: true });
    idpTab.modalUtils().confirmModal();

    idpTab.assertAddedSuccess();

    idpTab.tableUtils().checkRowItemExists("bitbucket");
  });
});
