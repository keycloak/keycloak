import { v4 as uuid } from "uuid";
import UserRepresentation from "@keycloak/keycloak-admin-client/lib/defs/userRepresentation";
import LoginPage from "../support/pages/LoginPage";
import Masthead from "../support/pages/admin-ui/Masthead";
import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import UsersPage from "../support/pages/admin-ui/manage/users/UsersPage";
import UserDetailsPage from "../support/pages/admin-ui/manage/users/user_details/UserDetailsPage";
import adminClient from "../support/util/AdminClient";
import { keycloakBefore } from "../support/util/keycloak_hooks";

const loginPage = new LoginPage();
const sidebarPage = new SidebarPage();
const usersPage = new UsersPage();
const userDetailsPage = new UserDetailsPage();
const masthead = new Masthead();

const createUser = (fields: UserRepresentation) =>
  cy
    .wrap(null)
    .then(() => adminClient.createUser({ username: uuid(), ...fields }));

const deleteUser = (username: string) =>
  cy.wrap(null).then(() => adminClient.deleteUser(username));

describe("User enable/disable", () => {
  beforeEach(() => {
    loginPage.logIn();
    keycloakBefore();
    sidebarPage.goToUsers();
  });

  it("disables a user", () => {
    createUser({ enabled: true }).then(({ username }) => {
      usersPage.goToUserDetailsPage(username!);
      userDetailsPage.assertEnabled(username!);

      userDetailsPage.toggleEnabled(username!);
      masthead.checkNotificationMessage("The user has been saved");
      cy.wait(1000);
      userDetailsPage.assertDisabled(username!);

      return deleteUser(username!);
    });
  });

  it("enables a user", () => {
    createUser({ enabled: false }).then(({ username }) => {
      usersPage.goToUserDetailsPage(username!);
      userDetailsPage.assertDisabled(username!);

      userDetailsPage.toggleEnabled(username!);
      masthead.checkNotificationMessage("The user has been saved");
      cy.wait(1000);
      userDetailsPage.assertEnabled(username!);

      return deleteUser(username!);
    });
  });

  // See: https://github.com/keycloak/keycloak/issues/19647
  it("ensures submitting doesn't reset the enabled state", () => {
    createUser({ enabled: true }).then(({ username }) => {
      usersPage.goToUserDetailsPage(username!);
      userDetailsPage.assertEnabled(username!);

      userDetailsPage.toggleEnabled(username!);
      masthead.checkNotificationMessage("The user has been saved");
      cy.wait(1000);
      userDetailsPage.assertDisabled(username!);

      userDetailsPage.save();
      masthead.checkNotificationMessage("The user has been saved");
      cy.wait(1000);
      userDetailsPage.assertDisabled(username!);

      return deleteUser(username!);
    });
  });
});
