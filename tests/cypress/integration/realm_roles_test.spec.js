import LoginPage from "../support/pages/LoginPage.js";
import Masthead from "../support/pages/admin_console/Masthead.js";
import ModalUtils from "../support/util/ModalUtils.js";
import ListingPage from "../support/pages/admin_console/ListingPage.js";
import SidebarPage from "../support/pages/admin_console/SidebarPage.js";
import CreateRealmRolePage from "../support/pages/admin_console/manage/realm_roles/CreateRealmRolePage.js";

let itemId = "realm_role_crud";
const loginPage = new LoginPage();
const masthead = new Masthead();
const modalUtils = new ModalUtils();
const sidebarPage = new SidebarPage();
const listingPage = new ListingPage();
const createRealmRolePage = new CreateRealmRolePage();

describe("Realm roles test", function () {

  describe("Realm roles creation", function () {
    beforeEach(function () {
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToRealmRoles();
    });

    it("should fail creating realm role", function () {
      listingPage.goToCreateItem();

      createRealmRolePage.save().checkRealmRoleNameRequiredMessage();

      createRealmRolePage.fillRealmRoleData("admin").save();

      // The error should inform about duplicated name/id (THIS MESSAGE DOES NOT HAVE QUOTES AS THE OTHERS)
      masthead.checkNotificationMessage(
        "Could not create role: Role with name admin already exists"
      );
    });

    it("Realm role CRUD test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      listingPage
        .itemExist(itemId, false)
        .goToCreateItem();

      createRealmRolePage.fillRealmRoleData(itemId).save();

      masthead.checkNotificationMessage("Role created");

      sidebarPage.goToRealmRoles();

      listingPage
        .searchItem(itemId)
        .itemExist(itemId);

      // Update
      
      // Delete
      listingPage
        .deleteItem(itemId);

      modalUtils.checkModalTitle("Delete role?").confirmModal();

      masthead.checkNotificationMessage("The role has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });
});
