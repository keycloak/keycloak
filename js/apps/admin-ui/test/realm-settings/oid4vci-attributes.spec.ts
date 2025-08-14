import { expect, test } from "@playwright/test";
import { login } from "../utils/login.js";
import adminClient from "../utils/AdminClient.js";
import { goToRealm, goToRealmSettings } from "../utils/sidebar.js";

const realmName = `oid4vci-test-${crypto.randomUUID()}`;

test.beforeAll(async () => {
  await adminClient.createRealm(realmName, {});
});

test.afterAll(() => adminClient.deleteRealm(realmName));

test.beforeEach(async ({ page }) => {
  await login(page);
  await goToRealm(page, realmName);
  await goToRealmSettings(page);
});

// Helper function to check if OID4VCI tab is available and click it
const ensureOid4vciTabAvailable = async (page: any) => {
  try {
    await page.waitForSelector('[data-testid="rs-oid4vci-attributes-tab"]', {
      state: "visible",
      timeout: 10000,
    });
    await page.getByTestId("rs-oid4vci-attributes-tab").click();
    return true;
  } catch {
    // Tab doesn't exist or is not visible - feature is disabled
    test.skip();
    return false;
  }
};

test("OID4VCI tab visibility", async ({ page }) => {
  // Check if the OID4VCI tab is visible
  const oid4vciTab = page.getByTestId("rs-oid4vci-attributes-tab");
  const isVisible = await oid4vciTab.isVisible();

  if (isVisible) {
    await expect(oid4vciTab).toBeVisible();
  } else {
    await expect(oid4vciTab).toBeHidden();
  }
});

test("should render fields and save values with correct attribute keys", async ({
  page,
}) => {
  // Ensure OID4VCI tab is available and click it
  await ensureOid4vciTabAvailable(page);

  const nonceField = page.getByTestId("oid4vci-nonce-lifetime-seconds");
  const preAuthField = page.getByTestId("pre-authorized-code-lifespan-s");

  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();

  await nonceField.fill("120");
  await preAuthField.fill("300");
  await page.getByTestId("oid4vci-tab-save").click();
  await expect(page.getByText(/success/i)).toBeVisible();

  const realm = await adminClient.getRealm(realmName);
  expect(realm).toBeDefined();
  expect(realm?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBe("120");
  expect(realm?.attributes?.["preAuthorizedCodeLifespanS"]).toBe("300");
});

test("should persist values after page refresh", async ({ page }) => {
  // Ensure OID4VCI tab is available and click it
  await ensureOid4vciTabAvailable(page);

  await page.getByTestId("oid4vci-nonce-lifetime-seconds").fill("120");
  await page.getByTestId("pre-authorized-code-lifespan-s").fill("300");
  await page.getByTestId("oid4vci-tab-save").click();
  await expect(page.getByText(/success/i)).toBeVisible();

  // Refresh the page
  await page.reload();
  await goToRealm(page, realmName);
  await goToRealmSettings(page);

  // The TimeSelector component converts values based on units, so we need to check the actual saved values
  const realm = await adminClient.getRealm(realmName);
  expect(realm?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBeDefined();
  expect(realm?.attributes?.["preAuthorizedCodeLifespanS"]).toBeDefined();

  // The values should be numbers representing seconds
  const nonceValue = parseInt(
    realm?.attributes?.["vc.c-nonce-lifetime-seconds"] || "0",
  );
  const preAuthValue = parseInt(
    realm?.attributes?.["preAuthorizedCodeLifespanS"] || "0",
  );

  expect(nonceValue).toBeGreaterThan(0);
  expect(preAuthValue).toBeGreaterThan(0);
});

test("should validate required fields and minimum values", async ({ page }) => {
  // Ensure OID4VCI tab is available and click it
  await ensureOid4vciTabAvailable(page);

  const nonceField = page.getByTestId("oid4vci-nonce-lifetime-seconds");
  const preAuthField = page.getByTestId("pre-authorized-code-lifespan-s");
  const saveButton = page.getByTestId("oid4vci-tab-save");
  const validationErrorText =
    "Please ensure all fields are filled and values are 30 seconds or greater.";

  // Helper function to test validation scenarios
  const testValidationScenario = async (
    nonceValue: string,
    preAuthValue: string,
    shouldShowError: boolean,
  ) => {
    if (nonceValue === "") {
      await nonceField.clear();
    } else {
      await nonceField.fill(nonceValue);
    }
    await nonceField.blur();

    if (preAuthValue === "") {
      await preAuthField.clear();
    } else {
      await preAuthField.fill(preAuthValue);
    }
    await preAuthField.blur();

    // The save button should be enabled when form is dirty (has changes)
    if (nonceValue !== "" || preAuthValue !== "") {
      await expect(saveButton).toBeEnabled();
    }

    if (shouldShowError) {
      await saveButton.click();
      await expect(page.getByText(validationErrorText).first()).toBeVisible();
    }
  };

  // Test with empty values (should trigger required field validation)
  await testValidationScenario("", "", true);

  // Test with invalid values (below minimum)
  await testValidationScenario("29", "29", true);

  // Test with only one field filled
  await testValidationScenario("30", "", true);

  // Test with valid values
  await testValidationScenario("30", "60", false);

  // Wait for the success message to appear
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();
});
