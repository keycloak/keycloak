import { type Page, expect, test } from "@playwright/test";
import { createTestBed } from "../support/testbed.ts";
import { goToClientScopes } from "../utils/sidebar.ts";
import { clickSaveButton, selectItem } from "../utils/form.ts";
import { clickTableRowItem, clickTableToolbarItem } from "../utils/table.ts";
import { login } from "../utils/login.ts";
import { toClientScopes } from "../../src/client-scopes/routes/ClientScopes.tsx";
import { assertNotificationMessage } from "../utils/masthead.ts";

async function goToMappersTab(page: Page) {
  await page.getByTestId("mappers").click();
}

async function createOid4vcClientScope(page: Page, scopeName: string) {
  await goToClientScopes(page);
  await clickTableToolbarItem(page, "Create client scope");
  await selectItem(page, "#kc-protocol", "OpenID for Verifiable Credentials");
  await page.getByTestId("name").fill(scopeName);
  await clickSaveButton(page);
  await assertNotificationMessage(page, "Client scope created");
  await page.waitForURL(/.*\/client-scopes\/.+/);
}

async function selectMapperType(page: Page, mapperType: string) {
  await page.getByText(mapperType, { exact: true }).click();
  await page.getByTestId("name").waitFor({ state: "visible" });
}

async function setupMapperConfiguration(
  page: Page,
  scopeName: string,
  mapperType: string = "Static Claim Mapper",
) {
  await createOid4vcClientScope(page, scopeName);
  await goToMappersTab(page);
  await page.getByRole("button", { name: "Configure a new mapper" }).click();
  await selectMapperType(page, mapperType);
}

async function fillBasicMapperFields(
  page: Page,
  mapperName: string,
  propertyName: string,
  propertyValue: string,
) {
  await page.getByTestId("name").fill(mapperName);
  await page
    .getByRole("textbox", { name: "Static Claim Property Name" })
    .fill(propertyName);
  await page
    .getByRole("textbox", { name: "Static Claim Value" })
    .fill(propertyValue);
}

async function addDisplayEntry(
  page: Page,
  index: number,
  name: string,
  locale: string,
) {
  await page.getByRole("button", { name: "Add display entry" }).click();
  await page
    .locator(`[data-testid="config.vc🍺display.${index}.name"]`)
    .fill(name);
  await page
    .locator(`[data-testid="config.vc🍺display.${index}.locale"]`)
    .fill(locale);
}

