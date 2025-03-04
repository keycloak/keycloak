import { expect, test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { login, logout } from "../utils/login";
import { assertAxeViolations } from "../utils/masthead";
import { goToClients } from "../utils/sidebar";
import {
  assertIsDesktopView,
  assertIsMobileView,
  clickDocumentationLink,
  clickGlobalHelp,
  getDocumentationLink,
  goToAccountManagement,
  toggleGlobalHelp,
  toggleMobileViewHelp,
  toggleUsernameDropdown,
} from "./main";

test.describe("Masthead tests", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test.describe("Desktop view", () => {
    test("Go to account console and back to admin console", async ({
      page,
    }) => {
      await goToAccountManagement(page);
      await expect(page).toHaveURL(/\/realms\/master\/account/);
    });

    test("Sign out reaches to log in screen", async ({ page }) => {
      await logout(page);
      await expect(page).toHaveURL(/\/auth/);
    });

    test("Go to realm info", async ({ page }) => {
      await goToClients(page);
      await toggleUsernameDropdown(page);
      await page.getByRole("menuitem", { name: "Realm info" }).click();
      await expect(page.getByTestId("welcomeTitle")).toContainText("Welcome");
    });

    test("Should go to documentation page", async ({ page }) => {
      await clickGlobalHelp(page);
      const href = await getDocumentationLink(page);
      if (href) {
        await clickDocumentationLink(page);
        await expect(page.locator("#header")).toContainText(
          "Server Administration Guide",
        );
      }
    });

    test("Enable/disable help mode in desktop mode", async ({ page }) => {
      const helpLabel = '[data-testid="help-label-enabledFeatures"]';
      await assertIsDesktopView(page);
      await page.getByTestId("infoTab").click();
      await expect(page.locator(helpLabel)).toBeVisible();
      await clickGlobalHelp(page);
      await toggleGlobalHelp(page);
      await expect(page.locator(helpLabel)).not.toBeVisible();
      await toggleGlobalHelp(page);
      await expect(page.locator(helpLabel)).toBeVisible();
    });

    test("Check a11y violations on load/ masthead", async ({ page }) => {
      await assertAxeViolations(page);
    });
  });

  test.describe("Login works for unprivileged users", () => {
    const realmName = `test-realm-${uuid()}`;
    const username = `test-user-${uuid()}`;

    test.beforeAll(async () => {
      await adminClient.createRealm(realmName, { enabled: true });
      await adminClient.createUser({
        realm: realmName,
        username,
        enabled: true,
        emailVerified: true,
        credentials: [{ type: "password", value: "test" }],
        firstName: "Test",
        lastName: "User",
        email: "test@keycloak.org",
      });
    });

    test.afterAll(() => adminClient.deleteRealm(realmName));

    test("Login without privileges to see admin console", async ({ page }) => {
      await logout(page);
      await login(page, username, "test", realmName);
      await logout(page, "Test User");
      await expect(page).toHaveURL(/\/auth/);
    });
  });

  test.describe("Mobile view", () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 360, height: 640 });
    });
    test("Mobile menu is shown when in mobile view", async ({ page }) => {
      await assertIsMobileView(page);
    });

    test("Enable/disable help mode in mobile view", async ({ page }) => {
      await assertIsMobileView(page);
      await toggleUsernameDropdown(page);
      await toggleMobileViewHelp(page);
      await expect(page.getByTestId("helpIcon")).toBeVisible();
      await expect(page.getByText("Help off")).toBeVisible();
    });

    test("Check a11y violations on load/ masthead", async ({ page }) => {
      await assertAxeViolations(page);
    });
  });
});
