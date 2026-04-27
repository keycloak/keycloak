import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { goToSessions } from "../utils/sidebar.ts";
import {
  assertNoResults,
  assertRowExists,
  clickRowKebabItem,
  clickTableRowItem,
  getTableData,
  searchItem,
} from "../utils/table.ts";
import {
  assertNotBeforeValue,
  assertRowHasSignOutKebab,
  clickNotBefore,
  clickPush,
  clickSetToNow,
} from "./main.ts";

const admin = "admin";
const client = "security-admin-console";
const tableName = "Sessions";
const placeHolder = "Search session";

test.describe.serial("Sessions test", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToSessions(page);
  });

  test.describe.serial("Sessions list view", () => {
    test("check item values", async ({ page }) => {
      await searchItem(page, placeHolder, client);
      const rows = await getTableData(page, tableName);
      expect(rows).not.toBeNull();

      await assertRowHasSignOutKebab(page, admin);
    });

    test("go to item accessed clients link", async ({ page }) => {
      await searchItem(page, placeHolder, client);
      await clickTableRowItem(page, admin);
      expect(page.url()).toMatch(/users\/.*\/sessions/);
    });
  });
});

test.describe.serial("Offline sessions", () => {
  const clientId = `offline-client-${uuid()}`;
  const username = `user-${uuid()}`;

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToSessions(page);
  });

  test.beforeEach(async () => {
    await Promise.all([
      adminClient.createClient({
        protocol: "openid-connect",
        clientId,
        publicClient: false,
        directAccessGrantsEnabled: true,
        clientAuthenticatorType: "client-secret",
        secret: "secret",
        standardFlowEnabled: true,
      }),
      adminClient.createUser({
        username,
        enabled: true,
        credentials: [{ type: "password", value: "password" }],
      }),
    ]);

    await adminClient.auth({
      username,
      password: "password",
      grantType: "password",
      clientId,
      clientSecret: "secret",
      scopes: ["openid", "offline_access"],
    });
  });

  test.afterAll(async () => {
    await Promise.all([
      adminClient.deleteClient(clientId),
      adminClient.deleteUser(username),
    ]);
  });

  test("check offline token", async ({ page }) => {
    await searchItem(page, placeHolder, clientId);
    await assertRowExists(page, username);
    await clickRowKebabItem(page, username, "Revoke");

    await searchItem(page, placeHolder, clientId);
    await assertRowExists(page, username, false);
  });
});

test.describe.serial("Search", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToSessions(page);
  });

  test("search non-existent session", async ({ page }) => {
    await searchItem(page, placeHolder, "non-existent-session");
    await assertNoResults(page);
  });
});

test.describe.serial("revocation", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToSessions(page);
  });

  test("Clear revocation notBefore", async ({ page }) => {
    await selectActionToggleItem(page, "Revocation");
    await clickNotBefore(page);
    await assertNotificationMessage(
      page,
      'Success! "Not Before" cleared for realm.',
    );
  });

  test("Check if notBefore cleared", async ({ page }) => {
    await selectActionToggleItem(page, "Revocation");
    await assertNotBeforeValue(page, "None");
  });

  test("Set revocation notBefore", async ({ page }) => {
    await selectActionToggleItem(page, "Revocation");
    await clickSetToNow(page);
    await assertNotificationMessage(
      page,
      'Success! "Not before" set for realm',
    );
  });

  test("Push when URI not configured", async ({ page }) => {
    await selectActionToggleItem(page, "Revocation");
    await clickPush(page);
    await assertNotificationMessage(
      page,
      "No push sent. No admin URI configured or no registered cluster nodes available",
    );
  });
});

test.describe.serial("Accessibility tests for sessions", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToSessions(page);
  });

  test("Check a11y violations on load/ sessions", async ({ page }) => {
    await assertAxeViolations(page);
  });

  test("Check a11y violations on revocation dialog", async ({ page }) => {
    await page.getByTestId("action-dropdown").click();
    await page.getByTestId("revocation").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on sign out all active sessions dialog", async ({
    page,
  }) => {
    await page.getByTestId("action-dropdown").click();
    await page.getByTestId("logout-all").click();
    await assertAxeViolations(page);
  });
});
