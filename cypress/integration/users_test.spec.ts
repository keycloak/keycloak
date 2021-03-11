import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";
import UserDetailsPage from "../support/pages/admin_console/manage/users/UserDetailsPage";
import ModalUtils from "../support/util/ModalUtils";

describe("Users test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createUserPage = new CreateUserPage();
  const masthead = new Masthead();
  const modalUtils = new ModalUtils();
  const listingPage = new ListingPage();
  const userDetailsPage = new UserDetailsPage();

  let itemId = "user_crud";

  describe("User creation", () => {
    beforeEach(() => {
      /* 
        Prevent unpredictable 401 errors from failing individual tests.
        These are most often occurring during the login process:
           GET /admin/serverinfo/
           GET /admin/master/console/whoami
      */
      cy.on("uncaught:exception", () => {
        return false;
      });
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToUsers();
    });

    it("Go to create User page", () => {
      cy.wait(100);

      createUserPage.goToCreateUser();
      cy.url().should("include", "users/add-user");

      // Verify Cancel button works
      createUserPage.cancel();
      cy.url().should("not.include", "/add-user");
    });

    it("Create user test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);

      // Create
      cy.wait(100);

      createUserPage.goToCreateUser();

      createUserPage.createUser(itemId).save();

      masthead.checkNotificationMessage("The user has been created");

      sidebarPage.goToUsers();
    });

    it("Go to user details test", function () {
      cy.wait(1000);
      listingPage.searchItem(itemId).itemExist(itemId);

      cy.wait(1000);
      listingPage.goToItemDetails(itemId);

      userDetailsPage.fillUserData().save();

      masthead.checkNotificationMessage("The user has been saved");

      sidebarPage.goToUsers();
      listingPage.searchItem(itemId).itemExist(itemId);

      // Delete
      cy.wait(1000);
      listingPage.deleteItem(itemId);

      modalUtils.checkModalTitle("Delete user?").confirmModal();

      masthead.checkNotificationMessage("The user has been deleted");

      listingPage.itemExist(itemId, false);
    });
  });
});
