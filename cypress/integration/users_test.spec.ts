import SidebarPage from "../support/pages/admin_console/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import CreateUserPage from "../support/pages/admin_console/manage/users/CreateUserPage";
import Masthead from "../support/pages/admin_console/Masthead";
import ListingPage from "../support/pages/admin_console/ListingPage";

describe("Users test", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const createUserPage = new CreateUserPage();
  const masthead = new Masthead();
  const listingPage = new ListingPage();

  let itemId = "user_crud";

  describe("User creation", () => {
    beforeEach(function () {
      cy.visit("");
      loginPage.logIn();
      sidebarPage.goToUsers();
    });

    it("Go to create User page", () => {
      createUserPage.goToCreateUser();
      cy.url().should("include", "users/add-user");

      // Verify Cancel button works
      createUserPage.cancel();
      cy.url().should("not.include", "/add-user");
    });

    it("Create user test", function () {
      itemId += "_" + (Math.random() + 1).toString(36).substring(7);
 
      // Create
      createUserPage.goToCreateUser();
      createUserPage.fillRealmRoleData(itemId).save();

      masthead.checkNotificationMessage("The user has been created");

      sidebarPage.goToUsers();
      listingPage.searchItem(itemId).itemExist(itemId);
    });
  });
});
