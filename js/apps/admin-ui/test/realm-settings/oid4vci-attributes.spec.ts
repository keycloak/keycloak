import { expect, test, type Page } from "@playwright/test";
import { generatePath } from "react-router-dom";
import { toRealmSettings } from "../../src/realm-settings/routes/RealmSettings.tsx";
import { createTestBed } from "../support/testbed.ts";
import adminClient from "../utils/AdminClient.js";
import { SERVER_URL, ROOT_PATH } from "../utils/constants.ts";
import { login } from "../utils/login.js";

// Helper function to check if OID4VCI feature is enabled by checking server info
async function isOid4vciFeatureEnabled() {
  const serverInfo = await adminClient.getServerInfo();
  const features = serverInfo.features ?? [];

  return features.some(
    (feature) => feature.name === "OID4VC_VCI" && feature.enabled,
  );
}

// Helper function to handle feature flag logic and navigate to OID4VCI section
const navigateToOid4vciSection = async (page: Page) => {
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
  const realm = await createTestBed();
  await login(page, { to: toRealmSettings({ realm }) });

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
  const realm = await createTestBed();
  await login(page, { to: toRealmSettings({ realm }) });

  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );

  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();

  await nonceField.fill("120");
  await preAuthField.fill("300");
  await page.getByTestId("tokens-tab-save").click();
  await expect(page.getByText(/success/i)).toBeVisible();

  const realmData = await adminClient.getRealm(realm);
  expect(realmData).toBeDefined();
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBe("120");
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBe("300");
});

test("should persist values after page refresh", async ({ page }) => {
  const realm = await createTestBed();
  await login(page, { to: toRealmSettings({ realm }) });

  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  await page.getByTestId("attributes.vc🍺c-nonce-lifetime-seconds").fill("120");
  await page.getByTestId("attributes.preAuthorizedCodeLifespanS").fill("300");
  await page.getByTestId("tokens-tab-save").click();
  await expect(page.getByText(/success/i)).toBeVisible();

  // Refresh the page
  await page.reload();

  // Navigate back to realm settings using the same pattern as login
  const url = new URL(generatePath(ROOT_PATH, { realm }), SERVER_URL);
  url.hash = toRealmSettings({ realm }).pathname!;
  await page.goto(url.toString());

  // The TimeSelector component converts values based on units, so we need to check the actual saved values
  const realmData = await adminClient.getRealm(realm);
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBeDefined();
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBeDefined();

  // The values should be numbers representing seconds
  const nonceValue = parseInt(
    realmData?.attributes?.["vc.c-nonce-lifetime-seconds"] || "0",
  );
  const preAuthValue = parseInt(
    realmData?.attributes?.["preAuthorizedCodeLifespanS"] || "0",
  );

  expect(nonceValue).toBeGreaterThan(0);
  expect(preAuthValue).toBeGreaterThan(0);
});

test("should validate form fields and save valid values", async ({ page }) => {
  const realm = await createTestBed();
  await login(page, { to: toRealmSettings({ realm }) });

  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );
  const saveButton = page.getByTestId("tokens-tab-save");

  // Test that fields are visible and can be filled
  await expect(nonceField).toBeVisible();
  await expect(preAuthField).toBeVisible();
  await expect(saveButton).toBeVisible();

  // Test with valid values - this should work
  // Clear fields first to ensure clean state
  await nonceField.clear();
  await preAuthField.clear();

  // Fill with smaller, more reasonable values for testing
  await nonceField.fill("60");
  await preAuthField.fill("120");

  // Save button should be enabled when form has values
  await expect(saveButton).toBeEnabled();

  await saveButton.click();
  await expect(
    page.getByText("Realm successfully updated").first(),
  ).toBeVisible();

  // Verify the values were saved correctly
  const realmData = await adminClient.getRealm(realm);
  expect(realmData?.attributes?.["vc.c-nonce-lifetime-seconds"]).toBeDefined();
  expect(realmData?.attributes?.["preAuthorizedCodeLifespanS"]).toBeDefined();

  // The values should be numbers representing seconds
  const nonceValue = parseInt(
    realmData?.attributes?.["vc.c-nonce-lifetime-seconds"] || "0",
  );
  const preAuthValue = parseInt(
    realmData?.attributes?.["preAuthorizedCodeLifespanS"] || "0",
  );

  expect(nonceValue).toBeGreaterThan(0);
  expect(preAuthValue).toBeGreaterThan(0);
});

test("should show validation error for values below minimum threshold", async ({
  page,
}) => {
  const realm = await createTestBed();
  await login(page, { to: toRealmSettings({ realm }) });

  const isFeatureEnabled = await navigateToOid4vciSection(page);
  if (!isFeatureEnabled) test.skip();

  const nonceField = page.getByTestId(
    "attributes.vc🍺c-nonce-lifetime-seconds",
  );
  const preAuthField = page.getByTestId(
    "attributes.preAuthorizedCodeLifespanS",
  );
  const saveButton = page.getByTestId("tokens-tab-save");

  // Fill with values below the minimum threshold (29 seconds)
  await nonceField.fill("29");
  await preAuthField.fill("29");

  await saveButton.click();

  // Check for validation error message
  const validationErrorText =
    "Please ensure all OID4VCI attribute fields are filled and values are 30 seconds or greater.";
  await expect(page.getByText(validationErrorText).first()).toBeVisible();
});
