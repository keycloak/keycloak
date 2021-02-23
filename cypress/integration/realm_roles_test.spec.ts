import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ModalUtils from "../support/util/ModalUtils";
import ListingPage from "../support/pages/admin_console/ListingPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
import CreateRealmRolePage from "../support/pages/admin_console/manage/realm_roles/CreateRealmRolePage";

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
      listingPage.itemExist(itemId, false).goToCreateItem();

      createRealmRolePage.fillRealmRoleData(itemId).save();

      masthead.checkNotificationMessage("Role created");

      sidebarPage.goToRealmRoles();

      listingPage.searchItem(itemId).itemExist(itemId);

      // Update

      // Delete
      listingPage.deleteItem(itemId);

      modalUtils.checkModalTitle("Delete role?").confirmModal();

      masthead.checkNotificationMessage("The role has been deleted");

      listingPage.itemExist(itemId, false);
    });

    it("Associated roles test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      listingPage.itemExist(itemId, false).goToCreateItem();

      createRealmRolePage.fillRealmRoleData(itemId).save();

      masthead.checkNotificationMessage("Role created");

      // Add associated realm role
      cy.get("#roles-actions-dropdown").last().click();

      cy.get("#add-roles").click();

      cy.wait(100);

      cy.get('[type="checkbox"]').eq(1).check();

      cy.get("#add-associated-roles-button").contains("Add").click();

      cy.url().should("include", "/AssociatedRoles");

      cy.get("#composite-role-badge").should("contain.text", "Composite");

      // Add associated client role

      cy.get('[data-cy=add-role-button]').click();

      cy.wait(100);

      cy.get('[data-cy=filter-type-dropdown]').click()

      cy.get('[data-cy=filter-type-dropdown-item]').click()

      cy.wait(2500);

      cy.get('[type="checkbox"]').eq(4).check({force: true});

      cy.get("#add-associated-roles-button").contains("Add").click();

      cy.wait(2500);
    });
  });
});
