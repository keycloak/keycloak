import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();

describe("Accessibility tests for user federation", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToUserFederation();
    cy.injectAxe();
  });

  it("Check a11y violations on load/ user federation", () => {
    cy.checkA11y();
  });
});
