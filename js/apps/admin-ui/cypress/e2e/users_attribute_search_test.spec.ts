import SidebarPage from "../support/pages/admin-ui/SidebarPage";
import LoginPage from "../support/pages/LoginPage";
import { keycloakBefore } from "../support/util/keycloak_hooks";
import adminClient from "../support/util/AdminClient";
import {
  DefaultUserAttribute,
  UserFilterType,
} from "../support/pages/admin-ui/manage/users/UsersListingPage";
import UsersPage from "../support/pages/admin-ui/manage/users/UsersPage";

describe("Query by user attributes", () => {
  const loginPage = new LoginPage();
  const sidebarPage = new SidebarPage();
  const usersPage = new UsersPage();
  const listingPage = usersPage.listing();

  const emailSuffix = "@example.org";

  const user1Username = "user-attrs-1";
  const user1FirstName = "John";
  const user1LastName = "Doe";
  const user1Pwd = "pwd";
  const user2Username = "user-attrs-2";
  const user2FirstName = "Jane";
  const user2LastName = user1LastName;

  before(async () => {
    await cleanupTestData();
    const user1 = await adminClient.createUser({
      username: user1Username,
      credentials: [
        {
          type: "password",
          value: user1Pwd,
        },
      ],
      email: user1Username + emailSuffix,
      firstName: user1FirstName,
      lastName: user1LastName,
      enabled: true,
    });
    const user1Id = user1.id!;
    await adminClient.addClientRoleToUser(user1Id, "master-realm", [
      "view-users",
    ]);

    await adminClient.createUser({
      username: user2Username,
      email: user2Username + emailSuffix,
      firstName: user2FirstName,
      lastName: user2LastName,
      enabled: true,
    });
  });

  beforeEach(() => {
    loginPage.logIn(user1Username, user1Pwd);
    keycloakBefore();
    sidebarPage.goToUsers();
  });

  after(async () => {
    await cleanupTestData();
  });

  async function cleanupTestData() {
    await adminClient.deleteUser(user1Username, true);
    await adminClient.deleteUser(user2Username, true);
  }

  it("Query with one attribute condition", () => {
    listingPage
      .selectUserSearchFilter(UserFilterType.AttributeSearch)
      .openUserAttributesSearchForm()
      .addUserAttributeSearchCriteria(
        DefaultUserAttribute.lastName,
        user1LastName,
      )
      .triggerAttributesSearch()
      .itemExist(user1Username, true)
      .itemExist(user2Username, true);
  });

  it("Query with two attribute conditions", () => {
    listingPage
      .selectUserSearchFilter(UserFilterType.AttributeSearch)
      .openUserAttributesSearchForm()
      .addUserAttributeSearchCriteria(
        DefaultUserAttribute.lastName,
        user1LastName,
      )
      .addUserAttributeSearchCriteria(
        DefaultUserAttribute.firstName,
        user1FirstName,
      )
      .triggerAttributesSearch()
      .itemExist(user1Username, true)
      .itemExist(user2Username, false);
  });
});
