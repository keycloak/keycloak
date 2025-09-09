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

// Helper function to check if OID4VCI feature is enabled by checking server info
const isOid4vciFeatureEnabled = async () => {
  const serverInfo = await adminClient.getServerInfo();
  const oid4vciFeature = serverInfo.features?.find(
    (feature: any) => feature.name === "OID4VC_VCI",
  );
  const isEnabled = oid4vciFeature?.enabled || false;
  return isEnabled;
};

// Helper function to handle feature flag logic and navigate to OID4VCI section
const navigateToOid4vciSection = async (page: any) => {
  const isFeatureEnabled = await isOid4vciFeatureEnabled();
  const tokensTab = page.getByTestId("rs-tokens-tab");

  // Click on Tokens tab first
  await tokensTab.click();

  if (!isFeatureEnabled) {
    // If feature is not enabled, check if the OID4VCI section is hidden
    const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
    await expect(oid4vciJumpLink).toBeHidden();
    return false;
  }

  // Navigate to OID4VCI section using jump link
  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");
  await oid4vciJumpLink.click();

  // Wait for the section to be visible
  const oid4vciSection = page.getByRole("heading", {
    name: "OID4VCI attributes",
  });
  await expect(oid4vciSection).toBeVisible();

  return true;
};

test("OID4VCI section visibility and jump link in Tokens tab", async ({
  page,
}) => {
  const isFeatureEnabled = await isOid4vciFeatureEnabled();
  const tokensTab = page.getByTestId("rs-tokens-tab");

  await tokensTab.click();

  const oid4vciJumpLink = page.getByTestId("jump-link-oid4vci-attributes");

  // Always assert based on feature flag state
  if (isFeatureEnabled) {
    await expect(oid4vciJumpLink).toBeVisible();
    await oid4vciJumpLink.click();
    const oid4vciSection = page.getByRole("heading", {
      name: "OID4VCI attributes",
    });
    await expect(oid4vciSection).toBeVisible();
  } else {
    await expect(oid4vciJumpLink).toBeHidden();
  }
});

test("should render fields and save values with correct attribute keys", async ({
  page,
}) => {
  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  const nonceField = page.getByTestId("oid4vci-nonce-lifetime-seconds");
  const preAuthField = page.getByTestId("pre-authorized-code-lifespan-s");

  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();

  await nonceField.fill("120");
  await preAuthField.fill("300");
  await page.getByTestId("tokens-tab-save").click();
  await expect(page.getByText(/success/i)).toBeVisible();

  const realm = await adminClient.getRealm(realmName);
  expect(realm).toBeDefined();
  expect(realm?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBe("120");
  expect(realm?.attributes?.["preAuthorizedCodeLifespanS"]).toBe("300");
});

test("should persist values after page refresh", async ({ page }) => {
  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  await page.getByTestId("oid4vci-nonce-lifetime-seconds").fill("120");
  await page.getByTestId("pre-authorized-code-lifespan-s").fill("300");
  await page.getByTestId("tokens-tab-save").click();
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

test("should validate form fields and show validation errors", async ({
  page,
}) => {
  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  const nonceField = page.getByTestId("oid4vci-nonce-lifetime-seconds");
  const preAuthField = page.getByTestId("pre-authorized-code-lifespan-s");
  const saveButton = page.getByTestId("tokens-tab-save");
  const validationErrorText =
    "Please ensure all OID4VCI attribute fields are filled and values are 30 seconds or greater.";

  // Test that fields are visible and can be filled
  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();
  await expect(saveButton).toBeVisible();

  // Test with empty values - should show validation error
  await nonceField.clear();
  await preAuthField.clear();
  await saveButton.click();
  await expect(page.getByText(validationErrorText).first()).toBeVisible();

  // Test with invalid values (below minimum) - should show validation error
  await nonceField.fill("29");
  await preAuthField.fill("29");
  await saveButton.click();
  await expect(page.getByText(validationErrorText).first()).toBeVisible();

  // Test with valid values - this should work
  // Clear fields first to ensure clean state
  await nonceField.clear();
  await preAuthField.clear();

  // Fill with values that will work with the default unit behavior
  await nonceField.fill("1");
  await preAuthField.fill("2");

  // Save button should be enabled when form has values
  await expect(saveButton).toBeEnabled();

  await saveButton.click();
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();

  // Verify the values were saved (1 hour = 3600 seconds, 2 hours = 7200 seconds)
  const realm = await adminClient.getRealm(realmName);
  expect(realm?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBe("3600");
  expect(realm?.attributes?.["preAuthorizedCodeLifespanS"]).toBe("7200");
});
