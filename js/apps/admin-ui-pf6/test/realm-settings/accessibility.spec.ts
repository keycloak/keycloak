import { test } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient.ts";
import { login } from "../utils/login.ts";
import { assertAxeViolations } from "../utils/masthead.ts";
import { pickRoleType } from "../utils/roles.ts";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.ts";
import {
  goToClientPoliciesList,
  goToClientPoliciesTab,
} from "./client-policies.ts";
import { goToRealmEventsTab } from "./events.ts";
import { goToAddProviders, goToKeys } from "./keys.ts";
import {
  goToLocalizationTab,
  goToRealmOverridesSubTab,
} from "./localization.ts";
import { goToLoginTab } from "./login.ts";

test.describe.serial("Accessibility tests for realm settings", () => {
  const realmName = `realm-settings-accessibility-${uuid()}`;

  test.beforeAll(() => adminClient.createRealm(realmName));
  test.afterAll(() => adminClient.deleteRealm(realmName));

  test.beforeEach(async ({ page }) => {
    await login(page);
    await goToRealm(page, realmName);
    await goToRealmSettings(page);
  });

  test("Check a11y violations on load/ realm settings/ general tab", async ({
    page,
  }) => {
    await assertAxeViolations(page);
  });

  test("Check a11y violations on login tab", async ({ page }) => {
    await goToLoginTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on email tab", async ({ page }) => {
    await page.getByTestId("rs-email-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on themes tab", async ({ page }) => {
    await page.getByTestId("rs-themes-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on keys tab/ keys list sub tab", async ({
    page,
  }) => {
    await goToKeys(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on keys tab/ providers sub tab", async ({
    page,
  }) => {
    await goToKeys(page);
    await goToAddProviders(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on keys tab/ providers sub tab / adding provider", async ({
    page,
  }) => {
    await goToKeys(page);
    await goToAddProviders(page);
    await page.getByTestId("addProviderDropdown").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on events tab/ event listeners sub tab", async ({
    page,
  }) => {
    await goToRealmEventsTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on events tab/ user events settings sub tab", async ({
    page,
  }) => {
    await goToRealmEventsTab(page);
    await page.getByTestId("rs-events-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on events tab/ admin events settings sub tab", async ({
    page,
  }) => {
    await goToRealmEventsTab(page);
    await page.getByTestId("rs-admin-events-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on localization locales sub tab", async ({
    page,
  }) => {
    await goToLocalizationTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on localization realm overrides sub tab", async ({
    page,
  }) => {
    await goToLocalizationTab(page);
    await goToRealmOverridesSubTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on localization realm overrides sub tab/ adding message bundle", async ({
    page,
  }) => {
    await goToLocalizationTab(page);
    await goToRealmOverridesSubTab(page);
    await page.getByTestId("add-translationBtn").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on localization effective message bundles sub tab", async ({
    page,
  }) => {
    await goToLocalizationTab(page);
    await page
      .getByTestId("rs-localization-effective-message-bundles-tab")
      .click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on security defenses tab", async ({ page }) => {
    await page.getByTestId("rs-security-defenses-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on sessions tab", async ({ page }) => {
    await page.getByTestId("rs-sessions-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on tokens tab", async ({ page }) => {
    await page.getByTestId("rs-tokens-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on client policies tab/ profiles sub tab", async ({
    page,
  }) => {
    await goToClientPoliciesTab(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on client policies tab/ creating profile", async ({
    page,
  }) => {
    await goToClientPoliciesTab(page);
    await page.getByTestId("createProfile").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on client policies tab/ policies sub tab", async ({
    page,
  }) => {
    await goToClientPoliciesTab(page);
    await goToClientPoliciesList(page);
    await assertAxeViolations(page);
  });

  test("Check a11y violations on client policies tab/ creating policy", async ({
    page,
  }) => {
    await goToClientPoliciesTab(page);
    await goToClientPoliciesList(page);
    await page.getByTestId("no-client-policies-empty-action").click();
    await assertAxeViolations(page);
    await page.getByTestId("cancelCreatePolicy").click();
  });

  test("Check a11y violations on user registration tab/ default roles sub tab", async ({
    page,
  }) => {
    await page.getByTestId("rs-userRegistration-tab").click();
    await assertAxeViolations(page);
  });

  test("Check a11y violations on user registration tab/ default roles sub tab/ assigning role", async ({
    page,
  }) => {
    await page.getByTestId("rs-userRegistration-tab").click();
    await pickRoleType(page, "client");
    await assertAxeViolations(page);
  });
});
