import { type Page, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login, logout } from "../utils/login.ts";
import { assertAxeViolations } from "../utils/masthead.ts";
import { goToEvents, goToRealm } from "../utils/sidebar.ts";
import {
  assertEmptyTable,
  assertRowExists,
  expandRow,
} from "../utils/table.ts";
import {
  assertSearchButtonDisabled,
  assertSearchChipGroupItemExist,
  clickSearchButton,
  clickSearchPanel,
  enableSaveEvents,
  fillSearchPanel,
  goToAdminEventsTab,
  goToEventsConfig,
} from "./list.ts";

test.describe.serial("Events tests", () => {
  const tableName = "Events";
  const realmName = `events-realm-${uuid()}`;

  const eventsTestUser = {
    eventsTestUserId: "",
    userRepresentation: {
      username: `events-test-${uuid()}`,
      enabled: true,
      credentials: [{ value: "events-test" }],
      realm: realmName,
      email: "some@email.com",
      firstName: "Erik",
      lastName: "Blankenburg",
    },
  };

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { enabled: true });
    const { id } = await adminClient.createUser(
      eventsTestUser.userRepresentation,
    );
    eventsTestUser.eventsTestUserId = id!;
    await adminClient.addClientRoleToUser(
      id!,
      "realm-management",
      ["realm-admin"],
      realmName,
    );
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.describe.serial("User events list empty", () => {
    test.beforeEach(async ({ page }) => {
      await login(page);
      await goToRealm(page, realmName);
      await goToEvents(page);
    });

    test("Show empty when no save events", async ({ page }) => {
      await goToEventsConfig(page);
      await goToEvents(page);
      await assertEmptyTable(page);
    });
  });

  test.describe.serial("User events with events", () => {
    let page: Page;
    test.beforeAll(async ({ browser }) => {
      page = await browser.newPage();
      await login(page);
      await goToRealm(page, realmName);
      await goToEvents(page);
      await goToEventsConfig(page);
      await enableSaveEvents(page);

      await logout(page);
    });

    test.afterAll(async () => {
      await page.close();
    });

    test.beforeEach(async ({ page }) => {
      await login(page, {
        realm: realmName,
        username: eventsTestUser.userRepresentation.username,
        password: eventsTestUser.userRepresentation.credentials[0].value,
      });
      await goToEvents(page);
    });

    test("Expand item to see more information", async ({ page }) => {
      await expandRow(page, tableName, 0);
      await assertRowExists(page, "token_id");
    });

    test("Search by user ID", async ({ page }) => {
      await clickSearchPanel(page);
      await assertSearchButtonDisabled(page);
      await fillSearchPanel(page, {
        userId: eventsTestUser.eventsTestUserId,
      });
      await clickSearchButton(page);

      await assertSearchChipGroupItemExist(
        page,
        eventsTestUser.eventsTestUserId,
      );
    });

    test("Check accessibility on user events tab", async ({ page }) => {
      await assertAxeViolations(page);
    });

    test("Check accessibility on admin events tab", async ({ page }) => {
      await goToAdminEventsTab(page);
      await assertAxeViolations(page);
    });
  });
});
