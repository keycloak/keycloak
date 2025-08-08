import { test, expect } from "@playwright/test";
import { v4 as uuid } from "uuid";
import adminClient from "../utils/AdminClient";
import { login } from "../utils/login";
import { goToRealm, goToRealmSettings } from "../utils/sidebar";

const realmName = `oid4vci-realm-${uuid()}`;

test.beforeAll(async () => {
  await adminClient.createRealm(realmName);
  const realm = await adminClient.getRealm(realmName);
  expect(realm).toBeDefined();
});

test.afterAll(async () => {
  await adminClient.deleteRealm(realmName);
});

test("OID4VCI tab visibility", async ({ page }) => {
  await login(page);
  await goToRealm(page, realmName);
  await goToRealmSettings(page);

  const oid4vciTab = page.getByTestId("rs-oid4vci-attributes-tab");
  await expect(oid4vciTab).toBeVisible();
});

test("should render fields and save values with correct attribute keys", async ({
  page,
}) => {
  await login(page);
  await goToRealm(page, realmName);
  await goToRealmSettings(page);
  const oid4vciTab = page.getByTestId("rs-oid4vci-attributes-tab");
  await oid4vciTab.click();

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
  await login(page);
  await goToRealm(page, realmName);
  await goToRealmSettings(page);
  await page.getByTestId("rs-oid4vci-attributes-tab").click();

  await page.getByTestId("oid4vci-nonce-lifetime-seconds").fill("120");
  await page.getByTestId("pre-authorized-code-lifespan-s").fill("300");
  await page.getByTestId("oid4vci-tab-save").click();

  await page.reload();
  await page.getByTestId("rs-oid4vci-attributes-tab").click();

  await page.waitForLoadState("domcontentloaded");
  await expect(page.getByTestId("oid4vci-nonce-lifetime-seconds")).toHaveValue(
    "2",
  );
  await expect(page.getByTestId("pre-authorized-code-lifespan-s")).toHaveValue(
    "5",
  );
});

test("should validate required fields and minimum values", async ({ page }) => {
  await login(page);
  await goToRealm(page, realmName);
  await goToRealmSettings(page);
  await page.getByTestId("rs-oid4vci-attributes-tab").click();

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

    await expect(saveButton).toBeEnabled();
    await saveButton.click();

    if (shouldShowError) {
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
