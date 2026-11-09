import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { SERVER_URL } from "../utils/constants.ts";
import { login } from "../utils/login.ts";
import {
  assertAxeViolations,
  assertNotificationMessage,
  selectActionToggleItem,
} from "../utils/masthead.ts";
import { goToRealm, goToSessions } from "../utils/sidebar.ts";
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
  const realmName = `offline-sessions-${uuid()}`;
  const clientId = `offline-client-${uuid()}`;
  const username = `user-${uuid()}`;

  test.beforeAll(async () => {
    await adminClient.createRealm(realmName, { enabled: true });
    await Promise.all([
      adminClient.createClient({
        protocol: "openid-connect",
        clientId,
        publicClient: false,
        directAccessGrantsEnabled: true,
        clientAuthenticatorType: "client-secret",
        secret: "secret",
        standardFlowEnabled: true,
        realm: realmName,
      }),
      adminClient.createUser({
        username,
        enabled: true,
        email: `${username}@example.org`,
        firstName: "Offline",
        lastName: "User",
        credentials: [{ type: "password", value: "password" }],
        realm: realmName,
      }),
    ]);
  });

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    // Each request creates a new offline session for the user.
    const response = await fetch(
      `${SERVER_URL}/realms/${realmName}/protocol/openid-connect/token`,
      {
        method: "POST",
        body: new URLSearchParams({
          grant_type: "password",
          client_id: clientId,
          client_secret: "secret",
          username,
          password: "password",
          scope: "openid offline_access",
        }),
      },
    );
    expect(response.ok).toBe(true);

    await login(page);
    await goToRealm(page, realmName);
    await goToSessions(page);
  });

  test("check offline token", async ({ page }) => {
    await searchItem(page, placeHolder, clientId);
    await assertRowExists(page, username);
    await clickRowKebabItem(page, username, "Revoke");

    // The realm has no other sessions, so the table and its search toolbar disappear.
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

// The not-before policy is realm-wide and invalidates the tokens of all its users, so it is changed in a separate realm.
test.describe.serial("revocation", () => {
  const realmName = `revocation-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));

  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page, { to: { pathname: `/${realmName}/sessions` } });
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