async function assertMandatoryClaimAndDisplayButtonVisible(page: Page) {
  await expect(page.getByText("Mandatory Claim")).toBeVisible();
  await expect(
    page.getByRole("checkbox", { name: "Mandatory Claim" }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Add display entry" }),
  ).toBeVisible();
}

async function saveMapperAndAssertSuccess(page: Page) {
  await clickSaveButton(page);
  await assertNotificationMessage(page, "Mapping successfully created");
}

test.describe("OID4VCI Protocol Mapper Configuration", () => {
  let testBed: Awaited<ReturnType<typeof createTestBed>>;

  test.beforeEach(async ({ page }) => {
    testBed = await createTestBed({ verifiableCredentialsEnabled: true });
    await login(page, { to: toClientScopes({ realm: testBed.realm }) });
  });

  test.afterEach(async () => {
    if (testBed) {
      await testBed[Symbol.asyncDispose]();
    }
  });

  test("should display mandatory claim toggle and claim display fields", async ({
    page,
  }) => {
    const scopeName = `oid4vci-mapper-test-${Date.now()}`;
    await setupMapperConfiguration(page, scopeName);
    await assertMandatoryClaimAndDisplayButtonVisible(page);
  });

  test("should save and persist mandatory claim and display fields", async ({
    page,
  }) => {
    const scopeName = `oid4vci-test-persist-${Date.now()}`;
    const mapperName = "test-persistent-mapper";
    await setupMapperConfiguration(page, scopeName);
    await fillBasicMapperFields(page, mapperName, "testClaim", "testValue");

    await page.getByText("Mandatory Claim").click();
    const mandatoryToggle = page.getByRole("checkbox", {
      name: "Mandatory Claim",
    });
    await expect(mandatoryToggle).toBeChecked();

    await addDisplayEntry(page, 0, "Test Claim Name", "en");
    await saveMapperAndAssertSuccess(page);

    await page.getByTestId("nav-item-client-scopes").click();
    await page.getByPlaceholder("Search for client scope").fill(scopeName);
    await clickTableRowItem(page, scopeName);
    await goToMappersTab(page);
    await clickTableRowItem(page, mapperName);

    await expect(
      page.getByRole("checkbox", { name: "Mandatory Claim" }),
    ).toBeChecked();
    await expect(
      page.locator('[data-testid="config.vc🍺display.0.name"]'),
    ).toHaveValue("Test Claim Name");
    await expect(
      page.locator('[data-testid="config.vc🍺display.0.locale"]'),
    ).toHaveValue("en");
  });

  test("should allow adding multiple display entries", async ({ page }) => {
    const scopeName = `oid4vci-multi-display-${Date.now()}`;
    await setupMapperConfiguration(page, scopeName);
    await fillBasicMapperFields(
      page,
      "multi-lang-mapper",
      "email",
      "user@example.com",
    );

    const displayEntries = [
      { name: "Email Address", locale: "en" },
      { name: "E-Mail-Adresse", locale: "de" },
      { name: "Adresse e-mail", locale: "fr" },
    ];

    for (let i = 0; i < displayEntries.length; i++) {
      await addDisplayEntry(
        page,
        i,
        displayEntries[i].name,
        displayEntries[i].locale,
      );
    }

    for (let i = 0; i < displayEntries.length; i++) {
      await expect(
        page.locator(`[data-testid="config.vc🍺display.${i}.name"]`),
      ).toHaveValue(displayEntries[i].name);
    }

    await saveMapperAndAssertSuccess(page);
  });

  test("should allow removing display entries", async ({ page }) => {
    const scopeName = `oid4vci-remove-display-${Date.now()}`;
    await setupMapperConfiguration(page, scopeName);
    await fillBasicMapperFields(page, "remove-test-mapper", "test", "value");

    await addDisplayEntry(page, 0, "First Entry", "en");
    await addDisplayEntry(page, 1, "Second Entry", "de");

    await page.locator('[data-testid="config.vc🍺display.0.remove"]').click();

    await expect(
      page.locator('[data-testid="config.vc🍺display.0.name"]'),
    ).toHaveValue("Second Entry");
    await expect(
      page.locator('[data-testid="config.vc🍺display.0.locale"]'),
    ).toHaveValue("de");

    await saveMapperAndAssertSuccess(page);
  });

  test("should work with all OID4VC mapper types", async ({ page }) => {
    const scopeName = `oid4vci-all-types-${Date.now()}`;
    const mapperTypes = [
      "User Attribute Mapper",
      "Static Claim Mapper",
      "CredentialSubject ID Mapper",
    ];
    await createOid4vcClientScope(page, scopeName);
    await goToMappersTab(page);

    for (const mapperType of mapperTypes) {
      const addButton = page
        .getByRole("button", { name: "Configure a new mapper" })
        .or(page.getByRole("button", { name: "Add mapper" }))
        .first();
      await addButton.click();

      // Handle different UI states: first mapper shows no dropdown menu,
      // subsequent mappers show "Add mapper" dropdown with "By configuration" option
      const byConfigMenuItem = page.getByRole("menuitem", {
        name: "By configuration",
      });
      const menuItemExists = (await byConfigMenuItem.count()) > 0;
      // eslint-disable-next-line playwright/no-conditional-in-test
      if (menuItemExists) {
        await byConfigMenuItem.click();
      }

      await selectMapperType(page, mapperType);
      await assertMandatoryClaimAndDisplayButtonVisible(page);

      const cancelButton = page
        .getByRole("button", { name: "Cancel" })
        .or(page.getByRole("link", { name: "Cancel" }));
      await cancelButton.click();
    }
  });
});
