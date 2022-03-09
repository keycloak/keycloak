import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmRolePage from "../support/pages/admin_console/manage/realm_roles/CreateRealmRolePage";
import AssociatedRolesPage from "../support/pages/admin_console/manage/realm_roles/AssociatedRolesPage";
import {
  keycloakBefore,
  keycloakBeforeEach,
} from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";

let itemId = "realm_role_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createRealmRolePage = new CreateRealmRolePage();
const associatedRolesPage = new AssociatedRolesPage();

describe("Realm roles test", () => {
  before(() => {
    keycloakBefore();
    loginPage.logIn();
  });

  beforeEach(() => {
    keycloakBeforeEach();
    sidebarPage.goToRealmRoles();
  });

  it("should fail creating realm role", () => {
    listingPage.goToCreateItem();
    createRealmRolePage.save().checkRealmRoleNameRequiredMessage();
    createRealmRolePage.fillRealmRoleData("admin").save();

    // The error should inform about duplicated name/id (THIS MESSAGE DOES NOT HAVE QUOTES AS THE OTHERS)
    masthead.checkNotificationMessage(
      "Could not create role: Role with name admin already exists",
      true
    );
  });

  it("shouldn't create a realm role based with only whitespace name", () => {
    listingPage.goToCreateItem();
    createRealmRolePage
      .fillRealmRoleData("  ")
      .checkRealmRoleNameRequiredMessage();
  });

  it("Realm role CRUD test", () => {
    itemId += "_" + (Math.random() + 1).toString(36).substring(7);

    // Create
    listingPage.itemExist(itemId, false).goToCreateItem();
    createRealmRolePage.fillRealmRoleData(itemId).save();
    masthead.checkNotificationMessage("Role created", true);
    sidebarPage.goToRealmRoles();

    const fetchUrl = "/admin/realms/master/roles?first=0&max=11";
    cy.intercept(fetchUrl).as("fetch");

    listingPage.deleteItem(itemId);

    cy.wait(["@fetch"]);
    modalUtils.checkModalTitle("Delete role?").confirmModal();
    masthead.checkNotificationMessage("The role has been deleted", true);

    listingPage.itemExist(itemId, false);
  });

  it("should delete role from details action", () => {
    itemId += "_" + (Math.random() + 1).toString(36).substring(7);
    listingPage.goToCreateItem();
    createRealmRolePage.fillRealmRoleData(itemId).save();
    masthead.checkNotificationMessage("Role created", true);
    createRealmRolePage.clickActionMenu("Delete this role");
    modalUtils.confirmModal();
    masthead.checkNotificationMessage("The role has been deleted", true);
  });

  it("should not be able to delete default role", () => {
    const defaultRole = "default-roles-master";
    listingPage.itemExist(defaultRole).deleteItem(defaultRole);
    masthead.checkNotificationMessage(
      "You cannot delete a default role.",
      true
    );
  });

  it("Associated roles test", () => {
    itemId += "_" + (Math.random() + 1).toString(36).substring(7);

    // Create
    listingPage.itemExist(itemId, false).goToCreateItem();
    createRealmRolePage.fillRealmRoleData(itemId).save();
    masthead.checkNotificationMessage("Role created", true);

    // Add associated realm role
    associatedRolesPage.addAssociatedRealmRole("create-realm");
    masthead.checkNotificationMessage("Associated roles have been added", true);

    // Add associated client role
    associatedRolesPage.addAssociatedClientRole("manage-account");
    masthead.checkNotificationMessage("Associated roles have been added", true);
  });

  describe("edit role details", () => {
    const editRoleName = "going to edit";
    const description = "some description";
    before(() =>
      adminClient.createRealmRole({
        name: editRoleName,
        description,
      })
    );

    after(() => adminClient.deleteRealmRole(editRoleName));

    it("should edit realm role details", () => {
      listingPage.itemExist(editRoleName).goToItemDetails(editRoleName);
      createRealmRolePage.checkNameDisabled().checkDescription(description);
      const updateDescription = "updated description";
      createRealmRolePage.updateDescription(updateDescription).save();
      masthead.checkNotificationMessage("The role has been saved", true);
      createRealmRolePage.checkDescription(updateDescription);
    });
  });
});
